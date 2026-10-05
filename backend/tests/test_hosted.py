"""Hosted boundary tests use in-memory WSGI requests and synthetic provider envelopes."""
import io
import json
import tempfile
import time
import unittest
from unittest.mock import patch

from backend.accounts import Accounts
from backend.budget import PersistentBudget
from backend.hosted import HostedApplication
from backend.providers import RestProvider
from backend.tests.test_diagnostics import reply


class HostedTests(unittest.TestCase):
    def setUp(self):
        self.folder = tempfile.TemporaryDirectory()
        self.accounts = Accounts(self.folder.name + "/users.db")
        self.budget = PersistentBudget(self.folder.name + "/budget.db", 20, 10)
        self.tokens = {"alice": "a" * 43, "bob": "b" * 43}
        for user, token in self.tokens.items():
            self.accounts.issue(user, token, int(time.time()) + 60, 4)
        self.sent = []
        def factory():
            def transport(provider):
                self.sent.append(provider)
                return reply(provider)
            return [RestProvider(p, "synthetic-key", "fixture-model", lambda *_, p=p: transport(p)) for p in ("gemini", "groq")]
        self.app = HostedApplication(self.accounts, self.budget, factory)

    def tearDown(self):
        self.app.close()
        self.folder.cleanup()

    def request(self, path="/v1/connection-status", body=None, user="alice", **overrides):
        data = json.dumps({} if body is None else body).encode()
        env = {"wsgi.url_scheme": "https", "wsgi.input": io.BytesIO(data), "REQUEST_METHOD": "POST", "PATH_INFO": path,
               "CONTENT_LENGTH": str(len(data)), "CONTENT_TYPE": "application/json", "HTTP_AUTHORIZATION": "Bearer " + self.tokens.get(user, user)}
        env.update(overrides)
        headers = []
        result = self.app(env, lambda status, values: headers.append((status, dict(values))))
        self.assertEqual(headers[0][1]["Cache-Control"], "no-store")
        return int(headers[0][0].split()[0]), json.loads(b"".join(result))

    def test_https_authentication_and_revocation_before_any_dispatch(self):
        self.assertEqual(self.request(**{"wsgi.url_scheme": "http"})[0], 403)
        self.assertEqual(self.request(user="wrong")[0], 401)
        self.assertEqual(self.request()[0], 200)
        self.accounts.revoke("alice")
        self.assertEqual(self.request()[0], 401)
        self.assertFalse(self.sent)

    def test_rotation_and_expiry_and_hash_only_storage(self):
        self.accounts.issue("alice", "c" * 43, int(time.time()) + 60, 4)
        self.assertEqual(self.request()[0], 401)
        self.assertEqual(self.request(user="c" * 43)[0], 200)
        with patch("backend.accounts.time.time", return_value=time.time() + 120):
            self.assertEqual(self.request(user="c" * 43)[0], 401)
        dump = "\n".join(self.accounts.db.iterdump())
        for token in (*self.tokens.values(), "c" * 43):
            self.assertNotIn(token, dump)

    def test_same_request_ids_are_isolated_and_other_users_cannot_cancel_or_read_them(self):
        self.assertEqual(self.request("/v1/cancel", {"request_id": "shared"})[0], 200)
        code, bob = self.request("/v1/provider-check", {"request_id": "shared", "consent": True}, user="bob")
        self.assertEqual(code, 200)
        self.assertEqual(bob["reason"], "accepted")
        alice = self.request()[1]
        self.assertTrue(all(p["attempts"] == 0 and p["last"] is None for p in alice["providers"]))
        denied = self.request("/v1/provider-check", {"request_id": "shared", "consent": True})[1]
        self.assertEqual(denied["reason"], "cancelled")
        self.assertEqual(len(self.sent), 2)
        self.assertIs(self.app.users["alice"].pool, self.app.users["bob"].pool)
        self.assertIs(self.app.users["alice"].slots, self.app.users["bob"].slots)

    def test_user_cap_does_not_take_another_users_allowance(self):
        for i in range(2):
            self.assertEqual(self.request("/v1/provider-check", {"request_id": str(i), "consent": True})[1]["reason"], "accepted")
        self.assertEqual(self.request("/v1/provider-check", {"request_id": "blocked", "consent": True})[1]["reason"], "quota_exhausted")
        self.assertEqual(self.request("/v1/provider-check", {"request_id": "0", "consent": True}, user="bob")[1]["reason"], "accepted")
        self.assertEqual(len(self.sent), 6)
        self.assertEqual(dict(self.budget.db.execute("SELECT principal,SUM(count) FROM user_calls GROUP BY principal")), {"alice": 4, "bob": 2})

    def test_global_cap_and_user_counts_survive_restart(self):
        self.budget.global_limit = 2
        self.assertEqual(self.request("/v1/provider-check", {"request_id": "first", "consent": True})[1]["reason"], "accepted")
        self.assertEqual(self.request("/v1/provider-check", {"request_id": "second", "consent": True}, user="bob")[1]["reason"], "quota_exhausted")
        second = PersistentBudget(self.folder.name + "/budget.db", 20, 10)
        try:
            self.assertFalse(second.reserve(["gemini", "groq"], "alice", 2))
            self.assertTrue(second.reserve(["gemini", "groq"], "bob", 2))
        finally:
            second.close()

    def test_invalid_requests_spend_no_calls(self):
        for fields, expected in (({"CONTENT_LENGTH": "9000"}, 413), ({"CONTENT_LENGTH": "-1"}, 400),
                ({"HTTP_TRANSFER_ENCODING": "chunked"}, 400), ({"CONTENT_TYPE": "text/plain"}, 415),
                ({"REQUEST_METHOD": "GET"}, 405), ({"QUERY_STRING": "token=secret"}, 404)):
            self.assertEqual(self.request(**fields)[0], expected)
        self.assertEqual(self.request("/v1/provider-check", {"request_id": "x", "consent": False})[0], 400)
        self.assertFalse(self.sent)

    def test_account_capacity_and_broken_storage_fail_closed(self):
        self.app.max_users = 1
        self.assertEqual(self.request()[0], 200)
        self.assertEqual(self.request(user="bob")[0], 503)
        with patch.object(self.accounts, "authenticate", side_effect=RuntimeError("private-detail")):
            self.assertEqual(self.request(), (503, {"error": "unavailable"}))

    def test_revocation_during_dispatch_discards_response(self):
        from backend.routing import dispatch
        def revoke(gateway, path, data):
            result = dispatch(gateway, path, data)
            self.accounts.revoke("alice")
            return result
        with patch("backend.hosted.dispatch", side_effect=revoke):
            self.assertEqual(self.request()[0], 401)
