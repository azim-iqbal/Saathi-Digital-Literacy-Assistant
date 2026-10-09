"""Bounded paired-proposal coordination; adapters own provider transport and credentials."""
from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass
import re
import threading
import time
from backend.errors import ProviderFailure


PREFIX = "com.saathi:id/"
PUBLIC_IDS = frozenset(PREFIX + name for name in (
    "recharge_bills", "electricity_biller", "water_biller", "dth_biller",
    "pay_button", "practice_back", "practice_detour", "success_title",
    "practice_electricity", "practice_water", "practice_dth",
))
TASKS = {"ELECTRICITY_BILL": "electricity", "WATER_BILL": "water", "DTH_RECHARGE": "dth"}


class InvalidRequest(ValueError):
    pass


class RequestCancellation(threading.Event):
    """Cooperative stop signal with the shared monotonic decision deadline."""
    def __init__(self, deadline, parent=None):
        super().__init__()
        self.deadline = deadline
        self.parent = parent
        self.overall_deadline = parent.overall_deadline if parent else deadline

    def is_set(self):
        return super().is_set() or (self.parent is not None and self.parent.is_set())

    def wait(self, timeout=None):
        if self.parent is None:
            return super().wait(timeout)
        until = None if timeout is None else time.monotonic() + timeout
        while not self.is_set():
            remaining = .01 if until is None else min(.01, until - time.monotonic())
            if remaining <= 0: break
            super().wait(remaining)
        return self.is_set()


@dataclass(frozen=True)
class Snapshot:
    request_id: str
    session_id: str
    screen_revision: int
    observed_at_ms: int
    package_name: str
    window_id: int
    locale: str
    task: str
    eligible_node_ids: frozenset

    @classmethod
    def parse(cls, data, now_ms=None):
        if not isinstance(data, dict) or set(data) != set(cls.__dataclass_fields__):
            raise InvalidRequest("Unexpected or missing fields")
        for name in ("request_id", "session_id"):
            if not isinstance(data[name], str) or not re.fullmatch(r"[A-Za-z0-9_-]{1,64}", data[name]):
                raise InvalidRequest("Invalid identity")
        for name in ("screen_revision", "observed_at_ms", "window_id"):
            if type(data[name]) is not int or not 0 <= data[name] <= 2**63 - 1:
                raise InvalidRequest("Invalid screen metadata")
        if data["package_name"] != "com.saathi" or data["locale"] not in ("en-IN", "hi-IN", "hinglish"):
            raise InvalidRequest("Unsupported practice surface or locale")
        if not isinstance(data["task"], str) or data["task"] not in TASKS:
            raise InvalidRequest("Unsupported task")
        ids = data["eligible_node_ids"]
        if not isinstance(ids, list) or not 1 <= len(ids) <= len(PUBLIC_IDS):
            raise InvalidRequest("Invalid controls")
        if any(not isinstance(i, str) or i not in PUBLIC_IDS for i in ids) or len(set(ids)) != len(ids):
            raise InvalidRequest("Unknown, private or duplicate controls")
        current = int(time.time() * 1000) if now_ms is None else now_ms
        if not 0 <= current - data["observed_at_ms"] <= 15_000:
            raise InvalidRequest("Expired observation")
        return cls(**{**data, "eligible_node_ids": frozenset(ids)})


@dataclass(frozen=True)
class Proposal:
    session_id: str
    screen_revision: int
    package_name: str
    window_id: int
    action: str
    target_id: str | None
    explanation: str
    expected_outcome: str
    uncertainty: tuple = ()
    completion_evidence: tuple = ()


