"""Offline latency injection: late model replies must never become current guidance."""
import json
import threading
import time
import unittest
from unittest.mock import patch

from backend.errors import ProviderFailure
from backend.gateway import Gateway, MockProvider, RequestCancellation, Snapshot, validate
from backend.providers import RestProvider, post_json
from backend.tests.test_gateway import payload
from backend.tests.test_providers import decision, live


def envelope(provider):
    if provider == "gemini":
        return {"candidates": [{"finishReason": "STOP", "content": {"parts": [{"text": decision()}]}}],
                "usageMetadata": {"totalTokenCount": 12}}
    return {"choices": [{"finish_reason": "stop", "message": {"content": decision()}}],
            "usage": {"total_tokens": 12}}


class DeadlineTests(unittest.TestCase):
    def test_both_adapters_pass_remaining_deadline_to_transport(self):
        for provider in ("gemini", "groq"):
            for remaining, expected in ((.25, .25), (20, 8)):
                seen = []
                def transport(url, headers, body, timeout):
                    seen.append(timeout)
                    return envelope(provider)
                adapter = RestProvider(provider, "synthetic", "fixture", transport)
                with patch("time.monotonic", return_value=100):
                    result = adapter.propose(live(), RequestCancellation(100 + remaining))
                self.assertEqual(result.target_id, "n1")
                self.assertEqual(seen, [expected])

    def test_expired_adapter_never_dispatches(self):
        adapter = RestProvider("groq", "synthetic", "fixture", lambda *_: self.fail("Expired request sent"))
        with patch("time.monotonic", return_value=100):
            with self.assertRaises(ProviderFailure) as raised:
                adapter.propose(live(), RequestCancellation(100))
        self.assertEqual(raised.exception.reason, "provider_timeout")
        self.assertFalse(adapter.diagnostics()["http_received"])

    def test_late_valid_reply_retains_usage_but_is_not_a_success(self):
        for provider in ("gemini", "groq"):
            now = [100.0]
            def transport(*_):
                now[0] += .3
                return envelope(provider)
            adapter = RestProvider(provider, "synthetic", "fixture", transport)
            with patch("time.monotonic", side_effect=lambda: now[0]):
                with self.assertRaises(ProviderFailure) as raised:
                    adapter.propose(live(), RequestCancellation(100.25))
            self.assertEqual(raised.exception.reason, "provider_timeout")
            record = adapter.diagnostics()
            self.assertEqual(record["outcome"], "provider_timeout")
            self.assertTrue(record["http_received"])
            self.assertEqual(record["total_tokens"], 12)

    def test_cancellation_during_transport_keeps_usage_without_advice(self):
        event = RequestCancellation(time.monotonic() + 5)
        def transport(*_):
            event.set()
            return envelope("groq")
        adapter = RestProvider("groq", "synthetic", "fixture", transport)
        with self.assertRaises(InterruptedError):
            adapter.propose(live(), event)
        self.assertEqual(adapter.diagnostics()["outcome"], "cancelled")
        self.assertEqual(adapter.diagnostics()["total_tokens"], 12)

    def test_old_screen_stops_waiting_at_freshness_deadline_without_tripping_circuit(self):
        observed = []
        released = [threading.Event(), threading.Event()]
        class Slow(MockProvider):
            def propose(self, snapshot, cancelled):
                observed.append(cancelled.deadline)
                try:
                    cancelled.wait(2)
                    return super().propose(snapshot, cancelled)
                finally:
                    released[int(self.id)].set()
        gateway = Gateway([Slow("0"), Slow("1")], timeout=2)
        try:
            request = Snapshot.parse(payload(observed_at_ms=int(time.time() * 1000) - 14800))
            start = time.monotonic()
            self.assertEqual(gateway.decide(request)["reason"], "stale")
            self.assertLess(time.monotonic() - start, 1)
            self.assertTrue(all(event.wait(1) for event in released))
            self.assertEqual(len(observed), 2)
            self.assertEqual(observed[0], observed[1])
            self.assertEqual(gateway.failures, {"0": 0, "1": 0})
            self.assertFalse(gateway.active)
            for _ in range(4):
                self.assertTrue(gateway.slots.acquire(blocking=False))
            self.assertFalse(gateway.slots.acquire(blocking=False))
            for _ in range(4):
                gateway.slots.release()
        finally:
            gateway.close()

    def test_validation_cannot_release_reply_after_decision_deadline(self):
        gateway = Gateway(timeout=1)
        now = [100.0]
        def slow_validation(snapshot, proposal):
            now[0] += 1
            return validate(snapshot, proposal)
        try:
            with patch("time.monotonic", side_effect=lambda: now[0]), patch("backend.gateway.validate", side_effect=slow_validation):
                self.assertEqual(gateway.decide(Snapshot.parse(payload()))["reason"], "timeout")
        finally:
            gateway.close()

    def test_expiry_during_reservation_spends_no_provider_calls(self):
        now = [100.0]
        class SlowBudget:
            def reserve(self, _):
                now[0] += .2
                return True
            def close(self): pass
        gateway = Gateway(timeout=2, budget=SlowBudget())
        try:
            request = Snapshot.parse(payload(observed_at_ms=int(time.time() * 1000) - 14900))
            with patch("time.monotonic", side_effect=lambda: now[0]):
                self.assertEqual(gateway.decide(request)["reason"], "stale")
            self.assertEqual(sum(gateway.calls.values()), 0)
        finally:
            gateway.close()

    def test_transport_refuses_late_body_and_late_eof(self):
        for late_eof in (False, True):
            now = [100.0]
            class Response:
                reads = 0
                def __enter__(self): return self
                def __exit__(self, *_): pass
                def read1(self, _):
                    self.reads += 1
                    if self.reads == 1 and late_eof:
                        return json.dumps({"ok": True}).encode()
                    now[0] = 101
                    return b"" if late_eof else b'{"ok":true}'
            with patch("time.monotonic", side_effect=lambda: now[0]), patch("backend.providers.urllib.request.build_opener") as opener:
                opener.return_value.open.return_value = Response()
                with self.assertRaises(TimeoutError):
                    post_json("https://fixture.invalid", {}, {}, timeout=.5)

