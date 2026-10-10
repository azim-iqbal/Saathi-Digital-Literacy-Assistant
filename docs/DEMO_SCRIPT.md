# Saathi mentor demonstration

Updated 10 October 2026. This walkthrough uses the existing app interface. It demonstrates local assistance and the reporting worksheet with fictional data; it does not certify arbitrary workflows, live AI accuracy, a submitted complaint or production deployment.

## Before the meeting

Use a dedicated Android emulator on the presentation computer. The Android app's local features do not require a backend, model key or Internet connection. Keep AI navigation disabled for this rehearsed route. Use the existing Backend connection settings to disable it if a previous session configured it; the launcher preserves that setting rather than silently changing it. Do not run Check APIs or Ask AI during the rehearsal: the previous four-call allowance is exhausted.

Build the debug app and separate compatibility fixture once:

```sh
./gradlew assembleDebug assembleDebugAndroidTest
python3 tools/mentor_demo.py --install
```

The launcher opens Saathi. It does not change permissions, send input, start a backend or call models. It refuses physical devices to avoid accidentally disturbing a personal phone. If several emulators are running, pass `--serial emulator-5554` with the intended serial. The separate fixture APK is synthetic test content and is not part of the production application.

In Saathi, complete the existing welcome flow. Grant accessibility and display-over-other-apps access through the app's existing controls. Choose **Text only**. Microphone access is unnecessary for this route. Choose the existing Dark theme if desired; do not reset app data to prepare a demo.

## Five-minute walkthrough

### 1. Explain the product (30 seconds)

“Saathi helps people understand their next action in another app. The user stays in control of typing, tapping and submitting. Local guidance can identify accessible controls and form fields; the backend adds consented research and model reasoning, whose reliability we evaluate separately.”

Show the home screen, existing navigation and request panel. Do not describe this build as bug-free or universally compatible.

### 2. Cross-app guidance (one minute)

Enter **Find Help**, choose **Text only**, and select **Start on-screen help**. Then open the separate demonstration surface:

```sh
python3 tools/mentor_demo.py --screen navigation
```

Saathi should mark Help in the other APK. Select **Explore** yourself to leave the correct page. Guidance should stop marking the missing option; choose **Back to choices** yourself and observe the fresh marker. The app never taps for you.

Explain: “This is a controlled external Android surface, not a recorded model reply or proof that every third-party app is supported.”

### 3. Reactive form guidance (one minute)

Return to Saathi or its floating request panel. Change the request to **Help fill form** and start/update on-screen help. Open:

```sh
python3 tools/mentor_demo.py --screen native-form
```

Enter `Fixturetown` in City, then select **Finish editing**. Guidance should move to State and skip the empty optional Company field. Clear City and finish editing; the city marker should return. Fill City and State with made-up values, then finish editing. The app should ask for your review, not claim the data is valid or submit the form. Show **Show dependent** / **Hide dependent** to demonstrate freshly appearing/disappearing fields. **Invalid city** / **Correct city** provide explicit synthetic validation metadata.

For the equivalent WebView route:

```sh
python3 tools/mentor_demo.py --screen web-form
```

No zoom, rotation or manual accessibility refresh is part of this walkthrough. A focused field remains the current field while the user edits it. Unknown metadata remains uncertain; field presence does not establish validity.

### 4. Pause and privacy (45 seconds)

Pause from Saathi, return to the request panel and select **Resume guidance**. The request should remain but the screen must be observed again. Do not imply a stopped or killed app silently restarts itself.

Optional controlled privacy example:

```sh
python3 tools/mentor_demo.py --screen portal
```

This is a synthetic portal inside the separate test APK, not the cybercrime website. Its private and CAPTCHA pages demonstrate the wait/resume boundary. The presenter navigates the fixture. Saathi must not read the value, solve a challenge, infer successful authentication or submit anything. Listening requires explicit reactivation after private handoffs.

### 5. Reporting worksheet (one minute)

Stop guidance. In Saathi choose **Report cyber fraud**. Use this fictional account:

> Someone asked me to send money for a prize. I declined and kept the message.

Choose **Show reporting advice**, then **Prepare complaint draft**. Review the account, leave unknown details blank, and select **I checked this matches what happened**. Choose **Copy incident description**. Demonstrate **Not now**, then reopen and approve **Allow copy**. Changes to the account require review again.

Explain: “This worksheet arranges the user's facts locally. It does not invent evidence, establish that a crime occurred or submit a complaint. Copying requires review and consent.” Do not open authenticated real forms, enter identity data or submit a report for the demo. Clear the worksheet and clipboard after the demonstration.

## If something goes wrong

| Symptom | Presenter fallback |
| --- | --- |
| Start button disabled | Use the existing permission controls; return to Saathi after granting the requested access. Android owns those screens. |
| Overlay not visible in a protected app | Return to Saathi for text. Do not bypass the app's overlay protection. |
| No accessible control / ambiguous fields | Show the uncertainty message and choose the controlled fixture. Do not say the app verified an inaccessible field. |
| Speech unavailable or noisy room | Keep Text only. Audio quality and background survival still need a physical-phone session. |
| Backend unavailable | Continue the local route. A phone without USB cannot reach a computer-local server through emulator addresses. Hosting is deferred. |
| Research/models disagree or time out | Withhold the plan. Show the saved diagnostics if discussing the architecture; do not substitute fixtures and call them genuine AI. |
| Presenter loses their place | Stop, open Saathi, state a fresh request, then reopen the desired fixture with the launcher. |

## What can be said about the backend

The backend has authenticated status, bounded provider checks, deadlines, cancellation, quotas, source validation and paired reasoning safeguards. On 10 October the genuine Hindi incident example passed both providers. The English planning example was withheld because the providers disagreed; Groq missed independent sequence expectations. Subsequent validator changes have offline evidence only. These are small sample results, not broad accuracy measurements.

Production hosting, multi-instance durable storage, final physical-device acceptance and legitimate protected-portal acceptance remain separate work. Do not present this rehearsal as completing those gates.

## Demo artifacts

- Prepared app: `deliverables/mentor-demo/Saathi-Mentor-Demo-debug.apk` (debug build; not a signed production release).
- Build output: `app/build/outputs/apk/debug/app-debug.apk`.
- Separate fixture: `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`.
- Rehearsal evidence: `docs/test-evidence/2026-10-10-mentor-demo/`.
- Current remaining engineering work: `docs/NEXT_CONTINUATION.md`.

## Optional local commerce rehearsal — 10 October

Use the current debug APK and separate fixture APK. In Saathi request **Order milk**, choose Text only and start live help with the existing accessibility/overlay permissions. Run `python3 tools/mentor_demo.py --screen commerce`. Follow Search, the marked Add belonging to Fixture milk, then View cart. Review stays a user decision. The fixture private step demonstrates CVV handoff and Return retains the same task. There are no real orders.

Label this clearly as a **synthetic cross-app demonstration**, not Zepto. The real Zepto acceptance path is still blocked by missing app/device access. Do not claim all products, checkout or real ordering work from this rehearsal.
