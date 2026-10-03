"""Explicit synthetic REST-envelope fixture for Android diagnostics tests; no external calls."""
from pathlib import Path
import os
import secrets
from backend.gateway import Gateway
from backend.providers import RestProvider
from backend.server import make_server
from backend.tests.test_diagnostics import reply

if __name__ == "__main__":
    path = Path("/tmp/saathi-gateway-test-token")
    if not path.exists():
        fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
        with os.fdopen(fd, "w") as stream: stream.write(secrets.token_urlsafe(32))
    providers = [RestProvider(p, "synthetic-key", "test-model", lambda *args, p=p: reply(p)) for p in ("gemini", "groq")]
    gateway = Gateway(providers, mode="dual_ai")
    server = make_server(path.read_text(), gateway, port=8767)
    print("Synthetic connection-check fixture at loopback:8767; no model calls", flush=True)
    try: server.serve_forever()
    finally: server.server_close(); gateway.close()
