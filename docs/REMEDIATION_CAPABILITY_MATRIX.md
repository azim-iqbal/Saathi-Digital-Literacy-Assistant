## 10 October 2026 — Samsung physical-device verification and real Zepto fixes

Usage stop: latest check4% short-window remaining/39% weekly. Stop new work now; threshold was crossed between checks. No reset/model calls/commit/push/deployment. Samsung SM-M076B Android16/API36 connected via wireless ADB (rediscover transport; do not reuse70 blindly). User approved uninstalling the incompatible-signature Saathi and replacing it; saved settings/drafts were deleted as explicitly authorized. Current debug build is installed; no release or deployment.

User explicitly authorized selecting an existing saved Zepto delivery location; Home selected. Zepto26.10.1 remains installed with its own data intact. No cart additions, order, payment, complaint or external message. Raw temporary screenshot/XML containing address details was removed from host/device; repository evidence contains filtered labels/structural metadata only.

PASS:165 Android unit tests plus debug/test builds. Physical controlled suite7/7 PASS after correcting Samsung instrumentation service reconnection (initial5/7 failed before scenarios because enabled service was recorded crashed after instrumentation restart). Service startup fix is test-harness-only, not proof of production crash recovery. Includes commerce3, native/WebView no-zoom forms2, pause/resume1 and permission-loss1. Seven passes precede the final initial-observation production fix; full post-fix device regression is PENDING.

Real Zepto found and fixed three general defects: explicit public weight ranges (450–500g) were falsely sensitive; rotating Search “suggestion” labels were not recognized; initial startLive on an already-open quiet screen did not explicitly request a snapshot. Reproduced first two in failing unit tests, third with real service trace (0 snapshots/no instruction) then fixed. Final real Zepto results: snapshot185ms, externalEvents0, GUIDING,0 private fields,0 redacted nodes, no failure; several milk products correctly produce user-choice guidance. Home search target also verified, with personal address redacted and local search guidance retained. This is PARTIAL real Zepto acceptance: specific product/Add/cart/checkout still NOT RUN. Earlier observer-only PASS with blank instruction was not a guidance pass; later probes assert guidance and preserve diagnostics.

Evidence: `docs/test-evidence/2026-10-10-samsung/`. `zepto-defects-before.txt`, `start-observation-build.txt`, `rebind-tests.txt`, `zepto-search-fixed.json`, `zepto-milk-diagnostics.json`, `zepto-milk-start-fixed.json`, `controlled-device.tar`. No screenshots of real address committed. UI production layout/theme unchanged.

NEXT FIRST: build latest test source (new quiet-start CommerceGuidanceTest wrapper added AFTER last APK build, not yet run); rerun affected physical commerce/forms/pause/revocation tests. Then continue actual Zepto with a specific observed product request, verify its own Add and cart state; any reversible test cart additions must be removed, never purchase. PhysicalSpeechTest source compiled earlier but NOT RUN; no microphone permission granted or real speech tested. Other broad gates remain unchanged. No background instrumentation/server remains. Installed app is current production code; ignored mentor APK predates these Samsung fixes and must be refreshed after regressions. Preserve all preexisting changes.

## 10 October 2026 — General commerce, element privacy and no-zoom form verification

Latest brief saved at `docs/specs/CROSS_APP_COMMERCE_REQUEST.txt`. Read `docs/CROSS_APP_COMMERCE.md` and `docs/test-evidence/2026-10-10-commerce/README.md` first. Existing work preserved; UI design unchanged; no model calls, release signing, commit, push or deployment.

Implemented: numeric resource-ID privacy false-positive fix; expanded/localized credential and price corpus; explicit node privacy categories; snapshot-local hierarchy; local search/product/Add/quantity/cart-review guidance; safe named promotional dismissal on recognized dialog containers; deduplicated badges for actual private editable fields; safe local form/product guidance alongside unfocused private fields with cloud/listening still suspended. No editable value getter or private upload. No automatic transaction/typing/clicking or completion assertion.

Verification:163 Android unit PASS; debug/test builds, release Kotlin and lint PASS (0 errors/61 warnings). Final selected emulator group18/19 passed; Chrome failed because its required local fixture server/reverse mapping were absent, then passed1/1 with those prerequisites restored.19 distinct selected cases therefore have passing final-code evidence across runs. Native/WebView forms advance without zoom and include mixed private-field guidance. Synthetic commerce follows the correct repeated Add to cart review and resumes the same goal after private entry. Backend unchanged; previous170/9 offline evidence retained, not rerun. Three of five UI captures pixel-identical; two light captures visually inspected with differences documented. Debug mentor APKs refreshed and exact configured-key scan passed.

