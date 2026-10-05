"""Explicit configuration. Missing placeholders fail before any provider can be contacted."""
import os
from pathlib import Path
from backend.gateway import Gateway
from backend.providers import RestProvider
from backend.budget import PersistentBudget


class UnconfiguredProvider:
    configured = False
    model = "not-configured"
    def __init__(self, name): self.id = name
    def propose(self, *args): raise RuntimeError("Provider is not configured")


def load_local_env(path=None):
    if path is not None:
        target = Path(path)
    elif Path("backend/.env").exists():
        target = Path("backend/.env")
    elif (Path(__file__).resolve().parent / ".env").exists():
        target = Path(__file__).resolve().parent / ".env"
    elif Path(".env").exists():
        target = Path(".env")
    else:
        return
    if not target.exists():
        return
    for line in target.read_text().splitlines():
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        name, separator, value = line.partition("=")
        if not separator or not name.isidentifier() or not name.isupper():
            raise ValueError("Invalid backend .env entry")
        os.environ.setdefault(name, value.strip().strip('"').strip("'"))


def provider_order():
    primary = os.environ.get("SAATHI_PRIMARY_PROVIDER", "gemini")
    if primary not in ("gemini", "groq"):
        raise ValueError("SAATHI_PRIMARY_PROVIDER must be gemini or groq")
    return (primary, "groq" if primary == "gemini" else "gemini")


def configured_gateway():
    load_local_env()
    mode = os.environ.get("SAATHI_PROVIDER_MODE", "mock")
    if mode == "mock":
        return Gateway()
    if mode != "dual_ai":
        raise ValueError("SAATHI_PROVIDER_MODE must be mock or dual_ai")
    providers = []
    for name in provider_order():
        try:
            providers.append(RestProvider(name, os.environ.get(name.upper() + "_API_KEY", ""), os.environ.get(name.upper() + "_MODEL", "")))
        except ValueError:
            providers.append(UnconfiguredProvider(name))
    total = int(os.environ.get("SAATHI_MAX_PROVIDER_CALLS", "100"))
    per_provider = int(os.environ.get("SAATHI_MAX_CALLS_PER_PROVIDER", "50"))
    if not 2 <= total <= 10000 or not 1 <= per_provider <= 5000:
        raise ValueError("Use bounded positive call limits")
    budget = PersistentBudget(os.environ.get("SAATHI_BUDGET_DB", "backend/local-budget.sqlite3"), total, per_provider)
    return Gateway(providers, timeout=10, global_limit=total, provider_limit=per_provider, mode=mode, budget=budget)
