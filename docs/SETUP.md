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

Generate a token locally with a password manager or Python's `secrets.token_urlsafe(32)`. Keep it private. Placeholder credentials/model names refuse AI initialization. No accounts, keys, subscriptions or billing were created by this implementation.

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
