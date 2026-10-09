# UI preserved — 9 October 2026

Eight UI tests pass (`ui-regression.txt`). Compared 14 captured PNGs with the previous completed phase's `../2026-10-09-private-handoff/after/` images. Twelve pairs are pixel-identical after RGB decoding. This includes onboarding, language, narrow onboarding, home light/dark/Hindi large text, assistant light/dark/paused, intake, disabled controls and the confirmation dialog.

Two pairs differ only within the Home selection and Slow speech-speed controls. Cropped baseline/current comparisons were visually inspected (`ui-comparison-crops.png`): transient pressed-state shading, same bounds, fonts, labels, palette definitions and component structure. This is not a claim that all 14 images are pixel-identical.

The new production changes are request lifecycle, gateway resource cleanup and private-practice handoff precedence. `lifecycle-only.patch` shows the request Activity changes; no setContent/rendering changes. All shared UI and resource files in `ui-source-baseline.json` remain unchanged. GatewaySetupActivity sits outside that baseline's top-level Activity glob; its diff is included in the lifecycle patch. Earlier uncommitted privacy/overlay work is preserved. Other device sizes and physical TalkBack remain manual gates.
