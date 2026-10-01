# Saathi launch experience

## Implemented

- Same generated S identity, now vector-based. Green/mint/white SVG sources in design/brand/saathi-v2; header, adaptive launcher and startup use Android vectors derived from those paths.
- Centered mint mark on #131513 startup surface. Android12+ animated vector gently breathes between100% and103.5% scale,800ms each direction, while its system starting screen is visible. Earlier Android versions use a static system icon before the exit animation.
- Once the first app frame is ready:500ms logo opacity/scale settle, then700ms smooth fade into the actual screen. Nominal reveal duration1.2s; system launch time and animator scale can change observed timing. No network dependency, fake progress percentage or artificial loading task.
- User reduced-motion preference, system disabled animations and activity recreation skip the custom reveal. The OS-owned starting vector appears before app preferences can be read; its earliest motion is controlled by the system animation setting.
- Reveal animation/provider cleanup on backgrounding and destruction. Returning from background does not deliberately replay the reveal.
- LaunchActivity retains its installed launcher component identity but now directly inherits the app screen instead of redirecting through a second activity. Existing MainActivity remains available for internal/tests. No routing rewrite or extra splash page.
- AndroidX core-splashscreen1.2.0 supplies platform/backport behavior. See [official release notes](https://developer.android.com/jetpack/androidx/releases/core) and [migration guidance](https://developer.android.com/develop/ui/views/launch/splash-screen/migrate).

## Continuing app fix

Notification Stop actions carry a session identity separate from screen-observation identity. A queued Stop from an older session is ignored after restart; the current session's Stop remains valid across screen changes. Creating a new notification cancels the old PendingIntent. Unit regression covers lifetime identity; actual delivery races still require service instrumentation.

## Limits and follow-up

See TEST_RESULTS.md for this phase's actual results and recording. Android26–30 backport behavior, Android12's initial implementation, reduced-motion system settings, themed icons/OEM masks, slow-start devices and physical frame timing need dedicated checks. The app currently initializes local UI synchronously; no backend startup/loading workflow exists to test. Do not equate the animated splash with backend readiness.

The broader glass parity, lifecycle, backend, speech and accessibility backlog remains in EXECUTION_PLAN.md and GLASS_UI.md.
