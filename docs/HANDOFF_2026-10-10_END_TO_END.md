# Saathi end-to-end engineering handoff

**Saved:** 10 October 2026  
**Stop reason:** available Codex usage reached 11%, below the user-set 15% floor. Do not start another implementation or test phase in this run.  
**Repository:** `Saathi- Digital Literacy Co-Pilot`  
**Working-tree rule:** preserve all current uncommitted changes. Do not reset, clean, rebase, discard, commit, push, deploy, publish or expose credentials unless the user explicitly asks.

## 1. What Saathi is

Saathi is an Android digital-literacy copilot. It provides local-first, accessibility-grounded guidance across Saathi itself and other Android apps. The product is designed to help a person understand visible controls, maintain a task across interruptions, explain research-backed plans, and safely hand back sensitive or consequential actions to the person.

Its operating model is deliberately conservative:

1. Observe the current accessibility tree.
2. Classify privacy, interruption and risk from structure and metadata without reading entered secret values.
3. Prefer a deterministic local step that is grounded in the current visible screen.
4. Use remote planning only through explicit consent, freshness, provenance, deadline and budget controls.
5. Re-observe after each user action. Never infer that a task is complete from an earlier screen.
6. Do not tap third-party controls, type into external apps, paste private values, accept declarations, bypass challenges, make payments or submit complaints.

The existing visual system is intentional: Saathi green, the established logo, glass controls, rounded navigation, compact content-sized buttons, the floating assistant and the launch animation must remain consistent. Do not redesign the UI while finishing core work.

## 2. Current implementation map

### Android app

| Area | Main implementation | Current behavior |
|---|---|---|
| Accessibility observation | `app/src/main/java/com/saathi/accessibility/` | Copies a bounded current accessibility tree, avoids partial-tree guidance, tracks freshness and releases copied nodes. |
| Privacy classification | `NodeContentPolicy.kt`, `NodeMasker.kt`, `SensitiveContent.kt`, `PrivacyClassification.kt` | Separates public labels/actions, normal inputs, private editable fields, authentication/payment fields and unknown/redacted content. Does not read editable values. |
| Live session lifecycle | `app/src/main/java/com/saathi/orchestrator/SaathiSession.kt` | Owns task/session identity, stale-response rejection, pause/resume, permission-loss shutdown, explicit re-observation and private/challenge handoff. |
| Local guidance | `LiveGuide.kt`, `FormGuide.kt`, `CommerceGuide.kt`, `PopupGuide.kt` | Performs snapshot-local matching and safe fallback. Targets require current-tree evidence, not remembered coordinates. |
| Overlay/presentation | `app/src/main/java/com/saathi/overlay/` | Shows a highlight or private-field marker; it does not execute the external action. |
| Speech/voice | existing voice service and preferences under `app/src/main/java/com/saathi/` | Session wiring and lifecycle controls exist. Real microphone/TTS quality and background survival remain unverified on a physical device. |
| Research/planning UI | existing research, source and reviewed-plan classes | Supports consented source reading and reviewed plans. Live browser binding remains deliberately fail-closed when evidence is insufficient. |
| Android tests | `app/src/androidTest/java/com/saathi/ui/` | Contains service, privacy, form, lifecycle, browser and physical-device harnesses. |

### Backend and AI connection

The backend has local-first guidance, paired Gemini/Groq adapters, provider diagnostics, monotonic deadline handling, cancellation, circuit/cap behavior, consented incident assessment, complaint worksheet support and deployment preparation. See `docs/AI_ORCHESTRATION.md`, `docs/PROVIDER_VERIFICATION.md`, `docs/BACKEND_CONNECTION_RECOVERY.md`, `docs/CLOUD_DURABLE_STATE_DESIGN.md` and `docs/TEST_RESULTS.md` for the detailed history.

Important provider rules:

- The screen-freshness limit is authoritative. Do **not** extend the eight-second Gemini bound just to receive a late answer; a response after the person changes screens is invalid for live guidance.
- Earlier genuine provider checks verified individual calls, but paired agreement, broad quality, latency reliability and production accounting are not certified.
- Credentials belong only in ignored local configuration. Never print, commit, search broadly for, or place API keys in documentation, APKs or test output.
- Do not run additional live-model probes without a new, explicit, scoped user allowance. Previous live-call allowances were consumed.

### Deployment status

No hosted production service is configured or deployed. The repository contains deployment design and local packaging work, but production HTTPS, domain/TLS, account provisioning, per-user authentication/accounting, durable shared storage and release connectivity still need the user's chosen host and external credentials. Deployment and release execution remain deferred.

## 3. Completed capability work

### Product, UI and navigation

