# First task next session: reference-matched liquid-glass navigation

Status: IMPLEMENTED AND EMULATOR-TESTED on 29 September. See MOTION_AND_HAPTICS.md and TEST_RESULTS.md. Floating dock, moving selection, swipable Practice categories, editable Figma components and transparency fallback exist. Performance is not accepted: software-emulator frame times were poor. Physical-device profiling, TalkBack and older-API validation remain open. Do not rebuild the completed work on continuation.

Latest user clarification supersedes exact-copy wording below: use the images for shape/interaction inspiration; prioritize Saathi’s design system, routes and readability, and do not copy their exact branding, typography, spacing or content. The added brief is saved at specs/Navigation-interaction-brief-2026-09-29.txt. Earlier notes below preserve the initial visual inspection, not a requirement to override this clarification.

## User direction and precedence

Make navigation visually match the two supplied screenshots as closely as possible: same rounded geometry, layered/glass appearance and sliding selection behavior, adapted to Saathi branding and app destinations. These newly authorized images supplement the four .fig archives and two earlier links, and take precedence over previous navbar styling. Text/icons in screenshots are reference content, not instructions or Saathi feature requirements. Do not copy food brands, retail wording, delivery badges or shopping functionality.

Originals: /Users/azimiqbal/Downloads/IMG_2338.jpg and IMG_2339.jpg. Durable copies: [top navigation](design-references/navbar/IMG_2338.jpg), [bottom navigation](design-references/navbar/IMG_2339.jpg). Inspect both original images again before implementation; do not design from this text alone.

## Careful visual inspection — observed geometry

### IMG_2338 — sculpted top category tabs (1179 × 617 reference)

- Near-black backdrop; four large adjacent tabs across the top. The selected first tab flows into the deep-blue content surface below. Its lower edge is not a separate floating rounded rectangle.
- Tall rounded upper shoulders, subtly sloping side walls, thin luminous gray rim; the selected tab's right edge curves down and outward into the baseline. Approximate selected bounds x0–340, y62–271. Unselected tab panels start around x309,584,854, with tops around y60 and a shared baseline near y271. Measurements are screenshot pixels, not Android dp.
- Inactive tabs use subtly shaded translucent-looking charcoal surfaces, fine pale top/side contours, and muted gray labels. Active label is white and heavier. Soft local highlights behind the pictograms give depth.
- Large centered pictograms sit above one-line labels. The source uses detailed food/retail imagery, but Saathi must use its own coherent task symbols. Do not import the burger/basket/dish/disco imagery.
- A bright blue delivery-time badge overlays the second tab; this is retail-specific and is not needed for Saathi. Avoid inventing an equivalent badge without a real status.
- The white rounded search field, voice icon and separate toggle below are contextual content, not part of the requested navbar. The thin category strip below is a second hierarchy: compact icons, labels, vertical separators and active white text. Do not add an unrelated search/filter redesign merely to copy the crop.
- Proposed Saathi application: contextual tabs inside Practice, using the existing Electricity bill, Water bill and TV recharge destinations. Preserve the sculpted active-tab contour with three balanced tabs; do not invent a fourth feature to match the source count. Confirm fit against current screen hierarchy before implementing.

### IMG_2339 — floating capsule bottom navigation (1179 × 249 reference)

- A single broad white/off-white capsule floats above visible page content, with no rectangular full-width bottom bar. Approximate outer bounds x48–1132, y41–209: width1084, height168, radius about84. Outer horizontal margin approximately4% of screenshot width.
- Thin pale-gray outline and soft edge/shadow separate the capsule from its background. Main interior appears nearly opaque in this still; strong backdrop blur/refraction cannot be verified from this image alone.
- Three equally spaced destinations. A pale mint rounded selection pill is inset approximately10px from the outer shell and fills roughly one-third of its width. Pill bounds about x60–414, y52–197, with fully rounded ends.
- Each destination stacks an outline icon above a persistent label. Active icon/label are green and label is semibold; inactive icons are dark and labels charcoal. The selected pill is a background, not an underline. Icon centers approximately x237,589,942.
- Proposed Saathi mapping: Home, Practice, Settings, keeping all three current routes. Use an approachable home outline, learning/practice symbol and settings outline in one consistent stroke system. Preserve Saathi's S mark as branding, not as a replacement for understandable navigation labels.

