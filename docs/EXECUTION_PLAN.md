## Current priorities — 3 October 2026

The connection checks, complaint worksheet and per-field copy flow are implemented. Both real providers have returned valid responses, but the last paired request timed out on Groq. See [provider verification](PROVIDER_VERIFICATION.md) for the exact evidence. Next work is provider latency and genuine guidance evaluation, followed by the remaining browser, voice and production-readiness items below.

## Completed continuation — 3 October 2026, connection evidence and complaint drafts

Implemented in the debug local pilot: authenticated server-status refresh (no model request), explicit two-provider API smoke check, independent Gemini/Groq outcomes and token metadata, safe recovery, and a reviewed complaint worksheet with per-field clipboard consent and an optional floating helper. The user clarified that they **removed the keys**; no live Gemini/Groq call was made. Test fixtures remain explicitly labelled. Main navigation, logo and existing primary palette are unchanged; shared secondary/tertiary defaults now use the existing greens instead of inherited Material purple.

Final checks: **75 Android unit, 54 backend and 12 focused emulator tests pass**; debug/test/release builds pass, lint 0 errors / 61 warnings. See TEST_RESULTS.md and NEXT_CONTINUATION.md. The local draft builder uses only supplied facts, not invented/model-written testimony. Private-form auto-paste, authenticated portal forms/submission, real model accuracy, hosted production/release connectivity, continuous natural speech and the original WebView post-tap root cause remain unfinished or unverified. Do not claim the full application is ready.

The older “Next scheduled priority” immediately below is historical: diagnostics and the safe manual-copy phase are now implemented. Follow the updated NEXT_CONTINUATION.md rather than rebuilding them.

## Next scheduled priority — latest user direction, 3 October 2026

Read [NEXT_CONTINUATION.md](NEXT_CONTINUATION.md) first. The user now reports configuring API keys but seeing no provider usage. First prove the real Android/backend/Gemini/Groq path with clear per-provider diagnostics and bounded, explicit checks. Then implement fact-grounded complaint drafts, user corrections and per-field copy/paste consent with return-to-browser guidance, preserving the existing theme. Do not treat the older no-credentials statements below as current fact; live provider connection remains unverified. No new paid service or automated final complaint submission is authorized.

## Latest continuation — 3 October 2026, backend/reporting reliability

Backend failure paths now distinguish storage failure, quota, authentication, provider throttling, timeout and malformed output. Added bounded early cancellation, failed-worker cleanup and strict incident-assessment contracts. Android provides reviewed recovery copy and per-use summary sharing in the existing reporting UI; offline reporting remains available. API/model placeholders are unchanged and no live provider was contacted.

Verified the **public** financial reporting route in emulator Chrome at https://cybercrime.gov.in/. The www hostname failed certificate validation in this environment; fixed app links to the working official apex hostname without bypassing TLS. The walkthrough reached the public filing explanation at `/Webform/Accept.aspx`, not a filed complaint.

Reproduced an idle presentation instability and corrected own-overlay `TYPE_WINDOWS_CHANGED` classification by attached window identity, regardless of event package metadata. Final service tests pass. **The original intermittent post-tap WebView failure's root cause remains unestablished**; retain diagnostics and the failing idle run separately.

Final checks: **71 Android unit, 46 backend, 11 focused emulator tests pass** across isolated mock and synthetic-paired suites; debug build passes, lint 0 errors / 61 warnings. See TEST_RESULTS.md and screenshots/2026-10-03-backend-reporting/verification.json. All providers in tests are synthetic. Next: real configured-provider assessment/navigation evaluation (credentials unavailable), production authentication/TLS/per-user accounting and release connectivity, remaining WebView cause investigation, and voice/device acceptance. Preserve current colors, logo, navigation and glass buttons. Do not repeat the already completed transport/consent phases.

## Latest continuation — 2 October 2026, browser follow-up

Fixed padded-field length bypasses in Android/backend validation. Final 67 unit, 34 backend and 2 focused browser checks pass; see TEST_RESULTS.md and screenshots/2026-10-02-browser-followup. Chrome now covers the public travel/date/fare fixture and private-form handover. Emulator browser setup is complete for this running emulator, but restoring an old snapshot may reset it. The original intermittent WebView post-tap failure remains unproven; diagnostics and bounded startup settling were added. Real cybercrime portal access still times out; genuine AI assessment, production/provider connection and hardware voice/OEM acceptance remain pending. Preserve existing theme. Next: current public portal validation when reachable, model-backed assessment contract with explicit data consent, and the remaining backend/voice/device acceptance gates. No real model or paid service enabled.

