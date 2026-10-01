package com.saathi.speech

import org.junit.Assert.*
import org.junit.Test

class VoiceRequestsTest {
    @Test fun explicitOptionsWorkInThreePilotLanguages() {
        listOf("find Help", "Please show Help!", "open Help", "Help dhundo", "Help dikhao", "Help kholo").forEach {
            assertEquals(it, "Help", VoiceRequests.option(it))
        }
        listOf("मदद खोजो", "मदद दिखाओ", "खोलो मदद", "मदद खोलो।").forEach {
            assertEquals(it, "मदद", VoiceRequests.option(it))
        }
        assertEquals("About us", VoiceRequests.option("find “About us”"))
    }

    @Test fun conversationAndControlsDoNotReplaceTheTask() {
        listOf("Help", "stop", "done", "I want to find Help", "do not open Help", "thanks", "find", "find ", "find\nHelp").forEach {
            assertNull(it, VoiceRequests.option(it))
        }
    }

    @Test fun secretsAndConsequentialOptionsAreRejected() {
        listOf("find PIN", "find 123456", "open Delete", "show Pay", "find Allow", "भुगतान खोजो", "find " + "a".repeat(81)).forEach {
            assertNull(it, VoiceRequests.option(it))
        }
    }
}
