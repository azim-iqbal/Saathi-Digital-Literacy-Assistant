"""Offline operator tool: tokens are written only to a new owner-readable file."""
import argparse
import os
from pathlib import Path
import secrets
import sqlite3
import time
from backend.accounts import Accounts


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--state-dir", required=True, type=Path)
    parser.add_argument("action", choices=("issue", "revoke"))
    parser.add_argument("user")
    parser.add_argument("--token-file", type=Path)
    parser.add_argument("--days", type=int, default=30)
    parser.add_argument("--calls", type=int, default=20)
    args = parser.parse_args()
    if not args.state_dir.is_dir():
        parser.error("Create a private state directory first")
    if args.action == "issue" and (args.token_file is None or not 1 <= args.days <= 90
                                  or not 2 <= args.calls <= 10000):
        parser.error("Issue requires a new token-file path, 1..90 days and 2..10000 calls")
    os.umask(0o077)
    accounts = None
    try:
        accounts = Accounts(args.state_dir / "accounts.sqlite3")
        if args.action == "revoke":
            accounts.revoke(args.user)
            print("Access revoked. Already dispatched provider calls may still consume usage.")
        else:
            # Exclusive creation refuses to overwrite any existing file or symlink.
            with args.token_file.open("x", encoding="utf-8") as output:
                token = secrets.token_urlsafe(32)
                output.write(token + "\n")
                output.flush()
                os.fsync(output.fileno())
            # Save both the contents and the new directory entry before revoking
            # previous access. Failed writes/syncs must never rotate the database.
            directory = os.open(args.token_file.parent, os.O_RDONLY | os.O_DIRECTORY)
            try:
                os.fsync(directory)
            finally:
                os.close(directory)
            accounts.issue(args.user, token, int(time.time()) + args.days * 86400, args.calls)
            print("Access issued. Token saved to the selected private file; never commit or log it.")
    except (ValueError, OSError, sqlite3.Error):
        # Retain any candidate file: an interrupted database commit may have an
        # uncertain outcome. Never delete a potentially active credential.
        raise SystemExit("Account operation failed. Check arguments, storage and private file permissions. "
                         "Keep any candidate token file private; issuance is not confirmed. "
                         "See docs/DEPLOYMENT.md for recovery.") from None
    finally:
        if accounts is not None:
            accounts.close()


if __name__ == "__main__":
    main()
