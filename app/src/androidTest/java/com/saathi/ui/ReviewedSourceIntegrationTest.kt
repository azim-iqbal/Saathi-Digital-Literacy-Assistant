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
class ReviewedSourceIntegrationTest {
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

    @Test fun sourceCompanionRetainsTaskAcrossPrivatePauseReturnAndReplacement() {
        assertTrue(android.os.Build.FINGERPRINT.contains("generic") || android.os.Build.MODEL.startsWith("sdk_"))
        val service="com.saathi/com.saathi.accessibility.SaathiAccessibilityService"
        val oldServices=shell("settings get secure enabled_accessibility_services")
        val oldEnabled=shell("settings get secure accessibility_enabled")
        val oldOverlay=Regex("SYSTEM_ALERT_WINDOW: (allow|ignore|deny|default)")
            .find(shell("appops get com.saathi SYSTEM_ALERT_WINDOW"))?.groupValues?.get(1) ?: "default"
        val external=Intent().setComponent(ComponentName(instrumentation.context.packageName,ExternalSurfaceActivity::class.java.name))
            .putExtra("resume_fixture",true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        fun restore(key:String,value:String) { if (value=="null" || value.isBlank()) shell("settings delete secure $key") else shell("settings put secure $key $value") }
        fun fixturePlan(expires:Long=System.currentTimeMillis()+120000)=com.saathi.core.EvidencePlan("Understand registration requirements",listOf(
            com.saathi.core.EvidenceStep("s1","Read requirements",emptyList(),"Registration requires verification.","e1","Synthetic source","https://fixture.example/requirements")),emptyList(),expires)
        try {
            automation.serviceInfo=automation.serviceInfo.apply { flags=flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS }
            shell("appops set com.saathi SYSTEM_ALERT_WINDOW allow")
            val enabled=(oldServices.takeUnless { it=="null" }.orEmpty().split(':').filter { it.isNotBlank() }+service).distinct().joinToString(":")
            shell("settings put secure enabled_accessibility_services $enabled")
            shell("settings put secure accessibility_enabled 1")
            waitFor("Service connected") { com.saathi.accessibility.SaathiAccessibilityService.isConnected() }
            ActivityScenario.launch(MainActivity::class.java).use { mainScreen ->
                val plan=fixturePlan().also { it.review(System.currentTimeMillis()) }
                mainScreen.onActivity { assertTrue(SaathiSession.startReviewedSource(it,plan,"s1",GuidanceLanguage.ENGLISH)); it.startActivity(external) }
                fun companion()=SaathiSession.instruction.value.contains("cannot verify a full HTTPS address") && SaathiSession.currentRequest()==plan.originalGoal
                waitFor("Untrusted external app never supplies browser provenance") { companion() }
                assertFalse(com.saathi.overlay.HighlightOverlayService.hasTarget())
                assertFalse(plan.complete(System.currentTimeMillis()))
                val original=SaathiSession.sessionKey()
                for ((label,state) in listOf("Private interruption" to com.saathi.core.GuidanceSessionState.SENSITIVE_HANDOVER,
                    "Message example" to com.saathi.core.GuidanceSessionState.SENSITIVE_HANDOVER,
                    "Human challenge" to com.saathi.core.GuidanceSessionState.WAITING_FOR_CAPTCHA)) {
                    tap(label); waitFor("Source mode preserves challenge handover") { SaathiSession.status.value==state }
                    assertFalse(com.saathi.overlay.HighlightOverlayService.hasTarget())
                    tap("Return to choices"); waitFor("Source context returns after private action") { companion() }
                    assertEquals(original,SaathiSession.sessionKey())
                }
                main { SaathiSession.pause() }
                assertSame(plan,SaathiSession.reviewedPlan()); assertTrue(SaathiSession.canResume())
                shell("appops set com.saathi SYSTEM_ALERT_WINDOW deny")
                main { assertFalse(SaathiSession.resume(context)) }
                assertSame(plan,SaathiSession.reviewedPlan())
                shell("appops set com.saathi SYSTEM_ALERT_WINDOW allow")
                main { assertTrue(SaathiSession.resume(context)) }
                waitFor("Fresh source observation after resume") { companion() }
                assertNotEquals(original,SaathiSession.sessionKey())
                ActivityScenario.launch(com.saathi.ResearchActivity::class.java).use { research ->
                    waitFor("Return pauses companion and invalidates applicability") { SaathiSession.canResume() && !plan.reviewed }
                    research.recreate()
                    assertSame(plan,SaathiSession.reviewedPlan()); assertFalse(plan.reviewed)
                    main { assertFalse(SaathiSession.resume(context)) }
                    assertSame(plan,SaathiSession.reviewedPlan())
                    research.onActivity { plan.review(System.currentTimeMillis()); assertTrue(SaathiSession.startReviewedSource(it,plan,"s1",GuidanceLanguage.ENGLISH)) }
                    // Finishing the review screen discards its own plan rather than leaving a hidden resumable task.
                }
                waitFor("Closed review discards plan") { SaathiSession.reviewedPlan()==null }
                val replacement=fixturePlan().also { it.review(System.currentTimeMillis()) }
                ActivityScenario.launch(MainActivity::class.java).use { restarted ->
                    restarted.onActivity { assertTrue(SaathiSession.startReviewedSource(it,replacement,"s1",GuidanceLanguage.ENGLISH)); it.startActivity(external) }
                }
                waitFor("Companion restarted") { SaathiSession.instruction.value.contains("cannot verify a full HTTPS address") }
                main { assertTrue(SaathiSession.changeLiveRequest("Help",SaathiSession.sessionKey())) }
                waitFor("New request clears old plan") { SaathiSession.instruction.value.startsWith("Find “Help”") }
                assertNull(SaathiSession.reviewedPlan())
                val expires=fixturePlan(System.currentTimeMillis()+2000).also { it.review(System.currentTimeMillis()) }
                main { assertTrue(SaathiSession.startReviewedSource(context,expires,"s1",GuidanceLanguage.ENGLISH)) }
                waitFor("Quiet page loses expired research") { SaathiSession.instruction.value.contains("sources have expired") }
                main { SaathiSession.pause(); assertFalse(SaathiSession.resume(context)); SaathiSession.stop() }
                assertFalse(SaathiSession.canResume()); assertNull(SaathiSession.reviewedPlan())
            }
        } finally {
            main { SaathiSession.stop() }
            restore("enabled_accessibility_services",oldServices); restore("accessibility_enabled",oldEnabled)
            shell("appops set com.saathi SYSTEM_ALERT_WINDOW $oldOverlay")
        }
    }
}
