package com.saathi.core

import org.junit.Assert.*
import org.junit.Test

class GatewayEndpointTest {
    @Test fun unconfiguredReleaseStaysOfflineAndOnlyDebugUsesLoopback() {
        assertNull(GatewayEndpoint.resolve("", false))
        assertEquals("http://127.0.0.1:8765", GatewayEndpoint.resolve("", true))
    }
    @Test fun configuredOriginRequiresHttpsAndCannotCarryCredentialsOrRedirectParameters() {
        assertEquals("https://api.saathi.example:8443", GatewayEndpoint.resolve("https://api.saathi.example:8443/", false))
        for (value in listOf("http://api.saathi.example", "https://user:password@api.saathi.example", "https://api.saathi.example/path",
            "https://api.saathi.example?key=secret", "https://api.saathi.example#redirect", "https://127.0.0.1", "https://localhost",
            "https://host.local", "https://host.localhost", "https://api.saathi.example:0", "https://api.saathi.example:65536",
            " https://api.saathi.example", "https://api..example", "https://api.example/../")) {
            assertNull(value, GatewayEndpoint.resolve(value, false))
            assertNull(value, GatewayEndpoint.resolve(value, true))
        }
    }
}
