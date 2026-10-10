"""Conservative checks for explicit source relations, never service-specific rules.

These catch supported grammatical forms; they are not semantic entailment or
complete natural-language understanding. Unknown wording still requires review.
"""
import re

_LABEL = r'[“\"]([^”\"\n]{1,80})[”\"]'
_ORDER = (
    re.compile(r'after\s+(?:reading|opening|completing)\s+'+_LABEL+r'\s*,?\s*(?:select|choose|open)\s+'+_LABEL,re.I),
    re.compile(_LABEL+r'\s*(?:पढ़ने|खोलने|पूरा करने)\s+के बाद\s*'+_LABEL+r'\s*(?:चुनें|खोलें)'),
    re.compile(_LABEL+r'\s*(?:padhne|kholne|poora karne)\s+ke baad\s*'+_LABEL+r'\s*(?:chunein|kholein)',re.I),
)

def missing_explicit_order(proposal,evidence):
    # Labels are not global identities. Keep evidence provenance and retain all
    # candidates so duplicate labels cannot silently overwrite an earlier step.
    selected={}
    for step in proposal.steps:
        if step.navigation is not None and step.navigation.kind=='READ_OPTION':
            selected.setdefault((step.evidence_id,step.navigation.label),[]).append(step)
    by_id={s.id:s for s in proposal.steps}
    def ancestors(step):
        seen=set();pending=list(step.depends_on)
        while pending:
            key=pending.pop()
            if key not in seen:
                seen.add(key)
                if key in by_id:pending.extend(by_id[key].depends_on)
        return seen
    for source in evidence:
        for pattern in _ORDER:
            for parent,child in pattern.findall(source.snippet):
                # A cited ordering instruction cannot evade the relationship check
                # by dropping every navigation annotation. Uncited source sections
                # do not impose unrelated steps on a narrower reviewed plan.
                cited = any(s.evidence_id == source.evidence_id and
                            (parent,child) in pattern.findall(s.quote) for s in proposal.steps)
                children=selected.get((source.evidence_id,child),[])
                if not children:
                    if cited:return True
                    continue
                parents=selected.get((source.evidence_id,parent),[])
                if len(children)!=1 or len(parents)!=1:return True
                if parents[0].id not in ancestors(children[0]):return True
    return False

_DENIALS = {
 'UNAUTHORISED_TRANSACTION': re.compile(r'no money (?:was |has been )?(?:transferred|debited|taken)|no unauthori[sz]ed transactions?|कोई पैसे नहीं कटे|कोई अनधिकृत लेनदेन नहीं|koi paise (?:nahi|nahin) kate|no money left my account',re.I),
 'ACCOUNT_ACCESS': re.compile(r'nobody accessed my account|no (?:unauthori[sz]ed |unauthori[sz]ed account )?access occurred|खाते में किसी ने प्रवेश नहीं किया|kisi ne account access (?:nahi|nahin) kiya',re.I),
}

def contradicts_explicit_denial(summary,signals):
    # Withhold contradictory hypotheses; do not infer that absence of these cues means safe.
    return any(signal in _DENIALS and _DENIALS[signal].search(summary) for signal in signals)
