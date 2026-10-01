package com.saathi.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GuidancePolicyTest {
    @Test fun `known payment apps are marked as restricted`() {
        assertEquals("Google Pay", GuidancePolicy.restrictedAppName("com.google.android.apps.nbu.paisa.user"))
        assertEquals("PhonePe", GuidancePolicy.restrictedAppName("com.phonepe.app"))
        assertEquals("Paytm", GuidancePolicy.restrictedAppName("net.one97.paytm"))
    }

    @Test fun `browser packages stay eligible for website guidance`() {
        assertNull(GuidancePolicy.restrictedAppName("com.android.chrome"))
        assertEquals(true, GuidancePolicy.isBrowser("com.sec.android.app.sbrowser"))
    }
}
