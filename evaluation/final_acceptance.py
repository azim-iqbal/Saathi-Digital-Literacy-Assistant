"""Explicit eight-call evaluation. Fixed fictional data, no retries, no response-body retention."""
import argparse
import json
import time
from pathlib import Path
from backend.configuration import configured_gateway
from backend.live import LiveSnapshot
from backend.planning import ResearchSnapshot, PlanProposal
from backend.research import ResearchEvidence
from backend.incident import IncidentSnapshot, IncidentProposal

SOURCE = ('First select “Overview”. After reading “Overview”, select “Requirements”. '
          'After reading “Requirements”, select “Checklist”. Eligibility requires residence in Fixture region.')

def case_snapshot(kind):
    now=int(time.time()*1000);identity='final-'+kind
    if kind.startswith('navigation'):
        return LiveSnapshot.parse(dict(request_id=identity,session_id=identity,screen_revision=1,observed_at_ms=now,
            package_name='org.saathi.fixture',window_id=1,locale='hinglish' if kind.endswith('hinglish') else 'en-IN',
            goal='Mujhe madad chahiye, kaunsa option kholun?' if kind.endswith('hinglish') else 'Where can I get assistance?',
            controls=[dict(id='n1',label='Catalog'),dict(id='n2',label='Help')],previous_steps=[]))
    if kind=='plan':
        evidence=ResearchEvidence('aaaaaaaaaaaaaaaaaaaaaaaa','https://fixture.example.test/','Fictional instructions',
            'official','synthetic source review',now,None,'Fixture region',SOURCE,'a'*64,'procedure')
        return ResearchSnapshot(identity,identity,1,now,'en-IN','Read the full sequence and explain eligibility requirements','Fixture region',(evidence,))
    return IncidentSnapshot.parse(dict(request_id=identity,session_id=identity,screen_revision=1,observed_at_ms=now,
        locale='hi-IN',summary='एक संदेश बैंक कर्मचारी होने का दावा करके पैसे भेजने को कह रहा है। मैंने पैसे नहीं भेजे हैं।',concern='UNSURE',consent=True))

def score(kind, proposal):
    # Expected outcomes remain outside the provider prompt. No service-specific production rules.
    if kind.startswith('navigation'):
        return dict(correct_target=getattr(proposal,'target_id',None)=='n2',
                    no_completion=getattr(proposal,'action',None)=='HIGHLIGHT' and not getattr(proposal,'completion_evidence',('invalid',)))
    if kind=='incident':
        return dict(cautious_financial=isinstance(proposal,IncidentProposal) and proposal.category=='POSSIBLE_FINANCIAL',
                    respects_no_transfer=isinstance(proposal,IncidentProposal) and set(proposal.signals)=={'DECEPTIVE_REQUEST'})
    if not isinstance(proposal,PlanProposal):return dict(type_valid=False)
    by_label={s.navigation.label:s for s in proposal.steps if s.navigation is not None}
    def edge(parent,child):
        return parent in by_label and child in by_label and by_label[parent].id in by_label[child].depends_on
    return dict(three_reading_steps=set(by_label)=={'Overview','Requirements','Checklist'},
                prerequisite_chain=edge('Overview','Requirements') and edge('Requirements','Checklist'),
                evidence_backed=all(s.quote in SOURCE and s.completion=='USER_CONFIRMATION' for s in proposal.steps),
                eligibility_requirement=any('residence in Fixture region' in c.quote for c in proposal.criteria))

class ScoredProvider:
    def __init__(self,provider,kind,rows):self.provider,self.kind,self.rows=provider,kind,rows
    def __getattr__(self,name):return getattr(self.provider,name)
    def propose(self,snapshot,stop):
        proposal=self.provider.propose(snapshot,stop)
        self.rows[self.id]=score(self.kind,proposal)
        return proposal

def run(output,factory=configured_gateway,*,suite='all'):
    if suite not in ('all','reasoning'): raise ValueError('Unknown evaluation suite')
    cases=('plan','incident') if suite=='reasoning' else ('navigation-en','navigation-hinglish','plan','incident')
    cap=2*len(cases)
    output=Path(output);output.mkdir(parents=True,exist_ok=True)
    with (output/'authorized-run.started').open('x') as marker:marker.write(f'At most {cap} authorized fictional provider calls. Suite: {suite}. No retries.\n')
    gateway=factory();gateway.global_limit=min(cap,gateway.global_limit);gateway.provider_limit=min(len(cases),gateway.provider_limit)
    providers=gateway.providers
    result=dict(maximum_provider_calls=cap,provider_reservations=0,cases=[],provenance='Genuine provider adapters; fictional evidence; paired validation, not Android E2E')
    try:
        if gateway.mode!='dual_ai' or any(not getattr(p,'configured',True) for p in providers):
            result['blocker']='not_configured';return result
        for kind in cases:
            scores={};gateway.providers=tuple(ScoredProvider(p,kind,scores) for p in providers)
            snapshot=case_snapshot(kind);start=time.monotonic();response=gateway.decide(snapshot)
            result['cases'].append(dict(case=kind,request_id=snapshot.request_id,elapsed_ms=int((time.monotonic()-start)*1000),
                accepted=response.get('status')=='accepted',reason=response.get('reason'),individual_scores=scores))
    finally:
        gateway.close()
        result['provider_reservations']=sum(gateway.calls.values())
        for row in result['cases']:
            row['providers']=[dict(provider=p.id,model=p.model,diagnostics=p.diagnostics(row['request_id'],include_network=True) if hasattr(p,'diagnostics') else None) for p in providers]
            row['status']='PASS' if row['accepted'] and len(row['individual_scores'])==2 and all(all(v.values()) for v in row['individual_scores'].values()) else 'FAIL'
        (output/'results.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
    return result

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--authorized',action='store_true');parser.add_argument('--output',required=True)
    parser.add_argument('--suite',choices=('all','reasoning'),default='all')
    args=parser.parse_args()
    if not args.authorized:raise SystemExit('New explicit authorization for the selected call cap required')
    result=run(args.output,suite=args.suite)
    print(json.dumps(dict(provider_reservations=result['provider_reservations'],cases=[dict(case=r['case'],status=r['status'],reason=r['reason']) for r in result['cases']],blocker=result.get('blocker'))))
