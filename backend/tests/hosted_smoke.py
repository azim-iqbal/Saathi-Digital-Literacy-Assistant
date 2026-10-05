"""Real local Gunicorn smoke check with no keys, models, public listener or TLS claims."""
import http.client
import json
import os
from pathlib import Path
import secrets
import subprocess
import sys
import tempfile
import time
from backend.accounts import Accounts


def main():
    with tempfile.TemporaryDirectory(prefix="saathi-hosted-smoke-") as state:
        token = secrets.token_urlsafe(32)
        accounts = Accounts(Path(state) / "accounts.sqlite3")
        accounts.issue("smoke", token, int(time.time()) + 60, 2)
        accounts.close()
        environment = {k: v for k, v in os.environ.items() if k not in (
            "GEMINI_API_KEY", "GROQ_API_KEY", "GEMINI_MODEL", "GROQ_MODEL", "GUNICORN_CMD_ARGS",
            "SAATHI_DEV_TOKEN", "SAATHI_MAX_PROVIDER_CALLS", "SAATHI_MAX_CALLS_PER_PROVIDER")}
        environment["SAATHI_STATE_DIR"] = state
        server = subprocess.Popen([sys.executable, "-m", "gunicorn", "--config", "deploy/gunicorn.conf.py",
                                   "--bind", "127.0.0.1:18768", "backend.hosted:create_app()"], env=environment,
                                  stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        def request(path, body=None, auth=True, forwarded=True):
            client = http.client.HTTPConnection("127.0.0.1", 18768, timeout=3)
            try:
                headers = {"Content-Type": "application/json"}
                if auth: headers["Authorization"] = "Bearer " + token
                if forwarded: headers["X-Forwarded-Proto"] = "https"
                client.request("GET" if body is None else "POST", path, None if body is None else json.dumps(body), headers)
                response = client.getresponse()
                return response.status, json.loads(response.read())
            finally:
                client.close()
        try:
            for _ in range(50):
                if server.poll() is not None: raise RuntimeError("Gunicorn exited before readiness")
                try:
                    if request("/health") == (200, {"status": "up"}): break
                except OSError: pass
                time.sleep(.1)
            else: raise RuntimeError("Gunicorn readiness timeout")
            assert request("/health", forwarded=False)[0] == 403
            assert request("/v1/connection-status", {}, auth=False)[0] == 401
            code, status = request("/v1/connection-status", {})
            assert code == 200 and all(not p["configured"] for p in status["providers"])
            code, probe = request("/v1/provider-check", {"request_id": "offline", "consent": True})
            assert code == 200 and probe["reason"] == "not_configured"
            assert all(p["attempts"] == 0 for p in probe["providers"])
            accounts = Accounts(Path(state) / "accounts.sqlite3")
            accounts.revoke("smoke")
            accounts.close()
            assert request("/v1/connection-status", {})[0] == 401
            print(json.dumps({"result": "passed", "server": "gunicorn", "checks": 6, "external_model_calls": 0,
                              "tls_verified": False, "proxy": "trusted localhost header simulation"}))
        finally:
            server.terminate()
            try: server.wait(timeout=20)
            except subprocess.TimeoutExpired:
                server.kill(); server.wait(timeout=5)


if __name__ == "__main__": main()
