package com.saathi.ui

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.speech.RecognizerIntent
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Brush
import com.saathi.ui.glass.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.core.view.WindowCompat
import com.saathi.ui.navigation.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.saathi.DemoBillPayActivity
import com.saathi.accessibility.SaathiAccessibilityService
import com.saathi.core.GuidanceSessionState
import com.saathi.guardrails.GuardrailDecision
import com.saathi.language.GuidanceLanguage
import com.saathi.orchestrator.SaathiSession
import com.saathi.speech.TtsManager
import com.saathi.speech.VoiceConversationService
import com.saathi.speech.VoicePhase

private enum class Screen { Welcome, Home, Practice, Intake, Setup, Session, Settings, Privacy }

private fun GuidanceSessionState.labelCopy() = when (this) {
    GuidanceSessionState.PREPARING -> Copy.PREPARING
    GuidanceSessionState.OBSERVING -> Copy.OBSERVING
    GuidanceSessionState.ANALYSING -> Copy.ANALYSING
    GuidanceSessionState.GUIDING -> Copy.ACTIVE
    GuidanceSessionState.WAITING_FOR_PRACTICE -> Copy.WAITING
    GuidanceSessionState.SENSITIVE_HANDOVER -> Copy.SENSITIVE
    GuidanceSessionState.PAUSED -> Copy.PAUSED
    GuidanceSessionState.COMPLETED -> Copy.COMPLETED
    GuidanceSessionState.ERROR -> Copy.START_ERROR
    GuidanceSessionState.STOPPED -> Copy.STOPPED
}

private fun GuidanceSessionState.bodyCopy() = when (this) {
    GuidanceSessionState.WAITING_FOR_PRACTICE -> Copy.WAITING_BODY
    GuidanceSessionState.SENSITIVE_HANDOVER -> Copy.SENSITIVE_BODY
    GuidanceSessionState.COMPLETED -> Copy.COMPLETED_BODY
    else -> Copy.SESSION_BODY
}

