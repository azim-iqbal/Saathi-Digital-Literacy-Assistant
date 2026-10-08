"""Explicit test harness: deterministic fake replies on live schema; NEVER a real AI benchmark."""
from pathlib import Path
import os
import secrets
from backend.gateway import Gateway, Proposal
from backend.server import make_server


class FixtureProvider:
    model = "synthetic-device-test"
    def __init__(self, name): self.id = name
    def propose(self, s, cancelled):
        if cancelled.is_set(): raise InterruptedError()
        from backend.planning import ResearchSnapshot, PlanProposal, Step, Criterion
        from backend.navigation_hint import NavigationHint
        if isinstance(s, ResearchSnapshot):
            eid = s.evidence[0].evidence_id
            if s.incident_mode:
                from backend.incident_research import IncidentHypothesis, Citation
                return IncidentHypothesis('REGIONAL',(Citation(eid,'A regional interruption is affecting this service.'),))
            return PlanProposal((Step('s1', 'Application', ('s2',), eid, 'Application requires registration.', 'USER_CONFIRMATION'),
                Step('s2', 'Registration', ('s3',), eid, 'Registration requires verification.', 'USER_CONFIRMATION'),
                Step('s3', 'Verification', (), eid, 'Registration requires verification. Select “Requirements”.', 'USER_CONFIRMATION', NavigationHint('READ_OPTION','Requirements'))),
                (Criterion('c1', eid, 'Applicants must meet the local residency condition.'),))
        from backend.incident import IncidentSnapshot, IncidentProposal
        if isinstance(s, IncidentSnapshot):
            return IncidentProposal("POSSIBLE_FINANCIAL", ("DECEPTIVE_REQUEST",))
        target = next((c["id"] for name in ("Help", "Back to choices") for c in s.controls if c["label"] == name), None)
        return Proposal(s.session_id, s.screen_revision, s.package_name, s.window_id,
                        "HIGHLIGHT" if target else "HANDOVER", target, "Fixture next step.", "Observe a new fixture screen.")


if __name__ == "__main__":
    # Reuse the ephemeral token from the host-only test preparation, never commit it.
    token_file = Path("/tmp/saathi-gateway-test-token")
    if not token_file.exists():
        fd = os.open(token_file, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
        with os.fdopen(fd, "w") as stream: stream.write(secrets.token_urlsafe(32))
    gateway = Gateway([FixtureProvider("gemini"), FixtureProvider("groq")], mode="dual_ai", global_limit=500, provider_limit=250)
    from backend.research import ResearchService, RegistryRetriever, SourceRegistry, Source
    registry = SourceRegistry([Source('https://fixture.example.test/', 'Synthetic requirements', 'official', 'Region A', 'synthetic test review')])
    gateway.research = ResearchService(RegistryRetriever(registry, lambda *args:
        '<p>Application requires registration. Registration requires verification. Select “Requirements”. Applicants must meet the local residency condition. A regional interruption is affecting this service.</p>'))
    server = make_server(token_file.read_text(), gateway, port=8767)
    print("Synthetic AI protocol fixture at loopback:8767; no external calls", flush=True)
    try: server.serve_forever()
    finally: server.server_close(); gateway.close()
