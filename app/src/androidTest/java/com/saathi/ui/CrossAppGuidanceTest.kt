package com.saathi.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.provider.Settings
import android.view.MotionEvent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saathi.MainActivity
import com.saathi.accessibility.NodeMasker
import com.saathi.language.GuidanceLanguage
import com.saathi.orchestrator.LiveGuide
import com.saathi.orchestrator.SaathiSession
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Uses only the emulator's Settings search; never types or changes a system setting. */
@RunWith(AndroidJUnit4::class)
class CrossAppGuidanceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val automation get() = instrumentation.uiAutomation
    private fun shell(command: String) = ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command)).bufferedReader().use { it.readText() }
    private fun waitUntil(condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 10_000
        while (!condition() && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(100)
        assertTrue("Expected Settings/overlay transition", condition())
    }
    @Test fun settingsTargetRemainsAvailableWhenSystemHidesOverlay() {
        val previous = shell("appops get com.saathi SYSTEM_ALERT_WINDOW")
        val mode = Regex("SYSTEM_ALERT_WINDOW: (allow|ignore|deny|default)").find(previous)?.groupValues?.get(1) ?: "default"
        shell("appops set com.saathi SYSTEM_ALERT_WINDOW allow")
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { it.startActivity(Intent(Settings.ACTION_SETTINGS)) }
                waitUntil { automation.rootInActiveWindow?.packageName?.toString() == "com.android.settings" }
                automation.serviceInfo = automation.serviceInfo.apply { flags = flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS }
                waitUntil {
                    val r = automation.rootInActiveWindow ?: return@waitUntil false
                    try { NodeMasker.flatten(r).any { n -> listOfNotNull(n.text, n.description).any { it.contains("search", true) } } }
                    finally { r.recycle() }
                }
                val root = requireNotNull(automation.rootInActiveWindow)
                val nodes = try { NodeMasker.flatten(root) } finally { root.recycle() }
                val output = File(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir") ?: context.filesDir.path)
                output.mkdirs()
                File(output, "settings-controls.txt").writeText(nodes.joinToString("\n") { "${it.className} | click=${it.isClickable} parent=${it.clickableAncestorBounds != null} | ${it.text} | ${it.description}" })
                val candidate = nodes.firstOrNull { n -> (n.isClickable || n.clickableAncestorBounds != null) && n.isEnabled &&
                    listOfNotNull(n.text, n.description).any { it.contains("search", true) && LiveGuide.label(it) != null } }
                requireNotNull(candidate) { "Settings must expose a labelled search control for this compatibility test" }
                val request = listOfNotNull(candidate.text, candidate.description).first { it.contains("search", true) }
                val step = LiveGuide.next(request, nodes, "en-IN")
                assertNotNull("A single public Settings search control is required", step.target)
                assertTrue(automation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK))
                waitUntil { automation.rootInActiveWindow?.packageName?.toString() == context.packageName }
                scenario.onActivity {
                    assertTrue(SaathiSession.startLive(it, request, GuidanceLanguage.ENGLISH, false))
                    it.startActivity(Intent(Settings.ACTION_SETTINGS))
                }
                waitUntil { automation.rootInActiveWindow?.packageName?.toString() == "com.android.settings" }
                val currentRoot = requireNotNull(automation.rootInActiveWindow)
                val currentWindow = currentRoot.windowId
                val currentNodes = try { NodeMasker.flatten(currentRoot) } finally { currentRoot.recycle() }
                instrumentation.runOnMainSync {
                    val ticket = requireNotNull(SaathiSession.beginObservation("com.android.settings", currentWindow))
                    SaathiSession.onScreenChanged(currentNodes, ticket)
                }
                waitUntil { SaathiSession.instruction.value.contains("Follow the marker") }
                // Wait for the actual presentation window, not a fixed screenshot delay.
                // A non-touchable decoration need not be in the accessibility window inventory.
                waitUntil { shell("dumpsys window windows").contains("Saathi guidance") }
                automation.waitForIdle(500, 5_000)
                File(output, "settings-windows.txt").writeText(shell("dumpsys window windows"))
                automation.takeScreenshot()?.let { bitmap -> File(output, "live-settings.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) } }
                val bounds = requireNotNull(LiveGuide.next(request, currentNodes, "en-IN").target).bounds
                val down = SystemClock.uptimeMillis()
                listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP).forEach { action ->
                    val event = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, bounds.exactCenterX(), bounds.exactCenterY(), 0)
                    try { assertTrue(automation.injectInputEvent(event, true)) } finally { event.recycle() }
                }
                waitUntil {
                    val r = automation.rootInActiveWindow ?: return@waitUntil false
                    try { NodeMasker.flatten(r).any { it.className?.contains("EditText") == true } } finally { r.recycle() }
                }
                assertTrue(SaathiSession.isActive())
                assertFalse(SaathiSession.hasSpokenGuidance())
            }
        } finally {
            instrumentation.runOnMainSync { SaathiSession.stop() }
            shell("appops set com.saathi SYSTEM_ALERT_WINDOW $mode")
            shell("input keyevent KEYCODE_HOME")
        }
    }

    @Test fun separateAppAndWebLinkSupportVisibleOverlayAndBubbleReturn() {
        val previous = shell("appops get com.saathi SYSTEM_ALERT_WINDOW")
        val mode = Regex("SYSTEM_ALERT_WINDOW: (allow|ignore|deny|default)").find(previous)?.groupValues?.get(1) ?: "default"
        shell("appops set com.saathi SYSTEM_ALERT_WINDOW allow")
        val external = Intent().setComponent(android.content.ComponentName(instrumentation.context.packageName, ExternalSurfaceActivity::class.java.name))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity {
                    assertTrue(SaathiSession.startLive(it, "Help", GuidanceLanguage.ENGLISH, false))
                    it.startActivity(external)
                }
                automation.serviceInfo = automation.serviceInfo.apply { flags = flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS }
                fun nodes(): List<com.saathi.core.UiNode> {
                    val root = automation.rootInActiveWindow ?: return emptyList()
                    return try { NodeMasker.flatten(root) } finally { root.recycle() }
                }
                waitUntil { automation.rootInActiveWindow?.packageName?.toString() == instrumentation.context.packageName && nodes().any { it.text == "Help" } }
                fun presentAndTap(label: String) {
                    waitUntil { nodes().any { it.text == label } }
                    val current = nodes()
                    val debugOutput = File(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir") ?: context.filesDir.path)
                    debugOutput.mkdirs()
                    File(debugOutput, "external-${label.lowercase()}-nodes.txt").writeText(current.joinToString("\n") { "${it.text} | ${it.description} | click=${it.isClickable} parent=${it.clickableAncestorBounds} private=${it.isSensitive} bounds=${it.bounds}" })
                    val step = LiveGuide.next(label, current, "en-IN")
                    val target = requireNotNull(step.target) { "Expected unique target for $label" }
                    instrumentation.runOnMainSync {
                        // Feed real external nodes through the coordinator's current snapshot route.
                        val ticket = requireNotNull(SaathiSession.beginObservation(instrumentation.context.packageName, automation.rootInActiveWindow!!.windowId))
                        if (label == "Help") SaathiSession.onScreenChanged(current, ticket)
                        else context.startService(com.saathi.overlay.HighlightOverlayService.intent(context, target.bounds, emptyList(), false, step.speechText, SaathiSession.presentationKey()))
                    }
                    waitUntil {
                        val dump = shell("dumpsys window windows")
                        dump.substringAfter("u0 Saathi guidance}").substringBefore("Window #").contains("isOnScreen=true")
                    }
                    automation.waitForIdle(500, 5_000)
                    val output = File(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir") ?: context.filesDir.path)
                    output.mkdirs()
                    automation.takeScreenshot()?.let { bitmap -> File(output, "external-${label.lowercase()}.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) } }
                    val down = SystemClock.uptimeMillis()
                    listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP).forEach { action ->
                        val event = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, target.bounds.exactCenterX(), target.bounds.exactCenterY(), 0)
                        try { assertTrue(automation.injectInputEvent(event, true)) } finally { event.recycle() }
                    }
                    waitUntil { nodes().any { it.text == "$label opened" } }
                }
                presentAndTap("Help")
                presentAndTap("Support")
                // The actual floating button opens the request panel, rather than launching it directly.
                val bubble = automation.windows.asSequence().mapNotNull { it.root }.mapNotNull { root ->
                    try { NodeMasker.flatten(root).firstOrNull { it.description?.startsWith("Saathi assistant") == true } }
                    finally { root.recycle() }
                }.firstOrNull()
                requireNotNull(bubble) { "Floating assistant must be accessible" }
                val down = SystemClock.uptimeMillis()
                listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP).forEach { action ->
                    val event = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, bubble.bounds.exactCenterX(), bubble.bounds.exactCenterY(), 0)
                    try { assertTrue(automation.injectInputEvent(event, true)) } finally { event.recycle() }
                }
                waitUntil { nodes().any { it.text == "What would you like to do?" } }
                assertNull(SaathiSession.presentationKey())
                assertTrue(SaathiSession.isActive())
                assertTrue(automation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK))
                waitUntil { automation.rootInActiveWindow?.packageName?.toString() == instrumentation.context.packageName }
            }
        } finally {
            instrumentation.runOnMainSync {
                val monitor = androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                androidx.test.runner.lifecycle.Stage.values().flatMap { monitor.getActivitiesInStage(it) }
                    .filterIsInstance<com.saathi.AssistantActivity>().forEach { it.finish() }
                context.startActivity(Intent(external).putExtra("close_fixture", true)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
            }
            instrumentation.runOnMainSync { SaathiSession.stop() }
            shell("appops set com.saathi SYSTEM_ALERT_WINDOW $mode")
            shell("input keyevent KEYCODE_HOME")
        }
    }
}
