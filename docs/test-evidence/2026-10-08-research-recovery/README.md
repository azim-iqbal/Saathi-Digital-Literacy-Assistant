# Research recovery evidence — 8 October 2026

All requests used offline fixtures or emulator loopback servers. No remote providers, accounts, real sites, private data or transactions were used. Production/release/deployment were not exercised.

- `before.txt`: reproduced retained cancelled evidence, blocked metadata reads and unsanitized retrieval exception failures.
- `retry-before.txt`: reproduced reused plan-session identity.
- `after.txt` / `after-fixed-fixture.txt`: retain the new test helper argument error and pre-existing incident fixture timestamp race. Fixed fixture inputs; production freshness was not relaxed.
- `targeted-final.txt`: 35 targeted tests passed at the earlier stage.
- `retry-end-to-end.txt`: 7 recovery regressions, including actual Gateway failure→explicit paired retry→accepted REVIEW_REQUIRED plan, duplicate replay, exhausted quota, cancellation and expiry. Synthetic providers only.
- `backend-final.txt`: 147 complete offline backend tests passed.
- `android-build.txt`, `test-build.txt`, `android-final.txt`: debug/test builds, 127 unit tests and lint; 0 errors / 61 warnings.
- `evaluation-tests.txt`: 4 harness tests plus 1 multilingual research architecture acceptance test passed.
- `offline.json`: 51/51 offline capability probes passed. These are not 51 completed real workflows.
- `emulator-first.txt`: 4 focused tests passed (recovery UI, two protocol tests and pause/resume).
- `emulator-final.txt`: 10 final synthetic UI/lifecycle tests passed. It covers explicit retry without automatic calls, preserved excerpts, expiry refresh, edited-task cleanup, cancelled late responses, research consent/checklists/outage/large-font Hindi, protocol, pause/resume, source companion and voice lifecycle. Test groups overlap; do not add their totals together.

The dark dependency checklist and Hindi large-font screenshots were saved from synthetic test screens; no private information is shown. Test server, emulator, temporary token and reverse mapping were cleaned up after verification.

Earlier provider failures, real extraction quality, HTTPS source matching on actual sites, physical speech/OEM endurance and the historical WebView transition cause are not resolved by these tests.
