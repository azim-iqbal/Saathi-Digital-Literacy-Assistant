## 9 October 2026 — Acceptance closeout, usage-floor handoff

**NOT COMPLETE / NOT READY.** Latest short-window usage is 7% remaining (weekly 70%); the user's 8% floor was crossed between checks. No resets, new live model calls, deployment, signing, commit or push. Stop new work and inspect active results on the next authorized continuation. Preserve all current changes on baseline `7012254`.

Completed: provider slow-header/cancellation defects reproduced and fixed with absolute-deadline TLS interruption and joined per-call watchers. 168 backend tests, 9 evaluation tests, 51/51 probes, 147 Android unit tests (explicit uncached rerun), six local hosted checks, five repetitions of 11 TLS/diagnostic tests, 2,000 recovery plus 2,000 pre-cancelled requests pass. Emulator TalkBack coexistence passed (not audible/human usability). The earlier two-hour run finished: 111 rounds, 7,209,179 ms, 1,554 local requests, 111 cancellations, zero model calls; file descriptors/threads bounded, PSS/heap increased modestly, no heap-leak certification. See `test-evidence/2026-10-09-acceptance-closeout/README.md` and JSON evidence.

**New active failure:** full core rerun reproduced stale guidance after a genuine WebView DOM change. `core-failure-device/mandate-core-final/transition-failure.png` visibly shows **Support opened** while Saathi still marks **Support**. `transition-input.txt`, windows, event and tap traces preserved. A separate portal fixture stopped at SENSITIVE_HANDOVER instead of CAPTCHA. Do not label either fixed by earlier passes. Settings test also failed; its corrected driver waits for the actual unique control and rechecks tap geometry. Its focused two-test retest passes and the next combined run passes Settings; this is a harness improvement, not proof of historical WebView causality.

**Unverified production candidate:** `SaathiAccessibilityService.scheduleCopy()` calls `clearCache()` on API33+ before reading the current root to avoid cached WebView descendants. Build/unit/release compilation/lint pass (`cache-build.txt`). This is a cache-staleness hypothesis pending functional verification, not an established root cause. Android <33 remains unchanged. Do not weaken privacy, old-window filtering or freshness to force acceptance.

Active tests at stop:
- emulator-5556, exec session **74450**: `cache-focused.txt`; LiveAccessibilityIntegrationTest with **40 WebView cycles**, PortalWorkflowIntegrationTest and PauseResumeIntegrationTest with **10 handoff rounds**. Output `/data/user/0/com.saathi/files/closeout-cache`. Last observed still running first test. Read final result and export evidence before any new install/test on this device.
- emulator-5554, exec session **54796**: `remaining-progress.txt`, runner `run_remaining.py`; practice **7 PASS**, research/reporting, connection, visual and browser groups pending/running. Output app files `mandate-<group>-final`; local fixture servers/reverse mapping are owned and cleaned by this runner. No genuine providers. Do not install/run another test there until finished.
- Original two-hour run is DONE; its data was exported before installing this final candidate on 5554. Original evidence belongs to the earlier APK, not this cache candidate.

UI: no design/layout/style/logo changes. Initial comparison 12/14 pairs identical; paused screenshot mismatch was Light vs Dark persisted preference, now the test explicitly selects/restores Dark; Home difference is inspected pressed shading. Final same-theme capture comparison is pending. `ui-source-preservation.json` was captured before the cache candidate; only new production Android edit since it is accessibility cache invalidation, not UI. Do not cite that earlier JSON as proving zero current production edits.

Next required work:
1. Inspect those two final test outputs. If WebView/portal still fail, collect traces and diagnose; do not keep a speculative fix just because it compiles. If passing, add deterministic cache/fresh-observation coverage and repeat affected combined core/service/privacy/cost checks; a passing rerun alone is not root-cause evidence.
2. Export final screenshots and perform same-theme paused comparison. Consolidate counts only after all final groups finish. Earlier core runs failed (one Settings failure, then WebView and portal failures); they must not be counted as 29 passing.
3. Update report/matrix with actual final evidence. Complete code/doc diff and secret checks after any new changes. Current code/doc diff check passed; raw completed log files retain normal trailing blank lines.
4. Genuine planning/incident retest awaits a fresh scoped answer. An asynchronous request for **up to four calls** was issued, but no answer was received before stopping. Previous eight-call allowance is exhausted. `evaluation.final_acceptance --suite reasoning` now enforces four reservations and has passing fixture tests; DO NOT execute absent explicit authorization.
5. Physical voice/OEM, legitimate protected portal, browser full HTTPS metadata limitations and Docker/runtime acceptance remain unverified. Single-host SQLite pilot is not stateless Cloud Run support; shared durable state/ownership architecture remains necessary for that target. No deployment.

