"""Loopback development server. Never expose this standard-library server publicly."""
import hmac
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
import os
import re
import threading
import time

from backend.gateway import Gateway, InvalidRequest, Snapshot
from backend.live import LiveSnapshot
from backend.incident import IncidentSnapshot

MAX_BODY = 8192


class LocalServer(ThreadingHTTPServer):
    """Bound handler threads as well as proposal workers; excess connections are closed."""
    def __init__(self, *args):
        self.handlers = threading.BoundedSemaphore(8)
        super().__init__(*args)

    def process_request(self, request, client_address):
        if not self.handlers.acquire(blocking=False):
            self.shutdown_request(request)
            return
        try:
            super().process_request(request, client_address)
        except Exception:
            self.handlers.release()
            raise

    def process_request_thread(self, request, client_address):
        try:
            super().process_request_thread(request, client_address)
        finally:
            self.handlers.release()


def decode(body):
    def unique(pairs):
        result = {}
        for key, value in pairs:
            if key in result:
                raise InvalidRequest("Duplicate key")
            result[key] = value
        return result
    try:
        return json.loads(body.decode("utf-8"), object_pairs_hook=unique,
                          parse_constant=lambda _: (_ for _ in ()).throw(InvalidRequest("Invalid number")))
    except (UnicodeError, ValueError, RecursionError) as error:
        raise InvalidRequest("Invalid JSON") from error


def make_server(token, gateway, port=8765):
    if not isinstance(token, str) or not 32 <= len(token) <= 256 or token.startswith("REPLACE_") or any(not 33 <= ord(c) <= 126 for c in token):
        raise ValueError("Set SAATHI_DEV_TOKEN to a random token of at least 32 ASCII characters")

    class Handler(BaseHTTPRequestHandler):
        # HTTP/1.0 closes each connection: no unread-body reuse after a rejected request.
        def setup(self):
            super().setup()
            self.connection.settimeout(3)

        def log_message(self, *_):
            pass  # Never print bearer tokens, requests, screen IDs, or provider output.

        def reply(self, code, payload):
            encoded = json.dumps(payload, separators=(",", ":")).encode()
            try:
                self.send_response(code)
                self.send_header("Content-Type", "application/json")
                self.send_header("Cache-Control", "no-store")
                self.send_header("Content-Length", str(len(encoded)))
                self.end_headers()
                self.wfile.write(encoded)
            except OSError:
                pass

        def do_GET(self):
            self.reply(200 if self.path == "/health" else 404,
                       {"mode": gateway.mode, "external_providers_enabled": gateway.mode == "dual_ai"} if self.path == "/health" else {"error": "not_found"})

        def do_POST(self):
            auth = self.headers.get_all("Authorization", [])
            if len(auth) != 1 or not hmac.compare_digest(auth[0].encode(), ("Bearer " + token).encode()):
                self.reply(401, {"error": "unauthorized"})
                return
            if self.path not in ("/v1/proposals", "/v1/live-proposals", "/v1/incident-assessment", "/v1/cancel", "/v1/connection-status", "/v1/provider-check"):
                self.reply(404, {"error": "not_found"})
                return
            lengths = self.headers.get_all("Content-Length", [])
            if (self.headers.get("Transfer-Encoding") or len(lengths) != 1
                    or not lengths[0].isascii() or not lengths[0].isdigit() or len(lengths[0]) > 6):
                self.reply(400, {"error": "invalid_length"})
                return
            length = int(lengths[0])
            if not 0 < length <= MAX_BODY:
                self.reply(413, {"error": "body_too_large"})
                return
            if len(self.headers.get_all("Content-Type", [])) != 1 or self.headers.get_content_type() != "application/json":
                self.reply(415, {"error": "json_required"})
                return
            try:
                deadline = time.monotonic() + 3
                body = bytearray()
                while len(body) < length:
                    remaining = deadline - time.monotonic()
                    if remaining <= 0: raise TimeoutError()
                    self.connection.settimeout(remaining)
                    chunk = self.rfile.read1(length - len(body))
                    if not chunk: break
                    body.extend(chunk)
                self.connection.settimeout(3)
                if len(body) != length:
                    raise InvalidRequest("Incomplete body")
                data = decode(body)
                if self.path == "/v1/connection-status":
                    if data != {}: raise InvalidRequest("Empty status request required")
                    from backend.diagnostics import status
                    self.reply(200, status(gateway))
                elif self.path == "/v1/provider-check":
                    from backend.diagnostics import check
                    self.reply(200, check(gateway, data))
                elif self.path == "/v1/cancel":
                    if not isinstance(data, dict) or set(data) != {"request_id"} or not isinstance(data["request_id"], str) or not re.fullmatch(r"[A-Za-z0-9_-]{1,64}", data["request_id"]):
                        raise InvalidRequest("Invalid cancellation")
                    self.reply(200, {"cancelled": gateway.cancel(data["request_id"]), "mode": gateway.mode})
                else:
                    if self.path in ("/v1/live-proposals", "/v1/incident-assessment"):
                        if gateway.mode != "dual_ai":
                            self.reply(200, gateway.rejected("not_configured"))
                        else:
                            self.reply(200, gateway.decide(IncidentSnapshot.parse(data) if self.path == "/v1/incident-assessment" else LiveSnapshot.parse(data)))
                    else:
                        self.reply(200, gateway.decide(Snapshot.parse(data)))
            except InvalidRequest:
                self.reply(400, {"error": "invalid_request"})
            except TimeoutError:
                self.reply(408, {"error": "request_timeout"})
            except Exception:
                # Keep the handler usable and never return raw storage/adapter failures.
                self.reply(503, {"error": "unavailable"})

    return LocalServer(("127.0.0.1", port), Handler)


def main():
    from backend.configuration import configured_gateway
    gateway = configured_gateway()
    try:
        server = make_server(os.environ.get("SAATHI_DEV_TOKEN", ""), gateway)
    except ValueError as error:
        gateway.close()
        raise SystemExit(str(error)) from None
    print("Saathi gateway: http://127.0.0.1:8765; mode=" + gateway.mode)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()
        gateway.close()


if __name__ == "__main__":
    main()