NOT COMPLETE: real Zepto acceptance blocked by no installed Zepto/connected phone (question pending). Generic fixture is not Zepto. Richer screen semantics, flexible/free-form commerce and cab/ticket flows, general form validation/dependencies/completion, historical stale-marker attribution, broader genuine AI planning and shared durable storage remain repository-level work. Prior live provider allowance exhausted. Physical speech/OEM/TalkBack/performance and authenticated portal require actual hardware/access. TinyFish documentation fetched, not integrated: no comparative improvement benchmark yet. Deployment remains deferred.

Next: obtain legitimate Zepto test access and inspect its actual accessibility hierarchy, then close unsupported general control/intent cases without retailer rules. Preserve this turn's regressions and privacy boundaries. Do not repeat the missing Chrome fixture setup mistake. No active instrumentation or backend server remains; emulator cleanup recorded below.

## 10 October 2026 — Source-scoped prerequisites, usage handoff

Latest instruction: continue remaining work and hand off near/below10% short-window remaining (supersedes8% floor for this run). Latest check12%; final check follows. No resets, emulator, model calls, deployment, signing, commit or push in this phase. Existing Android/UI changes and mentor APK preserved.

Implemented a general backend fix: prerequisite label identities now include evidence ID; identical names from unrelated sources cannot satisfy each other's ordering requirements. Duplicate same-source candidates are withheld rather than silently overwriting each other. Reproduced cross-source acceptance before fixing it. The second new test initially had an invalid quotation lacking the selecting instruction; that fixture was corrected, not the safety validator.

PASS:38 focused backend checks,9 evaluation checks,170 full offline backend regressions. Evidence `test-evidence/2026-10-10-source-relations/` preserves initial failures and final results. No genuine model retest; all earlier call allowances remain exhausted. Passing these guards does not certify model reasoning accuracy. Android code/UI unchanged this phase, so emulator/build checks were not repeated. Prior debug APK remains current for Android; backend source includes this additional fix.

Added CLOUD_DURABLE_STATE_DESIGN.md: concrete proposed invariants for shared account authority, atomic quota reservations, idempotency, cancellation, leases/fencing and source-plan isolation. Explicitly DESIGN ONLY; external-store adapter and multi-instance/runtime tests remain unfinished. No production readiness claim.

Next priorities: broaden safe general form validation/dependencies/completion without reading secrets; genuine planning quality retest only with fresh scoped authorization; remaining browser/renderer/long-session regressions; implement shared persistence only for the chosen hosting architecture and validate container runtime. Physical phone audio/OEM/performance and authenticated portal require actual hardware/access. Current privacy handoff preserves tasks and structure-only markers; masking-off/private-value upload is absent. Deployment stays deferred. All current tests/processes finished; no cleanup needed. Resume from this working tree, not historical active-session IDs below.

## 10 October 2026 — Dynamic web forms and private-task continuity

User requested production-wide compatibility and uninterrupted help around private fields. Keep the task alive, but do not equate the private badge with consent to upload secrets. No masking-off/automatic private-value upload was added. See PRIVATE_FIELD_CONTINUITY.md for the explicit boundary.

Fixed initial/retargeted requests on already-open forms not arming the finite local follow-up window. Expanded actual-service WebView tests to delayed JavaScript conditional insertion/removal, ARIA invalidity/correction and the same form task entering/exiting an OTP-like field. Private marker, preserved session, safe return, no copied secret and zero gateway calls verified. No zoom gesture/manual refresh used. This does not establish every website's compatibility or the historical intermittent cache failure's sole cause.

Evidence `test-evidence/2026-10-10-dynamic-web`: 10 targeted unit tests plus debug/test builds pass; combined native/WebView/pause suite3 PASS; extended private-web case1 PASS. UI/style unchanged. Updated local mentor debug APK; hash and exact configured-key scan saved in artifact.json. Earlier mentor artifact hash belongs to preceding build. No live model calls, deployment, signing, commit or push. Emulator stopped after evidence export. Full production gates remain open: AI planning consistency, broad semantic form validation/completion, complete regressions, physical speech/OEM, legitimate protected portal, container/shared-state/hosting acceptance. Prior provider-call authorization remains exhausted.

## 10 October 2026 — Mentor demo preparation, targeted verification

Latest user requested fewer tests and a usable mentor demonstration. Prepared `deliverables/mentor-demo/Saathi-Mentor-Demo-debug.apk` and separate synthetic fixture APK (ignored build artifacts), `tools/mentor_demo.py` launcher and updated `DEMO_SCRIPT.md`. No deployment, release signing, commit, push or live model calls. All earlier work preserved.

Additional production fixes: explicit “not required” labels remain optional in English/Hindi/Hinglish; conflicting metadata remains unknown; invalid required fields precede invalid optional fields. Bounded local form probes now retain the presentation on identical safe snapshots, avoiding fallback-induced marker clearing/speech interruption. External events still invalidate immediately; probes cannot cross revisions, windows, sessions or privacy handoffs. Actual speech quality remains unverified.

