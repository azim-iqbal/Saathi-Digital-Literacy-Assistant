# Commerce/privacy/form evidence — 10 October 2026

Final code: debug/test builds, release Kotlin compilation, lint PASS (0 errors, 61 warnings); **163 Android unit tests PASS** (`regression-build.txt`; Gradle XML results). Backend unchanged; preceding170 offline backend and9 evaluation results were not rerun or counted as new.

Final emulator group: `emulator-regression.txt` =19 tests,18 PASS /1 Chrome readiness FAIL. The Chrome test requires `python3 -m http.server 8766 --bind 127.0.0.1 --directory backend/fixtures` and `adb reverse tcp:8766 tcp:8766`. Those prerequisites were missing. Saved readiness tree contains Chrome's main-frame-error. With the required server/reverse mapping, `chrome-corrected.txt` =1 PASS. Thus19 distinct selected regressions passed across final runs, not one clean19-test run. Server and mapping stopped afterward.

The final group covers commerce3, reactive forms2, pause/resume1, privacy4, travel privacy1, Chrome1, controlled portal/rotation1, incomplete observation1, permission loss1, home UI1 and assistant UI3. No ProviderSmokeTest or genuine model call. This is a selected suite, not every emulator test or live HTTPS certification.

## Reproductions and corrections

- `before.txt`: public price plus numeric resource-ID false positive reproduced before fix.
- `commerce-before.txt`: Order milk could not start at observed Search before commerce guidance.
- `credentials-before.txt`: missing credential cue corpus fails before expansion.
- `commerce-build.txt`: first new unit test hit Android Rect.hashCode stub. Bounds dedup now uses copied integer coordinates; production safety checks were not relaxed.
- `commerce-emulator.txt`:1 failure in6 tests. External test-APK fixture could not load Kotlin Function0. `fixture-runtime.txt` identifies the missing class in com.saathi.test. Replaced the fixture's higher-order callback with primitive next-page parameters; unrelated to production app or historical WebView defect.
- `emulator-retest.txt`:11/11 PASS after that fixture correction.
- `mixed-emulator.txt`: earlier7/7 PASS; do not add overlapping counts to final total.

## Screenshots and artifacts

`device-final.tar` / `device/commerce-regression` hold final captures. `chrome-final.tar` has corrected browser evidence. Initial screenshots/evidence are retained separately. `commerce-contact-sheet.png` visually reviewed: correct milk Add, observed quantity/cart, private CVV-only badge, cart review. All shopping screens are explicitly synthetic. No retailer purchase, real credential entry, private-value upload or inferred completion.

`ui-comparison.json` compares five captures against the earlier mentor baseline: home-dark, assistant-dark and assistant-paused are pixel-identical. Home-light and assistant-light differ; `ui-comparison-sheet.png` was inspected side-by-side and shows the same layout, colors and controls. Assistant difference is confined to the Text-only control; home differences span rendered background. No layout/theme/brand/navigation production source changed. Do not describe all five as pixel-identical or claim universal visual QA.

`artifacts.json` records final ignored mentor/debug APK hashes and exact configured-key scan (no matches). This is not a universal secret-scan certification. No release signing, commit, push, deployment or publishing.

## Reproduce

Use JDK21 configured in the repository environment. Run `:app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:compileReleaseKotlin :app:lintDebug`; install both debug APKs on an emulator. Run the named classes from `emulator-regression.txt` with the AndroidJUnitRunner, configuring the local Chrome fixture first. Use a new app-files output directory and export it with run-as. Do not include ProviderSmokeTest: all live allowances are exhausted. Existing test permissions/settings are restored by the tests.

See CROSS_APP_COMMERCE.md for the acceptance matrix and remaining limitations. No physical phone or Zepto installation was available. The historical WebView stale-marker cause, full advanced forms, broad workflow semantics, genuine AI planning, shared production storage and external acceptance are not certified by this phase.