private fun GuidanceSessionState.isRunning() = this in setOf(
    GuidanceSessionState.PREPARING,
    GuidanceSessionState.OBSERVING,
    GuidanceSessionState.ANALYSING,
    GuidanceSessionState.GUIDING,
    GuidanceSessionState.WAITING_FOR_PRACTICE,
    GuidanceSessionState.SENSITIVE_HANDOVER
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SaathiApp() {
    val context = LocalContext.current
    val preferences = remember { Preferences(context) }
    var language by remember { mutableStateOf(preferences.language) }
    var theme by remember { mutableStateOf(preferences.theme) }
    var speech by remember { mutableStateOf(preferences.speech) }
    var speechRate by remember { mutableFloatStateOf(preferences.speechRate) }
    var reduceMotion by remember { mutableStateOf(preferences.reducedMotion) }
    var reduceTransparency by remember { mutableStateOf(preferences.reducedTransparency) }
    var practiceCategory by rememberSaveable { mutableIntStateOf(0) }
    val navigationEnvironment = rememberNavigationEnvironment()
    val stillMotion = reduceMotion || !navigationEnvironment.animationsEnabled
    val useBlur = navigationEnvironment.blurSupported && !reduceTransparency
    var haptics by remember { mutableStateOf(preferences.haptics) }
    var screen by rememberSaveable { mutableStateOf(if (preferences.welcomed) Screen.Home else Screen.Welcome) }
    var task by remember { mutableStateOf("") } // Deliberately not saved after process death.
    var selectedGoal by rememberSaveable { mutableStateOf("Pay my electricity bill") }
    var error by remember { mutableStateOf<Copy?>(null) }
    var overlay by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    fun canReadScreen(): Boolean {
        val component = ComponentName(context, SaathiAccessibilityService::class.java).flattenToString()
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            .orEmpty().split(':').any { it.equals(component, true) }
    }
    var accessibility by remember { mutableStateOf(canReadScreen()) }
    val lifecycle = LocalLifecycleOwner.current
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                overlay = Settings.canDrawOverlays(context)
                accessibility = canReadScreen()
            }
        }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer) }
    }
    val status by SaathiSession.status.collectAsState()
    val voicePhase by VoiceConversationService.phase.collectAsState()
    var pendingVoiceSession by remember { mutableStateOf<String?>(null) }
    val dark = theme == "Dark" || (theme == "System" && isSystemInDarkTheme())
    val colors = saathiColorScheme(dark)
    val view = LocalView.current
    fun navigate(next: Screen) {
        error = null
        if (haptics) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        screen = next
    }
    fun tr(id: Copy) = id.text(language)
    fun openPractice() { context.startActivity(Intent(context, DemoBillPayActivity::class.java)) }
    fun start() {
        if (!Settings.canDrawOverlays(context) || !canReadScreen()) { navigate(Screen.Setup); return }
        runCatching { SaathiSession.start(context, selectedGoal, language, speech, speechRate) }
            .onSuccess { result ->
                if (result.decision == GuardrailDecision.ALLOW && SaathiSession.isActive()) navigate(Screen.Session)
                else error = Copy.START_ERROR
            }
            .onFailure { SaathiSession.stop(); error = Copy.START_ERROR }
    }
    fun launchConversation() {
        if (!lifecycle.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return
        runCatching { SaathiSession.startConversation(context); openPractice() }
            .onFailure { VoiceConversationService.stop(context); error = Copy.VOICE_ERROR }
    }
    val voicePermissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        val expected = pendingVoiceSession
        pendingVoiceSession = null
        if (expected != null && expected == SaathiSession.sessionKey()) {
            val granted = context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED &&
                (android.os.Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED)
            if (granted) launchConversation() else error = Copy.VOICE_PERMISSION
        }
    }
    val recognize = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            task = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
        }
    }
    var clearDialog by remember { mutableStateOf(false) }
    BackHandler(screen != Screen.Home && screen != Screen.Welcome) {
        navigate(if (screen == Screen.Privacy) Screen.Settings else Screen.Home)
    }
    MaterialTheme(colorScheme = colors, typography = Typography(
        headlineLarge = androidx.compose.ui.text.TextStyle(fontSize = 34.sp, lineHeight = 42.sp, fontWeight = FontWeight.SemiBold),
        headlineMedium = androidx.compose.ui.text.TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold),
        titleLarge = androidx.compose.ui.text.TextStyle(fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
        bodyLarge = androidx.compose.ui.text.TextStyle(fontSize = 17.sp, lineHeight = 26.sp),
        labelLarge = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold)
    )) {
        SideEffect {
            (context as? Activity)?.window?.let { window ->
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
        }
        val backdrop = rememberGraphicsLayer()
        var backdropOrigin by remember { mutableStateOf(Offset.Zero) }
        var dockHeight by remember { mutableIntStateOf(0) }
        val dockSpace = with(LocalDensity.current) { dockHeight.toDp() } + 24.dp
        val screenStates = rememberSaveableStateHolder()
        CompositionLocalProvider(LocalContentColor provides colors.onBackground,
            LocalGlass provides GlassEnvironment(if (useBlur) backdrop else null, backdropOrigin, dark, stillMotion)) {
        Box(Modifier.fillMaxSize().background(colors.background).safeDrawingPadding()) {
            val duration = if (stillMotion) 0 else 180
            // Record a separate decorative layer, avoiding recursive sampling of glass controls.
            Box(Modifier.matchParentSize().onGloballyPositioned { backdropOrigin = it.positionInRoot() }
                .drawWithContent {
                    backdrop.record {
                        drawRect(colors.background)
                        drawCircle(Brush.radialGradient(listOf(colors.primaryContainer.copy(alpha = .65f), Color.Transparent),
                            center = Offset(size.width * .9f, size.height * .30f), radius = size.width * .85f),
                            radius = size.width * .85f, center = Offset(size.width * .9f, size.height * .30f))
                        drawCircle(Brush.radialGradient(listOf(colors.primary.copy(alpha = .09f), Color.Transparent),
                            center = Offset(size.width * .1f, size.height * .85f), radius = size.width * .8f),
                            radius = size.width * .8f, center = Offset(size.width * .1f, size.height * .85f))
                    }
                    drawLayer(backdrop)
                })
            Crossfade(screen, animationSpec = tween(duration), label = "page", modifier = Modifier.fillMaxSize()) { current ->
                screenStates.SaveableStateProvider(current.name) {
                Column(Modifier.fillMaxSize().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = 720.dp).padding(bottom = if (current == Screen.Welcome) 0.dp else dockSpace).testTag("page-${current.name}").verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SaathiBrand()
                        if (current !in listOf(Screen.Welcome, Screen.Home, Screen.Practice, Screen.Settings)) {
                            GlassButton(tr(Copy.BACK), onClick = { navigate(Screen.Home) }, primary = false, compact = true)
                        } else if (current != Screen.Welcome) {
                            GlassButton(when(language) { GuidanceLanguage.ENGLISH -> "English"; GuidanceLanguage.HINDI -> "हिन्दी"; GuidanceLanguage.HINGLISH -> "Hinglish" }, onClick = { navigate(Screen.Settings) }, primary = false, compact = true)
                        }
                    }
                    error?.let { Text(tr(it), color = colors.error) }
                    when (current) {
                        Screen.Welcome -> {
                            Heading(tr(Copy.WELCOME))
                            Body(tr(Copy.INTRO))
                            Feature(tr(Copy.THREE_STEPS), tr(Copy.SAFETY))
                            SectionTitle(tr(Copy.LANGUAGE))
                            Languages(language) { language = it; preferences.language = it }
                            Action(tr(Copy.CONTINUE)) { preferences.welcomed = true; navigate(Screen.Home) }
                        }
                        Screen.Home -> {
                            Action("Report cyber fraud") { context.startActivity(Intent(context, com.saathi.CyberReportActivity::class.java)) }
                            Heading(tr(Copy.HELLO)); Body(tr(Copy.SUBTITLE))
                            Action(tr(Copy.LIVE_HELP)) { context.startActivity(Intent(context, com.saathi.AssistantActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                            Feature(tr(Copy.FEATURE), tr(Copy.FEATURE_BODY)) {
                                Text(tr(Copy.LOCAL), style = MaterialTheme.typography.labelLarge)
                                Action(tr(Copy.START)) { navigate(Screen.Intake) }
                            }
                            if (status != GuidanceSessionState.STOPPED) SettingRow(tr(status.labelCopy()), tr(status.bodyCopy())) { navigate(Screen.Session) }
                            SectionTitle(tr(Copy.CHOOSE))
                            TaskChoices(language) { selectedGoal = it; navigate(Screen.Setup) }
                            SettingRow(tr(Copy.CHECK), tr(Copy.SETUP_BODY)) { navigate(Screen.Setup) }
                            Body(tr(Copy.LIMIT))
                        }
                        Screen.Practice -> {
                            Heading(tr(Copy.PRACTICE)); Body(tr(Copy.FEATURE_BODY))
                            PracticeNavigation(language, practiceCategory, { practiceCategory = it }, stillMotion) {
                                selectedGoal = it; navigate(Screen.Setup)
                            }
                            Body(tr(Copy.LIMIT))
                        }
                        Screen.Intake -> {
                            Heading(tr(Copy.YOUR_TASK)); Body(tr(Copy.FEATURE_BODY))
                            GlassPanel(Modifier.fillMaxWidth(), shape = GlassTokens.Input) {
                                OutlinedTextField(value = task, onValueChange = { task = it; error = null }, label = { Text(tr(Copy.YOUR_TASK)) }, placeholder = { Text(tr(Copy.TASK_HINT)) }, modifier = Modifier.fillMaxWidth(), shape = GlassTokens.Input)
                            }
                            Action(tr(Copy.VOICE), secondary = true) {
                                runCatching { recognize.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.sttTag).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)) }.onFailure { error = Copy.VOICE_ERROR }
                            }
                            Body(tr(Copy.VOICE_NOTE))
                            Action(tr(Copy.REVIEW)) {
                                val goal = PracticeTask.parse(task)
                                if (goal == null) error = Copy.TASK_ERROR else { selectedGoal = goal; navigate(Screen.Setup) }
                            }
                            SectionTitle(tr(Copy.CHOOSE)); TaskChoices(language) { selectedGoal = it; navigate(Screen.Setup) }
                        }
                        Screen.Setup -> {
                            Heading(tr(Copy.CHECK)); Body(tr(Copy.SETUP_BODY))
                            SettingRow(tr(Copy.ACCESS), tr(Copy.ACCESS_BODY) + "\n" + tr(if (accessibility) Copy.ENABLED else Copy.NEEDED)) {
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            }
                            SettingRow(tr(Copy.OVERLAY), tr(Copy.OVERLAY_BODY) + "\n" + tr(if (overlay) Copy.ENABLED else Copy.NEEDED)) {
                                context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
                            }
                            Feature(tr(if (overlay && accessibility) Copy.READY else Copy.NEED_SETUP), tr(Copy.SAFETY))
                            Action(tr(Copy.START_PRACTICE), enabled = overlay && accessibility) { start() }
                            Action(tr(Copy.NO_GUIDE), secondary = true) { SaathiSession.stop(); openPractice() }
                            Body(tr(Copy.LIMIT))
                        }
                        Screen.Session -> {
                            Heading(tr(status.labelCopy())); Feature(tr(Copy.LOCAL), tr(status.bodyCopy()))
                            if (status.isRunning()) {
                                Action(tr(Copy.OPEN_PRACTICE)) { openPractice() }
                                Feature(tr(Copy.CONVERSATION), tr(Copy.CONVERSATION_BODY)) {
                                    Text(voicePhase.text(language))
                                    Action(tr(Copy.START_CONVERSATION), enabled = VoiceConversationService.supported(context)) {
                                        pendingVoiceSession = SaathiSession.sessionKey()
                                        val permissions = mutableListOf(android.Manifest.permission.RECORD_AUDIO)
                                        if (android.os.Build.VERSION.SDK_INT >= 33) permissions += android.Manifest.permission.POST_NOTIFICATIONS
                                        voicePermissions.launch(permissions.toTypedArray())
                                    }
                                    if (voicePhase != VoicePhase.OFF) Action(tr(Copy.STOP_CONVERSATION), secondary = true) {
                                        VoiceConversationService.stop(context)
                                    }
                                    if (!VoiceConversationService.supported(context)) Body(tr(Copy.VOICE_ERROR))
                                }
                                Action(tr(Copy.PAUSE), secondary = true) { SaathiSession.pause() }
                            } else if (status == GuidanceSessionState.COMPLETED) {
                                Action(tr(Copy.PRACTISE_AGAIN)) { navigate(Screen.Setup) }
                            } else Action(tr(Copy.RESUME)) { start() }
                            Action(tr(Copy.STOP), secondary = true) { SaathiSession.stop() }
                            Body(tr(Copy.SAFETY))
                        }
                        Screen.Settings -> {
                            Heading(tr(Copy.SETTINGS)); SectionTitle(tr(Copy.LANGUAGE))
                            Languages(language) { language = it; preferences.language = it }
                            SectionTitle(tr(Copy.APPEARANCE))
                            listOf("System" to Copy.SYSTEM, "Light" to Copy.LIGHT, "Dark" to Copy.DARK).forEach { (value, label) ->
                                Choice(tr(label), theme == value) { theme = value; preferences.theme = value }
                            }
                            SectionTitle(tr(Copy.EXPERIENCE))
                            Action("Offline voice setup", secondary = true) { context.startActivity(Intent(context, com.saathi.VoiceSetupActivity::class.java)) }
                            ToggleRow(tr(Copy.SPEECH), tr(Copy.SPEECH_BODY), speech) { speech = it; preferences.speech = it }
                            if (speech) SpeechSpeed(language, speechRate) { value -> speechRate = value; preferences.speechRate = value }
                            ToggleRow(tr(Copy.MOTION), tr(Copy.MOTION_BODY), reduceMotion) { reduceMotion = it; preferences.reducedMotion = it }
                            ToggleRow(tr(Copy.TRANSPARENCY), tr(Copy.TRANSPARENCY_BODY), reduceTransparency) { reduceTransparency = it; preferences.reducedTransparency = it }
                            ToggleRow(tr(Copy.HAPTICS), tr(Copy.HAPTICS_BODY), haptics) { haptics = it; preferences.haptics = it }
                            SettingRow(tr(Copy.PRIVACY), tr(Copy.PRIVACY_BODY)) { navigate(Screen.Privacy) }
                            Action("Backend connection", secondary = true) { com.saathi.gateway.PracticeGateway.openSetup(context) }
                            Body(tr(Copy.LIMIT))
                        }
                        Screen.Privacy -> {
                            if (com.saathi.gateway.PracticeGateway.aiEnabled()) Body("AI navigation is enabled for new live sessions. Exact matches stay local. Otherwise your task, app identity and eligible labels go through your server to a primary model, with fallback only when needed. Clear local data disables this connection and forgets its token.")
                            if (com.saathi.gateway.PracticeGateway.enabled()) Body("Local backend test is enabled for practice: public control IDs and a task category go to your computer’s loopback server. Help in other apps stays local. Clear local data also forgets the temporary token.")
                            Heading(tr(Copy.PRIVACY)); Feature(tr(Copy.LOCAL), tr(Copy.PRIVACY_BODY))
                            Body(tr(Copy.ACCESS_BODY)); Body(tr(Copy.VOICE_NOTE)); Body(tr(Copy.LIMIT))
                            Action(tr(Copy.STOP)) { SaathiSession.stop() }
                            Action(tr(Copy.CLEAR), secondary = true) { clearDialog = true }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                }
            }
            if (screen != Screen.Welcome) {
                val destinations = listOf(Screen.Home, Screen.Practice, Screen.Settings)
                val selected = when (screen) {
                    Screen.Home -> 0
                    Screen.Settings, Screen.Privacy -> 2
                    else -> 1
                }
                GlassNavigation(
                    items = listOf(
                        NavigationItem("home", tr(Copy.HOME), NavigationGlyph.Home),
                        NavigationItem("practice", tr(Copy.PRACTICE), NavigationGlyph.Practice),
                        NavigationItem("settings", tr(Copy.SETTINGS), NavigationGlyph.Settings)
                    ), selectedIndex = selected, reduceMotion = stillMotion,
                    backdrop = if (useBlur) backdrop else null, backdropOrigin = backdropOrigin, dark = dark,
                    onSelect = { navigate(destinations[it]) },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 16.dp, vertical = 12.dp)
                        .widthIn(max = 600.dp).fillMaxWidth().onSizeChanged { dockHeight = it.height }
                )
            }
        }
        }
        if (clearDialog) CompositionLocalProvider(LocalGlass provides GlassEnvironment(dark = dark, reducedMotion = stillMotion)) {
        AlertDialog(containerColor = colors.surface, tonalElevation = 0.dp, onDismissRequest = { clearDialog = false }, title = { Text(tr(Copy.CLEAR)) }, text = { Text(tr(Copy.CLEAR_BODY)) }, confirmButton = {
            GlassButton(tr(Copy.CLEAR), primary = false, compact = true, onClick = {
                SaathiSession.stop()
                com.saathi.gateway.PracticeGateway.disable()
                com.saathi.storage.ConversationStore(context).clear()
                com.saathi.storage.GuidanceStateStore(context).clear()
                listOf("saathi_ui", "saathi_preferences", "saathi_theme", "completed_guidance_flows", "gemini_rate_limits").forEach { context.getSharedPreferences(it, 0).edit().clear().apply() }
                theme = "System"; language = GuidanceLanguage.ENGLISH; speech = false; speechRate = TtsManager.DEFAULT_SPEECH_RATE; reduceMotion = false; reduceTransparency = false; haptics = true; task = ""; practiceCategory = 0
                clearDialog = false; navigate(Screen.Welcome)
            })
        }, dismissButton = { GlassButton(tr(Copy.CANCEL), primary = false, compact = true, onClick = { clearDialog = false }) })
        }
    }
}

