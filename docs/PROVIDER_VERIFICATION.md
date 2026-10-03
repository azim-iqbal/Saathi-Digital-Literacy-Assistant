# Provider connection verification

Checked on 3 October 2026 using the Android debug app, an authenticated local backend and synthetic practice data. No personal incident, private screen, audio or complaint was sent.

## Result

Both providers returned valid responses, but on separate checks. The final paired request succeeded on Gemini and timed out on Groq. Saathi rejected the incomplete pair and kept local help available.

| Provider | Model | Successful response | Final paired check |
| --- | --- | --- | --- |
| Gemini | `gemini-3.1-flash-lite` | 336 input + 95 output = 431 reported tokens | Parsed successfully |
| Groq | `openai/gpt-oss-20b` | 419 input + 217 output = 636 reported tokens | Timed out |

These are per-request counts from provider responses, not account totals or a verified dashboard balance. A timed-out request may still consume usage. Missing usage metadata means unavailable, not zero.

This verifies that the app/backend path can reach each provider and parse a valid response. It does not establish reliable simultaneous agreement, incident-assessment accuracy or general task completion.

## Issues found

The first paired attempt failed certificate verification because this Mac's Python installation had no default CA bundle. The local backend was configured to use the existing `/etc/ssl/cert.pem` through `SSL_CERT_FILE`. Certificate verification stayed enabled throughout.

A Gemini 2.5 Flash-Lite request returned HTTP 404 even though the model appeared in the authenticated catalogue. Its exact provider-side cause was not established. The configuration was changed to the current stable Gemini 3.1 Flash-Lite model.

The Gemini adapter also needed to accept documented opaque signature metadata attached to a text part. It now ignores that metadata in this single-turn flow while continuing to reject thought content, tool calls, unknown fields and malformed signatures. The offline regression suite passes 55 tests.

Four paired attempts were admitted during diagnosis: the initial TLS failure, the model check, the metadata check and the check after the parser fix. There was no retry loop, and no further live call was made after the final Groq timeout.

## Evidence

- [Verification summary](screenshots/2026-10-03-live-provider-check/verification.json)
- [Final per-provider records](screenshots/2026-10-03-live-provider-check/connection-evidence.json)
- [Final Android report](screenshots/2026-10-03-live-provider-check/android-evidence/provider-report.txt)
- [Instrumentation result](screenshots/2026-10-03-live-provider-check/instrumentation.txt)
- [Backend regression log](screenshots/2026-10-03-live-provider-check/backend-regression.txt)

Earlier failures are retained in the same evidence directory. Some earlier nested Android pulls contain older reports; the linked top-level report is the current one. The instrumentation test passing means the connection check completed and returned its diagnostic report, not that both models accepted the request.

`ProviderSmokeTest` is skipped unless explicitly enabled with `allowRealProviderCheck=true`. It can consume provider quota and should not be part of routine CI. The temporary server, device token file and reverse mapping were removed after testing. Follow [Setup](SETUP.md) to start the regular local backend.

## Next checks

Investigate Groq latency within the existing observation-freshness limit, starting with offline tests. Further live checks should be bounded and tied to a material change. Evaluate navigation and incident assessment separately; repeating the same successful connectivity probe will not establish their quality.

Production hosting and release connectivity are still separate tasks. Credentials belong only in local ignored configuration, never Android source, screenshots, logs or Git.

Provider references: [Gemini 3.1 Flash-Lite](https://ai.google.dev/gemini-api/docs/models/gemini-3.1-flash-lite), [Gemini signature metadata](https://ai.google.dev/gemini-api/docs/generate-content/thought-signatures), [Groq model updates](https://console.groq.com/docs/deprecations).
