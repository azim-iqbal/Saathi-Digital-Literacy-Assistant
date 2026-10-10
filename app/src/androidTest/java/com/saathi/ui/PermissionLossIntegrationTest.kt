package com.saathi.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.UiAutomation
import android.content.ComponentName
import android.content.Intent
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saathi.MainActivity
import com.saathi.language.GuidanceLanguage
import com.saathi.orchestrator.SaathiSession
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Emulator-only live permission loss; no manual session Stop or observation injection drives the assertions. */
@RunWith(AndroidJUnit4::class)
class PermissionLossIntegrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val automation get() = instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
    private fun main(action: () -> Unit) = instrumentation.runOnMainSync(action)
    private fun shell(command: String) = ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command))
        .bufferedReader().use { it.readText().trim() }
    private fun waitFor(description: String, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 12_000
        while (!condition() && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(100)
        assertTrue("$description; state=${SaathiSession.status.value}; instruction=${SaathiSession.instruction.value}", condition())
    }
    private fun output() = File(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir") ?: context.filesDir.path).apply { mkdirs() }
    @Test fun overlayAndAccessibilityRevocationStopAndNeverAutoResume() {
        assertTrue("Emulator or explicitly authorized physical device required", android.os.Build.FINGERPRINT.contains("generic") || android.os.Build.MODEL.startsWith("sdk_") || InstrumentationRegistry.getArguments().getString("physicalDeviceConfirmed") == "true")
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
            DeviceTestAccess.reconnect(automation)
            waitFor("Real Saathi accessibility service bound") {
                shell("dumpsys accessibility").substringAfter("Bound services:").substringBefore("Enabled services:").contains("label=Saathi guidance")
            }
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity {
                    assertTrue(SaathiSession.startLive(it, "Help", GuidanceLanguage.ENGLISH, false))
                    it.startActivity(external)
                }
                waitFor("Live guidance before revocation") { SaathiSession.instruction.value.startsWith("Find “Help”") }
                waitFor("Attached guidance window") { shell("dumpsys window windows").contains("Saathi guidance}") }
                shell("appops set com.saathi SYSTEM_ALERT_WINDOW ignore")
                waitFor("Overlay permission loss stops session") { !SaathiSession.isActive() }
                waitFor("Overlay window removed") { !shell("dumpsys window windows").contains("Saathi guidance}") }
                assertNull(SaathiSession.presentationKey())
                assertEquals(com.saathi.speech.VoicePhase.OFF, com.saathi.speech.VoiceConversationService.phase.value)
                shell("appops set com.saathi SYSTEM_ALERT_WINDOW allow")
                SystemClock.sleep(500)
                assertFalse("Restoring overlay permission must not restart observation", SaathiSession.isActive())
                main { context.startActivity(Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)) }
                waitFor("Main activity returns") { scenario.state == androidx.lifecycle.Lifecycle.State.RESUMED }
                scenario.onActivity {
                    assertTrue(SaathiSession.startLive(it, "Help", GuidanceLanguage.ENGLISH, false))
                    it.startActivity(external)
                }
                waitFor("User explicitly restarted guidance") { SaathiSession.instruction.value.startsWith("Find “Help”") }
                val otherServices = enabled.split(':').filterNot { it == service }.joinToString(":")
                restore("enabled_accessibility_services", otherServices)
                restore("accessibility_enabled", if (otherServices.isBlank()) "0" else "1")
                waitFor("Accessibility revocation stops session") { !SaathiSession.isActive() }
                waitFor("No guidance window after service unbind") { !shell("dumpsys window windows").contains("Saathi guidance}") }
                assertNull(SaathiSession.presentationKey())
                shell("settings put secure enabled_accessibility_services $enabled")
                shell("settings put secure accessibility_enabled 1")
                waitFor("Service rebinds without a session") {
                    shell("dumpsys accessibility").substringAfter("Bound services:").substringBefore("Enabled services:").contains("label=Saathi guidance")
                }
                assertFalse("Restoring accessibility must not restart observation", SaathiSession.isActive())
                File(output(), "permission-loss-result.txt").writeText("PASS: live overlay permission revoked; session and presentation stopped; explicit restart; accessibility revoked; session and presentation stopped; neither permission restoration restarted observation. Text-only controlled session on ${android.os.Build.MODEL}, no microphone capture.\n")
            }
        } finally {
            File(output(), "service-state-final.txt").writeText(shell("dumpsys accessibility"))
            main { SaathiSession.stop() }
            restore("enabled_accessibility_services", priorServices)
            restore("accessibility_enabled", priorEnabled)
            shell("appops set com.saathi SYSTEM_ALERT_WINDOW $priorOverlay")
            main { context.startActivity(Intent(external).putExtra("close_fixture", true).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)) }
            shell("input keyevent KEYCODE_HOME")
            instrumentation.getUiAutomation(0)
        }
    }
}
