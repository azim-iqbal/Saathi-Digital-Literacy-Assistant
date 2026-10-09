# Complete observation and lifecycle regression — 9 October 2026

No genuine model calls, deployment, commits, UI layout/theme changes or reset credits.

## Reproduced defect and fix

`tree-before.txt` reproduces two failures: partial trees silently pass on node/depth limits or missing children. `CompleteTreeWalk` now rejects incomplete trees and releases each acquired child in `finally`. The caller retains root ownership. `tree-after.txt` and the final Android suite pass. Six walk tests include exact-limit acceptance, read failures, cancellation and 10,000 mixed successful/failing traversals. These are ownership tests, not Android heap leak certification.

Accessibility service copying now stops further reads after destruction or its two-second budget. Accepted queued work still executes cleanup rather than being discarded by shutdownNow. Capture time begins before copying; session delivery rejects observations outside its 15-second freshness limit. Private/CAPTCHA handling and user-action boundaries remain intact. Large/slow pages may safely withhold guidance; physical-device latency remains unverified.

Debug diagnostics expose only fixed observation-failure categories; release is no-op. The sanitization regression rejects arbitrary exception text.

## Fixture failure, diagnosed separately

Initial new integration failed because the standalone test APK process could not load Kotlin Intrinsics in the virtual-node provider. `fixture-java-crash.txt` contains the stack; `diagnostic/failure-screen.png` shows the test APK crash dialog. Moved that new fixture to Java using only Android APIs. No production dependency change. `emulator-fixed.txt` passes. The final combined run additionally requires an actual incomplete-observation diagnostic before asserting recovery, excluding a transition-only false pass.

This is NOT the cause of the historical intermittent WebView post-tap failure. `fixture-crash.txt` is unrelated emulator UWB native crash output and must not be cited as an app defect.

## Reproduction

Use the commands and loopback fixture setup in `../2026-10-09-private-handoff/REPRODUCE.md`. Run Android unit/debug/test builds, release Kotlin and lint. Backend: `python3 -m unittest discover -s backend/tests`. Evaluation: `python3 -m unittest evaluation.test_harness evaluation.test_research_acceptance evaluation.test_continuation_probe`; `python3 -m evaluation.run --output /tmp/saathi-offline.json`.

Final instrumentation class list (all under com.saathi.ui): ObservationFailureIntegrationTest, NodeMaskerPrivacyTest, PauseResumeIntegrationTest, ReviewedSourceIntegrationTest, VoiceLifecycleTest, LiveAccessibilityIntegrationTest, ChromeGuidanceIntegrationTest, LiveAiIntegrationTest, PermissionLossIntegrationTest, ResearchRecoveryUiTest, ResearchProtocolTest. Args: webview_cycles=120, additionalTestOutputDir=/data/user/0/com.saathi/files/observation-regressions. Never include ProviderSmokeTest. Extract artifacts using run-as before cleanup.

## Scope limits

No physical phone, protected cybercrime session, new paid-call authorization or production host is available. Genuine semantic/provider prompt retest remains blocked. Full HTTPS destination binding remains unavailable when browser metadata omits the scheme. Historical WebView and earlier synthetic-cloud intermittence causes remain unresolved. Broader hours-long mixed-fault/resource and complete semantic privacy coverage remain open. Test success is not a guarantee of no future bugs.

## Transition polling regression

The first combined run exposed `Missing observation branch` in the test helper's direct NodeMasker call while WebView was mutating. The production service already treats this as unavailable; the helper had assumed old prefix-accepting behavior. Five bounded polling helpers now retry only that explicit missing-branch category as an empty/unavailable observation. Other failures still propagate, and timeout/target assertions are unchanged. This observed harness incompatibility does not identify the older post-tap failure's cause. Final rerun evidence is separate.
