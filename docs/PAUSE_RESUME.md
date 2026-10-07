# Pause and resume — 7 October 2026

## Fixed behavior

Previously, `SaathiSession.pause()` used terminal cleanup and erased the goal. The practice Resume control reconstructed a task from current screen preferences; the cross-app assistant had no retained task to resume. Private-screen handovers were also presented as ordinary GUIDING because their target was null, and CAPTCHA had no explicit local pause boundary.

Pause now keeps only the original request, live/practice mode, language, spoken-prompt preference and speech rate **in process memory**. It cancels pending requests, invalidates observation/session identities, clears copied nodes and history, removes overlays, and stops speech/listening. Repeated Pause does not erase this descriptor. It is never written to preferences, a database or a restart intent.

Explicit Resume checks that the device is unlocked, overlay access is granted and the accessibility service is actually connected. It starts a new session identity and explicitly requests the current tree, even without another accessibility event. It cannot reuse old bounds/replies. A missing permission leaves the descriptor paused for retry. Stop, task replacement and process death discard the descriptor. A startup failure remains an error, not a claim of resumed guidance.

An immediate Start → Pause UI test also reproduced Android's `ForegroundServiceDidNotStartInTimeException`. The guidance service now acknowledges queued foreground starts before processing an inactive session, and stop requests wait for that acknowledgement. Merely moving promotion to `onCreate` was insufficient; the failed intermediate attempt is retained. Screen observation, pending callbacks and audio are still stopped synchronously.

The existing assistant screen uses the same glass controls for **Resume guidance** and **Discard paused task**. A separate new-request action remains available. Speech preference changes while paused are respected. Resume does not silently reactivate the microphone: hands-free conversation retains its separate visible-activity action.

## Temporary authentication/challenge interruption

A local `ScreenInterruption` classifier recognizes the existing private/password flags and explicit CAPTCHA/human-challenge labels/metadata. The session intercepts these before local form planning or gateway dispatch. Live planning and snapshot creation independently enforce the same boundary.

- Private screen: `SENSITIVE_HANDOVER`, no task target, no cloud dispatch, no listening.
- CAPTCHA: `WAITING_FOR_CAPTCHA`, no task target, no cloud dispatch, no listening. The user solves it; there is no solver or interaction automation.
- Safe replacement screen: a fresh observation is checked using the original goal and session; guidance resumes only if a current grounded option is available.
- Missing/protected/own-app screen: keep waiting without reusing the old marker.
- Manual Pause: screen events cannot resume it; the user must choose Resume.
- Stop, screen lock, permission revocation and process death remain safety stops. Restoring permission never automatically restarts microphone or guidance.

The privacy/challenge prompt is localized in English, Hindi and Hinglish. Public date/price/destination behavior is preserved. Private-field flags do not establish whether the secret is payment-specific, so no unverified payment-auth classification is invented.

## Verification and limits

Evidence directory: `docs/test-evidence/2026-10-07-resumption`.

- **Final regression: all 10 focused emulator tests pass, with no production guidance/audio services left afterward.** A separate fixture WebView service remained until emulator shutdown. See `final-regression.txt`, `services-after-tests.txt` and `verification.json`.
- 95 Android unit tests pass, including classifier privacy preservation, challenge detection, ordinary labels and cloud-snapshot exclusion.
- Debug/test APK builds and release Kotlin compilation pass.
- The pre-startup-fix nine-test emulator regression run passed: new resumption scenario plus existing real-service native/WebView navigation, permission revocation, voice lifecycle, three practice recoveries and two assistant-screen tests.
- The focused paused-screen UI check reproduced the startup crash (`paused-ui.txt` and `paused-ui-fixed.txt`), then passed with startup acknowledgement (`paused-ui-acknowledged.txt`). The final ten-test regression is recorded in `final-regression.txt`.
- New integration scenario performs three private-field and three CAPTCHA round trips; verifies unchanged original goal and fresh observations; manually pauses; changes the external screen while paused; denies overlay permission and verifies failed Resume preserves the task; resumes into a still-visible challenge; checks stale identities; performs three additional rapid pause/resume cycles; confirms Stop discards the task.
- The first two attempts failed before initial guidance because the separate test APK lacked `kotlin.collections.CollectionsKt`. The fixture now uses array iteration; changing launch flags alone did not fix the failure. This was a newly added test-fixture defect, not evidence of a production resume failure. Failed runner logs are retained.

No live API calls or real authentication/payment/CAPTCHA actions were performed. The fixture's Return button represents a user's completed private action; it does not certify any real website. Actual microphone/TTS resumption quality, browser-specific challenge detection, OEM restrictions, a genuinely unavailable accessibility tree with no subsequent events and physical-phone background behavior remain unverified. No guarantee of perfect operation across all apps/devices is made.

## Remaining broader implementation brief

The new brief is retained in `docs/specs/IMPLEMENTATION_REMEDIATION_REQUEST.txt`. This change addresses its pause/resume priority. Evidence-backed research/planning/eligibility, provider diagnostics, browser readiness, cloud-event stress and the other acceptance gates remain open; this phase does not mark that broader architecture complete.
