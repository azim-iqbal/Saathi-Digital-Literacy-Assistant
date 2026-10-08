import threading
import time
import unittest
from unittest.mock import patch
from backend.gateway import InvalidRequest
from backend.research import ResearchService, RegistryRetriever, SourceRegistry
from backend.tests.test_research_planning import source, request

class ResearchRecoveryTests(unittest.TestCase):
    def service(self):
        return ResearchService(RegistryRetriever(SourceRegistry([source()]), lambda *a:'<p>Registration requires verification.</p>'))

    def test_cancelling_completed_bundle_cannot_resurrect_after_tombstone_expires(self):
        service=self.service(); service.run(request())
        self.assertTrue(service.cancel('research_one'))
        with patch('backend.research.time.monotonic',return_value=time.monotonic()+61):
            with self.assertRaises(InvalidRequest): service.bundle('research_one')
        self.assertNotIn('research_one',service.records)

    def test_reading_existing_bundle_does_not_wait_for_unrelated_retrieval(self):
        service=self.service(); service.run(request()); done=threading.Event(); errors=[]
        def read():
            try: service.bundle('research_one')
            except Exception as error: errors.append(type(error).__name__)
            finally: done.set()
        service.lock.acquire(); worker=threading.Thread(target=read); worker.start()
        try: self.assertTrue(done.wait(.3),'Bundle reads must not queue behind a six-second retrieval')
        finally: service.lock.release(); worker.join(2)
        self.assertEqual(errors,[])

    def test_retrieval_exception_is_sanitized_and_lane_recovers(self):
        service=self.service()
        with patch.object(service.retriever,'retrieve',side_effect=RuntimeError('synthetic secret not for client')):
            self.assertEqual(service.run(request()),{'status':'rejected','reason':'research_unavailable'})
        self.assertEqual(service.active,{})
        with patch('backend.research.time.monotonic',return_value=time.monotonic()+11):
            self.assertEqual(service.run({**request(), 'request_id':'retry'})['status'],'researched')

    def test_cancel_during_retrieval_discards_late_bundle(self):
        service=self.service(); entered=threading.Event(); release=threading.Event(); result=[]
        original=service.retriever.retrieve
        def slow(*args):
            value=original(*args); entered.set(); release.wait(2); return value
        with patch.object(service.retriever,'retrieve',side_effect=slow):
            worker=threading.Thread(target=lambda:result.append(service.run(request())));worker.start()
            try:
                self.assertTrue(entered.wait(1)); self.assertTrue(service.cancel('research_one'))
            finally: release.set();worker.join(3)
        self.assertEqual(result,[{'status':'rejected','reason':'cancelled'}]);self.assertEqual(service.records,{})

    def test_explicit_plan_attempts_have_distinct_identity_with_same_reviewed_bundle(self):
        from backend.planning import ResearchSnapshot
        service=self.service(); service.run(request())
        first=ResearchSnapshot.from_request(dict(request_id='a'*63+'1',research_id='research_one',consent=True),service)
        second=ResearchSnapshot.from_request(dict(request_id='a'*63+'2',research_id='research_one',consent=True),service)
        self.assertNotEqual(first.session_id,second.session_id)
        self.assertEqual(first.evidence,second.evidence)

    def test_explicit_retry_after_provider_failure_accepts_without_reusing_failed_session(self):
        from backend.gateway import Gateway
        from backend.errors import ProviderFailure
        from backend.routing import dispatch
        from backend.tests.device_server import FixtureProvider
        class Recovering(FixtureProvider):
            def __init__(self,name): super().__init__(name); self.attempts=0
            def propose(self,s,cancelled):
                self.attempts+=1
                if self.attempts==1: raise ProviderFailure('provider_timeout')
                return super().propose(s,cancelled)
        providers=[Recovering(name) for name in ('gemini','groq')]
        gateway=Gateway(providers,mode='dual_ai',global_limit=4,provider_limit=2)
        gateway.research=ResearchService(RegistryRetriever(SourceRegistry([source()]),lambda *args:
            '<p>Application requires registration. Registration requires verification. Select “Requirements”. Applicants must meet the local residency condition.</p>'))
        try:
            gateway.research.run(request())
            def attempt(identity):
                return dispatch(gateway,'/v1/task-plan',dict(request_id=identity,research_id='research_one',consent=True))
            self.assertEqual(attempt('first')['reason'],'provider_timeout')
            self.assertEqual(sum(gateway.calls.values()),2)
            result=attempt('second')
            self.assertEqual(result['status'],'accepted')
            self.assertEqual(result['plan']['status'],'REVIEW_REQUIRED')
            self.assertEqual([p.attempts for p in providers],[2,2])
            self.assertEqual(attempt('second')['reason'],'stale')
            self.assertEqual(attempt('third')['reason'],'quota_exhausted')
            self.assertEqual(sum(gateway.calls.values()),4)
        finally: gateway.close()

    def test_expired_cancelled_and_empty_bundles_have_typed_recovery_without_provider_dispatch(self):
        from backend.gateway import Gateway
        from backend.routing import dispatch
        gateway=Gateway(mode='dual_ai'); gateway.research=self.service()
        try:
            gateway.research.run(request())
            data=dict(request_id='plan',research_id='research_one',consent=True)
            gateway.research.cancel('research_one')
            self.assertEqual(dispatch(gateway,'/v1/task-plan',data)['reason'],'research_cancelled')
            with patch('backend.research.time.monotonic',return_value=time.monotonic()+301):
                self.assertEqual(dispatch(gateway,'/v1/task-plan',data)['reason'],'research_expired')
            gateway.research.records['empty']=(time.monotonic(),None,())
            self.assertEqual(dispatch(gateway,'/v1/task-plan',{**data,'research_id':'empty'})['reason'],'evidence_missing')
            self.assertEqual(sum(gateway.calls.values()),0)
            with self.assertRaises(InvalidRequest): dispatch(gateway,'/v1/task-plan',{**data,'consent':False})
        finally: gateway.close()
