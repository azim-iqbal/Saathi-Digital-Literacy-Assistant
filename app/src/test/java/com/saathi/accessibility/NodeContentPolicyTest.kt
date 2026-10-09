package com.saathi.accessibility

import org.junit.Assert.*
import org.junit.Test

class NodeContentPolicyTest {
    @Test fun privateMetadataNeverCallsValueOrDescriptionGetters() {
        for ((password, hint, id, type) in listOf(
            listOf(true, null, null, 0), listOf(false, "OTP", null, 0),
            listOf(false, null, "field/password", 0), listOf(false, "पिन", null, 0),
            listOf(false, "ＣＶＶ", null, 0), listOf(false, null, null, 0x81),
            listOf(false, null, null, 0x91), listOf(false, null, null, 0xe1),
            listOf(false, null, null, 0x12))) {
            val result = NodeContentPolicy.read(password as Boolean, true, hint as String?, id as String?, type as Int, false,
                { error("Private text getter accessed") }, { error("Private description getter accessed") })
            assertTrue(result.sensitive); assertTrue(result.structuralPrivateField)
            assertFalse(result.hasValue); assertFalse(result.valueKnown)
            assertNull(result.text); assertNull(result.description)
        }
    }
    @Test fun unlabelledEditorsNeverReadValuesAndAbsenceOfHintIsUnknown() {
        for (showing in listOf(false, true)) {
            val result = NodeContentPolicy.read(false, true, "Destination", null, 1, showing,
                { error("Editable value getter accessed") }, { error("Editable description getter accessed") })
            assertFalse(result.hasValue); assertEquals(showing, result.valueKnown)
            assertNull(result.text); assertNull(result.description)
        }
    }
    @Test fun staticSecretsStillRedactedWhilePublicDatesAndPricesSurvive() {
        for (value in listOf("OTP 582139", "card number 4111 1111 1111 1111", "password synthetic-secret", "ＣＶＶ 123", "pass\u200Bword fictional")) {
            val result = NodeContentPolicy.read(false, false, null, null, 0, false, { value }, { null })
            assertTrue(result.sensitive); assertFalse(result.structuralPrivateField)
            assertFalse(result.hasValue); assertNull(result.text)
        }
        for (value in listOf("To", "01/10/2026", "₹5,221")) {
            val result = NodeContentPolicy.read(false, false, null, null, 0, false, { value }, { null })
            assertFalse(result.sensitive); assertEquals(value, result.text)
        }
    }
}
