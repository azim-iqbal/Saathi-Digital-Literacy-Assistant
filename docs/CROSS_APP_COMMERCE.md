# Cross-app commerce and field privacy — 10 October 2026

This phase adds local, hierarchy-grounded discovery/review guidance. It does not certify Zepto, arbitrary ordering or production readiness. Production styles, navigation, branding and layouts are unchanged.

## What was reproduced and fixed

`NodeContentPolicy` used the same numeric-content filter for resource IDs and displayed values. An ordinary `₹47` label with `shop:id/product_123456` therefore became sensitive. `before.txt` preserves the failing regression. Standalone ₹47/₹36/₹30 already passed in this checkout: money alone was not the reproduced cause. Without the reported Zepto accessibility tree, this path cannot be called the confirmed cause of every original banner.

Resource IDs now contribute credential metadata, not apparent entered numbers. Password flags/types and credential cues still win. Decimal digits in localized price/date formatting are normalized before existing public-display exceptions; bare numeric secrets remain redacted. The expanded corpus protects CVC, TOTP, spaced OTP, card expiry, security answers and authentication tokens as well as existing credentials. None of these tests reads an editable value or description.

Node categories distinguish public display/action, normal/personal input, authentication/payment, unknown redaction and decoration. Categories are structural/metadata assessments, not permissions to upload or transact. Personal inputs still have no copied values. Screen-wide private narrative minimization remains intact; broad semantic understanding is not claimed.

PRIVATE FIELD badges now require an actual structural editable private field, use its current bounds and are deduplicated. Arbitrary redacted product labels no longer receive editable-field badges. On eligible mixed forms/products, safe local targets can remain available while cloud/listening are suspended. Focused secrets, private narrative contexts, challenges, payment-state warnings, consent, errors, security warnings and recognized modals retain the handoff. No private-value upload was enabled.

## General commerce path

The request parser recognizes bounded English/Hindi/Hinglish ordering phrases. It retains the original request and recomputes the next step from each current snapshot:

- Guide a uniquely labelled Search control; the user enters the query.
- Match request terms to public product labels. Associate an Add control only through a copied accessibility ancestor containing the matching product and a price. Never choose by screen distance, app ID or remembered coordinates.
- If multiple cards match, ask the user to choose. Missing hierarchy, conflicting branches and inaccessible controls produce a no-target fallback.
- After an observed quantity/added state, guide a unique View cart control.
- At cart review, leave item, delivery, quantity, total and purchase decisions to the user. No order-completion claim, automatic typing, tapping, substitution, payment or submission.

This is deterministic local guidance, not genuine-model reasoning. The external fixture demonstrates search → correct repeated Add → observed quantity → cart review → private handoff → same-task return. It does not perform an order or show a real retailer. Arbitrary phrasing, customization, unavailable products, checkout/address selection, rides, tickets and every app's merged accessibility nodes are not certified by that test.

## Physical Zepto verification

On 10 October, a user-authorized Samsung test verified one real public Zepto product-results flow: exact product identification, a single reversible Add, observed in-card quantity state, a unique Cart review target and removal of the same test item. Guidance was snapshot-local and made no model calls. The final state returned to a visible Add control. No checkout, payment, purchase, private entry or automatic tap by Saathi occurred.

This closes the earlier "no phone / no Zepto" prerequisite for that narrow flow only. It does not make Zepto support universal: product cards, saved locations, stock, screen hierarchies and app versions can change. The evidence contains filtered public labels and structural metadata only; device screenshots/XML with address details were deleted.

## Popups

Only recognized modal hierarchy plus explicit promotional evidence plus a unique named dismissal produces a suggested target. Android's standard `android:id/parentPanel` is a platform dialog contract; no retailer IDs are used. Bare X controls do not qualify. Security, consent, permission, authentication and payment/destructive choices override promotional wording. Unknown or nested dialogs remain user decisions. No automatic dismissal exists. Custom modals without reliable container semantics remain unsupported rather than guessed.

## Forms and stale observations

