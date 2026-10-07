# Research remediation evidence — 7 October 2026

This directory contains deterministic/local evidence, not successful genuine-provider or real government/financial workflows. No live provider calls were made. Summary: [verification.json](verification.json). Overall remediation remains open; see [capability matrix](../../REMEDIATION_CAPABILITY_MATRIX.md).

## Final passing results

| Evidence | Scope |
|---|---|
| `backend-final.txt` | Complete backend suite: 139 tests |
| `android-final-validation.txt` | Android unit/build validation; 111 unit tests, debug/test and release Kotlin compilation |
| `evaluation-harness-final.txt`, `evaluation-final.txt`, `offline-evaluation-final.json` | 5 harness tests and 51/51 offline probes; not 142 completed real workflows |
| `emulator-final-validation.txt` | 18 final focused browser/research/pause-resume/lifecycle regressions |
| `emulator-extended.txt` | 22 UI/privacy/transport tests, including Hindi at 200% font; overlaps other groups |
| `connection-final.txt` | 3 connection/diagnostic checks using fake REST providers |
| `practice-gateway-final.txt` | 4 practice gateway checks; accepted private handover and observed completion retained |
| `webview-endurance.txt`, `webview-endurance.json` | 60 transitions in one session, about 77.3 s measured loop; PSS 106223→109262 KB; not a leak-free or hours-long claim |
| `lifecycle-resource-samples.json` | 120 pause/resume cycles with resource samples; subsequent idle failure investigated separately |
| `cloud-event-storm-latest.json` | 660 fixture events/60 mutations → 83 service records, 9 observations, 3 HTTP attempts, 1 cancellation; fake backend, no live costs |
| `android-complete-checks.txt` | Lint 0 errors/61 warnings; existing warnings remain |

Earlier `cloud-event-storm-final.json` is a separate passing run with 6 HTTP attempts and 3 cancellations; do not combine runs or describe framework coalescing as semantic request deduplication. Emulator groups overlap; counts are not a unique summed total. Logs named “final” before the latest filenames are intermediate runs, not stronger evidence.

## Reproductions and repairs

- `baseline.txt`: absent research architecture before implementation.
- `dns-multicast-before.txt` / `dns-multicast-after.txt`: reproduced unsafe DNS classification, explicit multicast/reserved/unspecified rejection, regression passing.
- `idle-failure-events.txt` / `emulator-after-window-fix.txt`: late old-window content event invalidated a newer root; narrowly filtered by current root identity. This does not establish the historical WebView post-tap failure's cause.
- `emulator-extended-before-consent-fix.txt`: debug AI default was preselected; corrected to explicit stored opt-in, rerun passing.
- `practice-gateway-old-expectation.txt`: outdated private-form instruction assertion; corrected to accepted handover/no-target behavior without relaxing privacy or completion assertions.
- Chrome readiness/cache/pipe logs preserve intermediate setup failures and retests. Test flags/debug-app settings are restored; production bypasses were not added.
- Other earlier logs may contain failed/interrupted runs. Use the scoped final results above; passing reruns do not erase unknown causes.

## Screenshots

`research-dependency-dark.png` and `research-eligibility-dark.png` show synthetic reviewed-source/plan interfaces with existing styling. `research-hindi-large-font.png` shows the scrollable Hindi interface at 200% font. These are fictional fixture evidence, not official source verification. Tests temporarily clear and restore FLAG_SECURE for these synthetic captures; normal research screens retain screenshot protection.

## Cleanup and limits

Task-local fixture servers/emulator stopped; test reverse mappings and temporary token files removed. Ignored real credentials unchanged. No deployment, signing, commit or push. Weekly usage reached 16% and the saved continuation floor is 25%; remaining implementation is documented rather than marked complete.
