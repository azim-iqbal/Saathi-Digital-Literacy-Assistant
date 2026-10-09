"""Offline production-path stress and latency samples; never genuine AI or Android latency."""
import argparse
import json
import math
import statistics
import time
from dataclasses import replace
from backend.gateway import Gateway,Proposal
from backend.errors import ProviderFailure
from backend.tests.test_providers import live
from backend.tests.test_research_planning import snapshot,proposal,source,request
from backend.tests.test_incident import request as incident_request
from backend.incident import IncidentSnapshot,IncidentProposal
from backend.research import ResearchService,RegistryRetriever,SourceRegistry

class Provider:
    model='deterministic-latency-fixture'
    def __init__(self,name):self.id=name;self.fail=False
    def propose(self,s,stop):
        if self.fail:raise ProviderFailure('provider_unavailable')
        if isinstance(s,IncidentSnapshot):return IncidentProposal('POSSIBLE_FINANCIAL',('DECEPTIVE_REQUEST',))
        if hasattr(s,'evidence'):return proposal()
        return Proposal(s.session_id,s.screen_revision,s.package_name,s.window_id,'HIGHLIGHT','n1','Choose Help','Help opens')

def summary(values):
    ordered=sorted(values)
    return dict(n=len(values),p50_ms=round(statistics.median(values),3),p95_ms=round(ordered[math.ceil(.95*len(values))-1],3),max_ms=round(max(values),3))

def run(rounds=2000):
    providers=[Provider('gemini'),Provider('groq')]
    gateway=Gateway(providers,timeout=.2,global_limit=10000,provider_limit=5000,mode='dual_ai')
    timings={k:[] for k in ('navigation_primary','navigation_fallback','precancel','planning','incident','retrieval')}
    start=time.monotonic()
    try:
        for i in range(rounds):
            failed=i%2==1;providers[0].fail=failed
            snap=live(request_id='recovery_'+str(i),screen_revision=i+1)
            before=time.monotonic();result=gateway.decide(snap,primary_navigation=True)
            timings['navigation_fallback' if failed else 'navigation_primary'].append((time.monotonic()-before)*1000)
            assert result.get('status')=='accepted',(i,result)
            assert result['decision_policy']==('fallback' if failed else 'primary')
            cancelled=replace(snap,request_id='cancel_'+str(i));gateway.cancel(cancelled.request_id)
            before=time.monotonic();rejected=gateway.decide(cancelled,primary_navigation=True)
            timings['precancel'].append((time.monotonic()-before)*1000)
            assert rejected.get('reason')=='cancelled',rejected
            assert not gateway.active and not gateway.circuit_probes
            assert len(gateway.cancelled_requests)<=256 and len(gateway.sessions)==1
        providers[0].fail=False
        for i in range(50):
            plan=replace(snapshot(),request_id='plan_'+str(i),session_id='plan',screen_revision=i+1)
            incident=IncidentSnapshot.parse({**incident_request(), 'request_id':'incident_'+str(i),'screen_revision':i+1})
            for key,snap in [('planning',plan),('incident',incident)]:
                before=time.monotonic();result=gateway.decide(snap)
                timings[key].append((time.monotonic()-before)*1000)
                assert result.get('status')=='accepted',result
            service=ResearchService(RegistryRetriever(SourceRegistry([source()]),lambda *args:'<p>Read the public requirements.</p>'))
            before=time.monotonic();result=service.run(request())
            timings['retrieval'].append((time.monotonic()-before)*1000)
            assert result.get('status')=='researched',result
        result=dict(status='PASS',recovery_requests=rounds,precancelled_requests=rounds,
                    synthetic_provider_reservations=sum(gateway.calls.values()),genuine_provider_calls=0,
                    elapsed_seconds=round(time.monotonic()-start,3),timing={k:summary(v) for k,v in timings.items()},
                    retained_sessions=len(gateway.sessions),retained_cancel_ids=len(gateway.cancelled_requests),
                    scope='Backend production dispatch with instantaneous fictional providers/retrieval; not network/model/Android performance')
        return result
    finally:gateway.close()

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--output',required=True);args=parser.parse_args()
    from pathlib import Path
    result=run();Path(args.output).write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(dict(status=result['status'],recovery_requests=result['recovery_requests'])))
