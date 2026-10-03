"""Failure injection exercises capacity, privacy and useful recovery reasons without model calls."""
from concurrent.futures import ThreadPoolExecutor
import http.client
import json
import sqlite3
import tempfile
import threading
import time
import unittest
from unittest.mock import patch
from urllib.error import HTTPError, URLError

from backend.budget import PersistentBudget
from backend.errors import BudgetUnavailable, ProviderFailure
from backend.gateway import Gateway, InvalidRequest, MockProvider, Snapshot
from backend.providers import RestProvider, proposal_from_text
from backend.server import make_server
from backend.tests.test_gateway import payload
from backend.tests.test_providers import live, decision

class FailureTests(unittest.TestCase):
    def test_damaged_or_locked_storage_is_not_reported_as_quota(self):
        with tempfile.TemporaryDirectory() as folder:
            budget = PersistentBudget(folder + "/calls.db", 10, 5)
            try:
                budget.db.execute("INSERT INTO calls VALUES ('gemini', -1)"); budget.db.commit()
                with self.assertRaises(BudgetUnavailable): budget.reserve(["gemini", "groq"])
                budget.db.execute("DELETE FROM calls"); budget.db.commit()
                other = sqlite3.connect(folder + "/calls.db")
                try:
                    other.execute("BEGIN IMMEDIATE")
                    start = time.monotonic()
                    with self.assertRaises(BudgetUnavailable): budget.reserve(["gemini", "groq"])
                    self.assertLess(time.monotonic() - start, 1)
                finally: other.rollback(); other.close()
                self.assertTrue(budget.reserve(["gemini", "groq"]))
            finally: budget.close()

    def test_failed_worker_submission_releases_every_reserved_slot(self):
        for fail_at in (1, 2):
            gateway = Gateway()
            original = gateway.pool.submit
            calls = 0
            def submit(*args):
                nonlocal calls
                calls += 1
                if calls == fail_at: raise RuntimeError("executor failure")
                return original(*args)
            try:
                with patch.object(gateway.pool, "submit", side_effect=submit):
                    self.assertEqual(gateway.decide(Snapshot.parse(payload()))["reason"], "provider_unavailable")
                self.assertFalse(gateway.active)
                # Wait for an already submitted worker to exit; never release its slot twice.
                gateway.pool.shutdown(wait=True)
                for _ in range(4): self.assertTrue(gateway.slots.acquire(blocking=False))
                self.assertFalse(gateway.slots.acquire(blocking=False))
                for _ in range(4): gateway.slots.release()
            finally: gateway.close()

    def test_cancel_before_admission_spends_no_calls_and_does_not_poison_next_request(self):
        gateway = Gateway()
        try:
            self.assertFalse(gateway.cancel("request-1"))
            self.assertEqual(gateway.decide(Snapshot.parse(payload()))["reason"], "cancelled")
            self.assertEqual(sum(gateway.calls.values()), 0)
            self.assertEqual(gateway.decide(Snapshot.parse(payload(request_id="next")))["status"], "accepted")
            for n in range(300): gateway.cancel("cancel-" + str(n))
            self.assertLessEqual(len(gateway.cancelled_requests), 256)
        finally: gateway.close()

    def test_cancelled_completed_workers_do_not_trip_provider_circuit(self):
        class CancelledProvider(MockProvider):
            def propose(self, snapshot, cancelled):
                cancelled.set()
                raise InterruptedError()
        gateway = Gateway([CancelledProvider("a"), CancelledProvider("b")])
        try:
            for revision in range(1, 6):
                result = gateway.decide(Snapshot.parse(payload(request_id=str(revision), screen_revision=revision)))
                self.assertEqual(result["reason"], "cancelled")
            self.assertEqual(gateway.failures, {"a": 0, "b": 0})
        finally: gateway.close()

    def test_slow_reservation_cannot_start_expired_work(self):
        class SlowBudget:
            def reserve(self, _): time.sleep(.03); return True
            def close(self): pass
        gateway = Gateway(timeout=.01, budget=SlowBudget())
        try:
            self.assertEqual(gateway.decide(Snapshot.parse(payload()))["reason"], "timeout")
            self.assertEqual(sum(gateway.calls.values()), 0)
            self.assertFalse(gateway.active)
        finally: gateway.close()

    def test_provider_failure_reasons_are_sanitized(self):
        for error, reason in ((HTTPError("https://private", 401, "secret", {}, None), "provider_auth"),
                              (HTTPError("https://private", 429, "secret", {}, None), "provider_rate_limited"),
                              (HTTPError("https://private", 503, "secret", {}, None), "provider_unavailable"),
                              (URLError(TimeoutError("secret")), "provider_timeout")):
            def transport(*_): raise error
            providers = [RestProvider(p, "synthetic", "test", transport) for p in ("gemini", "groq")]
            gateway = Gateway(providers, mode="dual_ai")
            try:
                result = gateway.decide(live())
                self.assertEqual(result["reason"], reason)
                self.assertNotIn("secret", json.dumps(result))
            finally: gateway.close()

    def test_malformed_envelopes_and_escaped_surrogates_are_rejected(self):
        for provider, envelope in (("groq", []), ("groq", {"choices": [None]}),
                ("groq", {"choices": [{"finish_reason": "stop", "message": None}]}),
                ("gemini", {"candidates": [{"finishReason": "STOP", "content": None}]})):
            with self.assertRaises(InvalidRequest):
                RestProvider(provider, "synthetic", "test", lambda *_: envelope).propose(live(), threading.Event())
        value = json.loads(decision()); value["explanation"] = chr(0xd800)
        with self.assertRaises(InvalidRequest): proposal_from_text(json.dumps(value), live())

    def test_http_unexpected_failure_is_sanitized_and_next_request_recovers(self):
        gateway = Gateway()
        server = make_server("x" * 40, gateway, port=0)
        thread = threading.Thread(target=server.serve_forever, daemon=True); thread.start()
        def post(body, duplicate=False, path="/v1/proposals"):
            client = http.client.HTTPConnection(*server.server_address, timeout=2)
            try:
                encoded = json.dumps(body)
                client.putrequest("POST", path)
                client.putheader("Authorization", "Bearer " + "x" * 40)
                client.putheader("Content-Type", "application/json")
                if duplicate: client.putheader("Content-Type", "text/plain")
                client.putheader("Content-Length", str(len(encoded))); client.endheaders(encoded.encode())
                response = client.getresponse(); return response.status, json.loads(response.read())
            finally: client.close()
        try:
            with patch.object(gateway, "decide", side_effect=RuntimeError("secret")):
                self.assertEqual(post(payload()), (503, {"error": "unavailable"}))
            self.assertEqual(post(payload())[1]["status"], "accepted")
            self.assertEqual(post(payload(), True)[0], 415)
            self.assertEqual(post({"request_id": ""}, path="/v1/cancel")[0], 400)
        finally:
            server.shutdown(); server.server_close(); thread.join(); gateway.close()
