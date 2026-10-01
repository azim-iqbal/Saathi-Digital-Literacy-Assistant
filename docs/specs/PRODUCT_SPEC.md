# Saathi — Product Specification & Technical Architecture

Repository: https://github.com/azim-iqbal/Saathi-Digital-Literacy-Assistant

## 1. Product purpose and priorities

Saathi helps people understand and navigate the screen they are currently using. It explains one next step in their preferred language and visually identifies the relevant control. The person performs every tap, types their own information, and makes every consequential decision.

Serve younger and older people, including people with limited digital confidence. Launch with English, Hindi, and Hinglish across interface, speech input, and speech output. Make the product reassuring, precise, attractive, and easy to stop.

Priority order:

1. User control, privacy, and correct guidance.
2. Reliable supported tasks and understandable failure handling.
3. Accessibility and simple interaction.
4. Premium visual design, meaningful animation, and tactile feedback.
5. Broader task, device, provider, and language coverage.

Design for broad cross-app support as a long-term ambition. Never advertise universal compatibility or try to bypass another application's privacy controls. Clearly distinguish verified support, experimental support, unsupported screens, and untested combinations.

Keep the core promise: observe, explain, and highlight. Do not add automatic tapping, typing, gestures, submitting forms, accepting permissions, booking, transferring money, or entering credentials. Do not imply that a highlighted action is mandatory or guaranteed safe.

## 2. Engineering standards and security boundaries

- Maintain strict architectural separation between accessibility observation, guidance orchestration, and UI rendering.
- Ensure user privacy: sensitive data, PINs, passwords, and payment credentials must never be captured, transmitted, or logged.
- Build accessible, high-contrast, tactile UI components matching the defined Figma tokens and design systems.
- Validate all screen guidance with localized, accessible, and non-blocking overlays.
- Keep the project runnable with comprehensive unit and instrumentation testing suites.
- Distinguish verified local flows from experimental cloud or proxy adapters.

## 3. Audit before redesign

Create docs/CURRENT_STATE_AUDIT.md with findings, evidence, severity, fixes, and verification status. Inspect build configuration, manifest, services, capture, masking, overlay, AI client, voice, storage, tests, and documentation.

Specifically recheck these previously observed areas:

- API keys embedded through BuildConfig and direct provider calls from the APK.
- An old Gemini model default; verify supported current model IDs rather than guessing replacements.
- targetSdk migration and current Play submission requirements; distinguish targetSdk, compileSdk, and minSdk.
- Missing or outdated foreground-service types and MediaProjection lifecycle handling.
- Full-screen TYPE_APPLICATION_OVERLAY touch-pass-through behaviour on modern Android.
- Missing request cancellation, stale-result rejection, screen/session identity checks, and target validation.
- Five-second unchanged-screen retries treating slow reading or typing as an error.
- Single-worker request backlogs and unsafe sharing of mutable state.
- Quota accounting before deciding whether a remote request is needed.
- Demo-specific fallbacks presented outside the demo and travel guidance that repeatedly chooses the first matching label.
- A completed-flow cache that records labels rather than actual reusable flow definitions.
- Static package exclusions that may overstate universal restrictions; e.g. an entire shopping package being labelled as a payment service.
- English-centric secret masking, unlabelled text, screenshot-to-node timing mismatches, and privacy claims stronger than the implementation.
- Session stop, provider cancellation, screen-capture release, microphone release, and in-flight work that might outlive consent.
- Stored goals, conversation history, preferences, Android backup behaviour, debug logs, and all documentation claims about retention.
- Broad substring guardrails causing false matches or false refusals.

Do not turn this audit into a speculative list: mark each item confirmed, resolved already, needs device reproduction, or not applicable.

## 4. Native architecture and dependencies

Prefer Kotlin, Jetpack Compose, Material 3, coroutines/Flow, lifecycle-aware state, and a clear repository/domain/UI separation. Use stable compatible dependencies unless an experimental dependency has a necessary benefit and a documented fallback.

Retain and improve useful existing native services. Do not rewrite everything merely for architectural neatness. Use separate modules only where the boundary earns its complexity.

Recommended logical boundaries:

- UI, navigation, tokens, themes, and motion.
- Session coordinator and task-state engine.
- Accessibility snapshots and platform capability checks.
- Privacy filtering and allowed data contracts.
- Target resolution and native overlay rendering.
- Model-provider gateway and response validation.
- Speech recognition, speech synthesis, and audio coordination.
- Local preferences and strictly necessary session state.
- A small protected backend for provider credentials, budgets, authentication, and routing.

A minimal Python/FastAPI backend is acceptable given the repository owner's background. Supply local development instructions and mock providers. Do not require Kubernetes, Kafka, multiple databases, or a paid cloud deployment for the pilot.

Use typed immutable state and structured errors. Keep blocking network, image processing, and JSON work off the main thread. UI state must survive ordinary configuration changes. After process death, restore preferences, but never silently resume screen observation or microphone recording.

## 5. Figma: complete new design direction

Create a distinct editable Figma design titled "Saathi — Product Design System and Android Experience". Replace the old visual language while preserving recognisable product purpose. Do not simply recolour the current layouts.

Create these Figma pages:

1. Product principles and user journeys.
2. Foundations: colour, typography, spacing, elevation, iconography, motion, accessibility.
3. Component library with variants and states.
4. Light-theme screens.
5. Dark-theme screens.
6. English, Hindi, Hinglish, large-text, and narrow-screen examples.
7. Interactive prototype and motion annotations.
8. Developer handoff and screen/component mapping.

Use editable text, auto-layout, named components, reusable instances, variables, and semantic tokens. If extending an existing file, inventory its styles first and avoid unwanted duplicate token systems. Map Figma concepts into Compose tokens. If an automatic Code Connect mapping is unsupported for the chosen setup, document a manual mapping rather than pretending it is connected.

### Visual identity

Direction: calm intelligence, warmth, clarity, and restrained depth. The result should feel intentionally designed by a product team, not generated from a generic AI dashboard template.

Starting palette to refine after contrast checks:

- Light background: warm ivory #F7F8F5.
- Light surface: #FFFFFF.
- Light primary text: deep ink #152923.
- Light primary action: deep teal #096B59 with accessible foreground.
- Dark background: #101A18.
- Dark surface: #192824.
- Dark primary text: #F0F6F3.
- Dark primary accent: soft mint #8CDDC3 with dark foreground.
- Secondary warmth: restrained amber; never the only signal for warning or success.

These are initial art-direction values, not pre-approved contrast results. Define semantic roles and validate every foreground/background pairing, including composited glass states.

Use a typeface with excellent Latin and Devanagari support. Prefer a restrained family pairing such as a licensed Latin family with Noto Sans Devanagari, or system fonts with consistent metrics. Use real Hindi text during design, not English placeholders that are translated at the end. Keep line heights generous and weight hierarchy simple.

Use a coherent icon family with suitable licensing. Add a small original Saathi symbol that can suggest guidance or companionship without a generic robot head. Prefer vector assets. Avoid random stock portraits, meaningless charts, oversized AI orbs, decorative badges, arbitrary gradients, and placeholder lorem ipsum.

### Liquid-glass treatment

Implement an Android-appropriate interpretation of liquid glass: translucent tonal surfaces, subtle borders, restrained blur where supported, soft highlights, and believable depth. Do not claim access to proprietary platform rendering.

Use glass selectively on the in-app navigation dock, selected floating controls, and a few decorative surfaces. Keep body text and important actions on high-contrast surfaces. Do not place transparent text panels over unpredictable third-party content.

Do not capture another app's screen merely to create a blurred backdrop. The cross-app overlay should use a readable self-contained chip and target outline. Older devices, reduced-transparency mode, high-contrast mode, and performance-constrained configurations must have opaque tonal alternatives that still look polished.

### Motion and parallax

- Use short, consistent transitions, approximately 160–280 ms as an initial tuning range.
- Use spring motion sparingly for direct interaction; avoid bounce on instructions or critical controls.
- Add a restrained collapsing home header and small decorative scroll parallax where it improves hierarchy.
- Limit parallax to noninteractive decorative layers. Never move a tappable control away from the user or animate reading text independently from its container.
- No mandatory gyroscope permission or sensor-driven parallax.
- Respect system animation settings and an in-app Reduce motion setting. Provide a Reduce transparency setting.
- Do not use continuous decorative animation while idle. Listening animation must reflect actual listening; processing indicators must reflect actual work.
- For guidance targets: clear stale highlights immediately, resolve the current control, then reveal the new marker. Never sweep a cursor through unrelated controls.

