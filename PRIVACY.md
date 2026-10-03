# Privacy — local practice and opt-in live option finding

Updated 3 October 2026. Saathi observes accessibility structure during a user-started session. Practice sessions still reject other packages before flattening trees. The new **Help in apps & browsers** action separately lets the user start live option finding: it reads accessible control labels/structure in other apps locally to match the named option. The intake surface explains this before Start. It never taps, types or submits for the user.

By default, cloud reasoning is disabled. Screenshot capture remains disabled. There is no provider API key field in newly built APKs. Task-screen Android speech recognition and installed TTS engines may use network services; their offline operation and retention have not been verified. The speech-speed preview uses the installed TTS engine. Use typing when voice is not desired.

Background conversation is an explicit per-session action with microphone and notification permission. It uses on-device recognition on supported Android 12+ devices, alternates speaking and listening, and provides notification Pause/Stop controls. It suspends the microphone on private forms and unsupported screens, pauses on silence/errors/interruption, and stops with the session or screen lock. No transcript or recording is saved by this service. Actual engine and device behavior still needs hardware verification. See docs/VOICE_CONVERSATION.md.

Sensitive-field filtering is best effort, including password flags, English/Hindi cues and conservative numeric checks. It is not perfect. Do not enter real secrets into the practice flow. No screenshot or audio recording is written by Saathi. Android speech engines have their own behavior.

Free-form task text, conversation text and completion state are held in process memory and discarded at process death. A selected synthetic practice category may be saved by Android to restore the activity. Older persisted transcript, goal and generic completion preferences are cleared when the corresponding stores are constructed or the privacy reset runs. Language and theme preferences persist. Android backup is disabled for this build; old backups, earlier APKs and OEM transfer behavior are not remediated or verified. Settings → Privacy → Clear local data stops the session and clears Saathi preferences and in-memory stores; this reset was tested on the emulator. Android Clear storage removes local app data; uninstalling also removes local app data according to platform behavior.

Stop invalidates pending observation results, removes overlays and stops session services and speech. Empty, unavailable and ineligible screens clear the previous marker and spoken output. Practice waits for its fixture; live mode waits for an accessible external screen and never infers task completion. Text-only mode stops narration and conversation audio. The movable bubble and notification reopen the assistant panel; opening the panel suspends external-screen guidance. Screen-off also requests Stop. End-to-end lifecycle verification on physical devices remains outstanding. Disable Saathi Accessibility in Android settings to revoke observation. The debug-only cloud pilot is described below; production hosting/authentication and verified provider data terms remain open.

## Optional debug AI navigation

Settings → Backend connection has a separate AI-navigation choice and disclosure. If explicitly enabled and a configured server is running, the app sends the goal, package/window/session identity, locale, observation time, up to32 eligible control labels and the last3 suggested labels to its loopback gateway. The gateway sends goal, locale and filtered control/history data to Gemini and Groq independently. It does not send screenshots, coordinates, audio or editable field values. Entire detected password/private screens are withheld. Filtering is best effort; labels and goals can still contain personal data. Avoid real private accounts in the pilot.

Provider keys stay on the server. The app's development bearer token lives only in process memory; its setup screen blocks screenshots. Disable/Clear local data drops the token and pending results. Already-started requests may complete at providers after cancellation. Provider retention/data use follows their account terms, not Saathi's local deletion behavior. Saathi stores no request bodies/model replies on disk; the server keeps bounded in-memory session state and persistent aggregate provider call counts only. Do not enable external HTTP body logging.

Default local/release mode sends no Saathi model requests. Task-intake Android speech services remain separate and may use the network. Offline language-pack setup talks to the installed device speech service only after the user's explicit request. Checking availability never starts recording.


## Reporting and complaint drafts

Incident intake and the complaint worksheet stay in memory. The worksheet arranges supplied facts locally; it does not upload evidence or invent missing details. Closing or recreating the worksheet clears it. Optional voice input uses the installed speech service and may be processed online by that service.

AI incident assessment has its own sharing preview and requires **Share once**. The approved summary and concern category go through the configured backend to Gemini and Groq. Editing, clearing or leaving cancels the app's pending result; a request already sent may still be processed by a provider. Keep identifying details and secret values out of the summary. Filtering cannot guarantee anonymity.

Copying a complaint field requires review and a separate **Allow copy** confirmation for the exact text. Saathi writes that text with Android's sensitive-clipboard marker and never reads the existing clipboard. It does not paste into a browser or submit a complaint. Copied text may remain after the worksheet closes, so clear it when finished. The optional floating helper holds reviewed fields in memory for up to two minutes and closes on dismissal, lock, screen-off or loss of overlay permission.

## Connection diagnostics

Refreshing server status makes no model request. The separate API check requires confirmation and sends synthetic practice data to both configured providers. The server keeps up to 64 diagnostic records per provider in memory: request identifiers, status, model, timing and available token counts. It does not record prompts, incident summaries, keys, raw replies or opaque model signatures. Records clear when the server restarts; aggregate call-budget counters remain in local storage.
