package com.saathi.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.view.WindowManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saathi.ComplaintDraftActivity
import com.saathi.cyber.ComplaintFacts
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComplaintDraftUiTest {
    @get:Rule val ui = createAndroidComposeRule<ComplaintDraftActivity>()
    private val account = "Synthetic example: someone asked me to send money. I declined."
    private fun fill() {
        ui.onNodeWithText("What happened, in order?").performScrollTo().performTextInput(account)
        androidx.test.espresso.Espresso.closeSoftKeyboard()
    }
    @Test fun copyRequiresReviewAndPerFieldConsentAndKeepsExactAccount() {
        val clipboard = ui.activity.getSystemService(ClipboardManager::class.java)
        ui.runOnUiThread { clipboard.setPrimaryClip(ClipData.newPlainText("Test", "unchanged")) }
        fill()
        ui.onNodeWithText("Copy incident description").performScrollTo().assertIsNotEnabled()
        ui.onNodeWithText("I checked this matches what happened").performScrollTo().performClick()
        ui.onNodeWithText("Copy incident description").performScrollTo().performClick()
        ui.onNodeWithText("Allow copy: Incident description?").assertIsDisplayed()
        ui.onNodeWithText("Not now").performClick()
        ui.runOnUiThread { assertEquals("unchanged", clipboard.primaryClip!!.getItemAt(0).text.toString()) }
        ui.onNodeWithText("Copy incident description").performClick()
        ui.onNodeWithText("Allow copy").performClick()
        ui.runOnUiThread {
            assertEquals(ComplaintFacts(account = account).fields().first().text, clipboard.primaryClip!!.getItemAt(0).text.toString())
            assertTrue(clipboard.primaryClip!!.description.extras!!.getBoolean("android.content.extra.IS_SENSITIVE"))
            clipboard.clearPrimaryClip()
        }
        ui.onNodeWithText("What happened, in order?").performScrollTo().performTextReplacement("Corrected account")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        ui.onNodeWithText("Copy incident description").performScrollTo().assertIsNotEnabled()
        ui.onNodeWithText("I checked this matches what happened").assertIsNotSelected()
    }
    @Test fun consentedCopySurvivesManualPasteAndReturnWithoutAutoSubmission() {
        fill()
        ui.onNodeWithText("I checked this matches what happened").performScrollTo().performClick()
        ui.onNodeWithText("Copy incident description").performScrollTo().performClick()
        ui.onNodeWithText("Allow copy").performClick()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val automation = instrumentation.getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        val external = android.content.Intent().setComponent(android.content.ComponentName(instrumentation.context.packageName, ExternalSurfaceActivity::class.java.name))
            .putExtra("paste_fixture", true).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
        try {
            ui.runOnUiThread { ui.activity.startActivity(external) }
            val deadline = android.os.SystemClock.uptimeMillis() + 10000
            var pasted = false
            while (!pasted && android.os.SystemClock.uptimeMillis() < deadline) {
                val root = automation.rootInActiveWindow
                if (root != null) {
                    val pending = java.util.ArrayDeque<android.view.accessibility.AccessibilityNodeInfo>()
                    pending.add(root)
                    try {
                        while (pending.isNotEmpty()) {
                            val node = pending.removeFirst()
                            try {
                                if (node.isEditable && node.packageName == instrumentation.context.packageName) {
                                    assertTrue(node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_FOCUS))
                                    // A synthetic USER paste in our fixture; no production auto-paste code.
                                    assertTrue(node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_PASTE))
                                    node.refresh()
                                    assertEquals(ComplaintFacts(account=account).fields().first().text,node.text.toString())
                                    pasted = true
                                }
                                for (index in 0 until node.childCount) node.getChild(index)?.let(pending::add)
                            } finally { node.recycle() }
                        }
                    } finally { while(pending.isNotEmpty()) pending.removeFirst().recycle() }
                }
                if (!pasted) android.os.SystemClock.sleep(100)
            }
            assertTrue("Exact reviewed text pasted by synthetic user",pasted)
        } finally {
            instrumentation.targetContext.startActivity(android.content.Intent().setComponent(external.component).putExtra("close_fixture",true)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP))
        }
        ui.onNodeWithText("I checked this matches what happened").performScrollTo().assertIsSelected()
        ui.runOnUiThread { ui.activity.getSystemService(ClipboardManager::class.java).clearPrimaryClip() }
    }
    @Test fun privateWorksheetClearsOnRecreationAndFitsExistingThemes() {
        val preferences = Preferences(ui.activity); val old = preferences.theme
        try {
            for (theme in listOf("Light", "Dark")) {
                ui.runOnUiThread { preferences.theme = theme }; ui.activityRule.scenario.recreate()
                fill()
                ui.onNodeWithText("I checked this matches what happened").performScrollTo().performClick()
                ui.onNodeWithText("Clear worksheet").performScrollTo()
                assertTrue(ui.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
                ui.runOnUiThread { ui.activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
                val out = java.io.File(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir") ?: ui.activity.filesDir.path)
                out.mkdirs()
                java.io.File(out, "draft-${theme.lowercase()}.png").outputStream().use {
                    ui.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
                }
                ui.runOnUiThread { ui.activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE) }
                ui.activityRule.scenario.recreate()
                ui.onNodeWithText(account, substring = true).assertDoesNotExist()
                ui.onNodeWithText("I checked this matches what happened").performScrollTo().assertIsNotEnabled()
            }
        } finally { ui.runOnUiThread { preferences.theme = old } }
    }
}