## 6. Required screens and reusable components

Design and implement the following, including meaningful empty, loading, denied, offline, error, and large-text states:

1. Welcome with a concise explanation and language choice.
2. Optional short illustration of observing versus acting; skippable, not an onboarding maze.
3. Step-by-step permissions setup with why it is needed, current status, and recovery after denial.
4. Home with one clear primary action: start help; a few supported task shortcuts; no busy dashboard.
5. Task intake by voice or text; transcript correction before starting.
6. Compatibility/readiness summary with supported, experimental, limited, and unsupported explanations.
7. Active session overview with current step, session status, and obvious stop control.
8. Cross-app floating status chip, expandable controls, and target highlight.
9. Listening, processing, verifying, speaking, waiting, and paused states.
10. Need-clarification and conflicting-model states in simple language.
11. Sensitive/protected-screen handover without misleading reassurance.
12. Offline, quota-exhausted, timeout, and provider-unavailable recovery.
13. Completion summary based on observed evidence; practise again where appropriate.
14. Settings: System/Light/Dark theme; interface, input, and output language; speech speed; haptic strength; reduced motion/transparency; text sizing support.
15. Privacy centre: active capabilities, data destinations, revoke/stop controls, and clear-local-data action.
16. Help and supported-task/device information.
17. Clearly labelled demo/practice mode using synthetic data.

Component library: buttons, text fields, language controls, navigation items, status chips, glass/opaque cards, permission cards, speech controls, transcript bubbles, instruction cards, dialogs, bottom sheets, banners, progress indicators, and focus/pressed/disabled/error states.

Use a compact labelled navigation structure only where needed, e.g. Home, Practice, Settings. Preserve Android back behaviour. Do not add a History tab unless there is useful privacy-conscious content to display.

## 7. Highlighting and the visible session indicator

The highlight must be immediately identifiable, pleasant, and precise:

- Default: a high-contrast rounded outline hugging the real target, a small directional pointer, and an optional short instruction chip.
- Adapt outline contrast to the background, using a dual stroke when appropriate. Colour must not carry meaning alone.
- Optional cursor style is acceptable, but never place a cursor randomly. It must anchor to the resolved target and never suggest that Saathi is clicking.
- Avoid obscuring the target label, keyboard, system bars, or nearby controls. Handle scrolling, rotation, display cutouts, split-screen, and large fonts.
- A pulse should settle into a stable marker. Haptic feedback should be rate-limited when the target changes.
- If the target is uncertain or absent, show no target and explain what is needed next.

Provide a small branded session dot/chip, preferably near a safe upper corner but movable or relocatable to avoid conflicts. This is Saathi's own UI, not Android's system privacy indicator. Do not imitate the native camera/microphone dot or hide those indicators.

Collapsed state may use a dot with an accessible description; expanded state must identify Saathi and expose Pause, Repeat, and Stop. Indicate actual modes with text/icon support, not colour alone. Show mic-active status only when recording is actually active. Do not display camera-active status if no camera is used.

Use legitimate overlay APIs and verified touch behaviour. Investigate appropriate accessibility-overlay versus application-overlay architecture; choose based on actual eligibility and platform documentation, not a workaround to defeat restrictions. Where overlays are unavailable, explain that limitation and use permitted audio/notification/in-app controls.

## 8. Dual-AI guidance: real checks, not two decorative API calls

Implement a provider-neutral dual-model path. Prefer a currently available free-tier Gemini model and a currently available free-tier model served by Groq if their capabilities and terms fit. Groq is an inference provider, not itself a model; record the actual model identity for both calls. Prefer meaningfully different model families when possible.

For each new AI-generated guidance step:

