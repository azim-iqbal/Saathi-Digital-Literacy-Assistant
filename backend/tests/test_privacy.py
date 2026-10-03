import unittest
from backend.privacy import sensitive
from backend.live import safe_text
from backend.providers import proposal_from_text
from backend.gateway import InvalidRequest
from backend.tests.test_providers import live, decision
import json

class PrivacyRegressionTests(unittest.TestCase):
    def test_whitespace_cannot_bypass_wire_field_limits(self):
        self.assertTrue(safe_text("Help" + " " * 76, 80))
        for changes in (dict(goal="Help" + " " * 157), dict(controls=[dict(id="n1", label="Help" + " " * 77)]),
                        dict(previous_steps=["Help" + " " * 77])):
            with self.subTest(changes=changes), self.assertRaises(InvalidRequest):
                live(**changes)
        self.assertFalse(safe_text(" " * 80, 80))
        for field in ("explanation", "expected_outcome"):
            for padded in ("Help" + " " * 237, " " * 240):
                data = json.loads(decision()); data[field] = padded
                with self.subTest(field=field), self.assertRaises(InvalidRequest):
                    proposal_from_text(json.dumps(data), live())

    def test_public_travel_labels(self):
        for label in ('From', 'To', '01/10/2026', '2026-10-01', '₹5221', '₹6,398', 'INR 12345.50', 'Rs. 1234', '10:20 AM'):
            with self.subTest(label=label):
                self.assertFalse(sensitive(label))
                self.assertTrue(safe_text(label, 80))
                self.assertEqual(live(controls=[dict(id='n1', label=label)]).controls[0]['label'], label)

    def test_secrets_still_rejected(self):
        for label in ('Date of birth 01/10/2026', 'dateOfBirth', 'accountNumber', 'PIN ₹5221', 'enterOtp', 'Your code is 582139', '५८२१३९', '₹5221 code 582139', '₹1234567890123456', 'https://example.com/01/10/2026'):
            self.assertTrue(sensitive(label), label)

    def test_invalid_provider_field_types_are_protocol_errors(self):
        for field in ('target_id', 'explanation', 'expected_outcome'):
            for value in ([], {}, 42, True):
                data = json.loads(decision()); data[field] = value
                with self.assertRaises(InvalidRequest):
                    proposal_from_text(json.dumps(data), live())
