# Backend connection recovery - 7 October 2026

## Confirmed cause of the reported phone failure

The user tested a physical Android phone **without USB forwarding** and has no deployed HTTPS backend. The development build defaults to `http://127.0.0.1:8765`: on the phone that is the phone itself, not the developer's computer. It cannot reach the computer server in this configuration. No provider request can be made through a connection that never reaches the gateway. A zero admitted-attempt count is not an API response or proof of provider failure.

The user explicitly chose **prepare the hosted connection now; deploy next phase**. Standalone phone AI remains blocked on that deployment. Local practice and exact visible-option help remain available. Do not describe this phase as making offline phones able to contact an undeployed server.

## Latest teammate changes reviewed

Fetched origin and based the local fix branch `fix/backend-response-time` on `5a37d93`. Reviewed Umair's `23d8e15` form guidance and `178bb42` spoken field guidance, plus their merge commits. These changes did not alter backend provider transport. Preserved their local form guidance and existing theme. Fixed a compile error in `FormGuide.fieldName`: it referenced `node` instead of its `field` parameter.

Also reviewed the preceding connectivity commit `1143dfd`. Its emulator fallback was unconditional on device type, permanently changed the active endpoint before successful connection, retried after possible POST writes, and could lose failed connections during cleanup. Cancellation could target the newly selected endpoint instead of the original request's server. Its `enabled()` change also sent practice to the backend when only cross-app AI was enabled.

## Implemented fixes

- Emulator address discovery runs only on recognized debug emulators, never physical phones or hosted release origins. It applies only after a failed connection attempt before a POST body is sent. No replay after writing, no provider retry loop and no sticky failed fallback destination.
- Shared bounded HTTP transport owns every connection and closes it on failure, rejection, cancellation and success. Connect/read phases share a deadline. Responses without a valid HTTP status, oversized bodies and invalid content types are rejected. Redirects remain disabled.
- Each request captures its endpoint and token generation. Endpoint replacement invalidates old callbacks, clears authorization and sends best-effort cancellation to the original destination. Debug overrides are limited to validated HTTPS origins or the existing loopback/emulator test origins.
- Restored independent modes: enabling cross-app AI does not enable networked local practice. The explicit mock practice test mode remains available.
- Recovery text distinguishes unreachable computer-local backends, TLS failures and a reached backend whose provider fails. Missing usage remains unavailable rather than being fabricated as zero; the existing diagnostics distinguish admitted attempts from billing.
- Provider circuit failures are now consecutive. Healthy responses clear the failure streak; valid uncertainty is a task outcome, not a network outage. After repeated upstream failures, a 30-second cooldown admits one recovery probe. A failed probe reopens the circuit; cancellation releases the probe. Quotas, freshness, cancellation and primary/fallback limits remain enforced. There is no automatic background model probe.

## Evidence

See [verification.json](test-evidence/2026-10-07-backend-recovery/verification.json) and accompanying logs.

- Baseline: 89 offline backend tests passed. New circuit regressions exposed two incorrect behaviors (failure counts surviving success; uncertainty opening the circuit); four additional recovery cases had no cooldown implementation to exercise.
- Final: **95 backend tests, 90 Android unit tests, debug/test builds and lint pass**. Lint has **0 errors / 61 warnings**.
- **12 emulator test executions, 11 distinct tests, all passed**: three new device-local HTTP recovery cases; four live guidance/decoder cases; four existing mock-practice/auth/cancellation cases; and one additional full live flow over `10.0.2.2` with adb reverse removed.
- Server-loss test: refusal, server start, ten successful status requests without reconfiguration, then refusal after shutdown. Endpoint replacement verifies cancellation reaches the old server and no obsolete result reaches the UI.
- Actual AccessibilityService + synthetic HTTP flow: Help, detour, observed Back, return, safe handover, Stop. Exact local matching still produces zero HTTP requests. The formerly failing primary-flow test passes in this environment; this does **not** prove the historical failure's cause.
- All provider transports used fixtures. No `.env` credentials were loaded, real provider checks repeated, public host deployed, signed release produced, push performed or UI theme changed. Test APKs are verification artifacts, not the upcoming distributable APK.

## Next phase: phone connection and APK

1. Select and deploy the reviewed hosted package with HTTPS, persistent state and per-user authorization. Follow [DEPLOYMENT.md](DEPLOYMENT.md); current single-worker state is not suitable for unmodified autoscaling/ephemeral Cloud Run storage.
2. Verify `/health`, authenticated `/v1/connection-status`, invalid-token rejection and revoked-token handling over real TLS. Status alone makes no model call.
3. Build with `-PsaathiBackendUrl=https://THE_ACTUAL_HOST` (public origin only). Release never falls back to localhost. Provider keys stay on the server; each phone receives only its own access token.
4. On the physical phone, disable USB forwarding and test Wi-Fi/mobile data, offline/reconnect, server outage/recovery and current grounded guidance. Authorize a bounded genuine model evaluation separately; fixture success does not establish real AI accuracy.
5. Complete signing/security/device acceptance before sharing the APK. Real-device speech/OEM survival, original intermittent WebView failure, live private portal workflow and broader release gates remain open.

Until deployment, physical-phone cloud testing requires the computer server plus USB debugging and `adb reverse tcp:8765 tcp:8765`. An emulator's `10.0.2.2` address cannot replace that on a phone. Transient outages can still occur; the fixes ensure bounded handling and recovery rather than promising permanent network availability.
