package com.saathi.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.UiAutomation
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.MotionEvent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saathi.MainActivity
import com.saathi.accessibility.NodeMasker
import com.saathi.language.GuidanceLanguage
import com.saathi.orchestrator.SaathiSession
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Real enabled service, real external events. Never injects snapshots or calls the guide/presenter. */
@RunWith(AndroidJUnit4::class)
class LiveAiIntegrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val automation get() = instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
    private fun main(action: () -> Unit) = instrumentation.runOnMainSync(action)
    private fun shell(command: String) = ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command))
        .bufferedReader().use { it.readText().trim() }
    private fun waitFor(description: String, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 12_000
        var ready = condition()
        while (!ready && SystemClock.uptimeMillis() < deadline) {
            SystemClock.sleep(100)
            ready = condition()
        }
        assertTrue("$description; state=${SaathiSession.status.value}; instruction=${SaathiSession.instruction.value}", ready)
    }
    private fun nodes() = automation.rootInActiveWindow?.let { root ->
        try { NodeMasker.flatten(root) } catch (error: IllegalStateException) {
            // A transitioning tree is unavailable; the bounded poll still requires a complete result.
            if (error.message != "Missing observation branch") throw error
            emptyList()
        } finally { @Suppress("DEPRECATION") root.recycle() }
    }.orEmpty()
    private fun tap(label: String) {
        var observed: com.saathi.core.UiNode? = null
        waitFor("Visible $label") {
            observed = nodes().firstOrNull { it.text == label && (it.isClickable || it.clickableAncestorBounds != null) }
            observed != null
        }
        // Keep the complete snapshot that satisfied the wait; a second tree can be mid-transition.
        val node = requireNotNull(observed)
        val bounds = if (node.isClickable) node.bounds else requireNotNull(node.clickableAncestorBounds)
        val down = SystemClock.uptimeMillis()
        for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
            val event = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, bounds.exactCenterX(), bounds.exactCenterY(), 0)
            try { assertTrue(automation.injectInputEvent(event, true)) } finally { event.recycle() }
        }
    }
    private fun output() = File(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir") ?: context.filesDir.path).apply { mkdirs() }
    private fun screenshot(name: String) { automation.takeScreenshot()?.let { image ->
        File(output(), "$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }; image.recycle()
    } }

    @Test fun primaryProtocolGuidesAnExternalDetourAndReturn() = runProtocol(false)
    @Test fun exactVisibleGoalUsesNoGatewayEvenWithAiEnabled() = runProtocol(true)

    private fun runProtocol(localFirst: Boolean) {
        assertTrue("Run only on a synthetic Android emulator", android.os.Build.FINGERPRINT.contains("generic") || android.os.Build.MODEL.startsWith("sdk_"))
        val service = "com.saathi/com.saathi.accessibility.SaathiAccessibilityService"
        val priorServices = shell("settings get secure enabled_accessibility_services")
        val priorEnabled = shell("settings get secure accessibility_enabled")
        val priorOverlay = Regex("SYSTEM_ALERT_WINDOW: (allow|ignore|deny|default)")
            .find(shell("appops get com.saathi SYSTEM_ALERT_WINDOW"))?.groupValues?.get(1) ?: "default"
        val external = Intent().setComponent(ComponentName(instrumentation.context.packageName, ExternalSurfaceActivity::class.java.name))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        fun restore(key: String, value: String) {
            if (value == "null" || value.isBlank()) shell("settings delete secure $key")
            else shell("settings put secure $key $value")
        }
        try {
            automation.serviceInfo = automation.serviceInfo.apply { flags = flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS }
            shell("appops set com.saathi SYSTEM_ALERT_WINDOW allow")
            val enabled = (priorServices.takeUnless { it == "null" }.orEmpty().split(':').filter { it.isNotBlank() } + service).distinct().joinToString(":")
            shell("settings put secure enabled_accessibility_services $enabled")
            shell("settings put secure accessibility_enabled 1")
            waitFor("Real Saathi accessibility service bound") {
                shell("dumpsys accessibility").substringAfter("Bound services:").substringBefore("Enabled services:").contains("label=Saathi guidance")
            }
            val reasonCounts = mutableMapOf<String,Int>()
            main { com.saathi.accessibility.ObservationDiagnostics.gatewayObserver = { reason ->
                reasonCounts[reason] = (reasonCounts[reason] ?: 0) + 1
                File(output(), "gateway-reason-counts-${if (localFirst) "local" else "cloud"}.json").writeText(org.json.JSONObject(reasonCounts.toMap()).toString(2))
            } }
            val token = if (localFirst) "a".repeat(43) else shell("cat /data/local/tmp/saathi-test-token")
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                val before = com.saathi.gateway.PracticeGateway.requestsStarted.get()
                scenario.onActivity {
                    assertTrue(com.saathi.gateway.PracticeGateway.configure(token, true))
                    assertTrue(SaathiSession.startLive(it, if (localFirst) "Help" else "Show me the help section", GuidanceLanguage.ENGLISH, false))
                    it.startActivity(external)
                }
                if (localFirst) {
                    waitFor("Exact Help stays local with AI enabled") { SaathiSession.instruction.value.contains("Find “Help”") }
                    SystemClock.sleep(500)
                    assertEquals("Local match must not even start HTTP", before, com.saathi.gateway.PracticeGateway.requestsStarted.get())
                    screenshot("local-first-ai-enabled")
                    main { SaathiSession.stop() }
                    assertNull(SaathiSession.presentationKey())
                    return@use
                }
                waitFor("Primary backend proposes Help for a free-form goal") { SaathiSession.instruction.value.contains("choose “Help”") }
                screenshot("ai-fixture-help")
                val requestsBeforeStorm = com.saathi.gateway.PracticeGateway.requestsStarted.get()
                val attemptsBefore = com.saathi.gateway.PracticeGateway.requestsAttempted.get()
                val cancellationsBefore = com.saathi.gateway.PracticeGateway.requestsCancelled.get()
                val localRejectionsBefore = com.saathi.gateway.PracticeGateway.requestsRejectedLocally.get()
                val serviceEvents = java.util.concurrent.atomic.AtomicInteger()
                val observations = java.util.concurrent.atomic.AtomicInteger()
                com.saathi.accessibility.ObservationDiagnostics.observer = { _, _, _, _ -> serviceEvents.incrementAndGet() }
                com.saathi.accessibility.ObservationDiagnostics.snapshotObserver = { observations.incrementAndGet() }
                try {
                    tap("Event storm")
                    SystemClock.sleep(1800)
                    waitFor("Cloud guidance recovers after burst") { SaathiSession.instruction.value.contains("choose “Help”") }
                    tap("Changing event storm")
                    SystemClock.sleep(1800)
                    waitFor("Cloud guidance recovers after changing-content burst") { SaathiSession.instruction.value.contains("choose “Help”") }
                    val dispatched = com.saathi.gateway.PracticeGateway.requestsStarted.get() - requestsBeforeStorm
                    assertTrue("Burst must not become hundreds of cloud requests: $dispatched", dispatched in 0..12)
                    File(output(), "cloud-event-storm.json").writeText(org.json.JSONObject()
                        .put("events_generated",660).put("actual_content_mutations",60).put("events_delivered_including_overlays",serviceEvents.get())
                        .put("observations_created",observations.get()).put("http_requests_started",dispatched)
                        .put("requests_attempted",com.saathi.gateway.PracticeGateway.requestsAttempted.get()-attemptsBefore)
                        .put("requests_cancelled",com.saathi.gateway.PracticeGateway.requestsCancelled.get()-cancellationsBefore)
                        .put("requests_rejected_locally",com.saathi.gateway.PracticeGateway.requestsRejectedLocally.get()-localRejectionsBefore)
                        .put("request_semantic_deduplication_implemented",false)
                        .put("live_provider_calls",0).put("scope","actual service to bounded local synthetic gateway; no paid providers").toString(2))
                } finally {
                    com.saathi.accessibility.ObservationDiagnostics.observer = null
                    com.saathi.accessibility.ObservationDiagnostics.snapshotObserver = null
                }

                tap("Explore")
                waitFor("Backend adapts to detour with observed Back control") { SaathiSession.instruction.value.contains("choose “Back to choices”") }
                screenshot("ai-fixture-recovery")
                tap("Back to choices")
                waitFor("Backend returns to original goal") { SaathiSession.instruction.value.contains("choose “Help”") }
                tap("Help")
                waitFor("Unsupported next screen hands over without guessing completion") { SaathiSession.instruction.value.contains("could not verify") }
                assertTrue(SaathiSession.isActive())
                main { SaathiSession.stop() }
                assertNull(SaathiSession.presentationKey())
                File(output(), "ai-fixture-result.txt").writeText("PASS: actual Android HTTP/live-schema transport and real AccessibilityService; free-form goal; primary synthetic adapter selects Help, correct wrong path to Back, return, hand over; Stop clears. PROVIDERS ARE DETERMINISTIC TEST FAKES, NOT LIVE GEMINI/GROQ.\n")
            }
        } finally {
            main { com.saathi.accessibility.ObservationDiagnostics.gatewayObserver = null }
            File(output(), "ai-fixture-service-state.txt").writeText(shell("dumpsys accessibility"))
            main { com.saathi.gateway.PracticeGateway.disable() }
            restore("enabled_accessibility_services", priorServices)
            restore("accessibility_enabled", priorEnabled)
            shell("appops set com.saathi SYSTEM_ALERT_WINDOW $priorOverlay")
            main { context.startActivity(Intent(external).putExtra("close_fixture", true).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)) }
            shell("input keyevent KEYCODE_HOME")
            instrumentation.getUiAutomation(0)
        }
    }
}
