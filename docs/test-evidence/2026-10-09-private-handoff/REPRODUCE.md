# Reproduction (local tests, no paid provider calls)

From repository root, with the existing JDK/SDK installed:

```sh
export JAVA_HOME=/Users/azimiqbal/Library/Java/JavaVirtualMachines/jbr-21.0.11/Contents/Home
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:compileReleaseKotlin :app:lintDebug
python3 -m unittest discover -s backend/tests
python3 -m unittest discover -s evaluation -p 'test_*.py'
python3 -m evaluation.run --output /tmp/saathi-offline.json
```

Install the debug app and androidTest APK on the existing emulator. Run these instrumentation classes with `am instrument -w -e class <comma-separated classes> com.saathi.test/androidx.test.runner.AndroidJUnitRunner`:

- NodeMaskerPrivacyTest, PauseResumeIntegrationTest, ReviewedSourceIntegrationTest, VoiceLifecycleTest, TravelPrivacyUiTest, PermissionLossIntegrationTest, GatewayTransportIntegrationTest, LiveGatewayCodecTest (package `com.saathi.ui`).
- LiveAccessibilityIntegrationTest with `-e webview_cycles 120`.
- PauseResumeIntegrationTest with `-e handoff_rounds 20`.
- ChromeGuidanceIntegrationTest requires `python3 -m http.server 8766 --bind 127.0.0.1 --directory backend/fixtures` and `adb reverse tcp:8766 tcp:8766`.
- LiveAiIntegrationTest requires `python3 -m backend.tests.device_server`, `adb reverse tcp:8765 tcp:8767`, and pushing its generated `/tmp/saathi-gateway-test-token` to `/data/local/tmp/saathi-test-token`. These are fictional providers, not live Gemini/Groq.

Use `-e additionalTestOutputDir /data/user/0/com.saathi/files/handoff-final`; extract through `adb exec-out run-as com.saathi tar -C files/handoff-final -cf - .`. Screenshots use the same explicit baseline/after class list in `baseline-ui-retry.txt` and this phase's report. No ProviderSmokeTest or live-model script is part of this suite.

Stop fixture servers, remove emulator reverse mappings and the temporary fixture token after testing. Never load or print backend/.env for these tests.