Verification: 23 targeted unit tests pass; eight mentor route checks passed before the final probe refinement; six affected form/pause/privacy emulator checks pass on the final APK. Two extra form runs captured rendered markers. No broad unchanged suite repeated. Existing backend168/evaluation9 evidence from the preceding phase applies; backend unchanged this run. Five home/assistant/paused screenshots are pixel-identical to the saved baseline; production design files unchanged. Launcher shortcuts verified (initial private MainActivity route corrected to public LaunchActivity). Exact configured provider keys absent from both final APKs; hashes recorded. Evidence: `test-evidence/2026-10-10-mentor-demo/`.

Use the documented LOCAL mentor route. The separate fixture demonstrates actual accessibility guidance over synthetic external content; it is not genuine AI or real portal acceptance. No new provider-call allowance remains. Planning quality, broader semantic form validation/dependencies/completion, historical stale WebView causality, remaining comprehensive regressions, physical voice/OEM/portal and container/Cloud Run durable storage are still open. Do not call the full project finished or production-ready. UI screenshot comparison for the five selected app states is complete. Emulator test processes stopped after evidence collection; no backend server was started.

## Final collected results — 10 October 2026

155 Android unit, 168 backend, 9 evaluation and 7 focused emulator tests PASS. Focused tests include both reactive forms, three privacy cases, retained-task pause/resume and controlled portal handoffs. Build/release Kotlin/lint pass (61 warnings). Both previously active runs finished successfully; device diagnostics exported to `test-evidence/2026-10-10-reactive-forms/device-evidence.tar`. No active instrumentation/server. Emulator stopped after collection. Full post-change emulator suite and screenshot comparison remain pending. Four-call allowance exhausted; planning quality remains FAIL. Usage9% remaining at final save; stopping before starting another phase to respect8% floor.

## 10 October 2026 — Reactive form implementation (not final acceptance)

Baseline 82b9224. No commit/push/deploy/sign. Current UI styles/layout/navigation/logo unchanged; functional form guidance text updated.

Implemented finite local form observation fallback (eight 750ms checks per relevant external event, stops when ineligible), focus/selection subscriptions, fresh-snapshot required/optional/conditional/unknown policy, invalid-field prioritization, optional skipping, clearing/rebuilding and cursor-metadata-only presence. No editable value getter, automatic input/submission, inferred validity or completion. Private-context minimization now also clears new form metadata. Required label trailing-space parsing fixed.

Evidence: `test-evidence/2026-10-10-reactive-forms/`. 155 Android unit tests PASS, debug/test builds, release Kotlin compilation and lint PASS (0 errors/61 warnings). New native/WebView service tests now pass within the active focused run, after fixing separate fixture APK Kotlin-runtime and uppercase-button driver failures. These are fixture defects, not app crashes. `focused-final.txt` is still running pause/resume and portal tests: exec59588, emulator-5554. Inspect final result before another install/instrumentation run. Export app files/reactive-final plus earlier reactive-forms-debug and reactive-forms-final diagnostics. Build final APK includes all production Android changes.

All FOUR newly authorized provider calls CONSUMED. `live-reasoning-four/results.json`: English planning FAIL/disagreement (Gemini met sequence, Groq failed independent sequence expectations); Hindi incident PASS both. Gemini plan2670ms/incident1097ms; Groq2252ms/641ms. Not accuracy certification. No further calls authorized. A deterministic omission regression then found that missing navigation annotations bypass explicit quoted-order checks; general validator/prompt fix added, no genuine retest. Backend full rerun exec92762 (`backend-final-permitted.txt`, then evaluation-final.txt) pending at save; initial sandbox run failed localhost permissions, preserved separately. No raw model bodies retained.

Next: collect those runs; run remaining complete emulator groups with existing runner and new output; finish screenshot comparison and Cloud Run durable-store documentation/runtime checks. Docker absent. Prior cache-focused 3 tests and remaining practice7/research18/connection3/visual15/browser2 runs from 9 October are complete and passing, but predate today's production changes. Historical stale WebView marker root cause is still a cache hypothesis, not established by passing reruns.

Form capability remains PARTIAL: presence is not validity; VALID/VALIDATION_PENDING have no positive evidence producer; unknown caret-zero fields stay uncertain; conditional membership is refreshed rather than arbitrary dependency inference; identities are scoped to current observation; no automatic form readiness/submission/completion claim. Generic trusted validation/dependency/completion architecture and broader dynamic/keyboard/renderer/E2E coverage remain repository work. Do not claim the 13-phase mandate complete. Preserve privacy/freshness/pause boundaries. Real phone/voice/OEM and authenticated portal require external acceptance. Cloud Run shared persistence remains unfinished; deployment deferred.

