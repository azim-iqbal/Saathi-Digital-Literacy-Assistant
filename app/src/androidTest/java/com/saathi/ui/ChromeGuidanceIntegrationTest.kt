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
class ChromeGuidanceIntegrationTest {
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
    private fun nodes() = automation.rootInActiveWindow?.let { root ->
        try { NodeMasker.flatten(root) } finally { @Suppress("DEPRECATION") root.recycle() }
    }.orEmpty()
    private fun tap(label: String) {
        waitFor("Visible $label") { nodes().any { it.text == label && (it.isClickable || it.clickableAncestorBounds != null) } }
        val node = nodes().first { it.text == label && (it.isClickable || it.clickableAncestorBounds != null) }
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

    @Test fun realChromePageTracksDetourPrivateFormAndRetarget() {
        assertTrue("Run only on a synthetic Android emulator", android.os.Build.FINGERPRINT.contains("generic") || android.os.Build.MODEL.startsWith("sdk_"))
        val service = "com.saathi/com.saathi.accessibility.SaathiAccessibilityService"
        val priorServices = shell("settings get secure enabled_accessibility_services")
        val priorEnabled = shell("settings get secure accessibility_enabled")
        val priorOverlay = Regex("SYSTEM_ALERT_WINDOW: (allow|ignore|deny|default)")
            .find(shell("appops get com.saathi SYSTEM_ALERT_WINDOW"))?.groupValues?.get(1) ?: "default"
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
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity {
                    assertTrue(SaathiSession.startLive(it, "Help", GuidanceLanguage.ENGLISH, false))
                    it.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("http://127.0.0.1:8766/browser.html"))
                        .setPackage("com.android.chrome").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                waitFor("Chrome exposes page Help control") { SaathiSession.instruction.value.startsWith("Find “Help”") }
                assertEquals("com.android.chrome", automation.rootInActiveWindow?.packageName?.toString())
                waitFor("Chrome permits guidance overlay") {
                    shell("dumpsys window windows").substringAfter("u0 Saathi guidance}").substringBefore("Window #").contains("isOnScreen=true")
                }
                screenshot("chrome-help")
                tap("Explore")
                waitFor("Chrome detour correction") { SaathiSession.instruction.value.startsWith("I cannot find") }
                tap("Back to choices")
                waitFor("Chrome return") { SaathiSession.instruction.value.startsWith("Find “Help”") }
                tap("Private example")
                waitFor("Password page suspends guidance") { SaathiSession.instruction.value.startsWith("This screen contains private fields") }
                screenshot("chrome-private")
                tap("Back to choices")
                waitFor("Chrome return from private screen") { SaathiSession.instruction.value.startsWith("Find “Help”") }
                val session = SaathiSession.sessionKey()
                main { assertTrue(SaathiSession.changeLiveRequest("To", session)) }
                tap("Travel options")
                waitFor("Chrome travel destination remains public") { SaathiSession.instruction.value.startsWith("Find “To”") }
                assertFalse("Public browser travel dates and fares must not pause guidance", nodes().any { it.isSensitive })
                screenshot("chrome-public-travel")
                main { assertTrue(SaathiSession.changeLiveRequest("01/10/2026", session)) }
                waitFor("Chrome travel date can be highlighted") { SaathiSession.instruction.value.startsWith("Find “01/10/2026”") }
                tap("Back to choices")
                waitFor("Departed date target clears") { SaathiSession.instruction.value.startsWith("I cannot find") }
                main { assertTrue(SaathiSession.changeLiveRequest("Support", session, SaathiSession.presentationKey())) }
                waitFor("Chrome link retarget") { SaathiSession.instruction.value.startsWith("Find “Support”") }
                screenshot("chrome-support")
                tap("Support")
                waitFor("Chrome link mutation clears target") { SaathiSession.instruction.value.startsWith("I cannot find") }
                main { SaathiSession.stop() }
                waitFor("Stop clears Chrome overlay") { !shell("dumpsys window windows").contains("Saathi guidance}") }
                File(output(), "chrome-result.txt").writeText("PASS: real Chrome package with localhost synthetic page; actual AccessibilityService; Help, detour/return, password-form suspension, Support retarget, changed label, overlay visibility/touch-through and Stop. No real account, transaction, audio or AI.\n" + shell("dumpsys package com.android.chrome").lineSequence().filter { it.contains("versionName=") }.joinToString("\n"))
            }
        } finally {
            File(output(), "chrome-sensitive-node-ids.txt").writeText(nodes().filter { it.isSensitive }.joinToString("\n") { "${it.resourceId} ${it.className} password=${it.isPassword}" })
            File(output(), "chrome-service-state-final.txt").writeText(shell("dumpsys accessibility"))
            main { SaathiSession.stop() }
            restore("enabled_accessibility_services", priorServices)
            restore("accessibility_enabled", priorEnabled)
            shell("appops set com.saathi SYSTEM_ALERT_WINDOW $priorOverlay")
            shell("am force-stop com.android.chrome")
            shell("input keyevent KEYCODE_HOME")
            instrumentation.getUiAutomation(0)
        }
    }
}
