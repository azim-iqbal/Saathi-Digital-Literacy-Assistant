package com.saathi

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*
import com.saathi.capture.ScreenshotCapture
import com.saathi.intake.IntakeResult
import com.saathi.intake.FlowResearchCoordinator
import com.saathi.intake.DocumentExample
import com.saathi.intake.DocumentExamples
import com.saathi.intake.TaskBrief
import com.saathi.intake.TaskIntake
import com.saathi.intake.TaskKind
import com.saathi.language.GuidanceCopy
import com.saathi.language.GuidanceLanguage
import com.saathi.orchestrator.SaathiSession
import com.saathi.speech.SpeechInputManager
import com.saathi.speech.TtsManager
import com.saathi.storage.ConversationStore
import com.saathi.storage.StoredMessage
import com.saathi.storage.GuidanceStateStore
import com.saathi.core.GuidancePolicy
import com.saathi.device.DeviceCapabilities

/** A task-first conversation surface. Saathi prepares before it begins observing a screen. */
class LegacyTaskActivity : Activity() {
    companion object {
        const val EXTRA_LAUNCH_TASK = "launch_task"
        const val SCREEN_CAPTURE_REQUEST = 42
        const val MICROPHONE_PERMISSION_REQUEST = 8
        const val PREFERENCES_NAME = "saathi_preferences"
        const val LANGUAGE_PREFERENCE = "guidance_language"
        const val LANGUAGE_PICKER_SHOWN = "language_picker_shown"

    }
    private val DEEP_TEAL get() = color(R.color.primary)
    private val MINT get() = color(R.color.primary_container)
    private val INK get() = color(R.color.text_primary)
    private val MUTED get() = color(R.color.text_secondary)
    private val BASE get() = color(R.color.surface)
    private val ELEVATED get() = color(R.color.surface_elevated)
    private val BORDER get() = color(R.color.border)