- Liquid-glass visual language, sculpted/floating navigation, shared colors, branded logo/vector assets, launch transition and shared controls are already implemented.
- The floating assistant supports typed/voice request entry and text-first/spoken guidance choices within the existing theme.
- Content-sized button rules and the shared green palette are preserved in new flows.
- No visual/layout/theme changes were made in the latest Samsung/Zepto continuation.

### Guidance, safety and lifecycle

- Explicit current-screen request when live guidance starts on an already open but quiet external screen.
- Strict session/presentation/window/revision freshness checks and cancellation boundaries.
- Pause/resume preserves the original task only when it is safe to do so; Stop, permission loss and process death do not silently resume unsafe work.
- Private/password, CAPTCHA/authentication, payment, consent, error and security-warning surfaces hand control back to the user.
- Private-field badges are structural and bounded to actual editable private fields. Product prices, dates, weight ranges and numeric resource IDs no longer make the whole screen look private.
- Mixed screens may retain a safe local target beside an unfocused private field, while cloud/listening remain suspended. No private editable value is copied or sent remotely.
- Native and WebView controlled forms react to changes without requiring manual zoom/zoom-out or a refresh.

### Research, planning and provenance

- Consent-gated retrieval/research architecture, source authority/provenance, freshness, source-scoped prerequisite identities, dependency relationships, reviewed eligibility reasoning and explicit completion predicates are implemented in the repository.
- Source opening and browser guidance are fail-closed around ambiguous browser chrome, unsafe destinations, certificate/security warning screens and untrusted retrieved text.
- Retrieved content is not treated as instructions. Plan evidence must be cited, fresh and reviewed before a current-screen target can be suggested.
- The architecture supports English, Hindi and Hinglish controls and fallback wording. It is not a certification of broad live-model language quality.

### Commerce and cross-app guidance

`CommerceGuide.kt` implements general hierarchy-based shopping guidance:

- It recognizes bounded English/Hindi/Hinglish order/search requests.
- It finds a public Search control or matches request terms to a current public product label.
- An Add control is accepted only when the matching product title, price and control share the same copied accessibility ancestor.
- Multiple matching cards remain the user's choice. Missing/conflicting hierarchy produces no target rather than a guess.
- The current code recognizes an exact visible `Cart` action and an in-card compact numeric quantity only when the same screen has one unambiguous Cart action. This is general accessibility-tree logic, not Zepto-specific code.
- Cart guidance is review-only. Saathi never confirms an order, chooses a substitute, taps purchase, enters a delivery address or makes a payment decision.

### Cybercrime support

The app includes a privacy-safe cybercrime reporting path and complaint worksheet. It can collect user-confirmed facts, show per-field reviewed copy text and guide the person back to a browser. It does not diagnose a legal outcome, accept declarations, enter identity data, bypass CAPTCHA/certificate warnings, automatically paste into private forms or submit a report. The public portal entry was checked historically; authenticated protected form steps remain unverified without legitimate access.

## 4. Latest physical-device evidence: Samsung + Zepto

The user connected a Samsung SM-M076B on Android 16 and authorized one controlled Zepto test using an existing saved Home location. The phone's existing incompatible Saathi build was replaced with the current debug build after explicit authorization; Saathi local settings/drafts were therefore deleted. Zepto data was not reset.

### Verified flow

1. On Zepto's public milk-result screen, Saathi received an exact visible product request.
2. It highlighted that same product's Add action using a fresh accessibility snapshot.
3. The user-authorized test added one item. Saathi then detected the compact quantity state for that exact card and highlighted the unique Cart action.
4. Cart was opened manually for review only. The same one item was present.
5. The test item was removed manually. Zepto returned to the product-results state, and Saathi again highlighted Add for that exact product.

### Evidence and results

- `PhysicalCommerceObservationTest`: 2/2 passing observations — added/Cart and cleared/Add.
- No live model request was made; both observations remained local.
- Fresh snapshot timings recorded: 353 ms for added/Cart and 246 ms for cleared/Add.
- `CommerceGuidanceTest`: 4/4 passing on the Samsung.
- `ReactiveFormIntegrationTest`, `PauseResumeIntegrationTest`, `PermissionLossIntegrationTest`: 4/4 passing on the Samsung.
- Full Android local unit suite: 166 passing.
- Debug and Android-test APK builds: passing.
- No checkout, payment, purchase, private entry, complaint submission or external message was performed.

### Test-harness correction made

The first final Samsung commerce run exposed a test-only race. Saathi's transparent overlay could briefly be reported as the active accessibility root while a test was attempting to interact with an already-grounded fixture control. `CommerceGuidanceTest` now retries only the **same exact prior rectangle** for up to four seconds. It does not search for a nearby control or change the requested target. The corrected 4/4 suite passes.

