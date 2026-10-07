import json
import unittest
from backend.gateway import Gateway
from backend.providers import RestProvider
from backend.tests.test_providers import live, decision

class ValidationDiagnosticsTests(unittest.TestCase):
    def test_safe_codes_distinguish_rejections_without_retaining_output(self):
        cases=[(decision('missing'),'TARGET_NOT_FOUND'),
               (decision().replace('"uncertainty": []','"uncertainty": ["ambiguous"]'),'LOW_CONFIDENCE'),
               (decision().replace('Choose Help.','My OTP is 123456'),'SAFETY_REJECTED'),
               (decision().replace('HIGHLIGHT','UPLOAD'),'UNSUPPORTED_ACTION'),
               ('{"tool":"send private text"}','SCHEMA_INVALID')]
        for body,expected in cases:
            def transport(*args): return {'choices':[{'finish_reason':'stop','message':{'content':body}}]}
            providers=[RestProvider('groq','synthetic-private-key','fixture-model',transport),
                       RestProvider('gemini','synthetic-private-key','fixture-model',lambda *a: {'candidates':[{'finishReason':'STOP','content':{'parts':[{'text':body}]}}]})]
            gateway=Gateway(providers,mode='dual_ai')
            try:
                self.assertEqual(gateway.decide(live())['status'],'rejected')
                record=providers[0].diagnostics()
                self.assertEqual(record['validation_reason'],expected)
                self.assertEqual(record['session_id'],'session')
                encoded=json.dumps(record)
                for secret in ('123456','private text','synthetic-private-key','Choose Help','ambiguous'):
                    self.assertNotIn(secret,encoded)
            finally: gateway.close()

    def test_high_risk_live_goals_without_bound_destination_provenance_are_rejected_before_dispatch(self):
        from backend.gateway import InvalidRequest
        for goal in ('Government services','Credit application','Bank account help','सरकारी सेवा','sarkari seva'):
            with self.assertRaises(InvalidRequest): live(goal=goal)

    def test_changing_claims_and_private_labels_never_reach_navigation_provider(self):
        from backend.request_policy import research_claim
        from backend.gateway import InvalidRequest
        for goals in [('eligibility',('Am I eligible?','मेरी पात्रता','meri patrata')),
                      ('requirements',('apply for a service','आवेदन के दस्तावेज','apply karna hai')),
                      ('outage',('service down','सेवा बंद','kaam nahin kar rahi'))]:
            for goal in goals[1]:
                self.assertEqual(research_claim(goal),goals[0])
                with self.assertRaises(InvalidRequest): live(goal=goal)
        for text in ('Inbox','Message body','Document editor','Email subject','व्यक्तिगत जानकारी','Service unavailable','सेवा उपलब्ध नहीं है','service uplabdh nahin'):
            with self.assertRaises(InvalidRequest): live(controls=[{'id':'n0','label':text}])

    def test_http_headers_survive_malformed_body_without_raw_error_logging(self):
        from unittest.mock import patch
        from backend.providers import post_json
        from backend.gateway import RequestCancellation
        import time
        class Response:
            status=200
            def __enter__(self): return self
            def __exit__(self,*args): pass
            def read1(self,n):
                if getattr(self,'read',False): return b''
                self.read=True
                return b'not JSON: synthetic-private-provider-body'
        class Opener:
            def open(self,*a,**k): return Response()
        provider=RestProvider('groq','synthetic-private-key','fixture-model')
        with patch('urllib.request.build_opener',return_value=Opener()):
            with self.assertRaises(Exception): provider.propose(live(), RequestCancellation(time.monotonic()+2))
        record=provider.diagnostics()
        self.assertTrue(record['http_received'])
        self.assertEqual(record['http_status'],200)
        self.assertEqual(record['validation_reason'],'SCHEMA_INVALID')
        self.assertNotIn('synthetic-private',json.dumps(record))