Stop at the user's 8% short-window floor; latest pre-handoff check12% remaining. Emulator started this run (exec81601); no live provider server remains. Preserve all evidence and ignored credentials.

## 9 October 2026 — Acceptance closeout, usage-floor handoff

**NOT COMPLETE / NOT READY.** Latest short-window usage is 7% remaining (weekly 70%); the user's 8% floor was crossed between checks. No resets, new live model calls, deployment, signing, commit or push. Stop new work and inspect active results on the next authorized continuation. Preserve all current changes on baseline `7012254`.

Completed: provider slow-header/cancellation defects reproduced and fixed with absolute-deadline TLS interruption and joined per-call watchers. 168 backend tests, 9 evaluation tests, 51/51 probes, 147 Android unit tests (explicit uncached rerun), six local hosted checks, five repetitions of 11 TLS/diagnostic tests, 2,000 recovery plus 2,000 pre-cancelled requests pass. Emulator TalkBack coexistence passed (not audible/human usability). The earlier two-hour run finished: 111 rounds, 7,209,179 ms, 1,554 local requests, 111 cancellations, zero model calls; file descriptors/threads bounded, PSS/heap increased modestly, no heap-leak certification. See `test-evidence/2026-10-09-acceptance-closeout/README.md` and JSON evidence.

**New active failure:** full core rerun reproduced stale guidance after a genuine WebView DOM change. `core-failure-device/mandate-core-final/transition-failure.png` visibly shows **Support opened** while Saathi still marks **Support**. `transition-input.txt`, windows, event and tap traces preserved. A separate portal fixture stopped at SENSITIVE_HANDOVER instead of CAPTCHA. Do not label either fixed by earlier passes. Settings test also failed; its corrected driver waits for the actual unique control and rechecks tap geometry. Its focused two-test retest passes and the next combined run passes Settings; this is a harness improvement, not proof of historical WebView causality.

**Unverified production candidate:** `SaathiAccessibilityService.scheduleCopy()` calls `clearCache()` on API33+ before reading the current root to avoid cached WebView descendants. Build/unit/release compilation/lint pass (`cache-build.txt`). This is a cache-staleness hypothesis pending functional verification, not an established root cause. Android <33 remains unchanged. Do not weaken privacy, old-window filtering or freshness to force acceptance.

Active tests at stop:
- emulator-5556, exec session **74450**: `cache-focused.txt`; LiveAccessibilityIntegrationTest with **40 WebView cycles**, PortalWorkflowIntegrationTest and PauseResumeIntegrationTest with **10 handoff rounds**. Output `/data/user/0/com.saathi/files/closeout-cache`. Last observed still running first test. Read final result and export evidence before any new install/test on this device.
- emulator-5554, exec session **54796**: `remaining-progress.txt`, runner `run_remaining.py`; practice **7 PASS**, research/reporting, connection, visual and browser groups pending/running. Output app files `mandate-<group>-final`; local fixture servers/reverse mapping are owned and cleaned by this runner. No genuine providers. Do not install/run another test there until finished.
- Original two-hour run is DONE; its data was exported before installing this final candidate on 5554. Original evidence belongs to the earlier APK, not this cache candidate.

UI: no design/layout/style/logo changes. Initial comparison 12/14 pairs identical; paused screenshot mismatch was Light vs Dark persisted preference, now the test explicitly selects/restores Dark; Home difference is inspected pressed shading. Final same-theme capture comparison is pending. `ui-source-preservation.json` was captured before the cache candidate; only new production Android edit since it is accessibility cache invalidation, not UI. Do not cite that earlier JSON as proving zero current production edits.

Next required work:
1. Inspect those two final test outputs. If WebView/portal still fail, collect traces and diagnose; do not keep a speculative fix just because it compiles. If passing, add deterministic cache/fresh-observation coverage and repeat affected combined core/service/privacy/cost checks; a passing rerun alone is not root-cause evidence.
2. Export final screenshots and perform same-theme paused comparison. Consolidate counts only after all final groups finish. Earlier core runs failed (one Settings failure, then WebView and portal failures); they must not be counted as 29 passing.
3. Update report/matrix with actual final evidence. Complete code/doc diff and secret checks after any new changes. Current code/doc diff check passed; raw completed log files retain normal trailing blank lines.
4. Genuine planning/incident retest awaits a fresh scoped answer. An asynchronous request for **up to four calls** was issued, but no answer was received before stopping. Previous eight-call allowance is exhausted. `evaluation.final_acceptance --suite reasoning` now enforces four reservations and has passing fixture tests; DO NOT execute absent explicit authorization.
5. Physical voice/OEM, legitimate protected portal, browser full HTTPS metadata limitations and Docker/runtime acceptance remain unverified. Single-host SQLite pilot is not stateless Cloud Run support; shared durable state/ownership architecture remains necessary for that target. No deployment.

