"""Explicit configuration. Missing placeholders fail before any provider can be contacted."""
import os
from pathlib import Path
from backend.gateway import Gateway
from backend.providers import RestProvider
from backend.budget import PersistentBudget


def load_local_env(path=Path("backend/.env")):
    if not path.exists():
        return
    for line in path.read_text().splitlines():
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        name, separator, value = line.partition("=")
        if not separator or not name.isidentifier() or not name.isupper():
            raise ValueError("Invalid backend .env entry")
        os.environ.setdefault(name, value.strip().strip('"').strip("'"))


def configured_gateway():
    load_local_env()
    mode = os.environ.get("SAATHI_PROVIDER_MODE", "mock")
    if mode == "mock":
        return Gateway()
    if mode != "dual_ai":
        raise ValueError("SAATHI_PROVIDER_MODE must be mock or dual_ai")
    providers = [RestProvider(p, os.environ.get(p.upper() + "_API_KEY", ""), os.environ.get(p.upper() + "_MODEL", "")) for p in ("gemini", "groq")]
    total = int(os.environ.get("SAATHI_MAX_PROVIDER_CALLS", "100"))
    per_provider = int(os.environ.get("SAATHI_MAX_CALLS_PER_PROVIDER", "50"))
    if not 2 <= total <= 10000 or not 1 <= per_provider <= 5000:
        raise ValueError("Use bounded positive call limits")
    budget = PersistentBudget(os.environ.get("SAATHI_BUDGET_DB", "backend/local-budget.sqlite3"), total, per_provider)
    return Gateway(providers, timeout=10, global_limit=total, provider_limit=per_provider, mode=mode, budget=budget)
