## 7 October 2026 - Backend recovery on latest teammate code

**95 backend tests / 90 Android unit tests / 12 emulator test executions (11 distinct) pass**; debug/test builds pass; lint **0 errors / 61 warnings**. New regressions first reproduced failure streaks surviving success and task uncertainty incorrectly opening permanent circuits. Cooldown recovery, single-probe concurrency, failed-probe reopening, cancellation and quota enforcement now pass. Android verifies disconnected local server -> restart -> ten successful status calls -> server shutdown; old-endpoint cancellation; no networked practice from AI opt-in. Existing live primary/decoder and mock-practice tests pass, plus the full synthetic live flow over the emulator host route without adb reverse.

[Evidence and exact scope](BACKEND_CONNECTION_RECOVERY.md), [verification](test-evidence/2026-10-07-backend-recovery/verification.json). Current teammate code initially failed compilation due to `FormGuide.fieldName` referencing an undefined `node`; corrected to its `field` parameter. No tests skipped. No live model calls, public HTTPS test, physical phone, signed release or deployment. The user's physical-phone configuration had no USB/hosted endpoint; standalone phone cloud use awaits the agreed next deployment phase. Historical WebView/primary failure causes are not claimed solved by passing reruns.

## In-progress handoff — 5 October 2026, local-first and conditional cloud navigation

The user authorized this manual run down to **8% weekly remaining**. Current usage is **6%**, so stop implementation and resume after allowance is available or the user explicitly changes the limit. Do not consume reset credits. The scheduled automation still has its separate 25% rule.

Implemented, uncommitted: LiveGuide.plan and SaathiSession now use an exact local match before cloud guidance, with private/ambiguous handover. Backend live navigation tries the configured primary (SAATHI_PRIMARY_PROVIDER, default gemini) then one secondary only for provider failure, uncertain/invalid output or explicit HANDOVER. Both share the original deadline and budgets; quota/cancellation/staleness do not trigger fallback. Incident assessment and explicit provider checks still use paired validation. Navigation responses declare decision_policy and actual single-provider provenance; Android strictly decodes this or the legacy paired contract. Consent copy reflects local/primary/fallback use; styles and colors are unchanged. The legacy mode name dual_ai now means two adapters available, not two calls per live navigation request.

Evidence: **80 Android unit tests pass**; debug/test builds passed before the final consent-copy edits. **89 offline backend tests passed**, including ten primary-navigation tests and localhost HTTP tests (prior tool output; the /tmp log disappeared, so no durable backend log is available). Four focused emulator tests produced **3 passes / 1 failure**: zero-HTTP exact local match and two decoder tests passed; primaryProtocolGuidesAnExternalDetourAndReturn failed at the first Help instruction with generic cannot-verify text. XML reports are preserved under test-evidence/2026-10-05-local-first-primary. Do not mark H2 integration or the whole release complete.

Failure investigation: a separate synthetic host request returned the expected primary-policy envelope; fixture status reported one Gemini attempt and zero Groq attempts, attributable to that host diagnostic. Thus the failed device flow had no confirmed provider dispatch. Next inspect safe request/rejection counters and snapshot eligibility; distinguish snapshot-null, Android validation, HTTP rejection and observation churn. Clock skew is a hypothesis only, never verified. Decoder tests passed, so do not loosen parsing or freshness to mask the failure. This failure is distinct from the earlier unresolved WebView post-tap issue. The emulator and fixture are no longer running on 5 October; temporary logs/tokens are gone. Recreate only synthetic test infrastructure and restart the fixture after any backend changes.

Next: reproduce/fix the failed integration, then run release build/lint and relevant emulator regressions against the final copy/config changes. H3 time/token/device/IP quotas, localization, voice/Sarvam, Cloud Run persistent state/enrollment, signing, real-model quality and physical-device acceptance remain open. No live model calls, hosting, signing, commit or push occurred. Preserve all pre-existing changes. Current audit remains docs/PROJECT_AUDIT.md and latest human spec is specs/PRODUCTION_RELEASE_BRIEF.md.

# Test results

## 4 October 2026 — account issuance failure recovery

**16 targeted offline tests pass**: eight new account-command tests and eight existing hosted-boundary tests. Before the fix, injected token-write failure reproduced loss of the old credential; the save-order and missing-sync tests also failed, and a SQLite error escaped unsanitized. The CLI now saves and syncs the token file/directory before rotating the database.

Coverage includes write/file-sync/directory-sync failure preserving previous access, database opening/issuance errors, retaining a usable candidate after a simulated post-commit error, exclusive file/symlink refusal, owner-only output, saved-before-rotation ordering, and no token in command output. Existing hosted isolation/quota/revocation checks still pass. See [targeted test log](test-evidence/2026-10-04-account-rotation/backend-tests.txt).

No live account/key, model call, external service or Android change. These are temporary SQLite/filesystem tests, not a host power-loss test. Prior full-suite/build/emulator evidence below was not rerun in this bounded phase.

## 4 October 2026 — hosted pilot and shared Android connection

**71 backend tests pass**, including eight hosted-token/isolation/budget tests. A real Gunicorn 26.2.0 subprocess passed six localhost checks with keys removed: readiness, HTTPS-scheme requirement, auth refusal, authenticated status, missing-provider refusal without calls, and revocation. See [backend log](test-evidence/2026-10-04-hosted-connection/backend-tests.txt) and [Gunicorn smoke](test-evidence/2026-10-04-hosted-connection/gunicorn-smoke.json).

