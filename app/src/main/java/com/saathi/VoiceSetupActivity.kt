package com.saathi

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.*
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
import com.saathi.orchestrator.SaathiSession
import com.saathi.speech.VoiceConversationService
import com.saathi.ui.*
import com.saathi.ui.glass.*
import com.saathi.ui.navigation.rememberNavigationEnvironment

/** User-initiated model setup. No microphone or guidance is started by checking/downloading. */
class VoiceSetupActivity : ComponentActivity() {
    private var engine: SpeechRecognizer? = null
    private var generation = 0
    private var message by mutableStateOf("Check whether your selected voice language is installed for offline replies.")
    private var busy by mutableStateOf(false)
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private fun request() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        .putExtra(RecognizerIntent.EXTRA_LANGUAGE, Preferences(this).language.sttTag)
        .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
    private fun begin(): Pair<SpeechRecognizer, Int>? {
        generation++; engine?.destroy(); engine = null; handler.removeCallbacksAndMessages(null)
        if (Build.VERSION.SDK_INT < 33 || !VoiceConversationService.supported(this)) {
            message = "This device cannot set up on-device replies here. Use text guidance or check your system voice settings."
            busy = false; return null
        }
        return try {
            val recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(this)
            engine = recognizer; busy = true
            val token = generation
            handler.postDelayed({ if (token == generation) { busy = false; message = "The speech service has not replied. You can check again later; text guidance is available." } }, 20_000)
            recognizer to token
        } catch (_: RuntimeException) { busy = false; message = "The speech service is unavailable. Text guidance is still available."; null }
    }
    private fun report(token: Int, text: String) {
        if (token != generation || isDestroyed) return
        handler.removeCallbacksAndMessages(null); busy = false; message = text
    }
    private fun check() {
        if (Build.VERSION.SDK_INT < 33) { begin(); return }
        checkSupportedLanguage()
    }
    @androidx.annotation.RequiresApi(33)
    private fun checkSupportedLanguage() {
        val (recognizer, token) = begin() ?: return
        message = "Checking offline voice…"
        runCatching { recognizer.checkRecognitionSupport(request(), mainExecutor, object : RecognitionSupportCallback {
            override fun onSupportResult(support: RecognitionSupport) {
                val wanted = Preferences(this@VoiceSetupActivity).language.sttTag
                val installed = support.installedOnDeviceLanguages.any { it.equals(wanted, ignoreCase = true) }
                report(token, if (installed) "Your selected language is installed. Return to Saathi and explicitly enable hands-free replies."
                    else "Your selected language is not installed for offline replies. You can ask your device’s speech service to download it below.")
            }
            override fun onError(error: Int) { report(token, "The speech service could not check this language. Use text guidance or your system voice settings.") }
        }) }.onFailure { report(token, "The speech service could not check this language. Text guidance remains available.") }
    }
    private fun download() {
        if (Build.VERSION.SDK_INT < 33) { begin(); return }
        val (recognizer, token) = begin() ?: return
        message = "Requesting the offline language from your speech service…"
        runCatching {
            if (Build.VERSION.SDK_INT >= 34) recognizer.triggerModelDownload(request(), mainExecutor, object : ModelDownloadListener {
                override fun onProgress(completedPercent: Int) { if (token == generation) message = "Language download: ${completedPercent.coerceIn(0, 100)}%" }
                override fun onSuccess() { report(token, "Language download finished. Check availability, then enable hands-free replies when ready.") }
                override fun onScheduled() { report(token, "Your speech service queued the download. Check availability later; text guidance is available now.") }
                override fun onError(error: Int) { report(token, "Your speech service could not download this language. Check its settings or use text guidance.") }
            }) else {
                recognizer.triggerModelDownload(request())
                report(token, "Download requested. Your speech service manages installation; check availability later.")
            }
        }.onFailure { report(token, "Language setup could not start. Text guidance remains available.") }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val preferences = remember { Preferences(this) }
            val dark = preferences.theme == "Dark" || (preferences.theme == "System" && isSystemInDarkTheme())
            val colors = saathiColorScheme(dark)
            val environment = rememberNavigationEnvironment()
            MaterialTheme(colorScheme = colors) {
                CompositionLocalProvider(LocalContentColor provides colors.onBackground,
                    LocalGlass provides GlassEnvironment(dark = dark, reducedMotion = preferences.reducedMotion || !environment.animationsEnabled)) {
                    Column(Modifier.fillMaxSize().background(colors.background).safeDrawingPadding()
                        .wrapContentWidth(androidx.compose.ui.Alignment.CenterHorizontally).widthIn(max = 720.dp)
                        .verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        SaathiBrand()
                        Text("Offline voice setup", style = MaterialTheme.typography.headlineMedium)
                        Text("Selected language: ${preferences.language.storageValue}")
                        GlassPanel(Modifier.fillMaxWidth()) { Text(message, Modifier.padding(20.dp)) }
                        GlassButton("Check availability", enabled = !busy, onClick = { check() })
                        Text("Downloading uses your device’s speech service and internet connection. The service controls availability and may continue downloading after you leave. No microphone is opened here.")
                        GlassButton("Download selected language", primary = false, enabled = !busy && Build.VERSION.SDK_INT >= 33, onClick = { download() })
                        GlassButton("System voice settings", primary = false, onClick = {
                            runCatching { startActivity(Intent(android.provider.Settings.ACTION_VOICE_INPUT_SETTINGS)) }
                                .onFailure { message = "Voice settings are unavailable on this device. You can continue with text guidance." }
                        })
                        GlassButton("Back to Saathi", primary = false, onClick = { finish() })
                    }
                }
            }
        }
    }
    override fun onResume() { super.onResume(); if (SaathiSession.isActive()) SaathiSession.onScreenUnavailable() }
    override fun onDestroy() { generation++; handler.removeCallbacksAndMessages(null); engine?.destroy(); super.onDestroy() }
}
