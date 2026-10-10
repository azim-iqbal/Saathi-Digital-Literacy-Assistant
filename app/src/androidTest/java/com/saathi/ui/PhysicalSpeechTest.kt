package com.saathi.ui

import android.speech.*
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.saathi.MainActivity
import com.saathi.language.GuidanceLanguage
import com.saathi.speech.TtsManager
import com.saathi.speech.VoiceConversationService
import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Explicitly authorized device speech probe. No raw audio or transcript is saved. */
class PhysicalSpeechTest {
    @Test fun physicalEngineCallbacksAndBoundedRecognition() {
        val inst=InstrumentationRegistry.getInstrumentation()
        assertEquals("true",InstrumentationRegistry.getArguments().getString("physicalDeviceConfirmed"))
        val context=inst.targetContext
        val result=JSONObject().put("model",android.os.Build.MODEL).put("sdk",android.os.Build.VERSION.SDK_INT)
        fun main(action:()->Unit)=inst.runOnMainSync(action)
        ActivityScenario.launch(MainActivity::class.java).use {
            for(language in GuidanceLanguage.entries) {
                val latch=CountDownLatch(1);var manager:TtsManager?=null
                var outcome=false
                try {
                    main {
                        manager=TtsManager(context)
                        manager!!.speak(if(language==GuidanceLanguage.HINDI) "यह साथी का आवाज़ परीक्षण है।" else if(language==GuidanceLanguage.HINGLISH) "Yeh Saathi ka voice test hai." else "This is Saathi's voice test.",language) { outcome=it;latch.countDown() }
                    }
                    val callback=latch.await(20,TimeUnit.SECONDS)
                    result.put(language.name,JSONObject().put("callback",callback).put("synthesisSuccess",outcome))
                    assertTrue("Speech must report completion or unavailability",callback)
                } finally { main { manager?.release() } }
            }
            val supported=VoiceConversationService.supported(context)
            result.put("onDeviceRecognitionAvailable",supported)
            if(supported && android.os.Build.VERSION.SDK_INT>=31) {
                val latch=CountDownLatch(1);var recognizer:SpeechRecognizer?=null
                try {
                    main {
                        recognizer=SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                        recognizer!!.setRecognitionListener(object:RecognitionListener {
                            override fun onReadyForSpeech(params:android.os.Bundle?) { result.put("recognizerReady",true) }
                            override fun onError(error:Int) { result.put("recognizerError",error);latch.countDown() }
                            override fun onResults(results:android.os.Bundle?) {
                                result.put("nonemptyResult",!results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).isNullOrEmpty());latch.countDown()
                            }
                            override fun onBeginningOfSpeech()=Unit
                            override fun onEndOfSpeech()=Unit
                            override fun onRmsChanged(rmsdB:Float)=Unit
                            override fun onBufferReceived(buffer:ByteArray?)=Unit
                            override fun onPartialResults(results:android.os.Bundle?)=Unit
                            override fun onEvent(eventType:Int,params:android.os.Bundle?)=Unit
                        })
                        recognizer!!.startListening(android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE,"en-IN"))
                    }
                    result.put("recognitionOutcomeWithin15s",latch.await(15,TimeUnit.SECONDS))
                } finally { main { recognizer?.cancel();recognizer?.destroy() } }
            }
        }
        result.put("scope","Engine callbacks only; no human intelligibility/transcript accuracy claim; no audio/transcripts saved")
        val output=java.io.File(context.filesDir,"samsung-speech").apply { mkdirs() }
        java.io.File(output,"result.json").writeText(result.toString(2))
    }
}
