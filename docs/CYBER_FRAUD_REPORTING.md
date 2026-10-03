## 3 October — reviewed complaint worksheet and per-field copy

`Prepare complaint draft` opens a secure, memory-only worksheet from the optional typed/dictated incident account. It asks when/contact/impact/actions/evidence follow-ups, leaves unknown facts blank and composes an incident narrative verbatim from supplied sections. Date/time and impact notes are offered separately when known. It never invents an amount, date, intent, guilt or evidence. This is local fact assembly; optional paired AI assessment remains on the reporting screen with separate summary-sharing consent, not a claim of model-written or legally verified complaints.

The person reviews the generated narrative and selects “I checked this matches what happened”. Editing resets review, invalidates a pending copy and stops any previous draft helper. Each field opens an exact-text preview and purpose-specific instructions. **Allow copy** writes only that field with Android's sensitive-clipboard marker; **Not now** changes nothing. Saathi never reads the clipboard. Copied text may outlive the worksheet; the UI tells the person to clear it when finished.

The optional helper carries at most three reviewed fields in process memory (not Intent extras, disk or cloud), asks again before every field copy, and expires after two minutes/lock/screen-off/permission loss/dismissal. It uses the existing palette and content-sized native pills, is screenshot-protected and has an opaque fallback. Android may stop the service earlier or hide overlays; the in-app copy path remains available. Return to the **already-open** browser using recent apps, check the official address, focus the matching field, long-press and Paste. No new tab or form reset is forced. Text-only is default; optional English voice reads the return/paste instructions and stops when leaving.

**Scope limits:** no automatic paste/field inference into private forms, no portal-specific field-length assumption, no captured browser values, no declarations/CAPTCHA/authentication/submission. Actual private portal labels and limits are not verified; date/amount controls may need manual formatting. A user must check the visible field and review pasted text. The worksheet is cleared on recreation/closing and cannot guarantee survival under process death. Passwords/OTPs/PINs/full account data belong only in the official form.

Checks cover exact supplied facts, unknown omissions, review reset, independent per-field consent, unchanged clipboard on decline, sensitive clipboard flag, recreation clearing, helper permission loss, and light/dark rendering. Real portal form-paste and audible instruction delivery remain unverified.

# 3 October follow-up — public portal and optional AI path

All fixed app/copy/FAQ links now use **https://cybercrime.gov.in/**. The apex host loaded with normal HTTPS validation on the host and in emulator Chrome; www produced an expired-certificate validation error here. No certificate checks were disabled. Public menu/category/filing-entry screenshots are in screenshots/2026-10-03-backend-reporting. Verified path stops at the filing explanation; private forms, acceptance, login and submission remain untested. The observed route labels are reflected in the existing checklist. If unavailable, immediate 1930/bank and offline reporting advice remain accessible.

An optional **Ask AI about this issue** action is implemented. It requires the configured debug AI backend, a short summary passing conservative privacy checks and explicit **Share once** approval. Keep private sends nothing. Typed/voice drafts remain reviewable; audio and screenshots are never assessment inputs. Editing, clearing or leaving cancels work and rejects obsolete callbacks. Provider failure/disagreement falls back to a clear unavailable result with offline advice available. The shared green/glass buttons and Saathi dialog surface are preserved.

The backend asks Gemini/Groq independently and only accepts matching bounded reporting-category/signal codes; UI uses reviewed cautious wording. This is model-backed code, **not yet a genuine live-model evaluation**: the incident-assessment tests used deterministic providers. Later connection checks verified each real provider independently, but did not evaluate genuine incident assessment; see [provider verification](PROVIDER_VERIFICATION.md). No crime is certified, no safe/no-fraud clearance is given and no complaint is submitted. Free-text personal-data filtering is incomplete by nature; explicit review remains essential. New assessment/reporting copy still needs Hindi/Hinglish review; offline keyword triage remains separately labelled.

