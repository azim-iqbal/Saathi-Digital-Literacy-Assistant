# Proposed durable-state architecture for multiple backend instances

10 October 2026 — design only; no migration, runtime or deployment certification.

The present hosted backend is a bounded single-worker pilot. Its account hashes and atomic call caps are SQLite-backed; sessions, in-flight ownership and cancellation are process-local. Do not start replicas against separate ephemeral copies and call the result a durable multi-instance service. The prepared Dockerfile explicitly retains this single-host boundary.

## Required invariants and proposed implementation

| State | Proposed external representation | Required behavior |
| --- | --- | --- |
| Accounts and token revocation | Transactional shared database with unique token hash, account ID, expiry and revocation version | Every authenticated request checks current authority. Never store plaintext bearer tokens. Rotation and revocation must be atomic across instances. |
| Global, provider and account caps | Shared reservation ledger and counter rows in the same database transaction | Reserve all three caps before dispatch. Concurrent instances cannot overspend. Cancellation/crash after possible dispatch does not refund an attempt. |
| Idempotency | Unique account/request/attempt key with payload digest, state and result metadata | A retry cannot create a second provider reservation or replay a response for different input. No raw screen/secret payload retained. |
| Worker ownership | Expiring lease with a monotonic fencing generation | An expired worker cannot publish after a replacement owns the request. Do not automatically redispatch uncertain provider attempts following a crash. |
| Cancellation | Durable cancellation tombstone plus optional wake-up channel | Cancellation is scoped to the authenticated account/session/request. Check it before dispatch, before fallback and before publishing. A message bus is only an optimization; missing notification must not erase cancellation. |
| Screen identity | Request-bound session/revision/package/window and original deadline | Reject stale replies even if the server restarts or a different instance handles the response. No deadline extension on retry. |
| Evidence and reviewed plans | Account-scoped, expiring records containing safe source metadata and reviewed graph | Check source freshness, scope and review version again on retrieval. Never confuse another user's identical labels or request IDs. |
| Provider health | Bounded shared health signals, with per-request deadlines enforced locally too | An outage cannot cause unlimited retries or an unbounded worker queue. Health hints never override privacy or freshness. |

A transactional database adapter must be introduced behind the existing account/budget interfaces before enabling replicas. The current SQLite adapters remain the explicitly selected pilot mode; production configuration must refuse multi-instance mode until every critical store and ownership path uses the shared adapter. An external database name in configuration alone is not this implementation.

## Acceptance required before implementation is called ready

1. Two independent worker processes contend for the final reservation: exactly one succeeds; restart preserves usage and token revocation.
2. Retry the same request on a different process: no second dispatch; different payload under the same ID is rejected.
3. Kill a worker after reservation and after dispatch: no refund or blind replay; the result remains uncertain unless reconciliation establishes it.
4. Cancel through a different process; delay/drop the wake-up signal: no late publication or secondary-provider call.
5. Expire and replace a lease, then deliver the old response: fencing rejects it.
6. Exhaust individual, provider and global caps concurrently; verify account isolation and bounded queues.
7. Rotate credentials and restore a backup: revoked authority cannot silently return, and spent quotas cannot roll back unnoticed.
8. Confirm TTL cleanup and diagnostics retain no raw screen, private field, bearer token or provider key.
9. Run container startup, read-only filesystem, graceful shutdown, TLS/auth and remote Android connection checks on the selected runtime.

These are proposed acceptance requirements, not passing evidence. Shared-store implementation, container execution and hosted acceptance remain unfinished. No paid resource or external credential is required to review this design; actual deployment remains deferred.
