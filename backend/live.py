"""Minimal, separately consented navigation data. Never accepts images, audio, fields or coordinates."""
from dataclasses import dataclass
import re
import time

from backend.gateway import InvalidRequest, Proposal

PRIVATE = re.compile(r"(?i)(?<!\w)(pin|otp|password|cvv|mpin|passcode|security code|recovery code)(?!\w)|पासवर्ड|पिन|ओटीपी|सीवीवी|(?<!\w)\d{4,}(?!\w)")
CONSEQUENTIAL = re.compile(r"(?i)\b(pay|purchase|buy|send|transfer|delete|remove|confirm|submit|install|allow|approve|accept|agree|reset|erase)\b|भुगतान|भेज|मिटा|स्वीकार|अनुमति")


def safe_text(value, limit):
    return isinstance(value, str) and 1 <= len(value.strip()) <= limit and not PRIVATE.search(value) and not any(ord(c) < 32 for c in value)


@dataclass(frozen=True)
class LiveSnapshot:
    request_id: str
    session_id: str
    screen_revision: int
    observed_at_ms: int
    package_name: str
    window_id: int
    locale: str
    goal: str
    controls: tuple
    previous_steps: tuple

    @property
    def task(self):
        return "LIVE_NAVIGATION"

    @property
    def eligible_node_ids(self):
        return frozenset(c["id"] for c in self.controls)

    @classmethod
    def parse(cls, data, now_ms=None):
        if not isinstance(data, dict) or set(data) != set(cls.__dataclass_fields__):
            raise InvalidRequest("Invalid live request")
        for name in ("request_id", "session_id"):
            if not isinstance(data[name], str) or not re.fullmatch(r"[A-Za-z0-9_-]{1,64}", data[name]):
                raise InvalidRequest("Invalid identity")
        for name in ("screen_revision", "observed_at_ms", "window_id"):
            if type(data[name]) is not int or not 0 <= data[name] <= 2**63 - 1:
                raise InvalidRequest("Invalid metadata")
        package = data["package_name"]
        if not isinstance(package, str) or not re.fullmatch(r"[A-Za-z0-9_.]{1,150}", package) or package in ("android", "com.android.systemui", "com.saathi") or any(x in package for x in ("permissioncontroller", "packageinstaller")):
            raise InvalidRequest("Protected surface")
        now = int(time.time() * 1000) if now_ms is None else now_ms
        if not 0 <= now - data["observed_at_ms"] <= 15_000 or data["locale"] not in ("en-IN", "hi-IN", "hinglish"):
            raise InvalidRequest("Expired observation or locale")
        if not safe_text(data["goal"], 160) or CONSEQUENTIAL.search(data["goal"]):
            raise InvalidRequest("Unsupported goal")
        controls = data["controls"]
        if not isinstance(controls, list) or not 1 <= len(controls) <= 32:
            raise InvalidRequest("Invalid controls")
        for control in controls:
            if not isinstance(control, dict) or set(control) != {"id", "label"} or not re.fullmatch(r"n[0-9]{1,3}", str(control["id"])) or not safe_text(control["label"], 80) or CONSEQUENTIAL.search(control["label"]):
                raise InvalidRequest("Private or consequential control")
        if len({c["id"] for c in controls}) != len(controls):
            raise InvalidRequest("Duplicate controls")
        previous = data["previous_steps"]
        if not isinstance(previous, list) or len(previous) > 3 or any(not safe_text(p, 80) or CONSEQUENTIAL.search(p) for p in previous):
            raise InvalidRequest("Invalid history")
        # Copy input: neither provider sees mutable request dictionaries or the other's answer.
        from types import MappingProxyType
        return cls(**{**data, "controls": tuple(MappingProxyType(dict(c)) for c in controls), "previous_steps": tuple(previous)})


def validate_live(snapshot, proposal):
    if not isinstance(proposal, Proposal) or type(proposal.screen_revision) is not int or type(proposal.window_id) is not int:
        return "malformed"
    if (proposal.session_id, proposal.screen_revision, proposal.package_name, proposal.window_id) != (snapshot.session_id, snapshot.screen_revision, snapshot.package_name, snapshot.window_id):
        return "stale"
    if not safe_text(proposal.explanation, 240) or not safe_text(proposal.expected_outcome, 240) or proposal.uncertainty or proposal.completion_evidence:
        return "uncertain"
    if proposal.action == "HANDOVER" and proposal.target_id is None:
        return None
    if proposal.action == "HIGHLIGHT" and proposal.target_id in snapshot.eligible_node_ids:
        return None
    return "invalid_target"
