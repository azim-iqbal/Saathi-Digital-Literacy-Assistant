## Current state — 3 October 2026

The local debug backend has independently verified Gemini and Groq responses. The last paired request timed out on Groq, so reliable dual-model guidance is not yet established. The complaint worksheet and copy helper are implemented; private-form paste, incident accuracy, physical-device acceptance and release connectivity remain open. [Provider verification](PROVIDER_VERIFICATION.md) records the connection findings and their limits.

## 3 October 2026 — latest connection/draft implementation

Implemented in the debug local pilot: authenticated server-status refresh (no model request), explicit two-provider API smoke check, independent Gemini/Groq outcomes and token metadata, safe recovery, and a reviewed complaint worksheet with per-field clipboard consent and an optional floating helper. The user clarified that they **removed the keys**; no live Gemini/Groq call was made. Test fixtures remain explicitly labelled. Main navigation, logo and existing primary palette are unchanged; shared secondary/tertiary defaults now use the existing greens instead of inherited Material purple.

Verification: 75 unit / 54 backend / 12 emulator checks, debug and release builds, 0 lint errors (61 existing warnings). Keys are absent by user choice, so genuine provider connectivity/accuracy remains blocked. The complaint worksheet and helper never read a private form, clipboard or browser DOM; copy is explicit, paste is manual. See NEXT_CONTINUATION.md for open work.

## 3 October 2026 update

The local backend now has tested storage/quota distinction, cancellation tombstones, worker-start cleanup, provider error taxonomy and safer malformed-output handling. A consented paired-provider incident-assessment path is wired into the reporting screen with offline fallback and the existing Saathi theme. The adapters are configurable, but **no real AI account/model call has been tested**. Release connectivity and production hosting/authentication remain unfinished.

The official apex portal loads in emulator Chrome through its public financial-reporting entry; www failed certificate validation, and all app fixed links now use the verified apex address. Own-overlay window-change filtering was corrected following an idle failure trace. The earlier post-tap WebView failure remains unexplained. Final evidence: 71 Android unit / 46 backend / 11 focused emulator passes, 0 lint errors / 61 warnings; see the newest TEST_RESULTS.md entry.

## 2 October 2026 update

The date/fare false-positive privacy rule and bounded backend admission/session defects have been corrected. A new cyber-fraud reporting companion provides optional text/dictation, conservative local triage, manual reporting steps, English step reading, browser selection and per-use official-link copy consent, including an optional short-lived floating helper. See [CYBER_FRAUD_REPORTING.md](CYBER_FRAUD_REPORTING.md) for exact scope, source limits and unfinished verification. This does not make the whole app production-ready or certify fraud/complaint completion.

# Starting audit — 28 September 2026

## Latest backend/browser continuation — 1 October 2026

Debug Android now connects to the loopback gateway for mock practice and separately consented AI navigation. Independent Gemini/Groq REST adapters and ignored credential/model placeholders are implemented. A live proposal is accepted only on paired agreement, a current eligible control and fresh observation. The app resolves bounds locally and uses its localized instruction template. Recent suggested labels support step-by-step wrong-path recovery, but are not claimed clicks or completion evidence. Default/release guidance stays local. See [AI_ORCHESTRATION.md](AI_ORCHESTRATION.md) and [SETUP.md](SETUP.md), which supersede older disconnected-backend notes below.

Actual Chrome on the API37 emulator passed a localhost synthetic-page flow: highlight/tap-through, detour/return, private password screen suspension, retargeting and Stop. The live Android/backend route passed wrong-path recovery using two **simulated** provider adapters. No provider account was configured, no external model request made, and no real account/transaction used. The user chose emulator testing for now.

Voice setup now offers an explicit installed-language check/download through the device speech service, with the existing brand, palette and glass controls. TTS completion callbacks succeeded in all3 languages; recognition returned missing-language error13. No intelligibility, microphone transcript or successful language download is claimed. Natural streaming/barge-in, physical-device/OEM survival, release backend connectivity and hosted authentication/TLS remain open.


Latest core continuation (1 October): live option requests can change within one session through typed intake or explicit speech commands, with fresh-tree observation and stale-callback rejection. The assistant now shares the main palette/vector header. This remains an exact-visible-option guide; Android is not connected to the mock gateway or real models. See EXECUTION_PLAN.md and the latest TEST_RESULTS entry.

Latest scope change (30September): the user authorized live app/browser assistance. A separate local visible-option finder, explicit intake/disclosure, text-only output, and floating assistant have been added. This supersedes the earlier practice-only restriction for explicitly started live sessions, without restoring the old generic demo fallback or enabling cloud/capture. See LIVE_ASSISTANT.md and the newest TEST_RESULTS entry; no universal compatibility claim.

Baseline: Initial architecture audit performed on Android client foundation. Core audit findings and remediation items are tracked below.

