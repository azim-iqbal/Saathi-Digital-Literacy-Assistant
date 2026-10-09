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

## Verdict at usage stop: NOT READY — validation gates remain

Implementation stopped at 5% weekly remaining; the earlier 8% threshold was crossed between checks. The two-hour endurance target and screenshot comparison are not certified. Chrome has a newly observed failure under diagnosis. See NEXT_CONTINUATION.md for exact active processes. All eight new live calls were consumed; planning/incident acceptance failed and post-fix genuine retesting needs new authorization.

# Final pre-deployment validation — 9 October 2026

This is an evidence report, not a production certification. Deployment, signing, distribution, Git commit and push were not performed. Earlier working-tree changes were preserved.

## Engineering changes

- Navigation previously allowed the primary provider to consume the whole decision window. A primary attempt now receives at most four seconds or 60% of the remaining window, whichever is shorter. Fallback uses the original deadline, including the remaining 15-second observation freshness allowance. Cancellation, replacement, quotas and late-result rejection remain mandatory. Research and incident validation still require two providers.
- Default navigation order is now Groq then Gemini, based on this limited measured sample. An explicit `SAATHI_PRIMARY_PROVIDER` overrides the default. This is a latency choice, not a claim that Groq is more accurate.
- Provider HTTPS now records fixed DNS/connect/TLS/write/header/body/JSON timings. Connections use validated public DNS addresses, verified TLS and one remaining deadline. Diagnostics never retain prompts, response bodies, credentials or arbitrary exception messages. Android's strict existing status contract is unchanged; operator evaluation opts into richer timing metadata.
- Complaint narrative context now enters the shared private handoff on Android and is rejected by backend navigation. Editable values remain unread. English, Hindi and Hinglish context tests cover this addition.
- Proposed reading steps are checked against explicit quoted prerequisite relations in supported English/Hindi/Hinglish grammatical forms. Omitted prerequisites/edges are withheld. Incident signals contradicting an explicit denial are withheld. Prompts clarify prerequisite direction and distinguish a request from an actual transfer. These conservative checks do not prove general semantic entailment.
- The approved visual system is preserved. Only outdated capability disclosure text changed: it no longer falsely says live-app help and cloud AI are unavailable. Consent/configuration requirements and screen-capture-off behavior are stated using existing components.
- Controlled portal coverage now includes private login, OTP, CAPTCHA, safe return, narrative fields, outage, final review, consented copy/manual paste, rotation and screen locking. Test-only actions operate on fictional forms; production has no automatic legal acceptance or submission.
- A source-allowlisted, non-root container template is prepared for the existing single-host pilot. It was not built here: Docker is unavailable. It is explicitly not a stateless Cloud Run deployment.

## Current verified checks

Evidence: `docs/test-evidence/2026-10-09-final-mandate/`.

| Check | Evidence / scope |
|---|---|
| Android unit, debug/test build, release Kotlin compilation, lint | 147 tests; zero failures/errors/skips; lint zero errors, 61 warnings. `android-checks.json`, `final-android-build.txt` |
| Backend regression | 165 tests passing, including real local TLS and original-deadline faults. `backend-verified.txt` |
| Evaluation | 9 tests passing and 51/51 offline capability probes. `evaluation-verified.txt`, `offline-verified.json` |
| Accelerated backend recovery | 2,000 successful recovery requests plus 2,000 pre-cancelled requests; 3,200 synthetic provider reservations, zero genuine calls. `backend-stress.json` |
| Hosted entry | Six local Gunicorn authentication/configuration/revocation checks pass; no public listener or model credentials. `hosted-smoke-final.txt` |
| Secret scan | No exact configured provider-key values in 1,275 tracked/non-ignored files or decompressed APK entries. `.env` remains owner-only. `secret-scan.json`; this is not a universal secret audit |
| Portal and worksheet | Four focused tests passed before the additional rotation/lock extension. See final emulator evidence for extended result; no authenticated portal compatibility claim |
| Endurance and full emulator pass | Consult the latest runner outputs/checkpoint. A two-hour target is not a pass until elapsed time reaches 7,200,000 ms and status is PASS |

## Genuine provider result and Gemini suitability

The newly authorized eight calls were all consumed, with no retries. `live-eight/results.json` retains every result and fixed diagnostics.

