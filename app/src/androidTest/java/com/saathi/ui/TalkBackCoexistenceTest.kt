package com.saathi.ui

import android.app.UiAutomation
import android.content.ComponentName
import android.content.Intent
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saathi.MainActivity
import com.saathi.language.GuidanceLanguage
import com.saathi.orchestrator.SaathiSession
import com.saathi.overlay.HighlightOverlayService
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Emulator coexistence only: this does not certify audible feedback or human usability. */
@RunWith(AndroidJUnit4::class)
class TalkBackCoexistenceTest {
    @Test fun installedTalkBackAndSaathiRetainFocusPauseAndFreshGuidance() {
        assertTrue(android.os.Build.MODEL.startsWith("sdk_") || android.os.Build.FINGERPRINT.contains("generic"))
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val automation = instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        fun shell(command: String) = ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command)).bufferedReader().use { it.readText().trim() }
        fun main(block: () -> Unit) = instrumentation.runOnMainSync(block)
        fun waitFor(label: String, condition: () -> Boolean) {
            val end = SystemClock.uptimeMillis() + 15000
            while (!condition() && SystemClock.uptimeMillis() < end) SystemClock.sleep(100)
            assertTrue(label, condition())
        }
        val talkback = "com.google.android.marvin.talkback/com.google.android.marvin.talkback.TalkBackService"
        assertTrue("Install TalkBack in the test emulator first", shell("pm path com.google.android.marvin.talkback").startsWith("package:"))
        val keys = listOf("enabled_accessibility_services", "accessibility_enabled", "touch_exploration_enabled")
        val prior = keys.associateWith { shell("settings get secure $it") }
        val overlay = Regex("SYSTEM_ALERT_WINDOW: (allow|ignore|deny|default)").find(shell("appops get com.saathi SYSTEM_ALERT_WINDOW"))?.groupValues?.get(1) ?: "default"
        val external = Intent().setComponent(ComponentName(instrumentation.context.packageName, ExternalSurfaceActivity::class.java.name))
            .putExtra("resume_fixture", true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        try {
            val services = (prior.getValue(keys[0]).takeUnless { it == "null" }.orEmpty().split(':').filter { it.isNotBlank() } + talkback + "com.saathi/com.saathi.accessibility.SaathiAccessibilityService").distinct().joinToString(":")
            shell("appops set com.saathi SYSTEM_ALERT_WINDOW allow")
            shell("settings put secure enabled_accessibility_services $services")
            shell("settings put secure accessibility_enabled 1")
            waitFor("Both accessibility services bound") {
                com.saathi.accessibility.SaathiAccessibilityService.isConnected() &&
                    shell("dumpsys accessibility").substringAfter("Bound services:").substringBefore("Enabled services:").contains("TalkBack")
            }
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { assertTrue(SaathiSession.startLive(it, "Help", GuidanceLanguage.ENGLISH, false)); it.startActivity(external) }
                fun guiding() = SaathiSession.instruction.value.startsWith("Find “Help”") && HighlightOverlayService.hasTarget()
                waitFor("Fresh guidance with TalkBack active") { guiding() }
                val root = requireNotNull(automation.rootInActiveWindow)
                val matches = root.findAccessibilityNodeInfosByText("Help")
                try {
                    assertTrue("External control remains accessible to focus", matches.any { it.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS) })
                } finally { matches.forEach { it.recycle() }; root.recycle() }
                main { SaathiSession.pause() }
                waitFor("Pause removes marker") { !HighlightOverlayService.hasTarget() }
                assertTrue(SaathiSession.canResume())
                main { assertTrue(SaathiSession.resume(instrumentation.targetContext)) }
                waitFor("Explicit resume gets fresh guidance") { guiding() }
                main { SaathiSession.stop() }
                waitFor("Stop removes marker") { !HighlightOverlayService.hasTarget() }
            }
        } finally {
            main { SaathiSession.stop() }
            keys.forEach { key -> val value=prior.getValue(key); shell(if(value=="null" || value.isBlank()) "settings delete secure $key" else "settings put secure $key $value") }
            shell("appops set com.saathi SYSTEM_ALERT_WINDOW $overlay")
            shell("input keyevent KEYCODE_HOME")
            instrumentation.getUiAutomation(0)
        }
    }
}
