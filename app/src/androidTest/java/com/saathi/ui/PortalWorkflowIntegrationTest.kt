package com.saathi.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.UiAutomation
import android.content.ComponentName
import android.content.Intent
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.MotionEvent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saathi.MainActivity
import com.saathi.accessibility.NodeMasker
import com.saathi.accessibility.SaathiAccessibilityService
import com.saathi.core.GuidanceSessionState
import com.saathi.gateway.PracticeGateway
import com.saathi.language.GuidanceLanguage
import com.saathi.orchestrator.SaathiSession
import com.saathi.speech.VoiceConversationService
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Controlled forms only. Synthetic user actions do not grant production automation rights. */
@RunWith(AndroidJUnit4::class)
class PortalWorkflowIntegrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val automation get() = instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
    private fun shell(command: String) = ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command)).bufferedReader().use { it.readText().trim() }
    private fun waitFor(name: String, condition: () -> Boolean) {
        val end = SystemClock.uptimeMillis() + 12000
        while (!condition() && SystemClock.uptimeMillis() < end) SystemClock.sleep(100)
        assertTrue("$name; status=${SaathiSession.status.value}", condition())
    }
    private fun fixtureRoot(): android.view.accessibility.AccessibilityNodeInfo? {
        val roots = automation.windows.mapNotNull { it.root }
        val chosen = roots.firstOrNull { it.packageName == instrumentation.context.packageName }
        roots.filter { it !== chosen }.forEach { it.recycle() }
        return chosen
    }
    private fun next(stage: Int) {
        // Only this test driver locates the synthetic transition control on private pages.
        var bounds: android.graphics.Rect? = null
        waitFor("Synthetic next control before stage $stage") {
            val root = fixtureRoot() ?: return@waitFor false
            val pending = java.util.ArrayDeque<android.view.accessibility.AccessibilityNodeInfo>()
            pending.add(root)
            try {
                while (pending.isNotEmpty()) {
                    val node = pending.removeFirst()
                    try {
                        if (node.text?.toString() == "Fixture next" && node.isClickable) {
                            bounds = android.graphics.Rect().also { node.getBoundsInScreen(it) }
                        }
                        for (index in 0 until node.childCount) node.getChild(index)?.let(pending::add)
                    } finally { node.recycle() }
                }
            } finally { while (pending.isNotEmpty()) pending.removeFirst().recycle() }
            bounds != null
        }
        val box = requireNotNull(bounds); val down = SystemClock.uptimeMillis()
        for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
            val event = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, box.exactCenterX(), box.exactCenterY(), 0)
            try { assertTrue(automation.injectInputEvent(event, true)) } finally { event.recycle() }
        }
    }
    @Test fun reportingWebFormPreservesGoalAndPrivateUserBoundaries() {
        val oldServices = shell("settings get secure enabled_accessibility_services")
        val oldEnabled = shell("settings get secure accessibility_enabled")
        val oldRotation = shell("settings get system user_rotation")
        val oldAutoRotation = shell("settings get system accelerometer_rotation")
        val oldOverlay = Regex("SYSTEM_ALERT_WINDOW: (allow|ignore|deny|default)").find(shell("appops get com.saathi SYSTEM_ALERT_WINDOW"))?.groupValues?.get(1) ?: "default"
        val service = "com.saathi/com.saathi.accessibility.SaathiAccessibilityService"
        val external = Intent().setComponent(ComponentName(instrumentation.context.packageName, ExternalSurfaceActivity::class.java.name))
            .putExtra("portal_fixture", true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        fun restore(key: String, value: String) { shell(if (value == "null" || value.isBlank()) "settings delete secure $key" else "settings put secure $key $value") }
        try {
            automation.serviceInfo = automation.serviceInfo.apply { flags = flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS }
            shell("appops set com.saathi SYSTEM_ALERT_WINDOW allow")
            shell("settings put secure enabled_accessibility_services " + (oldServices.takeUnless { it == "null" }.orEmpty().split(':').filter { it.isNotBlank() } + service).distinct().joinToString(":"))
            shell("settings put secure accessibility_enabled 1")
            waitFor("Service ready") { SaathiAccessibilityService.isConnected() }
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { assertTrue(SaathiSession.startLive(it,"Help",GuidanceLanguage.ENGLISH,false)); it.startActivity(external) }
                fun guided() = SaathiSession.instruction.value.startsWith("Find “Help”") && SaathiSession.presentationKey() != null
                waitFor("Public guidance") { guided() }
                val key = SaathiSession.sessionKey(); val requests = PracticeGateway.requestsStarted.get()
                for (stage in 1..7) {
                    next(stage)
                    if (stage in listOf(1,2,5)) {
                        waitFor("Private stage $stage") { SaathiSession.status.value == GuidanceSessionState.SENSITIVE_HANDOVER }
                        assertFalse(VoiceConversationService.isRunning())
                        val root = requireNotNull(fixtureRoot())
                        val nodes = try { NodeMasker.flatten(root) } finally { root.recycle() }
                        assertFalse(nodes.toString().contains("fictional-secret-canary"))
                        assertFalse(nodes.toString().contains("582139"))
                        assertFalse(nodes.toString().contains("Fictional private narrative canary"))
                    } else if (stage == 3) {
                        waitFor("Human challenge") { SaathiSession.status.value == GuidanceSessionState.WAITING_FOR_CAPTCHA }
                        assertFalse(VoiceConversationService.isRunning())
                    } else if (stage == 6) {
                        waitFor("Outage removes stale target") { !guided() }
                    } else waitFor("Safe continuation stage $stage") { guided() }
                    assertEquals(key,SaathiSession.sessionKey()); assertEquals("Help",SaathiSession.currentRequest())
                }
                assertEquals("No model calls or automatic submission",requests,PracticeGateway.requestsStarted.get())
                val root = requireNotNull(fixtureRoot())
                val pending = java.util.ArrayDeque<android.view.accessibility.AccessibilityNodeInfo>(); pending.add(root)
                var declarations = 0; var disabledSubmit = false
                try {
                    while (pending.isNotEmpty()) {
                        val node = pending.removeFirst()
                        try {
                            if (node.isCheckable) { declarations++; assertFalse("Declaration stays unchecked",node.isChecked) }
                            if (node.text?.toString() == "Submit complaint") disabledSubmit = !node.isEnabled
                            for (index in 0 until node.childCount) node.getChild(index)?.let(pending::add)
                        } finally { node.recycle() }
                    }
                } finally { while(pending.isNotEmpty()) pending.removeFirst().recycle() }
                assertEquals("The fixture declaration is observed",1,declarations)
                assertTrue("Submission remains disabled and user controlled",disabledSubmit)
                var previousPresentation: String? = null
                waitFor("Capture an actual pre-rotation presentation") {
                    instrumentation.runOnMainSync { previousPresentation = if (guided()) SaathiSession.presentationKey() else null }
                    previousPresentation != null
                }
                val capturedPresentation = requireNotNull(previousPresentation)
                shell("settings put system accelerometer_rotation 0")
                shell("settings put system user_rotation 1")
                waitFor("Rotation requires fresh guidance bounds") { guided() && SaathiSession.presentationKey() != capturedPresentation }
                assertEquals("Help",SaathiSession.currentRequest())
                instrumentation.runOnMainSync { assertFalse(SaathiSession.changeLiveRequest("Explore",key,capturedPresentation)) }
                shell("input keyevent KEYCODE_SLEEP")
                waitFor("Screen lock stops the session") { !SaathiSession.isActive() && !VoiceConversationService.isRunning() }
                assertNull(SaathiSession.presentationKey())
                shell("input keyevent KEYCODE_WAKEUP"); shell("wm dismiss-keyguard")
                SystemClock.sleep(500)
                assertFalse("Unlock does not silently resume observation",SaathiSession.isActive())
            }
        } finally {
            instrumentation.runOnMainSync { SaathiSession.stop() }
            instrumentation.targetContext.startActivity(Intent().setComponent(external.component).putExtra("close_fixture",true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
            shell("input keyevent KEYCODE_WAKEUP"); shell("wm dismiss-keyguard")
            for ((name,value) in listOf("user_rotation" to oldRotation,"accelerometer_rotation" to oldAutoRotation))
                shell(if (value == "null" || value.isBlank()) "settings delete system $name" else "settings put system $name $value")
            shell("appops set com.saathi SYSTEM_ALERT_WINDOW $oldOverlay")
            restore("enabled_accessibility_services",oldServices);restore("accessibility_enabled",oldEnabled)
        }
    }
}
