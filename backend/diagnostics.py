"""Authenticated connection evidence. No prompt, key, response text or incident is recorded."""
import re
import time
from backend.gateway import InvalidRequest, Snapshot, PREFIX


def status(gateway, request_id=None, reason="not_requested"):
    with gateway.lock:
        counts = dict(gateway.calls)
    providers = []
    for provider in gateway.providers:
        record = provider.diagnostics(request_id) if hasattr(provider, "diagnostics") else None
        providers.append(dict(provider=provider.id, model=provider.model, attempts=counts[provider.id],
                              configured=getattr(provider, "configured", True), last=record))
    return dict(status="connection", mode=gateway.mode, request_id=request_id, reason=reason, providers=providers)


def check(gateway, data):
    if (not isinstance(data, dict) or set(data) != {"request_id", "consent"} or data["consent"] is not True
            or not isinstance(data["request_id"], str) or not re.fullmatch(r"[A-Za-z0-9_-]{1,64}", data["request_id"])):
        raise InvalidRequest("Explicit probe consent required")
    request_id = data["request_id"]
    if gateway.mode != "dual_ai":
        return status(gateway, request_id, "mock_mode")
    # Same admission, durable quota, cancellation, deadlines, adapters and validation as app guidance.
    snapshot = Snapshot(request_id, "probe_" + request_id[:58], 1, int(time.time()*1000),
                        "com.saathi", 0, "en-IN", "ELECTRICITY_BILL", frozenset({PREFIX + "recharge_bills"}))
    result = gateway.decide(snapshot)
    return status(gateway, request_id, "accepted" if result["status"] == "accepted" else result["reason"])