1. Capture an immutable snapshot with session ID, monotonically increasing screen version, capture time, package/window identity, and eligible node IDs.
2. Run local privacy filtering and task/screen eligibility checks before network transmission.
3. Give both models the same minimal sanitised task and eligible controls. Let them propose independently to reduce copying/anchoring bias.
4. Require structured outputs: proposed action type, target node ID or null, brief explanation, expected observable outcome, uncertainty reasons, and completion evidence if claimed.
5. Compare proposed action meaning and target identity, not exact sentence wording. Validate both against the live snapshot and local policy.
6. Select a step only when the required checks pass. Generate one concise spoken instruction; the UI should not expose technical model chatter.
7. Immediately before display, verify that session, package, window, and screen version are still current and the control is still eligible.
8. After the user acts, check the observed state transition. Do not infer completion from elapsed time or model confidence alone.

Model agreement is an agreement signal, not statistical proof or a safety guarantee. Do not invent accuracy percentages or say "verified safe" merely because two models agree.

No model may authoritatively provide free-floating pixel coordinates when there is no matching current control. Resolve target bounds locally from stable eligible node IDs. For visual-only screens, classify guidance as unsupported/experimental until a separately tested grounded vision path exists. Never fake visual support by guessing positions.

Disagreement: perform at most one bounded clarification/retry when useful, otherwise ask a short user question, use a verified deterministic flow, or pause. Never silently pick one model for consequential steps.

If one provider fails or reaches quota, never claim dual verification occurred. For the initial pilot, use a clearly identified verified local flow or pause; any future single-model mode must be an explicit documented product decision.

Known local flows and unchanged already-validated screens need not spend two new API calls. Label their provenance accurately. Screen changes invalidate relevant cached decisions; include task, app/version, locale, and screen state in cache design. Do not cache private screenshots or credentials.

Implement provider adapters, schema validation, deadlines, cancellation, bounded retry/backoff, per-provider and global budgets, circuit breakers, and sanitised observability. Avoid queues of obsolete requests: latest relevant screen wins. Serialise or otherwise make session state concurrency-safe.

Treat all external screen content as untrusted data. A page that says "ignore your instructions" must not change system policy or cause data disclosure. Adversarial instructions must be in the test set.

Future providers should plug into a capability registry: supported modalities, locales, structured output, latency, retention policy, and cost/quota model. Evaluate providers on the same task suite before enabling them.

## 9. Free operation now, scalable operation later

Default to a zero-paid-spend development/pilot configuration. Verify provider terms, availability, and quotas at implementation time and document the verification date and official URLs.

- No automatic billing activation or paid fallback.
- Distinguish an ongoing free tier from a one-time credit balance or trial.
- Stop cloud work when budget/quota is exhausted; continue only supported local functionality.
- Do not rotate accounts or keys to evade limits.
- Do not assume per-device limits protect shared project quotas.
- Store provider secrets on the backend, never in the APK. Supply safe sample configuration and a local development gateway.
- Authenticate clients and enforce server-side budgets; App Check/device attestation may be considered as an additional measure, not sole protection.
- Do not treat account quotas from one developer account as guarantees for every user.
- Document hosting costs separately from model costs. Local development may be free; always-on hosting and premium voice are not automatically free.

Before enabling any cloud provider, verify what its unpaid terms allow for personal/sensitive data. Consent does not override a provider prohibition. If a free service is unsuitable for real user screens, keep that path synthetic-demo-only, use sufficiently minimised non-sensitive data where allowed, or use a suitable local alternative. Mark a compliant paid option as future scope rather than quietly weakening privacy promises.

Provide budget examples for 100, 1,000, and 10,000 daily active users using configurable sessions/day, new AI steps/session, two inference calls/step, retries, tokens, and speech minutes/characters. Clearly identify assumptions. Do not invent fixed rupee costs without verified prices.

## 10. Task engine, screen compatibility, and safe handover

Use an explicit state machine with states such as Idle, Preparing, Observing, Analysing, Verifying, Guiding, WaitingForUser, Clarifying, SensitiveHandover, Paused, Unsupported, Completed, Error, and Stopped. Voice state is separate from task state.