Android: **77 unit tests**, debug/release/test APK builds and lint pass (0 errors/58 warnings). [Five emulator checks](test-evidence/2026-10-04-hosted-connection/emulator-connection.txt) pass: setup consent, real mock-HTTP practice/private handover, strict decoding, wrong-token/cancelled callbacks, and suppressing old results after disable/reconfigure. A temporary synthetic server and ephemeral token were used; no model requests. Existing palette/navigation/logo/glass geometry remain unchanged; release now exposes the existing connection screen and only accepts a build-selected HTTPS origin. No release signing or remote host is configured.

Untested: Caddy/systemd deployment, real TLS/remote Android release connection, public load/abuse and host recovery, live AI accuracy, browser paste, the original intermittent WebView cause, voice/physical-device behavior and remaining localization/design parity. The local Gunicorn check simulates a trusted proxy header; it is not a TLS test. See DEPLOYMENT.md for the exact operating limits.

## 4 October 2026 — provider deadlines and screen freshness

**63 offline backend tests pass**, including eight new latency/cancellation regressions. [Complete output](test-evidence/2026-10-04-provider-deadlines/backend-tests.txt) and [verification summary](test-evidence/2026-10-04-provider-deadlines/verification.json) are saved. The initial restricted run passed its non-network checks but could not bind five localhost test servers; the permitted localhost run passed the full suite. All model transports were synthetic; no keys were loaded and no live provider call occurred.

Verified: both adapters receive the remaining shared decision time (at most eight seconds); expired requests never dispatch; aged screens stop waiting at their remaining freshness deadline; cooperative workers release all four slots; expiry during budget reservation spends no provider calls; late body/EOF and late validation cannot release accepted guidance. Parsed replies arriving too late or after cancellation retain available token metadata without being marked successful. Screen-expiry cancellation does not by itself count as a provider-health failure.

Only backend runtime/tests and documentation changed. Android builds, emulator/browser/voice tests and live model checks were not repeated. Existing Android evidence remains historical. The final 3 October Groq record shows an 8,101ms timeout without a parsed HTTP reply; it cannot distinguish network/TLS delay from provider processing. This phase fixes local deadline propagation, **not** the unproven cause of that live timeout. Python blocking network operations may still outlive cooperative cancellation; worker admission remains bounded and late results are rejected. Live paired accuracy, incident quality, the original WebView failure, production connectivity and physical-device acceptance remain open.

## Latest live-provider check — 3 October 2026

Both providers returned valid responses on separate checks. The final pair had a valid Gemini response and a Groq timeout; the app returned diagnostic information and withheld guidance. The explicitly opted-in instrumentation check completed, and the offline backend suite passed **55 tests** after the Gemini parser fix. See [provider verification](PROVIDER_VERIFICATION.md) for the full sequence and saved evidence. The previous **75 Android unit / 12 focused emulator** results below remain the baseline for that phase.

## 3 October 2026 — API diagnostics and reviewed complaint workflow (latest)

**Final source:** 75 Android unit tests, 54 backend tests and 12 focused emulator tests pass. Debug, Android-test and release APK builds pass; lint has **0 errors / 61 warnings**. Final logs and machine-readable [verification](screenshots/2026-10-03-connection-drafts/verification.json) are saved in `screenshots/2026-10-03-connection-drafts/`.

- Backend coverage adds no-call status/mock checks, auth/consent enforcement, independent partial success, sanitized errors/usage, explicit test-vs-live provenance, missing configuration, cancellation, quota, timeout and both-provider circuit accounting. Initial sandbox-only attempt could not bind local test sockets; the authorized local-socket run passed. No live provider was called.
- Final emulator suite: ConnectionScreenUiTest (1), ConnectionCheckTest (2), GatewaySetupUiTest (1), ComplaintDraftUiTest (2), CyberLinkOverlayTest (2), CyberReportUiTest (4). The Check APIs button traverses actual Android HTTP through adb reverse and the authenticated server to two **injected synthetic REST responses**, not Gemini/Groq. Wrong bearer token, strict metadata, denied copy, exact field content, review invalidation, privacy clearing and permission loss were exercised. Each run cleans the temporary token and reverse mapping.
- Unit tests add unknown-fact omission, exact supplied figures/uncertainty, blank-account rejection and section order. No assertion claims factual correctness beyond the supplied account.
- Initial 11-test and follow-up 3-test runs passed. Screenshot inspection then found default-purple selected chips; shared secondary/tertiary containers were bound to the existing Saathi greens, followed by the final 12-test run. `final-evidence/` supersedes earlier screenshots. Production screens/overlays keep FLAG_SECURE; tests only lift it for synthetic in-app screenshots. The protected floating overlay screenshot is intentionally not visual evidence of readable content.

**Blocked/untested:** live keys were removed; provider-account calls/dashboard usage and genuine AI accuracy remain unverified. No authenticated cybercrime form, automatic paste, complaint submission or real incident was tested. New TTS instructions compile but audible output was not exercised. Physical devices/OEM survival, streaming conversation, production release connectivity and the original intermittent WebView post-tap cause remain open. Prior service/browser results below are historical evidence, not rerun claims for this phase.

## 3 October 2026 — backend recovery, incident assessment and portal follow-up

**Final source:** 71 Android unit tests and 46 Python backend tests pass. Debug APK/test APK build and lint pass (0 errors, 61 warnings). Eleven distinct emulator tests pass in two final direct instrumentation suites: `GatewayIntegrationTest` + `CyberReportUiTest` (7, mock backend), then `IncidentAssessmentUiTest` + `LiveAiIntegrationTest` + `LiveAccessibilityIntegrationTest` (4, independent deterministic protocol fixtures). Each suite used a fresh loopback server/shared ephemeral token; test credentials and reverse mapping were removed afterward. The earlier 8-test mixed run and post-event-fix 4-test Gradle run also passed. Final direct logs and [verification](screenshots/2026-10-03-backend-reporting/verification.json) are retained.

