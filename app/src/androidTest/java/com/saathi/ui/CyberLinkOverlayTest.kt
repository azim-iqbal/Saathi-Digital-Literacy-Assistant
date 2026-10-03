package com.saathi.ui

import android.content.*
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saathi.MainActivity
import com.saathi.cyber.CyberReportGuide
import com.saathi.overlay.CyberLinkOverlayService
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CyberLinkOverlayTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val automation get() = instrumentation.uiAutomation
    private fun shell(command: String) = ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command)).bufferedReader().use { it.readText() }
    private fun waitUntil(condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 8000
        while (!condition() && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(100)
        assertTrue(condition())
    }
    private fun click(text: String) {
        var clicked = false
        waitUntil {
            automation.windows.forEach { window ->
                val root = window.root ?: return@forEach
                try { root.findAccessibilityNodeInfosByText(text).forEach { node ->
                    try { if (node.text?.toString() == text && node.isClickable) clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK) || clicked }
                    finally { node.recycle() }
                } } finally { root.recycle() }
            }
            clicked
        }
    }
    @Test fun reviewedDraftHelperCopiesOnlyChosenFieldAndStopsOnRevocation() {
        val original = Regex("SYSTEM_ALERT_WINDOW: (allow|ignore|deny|default)").find(shell("appops get com.saathi SYSTEM_ALERT_WINDOW"))?.groupValues?.get(1) ?: "default"
        val context = instrumentation.targetContext
        val service = Intent(context, CyberLinkOverlayService::class.java).setAction(CyberLinkOverlayService.DRAFT_ACTION)
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        val field = com.saathi.cyber.ComplaintField("Incident description", "Synthetic incident only. No money sent.", "Match the narrative field yourself.")
        automation.serviceInfo = automation.serviceInfo.apply { flags = flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS }
        shell("appops set com.saathi SYSTEM_ALERT_WINDOW allow")
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity {
                    clipboard.setPrimaryClip(ClipData.newPlainText("Test", "unchanged"))
                    com.saathi.cyber.ComplaintHelperHandoff.prepare(listOf(field))
                    it.startService(service)
                }
                click("Copy incident description")
                click("Not now")
                waitUntil { !shell("dumpsys window windows").contains("Saathi reporting link") }
                scenario.onActivity {
                    assertEquals("unchanged", clipboard.primaryClip!!.getItemAt(0).text.toString())
                    com.saathi.cyber.ComplaintHelperHandoff.prepare(listOf(field)); it.startService(service)
                }
                click("Copy incident description"); click("Allow copy")
                scenario.onActivity {
                    assertEquals(field.text, clipboard.primaryClip!!.getItemAt(0).text.toString())
                    clipboard.clearPrimaryClip()
                }
                shell("appops set com.saathi SYSTEM_ALERT_WINDOW deny")
                waitUntil { !shell("dumpsys window windows").contains("Saathi reporting link") }
                scenario.onActivity { assertTrue(com.saathi.cyber.ComplaintHelperHandoff.take().isEmpty()) }
            }
        } finally { context.stopService(service); com.saathi.cyber.ComplaintHelperHandoff.clear(); shell("appops set com.saathi SYSTEM_ALERT_WINDOW $original") }
    }

    @Test fun externalHelperRequiresConsentAndDisappearsAfterPermissionLoss() {
        val original = Regex("SYSTEM_ALERT_WINDOW: (allow|ignore|deny|default)").find(shell("appops get com.saathi SYSTEM_ALERT_WINDOW"))?.groupValues?.get(1) ?: "default"
        val context = instrumentation.targetContext
        val service = Intent(context, CyberLinkOverlayService::class.java)
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        val fixture = ComponentName(instrumentation.context.packageName, ExternalSurfaceActivity::class.java.name)
        automation.serviceInfo = automation.serviceInfo.apply { flags = flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS }
        shell("appops set com.saathi SYSTEM_ALERT_WINDOW allow")
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity {
                    clipboard.setPrimaryClip(ClipData.newPlainText("Test", "untouched"))
                    it.startService(service)
                    it.startActivity(Intent().setComponent(fixture).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                waitUntil { shell("dumpsys window windows").contains("Saathi reporting link") }
                waitUntil { automation.windows.any { window ->
                    val root = window.root ?: return@any false
                    try { root.packageName?.toString() == instrumentation.context.packageName && root.findAccessibilityNodeInfosByText("Help").isNotEmpty() }
                    finally { root.recycle() }
                } }
                click("Copy reporting link")
                automation.waitForIdle(300, 5000)
                val output = java.io.File(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir") ?: context.filesDir.path)
                output.mkdirs()
                automation.takeScreenshot()?.let { bitmap -> java.io.File(output, "cyber-floating-consent.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) } }
                click("Not now")
                waitUntil { !shell("dumpsys window windows").contains("Saathi reporting link") }
                scenario.onActivity { it.startActivity(Intent(it, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)) }
                waitUntil { automation.rootInActiveWindow?.packageName?.toString() == "com.saathi" }
                scenario.onActivity {
                    assertEquals("untouched", clipboard.primaryClip!!.getItemAt(0).text.toString())
                    it.startService(service)
                    it.startActivity(Intent().setComponent(fixture).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
                }
                waitUntil { automation.rootInActiveWindow?.packageName?.toString() == instrumentation.context.packageName }
                click("Copy reporting link"); click("Allow copy")
                scenario.onActivity { it.startActivity(Intent(it, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)) }
                waitUntil { automation.rootInActiveWindow?.packageName?.toString() == "com.saathi" }
                scenario.onActivity { assertEquals(CyberReportGuide.PORTAL, clipboard.primaryClip!!.getItemAt(0).text.toString()); clipboard.clearPrimaryClip() }
                shell("appops set com.saathi SYSTEM_ALERT_WINDOW deny")
                waitUntil { !shell("dumpsys window windows").contains("Saathi reporting link") }
                scenario.onActivity { it.startActivity(Intent().setComponent(fixture).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP).putExtra("close_fixture", true)) }
            }
        } finally { context.stopService(service); shell("appops set com.saathi SYSTEM_ALERT_WINDOW $original") }
    }
}
