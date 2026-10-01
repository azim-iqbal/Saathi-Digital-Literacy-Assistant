"""Send one made-up request to the loopback mock gateway, never an external service."""
import http.client
import json
import os
import secrets
import time


def main():
    token = os.environ.get("SAATHI_DEV_TOKEN", "")
    if not token:
        raise SystemExit("Set SAATHI_DEV_TOKEN to the same development token used by the local server")
    request = {
        "request_id": secrets.token_hex(16), "session_id": secrets.token_hex(16),
        "screen_revision": 1, "observed_at_ms": int(time.time() * 1000),
        "package_name": "com.saathi", "window_id": 2, "locale": "en-IN", "task": "WATER_BILL",
        "eligible_node_ids": ["com.saathi:id/water_biller", "com.saathi:id/electricity_biller"],
    }
    connection = http.client.HTTPConnection("127.0.0.1", 8765, timeout=5)
    try:
        connection.request("POST", "/v1/proposals", json.dumps(request),
                           {"Authorization": "Bearer " + token, "Content-Type": "application/json"})
        response = connection.getresponse()
        print(json.dumps(json.loads(response.read()), indent=2))
        if response.status != 200:
            raise SystemExit("Mock request rejected")
    finally:
        connection.close()


if __name__ == "__main__":
    main()