# Latest user priority — 2 October 2026

Read [CYBER_FRAUD_REPORTING.md](CYBER_FRAUD_REPORTING.md) for the new reporting companion, corrected date/fare privacy rules, optional consent-based floating link helper and backend admission/session fixes. Keep shared theme/buttons. Do not claim the checklist is a verified live portal workflow or a trained fraud classifier. Pending priority: current portal public-step validation and real-site/device privacy reproduction, then provider/production-backend and voice gaps below. No credentials or paid services are configured.

# Resume here — 1 October 2026

## Latest backend/browser continuation — 1 October 2026

Debug Android now connects to the loopback gateway for mock practice and separately consented AI navigation. Independent Gemini/Groq REST adapters and ignored credential/model placeholders are implemented. A live proposal is accepted only on paired agreement, a current eligible control and fresh observation. The app resolves bounds locally and uses its localized instruction template. Recent suggested labels support step-by-step wrong-path recovery, but are not claimed clicks or completion evidence. Default/release guidance stays local. See [AI_ORCHESTRATION.md](AI_ORCHESTRATION.md) and [SETUP.md](SETUP.md), which supersede older disconnected-backend notes below.

Actual Chrome on the API37 emulator passed a localhost synthetic-page flow: highlight/tap-through, detour/return, private password screen suspension, retargeting and Stop. The live Android/backend route passed wrong-path recovery using two **simulated** provider adapters. No provider account was configured, no external model request made, and no real account/transaction used. The user chose emulator testing for now.

Voice setup now offers an explicit installed-language check/download through the device speech service, with the existing brand, palette and glass controls. TTS completion callbacks succeeded in all3 languages; recognition returned missing-language error13. No intelligibility, microphone transcript or successful language download is claimed. Natural streaming/barge-in, physical-device/OEM survival, release backend connectivity and hosted authentication/TLS remain open.


## Core conversation continuation — 1 October 2026

The latest user priority is core app functionality first, with the established UI retained and shared by new interfaces. Live sessions can now change the requested visible option without restarting the foreground session. The assistant panel uses **Update on-screen help** for an active live session. Hands-free recognition can hand off explicit requests such as “find Help”, “Help dhundo” or “मदद खोजो”; ordinary conversation does not silently replace the goal. These are deterministic commands, not general AI reasoning.

Every accepted change invalidates the old screen/audio work and asks the actual AccessibilityService for a fresh current tree, even if the other app emits no event. Session identity stays the same; stale presentation/session requests, stopped sessions, private-screen voice requests and refused option labels do not change the goal. The scheduled reader also rechecks keyguard before copying. Only a validated requested option is retained as the session goal in memory; raw audio/transcript history is not persisted. Voice notifications now reopen the assistant panel.

`SaathiColors.kt` and `SaathiBrand.kt` share the existing main-app palette and vector header with AssistantActivity. Main palette values and header geometry are unchanged; assistant chips use the same green containers, and existing glass buttons/panels remain. The assistant respects system reduced motion and the main shell's maximum content width. Its glass remains an opaque fallback without sampling other apps. Panel localization, Figma parity, native/legacy surfaces and broader accessibility/device checks are still pending.

Verification: 52 unit tests and 5 focused emulator checks passed; debug build and lint pass (0 errors / 61 warnings). Evidence is in screenshots/2026-10-01-live-requests. Parser tests and direct request handoff are distinct from actual speech recognition: microphone/TTS delivery, natural streaming conversation, real browser compatibility and OEM survival remain unverified. No model or paid service was enabled.


**Newest user priority: floating assistant and scope expansion, 30 September.** Read LIVE_ASSISTANT.md first. An explicit Home entry now offers typed/dictated requests, Text only / Text + voice, and a draggable Saathi bubble with a notification return path. Live mode locally matches one visible option across accessible external packages; practice stays separate. It never infers a whole arbitrary task or completion. Natural multi-step reasoning, universal compatibility, streaming conversation and guaranteed background survival are NOT complete. Accessibility trees and output remain local; no network service enabled. Preserve earlier navigation/voice/gateway changes. Pending: new panel localization and Figma parity, real-browser/other-app compatibility beyond the tested fixture, physical-device background/audio behavior, and the backend/streaming backlog below.

