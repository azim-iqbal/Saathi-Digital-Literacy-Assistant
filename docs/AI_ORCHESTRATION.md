## 5 October — primary-navigation update, integration not yet accepted

Live requests now use local-first Android matching and primary/conditional-secondary server routing. Incident assessment and explicit API checks remain paired. SAATHI_PRIMARY_PROVIDER selects gemini or groq; the other is fallback. decision_policy=primary/fallback accompanies single-provider provenance. Legacy paired responses remain supported. No numeric confidence is invented; uncertainty or HANDOVER can trigger fallback. Cancellation, freshness and budget limits remain mandatory. See NEXT_CONTINUATION.md for the one failing emulator integration and exact evidence; do not treat historical always-paired descriptions below as current live routing.

## Hosted pilot transport — 4 October 2026

See DEPLOYMENT.md for the prepared WSGI/HTTPS proxy package. Hosted requests use expiring hashed per-user bearer tokens, isolated Gateway/provider instances and atomically reserved per-user/global budgets. All user gateways share four provider-worker slots. Status/cancellation cannot inspect another user's state. The shared development token is not accepted. One Gunicorn worker is required; 32 user contexts are retained until restart. Auth is rechecked before returning results after long-running work. Already dispatched calls may still spend quota after revocation.

Android main/release now uses the shared transport and existing themed setup. Release networking is disabled until a public HTTPS origin is supplied at build time and the user explicitly enables AI with a personal token. No endpoint or secrets are embedded by default. This supersedes the historical disabled-release-stub statements below; actual hosting, TLS and remote-release verification are still outstanding.

## Deadline propagation — 4 October 2026

Each admitted request carries one monotonic deadline through its cancellation signal: the earlier of the gateway decision deadline and the observation's remaining 15-second lifetime. Budget reservation and worker dispatch consume that same allowance. Both REST transports receive `min(8 seconds, remaining time)` rather than a fresh eight-second allowance. The injected transport contract is `(url, headers, payload, timeout)`; fixtures exercise the same contract without external calls.

The gateway stops waiting at that deadline and checks it again immediately before acceptance. A screen-lifetime expiry returns `stale`; it does not alone trip provider-health counters. Adapters reject late replies and transport body/EOF reads, including after parsing. When a transport does return a parsed envelope after cancellation/expiry, available usage stays in diagnostics but the adapter does not mark the request successful. No retry, single-provider fallback or extension of the freshness window was added.

**Verified:** 63 offline backend tests, including eight new deadline regressions; see TEST_RESULTS.md. **Not verified:** improved real-provider latency or accuracy. Socket timeouts bound individual blocking operations; DNS/TLS/body work cannot be forcibly interrupted by a Python Event. Cancellation prevents accepted guidance promptly, but does not guarantee immediate transport termination or zero provider usage.

## Live connection status — 3 October 2026

Gemini and Groq have each returned a valid response through the Android debug/backend path. Their successful checks were separate; the final paired request timed out on Groq and was rejected. [Provider verification](PROVIDER_VERIFICATION.md) records token counts, the local CA repair and the Gemini metadata-parsing fix. These checks establish connectivity, not reliable agreement or model accuracy.

## 3 October — connection evidence, not inferred dashboard activity