Official sources checked: [portal](https://cybercrime.gov.in/), [public filing explanation](https://cybercrime.gov.in/Webform/Accept.aspx), [FAQ](https://cybercrime.gov.in/Webform/FAQ.aspx). The earlier www timeouts below are historical and superseded for this public path, not a guarantee that all future pages work.

# Cyber-fraud reporting and privacy regression — 2 October 2026

## Implemented

Home and the assistant panel now open **Report cyber fraud**. The entry keeps the existing Saathi logo, light/dark palette and glass buttons/panels. Shared buttons size to their label plus accessible padding; long labels wrap within available width. The reporting screen supplies:

- Immediate 1930 dialler and bank-contact advice, available without completing an intake.
- Optional typed or speech-service description. The draft is memory-only, excluded from saved state and cloud requests unless the user approves the new per-use AI sharing dialog, and protected from screenshots. Device speech services may process audio online; the UI says so before voice input. Dictation is reviewed before advice.
- User-selected categories and a limited local English/Hindi keyword triage. It states possible concern/uncertainty; it never certifies a crime, declares an incident safe, or presents keyword checks as AI analysis. No report is sent to police from Saathi.
- Eight manually advanced steps: urgent action, evidence, browser/official URL, complaint route, private registration, incident/evidence details, user review/submission, acknowledgement/follow-up. Text is the default. Optional English TTS reads steps; changing steps cancels old speech, and leaving the screen stops this reader. This reader is not the background conversational service.
- Browser chooser, fixed official URL, and explicit per-use copy consent. Cancel does not change the clipboard. Approve replaces it with only the fixed official portal URL. Saathi never reads the clipboard or injects a paste.
- Optional short-lived floating copy helper. It uses the same palette, rounded containers and content-sized buttons over permitted apps. It closes on dismissal, lock, screen-off, overlay permission loss or a two-minute limit; it is not restarted. Without overlay access, a themed explanation opens Android settings only on request; in-app copy/open remain available. Android permission screens and protected-app overlay restrictions remain system controlled. The helper uses an opaque accessible fallback, not sampled browser blur.
- A route to the existing assistant for a visible-option marker. This does not hardcode a changing portal DOM or submit complaints. Opening the reporting screen ends a previously running guide; returning from an explicitly started helper suspends screen/audio work while Saathi is foreground and allows guidance on the subsequent external screen.

## Privacy bug and backend changes

The prior numeric rule treated any standalone four-or-more-digit sequence as private. That incorrectly matched the year in a travel date and ungrouped fares. Explicitly formatted public dates/currency now bypass that numeric rule, while password flags and sensitive metadata take precedence across all node properties. Date-of-birth, account/card-number, Aadhaar and passport cues remain private. Unexplained numeric strings, OTPs, secrets in URL paths and adjacent secret numbers still mask. This remains a conservative heuristic, not exhaustive privacy detection. Public-format exceptions must never authorize uploading editable values; live target selection excludes editable nodes.

The same display exceptions and semantic cues apply to backend live labels. The local consequential-action matcher now uses word boundaries, so Shopping/Spinning/Display do not accidentally match pin/pay substrings. Provider targets and explanation fields have explicit type/length validation. Expired observations are rejected before reserving calls and checked again before returning advice. Inactive session revision records expire after five minutes, beyond the 15-second observation window; quotas do not reset. Active requests are retained. Existing cancellation, disagreement, quotas and fail-closed circuits remain.

## Source basis and limits

Checked official source extracts on 2 October 2026:

- [National Cyber Crime Reporting Portal](https://www.cybercrime.gov.in/): complaint categories and distinction from suspect reporting.
- [Portal FAQ](https://www.cybercrime.gov.in/Webform/FAQ.aspx): routes for women/child-related and other cybercrime complaints.
- [Complaint checklist](https://www.cybercrime.gov.in/Webform/Crime_AuthoLogin.aspx): evidence preparation. The app deliberately follows displayed upload limits rather than freezing them in code.
- [Official helpline/contact page](https://www.cybercrime.gov.in/webform/Crime_NodalGrivanceList.aspx): national helpline 1930.
- [Official tracking page](https://www.cybercrime.gov.in/Webform/chkackstatus.aspx): acknowledgement/status follow-up.
- [RBI customer guidance](https://www.rbi.org.in/commonman/English/scripts/Notification.aspx?Id=2623): prompt reporting of unauthorised transactions to the bank.

Direct portal page loads returned gateway errors/timeouts during research; official indexed extracts were available. No live complaint, CAPTCHA, OTP, identity upload or submission was attempted. No portal-label-by-label end-to-end acceptance is claimed. Regional/SMS deadlines are not generalized. The user-supplied 18% statistic is omitted because its source was not established.

## Remaining / not certified

- Real IRCTC and Google Flights DOM/device reproduction, beyond regression labels and the emulator's native fixture.
- Current live portal/browser guided traversal through every public step; private forms always require user handover.
- Genuine model-based incident assessment, language review/localization and microphone recognition. No Gemini/Groq credentials or account are configured. Existing navigation adapters do not constitute a fraud-assessment model.
- Successful real complaint submission/status retrieval and physical-device voice, clipboard/OEM survival, accessibility and performance acceptance.
- Production backend hosting/TLS/authentication/release enablement and real provider responses. This phase fixes specific tested defects, not every possible backend failure.

See TEST_RESULTS.md for the final check counts and screenshot evidence. No paid service, publication, push or real transaction was performed.

### Browser follow-up
Chrome on the emulator now passes a localhost travel fixture with destination/date highlighting and unmasked formatted fares, plus private-password suspension. The direct official portal retry still times out. The current guide remains an offline reporting companion. See the 2 October browser follow-up in TEST_RESULTS.md; real-site/device reproduction is still pending.
