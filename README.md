# Saathi — Digital Literacy Assistant

Saathi helps people learn screen navigation while performing every tap themselves. **Saathi supports synthetic Bill Pay practice and an experimental local option finder for accessible apps/browser controls.** The live finder matches a named visible option; universal workflows and screen capture remain unavailable. A debug-only backend connection supports separately consented Gemini/Groq navigation once server credentials and model IDs are supplied.

[Development Roadmap](docs/EXECUTION_PLAN.md) · [Architecture Audit](docs/CURRENT_STATE_AUDIT.md) · [Documentation Index](docs/README.md) · [Figma Designs](https://www.figma.com/design/vx2M28p625yZQTAzcTLlQj)

## Build and try

Use JDK 17, Android SDK 35, Gradle wrapper 8.7 and AGP 8.6.1. Set only `sdk.dir=/absolute/path/to/Android/sdk` in ignored `local.properties`. Provider keys are neither needed nor read by the Android build.

```sh
./gradlew testDebugUnitTest assembleDebug lintDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. Install on an Android API26+ test device, enable Saathi Accessibility and overlay access, start a bill practice task and open Demo Bill Pay. Use made-up values only. No money moves. Notification Stop ends guidance; lifecycle/device validation is still pending.

English, Hindi and Hinglish guidance strings exist; the new Compose shell also has these three languages. Native-language, voice quality and accessibility review remain pending. Task-screen voice uses the device recognition service and is not guaranteed offline. See [privacy](PRIVACY.md) and [test results](docs/TEST_RESULTS.md).

No universal compatibility, production readiness, perfect redaction, unlimited free AI or successful real payment is claimed. The new app shell uses Jetpack Compose: welcome, home, practice, task intake, setup, session controls, settings and privacy. The synthetic practice activity remains Android Views. Editable light/dark Figma screens exist; full design parity, remaining variants and a recorded demo are pending. See docs/screenshots/2026-09-29 for emulator captures.

The navigation now uses a floating capsule with spring-driven selection and swipable synthetic Practice categories. [Implementation and measured limits](docs/MOTION_AND_HAPTICS.md) · [Dark Practice screenshot](docs/screenshots/2026-09-29-navigation/practice-dark.png). Emulator performance is below target; physical-device profiling remains required.

Latest UI phase: shared Liquid Glass controls and branded header are implemented in the Android shell. See [scope and remaining parity](docs/GLASS_UI.md), [current verification](docs/TEST_RESULTS.md), and [dark Home screenshot](docs/screenshots/2026-09-29-glass/home-dark.png). Native/legacy screen parity, Figma synchronization and physical-device performance remain open.

The latest launch update adds shared vector brand assets, adaptive/themed launcher support and a centered animated opening. [Launch details and limits](docs/LAUNCH_EXPERIENCE.md).

The 30 September continuation adds explicit background practice conversation with on-device speech turns and notification controls, plus wrong-category/detour recovery anchored to Back to choices. This is a local command-based assistant; natural AI chat remains pending; external guidance is experimental. [Voice behavior and verification limits](docs/VOICE_CONVERSATION.md).

The Python gateway is connected to the debug app for mock practice and optional paired-model navigation. Gemini/Groq REST adapters, strict validation, persistent aggregate AI call caps, cancellation and current-screen grounding are implemented. Credentials/model IDs remain placeholders; release connectivity and production hosting/authentication remain open. [Local setup and production gaps](docs/SETUP.md).

Choose **Help in apps & browsers** for the floating assistant: type or dictate a visible option name, select **Text only** or **Text + voice**, then start and open your app. The movable Saathi button returns to the request panel. [Implemented behavior and compatibility limits](docs/LIVE_ASSISTANT.md).
