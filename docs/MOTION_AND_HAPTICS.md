# Navigation motion and glass — 29 September 2026

Latest direction: [supplied navigation brief](specs/Navigation-interaction-brief-2026-09-29.txt). Screenshots inspire interaction and shape; Saathi destinations, palette and existing architecture take precedence. No claim of copying motion timing from still images.

## Implemented

- Floating Home / Practice / Settings capsule. One hoisted screen state remains; no duplicate Navigation Compose system. Intake, Setup and Session select Practice; Privacy selects Settings. Back behavior is retained. Tapping an already selected main tab does not restart its screen.
- One cancellable `Animatable` moves the selected pill. Spring damping .86, stiffness650; velocity produces at most12% horizontal stretch and3.5% vertical compression. Reads occur during drawing. No idle decorative animation. Rapid taps retarget from the current position instead of queueing route callbacks.
- Practice has Electricity bill / Water bill / TV recharge tabs with original outline icons. Minimum tab width132dp and height112dp; the strip scrolls and grows for large type. A Foundation HorizontalPager provides direct swipes. The selected contour tracks currentPage + currentPageOffsetFraction, not only settledPage. Tab taps use a .92 damping /550 stiffness spring. Selection survives leaving and returning to Practice.
- Existing Reduce motion and Android animator-duration-scale zero bypass programmatic motion; Compose respects nonzero system scale. Direct user drags remain direct manipulation. Native pager release behavior uses its default fling; this has not been separately verified under disabled system animations.
- Existing optional navigation haptics follow phone settings. Category taps do not add a separate vibration pattern.
- Thin rims, tonal translucent shell, subtle vertical highlights and shadow. On supported API31+ hardware, only Saathi's own recorded Compose layer is sampled through a14dp BlurEffect; icons and labels render afterward and remain sharp. No third-party app capture, screenshots, bitmap export or network is used for this effect.
- Opaque fallback on API26–30, software/nonaccelerated views, low-RAM devices, power saving, high-contrast text, or the new persisted Reduce transparency preference. System changes are observed; listeners are removed on disposal. Runtime adaptive frame-budget detection is not implemented.
- Edge-to-edge root honors safe drawing/keyboard insets. Dock width caps at600dp, with16dp side margins and12dp bottom spacing. Text wraps rather than ellipsizes. Touch targets exceed48dp, tabs expose selected semantics, and decorative icons do not duplicate labels.

## Deliberate layout tradeoff

The first overlay layout let scrolled controls land under the dock and intercepted their taps. The corrected scroll viewport reserves the dock's measured height plus24dp. The dock still floats with surrounding space, but interactive content cannot scroll beneath it. As a result, the backdrop in these screens is usually the plain page color and blur is visually subtle. This is not a refraction shader or a claim to reproduce proprietary liquid-glass rendering. Future decorative backdrops must not reintroduce occlusion or capture outside the app.

## Evidence and limits

See [test results](TEST_RESULTS.md) and [navigation captures](screenshots/2026-09-29-navigation). Position tests verify actual intermediate pill motion, retargeting and an unfinished swipe. Narrow320dp Hindi200% tests verify the category strip and action remain reachable. A60-second test-run recording is retained; it includes test transitions, is not an edited product demo, and has not received a frame-by-frame visual review.

Measured debug emulator TOTAL_DURATION is poor, not60Hz-ready evidence: with recording165 frames, p50=138.52ms, p95=253.50ms; without recording226 frames, p50=157.92ms, p95=299.70ms. Every sample exceeded16.67ms. Tests ran on a software-rendered API37 arm64 emulator. The second sample rules out attributing the issue solely to recording. These broad window-frame timings include screen composition and test work; they do not isolate the indicator. Profile a physical midrange device and compare blur on/off before asserting smooth performance or tuning the fallback further.

Primary references: [Compose graphics layers](https://developer.android.com/develop/ui/compose/graphics/draw/modifiers), [Compose pager state](https://developer.android.com/develop/ui/compose/layouts/pager), [value-based spring animation](https://developer.android.com/develop/ui/compose/animation/value-based).