- Only observe while a user-started session is active and the relevant permissions remain granted.
- Debounce changes without suppressing useful updates forever during event bursts.
- Do not classify slow reading/typing as a wrong tap. Use semantic outcome checks and adjustable inactivity reminders.
- On navigation, keyboard change, rotation, package change, or session replacement, invalidate obsolete results and remove outdated markers.
- Do not re-highlight a filled field just because its label is the first match.
- When a screen exposes no safe structure, stop grounded guidance. Do not redirect to the demo as if that were a real task continuation.
- Define a tested workflow registry with versioned steps and safe checkpoints. Keep demo flows separate from production guidance.
- Do not automatically approve final payments, purchases, bookings, permissions, legal declarations, or consent. Explain review requirements and leave the decision to the user.
- Never read, infer, repeat, store, or send OTPs, passwords, PINs, CVVs, recovery codes, or other secrets. Guide around those steps and resume only on an observable permitted screen.
- Package identity or the presence of a browser does not prove a website is genuine. Do not tell users a page is official based only on its title or self-description.

Maintain a compatibility matrix by task, app/site version, Android version, device, locale, capture mode, and result. Broad compatibility is a programme of testing and maintenance, not a boolean feature flag. Do not add a blanket allow-all policy to satisfy an "all apps" marketing claim.

## 11. Speech and language experience

Current scope is English, Hindi, and Hinglish for UI, input, and output. Treat Hinglish as a deliberate code-switching mode, typically with Latin-script UI and natural Hindi-English speech, not an assumed universally supported locale code.

- Let users choose one combined language preference initially. Provide separate UI/input/output overrides in advanced settings.
- English UI and speech should be clear Indian English. Hindi UI should use reviewed natural Devanagari. Hinglish should sound conversational and respectful, not exaggerated slang or awkward word-by-word translation.
- Example tone: English "Tap Electricity to continue." Hindi "आगे बढ़ने के लिए बिजली वाला विकल्प दबाएँ।" Hinglish "Aage badhne ke liye Electricity par tap kijiye."
- Adapt instructions to the actual control label; preserve its recognisable label even when the spoken explanation uses another language.
- Support editable speech transcripts and graceful fallback to typing.
- Implement interchangeable SpeechRecognizer and SpeechSynthesizer interfaces, separate from the reasoning providers.
- Evaluate actual English/Hindi/Hinglish samples from Android TTS and any available cloud provider, including Sarvam if suitable. Choose by intelligibility, latency, code-switching, privacy, device support, and cost, not marketing claims.
- Built-in/on-device speech should be the zero-paid-spend fallback where installed and supported. Check availability and explain missing language packs. Do not claim recognition or TTS is offline unless verified on that device/engine.
- Cloud premium voice must be optional and gated by consent, allowed data, and quota. Sarvam signup credits are not an unlimited free service; verify current terms. Do not create or clone anyone's voice without rights.
- Prefer short streaming/chunked speech where supported. Handle cancellation, audio focus, phone calls, headphones, Bluetooth, and interruptions. Provide speed control and a voice preview.
- Avoid hearing the app's own speech as user input. Coordinate microphone and playback explicitly. Barge-in is optional only if tested; push-to-talk must remain reliable.
- Pause/stop/repeat/help commands must work in all three supported modes. Do not upload continuous ambient audio.
- Never speak secrets. Avoid reading sensitive amounts or personal details aloud without an intentional user choice; use headphones-aware, respectful defaults.

Future multilingual plan: locale-independent message IDs, no hard-coded UI strings, plural rules, Unicode-safe processing, bidirectional/RTL layouts, fallback fonts, language-pack capability checks, provider routing by locale, code-switch handling, localised safety phrases, and native-speaker testing per language. Track supported UI, ASR, and TTS separately. "All languages" remains a roadmap aspiration until each is validated.

## 12. Haptics

Create a small semantic haptic system: selection, session started, new guidance available, warning, and completion. Prefer Android/Compose semantic haptic APIs and supported predefined effects. Check hardware capability and honour system settings.

Offer Off/Subtle/Standard or equivalent user-friendly controls; do not promise identical strength across phones. Good haptics are crisp and meaningful, not maximally strong. Avoid constant pulses, vibrations for every token, and repeated alerts while someone reads. Pair essential feedback with visual/audio alternatives. Test low-end hardware and devices with no vibrator.

## 13. Background sessions and lifecycle

