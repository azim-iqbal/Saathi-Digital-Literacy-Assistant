"""Atomic lifetime call caps across restarts. Stores aggregate counters, never goals or screens."""
import sqlite3
import threading
from backend.errors import BudgetUnavailable


class PersistentBudget:
    def __init__(self, path, global_limit, provider_limit):
        self.global_limit, self.provider_limit = global_limit, provider_limit
        self.lock = threading.Lock()
        self.db = sqlite3.connect(path, check_same_thread=False, timeout=0.25)
        self.db.execute("CREATE TABLE IF NOT EXISTS calls (provider TEXT PRIMARY KEY, count INTEGER NOT NULL)")
        self.db.commit()

    def reserve(self, providers):
        with self.lock:
            try:
                self.db.execute("BEGIN IMMEDIATE")
                counts = dict(self.db.execute("SELECT provider, count FROM calls"))
                if any(type(n) is not int or n < 0 for n in counts.values()):
                    raise BudgetUnavailable("Invalid counters")
                if sum(counts.values()) + len(providers) > self.global_limit or any(counts.get(p, 0) >= self.provider_limit for p in providers):
                    self.db.rollback()
                    return False
                for p in providers:
                    self.db.execute("INSERT INTO calls VALUES (?,1) ON CONFLICT(provider) DO UPDATE SET count=count+1", (p,))
                self.db.commit()
                return True
            except Exception:
                try:
                    self.db.rollback()
                except Exception:
                    pass
                raise BudgetUnavailable("Call budget unavailable") from None

    def close(self):
        with self.lock:
            self.db.close()
