"""A separately authorized, four-call synthetic evaluation. Never a default test.

The exclusive marker and gateway caps prevent replay or implicit retries. Stored
output contains fixed diagnostics and scoring fields, never provider bodies/keys.
"""
import argparse
import json
import time
from pathlib import Path
from backend.configuration import configured_gateway
from backend.live import LiveSnapshot
from backend.planning import ResearchSnapshot
from backend.research import ResearchEvidence


def run(output, factory=configured_gateway):
    output=Path(output);output.mkdir(parents=True,exist_ok=True)
    with (output/'authorized-run.started').open('x') as lock:
        lock.write('Explicit authorization: <=4 provider calls; synthetic cases only; no retries.\n')
    gateway=factory();gateway.global_limit=min(4,gateway.global_limit);gateway.provider_limit=min(2,gateway.provider_limit)
    result=dict(provenance='Genuine adapters with fictional public cases; not Android E2E or real policy research',maximum_provider_calls=4,provider_reservations=0,cases=[])
    def save():
        result['provider_reservations']=sum(gateway.calls.values())
        (output/'results.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
    try:
        if gateway.mode!='dual_ai' or any(not getattr(p,'configured',True) for p in gateway.providers):
            result['blocker']='not_configured'; return result
        for kind in ('navigation','cited-plan'):
            now=int(time.time()*1000);identity='continuation-'+kind
            if kind=='navigation':
                snapshot=LiveSnapshot.parse(dict(request_id=identity,session_id=identity,screen_revision=1,observed_at_ms=now,
                    package_name='org.saathi.fixture',window_id=1,locale='en-IN',goal='Where can I get assistance?',
                    controls=[dict(id='n1',label='Catalog'),dict(id='n2',label='Help')],previous_steps=[]))
            else:
                evidence=ResearchEvidence('aaaaaaaaaaaaaaaaaaaaaaaa','https://fixture.example.test/','Fictional public instructions',
                    'official','synthetic evaluation review',now,None,'Fixture region','Select “Requirements”.','a'*64,'procedure')
                snapshot=ResearchSnapshot(identity,identity,1,now,'en-IN','Read requirements','Fixture region',(evidence,))
            response=gateway.decide(snapshot)
            accepted=response.get('status')=='accepted'
            good=accepted and (response.get('target_id')=='n2' if kind=='navigation' else
                any(step.get('navigation')==dict(kind='READ_OPTION',label='Requirements') for step in response.get('plan',{}).get('steps',[])))
            result['cases'].append(dict(case=kind,status='PASS' if good else 'FAIL',accepted=accepted,reason=response.get('reason'),
                expected='Help (n2)' if kind=='navigation' else 'cited Requirements hint; review required',
                request_id=identity,providers=[]))
    finally:
        gateway.close()
        # Include late diagnostic completion after all bounded workers stop.
        for row in result['cases']:
            row['providers']=[dict(provider=p.id,model=p.model,diagnostics=p.diagnostics(row['request_id']) if hasattr(p,'diagnostics') else None) for p in gateway.providers]
        save()
    return result

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--authorized',action='store_true');parser.add_argument('--output',required=True)
    args=parser.parse_args()
    if not args.authorized: raise SystemExit('New explicit four-call authorization required')
    value=run(args.output)
    print(json.dumps(dict(provider_reservations=value['provider_reservations'],cases=[dict(case=r['case'],status=r['status'],reason=r['reason']) for r in value['cases']],blocker=value.get('blocker'))))
