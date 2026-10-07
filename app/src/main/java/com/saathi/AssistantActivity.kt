package com.saathi

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saathi.orchestrator.LiveGuide
import com.saathi.orchestrator.FormGuide
import com.saathi.orchestrator.SaathiSession
import com.saathi.overlay.AssistantBubbleService
import com.saathi.speech.VoiceConversationService
import com.saathi.ui.saathiColorScheme
import com.saathi.ui.SaathiBrand
import com.saathi.ui.navigation.rememberNavigationEnvironment
import com.saathi.ui.Preferences
import com.saathi.ui.glass.GlassButton
import com.saathi.ui.glass.GlassPanel
import com.saathi.ui.glass.GlassEnvironment
import com.saathi.ui.glass.LocalGlass

/** User-owned task intake. No conversation or transcript is saved to disk. */
class AssistantActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val preferences = remember { Preferences(this) }
            var request by remember { mutableStateOf(if (SaathiSession.isLive()) SaathiSession.currentRequest() else "") }
            var spoken by remember { mutableStateOf(SaathiSession.prefersSpokenGuidance()) }
            var error by remember { mutableStateOf<String?>(null) }
            var readiness by remember { mutableIntStateOf(0) }
            var pendingSession by remember { mutableStateOf<String?>(null) }
            val aiConfigured = com.saathi.gateway.PracticeGateway.aiEnabled()
            val localFormHelp = FormGuide.isRequest(request)
            val state by SaathiSession.status.collectAsState()
            val instruction by SaathiSession.instruction.collectAsState()
            val lastReply by SaathiSession.lastReply.collectAsState()
            val voicePhase by VoiceConversationService.phase.collectAsState()
            val voice = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) request = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty().take(80)
            }
            val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
                val expected = pendingSession; pendingSession = null
                if (expected != null && expected == SaathiSession.sessionKey() && results.values.all { it }) {
                    runCatching { SaathiSession.startConversation(this); finish() }.onFailure { error = "Voice could not start. Text guidance remains available." }
                } else error = "Microphone permission is needed for replies. Text guidance remains available."
            }
            val settings = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { readiness++ }
            val overlay = remember(readiness) { Settings.canDrawOverlays(this) }
            val accessibility = remember(readiness) {
                Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
                    .orEmpty().split(':').any { android.content.ComponentName.unflattenFromString(it)?.packageName == packageName }
            }
            val dark = preferences.theme == "Dark" || (preferences.theme == "System" && isSystemInDarkTheme())
            val colors = saathiColorScheme(dark)
            val environment = rememberNavigationEnvironment()
            MaterialTheme(colorScheme = colors) {
                CompositionLocalProvider(LocalContentColor provides colors.onSurface,
                    LocalGlass provides GlassEnvironment(dark = dark, reducedMotion = preferences.reducedMotion || !environment.animationsEnabled)) {
                Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding()
                    .wrapContentWidth(androidx.compose.ui.Alignment.CenterHorizontally).widthIn(max = 720.dp)
                    .imePadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    SaathiBrand()
                    GlassButton("Report cyber fraud", primary = false, onClick = { startActivity(Intent(this@AssistantActivity, CyberReportActivity::class.java)) })
                    val issueResearch = com.saathi.core.ScreenErrorPolicy.isNotice(lastReply)
                    GlassButton(if (issueResearch) "Research this issue" else "Research requirements", primary = false,
                        onClick = { ResearchActivity.open(this@AssistantActivity, if (issueResearch) "" else request, if (issueResearch) "outage" else null) })
                    Text("What would you like to do?", style = MaterialTheme.typography.titleLarge)
                    Text(if (aiConfigured) "Describe your navigation task. Form-filling help stays on this device and can mark safe visible fields. Other unresolved navigation tasks may use the configured AI route for one grounded next step. You perform every action." else "For apps and browsers, tell me a visible option’s name—such as Settings, Help or Search. For form filling, I can mark safe visible fields locally. Configure the backend to enable AI navigation for other tasks.")
                    GlassPanel(Modifier.fillMaxWidth()) {
                        OutlinedTextField(value = request, onValueChange = { request = it.take(if (aiConfigured) 160 else 80); error = null },
                            label = { Text("Your request") }, placeholder = { Text("Find Settings") },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                            modifier = Modifier.fillMaxWidth().padding(12.dp))
                    }
                    GlassButton("Speak your request", primary = false, onClick = {
                        runCatching { voice.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, preferences.language.sttTag)
                            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)) }
                            .onFailure { error = "Voice input is unavailable. You can type instead." }
                    })
                    Text("Voice input uses your installed speech service. Review the text before starting.", style = MaterialTheme.typography.bodySmall)
                    Text("How should I guide you?", style = MaterialTheme.typography.titleMedium)
                    if (spoken) {
                        Text(voicePhase.text(preferences.language), style = MaterialTheme.typography.bodySmall)
                        GlassButton("Offline voice setup", primary = false, onClick = { startActivity(Intent(this@AssistantActivity, VoiceSetupActivity::class.java)) })
                    }
                    val chipColors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.primaryContainer, selectedLabelColor = colors.onPrimaryContainer)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilterChip(colors = chipColors, selected = !spoken, onClick = { spoken = false; SaathiSession.setSpokenGuidance(false) }, label = { Text("Text only") })
                        FilterChip(colors = chipColors, selected = spoken, onClick = { spoken = true; SaathiSession.setSpokenGuidance(true) }, label = { Text("Text + voice") })
                    }
                    if (!accessibility) GlassButton("Enable screen guidance", primary = false, onClick = { settings.launch(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) })
                    if (!overlay) GlassButton("Enable floating assistant", primary = false, onClick = { settings.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) })
                    Text(if (aiConfigured) "AI navigation is enabled. Form-filling help and exact matches stay on this device. Other tasks may send your request, app identity, eligible labels and recent suggestions through your server to its primary model, with fallback only when needed. Editable values are excluded; detected private fields get no marker. Filtering is not perfect." else "Starting lets Saathi read accessible controls in other apps locally while this session is active. Nothing is sent to an AI provider. Form help marks only safe visible fields; detected private fields and unclear controls get no target. You perform every tap.", style = MaterialTheme.typography.bodySmall)
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    if (SaathiSession.canResume()) {
                        GlassButton("Resume guidance", enabled = overlay && accessibility, onClick = {
                            if (SaathiSession.resume(this@AssistantActivity)) finish()
                            else error = "Could not resume. Check screen guidance and overlay permissions, then try again."
                        })
                        GlassButton("Discard paused task", primary = false, onClick = { SaathiSession.stop(); request = "" })
                    }
                    GlassButton(if (SaathiSession.isActive() && SaathiSession.isLive()) "Update on-screen help" else "Start on-screen help", enabled = overlay && accessibility && request.isNotBlank(), onClick = {
                        if (com.saathi.core.ResearchIntent.claimType(request) != null) ResearchActivity.open(this@AssistantActivity, request)
                        else if (!SaathiSession.acceptsLiveRequest(request)) error = "Name a visible navigation option. Payments, deletion, permissions and secrets must be handled yourself."
                        else runCatching {
                            if (SaathiSession.isActive() && SaathiSession.isLive()) SaathiSession.changeLiveRequest(request, SaathiSession.sessionKey())
                            else SaathiSession.startLive(this@AssistantActivity, request, preferences.language, spoken)
                        }
                            .onSuccess { if (it) finish() else error = "Could not start. Check screen guidance and overlay permissions." }
                            .onFailure { SaathiSession.stop(); error = "Could not start. Please try again." }
                    })
                    if (SaathiSession.isActive()) {
                        GlassPanel(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp)) {
                            Text("Saathi’s guidance", style = MaterialTheme.typography.titleMedium)
                            Text(lastReply.ifBlank { instruction.ifBlank { "Return to the app you want help with." } })
                            Text("Last response · I’ll recheck the screen when you return.", style = MaterialTheme.typography.bodySmall)
                        } }
                        if (spoken && SaathiSession.isLive() && localFormHelp) Text("Hands-free listening is paused during form help. Saathi marks safe fields locally; you enter each value yourself.", style = MaterialTheme.typography.bodySmall)
                        if (spoken && SaathiSession.isLive() && !localFormHelp) Text("With hands-free replies enabled, say find Help to change the visible option. Repeat, pause and stop are also available.", style = MaterialTheme.typography.bodySmall)
                        if (spoken && !localFormHelp && VoiceConversationService.supported(this@AssistantActivity)) GlassButton("Enable hands-free replies", primary = false, onClick = {
                            pendingSession = SaathiSession.sessionKey()
                            val required = mutableListOf(Manifest.permission.RECORD_AUDIO)
                            if (Build.VERSION.SDK_INT >= 33) required += Manifest.permission.POST_NOTIFICATIONS
                            permissions.launch(required.toTypedArray())
                        })
                        GlassButton("Stop guidance", primary = false, onClick = { SaathiSession.stop() })
                        GlassButton("Pause guidance", primary = false, onClick = { SaathiSession.pause() })
                    }
                    Text(when (state) {
                        com.saathi.core.GuidanceSessionState.STOPPED -> "Guidance is off"
                        com.saathi.core.GuidanceSessionState.PAUSED -> "Guidance is paused"
                        com.saathi.core.GuidanceSessionState.SENSITIVE_HANDOVER -> "Waiting for you to finish privately · guidance resumes on a clear screen"
                        com.saathi.core.GuidanceSessionState.WAITING_FOR_CAPTCHA -> "Waiting for you to complete the CAPTCHA · guidance resumes afterward"
                        com.saathi.core.GuidanceSessionState.COMPLETED -> "Guidance is complete"
                        com.saathi.core.GuidanceSessionState.ERROR -> "Guidance could not continue. Please try again."
                        else -> "Guidance is active · open your app to continue"
                    }, style = MaterialTheme.typography.bodySmall)
                    GlassButton("Return to my screen", primary = false, onClick = { finish() })
                }
                }
            }
        }
    }
    override fun onResume() {
        super.onResume()
        AssistantBubbleService.panelVisible(true)
        if (SaathiSession.isActive()) SaathiSession.onScreenUnavailable()
    }
    override fun onPause() { AssistantBubbleService.panelVisible(false); super.onPause() }
}
