"""Transient outages must recover without restarting the server or resetting quotas."""
from dataclasses import replace
import threading
import unittest

from backend.errors import ProviderFailure
from backend.gateway import Gateway, MockProvider, Snapshot
from backend.tests.test_gateway import payload


class Flaky(MockProvider):
    fail = False
    entered = None
    release = None
    def propose(self, snapshot, stop):
        if self.entered:
            self.entered.set()
            self.release.wait(1)
        if self.fail:
            raise ProviderFailure("provider_unavailable")
        return super().propose(snapshot, stop)


class CircuitRecoveryTests(unittest.TestCase):
    def setUp(self):
        self.provider = Flaky("a")
        self.gateway = Gateway([self.provider, MockProvider("b")], failure_limit=2)
        self.revision = 0
    def tearDown(self): self.gateway.close()
    def decide(self):
        self.revision += 1
        return self.gateway.decide(Snapshot.parse(payload(request_id=f"r{self.revision}", screen_revision=self.revision)))
    def trip(self):
        self.provider.fail = True
        for _ in range(2): self.assertEqual(self.decide()["reason"], "provider_unavailable")
        self.assertEqual(self.decide()["reason"], "circuit_open")
    def expire(self):
        # Advance only the circuit's cooldown; keep screen freshness/decision clocks intact.
        self.gateway.circuit_until["a"] = 0

    def test_success_breaks_the_failure_streak(self):
        self.provider.fail = True
        self.assertEqual(self.decide()["reason"], "provider_unavailable")
        self.provider.fail = False
        self.assertEqual(self.decide()["status"], "accepted")
        self.provider.fail = True
        self.assertEqual(self.decide()["reason"], "provider_unavailable")
        self.provider.fail = False
        self.assertEqual(self.decide()["status"], "accepted")

    def test_cooldown_probe_recovers_without_resetting_call_count(self):
        self.trip()
        before = dict(self.gateway.calls)
        self.expire(); self.provider.fail = False
        self.assertEqual(self.decide()["status"], "accepted")
        self.assertEqual(self.gateway.failures["a"], 0)
        self.assertEqual(self.gateway.calls["a"], before["a"] + 1)

    def test_failed_probe_reopens_circuit(self):
        self.trip(); self.expire()
        self.assertEqual(self.decide()["reason"], "provider_unavailable")
        self.assertEqual(self.decide()["reason"], "circuit_open")

    def test_only_one_probe_runs_and_cancellation_releases_probe(self):
        self.trip(); self.expire(); self.provider.fail = False
        self.provider.entered, self.provider.release = threading.Event(), threading.Event()
        result = []
        probe = Snapshot.parse(payload(request_id="probe", session_id="probe_session"))
        thread = threading.Thread(target=lambda: result.append(self.gateway.decide(probe)))
        try:
            thread.start(); self.assertTrue(self.provider.entered.wait(1))
            self.assertEqual(self.decide()["reason"], "circuit_open")
            self.gateway.cancel("probe")
            thread.join(1); self.assertFalse(thread.is_alive())
            self.assertEqual(result[0]["reason"], "cancelled")
        finally:
            self.provider.release.set(); thread.join(2)
        self.provider.entered = None
        self.assertEqual(self.decide()["status"], "accepted")

    def test_quota_still_blocks_probe(self):
        self.trip(); self.expire()
        self.gateway.global_limit = sum(self.gateway.calls.values())
        self.assertEqual(self.decide()["reason"], "quota_exhausted")

    def test_uncertain_answer_does_not_mark_healthy_transport_as_failed(self):
        class Uncertain(MockProvider):
            def propose(self, snapshot, stop):
                return replace(super().propose(snapshot, stop), uncertainty=("ambiguous",))
        self.gateway.providers = (Uncertain("a"), MockProvider("b"))
        for _ in range(4): self.assertEqual(self.decide()["reason"], "uncertain")
        self.assertEqual(self.gateway.failures["a"], 0)
