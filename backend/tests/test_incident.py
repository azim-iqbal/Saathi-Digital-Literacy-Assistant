import json
import threading
import time
import unittest
from backend.gateway import Gateway, InvalidRequest
from backend.incident import IncidentSnapshot, parse_assessment, safe_summary
from backend.providers import RestProvider


def request(**changes):
    return dict(request_id="incident-r", session_id="incident-s", screen_revision=1, observed_at_ms=int(time.time()*1000),
        locale="en-IN", summary="Someone pretending to be bank staff tricked me into moving money.", concern="MONEY", consent=True, **changes)

class IncidentTests(unittest.TestCase):
    def test_privacy_and_explicit_consent_before_any_provider_work(self):
        for change in ({"consent": False}, {"summary": "My account is 123456789"}, {"summary": "Contact me at private@example.com"},
                {"summary": "my password is secretword"}, {"summary": " "*1300}, {"audio": "forbidden"}):
            data = {**request(), **change}
            with self.assertRaises(InvalidRequest): IncidentSnapshot.parse(data)
        self.assertTrue(safe_summary("I shared an OTP and somebody accessed my account."))

    def test_both_adapters_assess_independently_and_only_return_reviewed_codes(self):
        seen = []
        def transport(url, headers, payload, timeout):
            seen.append(payload)
            result = json.dumps({"category": "POSSIBLE_FINANCIAL", "signals": ["DECEPTIVE_REQUEST"]})
            if "googleapis" in url: return {"candidates": [{"finishReason": "STOP", "content": {"parts": [{"text": result}]}}]}
            return {"choices": [{"finish_reason": "stop", "message": {"content": result}}]}
        gateway = Gateway([RestProvider(p, "synthetic", "test", transport) for p in ("gemini", "groq")], mode="dual_ai")
        try:
            result = gateway.decide(IncidentSnapshot.parse(request()))
            self.assertEqual(result["category"], "POSSIBLE_FINANCIAL")
            self.assertEqual(len(seen), 2)
            self.assertNotIn("summary", result)
            self.assertNotIn("explanation", result)
            self.assertEqual(sum(gateway.calls.values()), 2)
        finally: gateway.close()

    def test_disagreement_withholds_assessment_and_cancellation_spends_nothing(self):
        def transport(url, headers, payload, timeout):
            result = json.dumps({"category": "POSSIBLE_FINANCIAL", "signals": ["DECEPTIVE_REQUEST" if "googleapis" in url else "UNAUTHORISED_TRANSACTION"]})
            return ({"candidates": [{"finishReason": "STOP", "content": {"parts": [{"text": result}]}}]} if "googleapis" in url else
                {"choices": [{"finish_reason": "stop", "message": {"content": result}}]})
        gateway = Gateway([RestProvider(p, "synthetic", "test", transport) for p in ("gemini", "groq")], mode="dual_ai")
        try:
            self.assertEqual(gateway.decide(IncidentSnapshot.parse(request()))["reason"], "disagreement")
            gateway.cancel("cancelled")
            data={**request(), "request_id": "cancelled", "screen_revision": 2}
            self.assertEqual(gateway.decide(IncidentSnapshot.parse(data))["reason"], "cancelled")
            self.assertEqual(sum(gateway.calls.values()), 2)
        finally: gateway.close()

    def test_models_cannot_certify_no_fraud_or_downgrade_financial_concern(self):
        snapshot=IncidentSnapshot.parse(request())
        for value in ({"category": "NO_FRAUD", "signals": []}, {"category": "CONFIRMED_CRIME", "signals": ["DECEPTIVE_REQUEST"]},
                {"category": "UNCLEAR", "signals": ["INSUFFICIENT_CONTEXT"]},
                {"category": "POSSIBLE_FINANCIAL", "signals": ["DECEPTIVE_REQUEST"], "url": "https://bad.example"}):
            with self.assertRaises(InvalidRequest): parse_assessment(json.dumps(value), snapshot)