Implement dependable user-started active sessions using the appropriate foreground-service architecture and a persistent notification with pause/stop actions. Explain that Android may restrict or terminate work; do not promise an immortal background service.

- Satisfy current target-SDK foreground-service, microphone, and MediaProjection requirements.
- Acquire projection consent when required and never reuse invalid grants.
- Respect protected windows and capture exclusions.
- Handle lock/unlock, app switching, network changes, rotation, revoked permissions, process death, screen-off, and battery restrictions.
- Stop or safely suspend observation when locked. Do not restart microphone or capture after boot/process death without required user action.
- On Stop: cancel jobs and provider requests where possible, release capture/mic/audio/overlay resources, remove session indicators appropriately, and prevent queued work from being sent. Explain that already-transmitted data cannot be recalled.
- Keep the session indicator and notification consistent with actual service state.
- Offer manufacturer-specific troubleshooting only when necessary. Do not demand blanket battery-optimisation exemptions or instruct users to disable security protections.

## 14. Privacy and security requirements

Minimise information before it leaves the phone. Sending everything to a backend for redaction is not equivalent to device-local privacy filtering.

Use password flags, multilingual semantic cues, field context, value patterns, and conservative screen-level rules. Do not claim redaction is perfect. For sensitive or poorly understood screens, withhold screenshots and pause cloud guidance.

Associate captures and node snapshots with a common screen identity and timing tolerance. Handle scaling/cropping/rotation. If alignment cannot be established, do not transmit the image. No screenshot capture merely for decoration.

By default, do not persist raw audio, screenshots, complete screen text, or model prompts/responses. If an optional diagnostic mode is needed, keep it explicit, minimised, short-lived, and based on synthetic data wherever possible. Ensure crash reporting and HTTP logs cannot capture secrets. Document backup exclusions and deletion behaviour.

Use TLS, protected key storage where appropriate, least privilege, safe network timeouts, input validation, endpoint authentication, and release-build debug restrictions. Validate model output as untrusted input. Do not let model responses change provider endpoints, call tools, grant permissions, or execute arbitrary code.

Provide separate, understandable consent for accessibility observation, screen capture, microphone, and cloud processing when required. A privacy policy is not a substitute for an in-context disclosure.

## 15. Accessibility and practical performance

- Aim for WCAG AA contrast: at least 4.5:1 for normal text and 3:1 for qualifying large text; validate controls and focus indicators too.
- Use at least 48 dp touch targets and larger primary actions where layout permits.
- Test large system font sizes, including approximately 200%, and narrow devices without clipped Hindi labels.
- Provide meaningful semantics, logical focus order, TalkBack labels, keyboard/switch access where applicable, and non-gesture alternatives.
- Avoid announcing every visual update. Coordinate with screen readers to prevent overlapping speech; document any unresolved coexistence limitations.
- Never rely on colour, vibration, sound, or animation alone.
- Meet reduced-motion/transparency preferences with equally polished static alternatives.
- Profile on a representative modest Android device and at least one newer device. Record exact hardware/OS/build and tests actually run.
- Aim for smooth 60 Hz interaction on the baseline device, but report actual frame timing/jank, memory, startup, battery impact, and guidance latency rather than claiming perfection.
- Measure AI latency as end-to-end p50/p95, including both providers. Local UI must remain responsive while waiting. Do not invent percentage progress for indeterminate inference.
- Disable expensive blur/parallax automatically when needed without breaking navigation or branding.

## 16. Testing and acceptance gates

Write meaningful tests for risky behaviour, not tests that merely mirror UI implementation. Include:

1. Target node disappears or moves while models are responding: stale result is discarded.
2. User switches to another app/session: previous guidance never appears there.
3. User pauses/stops during queued/network/speech work: no new outbound work or stale overlay follows.
4. Two models disagree, one times out, both hit quota, malformed JSON arrives, or the response names a nonexistent target: bounded recovery and no invented guidance.
5. Secrets in password fields, ordinary text, Hindi labels, notification-like content, and misaligned captures: conservative handling is verified.
6. A malicious webpage attempts prompt injection: local policy remains in control.
7. User spends time reading/typing, makes a wrong tap, returns back, or changes a field: task state updates correctly without nagging loops.
8. Provider credentials cannot be found in built APK/source; quotas enforced server-side and mock mode works without keys.
9. Protected screen, no accessibility tree, overlay blocked, missing voice pack, denied/revoked permission, offline network, and process death: each has a usable visible outcome.
10. English/Hindi/Hinglish UI and voice, light/dark/system themes, large text, reduced motion/transparency, haptics disabled, and TalkBack coexistence.
11. Cross-app overlay touch behaviour on real third-party test apps, not just the same-package demo.
12. No claims of completed payment/submission without suitable observable evidence.

