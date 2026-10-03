package com.saathi.ui

import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saathi.core.GatewayResult
import com.saathi.gateway.PracticeGateway
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Never runs in ordinary suites. Explicit opt-in makes one paired real-provider check. */
@RunWith(AndroidJUnit4::class)
class ProviderSmokeTest {
    @Test fun explicitlyAuthorizedLiveConnectionCheck() {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue("Requires explicit authorization; may consume model quota", args.getString("allowRealProviderCheck") == "true")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val token = ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand("cat /data/local/tmp/saathi-test-token")).bufferedReader().use { it.readText().trim() }
        val done = CountDownLatch(1); var result: GatewayResult? = null
        try {
            instrumentation.runOnMainSync {
                assertTrue(PracticeGateway.configure(token, true))
                PracticeGateway.checkProviders(true) { result = it; done.countDown() }
            }
            assertTrue("Local server must respond", done.await(16, TimeUnit.SECONDS))
            val response = result
            assertTrue("Expected a sanitized per-provider report", response is GatewayResult.Connection)
            val out = java.io.File(requireNotNull(args.getString("additionalTestOutputDir")))
            out.mkdirs()
            java.io.File(out, "provider-report.txt").writeText((response as GatewayResult.Connection).report)
            // A completed transport test is not an assertion of provider success or accuracy.
        } finally { instrumentation.runOnMainSync { PracticeGateway.disable() } }
    }
}
