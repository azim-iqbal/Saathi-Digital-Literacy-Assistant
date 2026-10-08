import time
import unittest
from unittest.mock import patch
from backend.planning import TaskPlan
from backend.research import TTL
from backend.tests.test_research_planning import snapshot, proposal

class PlanLifetimeTests(unittest.TestCase):
    def test_observed_staleness_cannot_be_reversed_by_clock_adjustment(self):
        snap=snapshot(); plan=TaskPlan(snap,proposal(),'page'); self.assertTrue(plan.review('page',True))
        future=time.time()+TTL['requirements']+1
        with patch('backend.planning.time.time',return_value=future):
            self.assertEqual(plan.eligibility({'c1':True})['state'],'NOT_EVALUATED')
        for _ in range(100):
            self.assertFalse(plan.review('page',True))
            self.assertIsNone(plan.next_step('page'))
            self.assertFalse(plan.complete)
