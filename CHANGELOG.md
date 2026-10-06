# Changes

## 6 October 2026

- Added local, field-by-field help for form-filling requests. The guide marks safe visible text fields, discards editable text from retained accessibility nodes and leaves entry and submission to the user. Private/password screens remain unmarked; hands-free listening pauses during form guidance.
- Form-filling tasks bypass the AI backend and use local field guidance, so backend handovers do not block this supported task.

## 3 October 2026

This update adds reporting tools and makes backend failures easier to understand. The existing navigation, branding and guidance model remain in place.

### Reporting and complaint preparation

- Added a cybercrime-reporting entry with an offline checklist, an official-portal link, and optional typed or dictated incident intake.
- Added a separate consent step for AI assessment. Unavailable or conflicting replies leave the offline advice accessible.
- Added a complaint worksheet that arranges the person's own account and known details. Unknown dates, amounts and other facts are left out rather than invented.
- Added review confirmation and an exact preview before each clipboard write. Editing a draft resets its review status. An optional floating helper carries reviewed fields for up to two minutes.
- Kept browser paste and final submission under the person's control. Instructions explain how to return to an already-open form without opening another tab.

### Guidance and privacy fixes

- Corrected false private-field matches on explicitly formatted travel dates and public fares. Sensitive metadata still takes precedence, and editable values remain excluded from AI guidance.
- Tightened label, request-size and observation-age checks.
- Fixed an own-overlay window-event classification gap that could destabilize presentation. The original intermittent post-tap WebView issue remains unresolved.
- Kept buttons sized to their labels and replaced unintended default-purple chip colors with the existing green palette.

### Backend and diagnostics

- Added separate server-status and provider-check actions. Status refresh makes no model call; the provider check requires explicit approval for a synthetic request to each model.
- Added individual outcomes, timing and available token usage for Gemini and Groq. Simulated replies are clearly distinguished from real API responses.
- Improved cancellation, worker cleanup, persistent call-budget failures, provider error handling and both-provider failure accounting.
- Added a consented incident-assessment contract with strict category validation and rejection of unsupported claims.
- Updated Gemini parsing to accept documented opaque signature metadata while still rejecting thought content, tool calls and unexpected fields.
- Diagnosed the local Python certificate configuration without disabling TLS verification. Both providers have returned valid replies; the latest paired request still timed out on Groq.

### Verification

Saved results cover 75 Android unit tests, 55 backend tests and 12 focused emulator checks. Debug and release builds passed; lint reported 0 errors and 61 warnings. Live-provider checks are explicitly opted in and are separate from the offline suite. See [test results](docs/TEST_RESULTS.md) and [provider verification](docs/PROVIDER_VERIFICATION.md).

Private portal forms, genuine incident-assessment accuracy, reliable paired-model guidance, physical-device voice/OEM behavior and production release connectivity remain open. This update does not mark the app production-ready.