## Usage-floor stop — final state

Further work stopped after the usage check returned 5% weekly remaining. No daily window was available. See NEXT_CONTINUATION.md for active test processes, completed groups, unresolved Chrome diagnostics and pending screenshot/endurance verification. This is NOT a completed pre-deployment certification.

## 9 October 2026 — Final mandate continuation (verification in progress)

Latest engineering/evidence report: [PREDEPLOYMENT_VALIDATION.md](PREDEPLOYMENT_VALIDATION.md). This section supersedes older counts and next-step lists below; historical records are retained.

Implemented: bounded primary attempt/fallback within the original screen deadline; Groq-first navigation default (explicit config preserved); private-safe phased HTTPS diagnostics; narrative private handoff; explicit prerequisite/denial guards; accurate capability copy using unchanged styles; controlled portal/copy/paste/rotation/lock tests; allowlisted non-root pilot container template.

Verified: 147 Android unit tests, 165 backend tests, 9 evaluation tests, 51/51 probes, 2,000 recovery plus 2,000 pre-cancelled synthetic requests, six local Gunicorn checks, zero lint errors/61 warnings, debug/test builds and release compilation. No configured-key matches in source/non-ignored files or decompressed APKs. Current emulator reruns and two-hour target must be read from `test-evidence/2026-10-09-final-mandate/`; do not call ongoing runs complete.

New eight-call allowance EXHAUSTED: English/Hinglish paired navigation passed; Gemini planning/incident timed out waiting for headers, and Groq failed independent plan/signal expectations. Post-run reasoning guard/prompt changes are offline-tested only. No more live calls without new scoped authorization. Physical audio/OEM, legitimate protected portal, browser-dependent full HTTPS origin, historical post-tap causality and deployment acceptance remain unresolved; do not declare production readiness.

## Current evidence — 9 October 2026, request lifecycle/transport continuation

The older dated matrix below is historical. Overall: **PARTIAL, not production-ready**; deployment/release not executed.

| Area | Current result | Evidence / limit |
|---|---|---|
| Research/incident/status cancellation | PASS | Response-after-Pause-before-Stop regressions reproduced, fixed and retested; existing UI preserved |
| Masked private practice handoff | PASS | Four integration tests; package boundary retained, no guessed target/no HTTP during private entry, safe receipt completion |
| Gateway capacity/callback ordering | PASS | Forced worker pause reproduced premature completion; cleanup-before-notification passes, including 1000 recovery requests |
| Core/privacy/browser/research regression | PASS within tested scope | 27 core/research/incident, 4 practice and 21 connection/reporting/recovery tests pass; 147 Android unit, 152 backend, 7 evaluation, 51 offline probes |
| UI preservation | PASS within captured scope | 8 tests, 12/14 PNG pairs identical; remaining two are inspected pressed-state shading; no rendering/style source changes |
| Genuine corrected live prompt | PARTIAL | English paired PASS, Groq Hinglish accepted; Gemini Hinglish timeout; four-call allowance exhausted |
| Mixed failure/resource coverage | PARTIAL | Eight-round retest passes (140 s, 112 local requests). Earlier recovery failure lacks definitive root attribution; no hours-long/OEM certification |
| Historical WebView/cloud intermittence | OPEN | Current WebView loops and cloud storm pass; original causes are not established |
| Trusted full browser destination | SAFE FALLBACK / PARTIAL | Missing/edited/scheme-less metadata still withholds source-bound highlights; do not invent HTTPS provenance |
| Protected portal and physical speech/device acceptance | BLOCKED / MANUAL | Legitimate portal access and physical Android phone unavailable |
| Deployment/production operations | DEFERRED | User requested stopping before deployment; no host/TLS/account/release execution |

Detailed failures, fixes, commands and per-suite scope: [evidence](test-evidence/2026-10-09-research-ordering/README.md), [latest handoff](NEXT_CONTINUATION.md). Fixture counts do not prove real semantic accuracy or completed real-world tasks.

## 9 October 2026 — Complete-tree privacy and resource cleanup

Implemented fail-closed complete accessibility traversal: node/depth limits, missing children and cancelled/expired copies cannot produce a partial safe screen. Child references are released on every unwind; accepted service work runs cleanup after destruction. Capture timestamps precede copying and stale delivery is rejected. No UI layout/theme changes, live provider calls, commits or deployment.

Verified so far: 147 Android unit tests, 152 backend tests, six evaluation tests and 51/51 offline probes pass. Debug/test builds, release Kotlin and lint pass (0 errors/61 warnings). Pattern scan found no matching secrets in tracked/nonignored text. New real-service oversized-tree regression passes with actual rejection evidence and original-task recovery. Ten thousand mixed traversal tests balance child ownership; this is not proof of Android heap leak freedom.

