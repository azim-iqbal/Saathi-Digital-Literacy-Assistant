package com.saathi.ui

import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saathi.core.GatewayResult
import com.saathi.gateway.ConnectionCodec
import com.saathi.gateway.PracticeGateway
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Requires the host's explicitly synthetic REST-adapter fixture. Never uses real keys. */
@RunWith(AndroidJUnit4::class)
class ConnectionCheckTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    @Test fun actualAndroidHttpShowsBothProvidersAndFixtureIsNeverLiveProof() {
        val token = ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand("cat /data/local/tmp/saathi-test-token")).bufferedReader().use { it.readText().trim() }
        try {
            instrumentation.runOnMainSync { assertTrue(PracticeGateway.configure(token, true)) }
            fun call(probe: Boolean): GatewayResult {
                val done = CountDownLatch(1); var result: GatewayResult? = null
                instrumentation.runOnMainSync {
                    val callback: (GatewayResult) -> Unit = { result = it; done.countDown() }
                    if (probe) PracticeGateway.checkProviders(true, callback) else PracticeGateway.connectionStatus(callback)
                }
                assertTrue(done.await(15, TimeUnit.SECONDS)); return requireNotNull(result)
            }
            assertTrue(call(false) is GatewayResult.Connection)
            val result = call(true)
            assertTrue(result.toString(), result is GatewayResult.Connection)
            val report = (result as GatewayResult.Connection).report
            assertTrue(report.contains("gemini")); assertTrue(report.contains("groq"))
            assertTrue(report.contains("Test fixture · does not verify a real API"))
            assertTrue(report.contains("10 / 9 / 19"))
            assertTrue(report.contains("Paired synthetic guidance passed"))
            assertFalse(report.contains(token))
            instrumentation.runOnMainSync { PracticeGateway.configure("x".repeat(40)) }
            assertEquals("unauthorized", (call(false) as GatewayResult.Rejected).reason)
        } finally { instrumentation.runOnMainSync { PracticeGateway.disable() } }
    }
    @Test fun metadataDecoderRejectsWrongCorrelationUnknownDataAndCoercions() {
        val body = JSONObject().put("status", "connection").put("mode", "mock").put("request_id", JSONObject.NULL).put("reason", "not_requested")
            .put("providers", JSONArray(listOf("mock-a", "mock-b").map { JSONObject().put("provider", it).put("model", "fixture")
                .put("attempts", 0).put("configured", true).put("last", JSONObject.NULL) }))
        assertTrue(ConnectionCodec.decode(body.toString(), null) is GatewayResult.Connection)
        assertTrue(ConnectionCodec.decode(body.toString(), "another-request") is GatewayResult.Rejected)
        body.getJSONArray("providers").getJSONObject(0).put("configured", "true")
        assertTrue(ConnectionCodec.decode(body.toString(), null) is GatewayResult.Rejected)
        body.getJSONArray("providers").getJSONObject(0).put("configured", true).put("secret", "must-never-display")
        assertTrue(ConnectionCodec.decode(body.toString(), null) is GatewayResult.Rejected)
    }
}
