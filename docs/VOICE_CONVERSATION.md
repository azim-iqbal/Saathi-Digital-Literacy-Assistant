## 9 October 2026 — Final mandate continuation (verification in progress)

Latest engineering/evidence report: [PREDEPLOYMENT_VALIDATION.md](PREDEPLOYMENT_VALIDATION.md). This section supersedes older counts and next-step lists below; historical records are retained.

Implemented: bounded primary attempt/fallback within the original screen deadline; Groq-first navigation default (explicit config preserved); private-safe phased HTTPS diagnostics; narrative private handoff; explicit prerequisite/denial guards; accurate capability copy using unchanged styles; controlled portal/copy/paste/rotation/lock tests; allowlisted non-root pilot container template.

Verified: 147 Android unit tests, 165 backend tests, 9 evaluation tests, 51/51 probes, 2,000 recovery plus 2,000 pre-cancelled synthetic requests, six local Gunicorn checks, zero lint errors/61 warnings, debug/test builds and release compilation. No configured-key matches in source/non-ignored files or decompressed APKs. Current emulator reruns and two-hour target must be read from `test-evidence/2026-10-09-final-mandate/`; do not call ongoing runs complete.

New eight-call allowance EXHAUSTED: English/Hinglish paired navigation passed; Gemini planning/incident timed out waiting for headers, and Groq failed independent plan/signal expectations. Post-run reasoning guard/prompt changes are offline-tested only. No more live calls without new scoped authorization. Physical audio/OEM, legitimate protected portal, browser-dependent full HTTPS origin, historical post-tap causality and deployment acceptance remain unresolved; do not declare production readiness.

## 9 October privacy verification

See FINAL_RELIABILITY_REPORT.md for the current passing evidence and remaining blockers. Editable/private value getters are skipped; private context content is discarded before session storage. A private/challenge handoff requires explicit microphone reactivation. Original task continuity and structure-only private markers have emulator evidence; actual speech and OEM behaviour remain unverified.

## 8 October 2026 update

Source companion lifecycle/browser acceptance and additional security/privacy fixes are recorded in [the latest handoff](NEXT_CONTINUATION.md) and [evidence index](test-evidence/2026-10-08-browser/README.md). Semantic private contexts now use the existing private/listening handover. Certificate warnings block local targets and cloud dispatch; raw warning controls are rejected server-side. Overall workflow/hosting/device gates remain open. Earlier dated counts below are historical.

## 7 October 2026 — Research architecture and deterministic remediation (overall work open)

The accepted pause/resume and challenge handover regressions remain passing. This phase adds no evidence of actual microphone/TTS delivery, natural streaming conversation or OEM background survival. Research pauses active guidance and uses explicit sharing/review; keyboard content is excluded before tree copying. Physical speech remains a manual gate.

Evidence and remaining gates: [remediation matrix](REMEDIATION_CAPABILITY_MATRIX.md).

## 7 October 2026 — Retained-task pause/resume and private challenge handover

Final verification: **95 Android unit tests and all 10 focused emulator tests pass**, debug/test builds and release Kotlin compile pass. The added immediate-start/pause UI test found a real `ForegroundServiceDidNotStartInTimeException`; fixed by acknowledging pending foreground starts before service shutdown (promotion alone was insufficient). No production guidance/audio services remained after the final tests; the separate fixture WebView service remained until emulator shutdown. Evidence: `test-evidence/2026-10-07-resumption/verification.json`, `final-regression.txt`, `paused-ui-acknowledged.txt` and screenshots.

Latest user priority: reliable resumption. Implemented memory-only paused task settings, explicit permission/unlock/service checks, fresh session identity/current-tree request, same-theme Resume/Discard controls and paused speech preference preservation. Private/password and CAPTCHA screens now intercept guidance before gateway dispatch, disable listening, preserve the original goal and re-observe to resume after a safe screen returns. Manual Pause never auto-resumes; Stop, permission-loss safety stops and process death do not revive tasks.

