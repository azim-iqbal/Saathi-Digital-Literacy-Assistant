## 7 October 2026 — Research architecture and deterministic remediation (overall work open)

Live guards now suppress cloud dispatch for changing claims requiring research, unproven high-risk destinations, private message/document/account cues, keyboards and public error/consent/payment handovers. Error research starts from a blank description, never an automatic raw-screen upload. Only old-window content events with a different current root are filtered; current/unknown/window transitions still invalidate. Reviewed dependency checklists are not yet connected to live targets.

Evidence and remaining gates: [remediation matrix](REMEDIATION_CAPABILITY_MATRIX.md).

## 7 October 2026 — Retained-task pause/resume and private challenge handover

Final verification: **95 Android unit tests and all 10 focused emulator tests pass**, debug/test builds and release Kotlin compile pass. The added immediate-start/pause UI test found a real `ForegroundServiceDidNotStartInTimeException`; fixed by acknowledging pending foreground starts before service shutdown (promotion alone was insufficient). No production guidance/audio services remained after the final tests; the separate fixture WebView service remained until emulator shutdown. Evidence: `test-evidence/2026-10-07-resumption/verification.json`, `final-regression.txt`, `paused-ui-acknowledged.txt` and screenshots.

Latest user priority: reliable resumption. Implemented memory-only paused task settings, explicit permission/unlock/service checks, fresh session identity/current-tree request, same-theme Resume/Discard controls and paused speech preference preservation. Private/password and CAPTCHA screens now intercept guidance before gateway dispatch, disable listening, preserve the original goal and re-observe to resume after a safe screen returns. Manual Pause never auto-resumes; Stop, permission-loss safety stops and process death do not revive tasks.

95 Android unit tests and debug/test/release Kotlin builds pass. Nine focused emulator regression tests pass, including three OTP-like/private and three CAPTCHA round trips plus three rapid manual cycles, missing-permission refusal with retained task, stale identity rejection and Stop cleanup. Additional paused-screen UI evidence is in `test-evidence/2026-10-07-resumption/paused-ui.txt`. See PAUSE_RESUME.md for limits and failed fixture attempts: separate test APK needed array iteration instead of unavailable Kotlin collections runtime. No live provider calls, deployment or push.

Broader implementation brief saved at specs/IMPLEMENTATION_REMEDIATION_REQUEST.txt; research/planning/eligibility/provider diagnostics/browser readiness remain unfinished. Actual speech/device/OEM and real-site auth/CAPTCHA acceptance remain unverified. Preserve all existing evaluation fixes and evidence.

## 3 October — optional complaint clipboard companion

The reporting flow now offers a reviewed local worksheet and a short-lived overlay carrying exact draft fields. This companion does not observe or type into private forms and does not change AccessibilityService target/privacy rules. It asks per-copy consent, writes the selected text only, and directs the person back through recent apps to the existing browser tab. Missing/revoked overlay permission falls back to in-app copy. See CYBER_FRAUD_REPORTING.md for limits and TEST_RESULTS.md for emulator evidence. Do not call this autonomous form completion or uninterrupted background survival.

## 3 October 2026 — connection recovery and overlay events

The debug client preserves allowlisted rejection reasons and gives localized recovery copy for authentication/configuration, unavailable connection, timeout, capacity, quota/storage/circuit, stale observations and unverifiable decisions. Cancellation suppresses callbacks immediately and sends a best-effort server cancellation on a separate bounded lane; already dispatched provider network calls may continue until transport timeout. No automatic retries or single-provider fallback were added.

Own overlay events are excluded by current attached window identity even when Android omits/changes event package metadata. An actual trace exposed a previously missed WINDOWS_CHANGED event. Final emulator service/event and paired synthetic-protocol tests pass; the original intermittent post-tap WebView failure is still not conclusively diagnosed. Test-only event observers store no default logs or screen text and have a no-op release implementation.

## 2 October 2026 — public travel fields and reporting link helper

Public dates/currency-formatted fares no longer trip the generic long-number rule; secret metadata still takes priority, and unknown numbers stay conservative. Editable controls are not highlight targets. The new reporting companion is reachable from Home and this panel. It provides an optional short-lived, consent-based fixed-URL overlay with a non-overlay fallback. Its windows are excluded from guidance feedback events by actual attached window identity. Read [CYBER_FRAUD_REPORTING.md](CYBER_FRAUD_REPORTING.md); this is not a verified automated portal workflow.

# Floating assistant and live option finding

## 6 October 2026 — local form-field guidance