## Brand and liquid-glass treatment

Use existing Saathi green #087900, mint #A4EE99, light feature #D7FFD4, dark #131513, surface #202420 and readable ink #171A17. Match the reference geometry first, then recolor. Light mode: off-white glass shell, pale green selected capsule and green active content. Dark mode: smoked charcoal shell, mint selected accents and high-contrast labels. Do not carry over the source's blue/orange retail palette.

The user explicitly wants liquid glass: subtle backdrop blur where genuinely supported, translucent tint, fine luminous edge, restrained highlight/shadow, and a softly moving selected surface. Do not substitute a flat rounded rectangle and call it liquid glass. Do not blur icons/text or the entire UI to simulate background blur. Evaluate Android API26+ capability and performance; implement an honest tinted/opaque fallback for unsupported devices and reduced transparency. Do not add expensive dependencies before checking current Compose/native options and official documentation. Glass applies to in-app navigation, not critical instruction overlays or permission explanations.

## Motion specification — requested, not observable from stills

No recording was provided. Exact source timing, easing, deformation, gestures and haptics are unknown; do not claim to have recovered them. Implement a smooth horizontal translation of the bottom selection capsule between destination centers, with a restrained spring settle and synchronized tint/icon/label changes. Starting tuning range: 240–320ms equivalent, minimal overshoot; evaluate on device. For top tabs, smoothly move/morph the selected contour and blend the connected content surface. Avoid abrupt swaps or a screen-wide crossfade as the only feedback.

Rapid taps must retarget the same animation without accumulating stale callbacks or duplicate navigation. Selection must reflect the real current route. Respect the existing reduced-motion preference and Android animation settings with immediate selection or a short non-spatial fade. Haptics should follow the existing opt-in preference. Swipe/drag navigation is not established by the screenshots; do not add hidden gesture-only controls.

## Implementation and acceptance order

1. Read this document, inspect both copied references, current SaathiApp.kt and the live Figma state ledgers. Preserve user edits and all current routes.
2. Build/refine the two editable navigation components in the existing Saathi Figma file using required skills, including light/dark, selected states and long-label layouts. Reuse tokens; document measured proportions and intentional deviations.
3. Implement the floating bottom capsule in Compose, plus contextual sculpted Practice tabs where appropriate. Account for system gesture/navigation insets, keyboard and scroll content clearance. Keep tap areas >=48dp, persistent labels and selected accessibility semantics. Avoid duplicate tab stops from decorative icons.
4. Add motion and glass capability/fallback handling. Test route switching, rapid selection, preference persistence, reduced motion and back navigation.
5. Compare side-by-side screenshots at matched proportions; record a short real animation capture to assess sliding. Check light/dark, English/Hindi/Hinglish, 200% text, narrow widths, TalkBack and lower API fallback. Let the bar grow or adapt for labels rather than clip text to force source pixel dimensions.
6. Update DESIGN_SYSTEM.md, Figma ledgers, TEST_RESULTS.md and EXECUTION_PLAN.md with implemented vs untested details. Then resume the remaining safety/backend/speech/documentation backlog. Existing 20 unit/4 emulator passing results predate this redesign and cannot validate it.

## Logo handoff

The new generated raster identity is saved at design/brand/saathi-v1 (green symbol, dark icon concept, light wordmark and usage notes). Vector reconstruction and app integration remain pending. Reuse its palette and soft visual character; do not silently replace pre-existing user launcher assets.