95 Android unit tests and debug/test/release Kotlin builds pass. Nine focused emulator regression tests pass, including three OTP-like/private and three CAPTCHA round trips plus three rapid manual cycles, missing-permission refusal with retained task, stale identity rejection and Stop cleanup. Additional paused-screen UI evidence is in `test-evidence/2026-10-07-resumption/paused-ui.txt`. See PAUSE_RESUME.md for limits and failed fixture attempts: separate test APK needed array iteration instead of unavailable Kotlin collections runtime. No live provider calls, deployment or push.

Broader implementation brief saved at specs/IMPLEMENTATION_REMEDIATION_REQUEST.txt; research/planning/eligibility/provider diagnostics/browser readiness remain unfinished. Actual speech/device/OEM and real-site auth/CAPTCHA acceptance remain unverified. Preserve all existing evaluation fixes and evidence.

# Background conversation — 30 September 2026

## Latest backend/browser continuation — 1 October 2026

Debug Android now connects to the loopback gateway for mock practice and separately consented AI navigation. Independent Gemini/Groq REST adapters and ignored credential/model placeholders are implemented. A live proposal is accepted only on paired agreement, a current eligible control and fresh observation. The app resolves bounds locally and uses its localized instruction template. Recent suggested labels support step-by-step wrong-path recovery, but are not claimed clicks or completion evidence. Default/release guidance stays local. See [AI_ORCHESTRATION.md](AI_ORCHESTRATION.md) and [SETUP.md](SETUP.md), which supersede older disconnected-backend notes below.

Actual Chrome on the API37 emulator passed a localhost synthetic-page flow: highlight/tap-through, detour/return, private password screen suspension, retargeting and Stop. The live Android/backend route passed wrong-path recovery using two **simulated** provider adapters. No provider account was configured, no external model request made, and no real account/transaction used. The user chose emulator testing for now.

Voice setup now offers an explicit installed-language check/download through the device speech service, with the existing brand, palette and glass controls. TTS completion callbacks succeeded in all3 languages; recognition returned missing-language error13. No intelligibility, microphone transcript or successful language download is claimed. Natural streaming/barge-in, physical-device/OEM survival, release backend connectivity and hosted authentication/TLS remain open.


## Core conversation continuation — 1 October 2026

The latest user priority is core app functionality first, with the established UI retained and shared by new interfaces. Live sessions can now change the requested visible option without restarting the foreground session. The assistant panel uses **Update on-screen help** for an active live session. Hands-free recognition can hand off explicit requests such as “find Help”, “Help dhundo” or “मदद खोजो”; ordinary conversation does not silently replace the goal. These are deterministic commands, not general AI reasoning.

Every accepted change invalidates the old screen/audio work and asks the actual AccessibilityService for a fresh current tree, even if the other app emits no event. Session identity stays the same; stale presentation/session requests, stopped sessions, private-screen voice requests and refused option labels do not change the goal. The scheduled reader also rechecks keyguard before copying. Only a validated requested option is retained as the session goal in memory; raw audio/transcript history is not persisted. Voice notifications now reopen the assistant panel.

`SaathiColors.kt` and `SaathiBrand.kt` share the existing main-app palette and vector header with AssistantActivity. Main palette values and header geometry are unchanged; assistant chips use the same green containers, and existing glass buttons/panels remain. The assistant respects system reduced motion and the main shell's maximum content width. Its glass remains an opaque fallback without sampling other apps. Panel localization, Figma parity, native/legacy surfaces and broader accessibility/device checks are still pending.

Verification is recorded in TEST_RESULTS.md. Parser tests and direct request handoff are distinct from actual speech recognition: microphone/TTS delivery, natural streaming conversation, real browser compatibility and OEM survival remain unverified. No model or paid service was enabled.


## Implemented scope