**Latest verification for the floating assistant phase:** 49 unit / 22 emulator tests pass in the complete run. Final panel visual polish was followed by 49 unit / 2 targeted panel passes; build succeeds and lint has 0 errors / 61 warnings. See TEST_RESULTS.md and screenshots/2026-09-30-assistant/verification.json for exact coverage and limitations.

**Latest bounded phase — 1 October:** the actual enabled AccessibilityService → snapshot → session → overlay path is now tested in the separate synthetic app and its local WebView. Real events cover detour/return, disappearing labels, own-app clearing, stable idle observations and explicit Stop, with no manual snapshot injection. The service declaration retains system-only binding permission and follows Android’s exported-service guidance. Existing colors, logo, navigation and glass remain unchanged. Final checks: 49 unit tests and 4 focused instrumented tests pass, debug build succeeds, lint 0 errors / 61 warnings. See TEST_RESULTS.md and screenshots/2026-10-01-live-service for evidence; full UI/browser/audio/device coverage remains pending.

**Latest follow-up — live permission loss:** the real emulator session now has integration coverage for overlay AppOps revocation and AccessibilityService revocation, clearing session/window state and requiring an explicit restart after permission restoration. Final 49 unit / 2 focused instrumented checks pass; build/lint pass (0 errors / 61 warnings). No production UI or code changed. See TEST_RESULTS.md and screenshots/2026-10-01-permissions.

**Next bounded phase (core functionality first):** reconcile Android/mock gateway contracts, then a debug-only local transport with strict observation-age, cancellation, grounded-target and completion-evidence checks. This is the remaining groundwork for real reasoning; do not present deterministic retargeting as multi-step AI. Keep the shared SaathiColors/SaathiBrand/glass system for new UI. Existing real-service, request-change and permission-loss checks should regress alongside runtime changes. Chrome still awaits first-run setup; actual microphone/TTS and physical-device reliability remain open. No provider, paid service or automatic background restart is authorized by this implementation.

**Local mock gateway:** `backend/` provides a loopback-only, bearer-authenticated mock API with strict public-ID requests, paired independent calls, deadline/cancellation/latest-screen handling, process-local quotas, bounded worker/session capacity, circuit breaking and completion-evidence validation. **19 Python tests pass**, including actual localhost HTTP checks. No provider key, external service, paid spend or Android network integration was enabled. See SETUP.md and AI_ORCHESTRATION.md. Next backend step: reconcile Android's proposal contract with observation age/evidence and test a debug-only mock transport before any real adapter. Production user auth, durable budgets, TLS, provider terms/capabilities, real model calls and natural speech transport remain unfinished. Previous Android 41-unit/18-emulator results are retained.

**Voice conversation phase:** Read [VOICE_CONVERSATION.md](VOICE_CONVERSATION.md) and the latest TEST_RESULTS entry first. Background voice is now explicitly user-started and integrated for local practice, with turn-based on-device recognition, notification controls, session/utterance cancellation, and private-form suspension. Wrong-category/detour recovery now points to a real Back to choices control. Natural open-ended AI, streaming/barge-in and real-app guidance remain unimplemented; do not advertise them as working or enable a cloud provider without the gateway/data-term gates.

Latest verification: **41 unit tests + 18 emulator tests pass**, debug build succeeds, lint 0 errors / 57 warnings. Real foreground-service waiting/background/replacement-notification behavior passed on an on-device-recognizer-capable API37 emulator; actual speech was not exercised. Real-tree recovery and form recreation passed. Evidence is in `docs/screenshots/2026-09-30-voice`. Physical-device performance remains unaccepted.

Complete product requirements and architectural goals: [Product Specification](specs/PRODUCT_SPEC.md). Latest design constraints: [DESIGN_REFERENCES.md](DESIGN_REFERENCES.md).

## Completed

