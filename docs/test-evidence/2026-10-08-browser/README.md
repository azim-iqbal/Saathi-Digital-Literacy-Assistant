# 8 October 2026 — browser/source boundary remediation

Base commit: `d91b216` (clean at start). No live providers, real accounts or protected portal actions used.

## Reproductions

- `url-before.txt`: numeric/local/special-use source host accepted before fix.
- `certificate-before.txt` and `server-warning-before.txt`: browser security warning did not consistently withhold Continue navigation/controls.
- `private-before.txt`: semantic private context blocked cloud but did not enter the shared private/listening handover.
- `clock-before.txt`: missing retrieved-time bound in local evidence review; new test initially could not compile against the old contract.
- `source-first.txt`: fixture selector said Private account instead of Private interruption. Fixed selector only.
- `source-second.txt`: test reused a destroyed ActivityScenario after a second activity launch. Fixed lifecycle harness, not application semantics.

## Passing evidence

- `url-tree-after.txt`: exact-source URL and browser-chrome traversal tests, including web-content spoof, truncation, ambiguous candidates and edited addresses.
- `server-warning-after.txt`, `backend-all.txt`: security-warning rejection then 140 complete offline backend tests.
- `android-final.txt`: 125 Android unit tests, debug/test build, release Kotlin compilation and lint.
- `source-chrome.txt`: 3 source/Chrome/pause-resume integration tests passed.
- `emulator-final.txt`: 10 focused integration/UI tests passed, before the last semantic-private-handover change.
- `privacy-final.txt`: 4 final-build private-message/source/Chrome/pause-resume/voice regression tests passed.
- `evaluation-tests.txt`: 5 harness tests; `offline.json`: 51/51 probes; `research-acceptance.txt`: controlled research architecture test across catalog domains/locales.

Counts from overlapping emulator runs must not be added as distinct tests. Test fixtures are fictional and do not prove source/model truth, genuine TLS/site safety, real speech delivery or arbitrary live task completion. Brave adapter traversal is unit-tested; Brave is not installed/tested as a real browser here. The actual Chrome fixture is HTTP and correctly cannot establish a full HTTPS source match. Positive matching is deterministic policy evidence only, not live browser authority certification.

Synthetic UI screenshots were exported from the passing research tests. They were not separately visually reviewed in this final run. Test fixture servers/emulator were stopped during handoff cleanup; temporary tokens and reverse mappings removed. No reset credit consumed. Final usage check was 13% remaining, below the requested 15% floor; no new implementation was started after that check.