- English navigation: Gemini 1,409 ms; Groq 844 ms. Both independently selected the expected current control.
- Hinglish navigation: Gemini 6,770 ms; Groq 609 ms. Both selected the expected control, but Gemini exceeded the new four-second primary-attempt budget. This was a paired adapter evaluation, not an Android screen-navigation timing test.
- Planning: Gemini timed out at 8,001 ms waiting for response headers. DNS/connect/TLS took approximately 22 ms combined. Groq returned in 2,258 ms but failed the independent prerequisite-chain expectation.
- Hindi incident: Gemini timed out at 8,001 ms waiting for response headers. Groq returned in 1,204 ms but failed the expected signal-set check for the fictional no-transfer account.

The delay is after connection establishment. Time waiting for headers includes network/provider queue/processing time; it cannot isolate model computation. Gemini is **not validated as the sole blocking real-time provider**. Extending its timeout would make screen guidance less useful and could produce an obsolete result. The app retains fresh-screen validation and safe fallback instead.

Two successful navigation examples do not establish broad AI accuracy. The post-run prompt/validation changes have passing offline evidence, **not a genuine-model retest**. Any new model run needs a fresh bounded authorization; do not replay the exhausted allowance. The evaluation command and independent expected outcomes are prepared.

Synthetic latency distributions, including p50/p95/max and sample counts, are in `backend-stress.json`. They measure dispatch/validation with instantaneous fixture providers and fake retrieval, not actual model, network, device or speech latency. Do not combine timed-out samples into a successful-response latency percentile.

## Historical intermittence

The previously reproduced old-window event defect and cleanup-before-callback transport defect remain covered. They do not establish the original WebView post-tap incident's cause. The original available failure does not contain enough matching input/window/renderer evidence for a causal attribution. Extended tests exercise real WebView transitions, pause/private/CAPTCHA handoffs and loopback recovery; passing repetitions must not be called proof that the historical incident is fixed.

New failures preserved this run include the initially starving fallback, missing narrative context, missing explicit evidence checks, a Settings test poll over an incomplete tree, and WebView text-search omissions in a test driver. Production still rejects incomplete observations. The test drivers now wait for complete observations and traverse owned WebView nodes, without relaxing production privacy.

## Browser and portal boundaries

Public official FAQ/checklist pages were fetched read-only on 9 October; `public-portal.json` records sources and limitations. No login, legal declaration, OTP, CAPTCHA bypass, identity data or complaint submission occurred. Public wording does not certify protected field structure or limits.

Chrome public-page observations are recorded separately from fixture navigation. Missing, editable, truncated or scheme-less address metadata cannot establish a trusted HTTPS origin. Page content and a requested URL do not prove the current post-redirect address. Safe fallback remains required. Brave is not installed on the available emulators. No physical phone is attached.

## Remaining acceptance gates

- **Genuine planning/incident quality: FAIL in the recorded run; post-fix live retest BLOCKED on new scoped authorization.** No claim that deterministic validation equals accurate reasoning.
- **Physical voice/OEM/TalkBack usability/performance: BLOCKED on hardware/manual acceptance.** Muted emulator engine callbacks and lifecycle tests cannot certify human speech accuracy or natural continuous conversation.
- **Authenticated portal: BLOCKED on legitimate access/manual acceptance.** Controlled fixtures do not certify the real protected site.
- **Historical post-tap incident: unreproduced cause remains uncertain.** Do not assign a root cause from unrelated passing reruns.
- **Real HTTPS source binding: browser-dependent PARTIAL.** Preserve fallback when the browser does not expose trustworthy complete metadata.
- **Deployment acceptance: deferred.** Actual hosting, trusted TLS, access provisioning, remote-device connectivity, container runtime, signing and operational recovery remain unexecuted.

The final verdict must follow the final test results and these gates; do not call Saathi fully production-ready.

## Future deployment handoff — do not execute now

1. Review `deploy/.env.example`, `deploy/gunicorn.conf.py`, `deploy/Caddyfile` and the single-host persistence/isolation limits in `docs/DEPLOYMENT.md`.
2. Choose the deployment architecture. Existing SQLite accounts/quotas and cancellation ownership require one worker with durable local state. A stateless/multiple-instance Cloud Run deployment requires a reviewed durable shared-state design first; do not mount ephemeral storage and assume quotas survive restarts.
3. Build and inspect the prepared container on a machine with Docker. Use the allowlisted build context, non-root user, read-only application filesystem, private durable state and maintained CA roots. Test backup/restore and crash recovery before exposure.
4. Provision the chosen host/domain/TLS and server secrets privately. Issue per-user expiring tokens with bounded allowance. Verify health and authenticated no-model-call status before any opted-in provider checks.
5. Compile the Android release with the real public HTTPS origin. Verify no debug endpoint/token/provider keys are embedded, then perform authorized remote-device and provider acceptance.
6. Sign and distribute only in the separately authorized release phase, after the remaining acceptance gates pass.
