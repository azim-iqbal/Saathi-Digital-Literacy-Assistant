# Pause-time request cancellation and final local verification — 9 October 2026

## Reproduced production defect

ResearchRecoveryUiTest failed before the change. Sanitized monotonic timings in before-order.txt show Pause at 7510509, response release at 7511078 and Stop at 7511587. ResearchActivity cancelled only in onStop, so the response updated report/retry state after the user left. This was an actual lifecycle race, not the fixture timeout hypothesis in the previous handoff.

ResearchActivity now cancels in onPause, retaining onStop cleanup. No layout, text, palette, spacing or rendering changes. after.txt passes both research tests; after-order.txt confirms the same pause-before-response-before-stop sequence. The complete source report may remain available for manual review; cancelled work does not restore a retry action or publish a late result.

## Browser harness correction

The first 21-test combined run passed 20, including 120 WebView cycles and 60 private/message/CAPTCHA handoffs. Chrome failed in a test-only tap helper: it found a control, then fetched a second incomplete tree and called first(), producing NoSuchElementException. Five helpers now keep the complete snapshot satisfying the bounded wait; their waits use the result once rather than reevaluating a changing tree after success. Stale-marker, mutation and task-continuity assertions remain. This does not establish the older historical WebView failure's cause.

## Provider probe preparation

The user separately authorized at most four new genuine calls, only after deterministic checks pass. The navigation-only probe uses two fictional English/Hinglish cases, two providers each, no retries, an exclusive marker and four total/two per-provider reservations. Offline probe tests include cap, replay refusal, locales and no raw-response retention. Its initial test fixture used a blank expected-outcome string and was correctly rejected; fixing the fixture preserves production validation.

## Reproduction

Use the JDK/SDK and loopback fixture commands in ../2026-10-09-private-handoff/REPRODUCE.md. Run ResearchRecoveryUiTest, PausedRequestUiTest and the core class list recorded in emulator-retest.txt. MixedLifecycleEnduranceTest accepts mixed_rounds=1..120 and runs actual service revocation/rebind, incomplete-tree recovery, loopback loss/recovery and endpoint replacement in one app process. Evidence writes after each completed round and on failure. No genuine AI is used by instrumentation.

No ProviderSmokeTest, deployment, signing, commit, push or reset credit is part of the local test suite. Live probe authorization is limited to its unique directory and cannot be reused.

## Same defect confirmed on other visible request screens

PausedRequestUiTest deliberately holds a local response until the Activity is STARTED (paused, not stopped), then releases it before resuming. Both incident assessment and server status failed before the fix (`paused-before.txt`, 2/2 failures). Both activities cancelled only on Stop. Their existing cancellation and TTS cleanup now run on Pause, keeping existing messages and rendering untouched. This adds no automatic retry or additional provider call.

The first core run after the ResearchActivity fix passed 20/21, including 120 WebView cycles in 198 seconds and 60 handoffs; Chrome's test-helper double-read failed. After the helper correction, `emulator-retest.txt` passes all 21 tests, with 40 further WebView cycles and 12 handoffs. The storm in device-retest generated 660 events / 60 mutations, delivered 76 records, created 7 observations and started 2 synthetic HTTP requests. No genuine provider calls.

## Genuine provider result (authorization exhausted)

Exactly four reservations were used. English navigation: Gemini HTTP 200 / 430 tokens / 2642 ms and Groq HTTP 200 / 601 tokens / 1878 ms, both ACCEPTED and expected Help target; paired PASS. Hinglish: Groq HTTP 200 / 626 tokens / 1017 ms / ACCEPTED; Gemini returned no HTTP response before its 8076 ms timeout; paired FAIL/provider_timeout. No raw replies, keys, private data or automatic retry. This is adapter/gateway validation of two fictional cases, not Android E2E, incident assessment, eligibility or broad multilingual accuracy. The earlier Gemini unsupported-completion error did not recur in its successful English response; timeouts remain a live reliability gate. Do not reuse the four-call authorization or remove its marker.

## Request resource-ordering defect

The new eight-round mixed-fault run failed during round seven's tenth recovery request; the old assertion did not retain a rejection reason. Six completed rounds and the failure are saved in mixed-first/ and mixed-endurance.txt. A separate 1000-request transport run passed. Those facts alone do not prove that the intermittent failure is fixed.

Code inspection found that PracticeGateway posted callbacks before releasing its two-request semaphore. A controlled worker pause reproduced premature completion deterministically in callback-before.txt. The worker now releases connection/pending-request/capacity state before posting its result; the main-thread generation/cancellation guard remains in place. The debug-only zero-argument scheduling hook is no-op in release and stores no request content. The original mixed failure cannot retrospectively be attributed to this race without its missing reason.

