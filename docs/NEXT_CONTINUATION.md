## 7 October 2026 - Phone backend diagnosis and recovery fixes

Latest user priority: fix intermittent backend failures before the APK phase. User confirmed the phone was used **without USB** and no hosted server exists; chose **prepare hosted connection, deploy next phase**. The computer-local debug endpoint cannot serve that phone configuration. Read [BACKEND_CONNECTION_RECOVERY.md](BACKEND_CONNECTION_RECOVERY.md) first.

Based on Umair's latest `5a37d93` (form/spoken guidance preserved). Fixed its form-guide compile error, emulator-only endpoint discovery, failed-connection cleanup, request-pinned cancellation, unsafe/sticky POST fallback, and AI opt-in inadvertently enabling networked practice. Provider circuits now recover after a bounded cooldown and count consecutive failures; uncertainty does not poison provider health. Existing UI/brand unchanged; recovery wording is clearer.

Verified: **95 backend tests, 90 Android unit tests, 12 emulator executions (11 distinct), debug/test build, lint 0 errors/61 warnings**. Includes ten status requests after server recovery, endpoint replacement, mock practice/auth/cancellation, actual-service synthetic primary guidance and an additional emulator-host-route run without adb reverse. No real provider calls, deployment, signed release or push. Evidence: `test-evidence/2026-10-07-backend-recovery`.

Next: chosen HTTPS host and real-TLS/auth connection acceptance, then connected/signed APK and physical-phone checks. The original intermittent WebView cause, genuine model accuracy, voice/OEM acceptance and other release audit gates remain open. Passing the earlier primary integration test now does not establish its historical failure cause. Changes are local on `fix/backend-response-time`.

## 5 October 2026 — Local-First Guidance and Conditional Cloud Navigation

Implemented: `LiveGuide.plan` and `SaathiSession` prioritize exact deterministic local matching before initiating cloud guidance, with private/ambiguous screen handover. Backend live navigation queries the configured primary provider (`SAATHI_PRIMARY_PROVIDER`, default `gemini`) and conditionally invokes a secondary provider only upon primary failure, uncertain/invalid output, or explicit handover. Both share the original deadline and budget tracking; quota exhaustion, cancellation, or stale observations do not trigger fallback. Incident assessment and explicit provider checks retain dual-provider validation. Navigation responses declare `decision_policy` and provider provenance, decoded by the Android client. Consent copy reflects local/primary/fallback routing while preserving existing UI styling and colors.

Verification evidence: **80 Android unit tests pass**; debug/test builds pass; **89 offline backend tests pass**, including ten primary-navigation tests and localhost HTTP tests. Focused emulator tests verify zero-HTTP exact local match and protocol decoder stability. Reports are preserved under `docs/test-evidence/2026-10-05-local-first-primary`.

## 4 October 2026 — Architecture & Release-Readiness Progress

Release architecture establishes: local deterministic guidance first, primary cloud provider with conditional secondary use, comprehensive localization/voice/failure handling, followed by production container deployment (Cloud Run) and release build signing.

Completed: Initial severity-classified architecture audit, compatibility/release-security tracking, signing properties ignore rules, and local configuration for multi-provider support. Code adheres strictly to local-first routing, session-level request limits, and backend multi-account quota isolation.

## 4 October 2026 — Account Rotation Reliability

Resolved an operator CLI bug: database rotation happened before the replacement token was saved, so disk-write failure invalidated the old token without a usable replacement. The CLI now saves/syncs the private candidate and directory before rotation, sanitizes SQLite failures (including database opening), and retains the candidate for uncertain-commit recovery.

Verified **16 targeted offline tests** (eight new account-command regressions plus eight existing hosted-boundary tests). Evidence: `docs/test-evidence/2026-10-04-account-rotation/backend-tests.txt`.

## 4 October 2026 — Hosted Pilot and Release Connection

Prepared a Gunicorn/Caddy/systemd deployment package, hashed expiring per-user tokens, isolated sessions/cancellation/diagnostics, atomic durable individual/global call caps, and shared bounded provider workers. Moved the Android gateway and existing themed setup into main sources; release accepts only a build-selected HTTPS origin and stays offline when absent. Connection replacement/disable now invalidates pending callbacks.

Verified: 71 offline backend tests; six real local Gunicorn checks with provider keys removed; 77 Android unit tests; debug/release/test builds; lint 0 errors/58 warnings; five emulator connection tests including the new credential-change cancellation regression. Saved under `docs/test-evidence/2026-10-04-hosted-connection`.

## Latest completed phase — 4 October 2026

