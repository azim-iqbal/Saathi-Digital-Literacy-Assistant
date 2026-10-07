package com.saathi.core
import org.junit.Assert.*
import org.junit.Test
class WindowObservationPolicyTest {
    @Test fun keyboardWindowsAndInstalledInputMethodsStayLocalWithoutBlockingTheUnderlyingApp() {
        val keyboards=setOf("third.party.editor","com.android.inputmethod.latin")
        assertFalse(WindowObservationPolicy.allows(2,"unknown.keyboard",emptySet()))
        assertFalse(WindowObservationPolicy.allows(null,"third.party.editor",keyboards))
        assertFalse(WindowObservationPolicy.allows(1,"com.android.inputmethod.latin",keyboards))
        assertTrue(WindowObservationPolicy.allows(1,"com.android.chrome",keyboards))
        assertTrue(WindowObservationPolicy.allows(null,"travel.app",keyboards))
    }
}
