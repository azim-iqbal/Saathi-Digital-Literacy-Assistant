# Build and backend setup

## Android build

Use JDK17, Android SDK35 and the supplied Gradle wrapper. Put only `sdk.dir=/absolute/path/to/Android/sdk` in ignored `local.properties`.

```sh
./gradlew testDebugUnitTest assembleDebug assembleRelease lintDebug
```

Developer APK: `app/build/outputs/apk/debug/app-debug.apk`. Install on a test emulator/device. Provider secrets are never read by Gradle or embedded in either APK. Release currently retains local guidance only; its gateway stub is disabled.

## Backend placeholders

Python3.10+ with the standard library; tested with3.14. From the repository root:

```sh
cp backend/.env.example backend/.env
chmod 600 backend/.env
python3 -m unittest discover -s backend/tests -v
```

The ignored `.env` is a simple `NAME=value` file, without shell interpolation. Existing environment variables override it. Fill these values locally, never in chat/source/screenshots:

| Setting | Value you supply |
| --- | --- |
| SAATHI_DEV_TOKEN | A random32–256-character ASCII bearer token; same token entered in the debug app |
| GEMINI_API_KEY / GROQ_API_KEY | Server-side account keys |
| GEMINI_MODEL / GROQ_MODEL | Current supported text/JSON model IDs from your accounts |
| SAATHI_PROVIDER_MODE | `mock` for zero external calls; `dual_ai` to explicitly enable both configured providers |

Generate a token locally with a password manager or Python's `secrets.token_urlsafe(32)`. Keep it private. Missing/placeholder/invalid credentials or model names block AI call admission. The server can still start for diagnostics when the development token and budget storage are valid; it reports the affected provider as not configured. No accounts, keys, subscriptions or billing were created by this implementation.