Provider work now shares a monotonic deadline capped by remaining observation freshness. Both REST adapters receive at most the smaller of eight seconds and the remaining decision time. Expired dispatch, late response reads and validation overrun are rejected; parsed late replies retain usage evidence without successful guidance. All **63 offline backend tests pass**; see TEST_RESULTS.md and `test-evidence/2026-10-04-provider-deadlines`. App code/UI and local credentials were untouched; no live API check, emulator rebuild or push occurred.

The Groq timeout's external cause is still unknown. Next useful latency work is transport-phase observability and bounded synthetic navigation/incident evaluation with explicit scoped authorization for any live probe. Do not increase deadlines beyond screen freshness or repeat unchanged connection checks. Blocking socket/DNS operations cannot be forcibly recalled by the cooperative stop signal. The original WebView post-tap investigation, browser clipboard return, voice/device acceptance and production release work below remain pending.

Read the latest [provider verification](PROVIDER_VERIFICATION.md), [test results](TEST_RESULTS.md) and [execution plan](EXECUTION_PLAN.md) first. Preserve the completed connection checks, reviewed complaint worksheet and themed copy helper.

Both providers are independently verified; the final pair timed out on Groq. Prioritize latency investigation within screen-freshness constraints, then bounded genuine navigation and incident-assessment evaluation. Use offline tests first and avoid repeating unchanged live probes. Remaining work includes browser return/paste validation, the original WebView transition failure, voice/device acceptance and production release connectivity.

Credentials stay in ignored local configuration. Never print or commit them. No private complaint, legal declaration or submission should be used as test data.

## Earlier handoff (historical credential status; preserve completed features)

# Next continuation after API diagnostics and complaint drafts — 3 October 2026

Read the top entries of EXECUTION_PLAN.md, TEST_RESULTS.md, AI_ORCHESTRATION.md, CYBER_FRAUD_REPORTING.md and CURRENT_STATE_AUDIT.md. Preserve all existing user changes. The user clarified that the API keys were removed; do not search broadly for secrets or print them. They must be restored privately before a genuine provider check. No real model call was made.

## Completed; preserve rather than rebuild

- Authenticated no-call status and explicit two-provider smoke check in debug Backend connection, same bounded transport/budget/cancellation path, per-provider metadata and recovery. Normal app model calls also update recent status. Both failure counters accounted for. Injected fixtures are explicitly marked as fixtures. Missing keys/models remain diagnosable without dispatching either provider.
- Memory-only complaint worksheet, focused factual follow-ups, unknowns omitted, editable account, review confirmation reset on edits, exact per-field clipboard previews/consent, sensitive clipboard flag and return-to-existing-browser instructions. Optional bounded themed helper closes on revocation/lock/expiry. Text-first with optional instruction TTS. No automatic paste, private-field observation or report submission.
- Shared secondary/tertiary colors now use the existing green palette; no main navigation/logo/primary-color replacement. Final 75 unit / 54 backend / 12 emulator tests; debug and release builds; lint0 errors/61 warnings. See screenshots/2026-10-03-connection-drafts/verification.json.

## Next work

1. When the user restores credentials, verify the actual app Check APIs path with one explicit bounded check, then evaluate consented synthetic navigation and incident assessments. Report each provider independently, current model compatibility, real usage metadata and guidance correctness. Do not infer a dashboard delay or claim agreement guarantees truth. Do not enable billing or new paid services.
2. The complaint builder is currently local fact assembly, not generative rewriting. Evaluate model-assisted focused follow-ups/field-purpose suggestions with strict grounding in user-confirmed facts, per-use redacted-context consent and no invented narrative. Keep identifying values local. Real model quality needs keys and evaluation.
3. Validate manual clipboard/return flow in real browsers and current public portal surfaces without private information, declarations or complaint submission. Authenticated field labels, constraints and paste behavior are unverified. Automatic paste is intentionally absent because current guidance hands private forms back to the user; only consider a separately scoped exact-field consent design if identity/freshness can be revalidated safely. Otherwise retain manual paste. Never bypass protected-overlay restrictions or certificates.
4. Continue investigating the original intermittent WebView post-tap failure with saved input/window diagnostics. The separate own-overlay event filter fix is not proof of its cause.
5. Production HTTPS/per-user auth/accounting and release connectivity, continuous speech/barge-in, physical-device microphone/TTS/OEM survival, accessibility/localization and remaining design parity remain unfinished. User authorizes emulator testing now; do not repeat unchanged software-emulator benchmarks or claim device results.

Scheduled runs check usage and defer below25% remaining in either window. One bounded phase per run; preserve themed controls and content-sized buttons, keep completed/blocked/untested evidence current. No merge, publishing or messages to others. Continue this existing automation; notify only meaningful completion, new blocker or required user action.
