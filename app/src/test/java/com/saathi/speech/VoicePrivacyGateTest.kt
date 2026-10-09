package com.saathi.speech

import org.junit.Assert.*
import org.junit.Test

class VoicePrivacyGateTest {
    @Test fun privateToSafeCannotReopenMicrophoneWithoutExplicitActivation() {
        val gate = VoicePrivacyGate()
        gate.observe(true); assertFalse(gate.canListen())
        gate.activate(); assertFalse(gate.canListen())
        gate.observe(true); assertTrue(gate.canListen())
        gate.observe(false); assertFalse(gate.canListen())
        repeat(1000) { gate.invalidateScreen(); gate.observe(true); assertFalse(gate.canListen()) }
        gate.activate(); assertFalse(gate.canListen())
        gate.observe(true); assertTrue(gate.canListen())
    }
    @Test fun activationOnPrivateScreenDoesNotAuthorizeFutureSafeListening() {
        val gate = VoicePrivacyGate()
        gate.activate(); gate.observe(false); gate.observe(true)
        assertFalse(gate.canListen())
    }
    @Test fun ordinaryScreenChangeRequiresFreshObservationWithoutAddingNewConsent() {
        val gate = VoicePrivacyGate()
        gate.activate(); gate.observe(true); gate.invalidateScreen()
        assertFalse(gate.canListen())
        gate.observe(true); assertTrue(gate.canListen())
    }
}
