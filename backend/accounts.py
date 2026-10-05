"""Operator-issued pilot access tokens. Stores hashes, never raw bearer tokens."""
import hashlib
import re
import sqlite3
import threading
import time


class Accounts:
    def __init__(self, path):
        self.lock = threading.Lock()
        self.db = sqlite3.connect(path, check_same_thread=False, timeout=.25)
        self.db.execute("CREATE TABLE IF NOT EXISTS users (id TEXT PRIMARY KEY, token_hash TEXT UNIQUE NOT NULL, expires INTEGER NOT NULL, call_limit INTEGER NOT NULL, enabled INTEGER NOT NULL)")
        self.db.commit()

    def issue(self, principal, token, expires, call_limit):
        if (not isinstance(principal, str) or not re.fullmatch(r"[A-Za-z0-9_-]{1,64}", principal)
                or not isinstance(token, str) or not re.fullmatch(r"[A-Za-z0-9_-]{43,128}", token)
                or type(expires) is not int or expires <= time.time()
                or type(call_limit) is not int or not 2 <= call_limit <= 10000):
            raise ValueError("Invalid account settings")
        digest = hashlib.sha256(token.encode()).hexdigest()
        with self.lock, self.db:
            self.db.execute("INSERT INTO users VALUES (?,?,?,?,1) ON CONFLICT(id) DO UPDATE SET token_hash=excluded.token_hash, expires=excluded.expires, call_limit=excluded.call_limit, enabled=1",
                            (principal, digest, expires, call_limit))

    def authenticate(self, authorization):
        if not isinstance(authorization, str) or not re.fullmatch(r"Bearer [A-Za-z0-9_-]{43,128}", authorization):
            return None
        digest = hashlib.sha256(authorization[7:].encode()).hexdigest()
        with self.lock:
            row = self.db.execute("SELECT id,call_limit FROM users WHERE token_hash=? AND enabled=1 AND expires>?", (digest, int(time.time()))).fetchone()
        return row

    def revoke(self, principal):
        with self.lock, self.db:
            self.db.execute("UPDATE users SET enabled=0 WHERE id=?", (principal,))

    def close(self):
        with self.lock:
            self.db.close()
