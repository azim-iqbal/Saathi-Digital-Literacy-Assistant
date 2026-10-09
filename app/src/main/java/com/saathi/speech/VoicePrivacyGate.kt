package com.saathi.speech

/** A safe screen cannot renew microphone consent after a private/challenge handoff. */
internal class VoicePrivacyGate {
    private var needsActivation = true
    private var safeScreen = false
    fun activate() { needsActivation = false; safeScreen = false }
    fun observe(microphoneAllowed: Boolean) {
        safeScreen = microphoneAllowed
        if (!microphoneAllowed) needsActivation = true
    }
    fun invalidateScreen() { safeScreen = false }
    fun canListen() = !needsActivation && safeScreen
    fun needsActivation() = needsActivation
}
