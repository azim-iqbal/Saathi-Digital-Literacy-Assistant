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

The gateway admits at most4 provider workers,8 HTTP handlers and128 sessions. Three counted failures open either provider's circuit until server restart. AI decisions have a10-second deadline; HTTP adapters use8-second socket timeouts and response-size/deadline checks. Cancellation immediately prevents presentation; already-started provider calls can continue until transport returns and can still count toward billing. Python cannot forcibly kill a stuck worker; worker admission stays bounded. Android has2 worker slots and an11-second live read timeout, discards callbacks immediately on cancellation, and sends best-effort server cancellation after the bounded operation ends.

AI call reservations persist in SQLite across restart (100 total /50 per provider by default), contain aggregate counts only, and are not refunded on failures. These are call caps, **not monetary budgets or a free-tier guarantee**. Mock budgets remain process-local. There is no per-user production quota/account system.

## Evidence and remaining work

Python tests use synthetic REST responses; emulator AI integration uses two explicitly simulated adapters. These demonstrate actual HTTP, schema handling, local grounding, wrong-path recovery and Stop—not real reasoning quality. Real Chrome was tested against a local synthetic page. See [TEST_RESULTS.md](TEST_RESULTS.md).

Before distribution: provision a backend host, TLS and user authentication; add a release connection and consent flow; review current provider models/terms and real response compatibility; test task/language accuracy, speech, privacy and OEM behavior. Natural streaming conversation/barge-in remain unimplemented. See [SETUP.md](SETUP.md) for the runnable local pilot and placeholders.
