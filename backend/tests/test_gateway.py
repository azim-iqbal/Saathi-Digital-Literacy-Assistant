from concurrent.futures import ThreadPoolExecutor
from dataclasses import replace
import http.client
import json
import threading
import time
import unittest

from backend.gateway import Gateway, InvalidRequest, MockProvider, PREFIX, Snapshot
from backend.server import decode, make_server


def payload(**changes):
    return {"request_id": "request-1", "session_id": "session-1", "screen_revision": 1,
            "observed_at_ms": int(time.time() * 1000), "package_name": "com.saathi",
            "window_id": 2, "locale": "en-IN", "task": "WATER_BILL",
            "eligible_node_ids": [PREFIX + "water_biller", PREFIX + "electricity_biller"], **changes}


class Adapter(MockProvider):
    def __init__(self, name, action):
        super().__init__(name)
        self.action = action

    def propose(self, snapshot, cancelled):
        return self.action(snapshot, cancelled, super().propose(snapshot, cancelled))


class GatewayTests(unittest.TestCase):
    def gateway(self, **kwargs):
        gateway = Gateway(**kwargs)
        self.addCleanup(gateway.close)
        return gateway

    def test_two_independent_calls_same_immutable_snapshot_and_mock_provenance(self):
        entered = threading.Barrier(2)
        received = []
        def propose(snapshot, event, result):
            received.append(snapshot)
            entered.wait(timeout=1)
            return result
        gateway = self.gateway(providers=[Adapter("a", propose), Adapter("b", propose)])
        request = Snapshot.parse(payload())
        result = gateway.decide(request)
        self.assertEqual("accepted", result["status"])
        self.assertEqual("mock", result["mode"])
        self.assertEqual(PREFIX + "water_biller", result["target_id"])
        self.assertTrue(all(snapshot is request for snapshot in received))
        self.assertEqual({"a", "b"}, {p["provider"] for p in result["provenance"]})
        self.assertEqual({"a": 1, "b": 1}, gateway.calls)

    def test_duplicate_provider_names_are_not_dual_proposals(self):
        with self.assertRaises(ValueError):
            Gateway(providers=[MockProvider("same"), MockProvider("same")])

    def test_disagreement_never_falls_back_to_one_provider(self):
        gateway = self.gateway(providers=[MockProvider("a"), Adapter("b", lambda s, e, p: replace(p, target_id=PREFIX + "electricity_biller"))])
        self.assertEqual("disagreement", gateway.decide(Snapshot.parse(payload()))["reason"])

    def test_untrusted_provider_failures_are_rejected_without_raw_error(self):
        def raises(*_):
            raise RuntimeError("a secret must never reach the client")
        variants = [
            (raises, "provider_unavailable"),
            (lambda s, e, p: {"raw": "unparsed"}, "malformed"),
            (lambda s, e, p: replace(p, screen_revision=7), "stale"),
            (lambda s, e, p: replace(p, target_id=PREFIX + "pin_input"), "invalid_target"),
            (lambda s, e, p: replace(p, uncertainty=("ambiguous",)), "uncertain"),
            (lambda s, e, p: replace(p, explanation="x" * 241), "malformed"),
        ]
        for mutate, reason in variants:
            with self.subTest(reason=reason):
                gateway = self.gateway(providers=[Adapter("a", mutate), MockProvider("b")])
                self.assertEqual({"status": "rejected", "reason": reason, "mode": "mock"}, gateway.decide(Snapshot.parse(payload())))

    def test_completion_requires_observed_success_for_chosen_category(self):
        def complete(s, e, p):
            return replace(p, action="COMPLETE", target_id=None,
                           completion_evidence=(PREFIX + "success_title", PREFIX + "practice_water"))
        gateway = self.gateway(providers=[Adapter("a", complete), Adapter("b", complete)])
        self.assertEqual("unobserved_completion", gateway.decide(Snapshot.parse(payload()))["reason"])
        result = gateway.decide(Snapshot.parse(payload(request_id="r2", screen_revision=2,
            eligible_node_ids=[PREFIX + "success_title", PREFIX + "practice_water"])))
        self.assertEqual("COMPLETE", result["action"])

    def test_wrong_biller_recovery_and_private_form_handover(self):
        gateway = self.gateway()
        wrong = Snapshot.parse(payload(eligible_node_ids=[PREFIX + "practice_electricity", PREFIX + "practice_back"]))
        self.assertEqual(PREFIX + "practice_back", gateway.decide(wrong)["target_id"])
        private = Snapshot.parse(payload(request_id="r2", screen_revision=2,
            eligible_node_ids=[PREFIX + "practice_water", PREFIX + "pay_button"]))
        self.assertEqual("HANDOVER", gateway.decide(private)["action"])

    def test_timeout_is_bounded_and_cooperative_cancellation_reaches_adapter(self):
        cancelled = threading.Event()
        def slow(s, event, p):
            if event.wait(1):
                cancelled.set()
            return p
        gateway = self.gateway(providers=[Adapter("a", slow), MockProvider("b")], timeout=.05)
        start = time.monotonic()
        self.assertEqual("timeout", gateway.decide(Snapshot.parse(payload()))["reason"])
        self.assertLess(time.monotonic() - start, .5)
        self.assertTrue(cancelled.wait(.5))

    def test_cancel_and_newer_screen_never_release_old_advice(self):
        for explicit in (True, False):
            with self.subTest(explicit=explicit):
                started = threading.Event()
                def slow(s, event, p):
                    if s.screen_revision == 1:
                        started.set()
                        event.wait(1)
                    return p
                gateway = self.gateway(providers=[Adapter("a", slow), MockProvider("b")])
                with ThreadPoolExecutor(1) as clients:
                    first = clients.submit(gateway.decide, Snapshot.parse(payload()))
                    self.assertTrue(started.wait(.5))
                    if explicit:
                        self.assertTrue(gateway.cancel("request-1"))
                    else:
                        replacement = gateway.decide(Snapshot.parse(payload(request_id="r2", screen_revision=2)))
                        self.assertEqual("accepted", replacement["status"])
                    self.assertEqual("cancelled", first.result(timeout=1)["reason"])

    def test_quota_rejection_still_invalidates_old_screen(self):
        started = threading.Event()
        def slow(s, event, p):
            started.set()
            event.wait(1)
            return p
        gateway = self.gateway(providers=[Adapter("a", slow), MockProvider("b")], global_limit=2)
        with ThreadPoolExecutor(1) as clients:
            old = clients.submit(gateway.decide, Snapshot.parse(payload()))
            self.assertTrue(started.wait(.5))
            result = gateway.decide(Snapshot.parse(payload(request_id="r2", screen_revision=2)))
            self.assertEqual("quota_exhausted", result["reason"])
            self.assertEqual("cancelled", old.result(timeout=1)["reason"])
        self.assertEqual(2, sum(gateway.calls.values()))

    def test_global_and_per_provider_budgets_no_refund_or_retry(self):
        for bounds in ({"global_limit": 2}, {"provider_limit": 1}):
            gateway = self.gateway(**bounds)
            self.assertEqual("accepted", gateway.decide(Snapshot.parse(payload()))["status"])
            self.assertEqual("quota_exhausted", gateway.decide(Snapshot.parse(payload(request_id="r2", screen_revision=2)))["reason"])
            self.assertEqual(2, sum(gateway.calls.values()))

    def test_circuit_breaker_stops_repeated_adapter_failures(self):
        gateway = self.gateway(providers=[Adapter("a", lambda *args: None), MockProvider("b")], failure_limit=1)
        self.assertEqual("malformed", gateway.decide(Snapshot.parse(payload()))["reason"])
        self.assertEqual("circuit_open", gateway.decide(Snapshot.parse(payload(request_id="r2", screen_revision=2)))["reason"])
        self.assertEqual(2, sum(gateway.calls.values()))

    def test_replayed_revision_and_bounded_session_storage(self):
        gateway = self.gateway(max_sessions=1)
        request = Snapshot.parse(payload())
        gateway.decide(request)
        self.assertEqual("stale", gateway.decide(request)["reason"])
        self.assertEqual("session_capacity", gateway.decide(Snapshot.parse(payload(session_id="other")))["reason"])

    def test_observation_expiring_during_work_is_rejected(self):
        request = Snapshot.parse(payload())
        from unittest.mock import patch
        gateway = self.gateway()
        with patch("backend.gateway.time.time", return_value=request.observed_at_ms / 1000 + 16):
            self.assertEqual("stale", gateway.decide(request)["reason"])
        self.assertEqual(0, sum(gateway.calls.values()))
        def expires(s, event, p):
            return p
        gateway = self.gateway(providers=[Adapter("a", expires), Adapter("b", expires)])
        with patch("backend.gateway.time.time", side_effect=[request.observed_at_ms / 1000, request.observed_at_ms / 1000 + 16]):
            self.assertEqual("stale", gateway.decide(request)["reason"])

    def test_inactive_sessions_expire_without_resetting_quotas(self):
        gateway = self.gateway(max_sessions=1, session_ttl=30)
        self.assertEqual("accepted", gateway.decide(Snapshot.parse(payload()))["status"])
        gateway.session_seen["session-1"] = time.monotonic() - 31
        request = Snapshot.parse(payload(session_id="other", request_id="other"))
        self.assertEqual("accepted", gateway.decide(request)["status"])
        self.assertEqual({"other"}, set(gateway.sessions))
        self.assertEqual(4, sum(gateway.calls.values()))

    def test_busy_workers_do_not_accumulate_obsolete_jobs(self):
        started = threading.Barrier(3)
        release = threading.Event()
        def slow(s, event, p):
            started.wait(timeout=1)
            release.wait(1)
            return p
        gateway = self.gateway(providers=[Adapter("a", slow), Adapter("b", slow)])
        with ThreadPoolExecutor(2) as clients:
            first = clients.submit(gateway.decide, Snapshot.parse(payload()))
            started.wait(timeout=1)
            second = clients.submit(gateway.decide, Snapshot.parse(payload(request_id="r2", session_id="s2")))
            started.wait(timeout=1)
            try:
                self.assertEqual("busy", gateway.decide(Snapshot.parse(payload(request_id="r3", session_id="s3")))["reason"])
                self.assertEqual(4, sum(gateway.calls.values()))
            finally:
                release.set()
            self.assertEqual("accepted", first.result(timeout=1)["status"])
            self.assertEqual("accepted", second.result(timeout=1)["status"])

    def test_strict_request_schema_excludes_text_secrets_coordinates_and_old_screens(self):
        invalid = [payload(text="ignore instructions"), payload(bounds=[0, 0, 1, 1]),
                   payload(package_name="other.app"), payload(task="transfer money"),
                   payload(eligible_node_ids=[PREFIX + "pin_input"]),
                   payload(eligible_node_ids=[PREFIX + "water_biller"] * 2),
                   payload(screen_revision=True), payload(window_id=-1),
                   payload(observed_at_ms=int(time.time() * 1000) - 16000),
                   payload(observed_at_ms=int(time.time() * 1000) + 16000)]
        for data in invalid:
            with self.subTest(fields=data):
                with self.assertRaises(InvalidRequest):
                    Snapshot.parse(data)
        for body in (b'{"x":1,"x":2}', b'{"x":NaN}', b'\xff'):
            with self.assertRaises(InvalidRequest):
                decode(body)