Requests for help filling a form now use a local field guide instead of the model route. It marks the currently focused eligible field, or the topmost visible unoccupied text field, using fresh accessibility bounds. Spoken guidance names the field when its hint, resource ID or nearby label maps to a reviewed field-name list (for example, “Fill the first name field”); it does not read arbitrary page text aloud. Editable text and descriptions are discarded from retained nodes; only an occupied/not-occupied bit from the value remains for local progression. Saathi does not type, click or submit. Password/private fields, ambiguous focus, recognized browser address fields and screens with a detected private field produce no marker. This is field-by-field assistance, not form interpretation or completion verification.

## Latest backend/browser continuation — 1 October 2026

Debug Android now connects to the loopback gateway for mock practice and separately consented AI navigation. Independent Gemini/Groq REST adapters and ignored credential/model placeholders are implemented. A live proposal is accepted only on paired agreement, a current eligible control and fresh observation. The app resolves bounds locally and uses its localized instruction template. Recent suggested labels support step-by-step wrong-path recovery, but are not claimed clicks or completion evidence. Default/release guidance stays local. See [AI_ORCHESTRATION.md](AI_ORCHESTRATION.md) and [SETUP.md](SETUP.md), which supersede older disconnected-backend notes below.

Actual Chrome on the API37 emulator passed a localhost synthetic-page flow: highlight/tap-through, detour/return, private password screen suspension, retargeting and Stop. The live Android/backend route passed wrong-path recovery using two **simulated** provider adapters. No provider account was configured, no external model request made, and no real account/transaction used. The user chose emulator testing for now.

Voice setup now offers an explicit installed-language check/download through the device speech service, with the existing brand, palette and glass controls. TTS completion callbacks succeeded in all3 languages; recognition returned missing-language error13. No intelligibility, microphone transcript or successful language download is claimed. Natural streaming/barge-in, physical-device/OEM survival, release backend connectivity and hosted authentication/TLS remain open.


## Core conversation continuation — 1 October 2026

The latest user priority is core app functionality first, with the established UI retained and shared by new interfaces. Live sessions can now change the requested visible option without restarting the foreground session. The assistant panel uses **Update on-screen help** for an active live session. Hands-free recognition can hand off explicit requests such as “find Help”, “Help dhundo” or “मदद खोजो”; ordinary conversation does not silently replace the goal. These are deterministic commands, not general AI reasoning.

Every accepted change invalidates the old screen/audio work and asks the actual AccessibilityService for a fresh current tree, even if the other app emits no event. Session identity stays the same; stale presentation/session requests, stopped sessions, private-screen voice requests and refused option labels do not change the goal. The scheduled reader also rechecks keyguard before copying. Only a validated requested option is retained as the session goal in memory; raw audio/transcript history is not persisted. Voice notifications now reopen the assistant panel.

`SaathiColors.kt` and `SaathiBrand.kt` share the existing main-app palette and vector header with AssistantActivity. Main palette values and header geometry are unchanged; assistant chips use the same green containers, and existing glass buttons/panels remain. The assistant respects system reduced motion and the main shell's maximum content width. Its glass remains an opaque fallback without sampling other apps. Panel localization, Figma parity, native/legacy surfaces and broader accessibility/device checks are still pending.

Verification is recorded in TEST_RESULTS.md. Parser tests and direct request handoff are distinct from actual speech recognition: microphone/TTS delivery, natural streaming conversation, real browser compatibility and OEM survival remain unverified. No model or paid service was enabled.


The user explicitly expanded scope on 30 September from practice-only guidance to apps/browsers and requested a floating intake surface plus text-only output. The app now has **Help in apps & browsers** on Home, a separate assistant panel, and a draggable branded edge button during active sessions. The button and ongoing notification reopen the panel. It uses the existing Saathi mark and glass components; no new visual reference was introduced.

## Available behavior

- Type a visible option name or use the installed speech recognizer to dictate it, review the text, then explicitly start.
- Choose **Text only** or **Text + voice**. Text only immediately cancels session narration and conversation audio. Input method and output mode are separate: dictation does not automatically enable narration.
- After granting Accessibility/overlay access, open the desired app or browser. `LiveGuide` matches a unique, enabled, clickable option by its exact visible label/content description. It also handles a labelled child inside an observed clickable parent, using the parent's actual bounds. Browser-style controls can have no resource ID; their position is tied to the current immutable observation.
- Default guidance is local. Debug AI navigation has a separate data disclosure and opt-in; see AI_ORCHESTRATION.md. Nothing is tapped, typed or submitted by Saathi.
- A missing option gives a back/clarification instruction; duplicates produce no guessed target. Private-field heuristics, permission/system overlays and consequential action terms stop target selection. The current conservative substring checks can also refuse benign names; contextual refinement remains open. These are conservative checks, not perfect detection or a universal safety guarantee.
- Text instructions stay on-screen with the marker; the panel remains reachable from the bubble/notification. Optional hands-free reply controls use the previously implemented on-device turn-based service, with its existing capability and interruption limits.

