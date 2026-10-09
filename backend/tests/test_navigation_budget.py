"""Synthetic stage budgets: a slow primary must not consume the fallback's time."""
import threading
import time
import unittest
from dataclasses import replace
from backend.gateway import Gateway
from backend.tests.test_primary_navigation import Provider
from backend.tests.test_providers import live

class NavigationBudgetTests(unittest.TestCase):
    def test_slow_primary_has_a_stage_deadline_and_fallback_uses_original_budget(self):
        deadlines = []
        finished = threading.Event()
        def slow(s, stop):
            deadlines.append(stop.deadline)
            stop.wait(1)
            finished.set()
            return Provider('late').propose(s, stop)
        def quick(s, stop):
            deadlines.append(stop.deadline)
            return Provider('quick').propose(s, stop)
        a, b = Provider('gemini', slow), Provider('groq', quick)
        gateway = Gateway([a, b], timeout=.3, mode='dual_ai')
        try:
            start = time.monotonic()
            result = gateway.decide(live(), primary_navigation=True)
            self.assertEqual(result.get('decision_policy'), 'fallback', result)
            self.assertLess(time.monotonic()-start, .3)
            self.assertTrue(finished.wait(.2))
            self.assertLess(deadlines[0], deadlines[1])
            self.assertLessEqual(deadlines[1], start + .31)
            self.assertEqual(gateway.calls, {'gemini':1,'groq':1})
            self.assertFalse(gateway.active)
            self.assertFalse(gateway.circuit_probes)
        finally: gateway.close()

    def test_late_primary_cannot_replace_fallback_or_retain_a_circuit_probe(self):
        release = threading.Event()
        def slow(s, stop):
            release.wait(.5)
            return Provider('late').propose(s, stop)
        a, b = Provider('gemini', slow), Provider('groq')
        gateway = Gateway([a,b], timeout=.15, mode='dual_ai')
        gateway.failures['gemini'] = gateway.failure_limit
        gateway.circuit_until['gemini'] = time.monotonic()-1
        try:
            result = gateway.decide(live(), primary_navigation=True)
            self.assertEqual(result.get('decision_policy'), 'fallback', result)
            self.assertEqual(result['provenance'][0]['provider'], 'groq')
            self.assertFalse(gateway.circuit_probes)
            release.set()
            self.assertFalse(gateway.active)
        finally: release.set(); gateway.close()

    def test_both_slow_providers_remain_inside_shared_deadline(self):
        def slow(s, stop):
            stop.wait(1)
            return Provider('late').propose(s, stop)
        gateway = Gateway([Provider('gemini',slow),Provider('groq',slow)], timeout=.2, mode='dual_ai')
        try:
            start=time.monotonic()
            result=gateway.decide(live(), primary_navigation=True)
            self.assertEqual(result.get('reason'), 'timeout')
            self.assertLess(time.monotonic()-start,.35)
            self.assertEqual(sum(gateway.calls.values()),2)
        finally: gateway.close()