Create a small synthetic test application or fixture screens for repeatable edge cases, clearly identified as fixtures. Keep them out of the normal production flow. Obtain permission before testing private accounts; do not perform real financial transactions for validation.

Build/lint/unit tests must pass where the environment supports them. Run instrumented/emulator and physical-device tests when devices are available. Mark unavailable tests "not run" with reason and reproduction steps; do not mark them passed from source inspection.

Acceptance gates:

- Editable Figma design, tokens, components, both themes, and representative language/large-text variants exist, or the access blocker is explicitly reported.
- Compose implementation visibly follows the approved self-reviewed Figma direction; provide actual screenshots and comparison notes.
- A supported non-demo low-risk task is tested end-to-end when a device is available; the mock demo is not the sole compatibility evidence.
- Dual-model guidance is real when configured and labelled honestly; failure paths work with mocks and without billing.
- No known stale-target, secret-in-APK, or stop-session data-leak defect remains without being clearly flagged as release-blocking.
- Language/theme/accessibility controls work and persist appropriately.
- Session visibility, lifecycle recovery, and resource release are tested to the extent available.
- Documentation matches the implementation and lists incomplete work.

## 17. Documentation: understandable without prior project knowledge

Write clear English, define technical terms at first use, and use small diagrams/tables where useful. Explain why each major choice exists. Include copyable commands with expected output and troubleshooting. Do not publish aspirational features as implemented.

Required documentation, grouping related topics if that makes maintenance easier:

- README.md: what Saathi does, who it serves, quick start, modes, limitations, screenshots, links to the rest.
- docs/CURRENT_STATE_AUDIT.md: starting findings and resolution evidence.
- docs/PRODUCT_SCOPE.md: current users/tasks, supported versus future scope, explicit exclusions.
- docs/DESIGN_SYSTEM.md: Figma link, tokens, typography, component mapping, glass fallbacks, assets/licences.
- docs/UX_FLOWS.md: onboarding and session/error journeys, microcopy and language examples.
- docs/MOTION_AND_HAPTICS.md: timings, triggers, reduced-motion behaviour, device caveats.
- docs/ARCHITECTURE.md: component responsibilities, data flow, concurrency, session state machine.
- docs/SETUP.md: Android Studio/JDK/SDK, backend, mock mode, safe environment variables, device connection, building an APK.
- docs/AI_ORCHESTRATION.md: schemas, independent proposals, comparison, validation, failures, provider adapters, provenance.
- docs/FREE_TIER_AND_COSTS.md: dated official sources, current provider choices, quotas, privacy terms, credit limits, zero-spend configuration, future scenarios.
- docs/VOICE_AND_LANGUAGES.md: current capabilities and quality checks; adding new languages/providers.
- docs/COMPATIBILITY.md: actual tested app/task/device matrix, restrictions, unsupported cases.
- docs/PRIVACY_AND_SECURITY.md plus aligned root PRIVACY.md: data inventory, destinations, retention, consent, threats, redaction limitations, deletion.
- docs/BACKGROUND_AND_PERMISSIONS.md: service lifecycle, notification/indicator truthfulness, platform requirements, troubleshooting.
- docs/TEST_PLAN.md and docs/TEST_RESULTS.md: commands, fixtures, devices, measured outcomes, not-run tests, release blockers.
- docs/SCALABILITY_ROADMAP.md: pilot to larger deployment, budget enforcement, provider expansion, regionalisation, task maintenance, secure optional app partnerships.
- docs/CONTRIBUTING.md or root CONTRIBUTING.md: project layout, conventions, adding a flow, tests, review checklist.
- docs/DEMO_SCRIPT.md: a concise honest hackathon story, offline fallback, and which demonstrations are simulated.
- docs/DECISIONS.md: short architecture/design decisions with tradeoffs.
- docs/KNOWN_LIMITATIONS.md: unresolved defects and out-of-scope promises in plain language.

