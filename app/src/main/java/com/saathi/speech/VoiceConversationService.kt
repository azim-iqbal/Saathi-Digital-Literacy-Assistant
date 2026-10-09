package com.saathi.speech

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.*
import android.speech.*
import com.saathi.AssistantActivity
import com.saathi.accessibility.SensitiveContent
import com.saathi.language.GuidanceCopy
import com.saathi.language.GuidanceLanguage
import com.saathi.orchestrator.SaathiSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class VoicePhase(val en: String, val hi: String, val hinglish: String) {
    OFF("Voice conversation off", "वॉइस बातचीत बंद", "Voice conversation off"),
    WAITING("Microphone off · waiting for a supported screen", "माइक बंद · समर्थित स्क्रीन की प्रतीक्षा", "Mic off · supported screen ka intezaar"),
    SPEAKING("Saathi is speaking · microphone off", "साथी बोल रहा है · माइक बंद", "Saathi bol raha hai · mic off"),
    STARTING("Preparing microphone", "माइक तैयार हो रहा है", "Mic taiyaar ho raha hai"),
    LISTENING("Listening · say help, repeat, pause or stop", "सुन रहे हैं · मदद, दोबारा, रोकें या रद्द बोलें", "Sun rahe hain · help, repeat, pause ya stop boliye"),
    PAUSED("Voice paused · resume in Saathi", "वॉइस रुकी है · साथी में फिर शुरू करें", "Voice paused · Saathi mein resume karein"),
    PRIVATE("Microphone off · enable hands-free replies to resume", "माइक बंद · फिर शुरू करने के लिए हैंड्स-फ़्री जवाब चालू करें", "Mic off · resume ke liye hands-free replies chalu karein"),
    NEEDS_LANGUAGE("Offline voice language unavailable · open voice setup", "ऑफलाइन वॉइस भाषा उपलब्ध नहीं · वॉइस सेटअप खोलें", "Offline voice language nahi hai · voice setup kholiye"),
    UNAVAILABLE("Voice unavailable · use visual guidance", "वॉइस उपलब्ध नहीं · स्क्रीन मार्गदर्शन देखें", "Voice unavailable · screen guidance dekhein");
    fun text(language: GuidanceLanguage) = when(language) {
        GuidanceLanguage.ENGLISH -> en
        GuidanceLanguage.HINDI -> hi
        GuidanceLanguage.HINGLISH -> hinglish
    }
}

