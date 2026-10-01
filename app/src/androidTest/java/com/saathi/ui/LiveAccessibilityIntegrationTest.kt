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
class LiveAccessibilityIntegrationTest {
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

    @Test fun realServiceTracksExternalDetourReturnAndWebChangeThenStops() {
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
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity {
                    assertTrue(SaathiSession.startLive(it, "Help", GuidanceLanguage.ENGLISH, false))
                    it.startActivity(external)
                }
                waitFor("Automatic native guidance") { SaathiSession.instruction.value.startsWith("Find “Help”") }
                waitFor("Overlay visible") {
                    shell("dumpsys window windows").substringAfter("u0 Saathi guidance}").substringBefore("Window #").contains("isOnScreen=true")
                }
                // The overlay must not invalidate its own observation in an accessibility feedback loop.
                waitFor("Initial web content loaded") { nodes().any { it.text == "Support" } }
                automation.waitForIdle(750, 8_000)
                waitFor("Presentation after initial layout settles") { SaathiSession.presentationKey() != null }
                val stable = SaathiSession.presentationKey()
                assertNotNull(stable)
                SystemClock.sleep(1_000)
                assertEquals("Idle presentation must remain stable", stable, SaathiSession.presentationKey())
                screenshot("live-service-native")
                val session = SaathiSession.sessionKey()
                main {
                    assertFalse(SaathiSession.changeLiveRequest("Delete", session, stable))
                    assertFalse(SaathiSession.changeLiveRequest("Support", "old-session", stable))
                    assertEquals("Help", SaathiSession.currentRequest())
                    assertTrue(SaathiSession.changeLiveRequest("Support", session, stable))
                    assertNull("Old bounds cleared before reading the replacement tree", SaathiSession.presentationKey())
                    assertFalse("Late reply from old screen is rejected", SaathiSession.changeLiveRequest("Help", session, stable))
                }
                // No user tap/window mutation: changing the request itself must read the real tree.
                waitFor("Live request change reads the current WebView control") { SaathiSession.instruction.value.startsWith("Find “Support”") }
                assertEquals("Request change preserves session identity", session, SaathiSession.sessionKey())
                screenshot("live-service-retarget")
                main { assertTrue(SaathiSession.changeLiveRequest("Help", session)) }
                waitFor("Typed-style replacement returns to native control") { SaathiSession.instruction.value.startsWith("Find “Help”") }
                tap("Explore")
                waitFor("Detour correction from external event") { SaathiSession.instruction.value.startsWith("I cannot find") }
                screenshot("live-service-detour")
                tap("Back to choices")
                waitFor("Guidance resumes after actual return") { SaathiSession.instruction.value.startsWith("Find “Help”") }
                tap("Help")
                waitFor("Removed option invalidates old guidance") { SaathiSession.instruction.value.startsWith("I cannot find") }
                main { context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)) }
                waitFor("Own app clears external presentation") { SaathiSession.presentationKey() == null }
                main { assertFalse(SaathiSession.changeLiveRequest("Explore", session, stable)) }
                scenario.onActivity {
                    assertTrue(SaathiSession.startLive(it, "Support", GuidanceLanguage.ENGLISH, false))
                    it.startActivity(external)
                }
                waitFor("Automatic WebView link guidance") { SaathiSession.instruction.value.startsWith("Find “Support”") }
                screenshot("live-service-web")
                tap("Support")
                waitFor("Web mutation clears old target") { SaathiSession.instruction.value.startsWith("I cannot find") }
                assertTrue(SaathiSession.isActive())
                assertFalse(SaathiSession.hasSpokenGuidance())
                main { SaathiSession.stop() }
                waitFor("Stop removes guidance window") { !shell("dumpsys window windows").contains("Saathi guidance}") }
                assertNull(SaathiSession.presentationKey())
                main { assertFalse(SaathiSession.changeLiveRequest("Help", session)) }
                File(output(), "live-service-result.txt").writeText("PASS: real enabled service; native/WebView events; stable idle presentation; in-session request change without a screen event; old presentation/session rejection; detour/return; changed label; own-app clearing; explicit Stop. Request handoff exercised directly; no real speech recognition/audio or model.\n")
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
