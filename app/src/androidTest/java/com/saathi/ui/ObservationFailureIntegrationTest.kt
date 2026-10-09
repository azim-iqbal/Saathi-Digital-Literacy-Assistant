package com.saathi.ui

import android.app.UiAutomation
import android.content.ComponentName
import android.content.Intent
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saathi.MainActivity
import com.saathi.core.GuidanceSessionState
import com.saathi.language.GuidanceLanguage
import com.saathi.orchestrator.SaathiSession
import com.saathi.overlay.HighlightOverlayService
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ObservationFailureIntegrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val automation get() = instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
    private fun main(action: () -> Unit) = instrumentation.runOnMainSync(action)
    private fun shell(command: String) = ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command))
        .bufferedReader().use { it.readText().trim() }
    private fun waitFor(message: String, condition: () -> Boolean) {
        val end = SystemClock.uptimeMillis() + 15000
        while (!condition() && SystemClock.uptimeMillis() < end) SystemClock.sleep(100)
        if (!condition()) {
            val output = java.io.File(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir") ?: context.filesDir.path).apply { mkdirs() }
            java.io.File(output, "failure-state.txt").writeText("$message; state=${SaathiSession.status.value}; active=${SaathiSession.isActive()}; instruction=${SaathiSession.instruction.value}")
            automation.takeScreenshot()?.let { bitmap -> java.io.File(output,"failure-screen.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }; bitmap.recycle() }
        }
        assertTrue("$message; state=${SaathiSession.status.value}", condition())
    }
    @Test fun oversizedTreeCannotHidePrivateFieldAndFreshCompleteTreeRecoversOriginalTask() {
        assertTrue(android.os.Build.MODEL.startsWith("sdk_") || android.os.Build.FINGERPRINT.contains("generic"))
        val service = "com.saathi/com.saathi.accessibility.SaathiAccessibilityService"
        val oldServices = shell("settings get secure enabled_accessibility_services")
        val oldEnabled = shell("settings get secure accessibility_enabled")
        val oldOverlay = Regex("SYSTEM_ALERT_WINDOW: (allow|deny|ignore|default)").find(shell("appops get com.saathi SYSTEM_ALERT_WINDOW"))?.groupValues?.get(1) ?: "default"
        fun external(extra: String) = Intent().setComponent(ComponentName(instrumentation.context.packageName, ExternalSurfaceActivity::class.java.name))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra(extra, true)
        fun restore(key: String, value: String) = shell(if (value == "null" || value.isBlank()) "settings delete secure $key" else "settings put secure $key $value")
        val failures = java.util.Collections.synchronizedList(mutableListOf<String>())
        com.saathi.accessibility.ObservationDiagnostics.failureObserver = { failures.add(it) }
        try {
            automation.serviceInfo = automation.serviceInfo.apply { flags = flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS }
            shell("appops set com.saathi SYSTEM_ALERT_WINDOW allow")
            shell("settings put secure enabled_accessibility_services $service")
            shell("settings put secure accessibility_enabled 1")
            waitFor("Service connected") { com.saathi.accessibility.SaathiAccessibilityService.isConnected() }
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { assertTrue(SaathiSession.startLive(it, "Help", GuidanceLanguage.ENGLISH, false)); it.startActivity(external("resume_fixture")) }
                fun guiding() = SaathiSession.instruction.value.startsWith("Find “Help”")
                waitFor("Initial guidance") { guiding() }
                val session = SaathiSession.sessionKey()
                val requests = com.saathi.gateway.PracticeGateway.requestsStarted.get()
                repeat(3) {
                    val rejected = failures.count { it == "Incomplete observation" }
                    main { context.startActivity(external("oversized_fixture")) }
                    waitFor("Oversized tree is actually scanned and rejected") {
                        failures.count { it == "Incomplete observation" } > rejected
                    }
                    waitFor("Partial tree must not produce a target") {
                        SaathiSession.status.value == GuidanceSessionState.WAITING_FOR_PRACTICE && !HighlightOverlayService.hasTarget()
                    }
                    assertEquals(session, SaathiSession.sessionKey()); assertEquals("Help", SaathiSession.currentRequest())
                    assertEquals(requests, com.saathi.gateway.PracticeGateway.requestsStarted.get())
                    main { context.startActivity(external("resume_fixture")) }
                    waitFor("Fresh complete tree resumes the original task") { guiding() }
                    assertEquals(session, SaathiSession.sessionKey())
                }
                main { SaathiSession.stop() }
                waitFor("Stop clears presentation") { !HighlightOverlayService.hasTarget() }
            }
        } finally {
            com.saathi.accessibility.ObservationDiagnostics.failureObserver = null
            val output = java.io.File(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir") ?: context.filesDir.path).apply { mkdirs() }
            java.io.File(output, "observation-failure-counts.json").writeText(org.json.JSONObject(failures.groupingBy { it }.eachCount()).toString(2))
            main { SaathiSession.stop(); context.startActivity(external("close_fixture")) }
            restore("enabled_accessibility_services", oldServices); restore("accessibility_enabled", oldEnabled)
            shell("appops set com.saathi SYSTEM_ALERT_WINDOW $oldOverlay")
            shell("input keyevent KEYCODE_HOME")
            instrumentation.getUiAutomation(0)
        }
    }
}
