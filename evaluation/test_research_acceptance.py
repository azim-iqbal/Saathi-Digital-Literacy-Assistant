"""Controlled architecture acceptance, never a live-model or real-service score.

Rules exist only in fictional retrieved documents/fixture extractor output. None
of these task names or prerequisite pairs occurs in production planning logic.
"""
import dataclasses
import json
from pathlib import Path
import time
import unittest
from backend.gateway import InvalidRequest
from backend.research import ResearchEvidence
from backend.planning import ResearchSnapshot,parse_plan,TaskPlan,validate_plan

CASES = (
    ('driving licence','Learner preparation','Identity verification'),
    ('Ayushman','Eligibility review','Region verification'),
    ('Kisan Credit','Application preparation','Eligibility review'),
    ('credit card','Product comparison','Eligibility review'),
    ('scholarship','Document preparation','Deadline verification'),
    ('passport','Document preparation','Identity verification'),
    ('subscription','Account review','Service verification'),
)

class ResearchAcceptanceTests(unittest.TestCase):
    def test_catalog_domains_share_the_same_dependency_and_eligibility_engine(self):
        catalog=json.loads((Path(__file__).parent/'catalog.json').read_text())
        for keyword,b,c in CASES:
            scenario=next((s for s in catalog if keyword.casefold() in s['user_goal'].casefold()),None)
            self.assertIsNotNone(scenario,keyword)
            goal=scenario['user_goal']
            # These are fictional acceptance conditions, not claims about actual policy.
            quotes=(f'For this fixture, the application requires {b}.',f'For this fixture, {b} requires {c}.',
                    'The fixture applicant must meet the stated regional condition.')
            now=int(time.time()*1000)
            evidence=ResearchEvidence('fixture', 'https://authority.example.test/fixture/', 'Fictional acceptance rules',
                'official','synthetic operator review',now,None,'Fixture region',' '.join(quotes),'a'*64,'requirements')
            body={'steps':[{'id':'s1','title':'Application','depends_on':['s2'],'evidence_id':'fixture','quote':quotes[0],'completion':'USER_CONFIRMATION'},
                {'id':'s2','title':b,'depends_on':['s3'],'evidence_id':'fixture','quote':quotes[1],'completion':'USER_CONFIRMATION'},
                {'id':'s3','title':c,'depends_on':[],'evidence_id':'fixture','quote':quotes[1],'completion':'USER_CONFIRMATION'}],
                'criteria':[{'id':'c1','evidence_id':'fixture','quote':quotes[2]}]}
            for locale in ('en-IN','hi-IN','hinglish'):
                with self.subTest(scenario=scenario['id'],locale=locale):
                    snapshot=ResearchSnapshot('r','s',1,now,locale,goal,'Fixture region',(evidence,))
                    proposal=parse_plan(json.dumps(body),snapshot)
                    plan=TaskPlan(snapshot,proposal,'fixture-window')
                    plan.review('fixture-window',True)
                    self.assertEqual(plan.next_step('fixture-window').title,c)
                    self.assertEqual(plan.eligibility({})['state'],'INSUFFICIENT_INFORMATION')
                    self.assertEqual(plan.eligibility({'c1':True})['state'],'POSSIBLY_ELIGIBLE')
                    for expected in ('s3','s2','s1'):
                        self.assertTrue(plan.confirm(expected,'fixture-window',plan.revision,True))
                    self.assertEqual(plan.snapshot.goal,goal)
                    self.assertTrue(plan.complete) # Only self-confirmed fixture checklist completion.
                    changed=dataclasses.replace(evidence,snippet='Rules changed. Prior requirements were withdrawn.')
                    self.assertEqual(validate_plan(dataclasses.replace(snapshot,evidence=(changed,)),proposal),'evidence_missing')
