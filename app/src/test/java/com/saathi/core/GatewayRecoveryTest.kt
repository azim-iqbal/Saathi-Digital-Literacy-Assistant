package com.saathi.core

import com.saathi.language.GuidanceLanguage
import org.junit.Assert.*
import org.junit.Test

class GatewayRecoveryTest {
    @Test fun unknownReasonsNeverDisplayServerContent() {
        val secret = "provider returned token=private"
        assertEquals("not_verified", GatewayRecovery.reason(secret))
        assertEquals("not_verified", GatewayRecovery.reason(null))
        GuidanceLanguage.entries.forEach {
            assertFalse(GatewayRecovery.message(secret, it).contains("private"))
            assertTrue(GatewayRecovery.message("quota_exhausted", it).isNotBlank())
        }
    }
    @Test fun httpFailuresHaveDistinctRecoveryPaths() {
        assertEquals("unauthorized", GatewayRecovery.httpStatus(401))
        assertEquals("timeout", GatewayRecovery.httpStatus(504))
        assertEquals("busy", GatewayRecovery.httpStatus(429))
        assertEquals("invalid_request", GatewayRecovery.httpStatus(413))
        assertEquals("unavailable", GatewayRecovery.httpStatus(302))
        assertNotEquals(GatewayRecovery.message("provider_auth", GuidanceLanguage.ENGLISH),
            GatewayRecovery.message("timeout", GuidanceLanguage.ENGLISH))
    }
    @Test fun phoneLocalServerFailureExplainsUsbAndHostedOptions() {
        val message = GatewayRecovery.message("local_backend_unreachable", GuidanceLanguage.ENGLISH)
        assertTrue(message.contains("USB")); assertTrue(message.contains("HTTPS"))
        assertNotEquals(message, GatewayRecovery.message("provider_timeout", GuidanceLanguage.ENGLISH))
        assertTrue(GatewayRecovery.message("circuit_open", GuidanceLanguage.ENGLISH).contains("30 seconds"))
    }
}
