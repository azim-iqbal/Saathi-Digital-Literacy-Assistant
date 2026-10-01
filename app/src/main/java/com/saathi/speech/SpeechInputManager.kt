package com.saathi.speech

import android.content.Context
import android.content.Intent
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class SpeechInputManager(
    private val context: Context,
    private val onText: (String) -> Unit,
    private val onLowConfidence: () -> Unit,
    private val onError: () -> Unit
) : RecognitionListener {
    private val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
    init { recognizer.setRecognitionListener(this) }
    fun listen(language: String) {
        runCatching { recognizer.cancel() }
        recognizer.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Tell Saathi what you want to do")
        })
    }
    fun destroy() = recognizer.destroy()
    override fun onResults(results: android.os.Bundle) {
        val confidence = results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)?.firstOrNull()
        val text = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
        if (text.isBlank() || (confidence != null && confidence < 0.55f)) onLowConfidence() else onText(text)
    }
    override fun onError(error: Int) = onError()
    override fun onReadyForSpeech(params: android.os.Bundle?) = Unit
    override fun onBeginningOfSpeech() = Unit
    override fun onRmsChanged(rmsdB: Float) = Unit
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onEndOfSpeech() = Unit
    override fun onPartialResults(partialResults: android.os.Bundle?) = Unit
    override fun onEvent(eventType: Int, params: android.os.Bundle?) = Unit
}