/** User-started, on-device speech turns. Never restarts from a background intent or process death. */
class VoiceConversationService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val turns = VoiceTurnGate()
    private val privacy = VoicePrivacyGate()
    private var recognizer: SpeechRecognizer? = null
    private var tts: TtsManager? = null
    private var key: String? = null
    private var language = GuidanceLanguage.ENGLISH
    private var manuallyPaused = false
    private var prompt: String? = null
    private var lastSpoken: String? = null
    private var focusHeld = false
    private lateinit var audio: AudioManager
    private lateinit var focus: AudioFocusRequest
    private val interruption = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) SaathiSession.stopForSession(key)
            else pause()
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        audio = getSystemService(AudioManager::class.java)
        focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setOnAudioFocusChangeListener({ change -> if (change < 0) pause() }, handler)
            .build()
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF).apply { addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY) }
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(interruption, filter, RECEIVER_NOT_EXPORTED)
        else { @Suppress("DEPRECATION") registerReceiver(interruption, filter) }
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Saathi voice", NotificationManager.IMPORTANCE_LOW))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val requested = intent?.getStringExtra(KEY)
        if (intent?.action == STOP || intent?.action == PAUSE) {
            if (requested != null && requested == key && requested == SaathiSession.sessionKey()) {
                if (intent.action == STOP) SaathiSession.stopForSession(key) else pause()
            }
            if (key == null) stopSelf(startId)
            return START_NOT_STICKY
        }
        if (intent?.action != START || requested == null || requested != SaathiSession.sessionKey()) {
            if (key == null) stopSelf(startId)
            return START_NOT_STICKY
        }
        cancelAudio()
        key = requested
        language = GuidanceLanguage.fromStorage(intent.getStringExtra(LANGUAGE))
        manuallyPaused = false
        privacy.activate()
        lastSpoken = null
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED || !supported(this)) {
            mutablePhase.value = VoicePhase.UNAVAILABLE
            stopSelf(startId)
            return START_NOT_STICKY
        }
        tts?.release()
        tts = TtsManager(this, intent.getFloatExtra(RATE, TtsManager.DEFAULT_SPEECH_RATE))
        mutablePhase.value = VoicePhase.WAITING
        try {
            if (Build.VERSION.SDK_INT >= 29) startForeground(NOTICE, notification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
            else startForeground(NOTICE, notification())
            SaathiSession.refreshVoice()
        } catch (_: RuntimeException) {
            mutablePhase.value = VoicePhase.UNAVAILABLE
            stopSelf(startId)
        }
        return START_NOT_STICKY
    }

    private fun valid() = key != null && key == SaathiSession.sessionKey() &&
        !getSystemService(KeyguardManager::class.java).isKeyguardLocked &&
        checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun cancelAudio() {
        turns.cancel()
        handler.removeCallbacksAndMessages(null)
        val previous = recognizer
        recognizer = null
        runCatching { previous?.cancel(); previous?.destroy() }
        tts?.stop()
        if (focusHeld) { audio.abandonAudioFocusRequest(focus); focusHeld = false }
    }
    private fun pause() {
        manuallyPaused = true
        cancelAudio()
        show(VoicePhase.PAUSED)
    }
    private fun suspendForScreen() {
        prompt = null
        privacy.invalidateScreen()
        cancelAudio()
        if (!manuallyPaused) show(if (privacy.needsActivation()) VoicePhase.PRIVATE else VoicePhase.WAITING)
    }
    private fun update(next: String, microphoneAllowed: Boolean) {
        if (!valid()) { cancelAudio(); show(VoicePhase.UNAVAILABLE); return }
        prompt = next
        privacy.observe(microphoneAllowed)
        if (manuallyPaused) return
        cancelAudio()
        if (!privacy.canListen()) {
            if (next != lastSpoken) say(next, listenAfter = false) else show(VoicePhase.PRIVATE)
            return
        }
        if (next != lastSpoken) say(next) else listen()
    }
    private fun acquireAudio(): Boolean {
        if (!valid() || manuallyPaused) return false
        if (!focusHeld) focusHeld = audio.requestAudioFocus(focus) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        if (!focusHeld) pause()
        return focusHeld
    }
    private fun say(text: String, listenAfter: Boolean = true) {
        cancelAudio()
        if (!acquireAudio()) return
        val token = turns.begin()
        show(VoicePhase.SPEAKING)
        handler.postDelayed({ if (turns.accepts(token)) pause() }, 30_000)
        tts?.speak(text, language) { success ->
            if (!turns.accepts(token) || !valid() || manuallyPaused) return@speak
            handler.removeCallbacksAndMessages(null)
            if (success) {
                lastSpoken = prompt
                if (listenAfter) listen() else { cancelAudio(); show(VoicePhase.PRIVATE) }
            }
            else { manuallyPaused = true; cancelAudio(); show(VoicePhase.UNAVAILABLE) }
        }
    }

    private fun listen() {
        cancelAudio()
        if (!privacy.canListen()) { show(VoicePhase.PRIVATE); return }
        if (prompt == null || !acquireAudio()) return
        if (!supported(this)) { show(VoicePhase.UNAVAILABLE); return }
        val token = turns.begin()
        val presentation = SaathiSession.presentationKey()
        show(VoicePhase.STARTING)
        try {
            if (Build.VERSION.SDK_INT < 31) { pause(); return }
            val engine = SpeechRecognizer.createOnDeviceSpeechRecognizer(this)
            recognizer = engine
            engine.setRecognitionListener(object : RecognitionListener {
                private fun current() = turns.accepts(token) && valid() && !manuallyPaused && privacy.canListen()
                override fun onReadyForSpeech(params: Bundle?) { if (current()) show(VoicePhase.LISTENING) }
                override fun onResults(results: Bundle) {
                    if (!current()) return
                    val text = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                    cancelAudio()
                    val command = VoiceCommands.parse(text)
                    when (command) {
                        VoiceCommand.STOP -> SaathiSession.stopForSession(key)
                        VoiceCommand.PAUSE -> pause()
                        VoiceCommand.REPEAT, VoiceCommand.HELP -> prompt?.let { say(it) }
                        VoiceCommand.ACKNOWLEDGE -> say(GuidanceCopy.acknowledged(language))
                        VoiceCommand.UNKNOWN -> {
                            // Retain only a validated option as the session's in-memory goal.
                            if (SensitiveContent.isSensitive(false, text)) pause()
                            else {
                                val option = VoiceRequests.option(text)
                                val changed = option != null && presentation != null &&
                                    SaathiSession.changeLiveRequest(option, key, presentation)
                                // An accepted change waits for a fresh snapshot to supply the next prompt.
                                if (!changed) say(GuidanceCopy.voiceFallback(language, SaathiSession.isLive()))
                            }
                        }
                    }
                }
                override fun onError(error: Int) {
                    if (!current()) return
                    if (error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE || error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED) {
                        manuallyPaused = true; cancelAudio(); show(VoicePhase.NEEDS_LANGUAGE)
                    } else pause()
                }
                override fun onBeginningOfSpeech() = Unit
                override fun onEndOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
            handler.postDelayed({ if (turns.accepts(token)) pause() }, 30_000)
            engine.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.sttTag)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            })
        } catch (_: RuntimeException) { manuallyPaused = true; cancelAudio(); show(VoicePhase.UNAVAILABLE) }
    }

    private fun show(phase: VoicePhase) {
        if (mutablePhase.value == phase) return
        mutablePhase.value = phase
        if (key != null) getSystemService(NotificationManager::class.java).notify(NOTICE, notification())
    }
    private fun notification(): Notification {
        fun action(name: String, requestCode: Int) = PendingIntent.getService(this, requestCode,
            Intent(this, VoiceConversationService::class.java).setAction(name)
                .setData(android.net.Uri.parse("saathi://voice/$key/$name")).putExtra(KEY, key),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val open = PendingIntent.getActivity(this, 42, Intent(this, AssistantActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, CHANNEL).setSmallIcon(com.saathi.R.drawable.ic_saathi_mark)
            .setContentTitle("Saathi · " + mutablePhase.value.text(language))
            .setContentText("Local voice guidance · Open Saathi to resume")
            .setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_media_pause, "Pause voice", action(PAUSE, 43))
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop guidance", action(STOP, 44)).build()
    }
    override fun onDestroy() {
        cancelAudio()
        tts?.release()
        unregisterReceiver(interruption)
        stopForeground(STOP_FOREGROUND_REMOVE)
        if (instance === this) {
            instance = null
            if (mutablePhase.value != VoicePhase.UNAVAILABLE) mutablePhase.value = VoicePhase.OFF
        }
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val START = "voice.start"
        private const val STOP = "voice.stop"
        private const val PAUSE = "voice.pause"
        private const val KEY = "session"
        private const val LANGUAGE = "language"
        private const val RATE = "rate"
        private const val CHANNEL = "saathi_voice_guidance"
        private const val NOTICE = 41
        private var instance: VoiceConversationService? = null
        private val mutablePhase = MutableStateFlow(VoicePhase.OFF)
        val phase: StateFlow<VoicePhase> = mutablePhase
        fun supported(context: Context): Boolean = Build.VERSION.SDK_INT >= 31 &&
            runCatching { SpeechRecognizer.isOnDeviceRecognitionAvailable(context) }.getOrDefault(false)
        /** Must be called by a visible activity after the user explicitly enables voice. */
        fun start(context: Context, language: GuidanceLanguage, rate: Float) {
            val session = SaathiSession.sessionKey() ?: return
            context.startForegroundService(Intent(context, VoiceConversationService::class.java).setAction(START)
                .putExtra(KEY, session).putExtra(LANGUAGE, language.storageValue).putExtra(RATE, rate))
        }
        fun update(prompt: String, microphoneAllowed: Boolean) { instance?.update(prompt, microphoneAllowed) }
        fun suspendForScreenChange() { instance?.suspendForScreen() }
        fun isRunning() = instance?.key != null
        fun stop(context: Context) {
            instance?.manuallyPaused = true
            instance?.cancelAudio()
            mutablePhase.value = VoicePhase.OFF
            context.stopService(Intent(context, VoiceConversationService::class.java))
        }
    }
}
