"""Conditional fallback uses fixtures only, never provider accounts."""
from dataclasses import replace
import threading
import time
import unittest
from unittest.mock import patch

from backend.errors import ProviderFailure
from backend.gateway import Gateway, InvalidRequest, Proposal, Snapshot
from backend.tests.test_providers import live
from backend.tests.test_gateway import payload


class Provider:
    model = "fixture"
    configured = True
    def __init__(self, name, action=None):
        self.id, self.action, self.calls = name, action, 0
    def propose(self, s, stop):
        self.calls += 1
        if self.action: return self.action(s, stop)
        return Proposal(s.session_id, s.screen_revision, s.package_name, s.window_id,
                        "HIGHLIGHT", "n1", "Open Help.", "A new screen appears.")


class PrimaryNavigationTests(unittest.TestCase):
    def setUp(self):
        self.primary, self.secondary = Provider("gemini"), Provider("groq")
        self.gateway = Gateway([self.primary, self.secondary], mode="dual_ai", timeout=.3)
    def tearDown(self): self.gateway.close()
    def decide(self, **changes): return self.gateway.decide(live(**changes), primary_navigation=True)

    def test_valid_primary_spends_exactly_one_reservation(self):
        reserved = []
        class Budget:
            def reserve(self, names): reserved.append(names); return True
            def close(self): pass
        self.gateway.budget = Budget()
        result = self.decide()
        self.assertEqual(result["decision_policy"], "primary")
        self.assertEqual(reserved, [["gemini"]])
        self.assertEqual(self.secondary.calls, 0)

    def test_configurable_primary_order_is_strict(self):
        from backend.configuration import provider_order
        with patch.dict("os.environ", {}, clear=True):
            self.assertEqual(provider_order(), ("groq", "gemini"))
        with patch.dict("os.environ", {"SAATHI_PRIMARY_PROVIDER": "groq"}):
            self.assertEqual(provider_order(), ("groq", "gemini"))
        with patch.dict("os.environ", {"SAATHI_PRIMARY_PROVIDER": "unknown"}):
            with self.assertRaises(ValueError): provider_order()

    def test_failed_primary_falls_back_once_and_names_actual_source(self):
        def fail(*_): raise ProviderFailure("provider_timeout")
        self.primary.action = fail
        result = self.decide()
        self.assertEqual(result["decision_policy"], "fallback")
        self.assertEqual(result["provenance"], [{"provider": "groq", "model": "fixture"}])
        self.assertEqual((self.primary.calls, self.secondary.calls), (1, 1))

    def test_missing_or_open_primary_does_not_block_configured_secondary(self):
        self.primary.configured = False
        self.assertEqual(self.decide()["decision_policy"], "fallback")
        self.assertEqual(self.primary.calls, 0)
        self.primary.configured = True
        self.gateway.failures["gemini"] = self.gateway.failure_limit
        self.assertEqual(self.decide(request_id="next", screen_revision=2)["decision_policy"], "fallback")

    def test_uncertain_or_handover_primary_uses_secondary_without_inventing_confidence(self):
        for revision, handover in enumerate((False, True), 1):
            self.primary.action = lambda s, _, handover=handover: Proposal(s.session_id, s.screen_revision,
                s.package_name, s.window_id, "HANDOVER" if handover else "HIGHLIGHT", None if handover else "n1",
                "Need clarification.", "Wait.", uncertainty=() if handover else ("unclear",))
            self.assertEqual(self.decide(request_id=str(revision), screen_revision=revision)["decision_policy"], "fallback")

    def test_invalid_fallback_target_is_never_accepted(self):
        self.primary.configured = False
        self.secondary.action = lambda s, stop: replace(Provider("fixture").propose(s, stop), target_id="invented")
        self.assertEqual(self.decide()["reason"], "invalid_target")

    def test_overall_timeout_does_not_start_another_call(self):
        now = [100.0]
        def expire(s, stop):
            now[0] = stop.overall_deadline + .01
            raise ProviderFailure("provider_timeout")
        self.primary.action = expire
        with patch("time.monotonic", side_effect=lambda: now[0]):
            self.assertEqual(self.decide()["reason"], "timeout")
        self.assertEqual(self.secondary.calls, 0)

    def test_cancel_during_primary_prevents_fallback_and_clears_active(self):
        ready = threading.Event()
        def pending(s, stop):
            ready.set(); stop.wait(1); raise ProviderFailure("provider_unavailable")
        self.primary.action = pending
        results = []
        thread = threading.Thread(target=lambda: results.append(self.decide()))
        thread.start(); self.assertTrue(ready.wait(1))
        self.gateway.cancel("request"); thread.join(1)
        self.assertFalse(thread.is_alive())
        self.assertEqual(results[0]["reason"], "cancelled")
        self.assertFalse(self.gateway.active)
        self.assertEqual(self.secondary.calls, 0)

    def test_fallback_does_not_get_a_new_deadline_or_bypass_quota(self):
        def fail(s, stop):
            self.deadline = stop.overall_deadline
            self.assertLess(stop.deadline, stop.overall_deadline)
            raise ProviderFailure("provider_unavailable")
        self.primary.action = fail
        def fallback(s, stop):
            self.assertEqual(stop.deadline, self.deadline)
            return Provider("fixture").propose(s, stop)
        self.secondary.action = fallback
        self.assertEqual(self.decide()["status"], "accepted")
        self.gateway.global_limit = 3
        self.assertEqual(self.decide(request_id="next", screen_revision=2)["reason"], "quota_exhausted")
        self.assertEqual(self.secondary.calls, 1)

    def test_stale_or_non_live_requests_cannot_use_this_policy(self):
        stale = live()
        stale = replace(stale, observed_at_ms=int(time.time()*1000)-16000)
        self.assertEqual(self.gateway.decide(stale, primary_navigation=True)["reason"], "stale")
        with self.assertRaises(InvalidRequest):
            self.gateway.decide(Snapshot.parse(payload()), primary_navigation=True)
        self.assertEqual((self.primary.calls, self.secondary.calls), (0, 0))