The Gemini adapter uses `generateContent` JSON mode; Groq uses `chat/completions` JSON-object mode. Choose models supporting those formats. Check the current [Gemini API reference](https://ai.google.dev/api), [Gemini pricing/data disclosures](https://ai.google.dev/gemini-api/docs/pricing), [Groq API reference](https://console.groq.com/docs/api-reference), [Groq rate limits](https://console.groq.com/docs/rate-limits) and [Groq data policy](https://console.groq.com/docs/your-data). Model compatibility and actual responses need a credentials-enabled smoke test; a free or unlimited service is not assumed.

## Connect the debug app

```sh
python3 -m backend.server
```

Expected startup: `Saathi gateway: http://127.0.0.1:8765; mode=mock` (or `dual_ai`). In another terminal, with the emulator attached:

```sh
adb reverse tcp:8765 tcp:8765
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Open Saathi Settings → **Backend connection**, enter only the development bearer token, and select **Use AI navigation in other apps** if the server is in `dual_ai` mode. Read the data disclosure, then enable. Without that selection, the connection is for synthetic mock practice. The setup screen blocks screenshots; the token is held only in memory and cleared by Disable, Clear local data or process death. Switching connections stops an active session.

For AI navigation, choose **Help in apps & browsers**, describe a low-risk navigation goal (for example, “Show me the help section”), choose text or spoken output, start, and open the test app/browser. Both models must agree on an eligible visible control. The person performs every action. Arbitrary completion, financial actions and protected screens remain outside this pilot.

Default/offline mode works without the gateway. For synthetic mock practice, leave the server in `mock`, enable the local-test connection and start a practice task. Private form values remain local. To end testing, disable the connection, stop the server with Ctrl-C and run `adb reverse --remove tcp:8765`.

## Contract and troubleshooting

Authenticated `POST /v1/proposals` accepts registered synthetic IDs only. `POST /v1/live-proposals` additionally accepts consented goal/labels and is available only in `dual_ai`. Both reject unknown/duplicate fields and bodies above8192bytes. `POST /v1/cancel` accepts only request_id. `GET /health` reports configured mode, not provider health or credential validity. Schemas are in `backend/gateway.py` and `backend/live.py`; examples/tests are in `backend/tests`.

A rejected decision can still return HTTP200; inspect status/reason, never assume200 means guidance. A disconnected server produces no guessed marker. Check mode, token, reverse mapping and current screen. After exhausted call caps, stop and review your usage before deliberately changing caps; deleting the budget file resets usage accounting and is not a billing control. Three provider failures open a circuit until restart. No automatic retries exist.

`backend/local-budget.sqlite3` holds durable aggregate AI call counts (default100 total,50 each). `.env`, SQLite files and keys must never be committed. Mock caps reset on restart. See [AI_ORCHESTRATION.md](AI_ORCHESTRATION.md) for limits and cancellation caveats.

This server binds **127.0.0.1 only**, uses a shared development token and local HTTP, and must not be exposed publicly. Hosted HTTPS, per-user authentication/quotas, release Android connectivity and distribution are unfinished. Filling API placeholders makes the local pilot configurable; it does not deploy a production backend.

## Voice and test evidence

Settings → **Offline voice setup** can check/request your installed speech service's language pack. Checking starts no microphone. Download is explicit, managed by that service, and may continue after leaving Saathi. Emulator TTS completion callbacks succeeded for English/Hindi/Hinglish, but en-IN recognition returned language-unavailable and no spoken transcript was verified. Use text until the engine is available. See [VOICE_CONVERSATION.md](VOICE_CONVERSATION.md) and [TEST_RESULTS.md](TEST_RESULTS.md).


## Optional incident assessment and recovery (3 October)

The existing Gemini/Groq keys/model placeholders also serve `POST /v1/incident-assessment`; no extra account is introduced. In configured debug AI mode, open Report cyber fraud, review a brief description without names, numeric/contact identifiers, links or secret values, choose Ask AI about this issue, then Share once. Without configuration or consent, no assessment call is made and Show reporting advice/checklist remain usable. This is reporting triage, not confirmation that a crime happened. Model-generated prose is never displayed.

The endpoint requires exactly request_id, session_id, screen_revision, observed_at_ms, locale, summary, concern and consent=true. Summaries are 12–1200 characters and pass conservative privacy checks; request age is at most15 seconds. Both providers must return the same allowed category/signal set. It shares navigation quotas and cancellation. Tests use synthetic data/providers; filling placeholders still requires live provider validation and does not deploy a hosted service.

Authentication/configuration, provider throttling, unavailable connection, timeout, exhausted budgets, unavailable budget storage and circuit pauses now produce distinct reviewed recovery paths. Storage failure is not proof of quota exhaustion. Do not delete a budget file to hide consumed calls. Cancellation is best effort for work already dispatched; no automatic retry spends more calls.


## Verify Gemini and Groq from the app

The user removed the keys during the 3 October session; **no live check has been performed**. Restore them privately in ignored `backend/.env`, together with supported model IDs. Do not put provider keys in `local.properties`, app code or the development-token field. Set `SAATHI_PROVIDER_MODE=dual_ai`, restart the server, and re-establish `adb reverse` after reconnecting the emulator. Existing shell environment variables take precedence over `.env`; editing a different local file is not loaded automatically.

In Settings → Backend connection, enter the server's development token and enable the AI option. **Refresh server status** confirms local transport/authentication and loaded mode without a provider call. **Check APIs → Run check** explicitly sends one synthetic request to each provider through the normal call caps. Read both cards: actual HTTPS vs test fixture, HTTP receipt, parsed response, timestamp, model and available token usage. No incident is sent. The combined result separately says whether synthetic guidance passed paired validation.

| Result | Next step |
| --- | --- |
| Server unreachable | Start the loopback server, check port8765 and reconnect adb reverse; local advice still works. |
| Unauthorized | Use that running server's development token; this is not a Gemini/Groq key. |
| Simulated mode | Change the server's mode to dual_ai and restart; mock never appears on model dashboards. |
| Not configured | Restore both keys and supported model IDs server-side, check overriding environment variables, restart. Neither model is contacted while either configuration is missing. |
| Access rejected / model endpoint / request format | Check account/model access and compatible JSON output settings. Raw provider bodies are deliberately hidden. |
| Rate limit / budget / circuit | Review account limits or durable local call caps; no silent retry/reset. Fix configuration before restarting a circuit. |
| Timeout / still running | Refresh status before deciding to retry; an already-sent request may still count. |
| Parsed response but rejected guidance | Connectivity and guidance validation differ; inspect both provider results and the reviewed combined reason. |

Status metadata lives only in server memory (64 entries/provider) and clears on restart. Attempt counts are not token billing. Unavailable usage metadata is not zero. The previous dashboard observation's cause is not established. App release transport/hosted authentication remains unfinished.


## Verified local connection and TLS repair (3 October)

The supplied keys are saved in ignored `backend/.env` with file mode0600. Both providers have returned valid responses and usage through the Android smoke-test path, but the final simultaneous pair timed out on Groq. The temporary test server/reverse mapping were stopped after testing; start the regular local server and connect the debug app as above when you want to use it. No production endpoint was deployed.

This Mac's Python had no default CA bundle. The local configuration uses the existing trusted `/etc/ssl/cert.pem` via `SSL_CERT_FILE`; never use an unverified SSL context to work around certificate errors. Other hosts should use their own maintained trusted CA bundle. Current selected models are `gemini-3.1-flash-lite` and `openai/gpt-oss-20b`; see the official [Gemini model documentation](https://ai.google.dev/gemini-api/docs/models/gemini-3.1-flash-lite) and [Groq model/deprecation guidance](https://console.groq.com/docs/deprecations). The old Gemini2.5 model call returned404 in this test even though it appeared in the catalogue.

Gemini text responses may carry [opaque thought-signature metadata](https://ai.google.dev/gemini-api/docs/generate-content/thought-signatures). The single-turn adapter safely ignores that metadata while refusing thought content/tool calls. No raw model response or signature is logged. The new ProviderSmokeTest is skipped unless explicitly enabled and must never be run repeatedly as part of an ordinary CI suite.
# Hosted pilot and release builds — 4 October update

For the new hosted server, individual access tokens, deployment templates and release HTTPS configuration, follow [DEPLOYMENT.md](DEPLOYMENT.md). A host has not been chosen. The local development instructions below still apply to debug loopback testing. Historical claims that release transport is absent are superseded: it is now shared but stays disabled without a configured HTTPS origin.