Phase 1: audit; local-practice safety restriction; removed direct provider/credential path; capture disabled; observation identity and stale overlay checks; notification/screen-off Stop; memory-only conversation stores; conservative multilingual masking; regression tests and build/lint repairs.

Phase 2: inspected four local archive previews and accessible Grabber source frames. Reworked Saathi Figma tokens; two component sets; seven light and seven dark editable screens. Compose welcome, home, practice, task intake, setup, session controls, settings and privacy. English/Hindi/Hinglish shell copy, theme persistence, optional speech/haptics/reduced motion, user-triggered voice transcription with typed fallback, permission links and readiness gating, pause/resume/stop controls, confirmed local-data reset. Legacy synthetic practice activity retained. Selected synthetic category survives activity recreation; free-form task text is not persisted.

Prior phase verification: 20 unit tests pass, debug APK builds, lint 0 errors/57 warnings. Four emulator tests cover routing/readiness, theme screenshots, Hindi 200% scrolling, onboarding language and privacy deletion. See TEST_RESULTS.md for exact final run and limitations. Home Figma screenshots were reviewed in both themes; most Figma frames still need individual checks.

## Next sessions, in order

**Navigation phase completed:** floating dock, spring selection, swipe-connected Practice tabs and fallbacks are implemented. Ten emulator tests pass. Remaining navigation validation: poor emulator frame timings require physical-device profiling and blur-on/off comparison; TalkBack, API26–30 and system reduced-motion release behavior are unverified. See [motion/results](MOTION_AND_HAPTICS.md). Resume from these open items when hardware is available, then the backlog below; do not redo completed components.


1. Lifecycle/state-machine code pass completed: notification Stop/restart identity, same-package fixture eligibility, empty-tree outcome, TTS queue completion, and preparing/observing/analysing/guiding/waiting/private-handover/paused/completed/error UI states are implemented. Still required: physical-device notification Stop/restart, overlay/service races, TalkBack loops, keyguard/permission revocation, actual TTS cancellation and speech-engine behavior. Keep cloud/capture disabled. Measure baseline performance.
2. Finish design parity: Figma intake/error/completion/overlay frames, Hindi/Hinglish/large-text variants, prototypes/motion, principles/handoff. Reconcile text styles, feature padding/radii, buttons and fixed bottom navigation against Android. Inspect live state and ledgers before writes. File https://www.figma.com/design/vx2M28p625yZQTAzcTLlQj . Do not recreate tokens/components. Exact local .fig layers and motion remain unparsed; user authorized proceeding with previews and accessible links.
3. Complete deterministic state machine, grounded targets, overlay touch behavior and SDK migration. Add meaningful instrumented fixtures and low-risk non-demo workflow tests before enabling real apps. No payment testing.
4. Local mock gateway and zero-spend failure tests now implemented; see SETUP.md. Remaining: production authentication/TLS, durable server quotas, Android integration, strict live adapter decoding and independent Gemini/Groq proposals. Verify current official models, pricing and unpaid data terms before cloud enablement. No secrets in APK.
5. The local background voice adapter, speed preview, and corrective practice guidance are implemented; see VOICE_CONVERSATION.md. Still required: natural open-ended AI via the protected gateway, streaming speech transport and interruption/barge-in, native English/Hindi/Hinglish review, and actual microphone/TTS/hardware testing. Conversation mode uses the on-device recognizer only; separate task-intake transcription uses the user's speech service. Speech speed takes effect at session start. All current instruction surfaces are opaque.
6. Accessibility/performance/device matrix, remaining acceptance gates, comprehensive docs, recorded demo and reviewable draft PR. No paid services, publishing, merge or messaging others.

## Roadmap & Development Milestones

Development is organized across bounded milestones covering Android Compose UI refinement, on-device accessibility service hardening, local test gateway infrastructure, and multilingual voice navigation. Verification status and test logs are tracked in [TEST_RESULTS.md](TEST_RESULTS.md).


## Latest navigation handoff

New navigation source is in app/src/main/java/com/saathi/ui/navigation. SaathiApp retains its single Screen state; MainActivity now enables edge-to-edge. No new dependencies, cloud enablement or unrelated guidance changes. Current verification:20 unit tests;10 emulator tests; debug build and lint pass (0errors/57warnings). The last targeted performance rerun contains one test and overwrites the generated connected-test report, so the full ten-test success is also recorded in TEST_RESULTS.md. Figma sets24:86 and24:189,12 variants,24 selection preview links;12 existing screens use the dock. Detailed Practice frame composition and large-font Figma examples remain pending. Current navigation screenshots and test-run recording are in docs/screenshots/2026-09-29-navigation. No physical device was attached.

