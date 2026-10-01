package com.saathi.speech

import org.junit.Assert.*
import org.junit.Test

class VoiceTurnGateTest {
    @Test fun stoppedSpeechCannotReopenMicrophone() {
        val gate = VoiceTurnGate()
        val speech = gate.begin()
        gate.cancel()
        assertFalse(gate.accepts(speech))
        val replacement = gate.begin()
        assertFalse(gate.accepts(speech))
        assertTrue(gate.accepts(replacement))
    }
    @Test fun oldRecognitionCannotStopOrAnswerNewTurn() {
        val gate = VoiceTurnGate()
        val old = gate.begin()
        val current = gate.begin()
        assertFalse(gate.accepts(old))
        assertTrue(gate.accepts(current))
    }
    @Test fun controlsWorkInThreePilotLanguages() {
        listOf("Stop!", "रद्द", "band karo").forEach { assertEquals(VoiceCommand.STOP, VoiceCommands.parse(it)) }
        listOf("pause", "रोकें", "thoda ruko").forEach { assertEquals(VoiceCommand.PAUSE, VoiceCommands.parse(it)) }
        listOf("repeat", "दोबारा", "phir bolo").forEach { assertEquals(VoiceCommand.REPEAT, VoiceCommands.parse(it)) }
        listOf("help", "मदद", "ab kya").forEach { assertEquals(VoiceCommand.HELP, VoiceCommands.parse(it)) }
        listOf("done", "समझ गया", "ho gaya").forEach { assertEquals(VoiceCommand.ACKNOWLEDGE, VoiceCommands.parse(it)) }
    }
    @Test fun arbitrarySpeechDoesNotBecomeAControlOrTaskChange() {
        listOf("unstoppable", "do not stop", "my PIN is 1234", "buy a ticket").forEach {
            assertEquals(VoiceCommand.UNKNOWN, VoiceCommands.parse(it))
        }
    }
}
