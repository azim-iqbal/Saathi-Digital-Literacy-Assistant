package com.saathi.core

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

class GatewayHttpTransportTest {
    private class Wire : HttpURLConnection(URL("http://127.0.0.1:8765")) {
        var connectError: IOException? = null
        var writeError: IOException? = null
        var closed = false
        var status = 200
        var content = "application/json"
        var response: InputStream = ByteArrayInputStream("{}".toByteArray())
        val sent = ByteArrayOutputStream()
        override fun connect() { connectError?.let { throw it } }
        override fun disconnect() { closed = true }
        override fun usingProxy() = false
        override fun getOutputStream() = writeError?.let { throw it } ?: sent
        override fun getInputStream() = response
        override fun getResponseCode() = status
        override fun getContentType() = content
    }
    private fun post(t: GatewayHttpTransport, origins: List<String> = listOf("http://127.0.0.1:8765"),
                     current: () -> Boolean = { true }, connected: (String) -> Unit = {}) =
        t.post(origins, "/v1/provider-check", "test-token", "{}".toByteArray(), 100, current, {}, connected)

    @Test fun physicalPhoneAndHostedReleaseNeverTryEmulatorAddress() {
        val loop = "http://127.0.0.1:8765"
        assertEquals(listOf(loop), GatewayEndpoint.candidates(loop, true, false))
        assertEquals(listOf(loop), GatewayEndpoint.candidates(loop, false, true))
        assertEquals(listOf(loop, "http://10.0.2.2:8765"), GatewayEndpoint.candidates(loop, true, true))
        assertEquals(listOf("https://saathi.example"), GatewayEndpoint.candidates("https://saathi.example", true, true))
    }
    @Test fun refusedConnectCanDiscoverEmulatorWithoutSendingFirstPost() {
        val first = Wire().apply { connectError = ConnectException() }; val second = Wire()
        val opened = mutableListOf<String>(); var destination = ""
        val t = GatewayHttpTransport({ opened.add(it); if (opened.size == 1) first else second })
        assertEquals(200, post(t, listOf("http://127.0.0.1:8765", "http://10.0.2.2:8765"), connected = { destination = it }).status)
        assertEquals(0, first.sent.size()); assertEquals("{}", second.sent.toString())
        assertTrue(first.closed && second.closed); assertEquals("http://10.0.2.2:8765", destination)
    }
    @Test fun writeFailureNeverReplaysPossiblySubmittedProviderCall() {
        val wire = Wire().apply { writeError = IOException("partial write") }; var attempts = 0
        val t = GatewayHttpTransport({ attempts++; wire })
        assertThrows(IOException::class.java) { post(t, listOf("http://127.0.0.1:8765", "http://10.0.2.2:8765")) }
        assertEquals(1, attempts); assertTrue(wire.closed)
    }
    @Test fun connectionCancellationDoesNotDispatchOrTryAnotherOrigin() {
        var active = true; var attempts = 0
        val wire = Wire().apply { connectError = ConnectException() }
        val t = GatewayHttpTransport({ attempts++; active = false; wire })
        assertThrows(IOException::class.java) { post(t, listOf("a", "b"), current = { active }) }
        assertEquals(1, attempts); assertEquals(0, wire.sent.size()); assertTrue(wire.closed)
    }
    @Test fun slowBodyHasOneDeadlineAcrossReadsAndReleasesConnection() {
        var time = 0L
        val wire = Wire().apply {
            response = object : InputStream() {
                override fun read(): Int { time += 60; return 32 }
                override fun read(b: ByteArray, off: Int, len: Int): Int { time += 60; b[off] = 32; return 1 }
            }
        }
        assertThrows(SocketTimeoutException::class.java) { post(GatewayHttpTransport({ wire }, { time })) }
        assertTrue(wire.closed); assertTrue(time < 200)
    }
    @Test fun zeroMissingResponseAndOversizedOrWrongBodiesAreRejected() {
        val noResponse = Wire().apply { status = -1 }
        assertThrows(IOException::class.java) { post(GatewayHttpTransport({ noResponse })) }
        for (wire in listOf(Wire().apply { content = "text/html" }, Wire().apply { response = ByteArrayInputStream(ByteArray(8193)) })) {
            assertThrows(IllegalArgumentException::class.java) { post(GatewayHttpTransport({ wire })) }
            assertTrue(wire.closed)
        }
        assertTrue(noResponse.closed)
    }
    @Test fun redirectsAndUnauthorizedResponsesDoNotReadOrReplayBodies() {
        for (status in listOf(302, 401, 429, 503)) {
            val wire = Wire().apply { this.status = status }
            var count = 0
            assertEquals(status, post(GatewayHttpTransport({ count++; wire })).status)
            assertEquals(1, count); assertTrue(wire.closed); assertFalse(wire.instanceFollowRedirects)
        }
    }
    @Test fun failureDoesNotPoisonNextRequestRoute() {
        val first = Wire().apply { connectError = ConnectException() }; var count = 0
        val t = GatewayHttpTransport({ count++; if (count == 1) first else Wire() })
        assertThrows(ConnectException::class.java) { post(t) }
        assertEquals(200, post(t).status)
    }
    @Test fun debugOverridesRejectCredentialBearingAndUntrustedCleartextOrigins() {
        assertEquals("http://127.0.0.1:8767", GatewayEndpoint.debugOverride("http://127.0.0.1:8767/"))
        assertEquals("https://saathi.example", GatewayEndpoint.debugOverride("https://saathi.example"))
        for (value in listOf("http://saathi.example", "http://127.0.0.1:8765/path", "https://secret@saathi.example", "http://127.0.0.1:8765?key=secret"))
            assertNull(GatewayEndpoint.debugOverride(value))
    }
}
