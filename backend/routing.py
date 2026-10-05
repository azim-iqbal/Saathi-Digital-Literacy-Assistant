"""Shared protocol dispatch for local and hosted transports."""
import re
from backend.gateway import InvalidRequest, Snapshot
from backend.live import LiveSnapshot
from backend.incident import IncidentSnapshot
from backend.diagnostics import status, check

PATHS = frozenset(("/v1/proposals", "/v1/live-proposals", "/v1/incident-assessment", "/v1/cancel", "/v1/connection-status", "/v1/provider-check"))


def dispatch(gateway, path, data):
    if path == "/v1/connection-status":
        if data != {}:
            raise InvalidRequest("Empty status request required")
        return status(gateway)
    if path == "/v1/provider-check":
        return check(gateway, data)
    if path == "/v1/cancel":
        if not isinstance(data, dict) or set(data) != {"request_id"} or not isinstance(data["request_id"], str) or not re.fullmatch(r"[A-Za-z0-9_-]{1,64}", data["request_id"]):
            raise InvalidRequest("Invalid cancellation")
        return {"cancelled": gateway.cancel(data["request_id"]), "mode": gateway.mode}
    if path in ("/v1/live-proposals", "/v1/incident-assessment"):
        if gateway.mode != "dual_ai":
            return gateway.rejected("not_configured")
        snapshot = IncidentSnapshot.parse(data) if path == "/v1/incident-assessment" else LiveSnapshot.parse(data)
    elif path == "/v1/proposals":
        snapshot = Snapshot.parse(data)
    else:
        raise InvalidRequest("Unknown route")
    return gateway.decide(snapshot, primary_navigation=isinstance(snapshot, LiveSnapshot))
