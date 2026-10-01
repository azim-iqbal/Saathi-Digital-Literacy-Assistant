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

/** Visible opt-in test setup; neither the token nor enabled state survives process death. */
class GatewaySetupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        setContent {
            val preferences = remember { Preferences(this) }
            val dark = preferences.theme == "Dark" || (preferences.theme == "System" && isSystemInDarkTheme())
            val colors = saathiColorScheme(dark)
            val environment = rememberNavigationEnvironment()
            var token by remember { mutableStateOf("") }
            var enabled by remember { mutableStateOf(PracticeGateway.enabled() || PracticeGateway.aiEnabled()) }
            var ai by remember { mutableStateOf(PracticeGateway.aiEnabled()) }
            var error by remember { mutableStateOf(false) }
            MaterialTheme(colorScheme = colors) {
                CompositionLocalProvider(LocalContentColor provides colors.onBackground,
                    LocalGlass provides GlassEnvironment(dark = dark, reducedMotion = preferences.reducedMotion || !environment.animationsEnabled)) {
                    Column(Modifier.fillMaxSize().background(colors.background).safeDrawingPadding().imePadding()
                        .wrapContentWidth(androidx.compose.ui.Alignment.CenterHorizontally).widthIn(max = 720.dp)
                        .verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        SaathiBrand()
                        Text("Backend connection", style = MaterialTheme.typography.headlineMedium)
                        Text("Connect through the server on your computer. Practice testing uses simulated providers. AI navigation needs both configured model accounts.")
                        FilterChip(selected = ai, onClick = { ai = !ai }, label = { Text("Use AI navigation in other apps") })
                        if (ai) Text("By enabling AI navigation, you allow your task, app identity, eligible visible control labels and up to three previous target labels to be sent through your server to Gemini and Groq. Private forms and text-entry values are excluded, but filtering is not perfect. Provider data terms apply. No screenshots, audio or coordinates are sent. Use synthetic or non-private tasks while testing.")
                        GlassPanel(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp)) {
                            Text(if (enabled) "Enabled for this app process" else "Off", style = MaterialTheme.typography.titleMedium)
                            Text(if (ai) "AI replies require agreement on an observed control. You perform every tap. Model agreement is not a safety guarantee." else "Only public practice IDs and a task category are sent. Help in other apps stays local.")
                        } }
                        Text("Follow Local setup in the project documentation to start the server and connect it over USB. Enter its temporary development token below.")
                        OutlinedTextField(token, { token = it.take(256); error = false }, label = { Text("Temporary development token") },
                            visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth(),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                        if (error) Text("Use the server’s token, between 32 and 256 characters with no spaces.", color = colors.error)
                        GlassButton(if (ai) "Enable AI navigation" else "Enable local test", enabled = token.isNotBlank(), onClick = {
                            if (PracticeGateway.configure(token, ai)) { token = ""; enabled = true } else error = true
                        })
                        Text("Enabling or disabling ends current guidance. Start a new guidance session to test. This setting and token are held only in memory.", style = MaterialTheme.typography.bodySmall)
                        GlassButton("Disable and forget token", primary = false, enabled = enabled, onClick = {
                            PracticeGateway.disable(); enabled = false; token = ""
                        })
                        GlassButton("Back to Saathi", primary = false, onClick = { finish() })
                    }
                }
            }
        }
    }
    override fun onResume() { super.onResume(); if (com.saathi.orchestrator.SaathiSession.isActive()) com.saathi.orchestrator.SaathiSession.onScreenUnavailable() }
}
