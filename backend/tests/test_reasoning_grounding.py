"""General source-order and explicit-negation regressions, independent of any service."""
import dataclasses
import unittest
from backend.planning import PlanProposal,Step,validate_plan
from backend.navigation_hint import NavigationHint
from backend.tests.test_research_planning import snapshot,evidence
from backend.incident import IncidentSnapshot,IncidentProposal,validate_assessment
from backend.tests.test_incident import request

class ReasoningGroundingTests(unittest.TestCase):
    def test_explicit_order_cannot_be_dropped_reversed_or_missing_in_any_locale(self):
        snippets=[
            'Select “Alpha”. After reading “Alpha”, select “Beta”. After reading “Beta”, select “Gamma”.',
            '“Alpha” चुनें। “Alpha” पढ़ने के बाद “Beta” चुनें। “Beta” पढ़ने के बाद “Gamma” चुनें।',
            '“Alpha” chunein. “Alpha” padhne ke baad “Beta” chunein. “Beta” padhne ke baad “Gamma” chunein.'
        ]
        for snippet in snippets:
            snap=snapshot(ev=evidence(snippet=snippet))
            steps=tuple(Step('s'+str(i+1),label,('s'+str(i),) if i else (), 'e1',snippet,'USER_CONFIRMATION',NavigationHint('READ_OPTION',label)) for i,label in enumerate(('Alpha','Beta','Gamma')))
            self.assertIsNone(validate_plan(snap,PlanProposal(steps,())))
            dropped=tuple(dataclasses.replace(s,depends_on=()) for s in steps)
            self.assertEqual(validate_plan(snap,PlanProposal(dropped,())),'evidence_missing')
            self.assertEqual(validate_plan(snap,PlanProposal((dataclasses.replace(steps[2],depends_on=()),),())),'evidence_missing')
    def test_explicit_denial_cannot_support_transaction_or_account_access(self):
        summaries=[
            'Someone requested money. No money was transferred and nobody accessed my account.',
            'पैसे भेजने को कहा गया। कोई पैसे नहीं कटे और खाते में किसी ने प्रवेश नहीं किया।',
            'Paise maange gaye. Koi paise nahi kate aur kisi ne account access nahi kiya.'
        ]
        for summary in summaries:
            snap=IncidentSnapshot.parse({**request(), 'summary':summary,'concern':'UNSURE'})
            self.assertIsNone(validate_assessment(snap,IncidentProposal('POSSIBLE_FINANCIAL',('DECEPTIVE_REQUEST',))))
            for signal in ('UNAUTHORISED_TRANSACTION','ACCOUNT_ACCESS'):
                self.assertEqual(validate_assessment(snap,IncidentProposal('POSSIBLE_FINANCIAL',(signal,))),'uncertain')