New backend tests cover corrupt/locked budget storage, failed first/second worker submission, pre-admission cancellation, cancellation without poisoning provider health, expired reservation deadlines, sanitized auth/rate/timeout failures, malformed envelopes, escaped surrogates, HTTP recovery, incident consent/privacy, paired agreement and rejection of invented legal certainty. Model transport tests use synthetic envelopes, not real provider calls.

Android checks exercise per-use Share once, Keep private without transmission, private-summary refusal, unavailable-backend offline advice, accepted paired fixture result, clearing/recreation, three-language recovery helpers, actual HTTP authentication and cancelled callbacks, service detour/return and WebView mutation. Screenshots of the synthetic consent dialog and light/dark reporting screens were reviewed. Reporting dialogs explicitly use Saathi's surface color, avoiding Material's default purple tint; buttons retain shared content-sized padding. Production screens remain screenshot-protected.

Portal: `https://cybercrime.gov.in/` returned HTTP200 with certificate verification, and emulator Chrome showed a secure connection. `https://www.cybercrime.gov.in/` failed curl certificate validation (expired-certificate error); web retrieval also failed for www. This is an observed environment result, not a claim of universal availability. Chrome traversal: mobile menu → Register a Complaint → FINANCIAL FRAUD → category page → financial Register a Complaint → public filing explanation (`Accept.aspx`). No legal declaration, private entry, OTP, report or tracking lookup was submitted. Public screenshots are saved as portal-menu/categories/handover.png.

**Failures retained:** A repetition before the overlay fix failed the idle-stability assertion (revision 11→13); this is separate from the original post-tap issue. Four later diagnostic repetitions passed. Trace showed a known Saathi overlay window's WINDOW_STATE event classified as own while its WINDOWS_CHANGED event was not; the old package-name requirement caused this gap. Filtering now uses only current attached window ownership. Final traces correctly classify own WINDOWS_CHANGED events and the service test passes. These results do not prove the cause of the original post-tap failure or guarantee no recurrence. Failure artifacts pulled from the first repetition directory include preceding successful-run files; only `webview-repeat-2.txt` is the authoritative failing assertion. Later reproduction directories were cleared before each run.

One standalone attempt could not launch after Gradle uninstalled the test APK; reinstall resolved it. After an approval-review usage interruption, the emulator needed cold boot/unlock; an instrumentation harness stalled before external storage was available and was terminated, then final bounded direct runs passed. These setup failures were not counted as passing tests. Approval review resumed after usage reset; no reset credits were consumed.

Still unverified: real Gemini/Groq responses and classification quality, private portal workflow/submission, original WebView post-tap cause, actual microphone transcripts/voice quality, physical-device/OEM survival, production HTTPS/user auth/release connectivity. Full historical UI/performance suites were not rerun. No paid service, publishing, merge or push occurred.

# Test results — 29 September 2026

## Latest: live request changes and shared assistant theme — 1 October 2026

Final check: **52 unit tests + 5 focused emulator tests passed**, zero failures/errors/skips. Debug build passed; lint passed with **0 errors / 61 warnings**. Command: `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./gradlew testDebugUnitTest assembleDebug lintDebug connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.saathi.ui.LiveAccessibilityIntegrationTest,com.saathi.ui.PermissionLossIntegrationTest,com.saathi.ui.AssistantUiTest,com.saathi.ui.VoiceLifecycleTest`.

- New parser checks cover explicit English/Hindi/Hinglish option requests, ordinary conversational replies and controls, forbidden/secret/oversized input.
- The real enabled service switches the requested target from native Help to WebView Support without an external event or session restart, then back. Immediate old-bound clearing, stale screen/session rejection, refused labels and post-Stop rejection pass. Existing event-driven detour/return, target disappearance and Stop also pass. This invokes the request handoff directly, **not through a real microphone**.
- Live overlay/accessibility revocation and no automatic restart after restoration still pass. Voice foreground waiting/background and obsolete notification actions still pass; no audio was exercised.
- Assistant text/voice mode selection remains functional and starts no microphone by itself. Light/dark captures were visually inspected: shared S mark/header, existing green/mint palette, readable text, rounded input, glass fallback actions and themed mode chips. The sampled portrait captures do not establish all-screen, large-font, TalkBack or tablet parity. The live overlay's existing NEXT STEP label still overlaps a nearby fixture heading; not redesigned in this core phase.
- Evidence: [verification](screenshots/2026-10-01-live-requests/verification.json), [light](screenshots/2026-10-01-live-requests/assistant-light.png), [dark](screenshots/2026-10-01-live-requests/assistant-dark.png), [retarget](screenshots/2026-10-01-live-requests/live-service-retarget.png). New code and earlier edits remain local/uncommitted; no push or provider enablement occurred.

Still incomplete: connected reasoning backend, arbitrary multi-step guidance, natural streaming/barge-in, actual speech-to-request delivery, real Chrome/Firefox page compatibility, physical-device microphone/TTS/OEM survival, and the broader release/design matrix. Chrome first-run setup is still pending. The emulator was closed after verification without saving a snapshot. Older entries below retain historical counts and limitations; this entry is the current scoped result.


Host: macOS arm64, Zulu JDK17.0.20.1, Gradle8.7, AGP8.6.1, Kotlin2.0.21. Android min26/compile35/target33. Compose BOM2024.10.01. Emulator: Pixel_9_Pro, Android17/API37.2, arm64 16KB page system image; no physical device.