The first combined emulator run was 16 tests / 2 failures. One was a polling helper treating an incomplete transitional WebView tree as an exception; helpers now retry only missing-branch observations under the existing timeout. The other is OPEN: ResearchRecoveryUiTest.leavingDuringPlanRequestDiscardsLateFailureAndDoesNotRestoreRetry showed late error copy after returning. Inspect whether the fixture's five-second release/Compose idle waiting completes before Home/onStop; do not assume that hypothesis is the cause or weaken cancellation assertions. Production ResearchActivity was not changed.

Final affected browser/service rerun is in test-evidence/2026-10-09-observation-reliability/emulator-final.txt. See that directory's REPRODUCE.md for baseline failures, fixture-runtime diagnosis and commands. Initial separate test-APK crash was missing Kotlin Intrinsics in a new provider; Java fixture fixed it. This is unrelated to the historical WebView failure.

NEXT FIRST: collect final rerun if still running, then diagnose the research cancellation regression with request-release and activity-stop ordering. Keep accepted pause/resume/privacy tests. Historical WebView/cloud intermittence, broader hours-long/resource checks, genuine provider prompt retest, browser full-HTTPS metadata limitations, physical voice/OEM and authenticated portal steps remain open. No claim of production readiness or zero bugs. Stop above the user's 8% five-hour floor; deployment/release remain deferred.

## 9 October 2026 — Handoff verification completed; broader remediation remains open

Collected the previously unfinished run: 120 WebView cycles passed in 198 seconds, 60 private/message/CAPTCHA handoffs passed. Fixed Chrome's outdated privacy-copy assertion. Added debug-only gateway reason counts with an allowlist test; release diagnostics are no-op, no bodies/credentials saved. The earlier synthetic-cloud failure did not reproduce in the focused or combined rerun: cause remains unresolved, not claimed fixed.

Final evidence: **140 Android unit, 152 backend, six evaluation, 17 combined emulator and 10 reporting/consent tests pass**. Debug/test builds, release Kotlin compilation and lint (0 errors/61 warnings) pass. 660 synthetic events produced four local-backend requests, two cancellations, zero genuine provider calls. The earlier 51/51 offline probes remain passing; no architecture changes there. Eight UI methods passed before/after; 11/14 PNG pairs pixel-identical, remaining differences inspected as privacy-copy/scroll and pressed-state tint. No design changes. See test-evidence/2026-10-09-private-handoff/UI_COMPARISON.md and continuation-* logs. Fixture servers, mappings and temporary token cleaned up. No commit/deploy/live model calls/reset credits.

Stop reason: weekly usage reached 2% remaining despite 50% five-hour remaining; preserve this verified phase. Still OPEN: historical WebView failure cause, earlier intermittent cloud-fixture cause, broader mixed-fault/hours-long and retained-resource testing, full real-HTTPS destination binding where Chrome omits metadata, genuine provider prompt/accuracy retest (new authorization required), physical voice/OEM/TalkBack and authenticated cybercrime portal steps. Current checks do not prove absence of all leaks, universal compatibility or production readiness. Deployment remains deferred.

Next: use existing sanitized diagnostics to reproduce unresolved failures; complete remaining privacy/resource coverage without repeating accepted pause/resume. Real provider retest requires a new bounded allowance; previous four calls were consumed. No hardware or legitimate protected portal session is available.

## 9 October 2026 — Private handoff fixes; usage-floor handoff

See FINAL_RELIABILITY_REPORT.md for the 15-area audit, fixes, evidence and open gates. 139 Android unit, 152 backend, six evaluation tests and 51/51 offline probes pass; build/release compilation/lint pass. New masking regression reproduced before fixing. 60 handoffs pass with zero cloud calls. Current UI preserved; functional privacy copy updated. Final stress/browser run and after-screenshot captures were still executing at the 9% usage check: inspect their logs, collect images and compare pixels before declaring this phase complete. Physical-device, genuine provider retest, authenticated portal and real HTTPS positive binding remain open. No commit/deploy/live calls. Prior pause/resume remains accepted. Exact brief saved in specs/FINAL_RELIABILITY_PRIVACY_HANDOFF_REQUEST.txt.

> Latest 8 October: optional paired source-cited control hints, per-step consent and local fresh/current/source-address matching are implemented. 132 Android unit / 152 backend / 6 evaluation tests and 51 offline probes pass; nine final emulator tests pass. Chrome's observed scheme-less editable address fails closed, so real HTTPS positive highlight certification remains OPEN. Four newly authorized genuine calls returned HTTP 200: cited plan PASS; navigation pair FAIL (Gemini completion evidence). Clarified prompt only has offline retest. Protected portal/device/endurance gates remain OPEN; deployment deferred.

