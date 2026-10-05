"""Account rotation failures must not strand a user without a saved token."""
import contextlib
import io
import os
from pathlib import Path
import sqlite3
import tempfile
import time
import unittest
from unittest.mock import patch

from backend.accounts import Accounts
from backend.manage_accounts import main


class AccountCommandTests(unittest.TestCase):
    def setUp(self):
        self.folder = tempfile.TemporaryDirectory()
        self.root = Path(self.folder.name)
        self.output = self.root / "new.token"
        self.accounts = Accounts(self.root / "accounts.sqlite3")
        self.old_token = "a" * 43
        self.accounts.issue("alice", self.old_token, int(time.time()) + 3600, 20)
        self.stdout = io.StringIO()

    def tearDown(self):
        self.accounts.close()
        self.folder.cleanup()

    def run_issue(self):
        args = ["manage_accounts", "--state-dir", str(self.root), "issue", "alice",
                "--token-file", str(self.output)]
        previous_umask = os.umask(0o077)
        try:
            with patch("sys.argv", args), contextlib.redirect_stdout(self.stdout):
                main()
        finally:
            os.umask(previous_umask)

    def assert_old_access(self):
        self.assertEqual(self.accounts.authenticate("Bearer " + self.old_token), ("alice", 20))
        self.assertNotIn("Access issued", self.stdout.getvalue())

    def test_failed_token_write_preserves_previous_access(self):
        original_open = Path.open

        @contextlib.contextmanager
        def failing_open(path, *args, **kwargs):
            with original_open(path, *args, **kwargs) as output:
                with patch.object(output, "write", side_effect=OSError("disk full")):
                    yield output

        with patch.object(Path, "open", failing_open), self.assertRaises(SystemExit):
            self.run_issue()
        self.assert_old_access()

    def test_failed_token_sync_preserves_previous_access(self):
        with patch("os.fsync", side_effect=OSError("sync failed")), self.assertRaises(SystemExit):
            self.run_issue()
        self.assert_old_access()

    def test_failed_directory_sync_preserves_previous_access(self):
        original_sync = os.fsync
        calls = []

        def fail_directory(fd):
            calls.append(fd)
            if len(calls) == 2:
                raise OSError("directory sync failed")
            original_sync(fd)

        with patch("os.fsync", side_effect=fail_directory), self.assertRaises(SystemExit):
            self.run_issue()
        self.assert_old_access()

    def test_database_open_failure_is_sanitized(self):
        with patch("backend.manage_accounts.Accounts", side_effect=sqlite3.OperationalError("internal detail")):
            with self.assertRaises(SystemExit) as error:
                self.run_issue()
        self.assertNotIn("internal detail", str(error.exception))
        self.assertFalse(self.output.exists())
        self.assert_old_access()

    def test_database_failure_keeps_saved_candidate_and_sanitizes_error(self):
        with patch.object(Accounts, "issue", side_effect=sqlite3.OperationalError("internal detail")):
            with self.assertRaises(SystemExit) as error:
                self.run_issue()
        self.assertNotIn("internal detail", str(error.exception))
        self.assert_old_access()
        self.assertEqual(len(self.output.read_text().strip()), 43)

    def test_error_after_commit_retains_the_active_candidate_for_recovery(self):
        original_issue = Accounts.issue

        def commit_then_fail(accounts, *args):
            original_issue(accounts, *args)
            raise sqlite3.OperationalError("commit acknowledgement failed")

        with patch.object(Accounts, "issue", commit_then_fail), self.assertRaises(SystemExit):
            self.run_issue()
        token = self.output.read_text().strip()
        self.assertEqual(self.accounts.authenticate("Bearer " + token), ("alice", 20))
        self.assertIsNone(self.accounts.authenticate("Bearer " + self.old_token))
        self.assertNotIn("Access issued", self.stdout.getvalue())
        self.assertNotIn(token, self.stdout.getvalue())

    def test_existing_file_and_symlink_are_never_overwritten(self):
        target = self.root / "existing.token"
        target.write_text("keep this")
        for symlink in (False, True):
            if symlink:
                self.output.symlink_to(target)
            else:
                self.output.write_text("keep this")
            with self.assertRaises(SystemExit):
                self.run_issue()
            self.assertEqual(self.output.read_text(), "keep this")
            self.assert_old_access()
            self.output.unlink()

    def test_success_saves_private_token_before_rotation_and_never_prints_it(self):
        original_issue = Accounts.issue

        def checked_issue(accounts, principal, token, expires, calls):
            self.assertTrue(self.output.read_text() == token + "\n", "Candidate must be saved before rotation")
            return original_issue(accounts, principal, token, expires, calls)

        with patch.object(Accounts, "issue", checked_issue):
            self.run_issue()
        token = self.output.read_text().strip()
        self.assertEqual(self.output.stat().st_mode & 0o777, 0o600)
        self.assertIsNone(self.accounts.authenticate("Bearer " + self.old_token))
        self.assertEqual(self.accounts.authenticate("Bearer " + token), ("alice", 20))
        self.assertNotIn(token, self.stdout.getvalue())


if __name__ == "__main__":
    unittest.main()
