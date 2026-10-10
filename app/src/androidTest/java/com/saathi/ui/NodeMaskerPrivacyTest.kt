package com.saathi.ui

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saathi.accessibility.NodeMasker
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NodeMaskerPrivacyTest {
    @Test fun privateOccupancyMustNotBeDerivedFromValue() {
        @Suppress("DEPRECATION") val node = AccessibilityNodeInfo.obtain()
        try {
            node.setBoundsInScreen(Rect(10, 20, 200, 90))
            node.isVisibleToUser = true
            node.isEditable = true
            node.isPassword = true
            node.text = "synthetic-secret"
            val masked = NodeMasker.flatten(node).single()
            assertNull(masked.text)
            assertFalse(masked.valueKnown)
            assertTrue(masked.structuralPrivateField)
            assertFalse("Private occupancy must not survive masking", masked.hasValue)
        } finally { @Suppress("DEPRECATION") node.recycle() }
    }
    @Test fun structureOnlyMarkerIsUniqueAndNeverConfirmsCompletion() {
        val field = com.saathi.core.UiNode(Rect(10, 20, 200, 90), null, null, null, null,
            "EditText", true, true, true, isSensitive = true, isEditable = true,
            valueKnown = false, structuralPrivateField = true)
        val target = com.saathi.core.ScreenInterruption.privateTarget(listOf(field))
        assertNotNull(target); assertNull(target!!.resourceId)
        assertEquals("Private field", target.description)
        assertNull(com.saathi.core.ScreenInterruption.privateTarget(listOf(field, field.copy(bounds = Rect(10, 100, 200, 170)))))
        assertNull(com.saathi.core.ScreenInterruption.privateTarget(listOf(field.copy(isEnabled = false))))
        assertNull(com.saathi.core.ScreenInterruption.privateTarget(listOf(field.copy(privateContext = true))))
        assertNull(com.saathi.core.ScreenInterruption.privateTarget(listOf(field.copy(structuralPrivateField = false))))
        val challenge = field.copy(text = "Verify you are human", isSensitive = false, isPassword = false, structuralPrivateField = false)
        assertNull(com.saathi.core.ScreenInterruption.privateTarget(listOf(field, challenge)))
        val gate = com.saathi.core.ObservationGate().apply { start() }
        val ticket = gate.observe("example.browser", 1)!!
        assertNull(com.saathi.core.LiveAiPolicy.snapshot(ticket, listOf(field), "Help", "en-IN", System.currentTimeMillis(), emptyList()))
    }
    @Test fun privateContextDiscardsNonNumericMessageAndUnknownFieldDoesNotBecomeBlankEvidence() {
        @Suppress("DEPRECATION") val node = AccessibilityNodeInfo.obtain()
        try {
            node.setBoundsInScreen(Rect(10, 20, 200, 90)); node.isVisibleToUser = true
            node.text = "Inbox fictional-private-message"
            node.isContentInvalid = true; node.inputType = 1
            val masked = NodeMasker.flatten(node).single()
            assertTrue(masked.privateContext); assertNull(masked.text); assertNull(masked.description)
            assertNull(masked.resourceId); assertFalse(masked.hasValue)
            assertFalse(masked.contentInvalid); assertFalse(masked.requiredField); assertEquals(0, masked.inputType)
            assertEquals(com.saathi.core.ScreenInterruption.Reason.PRIVATE, com.saathi.core.ScreenInterruption.reason(listOf(masked)))
            node.text = "fictional-entered-value"; node.isEditable = true; node.hintText = "Destination"
            val editable = NodeMasker.flatten(node).single()
            assertNull(editable.text); assertFalse(editable.valueKnown)
            assertNull(com.saathi.core.NavigationHint("FIELD_LABEL", "Destination").targetIndex(listOf(editable)))
        } finally { @Suppress("DEPRECATION") node.recycle() }
    }

}
