package com.saathi.core

import android.graphics.Rect
import org.junit.Assert.*
import org.junit.Test

class ScreenInterruptionTest {
    private fun node(text: String?) = UiNode(Rect(), text, null, null, null, "TextView", false, true, false)
    @Test fun `private state survives removal of raw field values`() {
        assertEquals(ScreenInterruption.Reason.PRIVATE, ScreenInterruption.reason(listOf(node(null).copy(isSensitive = true))))
    }
    @Test fun `challenge handover disappears only with a fresh clear tree`() {
        for (label in listOf("Complete CAPTCHA", "Verify you are human", "I'm not a robot", "कैप्चा")) {
            assertEquals(ScreenInterruption.Reason.CAPTCHA, ScreenInterruption.reason(listOf(node(label))))
        }
        assertNull(ScreenInterruption.reason(listOf(node("Help"))))
        assertNull(ScreenInterruption.reason(emptyList()))
    }
    @Test fun `ordinary navigation and dates are not authentication`() {
        for (label in listOf("To", "Help", "Application status", "01/10/2026", "₹5,221"))
            assertNull(ScreenInterruption.reason(listOf(node(label))))
    }
}
