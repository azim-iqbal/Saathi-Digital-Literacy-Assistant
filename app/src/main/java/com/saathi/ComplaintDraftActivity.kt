package com.saathi

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.net.Uri
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saathi.cyber.*
import com.saathi.overlay.CyberLinkOverlayService
import com.saathi.ui.*
import com.saathi.ui.glass.*
import com.saathi.ui.navigation.rememberNavigationEnvironment

/** A reviewed, memory-only complaint worksheet. Private form contents are never observed. */
class ComplaintDraftActivity : ComponentActivity() {
    private var speech: android.speech.tts.TextToSpeech? = null
    private var speechReady by mutableStateOf(false)
    private var speechNotice by mutableStateOf<String?>(null)
    private fun readInstructions() {
        val engine = speech
        if (!speechReady || engine == null || engine.setLanguage(java.util.Locale.forLanguageTag("en-IN")) < 0) {
            speechNotice = "Speech is unavailable. Follow the full written instructions below."; return
        }
        engine.setSpeechRate(Preferences(this).speechRate)
        if (engine.speak("Use recent apps to return to your existing browser tab. Check the official cybercrime website address. Focus the field matching your copied draft. Long press and choose Paste. Review the full text before continuing. Saathi has not submitted your complaint.",
                android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "complaint-copy") == android.speech.tts.TextToSpeech.ERROR)
            speechNotice = "Speech could not start. Follow the written instructions."
    }
    override fun onStop() { speech?.stop(); super.onStop() }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        com.saathi.orchestrator.SaathiSession.stop()
        val initial = pendingAccount.orEmpty(); pendingAccount = null
        speech = android.speech.tts.TextToSpeech(this) { status -> if (!isDestroyed) speechReady = status == android.speech.tts.TextToSpeech.SUCCESS }
        setContent {
            val preferences = remember { Preferences(this) }
            val dark = preferences.theme == "Dark" || (preferences.theme == "System" && isSystemInDarkTheme())
            val environment = rememberNavigationEnvironment()
            var facts by remember { mutableStateOf(ComplaintFacts(account = initial)) }
            var spoken by remember { mutableStateOf(false) }
            var reviewed by remember { mutableStateOf(false) }
            var selected by remember { mutableStateOf<ComplaintField?>(null) }
            var helperConsent by remember { mutableStateOf(false) }
            var permissionPrompt by remember { mutableStateOf(false) }
            var message by remember { mutableStateOf<String?>(null) }
            val fields = facts.fields()
            fun change(value: ComplaintFacts) {
                facts = value; reviewed = false; selected = null; message = null
                stopService(Intent(this, CyberLinkOverlayService::class.java)); ComplaintHelperHandoff.clear()
            }
            MaterialTheme(colorScheme = saathiColorScheme(dark)) {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface,
                    LocalGlass provides GlassEnvironment(dark = dark, reducedMotion = preferences.reducedMotion || !environment.animationsEnabled)) {
                    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding().imePadding()
                        .wrapContentWidth(androidx.compose.ui.Alignment.CenterHorizontally).widthIn(max = 720.dp)
                        .verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SaathiBrand()
                        Text("Prepare your complaint", style = MaterialTheme.typography.headlineSmall)
                        Text("Your facts, ready to review. This worksheet arranges what you tell us; it does not invent details or decide that a crime occurred. AI reporting advice is available on the previous screen with separate sharing consent.")
                        Text("These details stay on this device in memory. They clear if this screen is recreated or closed. Keep passwords, OTPs, PINs, full bank numbers and identity documents out; enter required identifying details privately on the official portal.", style = MaterialTheme.typography.bodySmall)
                        DraftInput("What happened, in order?", facts.account, 4000) { change(facts.copy(account = it)) }
                        DraftInput("Date and time, if known", facts.occurred, 200) { change(facts.copy(occurred = it)) }
                        DraftInput("How were you contacted?", facts.contact, 400) { change(facts.copy(contact = it)) }
                        DraftInput("Loss or other impact, if known", facts.impact, 400) { change(facts.copy(impact = it)) }
                        DraftInput("Steps already taken", facts.response, 400) { change(facts.copy(response = it)) }
                        DraftInput("Evidence kept (describe, do not upload)", facts.evidence, 400) { change(facts.copy(evidence = it)) }
                        if (facts.missing().isNotEmpty()) Text("Still to consider (optional):\n" + facts.missing().joinToString("\n") + "\nLeave unknown details blank. The draft will omit them.")
                        Text("Review before copying", style = MaterialTheme.typography.titleMedium)
                        fields.firstOrNull()?.let { field -> GlassPanel { Text(field.text, Modifier.padding(20.dp)) } }
                        FilterChip(selected = reviewed, enabled = fields.isNotEmpty(), onClick = { reviewed = !reviewed },
                            label = { Text("I checked this matches what happened") })
                        Text("Portal labels and limits can change. Match each draft to the field you can see. Saathi does not choose a private field, paste automatically, accept declarations or submit a report.")
                        fields.forEach { field ->
                            GlassButton("Copy ${field.title.lowercase()}", enabled = reviewed, primary = false, onClick = { selected = field })
                        }
                        GlassButton("Show floating draft helper", enabled = reviewed, onClick = {
                            if (Settings.canDrawOverlays(this@ComplaintDraftActivity)) helperConsent = true else permissionPrompt = true
                        })
                        Text("To use an already-open form: open Android's recent apps, select the browser tab with your complaint, check https://cybercrime.gov.in/, then focus the matching field. Long-press and choose Paste after approving a copy. Do not open the portal again if your form is already open. If the helper is hidden, return to this worksheet through recent apps.")
                        Text("Copy guidance")
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            FilterChip(selected = !spoken, onClick = { spoken = false; speech?.stop() }, label = { Text("Text only") })
                            FilterChip(selected = spoken, onClick = { spoken = true }, label = { Text("Text + voice") })
                        }
                        if (spoken) GlassButton("Read copy instructions", primary = false, onClick = { readInstructions() })
                        speechNotice?.let { Text(it) }
                        message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                        GlassButton("Clear worksheet", primary = false, onClick = { change(ComplaintFacts()) })
                        GlassButton("Back to reporting", primary = false, onClick = { finish() })
                    }
                    selected?.let { field -> AlertDialog(onDismissRequest = { selected = null }, containerColor = MaterialTheme.colorScheme.surface,
                        title = { Text("Allow copy: ${field.title}?") },
                        text = { Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(field.instruction); Text(field.text)
                            Text("Only the full text shown above will replace your clipboard. Saathi does not read the clipboard. The copied text may remain after this screen closes; clear it yourself when finished.")
                        } },
                        confirmButton = { GlassButton("Allow copy", compact = true, onClick = {
                            selected = null
                            message = if (ComplaintClipboard.copy(this@ComplaintDraftActivity, field))
                                "Copied ${field.title.lowercase()}. Use recent apps to return to your existing browser tab, focus the matching field, long-press and Paste. Review the pasted text; the portal may impose a limit."
                            else "Copy unavailable. Keep this worksheet open and enter the matching details manually."
                        }) },
                        dismissButton = { GlassButton("Not now", compact = true, primary = false, onClick = { selected = null }) }) }
                    if (helperConsent) AlertDialog(onDismissRequest = { helperConsent = false }, containerColor = MaterialTheme.colorScheme.surface,
                        title = { Text("Keep these drafts beside your browser?") },
                        text = { Text("The floating helper holds your reviewed fields in memory for up to two minutes. Select a field and approve its exact text each time you copy. Nothing is pasted or submitted automatically. Locking the phone, closing the helper or losing permission ends it. Protected apps may hide it; use this worksheet instead.") },
                        confirmButton = { GlassButton("Show helper", compact = true, onClick = {
                            helperConsent = false; ComplaintHelperHandoff.prepare(fields)
                            runCatching { startService(Intent(this@ComplaintDraftActivity, CyberLinkOverlayService::class.java).setAction(CyberLinkOverlayService.DRAFT_ACTION)) }
                                .onSuccess { message = "Helper ready. Use recent apps to return to your existing browser. Each field still needs Allow copy." }
                                .onFailure { ComplaintHelperHandoff.clear(); message = "Helper unavailable. Use the copy buttons here, then return through recent apps." }
                        }) }, dismissButton = { GlassButton("Not now", compact = true, primary = false, onClick = { helperConsent = false }) })
                    if (permissionPrompt) AlertDialog(onDismissRequest = { permissionPrompt = false }, containerColor = MaterialTheme.colorScheme.surface,
                        title = { Text("Allow a floating draft helper?") }, text = { Text("Enable display over other apps in Android settings, return here, then choose Show floating draft helper again. Copying from this worksheet works without that permission.") },
                        confirmButton = { GlassButton("Open settings", compact = true, onClick = {
                            permissionPrompt = false
                            runCatching { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) }
                                .onFailure { message = "Settings unavailable. You can still copy from this worksheet." }
                        }) }, dismissButton = { GlassButton("Not now", compact = true, primary = false, onClick = { permissionPrompt = false }) })
                }
            }
        }
    }
    override fun onDestroy() {
        if (isFinishing || isChangingConfigurations) {
            stopService(Intent(this, CyberLinkOverlayService::class.java)); ComplaintHelperHandoff.clear()
        }
        speech?.stop(); speech?.shutdown(); speech = null
        super.onDestroy()
    }
    companion object {
        private var pendingAccount: String? = null
        fun open(context: Context, account: String) {
            pendingAccount = account.take(1200)
            try { context.startActivity(Intent(context, ComplaintDraftActivity::class.java)) }
            catch (error: Exception) { pendingAccount = null; throw error }
        }
    }
}

@Composable private fun DraftInput(label: String, value: String, limit: Int, onChange: (String) -> Unit) {
    OutlinedTextField(value, { onChange(it.take(limit)) }, label = { Text(label) }, modifier = Modifier.fillMaxWidth(),
        minLines = 2, shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        supportingText = { Text("${value.length}/$limit · worksheet limit") })
}
