import tempfile
import unittest
from pathlib import Path
from backend.gateway import Gateway,Proposal
from backend.planning import ResearchSnapshot,PlanProposal,Step
from backend.navigation_hint import NavigationHint
from evaluation.continuation_probe import run

class ContinuationProbeTests(unittest.TestCase):
    def test_navigation_only_stays_within_four_calls_and_exercises_two_locales(self):
        seen=[]
        class Provider:
            model='synthetic'
            def __init__(self,name):self.id=name
            def propose(self,s,c):
                seen.append((self.id,s.locale))
                return Proposal(s.session_id,s.screen_revision,s.package_name,s.window_id,'HIGHLIGHT','n2','synthetic','Help appears')
        with tempfile.TemporaryDirectory() as tmp:
            value=run(tmp,lambda:Gateway([Provider('gemini'),Provider('groq')],mode='dual_ai'),navigation_only=True)
            self.assertEqual(value['provider_reservations'],4)
            self.assertEqual([r['status'] for r in value['cases']],['PASS','PASS'])
            self.assertCountEqual(seen,[(provider,locale) for provider in ('gemini','groq') for locale in ('en-IN','hinglish')])

    def test_four_call_cap_no_replay_and_no_raw_response_retention(self):
        class Provider:
            model='synthetic'
            def __init__(self,name):self.id=name
            def propose(self,s,c):
                if isinstance(s,ResearchSnapshot):
                    return PlanProposal((Step('s1','Read',(),s.evidence[0].evidence_id,'Select “Requirements”.','USER_CONFIRMATION',NavigationHint('READ_OPTION','Requirements')),),())
                return Proposal(s.session_id,s.screen_revision,s.package_name,s.window_id,'HIGHLIGHT','n2','synthetic raw response','Help appears')
        with tempfile.TemporaryDirectory() as tmp:
            value=run(tmp,lambda:Gateway([Provider('gemini'),Provider('groq')],mode='dual_ai'))
            self.assertEqual(value['provider_reservations'],4)
            self.assertEqual([r['status'] for r in value['cases']],['PASS','PASS'])
            self.assertNotIn('synthetic raw response',(Path(tmp)/'results.json').read_text())
            with self.assertRaises(FileExistsError):run(tmp,lambda:self.fail('Replay must fail before creating adapters'))
