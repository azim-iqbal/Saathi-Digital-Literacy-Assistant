# Mentor demo evidence — 10 October 2026

- `form-before.txt`: two reproduced policy failures before this run's fixes.
- `build.txt`: first targeted build/unit pass.
- `stable-build.txt`: final debug/test build and 23 targeted unit checks pass.
- `emulator.txt`: eight demo-route checks pass before the probe refinement.
- `form-rendered.txt`: two follow-up captures pass; initial capture timing preceded overlay drawing.
- `stable-emulator.txt`: final six form, pause/resume and privacy checks pass, including stable presentation sampling during fallback.
- `ui-comparison.json`: five home/assistant/paused states exactly match prior baseline. Final probe refinement changes only core observation behavior; no UI layout changes.
- `device/mentor-stable`: final form/private/paused evidence; earlier directories preserved.
- `artifacts.json`: final debug app/test APK hashes and narrow exact-key scan. No universal secret-scan claim.
- `launcher-home.txt`: corrected exported-entry launcher check. Other four fixture shortcuts also executed successfully.

Earlier failed fixture and broader tests remain under the preceding reactive-forms directory. No live providers or deployment were used here. Current backend/AI/external gates are recorded in NEXT_CONTINUATION.md. UI and API behavior must not be advertised as universally compatible or bug-free.
