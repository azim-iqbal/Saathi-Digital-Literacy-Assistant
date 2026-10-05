"""WSGI entry for a single bounded worker behind a trusted HTTPS reverse proxy."""
import atexit
from concurrent.futures import ThreadPoolExecutor
import json
import os
from pathlib import Path
import threading

from backend.accounts import Accounts
from backend.budget import PersistentBudget
from backend.configuration import UnconfiguredProvider, provider_order
from backend.gateway import Gateway, InvalidRequest
from backend.providers import RestProvider
from backend.routing import PATHS, dispatch
from backend.server import MAX_BODY, decode


class UserBudget:
    def __init__(self, shared, principal, limit):
        self.shared, self.principal, self.limit = shared, principal, limit
    def reserve(self, providers):
        return self.shared.reserve(providers, self.principal, self.limit)
    def close(self): pass


class HostedApplication:
    def __init__(self, accounts, budget, provider_factory, max_users=32):
        self.accounts, self.budget, self.provider_factory = accounts, budget, provider_factory
        self.max_users = max_users
        self.lock = threading.Lock()
        self.users = {}
        self.pool = ThreadPoolExecutor(max_workers=4, thread_name_prefix="saathi-hosted")
        self.slots = threading.BoundedSemaphore(4)
        self.handlers = threading.BoundedSemaphore(8)

    def gateway(self, principal, limit):
        with self.lock:
            if principal not in self.users:
                if len(self.users) >= self.max_users:
                    return None
                self.users[principal] = Gateway(self.provider_factory(), timeout=10, mode="dual_ai",
                    budget=UserBudget(self.budget, principal, limit), executor=self.pool, slots=self.slots,
                    global_limit=10000, provider_limit=5000)
            self.users[principal].budget.limit = limit
            return self.users[principal]

    def handle(self, environ):
        if environ.get("wsgi.url_scheme") != "https":
            return 403, {"error": "https_required"}
        if environ.get("PATH_INFO") == "/health" and environ.get("REQUEST_METHOD") == "GET":
            return 200, {"status": "up"}
        identity = self.accounts.authenticate(environ.get("HTTP_AUTHORIZATION"))
        if identity is None:
            return 401, {"error": "unauthorized"}
        if environ.get("REQUEST_METHOD") != "POST":
            return 405, {"error": "method_not_allowed"}
        path = environ.get("PATH_INFO")
        if path not in PATHS or environ.get("QUERY_STRING"):
            return 404, {"error": "not_found"}
        if environ.get("HTTP_TRANSFER_ENCODING"):
            return 400, {"error": "invalid_length"}
        size = environ.get("CONTENT_LENGTH", "")
        if not isinstance(size, str) or not size.isascii() or not size.isdigit() or len(size) > 6:
            return 400, {"error": "invalid_length"}
        if not 0 < int(size) <= MAX_BODY:
            return 413, {"error": "body_too_large"}
        if environ.get("CONTENT_TYPE", "").split(";", 1)[0].strip() != "application/json":
            return 415, {"error": "json_required"}
        body = environ["wsgi.input"].read(int(size))
        if len(body) != int(size):
            raise InvalidRequest("Incomplete body")
        data = decode(body)
        gateway = self.gateway(*identity)
        if gateway is None:
            return 503, {"error": "unavailable"}
        result = dispatch(gateway, path, data)
        # A revoked/expired credential cannot receive results from work already in flight.
        if self.accounts.authenticate(environ.get("HTTP_AUTHORIZATION")) != identity:
            return 401, {"error": "unauthorized"}
        return 200, result

    def __call__(self, environ, start_response):
        admitted = self.handlers.acquire(blocking=False)
        try:
            if not admitted:
                code, payload = 503, {"error": "unavailable"}
            else:
                try:
                    code, payload = self.handle(environ)
                except InvalidRequest:
                    code, payload = 400, {"error": "invalid_request"}
                except Exception:
                    code, payload = 503, {"error": "unavailable"}
            data = json.dumps(payload, separators=(",", ":")).encode()
            titles = {200: "OK", 400: "Bad Request", 401: "Unauthorized", 403: "Forbidden", 404: "Not Found",
                      405: "Method Not Allowed", 413: "Content Too Large", 415: "Unsupported Media Type", 503: "Service Unavailable"}
            start_response(f"{code} {titles[code]}", [("Content-Type", "application/json"), ("Content-Length", str(len(data))),
                           ("Cache-Control", "no-store"), ("X-Content-Type-Options", "nosniff")])
            return [data]
        finally:
            if admitted:
                self.handlers.release()

    def close(self):
        for gateway in self.users.values():
            gateway.close()
        self.pool.shutdown(wait=True)
        self.budget.close()
        self.accounts.close()


def create_app():
    # Hosted mode never reads a developer's .env or accepts the shared development token.
    state = Path(os.environ["SAATHI_STATE_DIR"])
    if not state.is_dir():
        raise ValueError("Create a private persistent state directory first")
    provider_order()  # Reject invalid routing configuration at startup.
    def providers():
        result = []
        for name in provider_order():
            try:
                result.append(RestProvider(name, os.environ.get(name.upper() + "_API_KEY", ""), os.environ.get(name.upper() + "_MODEL", "")))
            except ValueError:
                result.append(UnconfiguredProvider(name))
        return result
    total = int(os.environ.get("SAATHI_MAX_PROVIDER_CALLS", "100"))
    each = int(os.environ.get("SAATHI_MAX_CALLS_PER_PROVIDER", "50"))
    if not 2 <= total <= 10000 or not 1 <= each <= 5000:
        raise ValueError("Invalid hosted call caps")
    app = HostedApplication(Accounts(state / "accounts.sqlite3"), PersistentBudget(state / "budget.sqlite3", total, each), providers)
    atexit.register(app.close)
    return app
