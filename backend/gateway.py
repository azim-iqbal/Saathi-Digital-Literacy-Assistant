"""Bounded paired-proposal coordination; adapters own provider transport and credentials."""
from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass
import re
import threading
import time


PREFIX = "com.saathi:id/"
PUBLIC_IDS = frozenset(PREFIX + name for name in (
    "recharge_bills", "electricity_biller", "water_biller", "dth_biller",
    "pay_button", "practice_back", "practice_detour", "success_title",
    "practice_electricity", "practice_water", "practice_dth",
))
TASKS = {"ELECTRICITY_BILL": "electricity", "WATER_BILL": "water", "DTH_RECHARGE": "dth"}


class InvalidRequest(ValueError):
    pass


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
                 max_sessions=128, failure_limit=3, mode="mock", budget=None):
        self.mode, self.budget = mode, budget
        self.providers = tuple(providers or (MockProvider("mock-a"), MockProvider("mock-b")))
        if len(self.providers) != 2 or len({p.id for p in self.providers}) != 2:
            raise ValueError("Two distinct provider identities required")
        if timeout <= 0 or min(global_limit, provider_limit, max_sessions, failure_limit) < 1:
            raise ValueError("Positive bounds required")
        self.timeout, self.global_limit, self.provider_limit = timeout, global_limit, provider_limit
        self.max_sessions, self.failure_limit = max_sessions, failure_limit
        self.lock = threading.Lock()
        self.pool = ThreadPoolExecutor(max_workers=4, thread_name_prefix="saathi-provider")
        self.slots = threading.BoundedSemaphore(4)
        self.sessions, self.active = {}, {}
        self.calls = {p.id: 0 for p in self.providers}
        self.failures = {p.id: 0 for p in self.providers}
        self.closed = False

    def rejected(self, reason):
        return {"status": "rejected", "reason": reason, "mode": self.mode}

    def cancel(self, request_id):
        with self.lock:
            event = self.active.get(request_id)
            if event:
                event.set()
            return bool(event)

    def _call(self, provider, snapshot, cancelled):
        try:
            return provider.propose(snapshot, cancelled)
        finally:
            self.slots.release()

    def decide(self, snapshot):
        deadline = time.monotonic() + self.timeout
        event = threading.Event()
        with self.lock:
            if self.closed:
                return self.rejected("unavailable")
            if snapshot.request_id in self.active:
                return self.rejected("duplicate_request")
            previous = self.sessions.get(snapshot.session_id)
            if previous and snapshot.screen_revision <= previous[0]:
                return self.rejected("stale")
            if not previous and len(self.sessions) >= self.max_sessions:
                return self.rejected("session_capacity")
            # A new observation immediately invalidates the old decision, even when budget is exhausted.
            if previous:
                previous[1].set()
            self.sessions[snapshot.session_id] = (snapshot.screen_revision, event)
            if any(n >= self.failure_limit for n in self.failures.values()):
                return self.rejected("circuit_open")
            if sum(self.calls.values()) + 2 > self.global_limit or any(n >= self.provider_limit for n in self.calls.values()):
                return self.rejected("quota_exhausted")
            acquired = 0
            for _ in self.providers:
                if not self.slots.acquire(blocking=False):
                    for _ in range(acquired):
                        self.slots.release()
                    return self.rejected("busy")
                acquired += 1
            try:
                reserved = self.budget is None or self.budget.reserve([p.id for p in self.providers])
            except Exception:
                for _ in range(acquired):
                    self.slots.release()
                return self.rejected("budget_unavailable")
            if not reserved:
                for _ in range(acquired):
                    self.slots.release()
                return self.rejected("quota_exhausted")
            self.active[snapshot.request_id] = event
            for provider in self.providers:
                self.calls[provider.id] += 1
            futures = [self.pool.submit(self._call, p, snapshot, event) for p in self.providers]
        try:
            while not all(f.done() for f in futures):
                if event.wait(min(.01, max(0, deadline - time.monotonic()))):
                    return self.rejected("cancelled")
                if time.monotonic() >= deadline:
                    with self.lock:
                        for p, f in zip(self.providers, futures):
                            if not f.done():
                                self.failures[p.id] += 1
                    return self.rejected("timeout")
            if time.monotonic() >= deadline:
                return self.rejected("timeout")
            proposals = []
            for provider, future in zip(self.providers, futures):
                try:
                    proposal = future.result()
                    from backend.live import LiveSnapshot, validate_live
                    error = validate_live(snapshot, proposal) if isinstance(snapshot, LiveSnapshot) else validate(snapshot, proposal)
                except Exception:
                    error, proposal = "provider_unavailable", None
                if error:
                    with self.lock:
                        self.failures[provider.id] += 1
                    return self.rejected(error)
                proposals.append(proposal)
            a, b = proposals
            if (a.action, a.target_id) != (b.action, b.target_id):
                return self.rejected("disagreement")
            with self.lock:
                if event.is_set() or self.sessions[snapshot.session_id][1] is not event:
                    return self.rejected("cancelled")
                if not 0 <= int(time.time() * 1000) - snapshot.observed_at_ms <= 15_000:
                    return self.rejected("stale")
                return {"status": "accepted", "mode": self.mode, "request_id": snapshot.request_id,
                        "session_id": snapshot.session_id, "screen_revision": snapshot.screen_revision,
                        "package_name": snapshot.package_name, "window_id": snapshot.window_id,
                        "action": a.action, "target_id": a.target_id, "explanation": a.explanation,
                        "expected_outcome": a.expected_outcome, "completion_evidence": list(a.completion_evidence),
                        "provenance": [{"provider": p.id, "model": p.model} for p in self.providers]}
        finally:
            event.set()
            with self.lock:
                self.active.pop(snapshot.request_id, None)

    def close(self):
        with self.lock:
            self.closed = True
            for event in self.active.values():
                event.set()
        self.pool.shutdown(wait=True)
        if self.budget is not None:
            self.budget.close()