## Passing

`JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./gradlew testDebugUnitTest assembleDebug lintDebug connectedDebugAndroidTest`

- 20 unit tests, zero failures: guardrails, policy, demo progression, observation identity, multilingual filtering, task routing.
- Debug APK builds at app/build/outputs/apk/debug/app-debug.apk.
- Lint: zero errors, 57 warnings. Not a warning-free build.
- Four emulator UI tests: typed task→permission setup and disabled action; light/dark Home rendering; Hindi 200% scrolling and task intake; onboarding language and confirmed local-data reset.
- Final screenshot-export/theme-interaction rerun: all four tests passed, zero failures/errors, BUILD SUCCESSFUL. Light, dark and Hindi captures were visually reviewed.
- Screenshots: docs/screenshots/2026-09-29. Captures are Compose content, excluding Android system bars. Screenshots validate sampled views, not all-screen parity.
- Phase-one final unit count was 18 passing (28 September); earlier baseline was seven passing with seven lint errors/59 warnings.

The first emulator run failed before interactions because older Espresso reflected a removed InputManager method. Test dependencies updated to runner1.7.0, junit1.3.0, Espresso3.7.0, following [official AndroidX Test release notes](https://developer.android.com/jetpack/androidx/releases/test). Tests then passed. Screenshot export was changed to additionalTestOutputDir because the runner uninstalls test APKs afterward. Visual inspection found a light image mislabeled dark; the test now switches through Settings and checks both saved preference and rendered background before capturing.

## Not verified / not implemented

- Real-device TalkBack, cross-app touch passthrough, notification Stop, service/overlay races, microphone/TTS release, lock/unlock, permission revocation, process death, landscape/narrow displays, battery and latency: not verified. The selected synthetic goal uses saved state but recreation behavior has not yet been instrumented.
- Hindi large-text home is readable and the primary action is reachable; native translation review, Hinglish screenshots, and every screen at large fonts remain pending.
- No actual speech recognizer/TTS engine interaction was tested. Engines may use network services.
- Cloud/backend, dual-provider validation, authentication and quotas are not implemented; provider and capture paths remain disabled. No real-app or payment success claim.
- Figma components and Home in both themes visually inspected; other frames, exact local-source fidelity, fonts and animation timing remain unverified. See DESIGN_SYSTEM.md for known parity gaps.
- Source credential path removed; exhaustive APK binary/secret scanning not performed.

Use only made-up values for remaining synthetic practice tests. Record actual device/OS/build and observed outcomes before extending compatibility claims.


## Navigation phase — final verification

Full build/unit/lint plus nine functional UI tests passed after fixing dock occlusion and dark heading inheritance. Adding the frame-metrics test produced a final connected run with **10 tests, zero failures/errors, BUILD SUCCESSFUL**. The subsequent isolated metrics run passed its one test; it overwrites the generated connected XML. No production code changed after the full passing functional run.

New checks: nested route selection/Back; intermediate spring position; rapid-tap retarget; continuous category indicator during an unfinished drag; category restoration on leaving/returning; persisted opaque mode across activity recreation; immediate reduced-motion selection; reachable Hindi category/action at320dp and200% font scale. Original task routing/theme/privacy checks remain. Unit tests remain20 passing; lint0errors/57warnings. `git diff --check` passed.

Reviewed screenshots: light Home, dark Practice, Hindi200% narrow Practice/action and both Figma navigation sets plus integrated light/dark Home. Captures now include full Compose root with edge-to-edge insets. New images are in screenshots/2026-09-29-navigation. The earlier screenshots are historical and retained.

**Performance not passed:** Android Window FrameMetrics TOTAL_DURATION on the software-rendered emulator measured p50=138.52ms/p95=253.50ms with recording (165frames); p50=157.92ms/p95=299.70ms without recording (226frames). All frames exceeded16.67ms. This is a measured limitation, not evidence of smooth60Hz animation. See MOTION_AND_HAPTICS.md for profiling follow-up and exact JSON files.

Not run: physical-device frame profiling, TalkBack traversal, API26–30 fallback, low-RAM/power-save/high-contrast toggles, landscape, Hinglish-specific screenshots, full frame-by-frame video review, system-scale-zero pager release, and per-pixel blur validation. Reduced-transparency preference and relevant selection semantics were verified on the emulator; that does not substitute for TalkBack testing.

## Latest safety and Liquid Glass phase

21 unit tests, zero failures/errors/skips; debug APK builds; lint zero errors/57 warnings. Added stale-presentation matching test for permission-related stop so an obsolete overlay cannot terminate a newer observation/session. AppOps watching, startup permission check, expected overlay attachment exception handling and animator cleanup compile successfully. **Live permission revocation/attachment failure is not instrumented or device-verified.** Implementation uses the platform [AppOpsManager watcher](https://developer.android.com/reference/android/app/AppOpsManager).

11 emulator UI tests passed in the full final regression run. Added glass action/disabled-state semantics, opaque fallback on Home, task input, and privacy cancellation coverage. Initial new-test failure was in screenshot capture: the dialog adds a second root; fixed by explicitly targeting the dialog window. Tests and screenshots now distinguish that window. All earlier navigation, Hindi200%, narrow320dp, settings/recreation and privacy-reset checks pass.

Current visual evidence: screenshots/2026-09-29-glass. Reviewed light/dark Home, large Hindi text, dark intake, disabled setup action, and privacy dialog. The native practice button style compiles but its rendered appearance and native form interaction were not exercised during this phase. Pointer hover, keyboard traversal, full TalkBack and all device-fallback branches remain unverified.

Latest broad software-emulator frame sample:160frames, p50=154.34ms, p95=206.94ms; 160 exceed16.67ms. **Performance remains unaccepted.** This debug measurement includes screen composition/test work; it is not a device smoothness certification or a controlled comparison against the previous phase. No physical-device profiling and no new motion video were captured.

See GLASS_UI.md for the audited controls left in historical/native surfaces and Figma synchronization still pending. No full-specification or every-control parity claim.

Final dialog brand-color correction was followed by another complete successful11-test emulator run plus unit/build/lint checks. Exported screenshots and metrics reflect that run.

## Vector identity, launch transition and notification Stop follow-up

Final optimized-vector run:22 unit tests and13 emulator UI tests pass with zero failures/errors. Debug build succeeds; lint reports zero errors/60 warnings. Added launcher entry/recreation/background-return checks and installed adaptive-icon verification/export. Added unit regression proving notification session identity survives observation changes and becomes invalid after stop/restart. Actual queued notification delivery race remains untested.

New SVG and Android vector geometry was rendered and inspected in both standalone and installed adaptive-icon form. Initial detailed contours produced long-path warnings; simplified fitted curves reduced the mark to25 cubic segments and removed those warnings. Old raster assets are intentionally preserved. Remaining new lint findings include the preserved unused raster, a name-based CustomSplashScreen warning for LaunchActivity (which uses AndroidX SplashScreen, not a dedicated custom splash page), and a missing-monochrome warning on the API26 icon variant (the API33 variant contains monochrome). No warning-free claim.

Cold-launch recording and sampled frames: screenshots/2026-09-29-launch/saathi-launch.mp4 and launch-*.png. Reviewed centered mint logo, easing and fade into real content. This is a software-emulator recording, not proof of physical-device60Hz performance. Android26–30, Android12, reduced-motion settings, OEM masks/themed icon appearance and genuinely slow initialization remain unverified. Earlier glass/performance limitations continue to apply.

The original startup lint run flagged an API27 navigation-bar attribute in a min26 style; the unnecessary attribute was removed before the passing run. All changes remain local/uncommitted.

## Local-practice state safety follow-up

`testDebugUnitTest`, `assembleDebug` and `lintDebug` pass after the local-practice state pass. The unit suite now has **25 tests**, zero failures/errors: the added policy cases cover every registered synthetic Bill Pay control, reject a matching control ID in another package, and reject unregistered Saathi screens. The new debug APK builds successfully. Lint reports zero errors and 61 warnings; it is not warning-free.

This run did **not** start an emulator. The UI state mapping, empty-window callback, notification transition, overlay removal, actual TTS-engine callback timing, screen lock, permission revocation, TalkBack and physical-device behavior remain untested. The tests prove policy and compilation only; they do not prove an external app can never resemble the fixture or that Android will deliver every lifecycle callback on a particular device.

## Zero-spend proposal-validation foundation

The subsequent `testDebugUnitTest` run passes with **32 unit tests**, zero failures/errors. Seven new pure tests cover agreeing proposals, disagreement, stale screen metadata, invalid targets, timeout/quota/malformed results, safe handover and an identifier-only snapshot. No provider/network/emulator test was run because this contract is intentionally not connected to the app. A transport-free test contract does not verify a future backend, model, provider, data-retention policy, quota system or real AI output.

## Speech-setting safety follow-up

After adding persisted speech-rate choices, local preview wiring and dormant microphone-service guards, `testDebugUnitTest`, `assembleDebug` and `lintDebug` again pass with **32 unit tests**, zero failures/errors. No emulator or physical device was available. The result does not verify TTS initialization timing, installed language packs, preview output, rate audibility, microphone permission prompts, cancellation, phone-call/headset behavior, audio focus or the legacy service's broadcast consumer flow.

## Background voice and wrong-path recovery — 30 September 2026

Final combined `testDebugUnitTest assembleDebug lintDebug connectedDebugAndroidTest` run: **BUILD SUCCESSFUL**, **41 unit tests and 18 emulator tests**, zero failures/errors/skips. Lint: **0 errors, 57 warnings**. `git diff --check` passes. Environment: Pixel_9_Pro AVD, Android 17/API37, arm64, software graphics, headless/no audio. No physical device or cloud provider was used.

New unit coverage: utterance/recognition cancellation identities; English/Hindi/Hinglish voice command parsing; unknown phrases; wrong-category and wrong-success recovery; missing recovery target; returning to the original category; partial trees. New instrumented coverage reads the real fixture accessibility tree, verifies the actual Back to choices route, handles an unrelated tile, and proves activity recreation retains the bill type but clears account/PIN values. Speech speed persists and preview remains available without starting conversation.

The emulator reports `onDeviceRecognizerAvailable=true`. The real microphone foreground service was tested **in waiting mode with no audio turn**: it remains active after the activity backgrounds, stops, restarts, and ignores the previous session's Stop notification. Final session Stop removes the service. This is not proof of speech quality, permission-dialog behavior, audio capture/release, or continuous microphone operation. Recognition/TTS callbacks are unit-gated; actual engine race behavior still needs device testing. Recovery tests call the guide with a real tree, not the full live accessibility-service/overlay/audio pipeline.

Evidence: [verification summary](screenshots/2026-09-30-voice/verification.json), [recognizer capability](screenshots/2026-09-30-voice/voice-capability.txt), and [speech settings screenshot](screenshots/2026-09-30-voice/voice-settings.png). The screenshot was visually inspected: speed choices, selected Slow state, preview and floating navigation are readable. No new overlay-motion video or full session-screen visual review was captured.

An earlier emulator run encountered a **System UI ANR** whose window stole Espresso focus; it was interrupted and the emulator cold-started. A clean 17-test run then passed; after final source changes and the service test, the full 18-test run above passed. No test was skipped or disabled to get the result.

Performance remains **unaccepted**: the broad software-emulator frame sample recorded169 frames, p50=278.42ms and p95=456.26ms, all above16.67ms. This includes composition and test synchronization, is not a controlled comparison, and does not establish physical-device smoothness. Raw metrics are saved beside the screenshot.

Not implemented: natural open-ended AI conversation, streaming speech/barge-in, protected backend or real-app guidance. Not tested: actual audio, native language quality, calls/Bluetooth/headphones, battery, permission revocation, lock/process-death integration, live overlay correction end-to-end, or older Android fallback. Continue with VOICE_CONVERSATION.md and EXECUTION_PLAN.md; no all-bugs-fixed or full-specification-complete claim.

## Local mock gateway — 30 September continuation

`python3 -m unittest discover -s backend/tests -v`: **19 tests pass**, zero failures/errors/skips, Python3.14 on macOS. This includes four real localhost HTTP tests for authentication, JSON/body restrictions, cancellation authorization and startup token validation; the remaining tests cover concurrent isolated mock calls, disagreement, malformed/stale/private targets, uncertain responses, completion evidence, timeouts, cancellation, newer-screen invalidation, expiry during work, quota reservation, circuit breaking and worker/session bounds. The first sandboxed run passed13 core cases but could not bind localhost (PermissionError); rerunning with approved local-network permission passed17, and the final expanded suite passed19. No failing case was removed or skipped.

No provider calls, credentials, paid service, audio or emulator were involved. Android source was unchanged in this phase, so the earlier41-unit/18-emulator evidence was preserved rather than rerun. `git diff --check` passes. No deployed-backend, durable-quota, real-model independence, native-language-output, Android transport or natural-conversation claim. Mock completion requires a matching synthetic success/category marker; a future app must still verify that the live observation matches before showing it.


## Floating assistant and experimental cross-app guidance — 30 September 2026

Added Home’s **Help in apps & browsers**, a separate typed/dictated intake panel, explicit Text only / Text + voice selection, a draggable branded bubble, notification return, and a local exact-visible-option resolver. The production app never performs taps. Unclear/duplicate targets and detected private screens do not get a guessed target. The resolver handles labelled clickable parents and links without resource IDs; only identical nonempty tap rectangles are deduplicated. Tree-reading limits, remote-node failure handling, obsolete foreground-service shutdown protection, wrapped overlay text and Android cross-app touch opacity were hardened.

The cross-app tests read actual accessibility trees but supply observations manually: the native fixture and Settings pass through the session coordinator; the local WebView exercises the resolver and real overlay service directly. Tests inject taps to verify the underlying controls still work and tap the actual bubble to reopen the panel. This is **not** a test of the enabled Saathi AccessibilityService event pipeline or arbitrary browser compatibility. Text-mode tests cover selection and absence of automatic session/microphone startup; cancellation during an actual spoken utterance remains untested.

**Observed compatibility limit:** Settings force-hides the windows (`mIsForceHiddenNonSystemOverlayWindow=true`, `isOnScreen=false`). Its target still resolves, but the screenshot has no marker/bubble. A separately installed synthetic test APK, with native Help and a local WebView Support link, permits visible overlays and touch-through. No real account, payment, remote page, provider or captured audio was involved.

Earlier runs exposed test defects: forcing a background activity to RESUMED did not bring its task forward; launching non-exported MainActivity from the shell was rejected; and a non-null Kotlin parameter added a runtime check unavailable in the standalone test APK. Tests now use Home plus the appropriate launcher/app-owned return intent and a nullable fixture callback. The separate fixture closes explicitly between checks. The targeted cross-app/voice rerun passed all three checks before the final complete regression run. No failing test was disabled.

Visual review: marker, cursor, floating brand button and wrapped status are visible in the separate-app fixtures. The NEXT STEP badge can overlap surrounding page headings, although the target itself is legible; collision-aware placement remains open. These are static screenshots, not motion or performance acceptance.

Still incomplete: arbitrary multi-step AI, natural streaming/barge-in conversation, Android/backend integration, full panel localization/Figma parity, real AccessibilityService integration, Chrome/Firefox fixtures and real-device background/audio/permission/battery behavior. Existing physical-device performance and older-Android acceptance gaps remain.

Final complete regression before the small panel polish: **49 unit tests and 22 emulator tests pass**, zero failures/errors/skips; debug APK builds; lint **0 errors / 61 warnings**. Evidence: [verification summary](screenshots/2026-09-30-assistant/verification.json), [intake](screenshots/2026-09-30-assistant/assistant-intake.png), [native external fixture](screenshots/2026-09-30-assistant/external-help.png), [local web fixture](screenshots/2026-09-30-assistant/external-support.png), and [Settings overlay restriction](screenshots/2026-09-30-assistant/live-settings.png). The waiting-mode voice service still survives Home, ignores an obsolete Stop action after restart, and shuts down explicitly. No microphone audio or speech quality was tested.

The broad regression frame sample remains **unaccepted**: 173 frames, p50 284.31ms / p95 425.96ms, all above 16.67ms on the software-rendered emulator. This is not a controlled comparison or physical-device performance claim. Screenshot review found a clipped input outline and default purple mode selection; a subsequent panel-only correction adds inner spacing/rounded input geometry and the existing green palette. Terminal session states also no longer display an active-status sentence.

After that panel correction, all **49 unit tests plus both assistant-panel instrumented tests pass**, and build/lint pass again. The exported intake image was refreshed and visually reviewed; the complete 22-test run above preceded this isolated panel change. The latest generated connected XML therefore contains two tests; the saved verification JSON preserves both runs. Changes remain local/uncommitted; no push, paid service, provider call or publish occurred.


## Real AccessibilityService event pipeline — 1 October 2026

Final focused command (listed in the [verification JSON](screenshots/2026-10-01-live-service/verification.json)): **49 unit tests and 4 instrumented tests pass**, zero failures/errors/skips. Debug APK builds; lint **0 errors / 61 warnings**. This focused run includes the new real-service flow, both earlier cross-app component checks and the waiting-mode voice lifecycle check. The full UI suite was not rerun; its prior 22-test evidence remains historical. Backend code was unchanged and its earlier 19-test result was not rerun.

The new test uses Android’s [non-suppressing UiAutomation flag](https://developer.android.com/reference/android/app/UiAutomation#FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES), enables the actual Saathi AccessibilityService on the emulator and restores the prior service/AppOps settings afterward. It does not call the guide or presenter, supply observations, or manually invalidate the screen. A separate test APK exposes native Help/Explore/Back controls and a local WebView Support link. Real OS events produce initial guidance, a no-target detour correction, return guidance, changed-label invalidation, clearing on the Saathi app, WebView guidance/mutation, and explicit Stop. An idle observation remains stable after initial WebView loading settles.

Reviewed screenshots: [native target](screenshots/2026-10-01-live-service/live-service-native.png), [detour correction](screenshots/2026-10-01-live-service/live-service-detour.png), and [web target](screenshots/2026-10-01-live-service/live-service-web.png). The detour removes the target marker and shows the existing back/clarification text; it does not resolve or highlight a new Back control. The known NEXT STEP badge overlap with nearby headings remains open. The established Saathi colors, logo, bubble and overlay styling were preserved; no new design source or product UI was added. Static evidence only; no new timing/performance acceptance claim.

Early test failures were diagnosed rather than skipped: UiAutomation command arguments retained quote characters around the component; bound-service dumps list a service label instead of the expected class; first WebView initialization can still emit legitimate layout events after the first native target; and link labels can live under a clickable parent. Test setup/assertions now reflect those observed platform facts. The production service’s manifest is aligned with [Android’s documented declaration](https://developer.android.com/guide/topics/ui/accessibility/service): exported with the system-only BIND_ACCESSIBILITY_SERVICE permission retained. The tests do not establish that the old exported value caused the setup failures. No other production behavior changed in this phase.

**Remaining:** real Chrome/Firefox coverage, overlay-hidden screens, notification/permission revocation/lock integration, actual microphone/TTS turns, prolonged OEM background behavior, older Android and physical-device latency/battery. Natural multistep reasoning and streaming voice remain unimplemented. This controlled fixture closes one integration gap; it does not certify all apps/browsers or crash-free operation. No real page/account, provider, paid service, audio input or transaction was used. Work remains local and uncommitted.


## Live permission loss — 1 October 2026, next bounded run

Final focused command in [verification.json](screenshots/2026-10-01-permissions/verification.json): **49 unit tests and 2 instrumented tests pass**, zero failures/errors/skips. Debug build succeeds; lint **0 errors / 61 warnings**. This run combines the existing real-service native/WebView flow with the new permission-loss flow. The first standalone permission test also passed; its cleanup was then made robust to a previously enabled Saathi service before the final two-test run. No tests were skipped. Full UI, backend and performance suites were not rerun because production code/design was unchanged.

The new emulator-only test starts real text-only guidance in the external synthetic app, revokes overlay access using AppOps, and verifies the active session, presentation identity and guidance window clear. Restoring overlay access does not resume observation. After an explicit foreground restart, the test revokes Saathi’s AccessibilityService, verifies session/window shutdown, then re-enables the service and verifies no automatic session restart. It restores the original enabled-service list and overlay AppOps. It does not manually inject observations or call Stop to cause those assertions. The observation checks confirm the combined runtime behavior, not one particular callback being the sole cause.

Evidence: [permission result](screenshots/2026-10-01-permissions/permission-loss-result.txt). No new screenshot is needed for a non-visual permission transition. Existing UI, colors, logo, glass and navigation were preserved. No production code changed in this run.

**Browser readiness blocker:** the emulator’s installed Chrome opens its first-run screen with a Terms of Service/usage-data notice, an Add account option and Stay signed out. No consent was submitted and no account was added. Real Chrome page compatibility remains untested pending browser setup; the previous WebView evidence must not be relabelled as Chrome coverage. [Readiness record](screenshots/2026-10-01-permissions/browser-readiness.txt). The next continuation can work on lock/notification tests or Android/mock gateway contracts while browser setup remains pending.

Still unverified: permission revocation during actual microphone capture/TTS, Android/OEM differences, lock/process-death integration, prolonged background operation and physical-device battery/performance. No natural AI, streaming conversation, universal browser or crash-free claim. Changes remain local/uncommitted; no publishing, paid services or provider calls.


## Travel privacy, cyber-fraud reporting and backend admission — 2 October 2026

Final focused build: **66 Android unit tests, 33 backend tests and 12 emulator checks passed**, zero failures/errors/skips. Debug APK builds; Android lint **0 errors / 60 warnings**. The emulator run covers the four reporting-screen checks, copy-overlay consent/revocation, a real accessibility-tree travel/OTP fixture, two assistant checks, the actual AccessibilityService flow and three shared-UI/large-Hindi-text checks. It is not the entire historical UI suite. Backend tests include actual localhost HTTP and synthetic provider payloads, without paid/external provider calls.

[Verification summary](screenshots/2026-10-02-cyber-privacy/verification.json), [focused test XML](screenshots/2026-10-02-cyber-privacy/focused-tests.xml), [light reporting screen](screenshots/2026-10-02-cyber-privacy/cyber-light.png), [dark reporting screen](screenshots/2026-10-02-cyber-privacy/cyber-dark.png), [floating copy consent](screenshots/2026-10-02-cyber-privacy/cyber-floating-consent.png).

The native fixture exposes From/To, 01/10/2026, ₹5221, ₹6,398, a time and Shopping. Real Android trees retain those labels and resolve noneditable targets; adding an OTP input masks its value and removes the guidance target. This does not reproduce the current IRCTC/Google Flights browser DOM or the user's phone. Regression units cover sensitive metadata priority, date-of-birth/account-number labels, adjacent secrets and URL-contained numbers. Both Android and backend live-label rules are exercised.

The reporting checks verify text default/no running session, explicit approve versus decline before changing the clipboard, content-sized short buttons, draft clearing on recreation with checklist position retained, bounded Back/Next, light/dark appearance and denied-overlay fallback. The overlay check uses a separate synthetic app: refusal leaves the test clipboard intact, approval copies only the fixed portal URL, and permission revocation removes the helper. Screenshot protection was disabled only in the test on the empty synthetic reporting screen to capture theme evidence; production keeps it enabled. No personal incident or real transaction was entered. Actual microphone/TTS delivery, lock/timeout paths of this new helper, physical devices, very large-font overlay scrolling and browser/OEM differences remain unverified.

Issues encountered: the first new travel fixture used a Kotlin collections helper absent from the separately installed test APK; it now uses an array loop and passes. Visual review found dark text inheriting the wrong content color; shared theme content color was applied, rerun and reviewed. One earlier mixed run missed the existing WebView post-tap transition. Its assertion now first checks that the fixture link actually changed; the isolated run and final 12-test run passed. The cause of that intermittent failure is not conclusively established and should remain in device/regression follow-up. No failing test was skipped.

Backend fixes are specifically tested: public-display privacy exceptions, malformed provider target/explanation types, stale admission before model call reservation, expiry during work, and inactive-session eviction without resetting quotas. Existing cancellation/disagreement/quota/bounded-worker checks remain passing. This is not a guarantee that every backend edge case is fixed or that hosted authentication/TLS/release connectivity works.

Official portal page loads timed out or returned gateway errors; official indexed source extracts supported the reporting checklist. No live complaint walkthrough, CAPTCHA, OTP, upload, final submission or status confirmation was attempted. Incident triage is explicitly local/limited, not model-backed classification. No credentials, paid services, publishing or Git push. See [CYBER_FRAUD_REPORTING.md](CYBER_FRAUD_REPORTING.md) for sources and remaining scope.

Follow-up after the 12-case run: the overlay test was strengthened so **both refusal and approval happen while the separate app is foreground**. It passed on its own (1 test, zero failures/errors/skips); [result](screenshots/2026-10-02-cyber-privacy/external-copy-approval-tests.xml). Production code was unchanged. The original focused results remain saved rather than overwritten by this targeted rerun.


## Browser follow-up and whitespace bounds — 2 October 2026

**67 Android unit / 34 backend / 2 focused emulator tests pass**, zero failures/errors/skips. Debug APK builds; lint 0 errors / 61 warnings. [Verification](screenshots/2026-10-02-browser-followup/verification.json), [browser XML](screenshots/2026-10-02-browser-followup/browser-tests.xml), [Chrome travel screenshot](screenshots/2026-10-02-browser-followup/chrome-public-travel.png), [tap geometry](screenshots/2026-10-02-browser-followup/tap-geometry.txt).

Fixed an actual validation gap: length checks used trimmed length while forwarding untrimmed fields. Android live goals/labels/history and backend live fields/provider explanations now enforce actual string length plus nonblank content. Boundary/padded/whitespace-only regressions pass; no external model calls occurred. Established app UI and theme are unchanged.

Chrome now exercises public To, 01/10/2026, ₹5221 and ₹6,398 on a localhost synthetic page through the real AccessibilityService. It verifies no privacy flag, destination and date retargeting, removal on page departure, password-screen suspension, detour/return, link mutation and Stop. This is not a reproduction on IRCTC/Google Flights or a cybercrime complaint portal walkthrough. The screenshot still shows the previously known NEXT STEP badge overlapping a nearby heading; no overlay-placement fix is claimed.

The initial Chrome run hit first-run setup after emulator restoration, not the fixture. Setup was completed signed out, optional usage reporting switched off and notifications declined. One cold WebView run failed its initial idle assertion while the presentation revision changed during startup. The test now requires a bounded one-second settling interval before retaining the original separate one-second idle assertion; endless refresh still fails. The final paired Chrome/WebView run passed. The earlier post-tap mutation failure did not reproduce; geometry diagnostics now record tap areas, and failure diagnostics capture window/geometry state. That original intermittent cause is still not established and must not be described as a production fix. No assertion/test was skipped.

A direct retry of the official cybercrime portal again timed out. Live complaint navigation, real AI incident assessment, successful microphone input and production/provider/hardware acceptance remain open. Previous 12-test reporting/UI results are retained separately and were not rerun because those screens were unchanged in this continuation.
