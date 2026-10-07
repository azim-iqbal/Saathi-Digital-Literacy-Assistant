# Saathi capability evaluation

This suite measures existing production paths. It never injects domain answers into the app or promotes synthetic highlights to completed real-world workflows. `catalog.json` contains 142 operator scenarios and their hidden complications. Expectations stay outside provider prompts.

## Repeatable offline run

From the repository root:

```
python3 -m evaluation.scenarios
python3 -m unittest evaluation.test_harness
python3 -m evaluation.run --output /tmp/saathi-offline-results.json
python3 -m unittest discover -s backend/tests
```

No `.env`, provider account or network access is used by the offline runner. Backend HTTP regressions use loopback sockets. CI runs these checks; it never calls `live_models.py`. The first and corrected evaluation artifacts remain under `results/`.

## Controlled Android integration

Use an isolated emulator with no personal accounts. Build/install debug and instrumentation APKs. Serve `backend/fixtures` with `python3 -m http.server 8766 --bind 127.0.0.1 --directory backend/fixtures` and reverse emulator TCP 8766 to host 8766. Confirm `/browser.html` is the synthetic page (the mock gateway on the same port does **not** serve it). Chrome must have completed first-run setup before its test; do not mistake onboarding for a navigation failure.

Run `LiveAccessibilityIntegrationTest`, `ChromeGuidanceIntegrationTest`, `TravelPrivacyUiTest`, `PermissionLossIntegrationTest`, `GatewayTransportIntegrationTest`, and `VoiceLifecycleTest` through AndroidJUnitRunner. Never include `ProviderSmokeTest` in an unrestricted class list. The external fixture emits actual accessibility events; tests inject user taps, not production autonomous actions. The event diagnostic observer contains only event metadata/timings and has no release storage.

Save `files/event-storm.json`, synthetic screenshots and runner output. Record device/API/browser versions, first-run state, failed attempts, server setup and measured scope. PSS changes in one short sample do not establish a memory leak. The local-match storm does not test cloud dispatch under equivalent events.

## Genuine provider run

`python3 -m evaluation.live_models --authorized` is NOT a default test command. Obtain explicit scoped authorization first. The 7 October run used its entire twelve-call authorization (six paired cases). Do not repeat it under that authorization. The `.started` marker prevents accidental repeats. A later approved evaluation needs a separate run directory/identifier and its own bounded reservations. Existing durable server budgets remain in force. TLS verification stays enabled. No keys or raw HTTP errors are written to results.

This paired adapter/gateway probe is deliberately distinct from the app's primary-then-fallback navigation policy. It is not an Android E2E or current-web research test. Current artifacts preserve both provider diagnostics even when the pair is rejected. Rejection does not prove that each model selected an incorrect target.

## Manual real-world acceptance

Choose an ID, specify device/app version, language, goal, initial state and hidden complication. Use public pages or a permitted sandbox. Do not enter credentials/private values, accept legal declarations, bypass CAPTCHA/certificates, submit applications or transact for evaluation. Stop and mark BLOCKED where these are required.

1. Record the actual instruction/highlight and each user action, including mistakes, Back and layout changes. Redact screen recordings before retaining them.
2. For changing facts, record authoritative URL, date/retrieval time, jurisdiction and exact claim verified. Community evidence needs author/date attribution and an explicit anecdotal label. Our own research is not evidence that Saathi researched.
3. Check dependency discovery, missing information, four eligibility states and return to the original goal. A supplied mock dependency is not a discovered prerequisite.
4. Track provider calls/tokens, freshness, permission state, elapsed times and completion evidence. Mark unmeasured fields null.
5. Score each observed dimension: 0 harmful/absent; 1 major failure; 2 substantial assistance needed; 3 succeeds with material limitations; 4 correct with minor issues; 5 fully correct in the tested case. Never extrapolate to other sites.
6. Import a result through `python3 -m evaluation.scorecard input.json output.json`. Full PASS requires all eleven dimensions observed, each >=3, evidence/date and no critical gate failure. A critical failure forces FAIL regardless of total. BLOCKED/PARTIAL may retain null dimensions and null /55 totals.

A record has `scenario_id`, `level`, `status`, `observed_at`, `evidence` (paths/URLs), `critical_failures` (see `scorecard.py`) and `scores` (all eleven dimensions from `run.py`). Document model/fixture provenance and source evidence alongside it. Never put secret values in evidence.

## Current limitations of the harness

142 catalog cases are NOT 142 executed E2E tests. The executable offline probes cover shared backend boundaries, not rules for individual services. Source retrieval, dependency/eligibility reasoning, private-message semantics, semantic event deduplication, arbitrary completion and natural conversation remain evaluation/release gaps. Do not remove a critical failure just because an unrelated score improved.
