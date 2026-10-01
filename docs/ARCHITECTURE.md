# Current architecture — local-practice pilot

## Latest backend/browser continuation — 1 October 2026

Debug Android now connects to the loopback gateway for mock practice and separately consented AI navigation. Independent Gemini/Groq REST adapters and ignored credential/model placeholders are implemented. A live proposal is accepted only on paired agreement, a current eligible control and fresh observation. The app resolves bounds locally and uses its localized instruction template. Recent suggested labels support step-by-step wrong-path recovery, but are not claimed clicks or completion evidence. Default/release guidance stays local. See [AI_ORCHESTRATION.md](AI_ORCHESTRATION.md) and [SETUP.md](SETUP.md), which supersede older disconnected-backend notes below.

Actual Chrome on the API37 emulator passed a localhost synthetic-page flow: highlight/tap-through, detour/return, private password screen suspension, retargeting and Stop. The live Android/backend route passed wrong-path recovery using two **simulated** provider adapters. No provider account was configured, no external model request made, and no real account/transaction used. The user chose emulator testing for now.

Voice setup now offers an explicit installed-language check/download through the device speech service, with the existing brand, palette and glass controls. TTS completion callbacks succeeded in all3 languages; recognition returned missing-language error13. No intelligibility, microphone transcript or successful language download is claimed. Natural streaming/barge-in, physical-device/OEM survival, release backend connectivity and hosted authentication/TLS remain open.


**30September scope expansion:** an explicit `startLive` path now runs `LiveGuide` for external accessible packages. Practice remains separately gated by `PracticeSurfacePolicy`. Live mode matches a single public clickable label (or its observed clickable parent) without a model or automatic action; protected/empty/ambiguous surfaces do not get guessed targets. See LIVE_ASSISTANT.md for limits. `AssistantActivity` provides typed/dictated intake, independent output-mode controls and the last in-memory reply. `AssistantBubbleService` is a small draggable interactive window; the full-screen highlight remains noninteractive and uses the system opacity limit. A current snapshot index grounds browser controls that have no resource ID. No network or screenshot path was enabled.

The primary UI is the Compose `SaathiApp`; the separate Android Views Bill Pay activity is the only synthetic practice fixture. MainActivity starts an explicit session. Accessibility callbacks invalidate the previous presentation immediately; one off-main tree copy is allowed at a time with a dirty flag for the next observation. Results return to main with a session/revision/package/window ticket. The ticket must still match before evaluation.

`PracticeSurfacePolicy` requires both Saathi's package identity and a registered Bill Pay control before `DemoGuide` runs. Any empty, protected, transitioning, unrelated, or same-package-but-unregistered screen clears the overlay and spoken output and becomes `WaitingForPractice`. A current fixture target must resolve to one enabled, non-sensitive node; private fields enter `SensitiveHandover`, and synthetic success enters `Completed`. The overlay rechecks presentation identity when service delivery begins.

No model account is configured and screenshot capture is disabled. The old direct-provider helpers remain removed. Debug Android connects to a server-side paired-proposal gateway; release connectivity and public hosting/authentication remain unfinished. See SETUP.md and AI_ORCHESTRATION.md for current modes and boundaries. Notification Stop invalidates session state and releases session resources. See TEST_RESULTS for emulator lifecycle coverage; physical-device coverage is still pending.

Conversation/goal state and completion state are memory-only. Language and appearance preferences persist. See PRIVACY.md, CURRENT_STATE_AUDIT.md and EXECUTION_PLAN.md for unresolved boundaries and migration steps.

## Background voice and recovery — 30 September

An explicit visible-activity action requests microphone/notification permission and starts `VoiceConversationService`. It uses on-device recognition on supported Android 12+ devices, alternates TTS and recognition, and stays alive in the background with notification Pause/Stop. `VoiceTurnGate` rejects stale recognition/TTS callbacks. Screen transitions suspend audio immediately; validated practice snapshots restart an eligible turn. Private forms disable microphone turns. Audio-focus loss, errors and silence pause; screen-off stops. The service never restores microphone consent after process death. This is a local command conversation, not an AI streaming transport. See [VOICE_CONVERSATION.md](VOICE_CONVERSATION.md).

Practice category markers and a real Back to choices button let `DemoGuide` correct a wrong-category or unrelated-tile detour. Recreation retains the category and clears form values. Accessibility rejects other packages before tree copying. Clearing the overlay presentation retains its window to avoid remove/re-add event churn; new targets get a finite settling pulse, respecting reduced motion. Physical-device touch passthrough and audio interruption still need verification.


## Compose shell — 29 September

LaunchActivity renders MainActivity → SaathiApp. Preferences stores onboarding/language/theme/speech/motion/haptics; Copy provides English/Hindi/Hinglish shell text. PracticeTask accepts supported synthetic task categories. The UI observes SaathiSession status and Android permission readiness. Voice transcription is user-triggered and editable before continuing. DemoBillPayActivity remains the deterministic local fixture; LegacyTaskActivity is retained and non-exported. The session UI distinguishes preparing, observing, analysing, guiding, waiting, sensitive handover, paused, completed, stopped and error; it is not a cloud-processing claim. No backend was introduced.
