package com.saathi.ui

import android.Manifest
import android.os.Bundle
import android.speech.*
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.saathi.MainActivity
import com.saathi.language.GuidanceLanguage
import com.saathi.speech.TtsManager
import com.saathi.speech.VoiceConversationService
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Real installed engine callbacks on a muted emulator; not intelligibility or transcript accuracy. */
class SpeechEngineSmokeTest {
    @Test fun installedEnginesReturnAnExplicitOutcomeAndRelease() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        assertTrue(android.os.Build.FINGERPRINT.contains("generic") || android.os.Build.MODEL.startsWith("sdk_"))
        fun main(block: () -> Unit) = instrumentation.runOnMainSync(block)
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.RECORD_AUDIO)
        val results = mutableListOf<String>()
        ActivityScenario.launch(MainActivity::class.java).use {
            for (language in GuidanceLanguage.entries) {
                val done = CountDownLatch(1); var success = false; var tts: TtsManager? = null
                try {
                    main {
                        tts = TtsManager(context)
                        tts!!.speak(if (language == GuidanceLanguage.HINDI) "यह साथी का अभ्यास है।" else "This is a Saathi practice test.", language) {
                            success = it; done.countDown()
                        }
                    }
                    assertTrue("TTS must complete or report unavailable", done.await(20, TimeUnit.SECONDS))
                    results += "${language.name}: ttsCompletionSuccess=$success"
                } finally { main { tts?.release() } }
            }
            var recognizer: SpeechRecognizer? = null
            try {
                val outcome = CountDownLatch(1)
                val supported = VoiceConversationService.supported(context)
                results += "onDeviceRecognitionAvailable=$supported"
                if (supported && android.os.Build.VERSION.SDK_INT >= 31) {
                    main {
                        recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                        recognizer!!.setRecognitionListener(object : RecognitionListener {
                            override fun onReadyForSpeech(params: Bundle?) { results += "recognizerReady=true"; outcome.countDown() }
                            override fun onError(error: Int) { results += "recognizerError=$error"; outcome.countDown() }
                            override fun onResults(results: Bundle?) { outcome.countDown() }
                            override fun onBeginningOfSpeech() = Unit
                            override fun onEndOfSpeech() = Unit
                            override fun onRmsChanged(rmsdB: Float) = Unit
                            override fun onBufferReceived(buffer: ByteArray?) = Unit
                            override fun onPartialResults(partialResults: Bundle?) = Unit
                            override fun onEvent(eventType: Int, params: Bundle?) = Unit
                        })
                        recognizer!!.startListening(android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN"))
                    }
                    assertTrue("Recognizer must become ready or report an explicit error", outcome.await(15, TimeUnit.SECONDS))
                }
            } finally { main { recognizer?.cancel(); recognizer?.destroy() } }
        }
        val output = File(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir") ?: context.filesDir.path).apply { mkdirs() }
        File(output, "speech-engine-result.txt").writeText(results.joinToString("\n") + "\nNo human audio supplied or recorded by test. Emulator muted; quality, transcript accuracy and physical-device behavior unverified.\n")
    }
}
