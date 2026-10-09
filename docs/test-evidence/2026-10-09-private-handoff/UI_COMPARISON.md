# UI comparison — 9 October continuation

Baseline and after: 14 PNG pairs; eight UI test methods pass both runs. 11 pairs are byte-independent pixel-identical after RGB decoding (see pixel-comparison.json), including onboarding, language selection, narrow onboarding, home light/dark, Hindi large text, assistant light/dark, intake, disabled controls and confirmation dialog.

Three differences were visually inspected side by side:
- assistant-paused: the required accurate private-marker disclosure replaces the former “private fields get no target” statement. Its line wrapping changes the scroll offset when the test brings Resume into view. No spacing, font or component code changed.
- glass-home-opaque: difference confined to the selected Home control's transient pressed-state tint.
- voice-settings: difference confined to the selected Slow chip's transient pressed-state tint.

These are not 14 identical screenshots. No branding/theme/layout/navigation implementation was modified. The latest request's referenced approval screenshot was absent, so no design rollback was guessed. Floating/active-private overlay pixels are not certified unchanged: adding the expressly requested structure-only private marker and updating its instruction are functional changes, using the existing overlay rendering. Other screen sizes/TalkBack/device appearances remain manual coverage.
