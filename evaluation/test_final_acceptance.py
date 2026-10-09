import tempfile
import unittest
from pathlib import Path
from backend.gateway import Gateway,Proposal
from backend.planning import PlanProposal,Step,Criterion
from backend.incident import IncidentProposal
from backend.navigation_hint import NavigationHint
from evaluation.final_acceptance import run,score,SOURCE

class FinalAcceptanceTests(unittest.TestCase):
    def test_expected_chain_is_not_replaced_by_unconnected_valid_steps(self):
        steps=tuple(Step('s'+str(i+1),label,(), 'a'*24,SOURCE,'USER_CONFIRMATION',NavigationHint('READ_OPTION',label)) for i,label in enumerate(('Overview','Requirements','Checklist')))
        self.assertFalse(score('plan',PlanProposal(steps,()))['prerequisite_chain'])
        self.assertFalse(score('incident',IncidentProposal('POSSIBLE_FINANCIAL',('UNAUTHORISED_TRANSACTION',)))['respects_no_transfer'])
    def test_cap_replay_scorer_and_no_body_retention(self):
        class Provider:
            model='synthetic'
            def __init__(self,name):self.id=name
            def propose(self,s,c):
                if s.request_id.endswith('plan'):
                    labels=('Overview','Requirements','Checklist')
                    return PlanProposal(tuple(Step('s'+str(i+1),label,('s'+str(i),) if i else (), 'a'*24,SOURCE,'USER_CONFIRMATION',NavigationHint('READ_OPTION',label)) for i,label in enumerate(labels)),(Criterion('c1','a'*24,'Eligibility requires residence in Fixture region.'),))
                if s.request_id.endswith('incident'):return IncidentProposal('POSSIBLE_FINANCIAL',('DECEPTIVE_REQUEST',))
                return Proposal(s.session_id,s.screen_revision,s.package_name,s.window_id,'HIGHLIGHT','n2','private response canary','Help appears')
        with tempfile.TemporaryDirectory() as tmp:
            value=run(tmp,lambda:Gateway([Provider('gemini'),Provider('groq')],mode='dual_ai'))
            self.assertEqual(value['provider_reservations'],8)
            self.assertEqual([r['status'] for r in value['cases']],['PASS']*4)
            self.assertNotIn('private response canary',(Path(tmp)/'results.json').read_text())
            with self.assertRaises(FileExistsError):run(tmp,lambda:self.fail('No replay'))