- `POST /v1/connection-status` requires the development bearer token and exactly `{}`. It makes no provider call and returns current server mode, configured providers, per-process admitted-attempt counts and recent metadata. `/health` remains configuration-only.
- `POST /v1/provider-check` requires only `request_id` and literal `consent: true`. The server constructs a synthetic practice snapshot; no user incident or screen is accepted. It calls the same Gateway admission/SQLite budget/worker/cancellation/strict-schema path once per provider, with no retry. Mock mode returns `mock_mode` and zero external requests. Missing key/model/placeholder blocks both requests before call admission but leaves the server reachable for diagnostics.
- REST adapters retain at most 64 metadata records per provider in process memory: request ID, outcome, real-vs-injected transport flag, HTTP-receipt flag, timestamp, elapsed milliseconds and optional token counts. They never retain prompts, incident summaries, credentials or response prose. Normal navigation/assessment calls also update this evidence. Missing token metadata is `null`, displayed as unavailable, not zero. Counts are attempts, not billing totals.
- Both outcomes remain visible when one provider fails; timeout can show a pending provider alongside a completed one. Cancelling suppresses Android callbacks; status can be refreshed without creating another model call. Work already dispatched may still count at the provider. Both failed providers now update circuit-health counters rather than only the first failed future.
- Separate safe outcomes cover auth/permission rejection, missing model endpoint, unsupported request/format, throttling, network/unavailable, timeout, invalid response and cancellation. HTTP receipt, parsed schema and paired guidance acceptance are distinct. A successful API check is not proof of AI accuracy or complaint suitability.
- Android Backend connection uses existing glass panels for independent provider cards. Enabling only saves an in-memory token/opt-in. Refresh status checks transport/auth/configuration. Check APIs presents an explicit synthetic-request/quota disclosure before calling either provider. Leaving/cancelling/reconfiguring clears pending presentation; no provider keys reach Android.
- Token fields follow the official [Gemini usageMetadata contract](https://ai.google.dev/api/generate-content#UsageMetadata) and [Groq chat-completion usage contract](https://console.groq.com/docs/api-reference#chat-create), checked 3 October. Dashboard delays were not established as the cause of the user's original observation.

**Verified:** real Android-to-loopback HTTP, both independent injected REST envelopes, strict metadata decoding and failures. **Not verified:** live Gemini/Groq access, actual provider-reported usage, genuine model navigation or triage accuracy. Keys were removed by the user. Release gateway and hosted HTTPS/per-user authentication remain unfinished.

> Latest clarification (3 October): the user removed the API keys. Connection diagnostics below are implemented and tested with fixtures; live Gemini/Groq verification still needs keys restored privately.

## 3 October 2026 — tested reliability and optional incident triage

- Durable SQLite reservation errors/corruption return `budget_unavailable`, not `quota_exhausted`; lock acquisition is bounded to 250ms. Actual exhausted caps stay distinct. A reservation that becomes stale/late is conservatively not refunded, but provider calls do not start.
- Submission failure releases every unsubmitted worker slot and removes active state. Submitted workers release their own slots. Cancelled results do not increment provider-health failures. Auth, throttling, timeout and unavailability are sanitized reason codes; bodies/keys are never returned.
- A bounded 256-entry, 60-second cancellation tombstone map handles cancel-before-admission races. HTTP body reading has a total three-second body deadline. Invalid/duplicate content types, cancellation IDs, malformed provider envelopes and invalid Unicode are rejected.
- Android sends best-effort cancellation promptly on a separate two-slot lane and suppresses late callbacks. Transport reads remain bounded; cancellation cannot recall data already sent to a provider. Reviewed messages use the existing instruction surface. There is no automatic paid retry or downgrade to one model.
- `POST /v1/incident-assessment` is authenticated and dual_ai-only. It shares admission limits, durable reservations, worker capacity, cancellation and freshness with navigation. Only an explicitly consented, reviewed summary/category/locale and request metadata are accepted. Common numeric/contact/link/credential patterns are refused before calls. This heuristic cannot detect every identifying detail; the dialog asks the user to remove names/identifiers.
- Two independent RestProvider calls use a dedicated assessment prompt and strict code-only schema. Both must agree on possible-financial/possible-other/unclear and the supported signal set. Neither confirmed crime nor no-fraud clearance is a supported output. Model prose/URLs cannot become user instructions. MONEY concern cannot be downgraded. UI renders reviewed cautious wording and never delays urgent helpline/bank advice.

The path is implemented and verified with fake provider transports/emulator fixtures. **Real model accuracy and provider compatibility remain unverified without accounts/model IDs.** This is still a loopback debug pilot, not production hosting, user authentication, TLS or release connectivity.

# Guidance backend — 1 October 2026

## Implemented modes

| Mode | Behavior | External data |
| --- | --- | --- |
| Default / release | Local practice or exact visible-option finding | No Saathi model requests |
| Debug local test | Android → authenticated loopback gateway → two deterministic mocks | Registered synthetic IDs only; no external model calls |
| Debug AI navigation | Android → own loopback gateway → independent Gemini and Groq REST adapters | Explicitly consented goal, app identity, locale, up to32 eligible control labels, last3 suggested labels |

No accounts, real model keys or model IDs are configured. `backend/.env.example` contains placeholders. The adapters and Android live transport are implemented; live model accuracy, latency, availability and data terms are not validated. Release APKs deliberately have a disabled gateway stub; the tested connection is a developer/pilot feature. Public deployment is separate unfinished work.

## One step at a time

An explicit AI-navigation choice in Settings → Backend connection enables cloud requests for this process only. A new request/screen carries a fresh session, revision, package, window and observation timestamp. Android excludes editable controls (including browser address fields), private/password screens, protected packages, ambiguous repeated labels and consequential controls. Filtering is imperfect: public labels or a typed goal may still contain personal information. Do not use private accounts during the pilot.

The two providers receive the same immutable data independently. They do not read each other's reply. Their JSON must contain exactly the supported fields. Both must agree on action and target. The gateway checks schema, uncertainty, evidence, target membership, current revision and maximum15-second observation age. Android strictly decodes the reply and checks its own observation again before resolving the target to local bounds. Coordinates never come from a model. Android uses reviewed localized instruction templates, not model prose.

After the person changes screens, Saathi requests a new decision. A wrong path can propose an observed Back control. The last3 entries are **suggestions**, not proof of user actions. There is no arbitrary-goal completion detector: live AI never declares success. The user controls all taps; no typing, clicking, submission or permission acceptance is automated. Screens with no supported target hand over without a marker.

## Failure and resource limits

Timeout, disagreement, stale/replayed observations, invalid JSON, invented targets, private content, unavailable providers, exhausted quotas and open circuits all withhold guidance. No single-provider fallback or automatic request retry exists. New events may trigger a new observation; highly dynamic interfaces can therefore consume the bounded call allowance quickly.

The gateway admits at most4 provider workers,8 HTTP handlers and128 sessions. Inactive session revision records expire after five minutes; active records are retained and aggregate quotas are unchanged. Freshness is checked before provider admission as well as before reply. Three counted failures open either provider's circuit until server restart. AI decisions have a10-second deadline; HTTP adapters use8-second socket timeouts and response-size/deadline checks. Cancellation immediately prevents presentation; already-started provider calls can continue until transport returns and can still count toward billing. Python cannot forcibly kill a stuck worker; worker admission stays bounded. Android has2 worker slots and an11-second live read timeout, discards callbacks immediately on cancellation, and sends best-effort server cancellation after the bounded operation ends.

AI call reservations persist in SQLite across restart (100 total /50 per provider by default), contain aggregate counts only, and are not refunded on failures. These are call caps, **not monetary budgets or a free-tier guarantee**. Mock budgets remain process-local. There is no per-user production quota/account system.

## Evidence and remaining work

Python tests use synthetic REST responses; emulator AI integration uses two explicitly simulated adapters. These demonstrate actual HTTP, schema handling, local grounding, wrong-path recovery and Stop—not real reasoning quality. Real Chrome was tested against a local synthetic page. See [TEST_RESULTS.md](TEST_RESULTS.md).

Before distribution: provision a backend host, TLS and user authentication; add a release connection and consent flow; review current provider models/terms and real response compatibility; test task/language accuracy, speech, privacy and OEM behavior. Natural streaming conversation/barge-in remain unimplemented. See [SETUP.md](SETUP.md) for the runnable local pilot and placeholders.

### 2 October validation follow-up
Android and Python now apply the field-size bounds to the original string, not its trimmed length, while still rejecting blank content. Padded goals, control labels, history and provider explanations cannot bypass limits. Regression tests cover boundaries and whitespace-only input. No provider credentials or live model calls were used.