## Permission-loss safety follow-up (29 September)

Implemented AppOps monitoring during guidance, a startup permission check, current-presentation-scoped shutdown on denied overlay/attachment failure, and attachment-scoped animation cleanup. The 21-test unit suite passes; the new test covers stale presentation identity after invalidation and restart. Live permission revocation, Android/OEM callback delivery and overlay attachment failure still need instrumentation/device checks. Existing 10 navigation UI passes predate this safety patch.

Latest user addition: [Liquid Glass brief](specs/Liquid-glass-brief-2026-09-29.txt) and three new authorized reference images in design-references/glass. The shared Compose glass control/header pass and native-button fallback are implemented; see GLASS_UI.md for the explicit inventory, unconverted historical controls, Figma parity and verification backlog. Usage reset during this explicit user turn; heartbeat still keeps its 25% floor.

Latest UI continuation: read [GLASS_UI.md](GLASS_UI.md) before editing. Finish its remaining parity/verification items before broader scope. The daily automation now includes all three new reference images and the new brief.

Latest verified totals:21 unit tests and11 emulator UI tests passed; debug build and lint pass with57 warnings. Permission revocation and physical-device performance remain unverified; software-emulator performance remains poor. Test evidence:docs/screenshots/2026-09-29-glass and TEST_RESULTS.md. All changes remain uncommitted.

## Launch identity phase

See LAUNCH_EXPERIENCE.md: shared vector S assets, adaptive/monochrome launcher, centered breathing system mark,1.2s reveal and launch lifecycle cleanup. LaunchActivity now renders directly, avoiding its old redirect. Notification Stop is now bound to session identity; older queued actions cannot stop a restarted session. Preserve these changes on continuation. Verification is recorded in TEST_RESULTS.md.

Latest launch-phase verification:22 unit tests,13 emulator UI tests, debug build and lint pass;60 lint warnings remain. Vector assets and startup recording saved in design/brand/saathi-v2 and docs/screenshots/2026-09-29-launch. See LAUNCH_EXPERIENCE.md and TEST_RESULTS.md for untested conditions. Continue outstanding app fixes from this state; do not rebuild completed logo/navigation.

## Local-practice state safety follow-up

`PracticeSurfacePolicy` now permits the deterministic guide only on Saathi's registered synthetic Bill Pay controls, including Water and DTH choices. Unrelated, empty and unregistered Saathi screens clear the overlay and speech instead of showing a status panel over another app. The Compose session UI now explains waiting, private-field handover and synthetic completion, and refuses to navigate to a session if start fails or the selected task is refused. Generic completion-category persistence was removed. TTS now retains its completion callback when a request arrives before engine initialization. Verification: 25 unit tests pass; debug APK builds; lint has zero errors and 61 warnings. No emulator or physical-device lifecycle run has been repeated for this change yet.

## Zero-spend proposal validation foundation

Added a local `DualProposalValidator` and schema-like immutable snapshot contract. It has no provider implementation, network call, key, budget or runtime integration. It accepts only two agreeing, current proposals for a locally eligible node and rejects timeout, quota, unavailable, malformed, stale, invalid-target and disagreement cases. Seven unit tests pass for this contract and no single-provider fallback exists. The old unused direct-provider rate limiter and generic website guide were removed. The remaining backend phase still needs a protected gateway, real adapters, server-side authorization/quotas, deadlines/cancellation, primary-source provider verification and full integration tests.

## Speech settings and dormant-service safety

Settings now persist Slow (0.75x), Standard (0.9x) and Fast (1.25x) speech rates and provide a local preview using the selected installed engine. Session prompts use the selected rate. A pre-initialization TTS request retains its completion callback. The dormant foreground microphone service still is not started by the pilot session; if it is later used, it now checks recording permission, refuses sensitive prompts, accepts Cancel and never retries listening automatically after an error. Actual preview/TTS/microphone engine behavior and audio-focus handling remain device tests, not completed evidence.