### Evidence locations

- `docs/test-evidence/2026-10-10-samsung/zepto-e2e-2026-10-10.md` — current privacy-filtered summary.
- `docs/test-evidence/2026-10-10-samsung/README.md` — prior device setup and initial real-Zepto fixes.
- `docs/CROSS_APP_COMMERCE.md` — scope, algorithm and acceptance matrix.
- `docs/TEST_RESULTS.md` — current test-result ledger.

No raw screenshots or XML dumps were retained because they could contain the saved delivery address. Do not recreate a cart test unless the user explicitly authorizes it again; the Zepto cart was cleared.

## 5. Current validation summary

| Area | Evidence | Status |
|---|---|---|
| Android local unit suite | 166 tests | PASS for current local suite |
| Debug/test APK compilation | Current source | PASS |
| Samsung commerce fixture | 4 tests | PASS |
| Samsung native/WebView no-zoom form + lifecycle tests | 4 tests | PASS |
| Samsung real Zepto exact product/Add/cart/remove path | 2 filtered observations | PASS for this narrow path |
| Privacy false-positive regressions | Unit + service coverage | PASS for covered patterns |
| Pause/resume and permission loss | Device/service coverage | PASS for covered flows |
| Backend deterministic/offline suites | Historical evidence in `TEST_RESULTS.md` | PASS in prior phase; do not relabel as a fresh rerun |
| Genuine provider quality | Bounded historical synthetic checks | PARTIAL / not production-certified |
| Browser/Chrome real-site guidance | Controlled evidence plus known limits | PARTIAL |
| Continuous physical voice/OEM survival | Not fully exercised | NOT VERIFIED |
| Hosted production backend/release | No host configured | NOT STARTED |

Counts from distinct test groups overlap. Do not add them together as one total.

## 6. Files changed in the latest continuation

- `app/src/main/java/com/saathi/orchestrator/CommerceGuide.kt` — exact Cart recognition and safe compact-quantity/cart-review logic.
- `app/src/test/java/com/saathi/orchestrator/LiveGuideTest.kt` — regression coverage for compact quantity with and without a Cart action.
- `app/src/androidTest/java/com/saathi/ui/CommerceGuidanceTest.kt` — test-only exact-target retry across transient overlay-root changes.
- `app/src/androidTest/java/com/saathi/ui/PhysicalCommerceObservationTest.kt` — explicit opt-in, filtered physical observation harness; no external taps or raw screen retention.
- `docs/CROSS_APP_COMMERCE.md`, `docs/TEST_RESULTS.md`, `docs/NEXT_CONTINUATION.md`, `docs/test-evidence/2026-10-10-samsung/` — current evidence and scope.

The repository contains many older uncommitted changes from prior completed phases. Inspect `git status` and the existing docs before editing. Do not use a reset/clean to simplify the tree.

## 7. Non-negotiable safety and product boundaries

1. Never read or transmit passwords, OTPs, PINs, CVVs, authentication tokens, private editable values or full raw screen dumps.
2. A private-field marker is not consent to upload its value. Keep private values local even if visual highlighting remains enabled.
3. Never weaken stale-response, task/session identity, cancellation, pause/resume, CAPTCHA, authentication, consent, payment or protected-screen boundaries to make a test pass.
4. Never bypass a certificate warning, CAPTCHA or legal declaration. Do not submit a cybercrime complaint or complete a real purchase in testing.
5. Do not call local deterministic rules genuine AI. Do not claim arbitrary app support, natural continuous conversation, crash-free background survival or production readiness without evidence.
6. Do not log/commit provider keys, device identifiers, customer addresses, complaint facts, screenshots of private screens or raw accessibility XML.
7. Preserve the current UI system. Any new UI should use `SaathiColors`, `SaathiBrand`, glass controls and content-sized controls; no wholesale redesign.

## 8. Exact resume order for the next agent

### Step 0 — reconcile state

1. Check the available Codex usage before any substantial action.
2. Read this document, then the current top entries of `docs/NEXT_CONTINUATION.md`, `docs/TEST_RESULTS.md`, `docs/REMEDIATION_CAPABILITY_MATRIX.md`, `docs/EXECUTION_PLAN.md`, `docs/CROSS_APP_COMMERCE.md`, `docs/LIVE_ASSISTANT.md`, `docs/VOICE_CONVERSATION.md`, `docs/AI_ORCHESTRATION.md` and `docs/CURRENT_STATE_AUDIT.md`.
3. Run `git status --short`; preserve every existing change.
4. Inspect the latest relevant test/source before changing it. Historical test passes do not prove a newly edited path.

### Step 1 — physical-device acceptance remaining

