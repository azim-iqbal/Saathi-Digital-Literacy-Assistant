"""Stable failure codes only. Never return provider bodies, credentials or raw exceptions."""
class ProviderFailure(Exception):
    def __init__(self, reason):
        self.reason = reason if reason in {"provider_auth", "provider_request", "provider_model", "provider_rate_limited", "provider_unavailable", "provider_timeout"} else "provider_unavailable"
        super().__init__(self.reason)

class BudgetUnavailable(Exception):
    pass
