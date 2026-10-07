"""Fixed diagnostic vocabulary. No arbitrary exception or provider text is logged."""

CODES = {
    'provider_timeout': 'PROVIDER_TIMEOUT', 'timeout': 'PROVIDER_TIMEOUT',
    'provider_unavailable': 'PROVIDER_UNAVAILABLE', 'provider_auth': 'PROVIDER_HTTP_ERROR',
    'provider_request': 'PROVIDER_HTTP_ERROR', 'provider_model': 'PROVIDER_HTTP_ERROR',
    'provider_rate_limited': 'PROVIDER_HTTP_ERROR', 'invalid_target': 'TARGET_NOT_FOUND',
    'uncertain': 'LOW_CONFIDENCE', 'stale': 'STALE_OBSERVATION',
    'unobserved_completion': 'COMPLETION_UNPROVEN', 'invalid_response': 'SCHEMA_INVALID',
    'malformed': 'SCHEMA_INVALID', 'evidence_missing': 'EVIDENCE_MISSING',
    'source_unverified': 'SOURCE_UNVERIFIED', 'safety_rejected': 'SAFETY_REJECTED',
    'dependency_cycle': 'DEPENDENCY_CYCLE', 'jurisdiction_mismatch': 'JURISDICTION_MISMATCH',
}


def schema_code(error):
    # Exact messages originate in our parser; unknown strings are never returned.
    message = str(error)
    if message in CODES: return CODES[message]
    if message == 'Invalid action': return 'UNSUPPORTED_ACTION'
    if message == 'Incomplete provider response': return 'OUTPUT_INCOMPLETE'
    if message in ('Unexpected provider action', 'Unexpected provider content'): return 'UNSUPPORTED_ACTION'
    return 'SCHEMA_INVALID'


def validation_code(snapshot, proposal, reason):
    if reason == 'uncertain' and proposal is not None:
        from backend.live import safe_text
        if hasattr(proposal, 'explanation') and (not safe_text(proposal.explanation, 240) or not safe_text(proposal.expected_outcome, 240)):
            return 'SAFETY_REJECTED'
        if getattr(proposal, 'completion_evidence', ()):
            return 'COMPLETION_UNPROVEN'
    return 'ACCEPTED' if reason is None else CODES.get(reason, 'SCHEMA_INVALID')