Add an index and cross-links. Avoid duplicating instructions in multiple places where they will diverge. Date external policy/pricing claims and link official primary documentation. Add concise comments around non-obvious invariants, especially stale-result rejection and privacy boundaries.

Future scope should include additional reasoning/voice providers, verified language expansion including RTL, more Android/OEM testing, privacy-compatible on-device models, and optional in-app SDK partnerships for more reliable semantic guidance. An iOS companion would have a different permitted scope; do not promise an equivalent universal cross-app overlay/accessibility implementation.

## 18. Delivery phases and final handoff

Execute in this order, adapting only for concrete dependencies:

1. Audit and establish a runnable baseline; fix critical safety/build issues.
2. Create Figma foundations, key journeys, components, and visual direction.
3. Implement Compose themes, components, onboarding, home, settings, and session states.
4. Harden snapshots, target grounding, cancellation, overlay, capture, and lifecycle.
5. Implement backend/provider adapters and real dual-model validation with zero-spend limits.
6. Integrate English/Hindi/Hinglish speech, natural-voice options, haptics, and graceful fallbacks.
7. Complete remaining Figma variants and align implementation; test accessibility and performance.
8. Finish documentation, verification, demo, and reviewable delivery.

At each phase, update progress and keep running instructions accurate. Do not spend the entire effort on visual polish while leaving the central guidance engine unsafe.

Final handoff must include:

- A concise explanation of changes and resulting behaviour.
- Figma file and key node links if created successfully.
- Branch/commit and draft PR link where available.
- Actual light/dark screenshots and a recorded demonstration if recording is available.
- Build/APK location when built; never commit secrets or unnecessary build outputs.
- Verified commands and test results, including device identities and limitations.
- Documentation index.
- Current compatibility and provider/quota assumptions.
- A prioritised remaining-work list, with release blockers first.

Do not conclude with "production-ready," "works on all apps," "100% private," "natural voice on every device," "unlimited free AI," or "perfect background operation" unless the specific claim is actually supportable. Be proud of good work through evidence, not promises.

Begin by inspecting the repository, recording the baseline, and creating the execution plan. Then proceed through the implementation without repeatedly requesting confirmation for ordinary reversible choices.

---

## Reference starting points for the implementing agent

These are pointers, not frozen guarantees. Recheck at implementation time.

- Android Compose animation: https://developer.android.com/develop/ui/compose/animation/introduction
- Material 3: https://developer.android.com/develop/ui/compose/designsystems/material3
- Accessibility services: https://developer.android.com/guide/topics/ui/accessibility/service
- Android overlay and capture protections: https://developer.android.com/security/fraud-prevention/activities
- Android 12 touch behaviour: https://developer.android.com/about/versions/12/behavior-changes-all
- MediaProjection: https://developer.android.com/media/grow/media-projection
- Foreground services: https://developer.android.com/develop/background-work/services/fgs
- Haptics: https://developer.android.com/develop/ui/views/haptics
- Play accessibility API policy: https://support.google.com/googleplay/android-developer/answer/10964491
- Play target API requirements: https://support.google.com/googleplay/android-developer/answer/11926878
- Gemini pricing and data-use disclosures: https://ai.google.dev/gemini-api/docs/pricing
- Gemini API terms: https://ai.google.dev/gemini-api/terms
- Gemini model deprecations: https://ai.google.dev/gemini-api/docs/deprecations
- Gemini API-key guidance: https://ai.google.dev/gemini-api/docs/api-key
- Groq rate limits: https://console.groq.com/docs/rate-limits
- Groq data policies: https://console.groq.com/docs/your-data
- Sarvam pricing: https://docs.sarvam.ai/api/getting-started/pricing
- Sarvam credits and limits: https://docs.sarvam.ai/api/getting-started/ratelimits

Brief prepared September 28, 2026. All future scope is explicitly conditional on platform permission, evidence, budget, and successful testing.
