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

    @Test fun pairedProtocolGuidesAnExternalDetourAndReturn() {
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
            val token = shell("cat /data/local/tmp/saathi-test-token")
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity {
                    assertTrue(com.saathi.gateway.PracticeGateway.configure(token, true))
                    assertTrue(SaathiSession.startLive(it, "Show me the help section", GuidanceLanguage.ENGLISH, false))
                    it.startActivity(external)
                }
                waitFor("Paired backend proposes Help for a free-form goal") { SaathiSession.instruction.value.contains("choose “Help”") }
                screenshot("ai-fixture-help")
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
                File(output(), "ai-fixture-result.txt").writeText("PASS: actual Android HTTP/live-schema transport and real AccessibilityService; free-form goal; paired synthetic adapters select Help, correct wrong path to Back, return, hand over; Stop clears. PROVIDERS ARE DETERMINISTIC TEST FAKES, NOT LIVE GEMINI/GROQ.\n")
            }
        } finally {
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