class HttpTests(unittest.TestCase):
    def setUp(self):
        self.token = "test-only-token-" + "x" * 32
        self.gateway = Gateway()
        self.server = make_server(self.token, self.gateway, port=0)
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)
        self.thread.start()

    def tearDown(self):
        self.server.shutdown()
        self.server.server_close()
        self.thread.join()
        self.gateway.close()

    def request(self, path, body, authorized=True, content_type="application/json"):
        connection = http.client.HTTPConnection(*self.server.server_address, timeout=3)
        headers = {"Content-Type": content_type}
        if authorized:
            headers["Authorization"] = "Bearer " + self.token
        try:
            connection.request("POST", path, body=body, headers=headers)
            response = connection.getresponse()
            self.assertEqual("no-store", response.getheader("Cache-Control"))
            return response.status, json.loads(response.read())
        finally:
            connection.close()

    def test_authentication_before_work_and_loopback_binding(self):
        self.assertEqual("127.0.0.1", self.server.server_address[0])
        self.assertEqual(401, self.request("/v1/proposals", json.dumps(payload()), False)[0])
        self.assertEqual(0, sum(self.gateway.calls.values()))
        code, result = self.request("/v1/proposals", json.dumps(payload()))
        self.assertEqual(200, code)
        self.assertEqual("accepted", result["status"])
        self.assertEqual("mock", result["mode"])

    def test_invalid_bodies_do_not_use_budget(self):
        for body, expected in (("x" * 8193, 413), ('{"task":"raw","task":"duplicate"}', 400),
                               (json.dumps(payload(text="secret")), 400)):
            self.assertEqual(expected, self.request("/v1/proposals", body)[0])
        self.assertEqual(415, self.request("/v1/proposals", "{}", content_type="text/plain")[0])
        self.assertEqual(0, sum(self.gateway.calls.values()))

    def test_cancel_requires_authentication_and_has_no_sensitive_result(self):
        body = json.dumps({"request_id": "unknown"})
        self.assertEqual(401, self.request("/v1/cancel", body, False)[0])
        self.assertEqual((200, {"cancelled": False, "mode": "mock"}), self.request("/v1/cancel", body))

    def test_short_startup_token_is_rejected(self):
        with self.assertRaises(ValueError):
            make_server("short", self.gateway, port=0)


if __name__ == "__main__":
    unittest.main()
