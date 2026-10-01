package com.saathi.speech

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.saathi.language.GuidanceLanguage

/** All engine callbacks return to main and carry their own utterance identity. */
class TtsManager(
    context: Context,
    private val speechRate: Float = DEFAULT_SPEECH_RATE,
    private val onHindiVoiceMissing: (() -> Unit)? = null
) {
    private data class Pending(val text: String, val language: GuidanceLanguage, val done: ((Boolean) -> Unit)?)
    private val handler = Handler(Looper.getMainLooper())
    private val gate = VoiceTurnGate()
    private var ready = false
    private var failed = false
    private var released = false
    private var queued: Pending? = null
    private var completion: ((Boolean) -> Unit)? = null
    private var prompted = false
    private val tts = TextToSpeech(context.applicationContext) { result ->
        handler.post {
            if (!released) {
                ready = result == TextToSpeech.SUCCESS
                failed = !ready
                val pending = queued
                queued = null
                if (pending != null) speak(pending.text, pending.language, pending.done)
            }
        }
    }
    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) = Unit
            override fun onDone(utteranceId: String) = finish(utteranceId, true)
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String) = finish(utteranceId, false)
            override fun onError(utteranceId: String, errorCode: Int) = finish(utteranceId, false)
        })
    }
    private fun finish(id: String, success: Boolean) {
        handler.post {
            val token = id.toLongOrNull() ?: return@post
            if (released || !gate.accepts(token)) return@post
            gate.cancel()
            val callback = completion
            completion = null
            callback?.invoke(success)
        }
    }
    fun speak(text: String, language: String) = speak(text, GuidanceLanguage.fromApiTag(language))
    fun speak(text: String, language: GuidanceLanguage, onFinished: ((Boolean) -> Unit)? = null) {
        if (released || failed) { onFinished?.invoke(false); return }
        if (!ready) { queued = Pending(text, language, onFinished); return }
        stop()
        val availability = tts.setLanguage(language.ttsLocale)
        if (availability < TextToSpeech.LANG_AVAILABLE) {
            if (!prompted && language != GuidanceLanguage.ENGLISH) { prompted = true; onHindiVoiceMissing?.invoke() }
            onFinished?.invoke(false)
            return
        }
        tts.setSpeechRate(speechRate.coerceIn(MIN_SPEECH_RATE, MAX_SPEECH_RATE))
        val token = gate.begin()
        completion = onFinished
        if (tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, token.toString()) == TextToSpeech.ERROR) finish(token.toString(), false)
    }
    fun stop() { queued = null; completion = null; gate.cancel(); tts.stop() }
    fun release() { released = true; stop(); handler.removeCallbacksAndMessages(null); tts.shutdown() }
    companion object {
        const val MIN_SPEECH_RATE = 0.75f
        const val DEFAULT_SPEECH_RATE = 0.9f
        const val MAX_SPEECH_RATE = 1.25f
    }
}