    private lateinit var conversation: LinearLayout
    private lateinit var conversationScroll: ScrollView
    private lateinit var input: EditText
    private lateinit var quickReplies: LinearLayout
    private lateinit var languageButton: TextView
    private lateinit var voiceToggle: Switch
    private var speechInput: SpeechInputManager? = null
    private var conversationTts: TtsManager? = null
    private var pendingBrief: TaskBrief? = null
    private var waitingForApp = false
    private var waitingForDocumentField = false
    private var waitingForDocumentConfirmation = false
    private var pendingVoiceGuidanceBrief: TaskBrief? = null
    private val preferences by lazy { getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE) }
    private val conversationStore by lazy { ConversationStore(applicationContext) }
    private val guidanceStateStore by lazy { GuidanceStateStore(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().detectDiskReads().detectDiskWrites().detectNetwork().penaltyLog().build())
        }
        startMainThreadHeartbeat()
        setContentView(buildScreen())
        val launchTask = intent.getStringExtra(EXTRA_LAUNCH_TASK)
        if (launchTask == null) restoreConversationOrWelcome() else {
            showWelcome()
            input.setText(launchTask)
            input.postDelayed({ submitReply() }, 220)
        }
        if (!preferences.getBoolean(LANGUAGE_PICKER_SHOWN, false)) showLanguagePicker()
    }

    private fun startMainThreadHeartbeat() {
        val handler = Handler(Looper.getMainLooper())
        var previous = System.currentTimeMillis()
        handler.post(object : Runnable {
            override fun run() {
                val now = System.currentTimeMillis()
                if (now - previous > 4_500L) android.util.Log.w("SaathiANR", "Main thread heartbeat delayed ${now - previous}ms")
                previous = now
                handler.postDelayed(this, 2_000L)
            }
        })
    }

    private fun buildScreen(): View {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(16))
            setBackgroundColor(BASE)
        }
        page.addView(header())

        conversation = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(18), 0, 0)
        }
        conversationScroll = ScrollView(this).apply {
            isFillViewport = true
            addView(conversation)
        }
        page.addView(conversationScroll, LinearLayout.LayoutParams(-1, 0, 1f))

        quickReplies = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(10), 0, dp(8))
        }
        page.addView(HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(quickReplies) })
        page.addView(composer())
        return page
    }

    private fun header(): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(LinearLayout(this@LegacyTaskActivity).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(this@LegacyTaskActivity).apply {
                text = "Saathi"
                textSize = 29f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(DEEP_TEAL)
            })
            addView(TextView(this@LegacyTaskActivity).apply {
                text = "Your calm task companion"
                textSize = 13f
                setTextColor(MUTED)
            })
        }, LinearLayout.LayoutParams(0, -2, 1f))
        languageButton = TextView(this@LegacyTaskActivity).apply {
            text = languageMode().name.lowercase().replaceFirstChar(Char::uppercase)
            textSize = 13f
            setTextColor(DEEP_TEAL)
            setPadding(dp(14), dp(9), dp(14), dp(9))
            background = rounded(MINT, 999)
            setOnClickListener { showLanguagePicker() }
        }
        addView(languageButton)
        addView(ImageButton(this@LegacyTaskActivity).apply {
            setImageResource(if (com.saathi.theme.SaathiTheme.isDark(this@LegacyTaskActivity)) R.drawable.ic_light_mode else R.drawable.ic_dark_mode)
            setBackgroundColor(Color.TRANSPARENT); setPadding(dp(8), dp(8), dp(8), dp(8)); contentDescription = "Switch colour theme"
            setOnClickListener { com.saathi.theme.SaathiTheme.toggle(this@LegacyTaskActivity); recreate() }
        }, LinearLayout.LayoutParams(dp(44), dp(44)))
        addView(TextView(this@LegacyTaskActivity).apply {
            text = "ⓘ"; textSize = 20f; setTextColor(DEEP_TEAL); setPadding(dp(14), dp(9), 0, dp(9))
            contentDescription = "Where Saathi can help"
            setOnClickListener { startActivity(Intent(this@LegacyTaskActivity, TermsActivity::class.java)) }
        })
    }

    private fun composer(): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = rounded(ELEVATED, 22, BORDER)
        setPadding(dp(12), dp(10), dp(12), dp(10))
        voiceToggle = Switch(this@LegacyTaskActivity).apply {
            text = "Voice conversation while guiding"
            textSize = 13f
            isChecked = false
            setTextColor(MUTED)
        }
        addView(voiceToggle)
        addView(LinearLayout(this@LegacyTaskActivity).apply {
            gravity = Gravity.CENTER_VERTICAL
            input = EditText(this@LegacyTaskActivity).apply {
                hint = "Tell Saathi what you need to do"
                textSize = 16f
                setTextColor(INK)
                setSingleLine(true)
                background = null
            }
            addView(input, LinearLayout.LayoutParams(0, -2, 1f))
            addView(iconButton(R.drawable.ic_mic, "Voice input") { listenForReply() })
            addView(iconButton(R.drawable.ic_send, "Send") { submitReply() })
        })
    }

    private fun showWelcome() {
        assistantMessage("Hello, I’m Saathi. Tell me the task you want to complete, and I’ll first understand it before guiding you.")
        assistantMessage("I can help with bills, payments, forms, tickets, and essential phone settings.")
        setQuickReplies(TaskKind.entries.map { it.title }) { choice ->
            input.setText(choice)
            submitReply()
        }
    }

    private fun restoreConversationOrWelcome() {
        val saved = conversationStore.load()
        pendingBrief = guidanceStateStore.restore()
        if (saved.isEmpty()) showWelcome() else {
            saved.forEach { addMessage(it.text, it.fromUser, persist = false) }
            assistantMessage("Your previous conversation is restored. You can resume the task or tell me what has changed.")
            setQuickReplies(listOf("Resume guidance", "Start a new task", "Where can Saathi help?")) { action ->
                when (action) {
                    "Resume guidance" -> pendingBrief?.let(::startGuidance) ?: assistantMessage("Tell me the task you would like to resume.")
                    "Start a new task" -> { conversationStore.clear(); guidanceStateStore.clear(); conversation.removeAllViews(); showWelcome() }
                    else -> startActivity(Intent(this, TermsActivity::class.java))
                }
            }
        }
    }

    private fun submitReply() {
        val message = input.text.toString().trim()
        if (message.isBlank()) return
        input.text.clear()
        userMessage(message)

        when {
            waitingForDocumentConfirmation -> confirmDocumentHelp(message)
            waitingForDocumentField -> helpWithDocumentField(message)
            waitingForApp -> prepareForApp(message)
            else -> understandTask(message)
        }
    }

    private fun understandTask(goal: String) {
        when (val result = TaskIntake.understandGoal(goal)) {
            is IntakeResult.Refused -> {
                assistantMessage(result.message)
                setQuickReplies(TaskKind.entries.map { it.title }) { input.setText(it); submitReply() }
            }
            is IntakeResult.AskForApp -> {
                pendingBrief = result.brief
                waitingForApp = true
                assistantMessage(result.message)
                setQuickReplies(defaultAppChoices(result.brief.kind)) { input.setText(it); submitReply() }
            }
            is IntakeResult.Ready -> Unit
        }
    }

    private fun prepareForApp(appOrWebsite: String) {
        val brief = pendingBrief ?: return
        waitingForApp = false
        assistantMessage("Preparing a short, private checklist for $appOrWebsite…")
        window.decorView.postDelayed({
            val ready = TaskIntake.prepare(brief, appOrWebsite)
            val research = FlowResearchCoordinator.prepare(ready.brief)
            pendingBrief = ready.brief
            assistantMessage(ready.preparation.title)
            assistantMessage("${ready.preparation.detail}\n\n${ready.preparation.suggestedFirstStep}")
            assistantMessage(research.summary)
            if (research.likelySteps.isNotEmpty()) {
                assistantMessage("A likely route:\n${research.likelySteps.mapIndexed { index, step -> "${index + 1}. $step" }.joinToString("\n")}")
            }
            if (ready.brief.kind == TaskKind.FORM) {
                beginDocumentHelp()
                return@postDelayed
            }
            assistantMessage("When you are ready, I will guide one step at a time. Visual guidance is on by default; voice will speak only important changes if you enable it below.")
            setQuickReplies(listOf("Start guidance", "Open demo bill pay", "Change language", "Cancel")) { action ->
                when (action) {
                    "Start guidance" -> startGuidance(ready.brief)
                    "Open demo bill pay" -> startActivity(Intent(this, DemoBillPayActivity::class.java))
                    "Cancel" -> cancelConversation()
                    else -> showLanguagePicker()
                }
            }
        }, 420)
    }

    private fun startGuidance(brief: TaskBrief) {
        if (voiceToggle.isChecked && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            pendingVoiceGuidanceBrief = brief
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), MICROPHONE_PERMISSION_REQUEST)
            return
        }
        val result = SaathiSession.start(applicationContext, brief.goal, languageMode(), voiceToggle.isChecked)
        if (result.decision == com.saathi.guardrails.GuardrailDecision.REFUSE) {
            assistantMessage(GuidanceCopy.guardrailRedirect(languageMode()))
            return
        }
        guidanceStateStore.save(brief, languageMode(), voiceToggle.isChecked)
        assistantMessage("Local practice is ready. Open Demo Bill Pay to use synthetic examples. Guidance for other apps is not available in this build. You perform every tap yourself.")
        val device = DeviceCapabilities.detect(this)
        if (device.batteryRisk != DeviceCapabilities.BatteryRisk.NONE) {
            assistantMessage("${device.manufacturer} devices can pause background apps. To keep guidance active when you switch apps: ${device.manualInstructions}")
        }
        if (DeviceCapabilities.isBatterySaverOn(this)) assistantMessage("Battery Saver is on, so guidance may respond more slowly. You can continue, or turn it off for the most reliable background session.")
        setQuickReplies(listOf("Grant overlay", "Enable accessibility", "Keep guidance active", "Open demo bill pay", "Cancel")) { action ->
            when (action) {
                "Grant overlay" -> startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                "Enable accessibility" -> startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                "Keep guidance active" -> openBatterySetup()
                "Capture unavailable" -> assistantMessage("Screen capture is disabled in this local practice build.")
                "Open demo bill pay" -> startActivity(Intent(this, DemoBillPayActivity::class.java))
                else -> cancelConversation()
            }
        }
    }

    private fun openBatterySetup() {
        val profile = DeviceCapabilities.detect(this)
        profile.setupIntent?.let { startActivity(it); return }
        assistantMessage("Open this on your ${profile.manufacturer} device: ${profile.manualInstructions}")
    }

    private fun beginDocumentHelp() {
        waitingForDocumentField = true
        assistantMessage(
            "I’ll help with one upload field at a time. Tell me the label you see, such as “photo”, “address proof”, or “marksheet”. If you are unsure, say “I don’t know” and I’ll show a safe example.",
            speak = true
        )
        setQuickReplies(listOf("Photo", "Address proof", "Marksheet", "I don't know", "Cancel")) { choice ->
            input.setText(choice)
            submitReply()
        }
    }

    private fun helpWithDocumentField(answer: String) {
        if (isCancel(answer)) {
            cancelConversation()
            return
        }
        val requestedField = if (doesNotKnow(answer)) "identity document" else answer
        val example = DocumentExamples.forField(requestedField)
        waitingForDocumentField = false
        waitingForDocumentConfirmation = true
        assistantMessage("Here is a safe example for “$requestedField”. This is only an illustration, never a real person’s document.", speak = true)
        addDocumentExample(example)
        assistantMessage("${example.caption}\n\n${example.fieldHint}\n\nSay “understood”, “मुझे समझ आया”, or “samajh gaya” when this makes sense. You can also cancel at any time.", speak = true)
        setQuickReplies(listOf("I understand", "मुझे समझ आया", "Samajh gaya", "Show another example", "Cancel")) { choice ->
            input.setText(choice)
            submitReply()
        }
    }

    private fun confirmDocumentHelp(answer: String) {
        if (isCancel(answer)) {
            cancelConversation()
            return
        }
        if (doesNotKnow(answer) || answer.contains("another", ignoreCase = true)) {
            waitingForDocumentConfirmation = false
            waitingForDocumentField = true
            assistantMessage("No problem. Tell me the exact field label, or choose one below.", speak = true)
            setQuickReplies(listOf("Photo", "Address proof", "Marksheet", "I don't know", "Cancel")) { choice -> input.setText(choice); submitReply() }
            return
        }
        if (!isUnderstood(answer)) {
            assistantMessage("I can explain it again. Say “I don’t know” to see another example, or say “understood” when you are ready.", speak = true)
            return
        }
        waitingForDocumentConfirmation = false
        assistantMessage("Great. On the website, tap the upload field and choose only your own matching document. Check that the preview is clear before continuing.", speak = true)
        setQuickReplies(listOf("Start guidance", "Show another example", "Cancel")) { action ->
            when (action) {
                "Start guidance" -> pendingBrief?.let(::startGuidance)
                "Show another example" -> {
                    waitingForDocumentField = true
                    beginDocumentHelp()
                }
                else -> cancelConversation()
            }
        }
    }

    private fun cancelConversation() {
        waitingForApp = false
        waitingForDocumentField = false
        waitingForDocumentConfirmation = false
        pendingBrief = null
        guidanceStateStore.clear()
        SaathiSession.stop()
        assistantMessage("Guidance is paused. Nothing was uploaded or submitted. You can start a new task whenever you are ready.")
        setQuickReplies(TaskKind.entries.map { it.title }) { choice -> input.setText(choice); submitReply() }
    }

    private fun doesNotKnow(value: String): Boolean = listOf("don't know", "dont know", "not sure", "unknown", "नहीं पता", "pata nahi", "pata nahin").any { value.contains(it, ignoreCase = true) }
    private fun isCancel(value: String): Boolean = listOf("cancel", "stop", "exit", "रद्द", "बंद", "cancel karo").any { value.contains(it, ignoreCase = true) }
    private fun isUnderstood(value: String): Boolean = listOf("understood", "i understand", "got it", "samajh", "समझ", "मुझे समझ", "haan", "हाँ").any { value.contains(it, ignoreCase = true) }

    private fun listenForReply() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), MICROPHONE_PERMISSION_REQUEST)
            return
        }
        val language = languageMode()
        if (speechInput == null) {
            speechInput = SpeechInputManager(
                this,
                { value -> input.setText(value); submitReply() },
                { assistantMessage(GuidanceCopy.lowConfidence(language)) },
                { assistantMessage(GuidanceCopy.lowConfidence(language)) }
            )
        }
        speechInput?.listen(language.sttTag)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (requestCode != MICROPHONE_PERMISSION_REQUEST || results.firstOrNull() != PackageManager.PERMISSION_GRANTED) return
        pendingVoiceGuidanceBrief?.let {
            pendingVoiceGuidanceBrief = null
            startGuidance(it)
        } ?: listenForReply()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != SCREEN_CAPTURE_REQUEST || resultCode != RESULT_OK || data == null) return

        ScreenshotCapture.stop()
        assistantMessage("Screen capture is disabled until screen alignment and lifecycle checks are implemented.")
    }

    private fun assistantMessage(value: String, speak: Boolean = false) {
        addMessage(value, fromUser = false)
        if (speak && voiceToggle.isChecked) {
            val tts = conversationTts ?: TtsManager(applicationContext).also { conversationTts = it }
            tts.speak(value, languageMode())
        }
    }
    private fun userMessage(value: String) = addMessage(value, fromUser = true)

    private fun addDocumentExample(example: DocumentExample) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = rounded(MINT, 20, BORDER)
            contentDescription = "Safe illustrated example: ${example.title}. ${example.caption}"
        }
        card.addView(DemoDocumentView(this, example.title), LinearLayout.LayoutParams(-1, dp(150)))
        card.addView(TextView(this).apply {
            text = "SAFE EXAMPLE • ${example.title.uppercase()}"
            textSize = 12f
            letterSpacing = .08f
            setTextColor(DEEP_TEAL)
            setPadding(0, dp(10), 0, dp(4))
        })
        card.addView(TextView(this).apply {
            text = example.caption
            textSize = 15f
            setTextColor(INK)
        })
        conversation.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
        conversationScroll.post { conversationScroll.smoothScrollTo(0, conversation.bottom) }
    }

    private fun addMessage(value: String, fromUser: Boolean, persist: Boolean = true) {
        val bubble = TextView(this).apply {
            text = value
            textSize = 16f
            setTextColor(if (fromUser) color(R.color.chat_user_text) else color(R.color.chat_assistant_text))
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = rounded(if (fromUser) color(R.color.chat_user_bg) else color(R.color.chat_assistant_bg), 20, if (fromUser) null else color(R.color.border_subtle))
        }
        conversation.addView(LinearLayout(this).apply {
            gravity = if (fromUser) Gravity.END else Gravity.START
            addView(bubble, LinearLayout.LayoutParams(-2, -2).apply { width = minOf(resources.displayMetrics.widthPixels - dp(72), dp(420)) })
        }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
        bubble.alpha = 0f
        bubble.animate().alpha(1f).setDuration(180).start()
        conversationScroll.post { conversationScroll.smoothScrollTo(0, conversation.bottom) }
        if (persist) conversationStore.append(StoredMessage(value, fromUser))
    }

    private fun setQuickReplies(items: List<String>, action: (String) -> Unit) {
        quickReplies.removeAllViews()
        items.forEach { label ->
            quickReplies.addView(TextView(this).apply {
                text = label
                textSize = 14f
                setTextColor(DEEP_TEAL)
                setPadding(dp(14), dp(9), dp(14), dp(9))
                background = rounded(MINT, 999, BORDER)
                setOnClickListener {
                    animate().scaleX(0.96f).scaleY(0.96f).setDuration(70).withEndAction {
                        animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                    }.start()
                    action(label)
                }
            }, LinearLayout.LayoutParams(-2, -2).apply { rightMargin = dp(8) })
        }
    }

    private fun defaultAppChoices(kind: TaskKind) = when (kind) {
        TaskKind.BILL_PAYMENT -> listOf("Official provider website", "Other website", "Demo Bill Pay")
        TaskKind.PAYMENT -> listOf("Official provider website", "Other website")
        TaskKind.FORM -> listOf("Government portal", "College website", "Other website")
        TaskKind.TICKET -> listOf("IRCTC", "redBus", "Airline website")
        TaskKind.SETTINGS -> listOf("Android Settings", "Accessibility settings", "App settings")
    }

    private fun showLanguagePicker() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Choose your guidance language")
            .setSingleChoiceItems(arrayOf("English", "हिंदी", "Hinglish"), languageMode().ordinal) { dialog, which ->
                val selected = GuidanceLanguage.entries[which]
                preferences.edit().putString(LANGUAGE_PREFERENCE, selected.storageValue).putBoolean(LANGUAGE_PICKER_SHOWN, true).apply()
                languageButton.text = selected.name.lowercase().replaceFirstChar(Char::uppercase)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun languageMode(): GuidanceLanguage = if (preferences.contains(LANGUAGE_PREFERENCE)) {
        GuidanceLanguage.fromStorage(preferences.getString(LANGUAGE_PREFERENCE, null))
    } else {
        GuidanceLanguage.initialForSystemLocale()
    }

    private fun iconButton(icon: Int, description: String, action: () -> Unit) = ImageButton(this).apply {
        setImageResource(icon); contentDescription = description; scaleType = ImageView.ScaleType.CENTER
        background = rounded(DEEP_TEAL, 18); setPadding(dp(8), dp(8), dp(8), dp(8))
        setOnClickListener { animate().translationX(dp(4).toFloat()).setDuration(90).withEndAction { animate().translationX(0f).setDuration(90).start(); action() }.start() }
    }.also { it.layoutParams = LinearLayout.LayoutParams(dp(44), dp(44)).apply { leftMargin = dp(8) } }

    private fun color(resource: Int) = com.saathi.theme.SaathiTheme.color(this, resource)

    private fun rounded(fill: Int, radiusDp: Int, stroke: Int? = null) = GradientDrawable().apply {
        setColor(fill)
        cornerRadius = dp(radiusDp).toFloat()
        stroke?.let { setStroke(dp(1), it) }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        speechInput?.destroy()
        conversationTts?.release()
        super.onDestroy()
    }
}

private class DemoDocumentView(context: android.content.Context, private val title: String) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val page = RectF(dp(10).toFloat(), dp(6).toFloat(), (width - dp(10)).toFloat(), (height - dp(6)).toFloat())
        paint.color = Color.WHITE
        canvas.drawRoundRect(page, dp(16).toFloat(), dp(16).toFloat(), paint)
        paint.color = 0xFF0B6B5A.toInt()
        canvas.drawRoundRect(RectF(page.left, page.top, page.right, page.top + dp(30)), dp(16).toFloat(), dp(16).toFloat(), paint)
        paint.textSize = dp(11).toFloat()
        paint.color = Color.WHITE
        canvas.drawText("SAMPLE • NOT FOR UPLOAD", page.left + dp(14), page.top + dp(20), paint)
        paint.color = 0xFFDDF1EB.toInt()
        canvas.drawCircle(page.left + dp(42), page.top + dp(72), dp(22).toFloat(), paint)
        paint.color = 0xFF94B4A8.toInt()
        canvas.drawCircle(page.left + dp(42), page.top + dp(66), dp(8).toFloat(), paint)
        canvas.drawRoundRect(RectF(page.left + dp(28), page.top + dp(75), page.left + dp(56), page.top + dp(88)), dp(7).toFloat(), dp(7).toFloat(), paint)
        paint.color = 0xFF35564D.toInt()
        paint.textSize = dp(12).toFloat()
        canvas.drawText(title, page.left + dp(78), page.top + dp(58), paint)
        paint.color = 0xFFB6CBC3.toInt()
        repeat(3) { index ->
            val y = page.top + dp(70 + index * 16)
            canvas.drawRoundRect(RectF(page.left + dp(78), y, page.right - dp(22), y + dp(6)), dp(3).toFloat(), dp(3).toFloat(), paint)
        }
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
