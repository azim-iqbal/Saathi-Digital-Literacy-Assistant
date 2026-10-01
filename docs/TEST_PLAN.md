# Test plan and current coverage

## Passing local tests

- Node masking and sensitive-content heuristics.
- Guardrail classification and synthetic task routing.
- Observation session/revision/window identity and stale overlay/notification protection.
- Deterministic Electricity, Water and DTH fixture progression.
- Synthetic-surface eligibility: registered fixture IDs are accepted only in Saathi's package; unregistered and third-party screens are rejected.
- Local dual-proposal validation: agreement, disagreement, stale metadata, invalid target, malformed result, timeout, quota result, handover, and identifier-only snapshot construction.
- Python mock gateway: actual localhost authentication/schema/body checks, two concurrent immutable-input calls, cancellation/replacement, deadlines, observation expiry during work, bounded worker/session admission, global/per-adapter budgets, circuit breaking, uncertainty and completion evidence. These tests do not contact real models or exercise Android networking.

## Required before release or broader guidance

Live option finder tests cover exact labels, clickable parent bounds, no-ID controls, ambiguity/missing controls, private screens, consequential requests and package policy. New instrumentation covers intake mode selection, a separate test APK's native button and local WebView link, visible overlay/touch-through, bubble reopening the assistant and Settings hiding overlays. See TEST_RESULTS for the exact final run; real AccessibilityService event delivery is not exercised by these manually supplied observation fixtures.

- Exercise `SaathiSession` on-device with accessibility, overlay and notification permissions granted and revoked during every state.
- Test real Android notification Stop/restart races, screen-off, lock/unlock, process death, rotation, split-screen and empty/secure accessibility trees.
- Test TalkBack focus order, keyboard navigation, large text, Hindi/Hinglish copy, reduced motion, reduced transparency and API26–30 fallbacks.
- Test real installed Android speech/TTS engines: availability, language packs, the Slow/Standard/Fast preview and session rates, cancellation, audio focus, headset/call interruption and transcript fallback.
- Profile release builds on physical low/mid/high-tier devices. Software-emulator frame timing is not a performance acceptance result.
- Before network enablement, test gateway authorization, quota, cancellation, deadlines, provider disagreement, malformed structured data, redaction, adversarial screen text and data-retention controls.

Use made-up values only in every Bill Pay fixture run. A passing synthetic practice test is never evidence of real payment compatibility.

## Real accessibility-service regression

Run `LiveAccessibilityIntegrationTest` only on the synthetic emulator. It preserves/restores enabled accessibility services and overlay AppOps, enables the real service using `UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES`, and never calls `beginObservation`, `onScreenChanged`, `LiveGuide.next` or the overlay presenter. Initial WebView loading must settle before the idle-identity assertion. Native detour/return, changed native/web labels, own-app clearing and explicit Stop are asserted. Injected input belongs only to the test harness. Broader real-browser and permission/audio/device checks remain separate.

The current instrumentation runner otherwise suppresses ordinary accessibility services; the older manually supplied snapshot checks are retained as component-level coverage. Official behavior: [UiAutomation flags](https://developer.android.com/reference/android/app/UiAutomation#FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES).

`PermissionLossIntegrationTest` is emulator-only and preserves original service/AppOps settings. It exercises a real session, overlay revocation, explicit restart, service revocation and restored permissions without automatic observation. Keep microphone-revocation and physical-device cases separate; this test runs text only. Real Chrome testing requires completed browser setup; do not silently accept first-run consent or count WebView fixtures as Chrome.