> Latest expiry regression: observed expired plans cannot revive after clock rollback; 128 Android unit / 148 backend / 5 evaluation tests pass. Debug build passes. No new emulator, live provider, portal or physical-device evidence in this subphase. All broader gates remain open.

> Latest 8 October recovery update: 147 backend and 127 Android unit tests pass; explicit retry/cancellation/source retention and fixed multilingual fallbacks now have regression evidence. Five evaluation tests means four harness tests plus one multilingual architecture test (older entries grouped these inconsistently). Deployment/release are deferred by user request. Overall capability gates below remain open; synthetic recovery does not certify real-model reasoning or arbitrary browser guidance.

> 8 October update: source-reading mode now has real-service, Chrome HTTP-fixture, consent UI, pause/resume and return/rotation/replacement/expiry evidence. Public-host, partial-tree, certificate-warning and semantic-private-context gaps were fixed. 125 Android unit and 140 backend tests pass. This does **not** close actionable live-plan binding or prove real HTTPS browser authority. See the latest handoff/evidence index for final emulator scope.

# Remediation evidence — 7 October 2026

**Overall remediation remains open. Saathi is not ready for arbitrary real-world task guidance.** The accepted pause/resume phase is preserved. This phase adds executable research/planning and safety components; controlled fixtures are not genuine-model accuracy or successful real applications.

Final counts: **139 backend tests, 111 Android unit tests, 5 evaluation-harness tests and 51/51 offline probes pass**. Emulator results are indexed separately with overlap; debug/test builds and release Kotlin compilation pass, lint 0 errors/61 warnings.

Evidence directory: `test-evidence/2026-10-07-research/`. No live provider calls, deployment, signing, commit or push in this phase. Existing 142 catalog workflows retain their independent acceptance requirements.

| Capability | Passing implementation evidence | Remaining gate |
|---|---|---|
| Backend web retrieval | Provider-neutral broker, optional SearXNG POST search, reviewed seed crawl, public-IP TLS pinning, DNS/read/page/byte limits, cancellation | Chosen search service, reviewed real source scopes and actual hosted TLS retrieval acceptance |
| Provenance/authority | Exact reviewed origin/path scopes; official/primary/secondary/community/unverified; original URL/title, review basis, digest and quoted excerpt; unverified hits cannot starve official seeds | Human authority review of real deployments; no automatic claim that a government-looking domain is official |
| Freshness/current information | Claim-specific TTLs, publisher date metadata, old community outage reports excluded, invalid/future dates rejected; changing-claim intake routes to research | A fresh fetch is not proof the underlying rule remains current; interpretation and source completeness require verification |
| General prerequisites | Paired provider extraction contract, source membership/quotes, jurisdiction checks, bounded DAG, cycle/missing-node rejection | Genuine extraction quality and applicability across real workflows |
| Multi-level dependency planning | Three-level graph returns to the original goal; each dependency requires confirmation; seven catalog domains use the same general mechanism in three locales | Source-cited hint binding now has controlled contract/policy and consent evidence; supported real-browser full-address binding and positive service-to-overlay acceptance remain open |
| Eligibility | Relevant local boolean facts only; unknown remains insufficient; source/freshness review required; positive result at most possibly eligible | Full semantic criteria coverage, alternatives/exceptions and official determination are not established |
| Plans/completion | Explicit retry uses fresh attempt identity, preserves evidence and respects replay/budget limits; Explicit USER_CONFIRMATION predicates, no model COMPLETE; context/expiry checks; memory-only checklist and original goal | Source expiry/return/detour checks exist; source-cited hint binding is implemented under exact-address/current-step conditions; positive real-browser binding and independently verified completion remain open |
| Error/outage research | Explicit Service issue mode; paired free-prose incident hypotheses require exact current citations and authority/jurisdiction checks; conflicting/absent evidence remains unknown; public error cues offer blank consented research; no payment retry | Genuine semantic inference, actual account diagnosis and live workflow binding remain unverified |
| Community/Reddit distinction | General community source type and attribution; anecdotes cannot support eligibility/requirements or override official policy; historical/undated reports distinguished | Live coverage of selected community sites (access restrictions may apply) |
| Malicious destinations | Canonical HTTPS URLs, reviewed origin/path authority, no redirects/private DNS/userinfo/IDN; high-risk cross-app guidance without bound provenance fails closed | Full-address source matching is unit-tested, browser unknown-address handover tested; real HTTPS positive binding and high-risk guidance remain open |
| Prompt injection | Retrieved text inert; strict schemas, no tools/actions/LLM URLs, exact evidence IDs/quotes; adversarial directives refused | No proof of universal semantic resistance or genuine-model adversarial accuracy |
| Provider diagnostics | Request/session correlation, model/provider, latency, HTTP receipt/status, token metadata and fixed validation categories; malformed-body HTTP receipt retained without raw body | Earlier six rejected live pairs cannot be diagnosed retrospectively; new live retest needs separate scoped authorization after remaining gates |
| Chrome readiness | Test-only flags/debug-app setup, prior settings restored, alphabetic fixture nonce avoids private-number guard; actual Chrome service guidance verified | Other browser versions and real sites require acceptance; no first-run or certificate bypass in production |
| Browser interruption | Real Chrome synthetic CAPTCHA and cookie choice handovers have no target; retained original goal resumes afterward | Source-mode interruption/resume also tested; real authentication, CAPTCHA and authenticated forms remain manual |
| Cloud storm/cost | Actual service to local fake backend: 660 emitted events, 60 content mutations, 83 delivered records, 9 observations, 3 HTTP requests, 1 cancellation (latest run); durable global/user budgets tested | No semantic request deduplication, public load test or cost-per-completed-real-task certification |
| Privacy/telemetry | Private message/document/account cues now enter the shared private/listening handover; strict payloads reject raw screen additions; synthetic private payload absent from logs/DB/diagnostics; unused growing session history removed; debug AI default opt-in fixed; input-method windows/packages excluded before tree copying; research uses FLAG_SECURE | Exhaustive semantic privacy classification and external provider/host telemetry assessment remain open |
| Lifecycle/resources | 120 pause/resume cycles with PSS/heap samples; 60 same-session WebView cycles in 77.3 seconds, PSS 106223→109262 KB; reproduced late-old-window content event fix preserves real root/window invalidation | Hours-long mixed-fault, physical-device/OEM, actual audio and performance validation remain open; historical post-tap failure cause not established |
| English/Hindi/Hinglish | Same evidence graph/fact logic across three locales; category routing, checklist and report labels/glossary localized; Hindi 200% font test | Real language intent/extraction equivalence and source translation quality not certified; quotations deliberately remain original |
| Hosted readiness | Existing HTTPS/per-user auth, reauthentication after work, durable quotas, isolation and bounded workers extended to research; optional deployment configuration documented | Real chosen host/TLS, account provisioning, operational testing and release connectivity external configuration |
| Existing boundaries/UI | Existing green/glass components retained; content-sized actions, local secret handling, stale/cancellation guards and pause/resume regressions | TalkBack, device performance and full design parity retain previous manual gates |

