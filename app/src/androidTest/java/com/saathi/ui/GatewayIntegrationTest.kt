package com.saathi.ui

import android.app.UiAutomation
import android.content.Intent
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saathi.*
import com.saathi.core.*
import com.saathi.gateway.MockGatewayCodec
import com.saathi.gateway.PracticeGateway
import com.saathi.language.GuidanceLanguage
import com.saathi.orchestrator.SaathiSession
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Requires the loopback host mock server and adb reverse; never uses a model or real data. */
@RunWith(AndroidJUnit4::class)
class GatewayIntegrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val automation get() = instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
    private fun main(block: () -> Unit) = instrumentation.runOnMainSync(block)
    private fun shell(command: String) = ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command)).bufferedReader().use { it.readText().trim() }
    private fun token(): String {
        assertTrue(android.os.Build.FINGERPRINT.contains("generic") || android.os.Build.MODEL.startsWith("sdk_"))
        val value = shell("cat /data/local/tmp/saathi-test-token")
        assertTrue("Provide the ephemeral test token file per SETUP.md", value.length in 32..256 && !value.contains(' '))
        return value
    }
    private fun waitFor(message: String, block: () -> Boolean) {
        val end = SystemClock.uptimeMillis() + 12_000
        while (!block() && SystemClock.uptimeMillis() < end) SystemClock.sleep(80)
        assertTrue("$message; ${SaathiSession.instruction.value}", block())
    }
    private fun snapshot() = SanitizedScreenSnapshot(123, 1, "com.saathi", 1, "en-IN", setOf("com.saathi:id/recharge_bills"),
        requestId = java.util.UUID.randomUUID().toString()).copy(sessionId = System.nanoTime())
    private fun response(s: SanitizedScreenSnapshot) = JSONObject().apply {
        put("status", "accepted"); put("mode", "mock"); put("request_id", s.requestId); put("session_id", s.sessionId.toString())
        put("screen_revision", s.screenRevision); put("package_name", s.packageName); put("window_id", s.windowId)
        put("action", "HIGHLIGHT"); put("target_id", "com.saathi:id/recharge_bills")
        put("explanation", "Open practice"); put("expected_outcome", "Choices appear"); put("completion_evidence", JSONArray())
        put("provenance", JSONArray(listOf("mock-a", "mock-b").map { JSONObject().put("provider", it).put("model", "deterministic-fixture-v1") }))
    }

    @Test fun strictDecoderRejectsForgedStaleAndMalformedDecisions() {
        val s = snapshot()
        assertTrue(MockGatewayCodec.decode(response(s).toString(), s) is GatewayResult.Accepted)
        val invalid = listOf(response(s).put("mode", "live"), response(s).put("screen_revision", "1"),
            response(s).put("screen_revision", 9), response(s).put("request_id", "old"),
            response(s).put("target_id", "com.saathi:id/pin_input"), response(s).put("extra", "ignored?"),
            response(s).put("provenance", JSONArray().put(JSONObject().put("provider", "mock-a").put("model", "deterministic-fixture-v1"))),
            response(s).put("action", "COMPLETE").put("target_id", JSONObject.NULL))
        invalid.forEach { assertTrue(MockGatewayCodec.decode(it.toString(), s) is GatewayResult.Rejected) }
        assertTrue(MockGatewayCodec.decode(response(s).toString().replaceFirst("{", "{\"mode\":\"mock\","), s) is GatewayResult.Rejected)
        assertTrue(MockGatewayCodec.decode(response(s).toString(), s.copy(observedAtMs = System.currentTimeMillis() - 16_000)) is GatewayResult.Rejected)
        assertTrue(MockGatewayCodec.decode("x".repeat(8193), s) is GatewayResult.Rejected)
    }

    @Test fun realHttpRejectsWrongTokenAndCancelledCallbacks() {
        val secret = token()
        fun send(): GatewayResult {
            val done = CountDownLatch(1); var result: GatewayResult? = null
            main { PracticeGateway.request(snapshot()) { result = it; done.countDown() } }
            assertTrue(done.await(8, TimeUnit.SECONDS)); return requireNotNull(result)
        }
        try {
            main { assertTrue(PracticeGateway.configure(secret)) }
            assertTrue(send() is GatewayResult.Accepted)
            val cancelled = CountDownLatch(1)
            main { PracticeGateway.request(snapshot()) { cancelled.countDown() }.cancel() }
            assertFalse("Cancelled callback never delivered", cancelled.await(1, TimeUnit.SECONDS))
            main { assertTrue(PracticeGateway.configure("x".repeat(40))) }
            assertEquals("unauthorized", (send() as GatewayResult.Rejected).reason)
        } finally { main { PracticeGateway.disable() }; instrumentation.getUiAutomation(0) }
    }

    @Test fun disableOrReconfigureSuppressesQueuedConnectionResults() {
        val secret = token()
        try {
            for (replace in listOf(false, true)) {
                val obsolete = CountDownLatch(1)
                main {
                    assertTrue(PracticeGateway.configure(secret))
                    val before = PracticeGateway.requestsStarted.get()
                    PracticeGateway.connectionStatus { obsolete.countDown() }
                    // Hold the main loop so even a completed HTTP result remains queued.
                    val end = SystemClock.uptimeMillis() + 1500
                    while (PracticeGateway.requestsStarted.get() == before && SystemClock.uptimeMillis() < end) SystemClock.sleep(10)
                    assertTrue(PracticeGateway.requestsStarted.get() > before)
                    if (replace) assertTrue(PracticeGateway.configure(secret)) else PracticeGateway.disable()
                }
                assertFalse("Old connection must not report after credentials change", obsolete.await(500, TimeUnit.MILLISECONDS))
            }
            val fresh = CountDownLatch(1)
            var result: GatewayResult? = null
            main { PracticeGateway.connectionStatus { result = it; fresh.countDown() } }
            assertTrue(fresh.await(5, TimeUnit.SECONDS))
            assertTrue(result is GatewayResult.Connection)
        } finally { main { PracticeGateway.disable() }; instrumentation.getUiAutomation(0) }
    }

    @Test fun realServiceUsesBackendAcrossPracticeStepsAndKeepsPrivateFormLocal() {
        val secret = token()
        val service = "com.saathi/com.saathi.accessibility.SaathiAccessibilityService"
        val priorServices = shell("settings get secure enabled_accessibility_services")
        val priorEnabled = shell("settings get secure accessibility_enabled")
        val priorOverlay = Regex("SYSTEM_ALERT_WINDOW: (allow|ignore|deny|default)").find(shell("appops get com.saathi SYSTEM_ALERT_WINDOW"))?.groupValues?.get(1) ?: "default"
        fun restore(key: String, value: String) { shell(if (value == "null" || value.isBlank()) "settings delete secure $key" else "settings put secure $key $value") }
        try {
            shell("appops set com.saathi SYSTEM_ALERT_WINDOW allow")
            val services = (priorServices.takeUnless { it == "null" }.orEmpty().split(':').filter { it.isNotBlank() } + service).distinct().joinToString(":")
            shell("settings put secure enabled_accessibility_services $services"); shell("settings put secure accessibility_enabled 1")
            waitFor("Bound service") { com.saathi.accessibility.SaathiAccessibilityService.isConnected() }
            ActivityScenario.launch(MainActivity::class.java).use { mainScreen ->
                mainScreen.onActivity {
                    assertTrue(PracticeGateway.configure(secret))
                    SaathiSession.start(it, "Pay my water bill", GuidanceLanguage.ENGLISH)
                    it.startActivity(Intent(it, DemoBillPayActivity::class.java))
                }
                val initial = PracticeGateway.requestsStarted.get()
                waitFor("Backend home guidance") { SaathiSession.instruction.value.startsWith("Tap Recharge") && PracticeGateway.requestsStarted.get() > initial }
                onView(withId(R.id.recharge_bills)).perform(click())
                waitFor("Backend selected category") { SaathiSession.instruction.value.startsWith("Tap Water") }
                onView(withId(R.id.water_biller)).perform(click())
                waitFor("Private form hands over before any backend call") {
                    SaathiSession.status.value == GuidanceSessionState.SENSITIVE_HANDOVER &&
                        SaathiSession.instruction.value.startsWith("This screen contains private fields")
                }
                waitFor("Private form has no target marker") { !com.saathi.overlay.HighlightOverlayService.hasTarget() }
                SystemClock.sleep(500)
                val beforePrivate = PracticeGateway.requestsStarted.get()
                onView(withId(R.id.account_input)).perform(scrollTo(), replaceText("123456"), closeSoftKeyboard())
                onView(withId(R.id.amount_input)).perform(scrollTo(), replaceText("10"), closeSoftKeyboard())
                onView(withId(R.id.pin_input)).perform(scrollTo(), replaceText("1111"), closeSoftKeyboard())
                SystemClock.sleep(700)
                assertEquals("No request during private form", beforePrivate, PracticeGateway.requestsStarted.get())
                onView(withId(R.id.pay_button)).perform(scrollTo(), click())
                waitFor("Observed completion accepted") { SaathiSession.status.value == GuidanceSessionState.COMPLETED }
                assertTrue(PracticeGateway.requestsStarted.get() > beforePrivate)
                assertFalse(SaathiSession.isActive())
            }
        } finally {
            main { PracticeGateway.disable() }
            restore("enabled_accessibility_services", priorServices); restore("accessibility_enabled", priorEnabled)
            shell("appops set com.saathi SYSTEM_ALERT_WINDOW $priorOverlay")
            shell("input keyevent KEYCODE_HOME"); instrumentation.getUiAutomation(0)
        }
    }
}
