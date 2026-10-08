## 8 October hardening

The browser reader only examines supported browser chrome, excludes WebView subtrees and rejects incomplete/ambiguous traversal or edited address fields. Public DNS hostname syntax is required; local/numeric/special-use addresses are refused. No omitted HTTPS scheme is inferred. A visible address match is not proof of certificate health, source applicability or completion. Browser security warnings invoke a no-target handover.

Research retains its original goal/current dependency during source reading and explicit pause/resume, rechecks expiry even on quiet pages, and returns through the bubble to applicability review. New research or abandoning the review discards its own retained plan; a temporarily blocked resume preserves the task for review. Local evidence rejects clock rollback before retrieval. Source mode remains local and never sends browser address values to providers.

> In-progress continuation: Research now offers an explicit “Read source in browser” consent path and a local source-reading companion. It preserves the current dependency/original goal and compares only supported browser-chrome full HTTPS addresses, without targets, cloud calls or completion inference. UI/service behavior now has controlled emulator evidence; this does not establish actionable live task navigation or positive real HTTPS site verification. See NEXT_CONTINUATION.md for required tests and hardening.

# Research and proposed plans

Implementation added 7 October 2026. This is a bounded research and review path, not production acceptance of arbitrary workflows or model accuracy.

## Data and authority boundary

`POST /v1/research` requires the existing authenticated transport and separate consent. Its exact request fields are `request_id`, `goal`, `locale`, `jurisdiction`, `claim_type`, `consent`. Supported claim types are outage, pricing, deadline, eligibility, requirements, procedure and background. Goals/regions are bounded and pass the existing secret filter. This filter does not prove free text contains no personal information. Android sends only the explicitly entered task/region; no screen tree or private form is attached.

Sources carry original URL/title, operator authority review, source type, jurisdiction, retrieval time, publisher-supplied date (if present), excerpt and content digest. Authority comes from exact reviewed origin/path scopes. It never comes from words such as government/official in a domain, search rank, a model URL, or a web page's instructions. Search results outside reviewed scopes are unverified. Cross-host links do not inherit authority. Official/primary evidence is required for proposed requirements and eligibility criteria; community excerpts remain anecdotal.

Retrieval is credential-free HTTPS without cookies or ambient proxy settings. It refuses redirects, URL credentials/query strings/fragments, IDN destinations, private/mixed, multicast, reserved or unspecified DNS answers, compressed/binary/oversized documents and unsupported status codes. DNS is time-bounded through two admission-limited resolver workers; connections pin the validated public IP and verify TLS against the original host. In-flight OS resolution may finish after cancellation, but cannot spawn unbounded work. Text is data, never executed HTML or a tool instruction.

Each principal has one active research request, ten-second admission spacing, at most four fetched pages per request, a six-second shared deadline and a process cap of 32 retrieval/search reservations. Hosted/local configured research additionally reserves against existing durable per-user/global provider budgets. Search uses one reservation; each page uses another. Cancellation has bounded tombstones, discards late bundles and prevents follow-on planning. Evidence bundles stay only in memory, at most eight per principal for five minutes. No query/evidence database, analytics or default request logging was added.

Freshness is claim-specific: outage five minutes; pricing/deadline one hour; eligibility/requirements/procedure one day; background seven days. Old community outage posts remain historical even when freshly fetched; future/invalid publisher dates are rejected for planning. A recent fetch does **not** prove the underlying policy is current. Unknown or publisher-supplied dates remain labelled as such. Android checklists expire conservatively five minutes after the oldest retrieval.

## Configure without putting credentials in the app

Set `SAATHI_RESEARCH_SOURCES` to a server-owned JSON array of reviewed sources. Each object has exactly:

```json
{
  "url": "https://reviewed-service.example/approved-path/",
  "title": "Reviewed source title",
  "source_type": "official",
  "jurisdiction": "Applicable region",
  "verified_by": "Operator review record, date and basis"
}
```

The example is intentionally not a real authority. Replace it after an actual source review; do not deploy it as factual configuration. At most 32 scopes are accepted. A URL ending in `/` includes descendants under that path; an exact document URL does not grant the entire origin authority. Valid classifications are official, primary, secondary, community and unverified. Keep the file writable only by the deployment operator. A government-looking host name or a public search result is insufficient review.

