# Final mandate verification — 9 October 2026

Run from the repository root. No deployment, signing, publishing or Git writes are included. Preserve failures and use a new output directory for subsequent runs.

## Offline

```
python3 -m unittest discover -s backend/tests
python3 -m unittest discover -s evaluation -p 'test_*.py'
python3 -m evaluation.run --output /tmp/new-offline-results.json
python3 -m evaluation.reliability_stress --output /tmp/new-stress-results.json
./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest compileReleaseKotlin lintDebug
python3 -m backend.tests.hosted_smoke
```

Backend socket tests require loopback permission; provider TLS tests create and trust a temporary local certificate only inside the test. The installed production runner is pinned in `deploy/requirements.txt`. This run used JBR 21.0.11 and Python 3.14; the container target is Python 3.12 and was not built here because Docker is unavailable.

## Android

Install the debug and instrumentation APKs on an isolated emulator. Supply an absolute adb path, emulator serial and new output directory to `run_emulator_groups.py`. It runs fixture servers only, preserves permission state through the individual tests, removes its reverse mapping/device token, and excludes ProviderSmokeTest. Synthetic host token files remain private under `/tmp` if they pre-existed.

The separate endurance command (serial emulator-5554 in this run):

```
adb -s emulator-5554 shell am instrument -w \
  -e class com.saathi.ui.MixedLifecycleEnduranceTest \
  -e mixed_minutes 120 -e mixed_broad true -e mixed_rounds 1 \
  -e webview_cycles 3 -e handoff_rounds 3 -e endurance_cycles 3 \
  -e additionalTestOutputDir /data/user/0/com.saathi/files/mandate-two-hour \
  com.saathi.test/androidx.test.runner.AndroidJUnitRunner
```

Read `files/mandate-two-hour/mixed-endurance.json` through `adb exec-out run-as com.saathi` for checkpoints. Do not install/relaunch that app or run another instrumentation suite on the same emulator during endurance. A second temporary AVD was used for the other regression groups. Its initial failed same-profile launch is retained separately.

## Genuine evaluation — DO NOT REPEAT WITHOUT NEW AUTHORIZATION

The eight-call allowance was completely consumed on 9 October. The four predeclared fictional cases are in `evaluation/final_acceptance.py`; the exclusive marker prevents reuse of a result directory. Expected scoring criteria are outside model prompts. Network traces retain fixed phase names/times, not provider bodies or secrets.

A future separately authorized run:

```
SSL_CERT_FILE=/etc/ssl/cert.pem python3 -m evaluation.final_acceptance --authorized --output /private/path/new-authorized-run
```

Use the target host's real trusted CA bundle; never disable certificate verification. Two navigation cases passed. Planning and incident pairs timed out waiting for Gemini headers; Groq also failed independent semantic expectations. Post-run general validation/prompt changes have offline evidence only. Do not label them genuine-model retest success.

## Preserved failures

- `navigation-budget-before.txt`: primary consumed the fallback budget.
- `narrative-before.txt`, `narrative-backend-before.txt`: narrative context did not trigger private handoff.
- `reasoning-before.txt`: explicit source ordering and denials were not checked.
- `default-route-before.txt`: default routing was still Gemini-first.
- `portal-tests.txt`, `portal-retest.txt`, `portal-traversal.txt`: WebView text search in the test driver omitted observable buttons/declaration; full traversal fixes the driver. These are distinct from the historical WebView incident.
- `core.txt`: Settings tree changed during a test poll; production correctly rejected the incomplete tree. The test now waits for a complete tree within its existing deadline.
- `stress.txt`: harness expected `ready` instead of the actual research contract `researched`; no production change.
- `hosted-smoke.txt`: selected Python environment lacked Gunicorn; pinned temporary environment then passed all six checks.
