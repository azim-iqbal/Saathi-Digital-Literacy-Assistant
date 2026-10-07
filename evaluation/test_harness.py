import unittest
from evaluation.scenarios import catalog
from evaluation.scorecard import evaluate, GATES
from evaluation.run import DIMENSIONS
class HarnessTests(unittest.TestCase):
    def record(self):
        return dict(scenario_id='RW-001',level=3,status='PASS',scores={d:5 for d in DIMENSIONS},critical_failures=[],evidence=['fixture test evidence'],observed_at='synthetic-test')
    def test_catalog_has_distinct_meaningful_cases_and_dependency_coverage(self):
        cases=catalog()
        self.assertGreaterEqual(len(cases),75)
        self.assertEqual(len(cases),len({c['id'] for c in cases}))
        self.assertEqual(len(cases),len({(c['user_goal'],c['hidden_complication']) for c in cases}))
        for tag in ['prerequisite','eligibility']: self.assertGreaterEqual(sum(tag in c['tags'] for c in cases),15)
    def test_every_critical_gate_overrides_perfect_score(self):
        for gate in GATES:
            r=self.record();r['critical_failures']=[gate]
            self.assertEqual(evaluate(r)['status'],'FAIL')
    def test_missing_dimensions_cannot_be_reported_as_pass(self):
        r=self.record();r['scores']['source_quality']=None
        with self.assertRaises(ValueError):evaluate(r)
        r['status']='BLOCKED';self.assertIsNone(evaluate(r)['total_out_of_55'])
    def test_boolean_scores_and_evidenceless_results_are_rejected(self):
        r=self.record();r['scores']['privacy']=True
        with self.assertRaises(ValueError):evaluate(r)
        r=self.record();r['evidence']=[]
        with self.assertRaises(ValueError):evaluate(r)
