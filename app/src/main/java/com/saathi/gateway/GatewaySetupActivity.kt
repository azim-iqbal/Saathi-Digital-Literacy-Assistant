package com.saathi.gateway

import android.os.Bundle
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.saathi.ui.*
import com.saathi.ui.glass.*
import com.saathi.ui.navigation.rememberNavigationEnvironment

/** Visible opt-in setup; neither the access token nor enabled state survives process death. */
class GatewaySetupActivity : ComponentActivity() {
    private var checking by mutableStateOf(false)
    private var report by mutableStateOf<String?>(null)
    private var check: com.saathi.core.GatewayCancellation? = null
    private fun cancelCheck() { check?.cancel(); check = null; checking = false }
    private fun runCheck(providers: Boolean) {
        cancelCheck(); checking = true; report = null
        val callback: (com.saathi.core.GatewayResult) -> Unit = { result ->
            checking = false; check = null
            report = if (result is com.saathi.core.GatewayResult.Connection) result.report
                else com.saathi.core.GatewayRecovery.message((result as? com.saathi.core.GatewayResult.Rejected)?.reason ?: "invalid_response", com.saathi.language.GuidanceLanguage.ENGLISH)
        }
        check = if (providers) PracticeGateway.checkProviders(true, callback) else PracticeGateway.connectionStatus(callback)
    }
    override fun onStop() {
        if (checking) report = "Check cancelled when you left. Refresh server status on return; calls already sent may still count."
        cancelCheck(); super.onStop()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        setContent {
            val preferences = remember { Preferences(this) }
            val dark = preferences.theme == "Dark" || (preferences.theme == "System" && isSystemInDarkTheme())
            val colors = saathiColorScheme(dark)
            val environment = rememberNavigationEnvironment()
            val development = com.saathi.BuildConfig.DEBUG
            var token by remember { mutableStateOf("") }
            var enabled by remember { mutableStateOf(PracticeGateway.enabled() || PracticeGateway.aiEnabled()) }
            var ai by remember { mutableStateOf(if (development) true else PracticeGateway.aiEnabled()) }
            var probeConsent by remember { mutableStateOf(false) }
            var error by remember { mutableStateOf(false) }
            MaterialTheme(colorScheme = colors) {
                CompositionLocalProvider(LocalContentColor provides colors.onBackground,
                    LocalGlass provides GlassEnvironment(dark = dark, reducedMotion = preferences.reducedMotion || !environment.animationsEnabled)) {
                    Column(Modifier.fillMaxSize().background(colors.background).safeDrawingPadding().imePadding()
                        .wrapContentWidth(androidx.compose.ui.Alignment.CenterHorizontally).widthIn(max = 720.dp)
                        .verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        SaathiBrand()
                        Text("Backend connection", style = MaterialTheme.typography.headlineMedium)
                        Text(if (development) "Connect through the server on your computer. Practice testing uses simulated providers. AI navigation uses local matching first, then a primary model; the second model is used when needed."
                            else "Connect to the Saathi server supplied with this app using your access token. The server operator configures the primary and fallback models.")
                        Text(PracticeGateway.serverLabel(), style = MaterialTheme.typography.bodySmall)
                        if (development) {
                            Text("Note: On an Android emulator, connections fall back to 10.0.2.2 automatically. On a physical USB phone, execute 'adb reverse tcp:8765 tcp:8765' in your computer terminal.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                        FilterChip(selected = ai, onClick = { ai = !ai }, enabled = PracticeGateway.available(), label = { Text("Use AI navigation in other apps") })
                        if (ai) Text("By enabling AI navigation, you allow your task, app identity, eligible visible control labels and up to three previous target labels to be sent through your server to the primary model and, when needed, the fallback model. Private forms and text-entry values are excluded, but filtering is not perfect. Provider data terms apply. No screenshots, audio or coordinates are sent. Use synthetic or non-private tasks while testing.")
                        GlassPanel(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp)) {
                            Text(if (enabled) "Enabled for this app process" else "Off", style = MaterialTheme.typography.titleMedium)
                            Text(if (ai) "Local matches stay on this device. Cloud replies must name an observed control. You perform every tap; a model answer is not a safety guarantee." else "Only public practice IDs and a task category are sent. Help in other apps stays local.")
                        } }
                        Text(if (!PracticeGateway.available()) "This build has no configured HTTPS server. Local practice and on-screen option finding are available. Contact the app provider for a connected build."
                            else if (development) "Follow Local setup in the project documentation to start the server and connect it over USB. Enter its temporary development token below."
                            else "Enter your personal server access token. Do not enter Gemini or Groq API keys here. Your token stays in memory and is forgotten when this app process closes.")
                        OutlinedTextField(token, { token = it.take(256); error = false }, label = { Text(if (development) "Temporary development token" else "Server access token") },
                            visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth(),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                        if (error) Text("Use the server’s token, between 32 and 256 characters with no spaces.", color = colors.error)
                        GlassButton(if (ai) "Enable AI navigation" else if (development) "Enable local test" else "Choose AI navigation above",
                            enabled = PracticeGateway.available() && token.isNotBlank() && (development || ai), onClick = {
                            if (PracticeGateway.configure(token, ai)) { cancelCheck(); report = null; token = ""; enabled = true } else error = true
                        })
                        Text("API verification", style = MaterialTheme.typography.titleMedium)
                        Text("Enabled is not proof of a connection. Refresh status checks your server without contacting a model. Check APIs sends one synthetic request to each provider.")
                        GlassButton("Refresh server status", primary = false, enabled = enabled && !checking, onClick = { runCheck(false) })
                        GlassButton("Check APIs", enabled = enabled && !checking, onClick = { probeConsent = true })
                        if (checking) {
                            Text("Checking connection…")
                            GlassButton("Cancel check", primary = false, onClick = { cancelCheck(); report = "Cancelled. Refresh status before retrying; a call already sent may still count." })
                        }
                        report?.split("\n\n")?.forEach { value -> GlassPanel { Text(value, Modifier.padding(20.dp)) } }
                        Text("Enabling or disabling ends current guidance. Start a new guidance session to test. This setting and token are held only in memory.", style = MaterialTheme.typography.bodySmall)
                        GlassButton("Disable and forget token", primary = false, enabled = enabled, onClick = {
                            cancelCheck(); report = null; PracticeGateway.disable(); enabled = false; token = ""
                        })
                        GlassButton("Back to Saathi", primary = false, onClick = { finish() })
                    }
                    if (probeConsent) AlertDialog(onDismissRequest = { probeConsent = false }, containerColor = colors.surface,
                        title = { Text("Check both APIs now?") },
                        text = { Text("This sends one small synthetic practice request to Gemini and one to Groq through your server. It uses your existing provider quota and may incur charges under your account plan. No incident, screen content or audio is sent. There are no automatic retries. Simulated mode never calls either API.") },
                        confirmButton = { GlassButton("Run check", compact = true, onClick = { probeConsent = false; runCheck(true) }) },
                        dismissButton = { GlassButton("Not now", primary = false, compact = true, onClick = { probeConsent = false }) })
                }
            }
        }
    }
    override fun onResume() { super.onResume(); if (com.saathi.orchestrator.SaathiSession.isActive()) com.saathi.orchestrator.SaathiSession.onScreenUnavailable() }
}