Use the connected Samsung only with a narrow purpose and user authorization for any external side effect.

- Run the unexecuted physical speech/microphone/TTS checks only after checking permissions and without keeping recordings.
- Test background/foreground survival, accessibility-service continuity, overlay permission loss and TalkBack coexistence with a bounded real-device protocol.
- Measure only useful performance/latency metrics; do not repeat known poor emulator blur benchmarks.
- Keep Zepto cart empty. Do not revisit checkout/pay screens. A new cart add requires fresh user authorization.

### Step 2 — browser/WebView reliability

- Revisit the historical intermittent WebView post-tap failure only using saved sanitized input/window diagnostics. A passing rerun is not a root-cause explanation.
- Expand real HTTPS/browser validation where the browser exposes trustworthy destination metadata. If Chrome does not expose enough evidence, preserve the safe fallback rather than fabricating provenance.
- Continue general live-screen plan binding: only guide a reviewed, fresh, evidence-supported action that has one exact visible public target. Keep authentication, CAPTCHA, payment, private form and browser-security surfaces manual.

### Step 3 — research/AI/provider quality

- First run deterministic/offline contract tests after any backend/prompt change.
- Do not make new Gemini/Groq calls without a fresh explicit allowance. If granted, use only fictional non-sensitive navigation/planning/incident cases, enforce the existing screen-freshness deadline, record sanitized timing/token metadata and stop at the authorized cap.
- Diagnose provider latency by phase and freshness validity. Do not simply lengthen timeouts.
- Keep provider failures distinct: availability, timeout, cancellation, stale observation, validation rejection and paired disagreement are different outcomes.

### Step 4 — deployment preparation only

The user has not selected a host. Complete only repository-local preparation: configuration validation, container/runtime documentation, secrets placeholders, TLS/auth assumptions, durable-state design and tests that do not need external credentials. Do not deploy or publish. Do not claim Cloud Run/shared persistence is ready until the chosen infrastructure and real multi-instance storage are validated.

## 9. Remaining work, categorized honestly

### Repository work still open

- Broader general multi-step browser guidance that connects researched dependencies to verified controls on current live pages.
- Historical intermittent WebView post-tap root-cause investigation.
- More diverse changing-screen, long-session, cancellation and resource tests.
- Broader semantic form dependency/validation/completion reasoning without examining entered values.
- Provider prompt/validation improvements and deterministic diagnostics, subject to no-live-call default.
- Full regression reconciliation after any core guidance/backend change.
- Hosted-backend packaging and validation that require no external host credentials.

### External/manual blockers

- Chosen HTTPS host, domain/TLS and real production backend deployment.
- Provider-account operation and fresh bounded model-quality allowance.
- Physical microphone/TTS quality, OEM battery/background behavior, TalkBack usability and performance acceptance.
- Legitimate authenticated cybercrime-portal access and non-submitting protected-form verification.
- Real payments, identity verification, complaints or purchases must never be used as normal tests.

## 10. Suggested first commands next time

Run these only after confirming the usage budget and the desired bounded phase:

```bash
git status --short
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest
```

For the physical-device test harness, rediscover the connected device rather than copying an old transport identifier. Use only the focused test class needed for the current change. Do not start broad model/provider checks, live portal flows or transaction tests by default.

## 11. Documentation index

- `docs/NEXT_CONTINUATION.md` — chronological continuation ledger; newest entry is first.
- `docs/TEST_RESULTS.md` — test evidence and limits.
- `docs/REMEDIATION_CAPABILITY_MATRIX.md` — capability status and open gates.
- `docs/EXECUTION_PLAN.md` — engineering roadmap/history.
- `docs/CROSS_APP_COMMERCE.md` — current commerce/privacy rules and real Zepto evidence.
- `docs/PRIVATE_FIELD_CONTINUITY.md` — explicit privacy behavior and non-upload boundary.
- `docs/LIVE_ASSISTANT.md`, `docs/VOICE_CONVERSATION.md` — live assistant and voice scope.
- `docs/AI_ORCHESTRATION.md`, `docs/PROVIDER_VERIFICATION.md` — backend/provider architecture and validation.
- `docs/CYBER_FRAUD_REPORTING.md` — cyber-reporting workflow limits.
- `docs/test-evidence/2026-10-10-samsung/` — latest physical-device evidence.

## 12. Final status

Saathi has substantial implemented, regression-tested local guidance, privacy, lifecycle, research and backend foundations, plus a narrowly verified real Samsung/Zepto product-to-cart review path. It is **not** honestly ready to be described as universally compatible, fully production-ready or bug-free. The next agent should resume from the documented evidence, preserve the current UI and safety model, and advance one bounded, test-backed capability at a time.
