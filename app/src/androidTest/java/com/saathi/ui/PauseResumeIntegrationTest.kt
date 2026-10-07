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
class PauseResumeIntegrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val automation get() = instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
    private fun main(action: () -> Unit) = instrumentation.runOnMainSync(action)
    private fun shell(command: String) = ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command))
        .bufferedReader().use { it.readText().trim() }
    private fun waitFor(description: String, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 12_000
        while (!condition() && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(100)
        if (!condition()) {
            File(output(), "transition-failure.txt").writeText(description + "\n" + nodes().joinToString("\n") {
                "class=${it.className} bounds=${it.bounds} clickable=${it.isClickable} parent=${it.clickableAncestorBounds}"
            })
            File(output(), "transition-windows.txt").writeText(shell("dumpsys window windows"))
            File(output(), "transition-input.txt").writeText(shell("dumpsys input"))
            File(output(), "transition-logcat.txt").writeText(shell("logcat -d -t 500 -s InputDispatcher chromium ViewRootImpl"))
            screenshot("transition-failure")
        }
        assertTrue("$description; state=${SaathiSession.status.value}; instruction=${SaathiSession.instruction.value}", condition())
    }
    private fun nodes() = automation.rootInActiveWindow?.let { root ->
        try { NodeMasker.flatten(root) } finally { @Suppress("DEPRECATION") root.recycle() }
    }.orEmpty()
    private fun tap(label: String) {
        waitFor("Visible $label") { nodes().any { it.text == label && (it.isClickable || it.clickableAncestorBounds != null) } }
        val node = nodes().first { it.text == label && (it.isClickable || it.clickableAncestorBounds != null) }
        val bounds = if (node.isClickable) node.bounds else requireNotNull(node.clickableAncestorBounds)
        File(output(), "tap-geometry.txt").appendText("$label: text=${node.bounds}; clickable=${node.isClickable}; tap=$bounds\n")
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

    @Test fun manualPauseAndPrivateChallengeRoundTripsRequireFreshObservations() {
        assertTrue(android.os.Build.FINGERPRINT.contains("generic") || android.os.Build.MODEL.startsWith("sdk_"))
        val service = "com.saathi/com.saathi.accessibility.SaathiAccessibilityService"
        val priorServices = shell("settings get secure enabled_accessibility_services")
        val priorEnabled = shell("settings get secure accessibility_enabled")
        val priorOverlay = Regex("SYSTEM_ALERT_WINDOW: (allow|ignore|deny|default)")
            .find(shell("appops get com.saathi SYSTEM_ALERT_WINDOW"))?.groupValues?.get(1) ?: "default"
        val external = Intent().setComponent(ComponentName(instrumentation.context.packageName, ExternalSurfaceActivity::class.java.name))
            .putExtra("resume_fixture", true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
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
            waitFor("Service connected") { com.saathi.accessibility.SaathiAccessibilityService.isConnected() }
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity {
                    assertTrue(SaathiSession.startLive(it, "Help", GuidanceLanguage.ENGLISH, false))
                    it.startActivity(external)
                }
                fun guiding() = SaathiSession.instruction.value.startsWith("Find “Help”") && SaathiSession.presentationKey() != null
                waitFor("Initial guidance") { guiding() }
                val original = SaathiSession.sessionKey()
                repeat(3) {
                    for ((label, expected) in listOf("Private interruption" to com.saathi.core.GuidanceSessionState.SENSITIVE_HANDOVER,
                        "Human challenge" to com.saathi.core.GuidanceSessionState.WAITING_FOR_CAPTCHA)) {
                        val old = SaathiSession.presentationKey()
                        tap(label)
                        waitFor("Explicit interruption state") { SaathiSession.status.value == expected }
                        assertEquals(original, SaathiSession.sessionKey())
                        assertEquals("Help", SaathiSession.currentRequest())
                        assertFalse(com.saathi.speech.VoiceConversationService.isRunning())
                        main { assertFalse(SaathiSession.changeLiveRequest("Explore", original, old)) }
                        screenshot(if (expected == com.saathi.core.GuidanceSessionState.SENSITIVE_HANDOVER) "resume-private" else "resume-captcha")
                        tap("Return to choices") // Synthetic user completes privately; no solver or secret entry.
                        waitFor("Automatic resume only from fresh safe tree") { guiding() }
                        assertEquals(original, SaathiSession.sessionKey())
                        assertNotEquals(old, SaathiSession.presentationKey())
                    }
                }
                val previousPresentation = SaathiSession.presentationKey()
                main { SaathiSession.pause(); SaathiSession.pause() }
                assertEquals(com.saathi.core.GuidanceSessionState.PAUSED, SaathiSession.status.value)
                assertFalse(SaathiSession.isActive())
                assertTrue(SaathiSession.canResume())
                assertEquals("Help", SaathiSession.currentRequest())
                assertNull(SaathiSession.sessionKey())
                assertNull(SaathiSession.presentationKey())
                waitFor("Pause removes overlay") { !shell("dumpsys window windows").contains("Saathi guidance}") }
                // No accessibility event may silently resume a manually paused session.
                tap("Human challenge")
                SystemClock.sleep(500)
                assertFalse(SaathiSession.isActive())
                shell("appops set com.saathi SYSTEM_ALERT_WINDOW deny")
                main { assertFalse(SaathiSession.resume(context)) }
                assertTrue(SaathiSession.canResume())
                shell("appops set com.saathi SYSTEM_ALERT_WINDOW allow")
                main { assertTrue(SaathiSession.resume(context)) }
                assertNotEquals(original, SaathiSession.sessionKey())
                main {
                    SaathiSession.stopForSession(original)
                    SaathiSession.stopForPresentation(previousPresentation)
                    assertTrue(SaathiSession.isActive())
                }
                // Resume must not reuse Help's old bounds while a challenge is still visible.
                waitFor("Resume rechecks the currently visible challenge") {
                    SaathiSession.status.value == com.saathi.core.GuidanceSessionState.WAITING_FOR_CAPTCHA
                }
                tap("Return to choices")
                waitFor("Guidance after resumed challenge") { guiding() }
                repeat(3) {
                    val oldSession = SaathiSession.sessionKey()
                    main {
                        SaathiSession.pause()
                        SaathiSession.setSpokenGuidance(true)
                        assertTrue(SaathiSession.prefersSpokenGuidance())
                        SaathiSession.setSpokenGuidance(false)
                        assertFalse(SaathiSession.prefersSpokenGuidance())
                        assertTrue(SaathiSession.resume(context))
                        assertFalse("Repeated resume must not restart an active task", SaathiSession.resume(context))
                        SaathiSession.stopForSession(oldSession)
                    }
                    waitFor("Rapid pause/resume survives old service teardown") { guiding() }
                    assertNotEquals(oldSession, SaathiSession.sessionKey())
                    assertFalse(SaathiSession.hasSpokenGuidance())
                }
                screenshot("resume-restored")
                main { SaathiSession.pause(); SaathiSession.stop(); assertFalse(SaathiSession.resume(context)) }
                assertFalse(SaathiSession.canResume())
                assertEquals("", SaathiSession.currentRequest())
                File(output(), "pause-resume-result.txt").writeText("PASS: three private/CAPTCHA round trips each; manual pause retains only task settings; no auto-resume; denied overlay blocks resume without losing task; fresh identity/tree on resume; old notification/presentation rejected; Stop discards paused task. No real speech, authentication, model or CAPTCHA solver.\n")
            }
        } finally {
            main { SaathiSession.stop() }
            restore("enabled_accessibility_services", priorServices)
            restore("accessibility_enabled", priorEnabled)
            shell("appops set com.saathi SYSTEM_ALERT_WINDOW $priorOverlay")
            main { context.startActivity(Intent(external).putExtra("close_fixture", true)) }
            shell("input keyevent KEYCODE_HOME")
            instrumentation.getUiAutomation(0)
        }
    }
}