# Pre-deployment acceptance closeout — 9 October 2026

No deployment, signing, distribution, Git commit/push or new live provider calls. Existing changes are preserved. Baseline commit: `7012254`.

## Reproduced and corrected

`transport-before.txt` records two failures: trickling response headers outlasted a 120 ms absolute deadline (observed 563 ms), and cancellation waited for a blocked response (205 ms). A socket idle timeout is not a total-operation deadline. `TransportDeadline` now owns a bounded watcher per active provider call, interrupts TLS I/O on cancellation/deadline, normalizes EOF/socket failures to the fixed cause, and joins the watcher on every exit. No extra provider retry or increased freshness/timeout allowance. TLS certificate and public-address checks remain enforced. Tests include headers, body, untrusted certificates, cancellation, malformed JSON and resource cleanup. `transport-repeated.txt` contains five passing 11-test repetitions. These are synthetic local TLS tests, not provider latency measurements.

The first combined emulator group failed one Settings assertion. The isolated diagnostic subsequently reached correct GUIDING text but failed the post-tap transition. The test previously treated package visibility as control readiness and reused pre-animation coordinates. Its corrected driver keeps the complete snapshot establishing a unique target and rereads geometry immediately before the single tap. `settings-fixed.txt` passes both tests. This improves the test; it does not establish causality for the original historical WebView incident or every earlier failure. Production Android files are unchanged in this run.

## Completed endurance evidence

The earlier background run finished: `endurance-final.json` reports PASS, 7,209,179 ms, 111 rounds, 1,554 local requests, 111 cancellations, zero genuine provider calls. Instrumentation log in the previous directory reports 8,183.965 seconds overall. The active mixed test duration, not shell duration, establishes the two-hour target.

Across 111 samples: PSS 112,118–120,777 KB (last 120,329); used heap 6,158,608–8,272,144 bytes; file descriptors 131–136; live threads 40–41. The first/last quarter median PSS rises from 115,466 to 119,910 KB and heap from 7,092,496 to 8,010,000 bytes. Handle/thread counts remain bounded; rising memory is not proof of zero leaks. This run has no heap dominator evidence. `emulator-exit-info.txt` records instrumentation force-stop exits, not a crash/ANR during the run. The process had exited before final meminfo; its unavailable post-exit snapshot is not a memory sample. The APK predates the prior narrative/disclosure changes; focused tests cover those later edits. No physical speech/OEM certification.

## UI evidence

Saved before/after pairs: 12 of 14 initially pixel-identical. `assistant-paused.png` had different persisted themes; the test now explicitly selects and restores Dark. `glass-home-opaque.png` differs only within Home selected/pressed shading (bbox 66,2514–448,2730), visually inspected with unchanged geometry and text. No image pixels were edited to make comparisons pass. `ui-source-preservation.json` checks no changes under `app/src/main` against the current baseline. Final same-theme paused capture is collected separately.

## External/manual boundaries

- The previous eight genuine calls remain exhausted. An optional four-call reasoning-only run is prepared with `python3 -m evaluation.final_acceptance --authorized --suite reasoning --output <new-private-directory>`. Do not execute without a new answer authorizing it. It preserves independent expected outcomes, exclusive run markers and a four-reservation cap.
- The last real Chrome observation sees example.org public content but only an editable scheme-less address. The cybercrime public page content was not established. This is not positive HTTPS source binding; Saathi keeps the safe fallback. No bypass, login, legal acceptance or complaint submission.
- TalkBack emulator coexistence passes: installed service plus Saathi, accessibility focus, explicit pause/resume and marker cleanup. Audible quality and human usability need physical/manual testing.
- Docker runtime is unavailable here. The single-host SQLite pilot template is not stateless Cloud Run compatibility. A shared durable state/ownership design is required before any multi-instance Cloud Run deployment. No cloud resources were created.

## Reproduction

From repository root:

```
python3 -m unittest discover -s backend/tests
python3 -m unittest discover -s evaluation -p 'test_*.py'
python3 -m evaluation.run --output /tmp/saathi-new-offline.json
python3 -m evaluation.reliability_stress --output /tmp/saathi-new-stress.json
python3 -m backend.tests.hosted_smoke
./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest compileReleaseKotlin lintDebug
python3 docs/test-evidence/2026-10-09-final-mandate/run_emulator_groups.py --adb /absolute/path/to/adb --serial emulator-NNNN --output /new/evidence/directory
```

The hosted smoke needs the pinned Gunicorn environment. All model providers in these commands are local fixtures. ProviderSmokeTest and live evaluation are excluded. The emulator runner creates/removes its own local servers, reverse mapping and device fixture token; prior failures remain preserved.