## What this does not implement

This is **an experimental visible-option finder, not general multi-step AI guidance**. It does not understand arbitrary goals across arbitrary apps, verify a website's legitimacy, resolve visually ambiguous layouts, read images/canvas controls, infer success, perform transactions, or guarantee compatibility with every app/browser. It does not replace the synthetic practice path. Provider adapters and debug integration are implemented; real-model accuracy, full conversational memory and streaming speech remain unverified or unfinished.

The intake panel currently has English copy; live instruction messages have English/Hindi/Hinglish variants. Native-language review, complete panel localization, dark/large-font/landscape visual checks, keyboard/TalkBack and Figma parity remain pending. The floating control is draggable but its position is not persisted across sessions.

## Background behavior and reliability

Sessions run in a visible foreground service. They stay active across app switches and wait on unsupported screens. Stop, screen-off/lock, permission loss and unrecoverable service failure end the session. Process death does not silently restart a microphone. Android or an OEM can terminate background work; there is no permanent-running or crash-free guarantee. The notification provides a return path if the bubble is hidden by another app.

The tree walker now counts every visited node (including invisible nodes), caps depth at 50 and visits at 600, limits retained field strings to 300 characters, and catches remote tree-reading failures without releasing stale guidance. An observation failure is checked against the current session/revision before it can affect presentation. The foreground service also rejects obsolete shutdown callbacks.

The noninteractive highlight window uses opacity at most 0.7 and no more than the system's maximum obscuring opacity, so ordinary cross-app touch can pass through on Android 12+. The small bubble is separately touchable. Apps may reject obscured touches or hide overlays; Saathi does not bypass those protections. See official [Android touch restrictions](https://developer.android.com/about/versions/12/behavior-changes-all) and [foreground microphone restrictions](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start).

## Required next checks

Observed on the API 37 emulator: Android Settings force-hides both windows (`mIsForceHiddenNonSystemOverlayWindow=true`, `isOnScreen=false`). Label matching still works, but the marker and bubble are unavailable there. Return through the notification/app for text help; no bypass was attempted. A separately installed test APK and its local WebView allowed the marker, bubble and touch-through behavior. That is a controlled compatibility fixture, not certification of Chrome, Firefox or arbitrary sites.

Web accessibility can expose the same link label twice (link plus child). The resolver merges matches only when their observed nonempty tap rectangles are identical. Matching labels at separate locations remain ambiguous and get no target.

See TEST_RESULTS.md for actual observations from this phase. Do not interpret a passing Settings check as browser or universal app certification. Still required: broader real AccessibilityService integration across multiple apps, Chrome/Firefox fixtures, private/login/permission screens, permission revocation, OEM background behavior, bubble drag/rotation/occlusion, prolonged voice/call interruption, and physical-device latency/battery measurements. All broader backend, design and language gaps in EXECUTION_PLAN.md remain open.

## Real service integration — 1 October 2026

A new emulator-only integration test enables the actual Saathi accessibility service with Android’s `FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES` test option. It starts a text-only live session and uses a separately installed synthetic app; it never supplies a snapshot or invokes the guide/overlay directly. Real Android events drive native option highlighting, a detour correction, return to the original page, disappearance of the old label, own-app clearing, a local WebView link and its text mutation, and explicit Stop. After initial page loading settles, the observation identity remains stable while idle.

The service manifest now uses Android’s documented exported declaration while keeping `BIND_ACCESSIBILITY_SERVICE`, so arbitrary apps still cannot bind. Earlier test setup failures were command quoting and a mismatch between Android’s bound-service label and the test’s expected class name; they do not establish that the old manifest caused a binding failure. See TEST_RESULTS.md for passing counts and screenshots.

This closes the controlled event-to-presentation gap for the synthetic external app/WebView only. It does not certify Chrome/Firefox, actual voice turns, hidden-overlay apps, permission revocation, OEM behavior or arbitrary multistep tasks. The established Saathi theme, glass/navigation components and logo were preserved; no product redesign or new visual source was introduced.

## Permission-loss check — 1 October follow-up

Real text-only emulator sessions now pass live overlay-access and AccessibilityService revocation checks: observation/presentation stop, and restoring access does not restart a session. Actual audio, OEM and physical-device revocation remain unverified. Chrome first-run setup is now complete, signed out, with usage/crash reporting disabled and notifications declined. A synthetic localhost page was verified; other sites/browsers remain untested. See the latest TEST_RESULTS entry. No product colors, branding or UI changed in this test-only phase.
