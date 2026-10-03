package com.saathi.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.view.WindowManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saathi.CyberReportActivity
import com.saathi.cyber.CyberReportGuide
import com.saathi.orchestrator.SaathiSession
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CyberReportUiTest {
    @get:Rule val ui = createAndroidComposeRule<CyberReportActivity>()

    @Test fun missingOverlayPermissionOffersOptionalSettingsAndKeepsInAppCopyAvailable() {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        fun shell(command: String) = android.os.ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command)).bufferedReader().use { it.readText() }
        val old = Regex("SYSTEM_ALERT_WINDOW: (allow|ignore|deny|default)").find(shell("appops get com.saathi SYSTEM_ALERT_WINDOW"))?.groupValues?.get(1) ?: "default"
        try {
            shell("appops set com.saathi SYSTEM_ALERT_WINDOW deny")
            ui.onNodeWithText("Show floating link helper").performScrollTo().performClick()
            ui.onNodeWithText("Show a helper over your browser?").assertIsDisplayed()
            ui.onNodeWithText("Open settings").assertHasClickAction()
            ui.onNodeWithText("Not now").performClick()
            ui.onNodeWithText("Copy official link").performScrollTo().performClick()
            ui.onNodeWithText("Allow copying this link?").assertIsDisplayed()
            ui.onNodeWithText("Not now").performClick()
        } finally { shell("appops set com.saathi SYSTEM_ALERT_WINDOW $old") }
    }

    @Test fun consentDefaultsToNoCopyAndOnlyCopiesOfficialUrlAfterApproval() {
        val clipboard = ui.activity.getSystemService(ClipboardManager::class.java)
        ui.runOnUiThread { clipboard.setPrimaryClip(ClipData.newPlainText("Test", "unchanged-test-value")) }
        ui.onNodeWithText("Text only").performScrollTo().assertIsSelected()
        assertFalse(SaathiSession.isActive())
        ui.onNodeWithText("Copy official link").performScrollTo().performClick()
        ui.onNodeWithText("Allow copying this link?").assertIsDisplayed()
        ui.onNodeWithText("Not now").performClick()
        ui.runOnUiThread { assertEquals("unchanged-test-value", clipboard.primaryClip!!.getItemAt(0).text.toString()) }
        ui.onNodeWithText("Copy official link").performClick()
        ui.onNodeWithText("Allow copy").performClick()
        ui.runOnUiThread { assertEquals(CyberReportGuide.PORTAL, clipboard.primaryClip!!.getItemAt(0).text.toString()); clipboard.clearPrimaryClip() }
    }

    @Test fun shortButtonsWrapAndDraftClearsOnRecreationWhileChecklistPositionSurvives() {
        val button = ui.onNodeWithText("Call 1930").fetchSemanticsNode().boundsInRoot
        val root = ui.onRoot().fetchSemanticsNode().boundsInRoot
        assertTrue("Short label must not stretch across the screen", button.width < root.width * .7f)
        ui.onNode(hasSetTextAction()).performScrollTo().performTextInput("Synthetic test incident")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        ui.onNodeWithText("Next").performScrollTo().performClick()
        ui.activityRule.scenario.recreate()
        ui.onNodeWithText("Synthetic test incident", substring = true).assertDoesNotExist()
        ui.onNodeWithText("Step 2 of 8", substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test fun guideHasBoundedStepsAndUsesExistingLightAndDarkTheme() {
        val preferences = Preferences(ui.activity)
        val old = preferences.theme
        try {
            for (theme in listOf("Light", "Dark")) {
                ui.runOnUiThread { preferences.theme = theme }
                ui.activityRule.scenario.recreate()
                ui.onNodeWithText("Call 1930").assertIsDisplayed()
                // Synthetic empty screen only: permit the test screenshot, never user drafts.
                ui.runOnUiThread { ui.activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
                screenshot("cyber-${theme.lowercase()}")
                ui.runOnUiThread { ui.activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE) }
            }
            ui.onNodeWithText("Back", useUnmergedTree = false).performScrollTo().assertIsNotEnabled()
            repeat(CyberReportGuide.steps.lastIndex) { ui.onNodeWithText("Next").performScrollTo().performClick() }
            ui.onNodeWithText("Next").assertIsNotEnabled()
            ui.onNodeWithText("Save and follow up", substring = true).performScrollTo().assertIsDisplayed()
            assertFalse(SaathiSession.isActive())
        } finally { ui.runOnUiThread { preferences.theme = old } }
    }

    private fun screenshot(name: String) {
        val output = java.io.File(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir") ?: ui.activity.filesDir.path)
        output.mkdirs()
        val bitmap = ui.onRoot().captureToImage().asAndroidBitmap()
        java.io.File(output, "$name.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }
}
