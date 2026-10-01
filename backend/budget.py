"""Atomic lifetime call caps across restarts. Stores aggregate counters, never goals or screens."""
import sqlite3
import threading


class PersistentBudget:
    def __init__(self, path, global_limit, provider_limit):
        self.global_limit, self.provider_limit = global_limit, provider_limit
        self.lock = threading.Lock()
        self.db = sqlite3.connect(path, check_same_thread=False)
        self.db.execute("CREATE TABLE IF NOT EXISTS calls (provider TEXT PRIMARY KEY, count INTEGER NOT NULL)")
        self.db.commit()

    def reserve(self, providers):
        with self.lock:
            self.db.execute("BEGIN IMMEDIATE")
            try:
                counts = dict(self.db.execute("SELECT provider, count FROM calls"))
                if sum(counts.values()) + len(providers) > self.global_limit or any(counts.get(p, 0) >= self.provider_limit for p in providers):
                    self.db.rollback()
                    return False
                for p in providers:
                    self.db.execute("INSERT INTO calls VALUES (?,1) ON CONFLICT(provider) DO UPDATE SET count=count+1", (p,))
                self.db.commit()
                return True
            except Exception:
                self.db.rollback()
                return False

    def close(self):
        self.db.close()