@Composable private fun Heading(value: String) { Text(value, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() }) }
@Composable private fun SectionTitle(value: String) { Text(value, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() }) }
@Composable private fun Body(value: String) { Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
@Composable private fun Action(label: String, secondary: Boolean = false, enabled: Boolean = true, action: () -> Unit) {
    GlassButton(label, onClick = action, primary = !secondary, enabled = enabled, arrow = !secondary)
}
@Composable private fun Feature(title: String, description: String, content: @Composable ColumnScope.() -> Unit = {}) {
    GlassPanel(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text(title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
            Body(description)
            content()
        }
    }
}
@Composable private fun SettingRow(title: String, description: String, action: () -> Unit) {
    GlassPanel(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = action).padding(22.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(title, style = MaterialTheme.typography.labelLarge); Body(description) }
            Spacer(Modifier.width(16.dp)); GlassChevron(MaterialTheme.colorScheme.primary)
        }
    }
}
@Composable private fun Choice(label: String, selected: Boolean, action: () -> Unit) {
    GlassPanel(Modifier.fillMaxWidth(), shape = GlassTokens.Pill, focused = selected) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).selectable(selected = selected, role = Role.RadioButton, onClick = action).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = null); Spacer(Modifier.width(12.dp)); Text(label, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
@Composable private fun Languages(selected: GuidanceLanguage, change: (GuidanceLanguage) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { listOf(GuidanceLanguage.ENGLISH to "English", GuidanceLanguage.HINDI to "हिन्दी", GuidanceLanguage.HINGLISH to "Hinglish").forEach { (value, label) -> Choice(label, selected == value) { change(value) } } }
}
@Composable private fun ToggleRow(title: String, description: String, checked: Boolean, change: (Boolean) -> Unit) {
    GlassPanel(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).toggleable(value = checked, role = Role.Switch, onValueChange = change).padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(title, style = MaterialTheme.typography.labelLarge); Body(description) }
            Switch(checked = checked, onCheckedChange = null, modifier = Modifier.padding(start = 12.dp))
        }
    }
}
@Composable private fun SpeechSpeed(language: GuidanceLanguage, selectedRate: Float, change: (Float) -> Unit) {
    val context = LocalContext.current
    val preview = remember(selectedRate) { TtsManager(context, selectedRate) }
    DisposableEffect(preview) { onDispose { preview.release() } }
    val lifecycle = LocalLifecycleOwner.current
    DisposableEffect(lifecycle, preview) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) preview.stop() }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(Copy.SPEECH_SPEED.text(language))
        listOf(
            Copy.SLOW to TtsManager.MIN_SPEECH_RATE,
            Copy.STANDARD to TtsManager.DEFAULT_SPEECH_RATE,
            Copy.FAST to TtsManager.MAX_SPEECH_RATE
        ).forEach { (label, rate) ->
            Choice(label.text(language), selectedRate == rate) { change(rate) }
        }
        Action(Copy.PREVIEW_VOICE.text(language), secondary = true) {
            preview.speak(Copy.SESSION_BODY.text(language), language)
        }
    }
}
@Composable private fun TaskChoices(language: GuidanceLanguage, choose: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(Copy.ELECTRICITY to "Pay my electricity bill", Copy.WATER to "Pay my water bill", Copy.DTH to "Recharge my DTH").forEach { (label, goal) ->
            SettingRow(label.text(language), Copy.LOCAL.text(language)) { choose(goal) }
        }
    }
}
