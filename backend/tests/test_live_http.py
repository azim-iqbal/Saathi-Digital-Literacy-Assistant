"""Local HTTP through actual adapters with synthetic provider transport; no external calls."""
import http.client
import json
import threading
import time
import unittest
from backend.gateway import Gateway
from backend.providers import RestProvider
from backend.server import make_server
from backend.tests.test_providers import decision

class LiveHttpTests(unittest.TestCase):
    def setUp(self):
        self.target = "n1"
        self.requests = []
        def transport(url, headers, payload):
            self.requests.append(payload)
            text = decision(self.target)
            if "googleapis" in url:
                return {"candidates": [{"finishReason": "STOP", "content": {"parts": [{"text": text}]}}]}
            return {"choices": [{"finish_reason": "stop", "message": {"content": text}}]}
        self.gateway = Gateway([RestProvider(p, "synthetic", "test-model", transport) for p in ("gemini", "groq")], mode="dual_ai")
        self.token = "test-only-" + "x" * 40
        self.server = make_server(self.token, self.gateway, port=0)
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)
        self.thread.start()
    def tearDown(self):
        self.server.shutdown(); self.server.server_close(); self.thread.join(); self.gateway.close()
    def body(self, **changes):
        return dict(dict(request_id="r", session_id="s", screen_revision=1, observed_at_ms=int(time.time()*1000),
            package_name="com.android.chrome", window_id=1, locale="en-IN", goal="Open help", controls=[dict(id="n1", label="Help")], previous_steps=[]), **changes)
    def post(self, body):
        connection = http.client.HTTPConnection(*self.server.server_address, timeout=3)
        try:
            connection.request("POST", "/v1/live-proposals", json.dumps(body), {"Authorization": "Bearer " + self.token, "Content-Type": "application/json"})
            response = connection.getresponse()
            return response.status, json.loads(response.read())
        finally: connection.close()
    def test_live_route_runs_both_adapters_and_rejects_replay(self):
        code, result = self.post(self.body())
        self.assertEqual(code, 200); self.assertEqual(result["status"], "accepted")
        self.assertEqual(len(self.requests), 2)
        self.assertEqual(self.post(self.body())[1]["reason"], "stale")
        self.assertEqual(len(self.requests), 2)
    def test_invented_target_returns_no_guidance(self):
        self.target = "n99"
        self.assertEqual(self.post(self.body())[1]["reason"], "invalid_target")
    def test_private_or_expired_request_never_reaches_models(self):
        for change in (dict(goal="PIN 1234"), dict(observed_at_ms=1), dict(audio="not allowed")):
            self.assertEqual(self.post(self.body(**change))[0], 400)
        self.assertEqual(self.requests, [])