`transport-final.txt` passes eight tests: the deterministic callback/capacity ordering regression, 1000 sequential loopback recovery requests, endpoint cancellation and opt-in preservation, plus pause-time request cancellation on research, diagnostics and incident screens. `paused-reporting-after.txt` passes 14 tests before the final transport-ordering change, including reporting, worksheet, link overlays, explicit sharing and offline fallbacks. Later final groups are recorded separately; overlapping counts are not unique test totals.

## Private practice handoff precedence

The full practice integration suite exposed a second-order privacy regression. On the synthetic private form, whole-context minimization intentionally removed resource IDs. SaathiSession checked those IDs before checking for a private interruption, so it requested a return to practice instead of entering SENSITIVE_HANDOVER. Updating an outdated test-copy selector alone did not fix this (`practice-final.txt`, `practice-copy-retest.txt`).

The session now checks the package boundary first and handles private/CAPTCHA interruption before practice-ID eligibility. An unrecognized own-app private screen can receive only the existing local no-target handoff; ordinary practice guidance and backend requests still require the recognized synthetic controls. No IDs or private values are restored. `private-practice-after.txt` passes all four integration tests, including no HTTP requests while entering fictional private values and observed completion only after the safe synthetic receipt screen. The regression retains the original no-target/privacy assertions.

## Final build after handoff precedence fix

`private-practice-build.txt`: debug APK, instrumentation APK, release Kotlin compilation and lint successful. `final-unit-summary.json`: 147 tests, zero failures/errors/skips. Lint has zero errors and 61 existing warnings. `core-private-final.txt`: 27/27 pass after the last production edit, including private masking, accepted pause/resume, source binding, voice lifecycle, Chrome, real-service cloud fixture, permission loss, transport/callback sequencing, research consent/cancellation and synthetic incident assessment. `core-private-device/` contains its screenshots/diagnostics. Its 660-event / 60-mutation storm delivered 74 records, created seven observations and sent three synthetic HTTP requests; zero genuine provider calls.

The exact configured provider keys are absent from the final debug APK under the byte/UTF16 scan; no provider-key patterns matched tracked/nonignored text. `secret-scan.json` records the final APK hash, ignored credential-file status and mode 0600 without values. This targeted check is not an exhaustive secret or telemetry audit.

## Obsolete private-form recovery expectation

The broader reporting/startup/recovery sweep ran 21 tests, with one failure (`reporting-private-final.txt`). PracticeRecoveryTest expected a Back target on a private form, contradicting whole-context minimization and the accepted no-target handoff. The test now asserts PRIVATE classification, absent IDs and absent private target, then performs the person's manual Back action and verifies that Water remains the original goal. Unrelated non-private detour highlighting and recreation clearing fictional private values remain covered. This is a test requirement correction, not a new production or UI change; the final retest is recorded separately.

`reporting-private-retest.txt` confirms the corrected practice recovery passes, but caught a separate TravelPrivacyUiTest harness exception during a transitional tree (`Missing observation branch`). Its existing ten-second poll now treats exactly that exception as unavailable and requires a later complete tree; all date/price/destination and OTP assertions remain. Unexpected exceptions still fail. This does not change NodeMasker's production fail-closed behavior or establish a cause for the old WebView incident. `reporting-complete.txt` records the combined rerun after both harness corrections.

The next combined run reached a different harness limitation: ActivityScenario.moveToState(RESUMED) left both artificially paused request screens STOPPED instead of returning foreground focus. Earlier pause-response regression groups passed; this failure occurs at the return operation before the result assertions (`reporting-complete.txt`). The helper now foregrounds the existing task through an explicit REORDER_TO_FRONT/SINGLE_TOP intent, requires RESUMED within five seconds, and asserts the same Activity instance. It still releases the held response only after asserting STARTED and still checks cancellation copy and absent pending action. Production lifecycle handling and request cancellation are unchanged. A separate final pause-test run records the outcome after this correction.

`paused-return-final.txt` passes both corrected pause tests in 10.8 seconds. `reporting-verified.txt` is the final combined check in the same ordering that exposed the task-focus problem; the earlier failing combined runs are retained. The test-only return correction never changes request cancellation, sensitive-screen handling, production foreground/background behavior or visual rendering.

## Final result and cleanup

`reporting-verified.txt`: 21/21 pass in 120.965 seconds, including the corrected pause-return helper in the same combined sequence that previously failed. Together with `core-private-final.txt` (27/27) and `private-practice-after.txt` (4/4), the final application has 52 passing emulator checks across these groups. Screenshots from the final reporting group are in `reporting-device/`. No further production changes followed these tests.

All four known local fixture servers were stopped, reverse mappings 8765/8766 removed and the temporary host/device fixture token deleted. Provider configuration remains ignored and untouched. No commit, push, deployment, signing or reset credit was used. Only the four separately authorized genuine calls were performed; their allowance is exhausted.

Open acceptance remains explicit in NEXT_CONTINUATION.md and the capability matrix. Recent passing loops do not establish the original WebView/cloud failure's cause; 140 seconds of mixed-fault testing is not hours-long validation, and no phone/legitimate protected portal session was available.