## Failures investigated, not erased

- Missing research modules at baseline: `baseline.txt`. New retrieval/planning/search implementations and deterministic tests now exist.
- Chrome preparation needed persistent debug-app setting and a pipe-written flag file; earlier shell redirection did not write the flags. Browser setup is test infrastructure only.
- Cached browser fixture omitted new buttons. A unique URL fixes cache reuse. Numeric nonce was rejected by the existing private-value guard; changed only the fixture identifier to letters.
- CAPTCHA test incorrectly assumed no presentation identity. Corrected it to inspect the actual no-target overlay; a text-only handover legitimately retains observation identity.
- A new idle trace showed an old window's late content event invalidating a newer active root. `ObservationEventPolicy` filters that case only. Unknown/current roots and window transitions still invalidate. This is not proof of the historical WebView post-tap cause.
- Extended UI run exposed preselected AI in debug setup. Restored explicit opt-in; updated an obsolete text selector without removing the consent assertion. The failing run is retained in `emulator-extended-before-consent-fix.txt`.
- The DNS regression reproduced multicast addresses being accepted by Python’s `is_global`. Retrieval now explicitly excludes multicast/reserved/unspecified addresses; failing and passing evidence is retained.
- Practice gateway tests expected an obsolete instruction on private forms. They now assert the accepted SENSITIVE_HANDOVER/no-target behavior while preserving no-private-HTTP and observed-completion checks.
- One local permission-review attempt timed out; retry succeeded. A sandbox-only socket test failure was rerun with localhost permissions. Neither is reported as an application defect.

## Next repository work (not external blockers)

1. Bind a reviewed plan's current step and destination provenance to a fresh browser observation; preserve original goal and require revalidation on redirect/detour/error. Do not pass an unconstrained plan to the screen model or relax existing high-risk guards.
2. Integrate incident hypotheses with plan/screen revalidation; preserve unknown scope and do not treat exact citations as verified semantic entailment.
3. Expand mixed-fault, long-session, changing-screen and semantic privacy acceptance; add coverage without calling fixture success real-model correctness.

## External/manual work

Choose and review source/search hosting and source-authority records; configure the actual HTTPS backend/accounts; review current official policies and protected workflows; separately authorize genuine-model evaluation after architecture gates; validate physical speech/OEM/TalkBack/performance. No real private data, transaction, CAPTCHA solving or complaint submission should be used as automated tests.