Optional `SAATHI_SEARCH_ENDPOINT` points to an operator-selected HTTPS SearXNG `/search` endpoint with JSON enabled. The adapter sends a bounded form POST; there is no default public search instance and no search key in Android. Without this setting, retrieval crawls only configured seeds and their in-scope links. Without both settings, the endpoint returns `research_not_configured`, making no model call. Search failure falls back to reviewed seeds where available. JSON API behavior follows [SearXNG's Search API documentation](https://docs.searxng.org/dev/search_api.html). Actual instance availability, privacy policy and search coverage require operator review and hosted validation.

## Planning and eligibility

`POST /v1/task-plan` accepts only `request_id`, a same-principal `research_id`, and a new explicit consent. It does not accept client-invented evidence or URLs. The existing two independent adapters receive inert evidence excerpts and propose a bounded graph. Existing admission, durable budget, deadline, cancellation and circuit protections apply. Both must agree on cited graph/criteria; evidence membership, exact excerpt support, jurisdiction, freshness, unique IDs, dependency existence and acyclicity are independently validated.

Citation membership is **not** proof of semantic entailment. Results are marked `REVIEW_REQUIRED`, with incomplete-coverage and semantic-verification limitations. The UI lets the person review applicability, then confirm one dependency-ready step at a time while preserving the original goal. It never performs an action or lets a model declare completion. Leaving the screen invalidates applicability review; process death loses this memory-only checklist. It is not yet automatically linked to the cross-app navigation session.

Eligibility accepts only relevant local yes/no/unknown self-attestations, never secret values. Missing facts remain insufficient information. Matching all proposed criteria yields at most “possibly eligible”; official verification and completeness remain unresolved. A failed self-attested criterion is explained as a condition not met, not a definitive legal determination. A completed checklist is explicitly user-confirmed, not a verified payment, application or official outcome.

## Additional safeguards and diagnostics

Changing-claim intake categories route requirements/eligibility/deadline/pricing/outage questions to consented research. These are general claim categories, not a service-to-prerequisite lookup. The UI retains the typed task in a one-use memory handoff; no task text is added to Intent extras. Unrecognized natural-language intent is not certified.

For outage bundles, the same separately consented paired endpoint proposes a strict incident hypothesis: USER_SPECIFIC, SERVICE_WIDE, REGIONAL, ACCOUNT_SPECIFIC or UNKNOWN with one to four exact evidence quotations. Non-UNKNOWN scope requires current official/primary evidence and jurisdiction validation; community evidence alone supports only UNKNOWN. Results remain REVIEW_REQUIRED, never a confirmed account diagnosis or payment retry instruction. Public provider status JSON can support only a possible service-wide explanation. Conflicting official status stays unknown. Community reports are separately attributed, historical/undated reports labelled, and no payment retry inferred. High-risk cross-app contexts without proven destination provenance get no target or cloud request; a positive browser-origin binding is still unfinished.

Provider diagnostics now distinguish bounded validation categories and HTTP status/receipt, including a received malformed body. They do not retain raw model output, keys or private prompts. The existing diagnosis cannot retroactively explain earlier live failures. Existing socket/screen deadlines remain unchanged.

Research intake/checklist/report labels and the common-term glossary support English, Hindi and Hinglish. Source quotes and URLs stay unchanged; translated-source accuracy is not claimed. Checklist expiry refreshes only while the activity is started. The ordinary guidance session's unused unbounded text history was removed. Research uses FLAG_SECURE; synthetic screenshot tests temporarily clear and restore it only in the test. Input-method windows and installed/default keyboard packages are excluded before accessibility tree copying. Public error cues offer a blank, consented research description rather than sending screen contents automatically.

## Evidence and remaining gates

See `test-evidence/2026-10-07-research/`. The controlled tests cover live HTTP Android-to-fake-gateway retrieval and a three-level plan; they do not measure real model extraction quality. No live model calls were made.

Release gates still open: reviewed real source/search deployment; live TLS retrieval acceptance; broad natural-language extraction/coverage and contradiction reasoning; high-risk destination verification integrated into every cross-app guidance path; incident-to-live-plan revalidation; persistent context revalidation between a plan and live screen guidance; full translated source explanations and TalkBack acceptance (Hindi 200% font has controlled evidence); comprehensive semantic privacy assessment; broader changing-screen cloud stress beyond the saved 660-event fixture; hours-long resource/device/voice testing; genuine provider failure retest after controlled architecture gates; production HTTPS/account configuration. The 142 workflow catalog must not be marked passed by these component tests.

See [the current remediation matrix](REMEDIATION_CAPABILITY_MATRIX.md) for scoped evidence and the explicit separation between remaining repository work and external/manual acceptance.