class MockProvider:
    """Two separate instances receive only the same immutable request, never each other's answer."""
    def __init__(self, name):
        self.id = name
        self.model = "deterministic-fixture-v1"

    def propose(self, snapshot, cancelled):
        if cancelled.is_set():
            raise InterruptedError()
        ids = snapshot.eligible_node_ids
        category = TASKS[snapshot.task]
        desired = PREFIX + "practice_" + category
        actual = ids & {PREFIX + "practice_" + value for value in TASKS.values()}
        action, target, evidence = "HANDOVER", None, ()
        text, outcome = "Continue with local practice guidance.", "The local guide checks the next practice screen."
        if PREFIX + "practice_detour" in ids or (actual and actual != {desired}):
            if PREFIX + "practice_back" in ids:
                action, target = "HIGHLIGHT", PREFIX + "practice_back"
                text, outcome = "Return to the practice choices.", "The choices screen appears."
        elif PREFIX + "success_title" in ids and desired in ids:
            action, evidence = "COMPLETE", (PREFIX + "success_title", desired)
            text, outcome = "Practice complete. No money moved.", "The matching synthetic completion is visible."
        else:
            for name in ("recharge_bills", category + "_biller"):
                if PREFIX + name in ids:
                    action, target = "HIGHLIGHT", PREFIX + name
                    text, outcome = "Open the selected practice option.", "The selected practice screen appears."
                    break
        return Proposal(snapshot.session_id, snapshot.screen_revision, snapshot.package_name,
                        snapshot.window_id, action, target, text, outcome, completion_evidence=evidence)


def validate(snapshot, proposal):
    if not isinstance(proposal, Proposal):
        return "malformed"
    if type(proposal.screen_revision) is not int or type(proposal.window_id) is not int:
        return "malformed"
    if (proposal.session_id, proposal.screen_revision, proposal.package_name, proposal.window_id) != (
            snapshot.session_id, snapshot.screen_revision, snapshot.package_name, snapshot.window_id):
        return "stale"
    if any(not isinstance(text, str) or not text.strip() or len(text) > 240
           for text in (proposal.explanation, proposal.expected_outcome)):
        return "malformed"
    if not isinstance(proposal.uncertainty, tuple) or proposal.uncertainty:
        return "uncertain"
    if not isinstance(proposal.completion_evidence, tuple):
        return "malformed"
    if proposal.action == "HIGHLIGHT":
        if not isinstance(proposal.target_id, str) or proposal.target_id not in snapshot.eligible_node_ids:
            return "invalid_target"
        if proposal.target_id.rsplit("/", 1)[-1] not in (
                "recharge_bills", "electricity_biller", "water_biller", "dth_biller", "practice_back", "pay_button"):
            return "invalid_target"
    elif proposal.action in ("HANDOVER", "COMPLETE"):
        if proposal.target_id is not None:
            return "invalid_target"
    else:
        return "malformed"
    if proposal.action == "COMPLETE":
        expected = {PREFIX + "success_title", PREFIX + "practice_" + TASKS[snapshot.task]}
        if any(not isinstance(i, str) for i in proposal.completion_evidence):
            return "malformed"
        categories = snapshot.eligible_node_ids & {PREFIX + "practice_" + value for value in TASKS.values()}
        if (set(proposal.completion_evidence) != expected or not expected <= snapshot.eligible_node_ids
                or categories != {PREFIX + "practice_" + TASKS[snapshot.task]}):
            return "unobserved_completion"
    elif proposal.completion_evidence:
        return "malformed"
    return None


