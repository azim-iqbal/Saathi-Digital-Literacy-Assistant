# Saathi — Digital Literacy Assistant

Saathi helps people find their way around digital tasks on Android. It points out the next option, explains the step in text or speech, and leaves the tap to the person using the phone.

The app brings together a practice space, an experimental assistant for other apps and browsers, and a cybercrime-reporting companion. It is an active development project. Local practice works without model accounts; AI guidance currently requires the debug app and a local backend.

[Setup](docs/SETUP.md) · [What changed](CHANGELOG.md) · [Test results](docs/TEST_RESULTS.md) · [Privacy](PRIVACY.md) · [Documentation](docs/README.md)

## What you can try

| Area | Current behavior |
| --- | --- |
| Practice | Walk through synthetic electricity, water and TV-recharge tasks. No money moves. |
| Help in apps and browsers | Ask for a visible option by text or voice. A floating button reopens the request panel; choose text-only or spoken guidance. Accessible controls vary by app. |
| AI navigation | In the debug build, Gemini and Groq independently suggest a step from the current eligible controls. Saathi requires agreement before showing it. |
| Report cyber fraud | Follow an offline reporting checklist, open the official portal, and optionally request a consented AI assessment. The assessment is provisional. |
| Complaint drafts | Describe what happened, fill in any known details, review the draft, and approve each field before copying it. Return to the existing browser tab to paste it yourself. |
| Connection checks | Check the local server without making a model call, or explicitly test both providers and inspect their separate results and available token usage. |

The interface uses a shared green palette, rounded glass controls, a floating navigation capsule and vector brand assets. Light and dark themes are available. English, Hindi and Hinglish are supported in the main guidance flows; newer reporting and setup copy still needs language review.

## Run the Android app

Use JDK 17 and Android SDK 35. Set the SDK path in your ignored `local.properties` file:

```properties
sdk.dir=/absolute/path/to/Android/sdk
```

Build with the supplied wrapper:

```sh
./gradlew testDebugUnitTest assembleDebug lintDebug
```

Install `app/build/outputs/apk/debug/app-debug.apk` on an Android API 26+ test device or emulator. Start with **Practice** and use made-up values. Enable accessibility and overlay access when the guidance flow asks for them. The notification's **Stop** action ends a session.

Provider keys are not needed to build Android and never belong in the APK. To connect the optional AI features, follow [local backend setup](docs/SETUP.md). The server binds to loopback and uses a development token; it is not a hosted production service.

## What has been checked

The latest saved verification includes 75 Android unit tests, 55 backend tests and 12 focused emulator checks. Debug and release builds passed. Lint reported no errors and 61 warnings. These checks were run in the phases recorded in [Test results](docs/TEST_RESULTS.md), rather than as one full acceptance run.

Both real providers returned valid responses through the Android-to-backend check. Those successes occurred on separate requests: the last paired check timed out on Groq, and Saathi rejected the incomplete result. See [provider verification](docs/PROVIDER_VERIFICATION.md) for the evidence and what it does—and does not—establish.

## Work still ahead

Reliable multi-step AI guidance and incident-assessment accuracy need more evaluation. Private portal forms and browser paste behavior have not been verified end to end; Saathi does not fill or submit complaints automatically. The cause of an intermittent WebView transition failure is still under investigation.

Real microphone behavior, voice quality, background survival and performance need physical-device testing. Continuous conversation currently uses turn-based device speech services, not natural streaming speech. Some apps deliberately hide overlays, and Saathi respects those restrictions.

Production hosting, per-user authentication, release-build AI connectivity, full localization and remaining design parity are also open. The [execution plan](docs/EXECUTION_PLAN.md) tracks these items separately from completed work.
