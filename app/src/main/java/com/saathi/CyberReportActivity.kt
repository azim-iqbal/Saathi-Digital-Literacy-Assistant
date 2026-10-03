package com.saathi

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saathi.cyber.CyberReportGuide
import com.saathi.orchestrator.SaathiSession
import com.saathi.ui.*
import com.saathi.ui.glass.*
import com.saathi.ui.navigation.rememberNavigationEnvironment
import java.util.Locale

/** Memory-only incident intake; sharing and clipboard writes require separate visible consent. */
class CyberReportActivity : ComponentActivity() {
    private var tts: TextToSpeech? = null
    private var speechReady by mutableStateOf(false)
    private var message by mutableStateOf<String?>(null)
    private var assessmentCall: com.saathi.core.GatewayCancellation? = null
    private var assessmentGeneration = 0L
    private var assessing by mutableStateOf(false)
    private var aiAssessment by mutableStateOf<String?>(null)
    private fun cancelAssessment() {
        assessmentGeneration++
        assessmentCall?.cancel(); assessmentCall = null; assessing = false
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        // Do not allow an old live session to observe a personal incident description.
        SaathiSession.stop()
        tts = TextToSpeech(this) { status ->
            if (!isDestroyed) speechReady = status == TextToSpeech.SUCCESS
        }
        setContent {
            val preferences = remember { Preferences(this) }
            val dark = preferences.theme == "Dark" || (preferences.theme == "System" && isSystemInDarkTheme())
            val environment = rememberNavigationEnvironment()
            var draft by remember { mutableStateOf("") }
            var concern by remember { mutableStateOf(CyberReportGuide.Concern.UNSURE) }
            var assessment by remember { mutableStateOf<String?>(null) }
            var step by rememberSaveable { mutableIntStateOf(0) }
            var spoken by remember { mutableStateOf(false) }
            var aiConsent by remember { mutableStateOf(false) }
            var copyConsent by remember { mutableStateOf(false) }
            var overlayConsent by remember { mutableStateOf(false) }
            val voice = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    draft = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty().take(1200)
                    assessment = null; cancelAssessment(); aiAssessment = null
                    message = "Review the transcript before continuing."
                } else message = "No transcript received. You can type or skip the description."
            }
            MaterialTheme(colorScheme = saathiColorScheme(dark)) {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface, LocalGlass provides GlassEnvironment(dark = dark,
                    reducedMotion = preferences.reducedMotion || !environment.animationsEnabled)) {
                    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding()
                        .wrapContentWidth(androidx.compose.ui.Alignment.CenterHorizontally).widthIn(max = 720.dp)
                        .imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SaathiBrand()
                        Text("Report cyber fraud", style = MaterialTheme.typography.headlineSmall)
                        Text("India • Reporting companion")
                        GlassPanel { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Money lost? Act now", style = MaterialTheme.typography.titleMedium)
                            Text("Call 1930 and notify your bank immediately using its official contact. You do not need to describe the issue here first.")
                            GlassButton("Call 1930", onClick = { launchExternal(Intent(Intent.ACTION_DIAL, Uri.parse("tel:1930"))) })
                        } }
                        Text("Would you like to describe what happened?", style = MaterialTheme.typography.titleMedium)
                        Text("Optional. Keep names, passwords, OTPs, account numbers and evidence out of this box. The draft stays in memory and clears when this screen is recreated or closed. It is shared with the configured AI providers only if you approve Share once. Voice input uses your installed speech service, which may process audio online.", style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(value = draft, onValueChange = { draft = it.take(1200); assessment = null; cancelAssessment(); aiAssessment = null },
                            label = { Text("Describe your issue (optional)") }, modifier = Modifier.fillMaxWidth(), minLines = 3,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                        GlassButton("Describe by voice", primary = false, onClick = {
                            tts?.stop()
                            runCatching { voice.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, preferences.language.sttTag)
                                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)) }
                                .onFailure { message = "Voice input is unavailable. Type instead or skip the description." }
                        })
                        Text("Choose the closest situation")
                        CyberReportGuide.Concern.entries.forEach { option ->
                            FilterChip(selected = concern == option, onClick = { concern = option; assessment = null; cancelAssessment(); aiAssessment = null }, label = { Text(option.title) })
                        }
                        GlassButton("Show reporting advice", onClick = { assessment = CyberReportGuide.assessment(draft, concern) })
                        assessment?.let { Text(it) }
                        GlassButton(if (assessing) "Checking assessment…" else "Ask AI about this issue", primary = false, enabled = !assessing, onClick = {
                            if (!com.saathi.gateway.PracticeGateway.aiEnabled()) message = "AI assessment is not configured. Use Show reporting advice and the checklist; urgent reporting does not need AI."
                            else if (!com.saathi.core.IncidentAssessmentPolicy.allowed(draft)) message = "Write a short summary without names, numbers, contact details, links or secret values before requesting AI. You can use the offline checklist without sharing it."
                            else aiConsent = true
                        })
                        if (assessing) GlassButton("Cancel assessment", primary = false, onClick = { cancelAssessment(); message = "AI assessment cancelled. The reporting checklist is still available." })
                        aiAssessment?.let { Text(it) }

                        GlassButton("Prepare complaint draft", primary = false, onClick = {
                            tts?.stop(); cancelAssessment()
                            runCatching { ComplaintDraftActivity.open(this@CyberReportActivity, draft) }
                                .onFailure { message = "Draft worksheet unavailable. Use the checklist and describe the incident directly on the official portal." }
                        })
                        Text("Guidance style", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            FilterChip(selected = !spoken, onClick = { spoken = false; tts?.stop() }, label = { Text("Text only") })
                            FilterChip(selected = spoken, onClick = { spoken = true }, label = { Text("Text + voice") })
                        }
                        val current = CyberReportGuide.steps[step]
                        GlassPanel(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Step ${step + 1} of ${CyberReportGuide.steps.size} · ${current.title}", style = MaterialTheme.typography.titleMedium)
                            Text(current.body)
                            if (spoken) GlassButton("Read this step", primary = false, onClick = { read(current.title + ". " + current.body) })
                            if (spoken) GlassButton("Stop reading", primary = false, onClick = { tts?.stop() })
                        } }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            GlassButton("Back", compact = true, primary = false, enabled = step > 0,
                                onClick = { tts?.stop(); step--; if (spoken) read(CyberReportGuide.steps[step].body) })
                            GlassButton("Next", compact = true, enabled = step < CyberReportGuide.steps.lastIndex,
                                onClick = { tts?.stop(); step++; if (spoken) read(CyberReportGuide.steps[step].body) })
                        }
                        Text(CyberReportGuide.PORTAL)
                        GlassButton("Open official portal", onClick = {
                            tts?.stop()
                            launchExternal(Intent.createChooser(Intent(Intent.ACTION_VIEW, Uri.parse(CyberReportGuide.PORTAL))
                                .addCategory(Intent.CATEGORY_BROWSABLE), "Choose your browser"))
                        })
                        GlassButton("Copy official link", primary = false, onClick = { copyConsent = true })
                        GlassButton("Show floating link helper", primary = false, onClick = {
                            if (!Settings.canDrawOverlays(this@CyberReportActivity)) overlayConsent = true
                            else runCatching { startService(Intent(this@CyberReportActivity, com.saathi.overlay.CyberLinkOverlayService::class.java)) }
                                .onSuccess { message = "The optional helper stays for up to two minutes. Open your browser; tap Copy reporting link, then choose whether to allow copying." }
                                .onFailure { message = "Floating helper unavailable. Use Copy official link or Open official portal here." }
                        })
                        GlassButton("Help mark a visible option", primary = false, onClick = {
                            startActivity(Intent(this@CyberReportActivity, AssistantActivity::class.java))
                        })
                        Text("Marking uses Saathi's existing visible-option finder. Enable screen guidance and the floating assistant in that panel if requested. Private forms, OTPs and final submission stay under your control. Android owns its permission screens; Saathi cannot restyle them or draw over protected screens.", style = MaterialTheme.typography.bodySmall)
                        message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                        Text("Sources: National Cyber Crime Reporting Portal (FAQ and complaint checklist), RBI unauthorised-transaction guidance. Checked ${CyberReportGuide.VERIFIED}. Public financial-reporting entry checked in Chrome; private forms and submission are not verified. Portal labels may change.", style = MaterialTheme.typography.bodySmall)
                        GlassButton("Official reporting FAQ", primary = false, onClick = {
                            launchExternal(Intent(Intent.ACTION_VIEW, Uri.parse("https://cybercrime.gov.in/Webform/FAQ.aspx")))
                        })
                        GlassButton("Clear description", primary = false, onClick = { draft = ""; assessment = null; cancelAssessment(); aiAssessment = null; tts?.stop() })
                        GlassButton("Back to Saathi", primary = false, onClick = { finish() })
                    }
                    if (aiConsent) AlertDialog(onDismissRequest = { aiConsent = false },
                        containerColor = MaterialTheme.colorScheme.surface,
                        title = { Text("Share this summary for AI assessment?") },
                        text = { Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Only the summary below and your selected category will go through the configured backend to Gemini and Groq. Provider processing/retention terms apply. Remove names and identifying details. Saathi does not send screen contents, audio or evidence. This can suggest a reporting category; it cannot confirm a crime or submit a complaint.")
                            Text(draft)
                        } },
                        confirmButton = { GlassButton("Share once", compact = true, onClick = {
                            aiConsent = false
                            cancelAssessment(); aiAssessment = null; assessing = true; tts?.stop()
                            val generation = assessmentGeneration
                            val request = com.saathi.core.IncidentAssessmentRequest(locale = preferences.language.apiTag, summary = draft, concern = concern.name)
                            assessmentCall = com.saathi.gateway.PracticeGateway.assessIncident(request, true) { result ->
                                if (generation == assessmentGeneration && !isFinishing && !isDestroyed) {
                                    assessmentCall = null; assessing = false
                                    aiAssessment = when (result) {
                                        is com.saathi.core.GatewayResult.Assessed -> com.saathi.core.IncidentAssessmentPolicy.description(result.assessment)
                                        else -> "AI assessment could not be verified. Use Show reporting advice and the checklist. Do not delay calling 1930 and your bank for suspected financial fraud."
                                    }
                                }
                            }
                        }) },
                        dismissButton = { GlassButton("Keep private", compact = true, primary = false, onClick = { aiConsent = false }) })
                    if (copyConsent) AlertDialog(onDismissRequest = { copyConsent = false },
                        containerColor = MaterialTheme.colorScheme.surface,
                        title = { Text("Allow copying this link?") },
                        text = { Text("Saathi will copy only ${CyberReportGuide.PORTAL} to your clipboard. In your browser, long-press the address bar, choose Paste, check the address, then press Go. This replaces the current clipboard item. Saathi does not read your clipboard or paste into another app automatically.") },
                        confirmButton = { GlassButton("Allow copy", compact = true, onClick = {
                            copyConsent = false
                            runCatching { getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Official cybercrime portal", CyberReportGuide.PORTAL)) }
                                .onSuccess { message = "Official link copied. Paste it into your browser's address bar and press Go." }
                                .onFailure { message = "Clipboard unavailable. Use Open official portal instead." }
                        }) },
                        dismissButton = { GlassButton("Not now", compact = true, primary = false, onClick = { copyConsent = false }) })
                    if (overlayConsent) AlertDialog(onDismissRequest = { overlayConsent = false },
                        containerColor = MaterialTheme.colorScheme.surface,
                        title = { Text("Show a helper over your browser?") },
                        text = { Text("Allow Saathi to display over other apps in Android settings, return here, then tap Show floating link helper again. You can continue without this permission using the copy or open-link buttons. Protected apps may hide the helper.") },
                        confirmButton = { GlassButton("Open settings", compact = true, onClick = {
                            overlayConsent = false
                            launchExternal(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                        }) },
                        dismissButton = { GlassButton("Not now", compact = true, primary = false, onClick = { overlayConsent = false }) })
                }
            }
        }
    }
    private fun launchExternal(intent: Intent) {
        runCatching { startActivity(intent) }.onFailure { message = "No compatible app could open. Install or enable a browser, or dial 1930 using your phone." }
    }
    private fun read(text: String) {
        val engine = tts
        if (!speechReady || engine == null) { message = "Speech is unavailable. The full step remains readable."; return }
        val available = engine.setLanguage(Locale.forLanguageTag("en-IN"))
        if (available < 0) { message = "English speech is not installed. Use text or set up your device voice engine."; return }
        engine.setSpeechRate(Preferences(this).speechRate)
        if (engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "cyber-step") == TextToSpeech.ERROR)
            message = "Speech could not start. Use the written step."
    }
    override fun onResume() {
        super.onResume()
        // Suspend screen/audio work here, but keep a session explicitly started in the helper
        // available when the user returns to their browser.
        if (SaathiSession.isActive()) SaathiSession.onScreenUnavailable()
    }
    override fun onStop() {
        if (assessing) message = "AI assessment cancelled when you left. You can request it again after reviewing the summary."
        cancelAssessment(); tts?.stop(); super.onStop()
    }
    override fun onDestroy() { tts?.stop(); tts?.shutdown(); tts = null; super.onDestroy() }
}
