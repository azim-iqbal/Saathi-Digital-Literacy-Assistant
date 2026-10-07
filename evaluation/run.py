"""Offline boundary evaluation against production backend classes; no credentials loaded.
Run: python3 -m evaluation.run --output evaluation/results/offline.json
Exit nonzero on a failed executable probe, not on honestly blocked domain scenarios.
"""
import argparse
import json
import time
import threading
import urllib.error
from dataclasses import replace
from pathlib import Path
from backend.gateway import Gateway, InvalidRequest, Proposal
from backend.providers import RestProvider, proposal_from_text
from backend.live import LiveSnapshot, validate_live
from backend.tests.test_providers import live, decision
from backend.tests.test_primary_navigation import Provider
from evaluation.scenarios import catalog

DIMENSIONS = ['goal_understanding', 'prerequisite_discovery', 'information_accuracy', 'source_quality',
              'navigation_accuracy', 'adaptability', 'error_recovery', 'privacy', 'safety',
              'explanation_quality', 'completion_detection']

def score(dimensions, passed):
    # No inferred points for dimensions the probe cannot measure. Never inflate a /55 score.
    return {d: (5 if passed else 0) if d in dimensions else None for d in DIMENSIONS}

def run():
    results = []
    def check(name, dims, action, critical=False):
        start = time.perf_counter()
        try:
            metrics = action() or {}
            passed, failure = True, None
        except Exception as e:
            passed, metrics, failure = False, {}, type(e).__name__
        results.append(dict(id=f'B-{len(results)+1:03}', name=name, level=1, scope='production backend boundary with synthetic input/provider',
            status='PASS' if passed else 'FAIL', critical=critical, scores=score(dims, passed), total_out_of_55=None,
            duration_ms=round((time.perf_counter()-start)*1000, 3), metrics=metrics, failure=failure))
    def require(condition):
        if not condition: raise AssertionError()
    def rejects(data):
        try: LiveSnapshot.parse(data)
        except InvalidRequest: return
        raise AssertionError()
    def body(**changes):
        s = live()
        return {**s.__dict__, 'controls':[dict(c) for c in s.controls], 'previous_steps':[], **changes}
    for name, text in [('password','password test-only'),('OTP','OTP 582139'),('Aadhaar','1234 5678 9012'),
                       ('card','4111 1111 1111 1111'),('CVV','CVV 123'),('bank','account number 123456'),
                       ('invisible password','pass\u200bword fictional'),('invisible OTP','5\u200b8\u200b2\u200b1\u200b3\u200b9'),
                       ('spaced OTP','5 8 2 1 3 9'),('fullwidth secret cue','ｐａｓｓｗｏｒｄ fictional')]:
        check('Reject synthetic '+name+' before provider dispatch', ['privacy','safety'], lambda t=text: rejects(body(goal=t)), True)
    for label in ['From','To','01/10/2026','₹5,221','10:20 AM','Shopping']:
        check('Public label remains usable: '+label, ['privacy','navigation_accuracy'], lambda t=label: require(live(controls=[dict(id='n1',label=t)]).controls[0]['label']==t))
    check('Ambiguous repeated labels cannot enter cloud targeting', ['navigation_accuracy','safety'],
          lambda: rejects(body(controls=[dict(id='n1',label='Help'),dict(id='n2',label=' help ')])))
    for name, changes in [('expired',{'observed_at_ms':1}),('protected package',{'package_name':'com.android.systemui'}),
                          ('raw image',{'image':'synthetic'}),('raw audio',{'audio':'synthetic'}),
                          ('extra coordinates',{'coordinates':[0,0]}),('consequential goal',{'goal':'Pay now'})]:
        check('Reject '+name, ['safety','privacy'], lambda c=changes: rejects(body(**c)), True)
    for name, text in [('invalid JSON','{broken'),('tool call','{"tool":"upload"}'),
                       ('extra coordinates',decision()[:-1]+',"x":9000}'),
                       ('duplicate keys',decision().replace('{','{"action":"COMPLETE",',1))]:
        def malformed(t=text):
            try: proposal_from_text(t,live())
            except InvalidRequest: return
            raise AssertionError()
        check('Model response: '+name, ['safety'], malformed, True)
    for target, action in [('missing','HIGHLIGHT'),(None,'COMPLETE')]:
        check('No invented target or unproved completion: '+action, ['navigation_accuracy','completion_detection','safety'],
              lambda t=target,a=action: require(validate_live(live(),proposal_from_text(decision(t,a),live())) is not None),True)
    for field, value in [('session_id','older'),('screen_revision',0),('window_id',2),('package_name','other.app')]:
        check('Late response identity mismatch: '+field,['adaptability','safety'],lambda f=field,v=value: require(validate_live(live(),replace(Provider('x').propose(live(),None),**{f:v}))=='stale'),True)
    for code in [400,401,403,404,408,409,429,500,502,503]:
        def http_error(c=code):
            def fail(*args): raise urllib.error.HTTPError('https://fixture.invalid',c,'synthetic',{},None)
            provider = RestProvider('gemini','synthetic','fixture',fail)
            gateway = Gateway([provider, Provider('groq')],mode='dual_ai',timeout=.3)
            try:
                result = gateway.decide(live(),primary_navigation=True)
                require(result.get('decision_policy')=='fallback')
                require(provider.diagnostics()['http_received'])
                return {'provider_calls':sum(gateway.calls.values()),'fallback_used':True,'http_status':c}
            finally: gateway.close()
        check('HTTP failure fallback '+str(code),['error_recovery','safety'],http_error)
    for name, error in [('DNS',urllib.error.URLError('synthetic DNS')),('offline',ConnectionError()),
                        ('TLS',urllib.error.URLError('synthetic TLS')),('timeout',TimeoutError())]:
        def network(e=error):
            def fail(*args): raise e
            gateway=Gateway([RestProvider('gemini','synthetic','fixture',fail),Provider('groq')],mode='dual_ai',timeout=.3)
            try: require(gateway.decide(live(),primary_navigation=True).get('decision_policy')=='fallback')
            finally: gateway.close()
        check('Network fallback '+name,['error_recovery'],network)
    def race():
        ready=threading.Event()
        def respond(s,stop):
            if s.request_id=='A': ready.set(); stop.wait(2)
            return Provider('x').propose(s,stop)
        gateway=Gateway([Provider('gemini',respond),Provider('groq')],mode='dual_ai',timeout=1)
        old=[]
        try:
            thread=threading.Thread(target=lambda:old.append(gateway.decide(live(request_id='A'),primary_navigation=True)))
            thread.start(); require(ready.wait(1))
            new=gateway.decide(live(request_id='B',screen_revision=2,package_name='fixture.second'),primary_navigation=True)
            thread.join(2)
            require(not thread.is_alive() and old[0]['status']=='rejected' and new['status']=='accepted')
            require(not gateway.active)
            return {'old_response':old[0]['reason'],'new_response':new['status']}
        finally: gateway.close()
    check('Task A cannot present after Task B replaces its screen',['adaptability','safety'],race,True)
    def replay():
        gateway=Gateway([Provider('gemini'),Provider('groq')],mode='dual_ai')
        try:
            for _ in range(500): gateway.decide(live(),primary_navigation=True)
            require(sum(gateway.calls.values())==1)
            return {'requests_received':500,'provider_calls':1,'replays_rejected':499}
        finally: gateway.close()
    check('Identical revision replay storm',['safety','error_recovery'],replay,True)
    def changed_revisions():
        gateway=Gateway([Provider('gemini'),Provider('groq')],mode='dual_ai',global_limit=12,provider_limit=6)
        try:
            for n in range(500): gateway.decide(live(request_id=str(n),screen_revision=n+1),primary_navigation=True)
            count=sum(gateway.calls.values()); require(count<=12)
            return {'requests_received':500,'provider_calls':count,'limit':12,
                    'limitation':'Checks quota cap, not accessibility coalescing or semantic request deduplication.'}
        finally: gateway.close()
    check('Changing revision storm cannot exceed configured quota',['safety'],changed_revisions,True)
    def long_run():
        gateway=Gateway([Provider('gemini'),Provider('groq')],mode='dual_ai',global_limit=1000,provider_limit=1000)
        durations=[]
        try:
            for n in range(300):
                start=time.perf_counter()
                r=gateway.decide(live(request_id=str(n),screen_revision=n+1),primary_navigation=True)
                require(r['status']=='accepted'); durations.append((time.perf_counter()-start)*1000)
                require(not gateway.active)
            return {'transitions':300,'retained_sessions':len(gateway.sessions),'active_after_each':0,
                    'first_50_mean_ms':sum(durations[:50])/50,'last_50_mean_ms':sum(durations[-50:])/50,
                    'scope':'Synthetic fast providers; no Android memory or real-model latency measured'}
        finally: gateway.close()
    check('Long session state stays bounded',['adaptability','safety'],long_run)
    cases=catalog()
    return dict(schema_version=1, generated_at=time.strftime('%Y-%m-%dT%H:%M:%SZ',time.gmtime()),
                provenance='Executed production backend code; synthetic providers and transport; zero external calls',
                score_policy='0=failed assertion, 5=passed scoped assertion, null=unmeasured; total /55 only for fully measured workflows. Critical failures override all scores.',
                summary={state:sum(r['status']==state for r in results) for state in ['PASS','FAIL']},
                probes=results, scenarios=[dict(id=c['id'],status='BLOCKED',scores=score([],False),total_out_of_55=None,reason=c['reason']) for c in cases])

if __name__=='__main__':
    parser=argparse.ArgumentParser(); parser.add_argument('--output',default='evaluation/results/offline.json'); args=parser.parse_args()
    result=run(); Path(args.output).write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps(result['summary'])); raise SystemExit(bool(result['summary']['FAIL']))