Native/WebView service regressions cover field entry/clearing, invalidity, optional skipping, delayed dependent-field insertion/removal and a safe empty City field beside a private input. No zoom gesture, rotation or manual refresh is used to advance those forms. Private contents are absent and the session stays the same. Existing revision/session/window checks, cache invalidation, immediate external-event invalidation and finite form probes are preserved.

These results do not establish the historical WebView incident's sole cause, universal browser compatibility, positive validity of every filled field, inferred dependency semantics or verified submission. Those remain separate acceptance work. Repeated successful tests are not causal attribution.

## TinyFish decision

The [official documentation](https://docs.tinyfish.ai) was fetched through the TinyFish CLI. It lists Agent, Research, Search, Fetch and Browser. Search/Fetch have a daily free allowance; other surfaces draw from a wallet. The one public documentation fetch reported 321 ms. This is not a representative latency/cost/accuracy comparison.

**Not integrated.** No comparative benchmark yet demonstrates an improvement over the existing retrieval stack. No wallet-based Agent/Research/Browser call was made. Remote Chromium cannot represent native Android's currently visible UI; it must never replace local accessibility grounding. A future retrieval comparison must measure source/freshness/provenance, failures, latency, token/cost impact and data minimization before choosing an adapter.

## Acceptance matrix

PASS means only the exact controlled assertion has evidence; PARTIAL is not an end-to-end pass.

| Category | Discovery/search | Product/option selection | Cart/review | Auth/payment handoff | Completion | Real app |
|---|---|---|---|---|---|---|
| Quick commerce | PASS synthetic + one physical Zepto result screen | PASS hierarchy/repeated Add + exact observed Zepto item | PASS synthetic + physical reversible cart review | PASS synthetic return | NOT RUN, no purchase | PARTIAL: one Samsung/Zepto flow; not universal |
| Food delivery | PARTIAL phrase/card fixture | PARTIAL generic veg-burger card | NOT RUN | NOT RUN category-specific | NOT RUN | NOT RUN |
| General commerce | PARTIAL phrase/card fixture | PARTIAL generic soap card | NOT RUN category-specific | NOT RUN category-specific | NOT RUN | NOT RUN |
| Ride booking | NOT RUN | PARTIAL public-fare corpus only | NOT RUN | NOT RUN | NOT RUN | NOT RUN |
| Ticketing | NOT RUN | PARTIAL public-ticket-price corpus only | NOT RUN | NOT RUN | NOT RUN | NOT RUN |
| Government/public service | Prior controlled source/portal evidence | PARTIAL | NOT RUN authenticated | PARTIAL controlled portal | NOT RUN submission | Protected portal BLOCKED by legitimate access |
| Native form | PASS controlled | PASS reactive metadata | PARTIAL review | Prior private-field tests | NOT RUN validated submission | Broader apps NOT RUN |
| WebView form | PASS controlled | PASS without zoom | PARTIAL review | PASS mixed private return | NOT RUN validated submission | Broader sites NOT RUN |

## Remaining work and boundaries

- A real Zepto installation/location and one public shopping path were verified on the user-authorized Samsung. Checkout, payment, stock changes, other locations, other product layouts and arbitrary retailer compatibility remain unverified.
- General screen-context inference, richer commerce/ride/ticket flows and more flexible intent understanding remain PARTIAL/ABSENT, not external blockers.
- Broader form validity/dependency/completion and historical stale-highlight attribution remain PARTIAL. No timeout/freshness/privacy guard was weakened.
- Genuine planning quality remains FAIL/PARTIAL from the last evaluation. All previous live-call allowances are exhausted; zero new model calls in this phase.
- Physical voice, OEM survival, TalkBack/performance and protected portal acceptance require actual hardware/access. Hours-long resource acceptance was not repeated here.
- Shared durable production storage remains design-only; deployment stays deferred. No commit, push, deploy or publish.

See `test-evidence/2026-10-10-commerce/README.md` for exact final counts, artifacts and screenshot comparison, and NEXT_CONTINUATION.md for the next entry point.