| Finding and evidence | Severity / starting status | Phase-one disposition |
|---|---|---|
| app/build.gradle.kts embeds GEMINI_API_KEY; GeminiClient directly calls provider | Critical, confirmed | Removed client and BuildConfig fields; cloud guidance disabled. Existing old APKs not remediated. |
| SaathiSession checks only active when presenting asynchronous results | Critical, confirmed | Session/revision/package/window tickets, main-thread state, stale overlay intent checks added; pure gate tests pass. Device races not verified. |
| Notification Stop only stops GuidanceForegroundService | Critical, confirmed | Calls SaathiSession.stop; device verification pending. |
| Capture lacks screen/timestamp alignment, projection callback and suitable lifecycle | Critical, confirmed | Capture implementation and UI entry disabled; no image path remains. Future implementation must start from current platform requirements. |
| NodeMasker ignores raw text and Hindi cues | High, confirmed | Added conservative text/Unicode/camel-case checks; tests pass; not complete redaction. |
| Five-second wrong-tap timer and twenty-second watchdog repeat unchanged work | High, confirmed | Removed; no inactivity-based inference. |
| Worker queue and shared mutable session state | High, confirmed | Session state serialized on main; one tree copy plus dirty flag; old copies rejected. Device burst behavior remains untested. |
| Quota acquired before remote eligibility | High, confirmed | Remote path removed, limiter unused. Server budgets still absent. |
| Demo fallback used on third-party screens; WebsiteGuide repeatedly selects first label | High, confirmed | Both removed from external session routing; only identified same-package practice nodes eligible. The unused WebsiteGuide source was later removed. |
| Completed cache stores generic labels, not flow definitions | Medium, confirmed | Still generic categories; not a workflow registry. |
| Amazon shopping package labelled Amazon Pay restriction; other package claims unverified | Medium, confirmed | Policy remains legacy and unverified; current session no longer uses it as compatibility evidence. |
| ConversationStore saves 80 messages; GuidanceStateStore saves goals; allowBackup true | High, confirmed | Stores now memory-only, legacy values cleared on construction; backup disabled. OEM transfer and migration need device checks. |
| Broad substring guardrails e.g. bet in ordinary words | Medium, confirmed | Not fixed; release limitation. |
| App uses Views, no Compose; themes partly hard-coded | Scope gap, confirmed | User's existing UI preserved; Figma foundations created, migration pending. |
| minSdk26 / compileSdk35 / targetSdk33, guidance foreground service has no type | Release blocker, confirmed | No blind SDK bump; foreground service and notification migration remains pending. |
| Full-screen application overlay at default opacity | Release blocker, needs device reproduction | Cross-app guidance disabled; touch behavior still requires redesign/testing before re-enabling. |
| Voice service error retries and audio coordination | High, confirmed | Automatic session microphone path disabled; task-screen speech input remains opt-in and engine-dependent. |
| Baseline lint | Confirmed | 7 errors/59 warnings. Typed typeface constants and missing Hindi resource fixed. Final lint 0 errors/59 warnings. |

Baseline build and seven unit tests passed; baseline lint failed. Current verification is in [TEST_RESULTS](TEST_RESULTS.md).

Official sources checked 28 September 2026: [Android 12 touch restrictions](https://developer.android.com/about/versions/12/behavior-changes-all), [MediaProjection](https://developer.android.com/media/grow/media-projection), [Gemini deprecations](https://ai.google.dev/gemini-api/docs/deprecations). Current model choice is deliberately unset. The Play target-policy URL failed to load; current submission deadline is **not verified**. Recheck before SDK migration. No current pricing, free-tier or data-use approval is claimed.


## 29 September follow-up

The baseline findings above describe the original audit. A Compose app shell and core light/dark Figma frames now exist. Test evidence and remaining failures/unknowns are maintained in TEST_RESULTS.md and EXECUTION_PLAN.md. This does not close the audit's backend, lifecycle, accessibility, performance or real-app guidance requirements.

## Local-practice state follow-up

The old coordinator could create a status overlay after any non-fixture tree, including a third-party app, despite the pilot's local-only scope. It now requires the Saathi package plus a registered synthetic Bill Pay resource ID before calling `DemoGuide`; other, empty and unregistered Saathi screens clear overlay and speech output and move to an explicit waiting state. The fixture registry now includes Water and DTH controls, correcting a gap where those approved task shortcuts could not enter the guide. The session has explicit preparing, observing, analysing, guiding, waiting, sensitive-handover, paused, completed, stopped and error states, but live lifecycle delivery and overlay behavior remain device-unverified. Generic completion-category persistence was removed; task/conversation/completion state remains memory-only.

## Real accessibility event integration — 1 October

The previously unverified service pipeline now has a controlled emulator test that enables the actual service and observes external native/WebView events, detour/return, stale-label correction, own-app clearing and Stop without manual snapshot injection. Manifest declaration is aligned with Android’s documented exported accessibility service, retaining the system-only binding permission. This is not a general-app/browser or audio certification. Existing source limitations and physical-device requirements remain; see TEST_RESULTS.md for exact evidence. No UI palette, logo, navigation or glass style changed.

## Live permission-loss follow-up

Overlay AppOps revocation and AccessibilityService revocation are now verified for a real text-only session on the API37 emulator, including window/session cleanup and no restart on restored access. This narrows the earlier unverified lifecycle finding; it does not certify audio capture, other OS/OEM devices or process death. Browser setup is pending first-run consent. See TEST_RESULTS.md.
