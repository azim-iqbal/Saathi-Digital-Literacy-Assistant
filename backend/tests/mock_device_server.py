"""Local Android regression server. No environment loading or external providers."""
import os
from pathlib import Path
import secrets
from backend.gateway import Gateway
from backend.server import make_server


if __name__ == "__main__":
    token_path = Path("/tmp/saathi-release-test-token")
    if not token_path.exists():
        with os.fdopen(os.open(token_path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600), "w") as output:
            output.write(secrets.token_urlsafe(32))
    gateway = Gateway(global_limit=200, provider_limit=100)
    server = make_server(token_path.read_text(), gateway, port=8766)
    print("Synthetic mock server ready on loopback:8766", flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()
        gateway.close()