From an active session, **Start voice & open practice** requests microphone and notification permission while the activity is visible, starts a microphone foreground service, and opens the synthetic practice screen. The foreground service continues when the main activity goes into the background. It does not start on boot, restore itself after process death, or turn on because a speech preference was saved.

The current conversation is a deterministic practice assistant. It speaks the current step and then starts an on-device recognition turn. A recognised reply can trigger another spoken reply and listening turn without another button press. English, Hindi and Hinglish command phrases cover repeat, help, understood, pause and stop. Saying “done” only acknowledges the user; completion still requires the synthetic success screen. Unknown requests get an honest local-guide explanation. Raw transcripts are not persisted or kept as conversation history. An explicitly requested, validated visible option can now become the in-memory live-session goal and appear in guidance; see the latest continuation above.

This is **not yet natural, open-ended AI conversation**. A debug reasoning connection is implemented but not tested with real credentials. Streaming speech, simultaneous speech/listening and barge-in remain absent; cross-app evidence is limited to synthetic fixtures and Chrome on a localhost page. The new service requires Android 12+ with an available on-device recognizer; older or unsupported devices keep visual and optional spoken guidance. Installed recognition language packs may still be missing. Task-intake voice continues to use the user's Android speech service separately.

## Lifecycle and privacy

- Service/notification actions carry a session identity. Audio callbacks carry turn identities; old TTS completions and cancelled recognition cannot resume a newer turn.
- The notification shows preparing, speaking, listening, waiting, private-form or paused status. Only the recognizer's ready callback says “Listening”. Pause voice and Stop guidance remain accessible in the notification; the app also has a conversation-off control.
- On a screen change, cancel speech and recognition immediately. Resume only after the current supported snapshot is validated. Unsupported packages are rejected before their accessibility trees are flattened.
- Private forms allow static local spoken instructions, but no microphone turn. The recognizer is suspended for the whole form, not merely when a secret field receives focus.
- Audio-focus loss and headphone disconnection pause voice. Screen-off/lock stops the guidance session. Returning does not silently resume a paused microphone.
- A recognition error or silence timeout pauses instead of retrying indefinitely. Each speech/listening turn has a 30-second watchdog. A successful conversational exchange can continue through repeated turns.
- Speech speed follows the session setting. TTS requests preserve completion callbacks during initialization, reject stale utterance callbacks, and fail visibly if the selected language is unavailable.
- No source audio is written to a file by Saathi. This does not certify every vendor's speech engine or device.

The API constraints were checked against the official [SpeechRecognizer reference](https://developer.android.com/reference/android/speech/SpeechRecognizer) and [foreground service restrictions](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start). Android's recognizer is designed for speech turns, not indefinite continuous recognition. A future natural streaming mode needs a separately tested speech transport and protected reasoning gateway.

## Recovering from a different path

The synthetic fixture exposes stable category markers and an actual Back to choices button. A mismatched category or unrelated practice tile produces a corrective instruction anchored to that button. After the user returns, the guide points to the original chosen category. It never taps Back or changes the user's selected goal itself. Partial trees with no recovery control produce no invented target and no exception.

Synthetic form recreation retains the category but clears entered values, including the made-up PIN. The success screen for a different category cannot complete the originally selected task.

## Still required

Verified this phase:41 unit tests and18 emulator tests pass, including real-tree corrective navigation, category/form recreation, persisted speech speed, and background waiting/Stop/replacement-notification behavior of the real foreground service. The emulator has an on-device recognizer, but the service test intentionally does not feed or record speech. See [TEST_RESULTS.md](TEST_RESULTS.md) for evidence and the remaining performance failure.

Actual audio conversation, Hindi/Hinglish recognition quality, TTS intelligibility/latency, audio-focus/phone-call/Bluetooth behavior, battery use, and real microphone permission-revocation need physical-device testing. Backend authentication, independent provider validation, current unpaid data terms, quotas, streaming cancellation and grounded compatibility tests remain in the execution plan. No paid service or provider has been enabled.