class Gateway:
    """Paired decisions with process limits and optional durable aggregate call reservations."""
    def __init__(self, providers=None, timeout=2.0, global_limit=100, provider_limit=50,
                 max_sessions=128, failure_limit=3, mode="mock", budget=None, session_ttl=300,
                 executor=None, slots=None):
        self.mode, self.budget = mode, budget
        self.providers = tuple(providers or (MockProvider("mock-a"), MockProvider("mock-b")))
        if len(self.providers) != 2 or len({p.id for p in self.providers}) != 2:
            raise ValueError("Two distinct provider identities required")
        if timeout <= 0 or min(global_limit, provider_limit, max_sessions, failure_limit) < 1:
            raise ValueError("Positive bounds required")
        if session_ttl < 30:
            raise ValueError("Session retention must exceed the observation freshness window")
        self.timeout, self.global_limit, self.provider_limit = timeout, global_limit, provider_limit
        self.max_sessions, self.failure_limit = max_sessions, failure_limit
        self.lock = threading.Lock()
        self.owns_pool = executor is None
        self.pool = executor or ThreadPoolExecutor(max_workers=4, thread_name_prefix="saathi-provider")
        self.slots = slots or threading.BoundedSemaphore(4)
        self.sessions, self.active, self.cancelled_requests = {}, {}, {}
        self.session_seen, self.session_ttl = {}, session_ttl
        self.calls = {p.id: 0 for p in self.providers}
        self.failures = {p.id: 0 for p in self.providers}
        self.circuit_until, self.circuit_probes = {}, {}
        self.closed = False
        self.research = None

    def rejected(self, reason):
        return {"status": "rejected", "reason": reason, "mode": self.mode}

    def cancel(self, request_id):
        with self.lock:
            now = time.monotonic()
            self.cancelled_requests = {key: until for key, until in self.cancelled_requests.items() if until > now}
            if len(self.cancelled_requests) >= 256:
                del self.cancelled_requests[next(iter(self.cancelled_requests))]
            self.cancelled_requests[request_id] = now + 60
            event = self.active.get(request_id)
            if event:
                event.set()
            return bool(event)

    def _call(self, provider, snapshot, cancelled):
        try:
            if cancelled.is_set():
                raise InterruptedError()
            if time.monotonic() >= cancelled.deadline:
                raise TimeoutError("Decision expired before provider dispatch")
            return provider.propose(snapshot, cancelled)
        finally:
            self.slots.release()

    def _launch(self, providers, snapshot, event, deadline_reason):
        """Reserve only the calls about to run, within the original request deadline."""
        with self.lock:
            if event.is_set():
                return [], "cancelled"
            if self.closed:
                return [], "unavailable"
            if time.monotonic() >= event.deadline:
                return [], deadline_reason
            if any(not getattr(p, "configured", True) for p in providers):
                return [], "not_configured"
            now = time.monotonic()
            recovering = [p.id for p in providers if self.failures[p.id] >= self.failure_limit]
            for name in recovering:
                until = self.circuit_until.setdefault(name, now + 30)
                if now < until or name in self.circuit_probes:
                    return [], "circuit_open"
            if sum(self.calls.values()) + len(providers) > self.global_limit or any(self.calls[p.id] >= self.provider_limit for p in providers):
                return [], "quota_exhausted"
            acquired = 0
            for _ in providers:
                if not self.slots.acquire(blocking=False):
                    for _ in range(acquired): self.slots.release()
                    return [], "busy"
                acquired += 1
            try:
                reserved = self.budget is None or self.budget.reserve([p.id for p in providers])
            except Exception:
                for _ in range(acquired): self.slots.release()
                return [], "budget_unavailable"
            if not reserved:
                for _ in range(acquired): self.slots.release()
                return [], "quota_exhausted"
            if time.monotonic() >= event.deadline or not 0 <= int(time.time()*1000) - snapshot.observed_at_ms <= 15_000:
                for _ in range(acquired): self.slots.release()
                return [], deadline_reason if time.monotonic() >= event.deadline else "stale"
            futures = []
            try:
                for name in recovering:
                    self.circuit_probes[name] = event
                for provider in providers:
                    futures.append(self.pool.submit(self._call, provider, snapshot, event))
                    self.calls[provider.id] += 1
            except Exception:
                event.set()
                for _ in range(acquired - len(futures)): self.slots.release()
                return [], "provider_unavailable"
            return futures, None

    def _failed(self, name):
        """Called under lock. Consecutive upstream failures pause new calls briefly."""
        self.failures[name] += 1
        if self.failures[name] >= self.failure_limit:
            self.circuit_until[name] = time.monotonic() + 30

    def _healthy(self, name):
        self.failures[name] = 0
        self.circuit_until.pop(name, None)

    def _collect(self, providers, futures, snapshot, event, deadline_reason):
        while not all(f.done() for f in futures):
            if event.wait(min(.01, max(0, event.deadline - time.monotonic()))):
                return [], ["cancelled"]
            if time.monotonic() >= event.deadline:
                if deadline_reason in ("timeout", "provider_timeout"):
                    with self.lock:
                        for p, f in zip(providers, futures):
                            if not f.done(): self._failed(p.id)
                return [], [deadline_reason]
        if event.is_set(): return [], ["cancelled"]
        if time.monotonic() >= event.deadline: return [], [deadline_reason]
        proposals, errors = [], []
        for provider, future in zip(providers, futures):
            try:
                proposal = future.result()
                from backend.live import LiveSnapshot, validate_live
                from backend.incident import IncidentSnapshot, validate_assessment
                from backend.planning import ResearchSnapshot, validate_plan
                error = (validate_plan(snapshot, proposal) if isinstance(snapshot, ResearchSnapshot) else
                         validate_assessment(snapshot, proposal) if isinstance(snapshot, IncidentSnapshot) else
                         validate_live(snapshot, proposal) if isinstance(snapshot, LiveSnapshot) else validate(snapshot, proposal))
            except ProviderFailure as failure:
                error, proposal = failure.reason, None
            except InvalidRequest as invalid:
                from backend.planning import PlanValidationError
                error, proposal = invalid.reason if isinstance(invalid, PlanValidationError) else "invalid_response", None
            except Exception:
                error, proposal = "provider_unavailable", None
            if hasattr(provider, "record_validation"):
                provider.record_validation(snapshot, proposal, error)
            if error:
                with self.lock:
                    if event.is_set(): return [], ["cancelled"]
                    # Valid uncertainty is a task outcome, not an upstream outage.
                    if error in ("uncertain", "invalid_target", "unobserved_completion", "stale", "evidence_missing", "source_unverified", "jurisdiction_mismatch", "dependency_cycle", "safety_rejected"):
                        self._healthy(provider.id)
                    else:
                        self._failed(provider.id)
                errors.append(error)
            else:
                with self.lock:
                    if event.is_set(): return [], ["cancelled"]
                    self._healthy(provider.id)
                proposals.append(proposal)
        return proposals, errors

    def _accepted(self, snapshot, proposal, providers, event, deadline_reason, policy=None):
        with self.lock:
            if event.is_set() or self.sessions[snapshot.session_id][1] is not event:
                return self.rejected("cancelled")
            if time.monotonic() >= event.deadline: return self.rejected(deadline_reason)
            if not 0 <= int(time.time()*1000) - snapshot.observed_at_ms <= 15_000:
                return self.rejected("stale")
            result = {"status": "accepted", "mode": self.mode, "request_id": snapshot.request_id,
                      "session_id": snapshot.session_id, "screen_revision": snapshot.screen_revision,
                      "provenance": [{"provider": p.id, "model": p.model} for p in providers]}
            from backend.incident import IncidentSnapshot
            from backend.planning import ResearchSnapshot, plan_result
            if isinstance(snapshot, ResearchSnapshot):
                result.update(plan=plan_result(snapshot, proposal))
            elif isinstance(snapshot, IncidentSnapshot):
                result.update(category=proposal.category, signals=sorted(proposal.signals))
            else:
                result.update(package_name=snapshot.package_name, window_id=snapshot.window_id,
                              action=proposal.action, target_id=proposal.target_id, explanation=proposal.explanation,
                              expected_outcome=proposal.expected_outcome, completion_evidence=list(proposal.completion_evidence))
            if policy is not None: result["decision_policy"] = policy
            return result

    def decide(self, snapshot, primary_navigation=False):
        from backend.live import LiveSnapshot
        if primary_navigation and not isinstance(snapshot, LiveSnapshot):
            raise InvalidRequest("Primary policy is limited to non-sensitive navigation")
        event = RequestCancellation(time.monotonic() + self.timeout)
        deadline_reason = "timeout"
        with self.lock:
            if not primary_navigation and any(not getattr(p, "configured", True) for p in self.providers):
                return self.rejected("not_configured")
            if self.closed: return self.rejected("unavailable")
            age_ms = int(time.time()*1000) - snapshot.observed_at_ms
            if not 0 <= age_ms < 15_000: return self.rejected("stale")
            now = time.monotonic()
            freshness_deadline = now + (15_000 - age_ms) / 1000
            if freshness_deadline <= event.deadline:
                event.deadline, deadline_reason = freshness_deadline, "stale"
            self.cancelled_requests = {key: until for key, until in self.cancelled_requests.items() if until > now}
            if snapshot.request_id in self.cancelled_requests: return self.rejected("cancelled")
            if now >= event.deadline: return self.rejected(deadline_reason)
            for session_id, seen in list(self.session_seen.items()):
                retained = self.sessions[session_id][1]
                if now - seen >= self.session_ttl and retained not in self.active.values():
                    del self.sessions[session_id]
                    del self.session_seen[session_id]
            if snapshot.request_id in self.active: return self.rejected("duplicate_request")
            previous = self.sessions.get(snapshot.session_id)
            if previous and snapshot.screen_revision <= previous[0]: return self.rejected("stale")
            if not previous and len(self.sessions) >= self.max_sessions: return self.rejected("session_capacity")
            if previous: previous[1].set()
            self.sessions[snapshot.session_id] = (snapshot.screen_revision, event)
            self.session_seen[snapshot.session_id] = now
            self.active[snapshot.request_id] = event
        try:
            if primary_navigation:
                # Safe eligible navigation only. An explicit HANDOVER represents an
                # unresolved primary answer; no numeric confidence is invented.
                recoverable = {"not_configured", "circuit_open", "provider_auth", "provider_model", "provider_request",
                               "provider_rate_limited", "provider_unavailable", "provider_timeout", "invalid_response",
                               "malformed", "uncertain", "invalid_target"}
                for index, provider in enumerate(self.providers):
                    # Navigation must retain time for a conditional fallback inside the
                    # original decision/freshness window. Never extend the screen's life.
                    now = time.monotonic()
                    if event.is_set(): return self.rejected("cancelled")
                    if now >= event.deadline: return self.rejected(deadline_reason)
                    attempt = RequestCancellation(now + min(4.0, (event.deadline - now) * .6), event) if index == 0 else event
                    attempt_reason = "provider_timeout" if index == 0 else deadline_reason
                    try:
                        futures, error = self._launch((provider,), snapshot, attempt, attempt_reason)
                        proposals, errors = ([], [error]) if error else self._collect((provider,), futures, snapshot, attempt, attempt_reason)
                    finally:
                        if attempt is not event:
                            attempt.set()
                            with self.lock:
                                for name, probe in list(self.circuit_probes.items()):
                                    if probe is attempt: del self.circuit_probes[name]
                    if errors:
                        if index == 0 and errors[0] in recoverable: continue
                        return self.rejected(errors[0])
                    proposal = proposals[0]
                    if index == 0 and proposal.action == "HANDOVER": continue
                    return self._accepted(snapshot, proposal, (provider,), event, deadline_reason,
                                          "primary" if index == 0 else "fallback")
            else:
                futures, error = self._launch(self.providers, snapshot, event, deadline_reason)
                if error: return self.rejected(error)
                proposals, errors = self._collect(self.providers, futures, snapshot, event, deadline_reason)
                if errors: return self.rejected(errors[0])
                a, b = proposals
                from backend.incident import IncidentSnapshot
                from backend.planning import ResearchSnapshot
                planning = isinstance(snapshot, ResearchSnapshot)
                if planning:
                    if a.agreement_key() != b.agreement_key(): return self.rejected("disagreement")
                    return self._accepted(snapshot, a, self.providers, event, deadline_reason)
                incident = isinstance(snapshot, IncidentSnapshot)
                if (incident and (a.category, set(a.signals)) != (b.category, set(b.signals))) or (
                        not incident and (a.action, a.target_id) != (b.action, b.target_id)):
                    return self.rejected("disagreement")
                return self._accepted(snapshot, a, self.providers, event, deadline_reason)
        finally:
            event.set()
            with self.lock:
                self.active.pop(snapshot.request_id, None)
                for name, probe in list(self.circuit_probes.items()):
                    if probe is event:
                        del self.circuit_probes[name]

    def close(self):
        with self.lock:
            self.closed = True
            for event in self.active.values():
                event.set()
        if self.owns_pool:
            self.pool.shutdown(wait=True)
        if self.budget is not None:
            self.budget.close()
