package com.saathi.ui

import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saathi.CyberReportActivity
import com.saathi.gateway.PracticeGateway
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Actual Android HTTP -> paired synthetic server. Never a live model assessment. */
@RunWith(AndroidJUnit4::class)
class IncidentAssessmentUiTest {
    @get:Rule val ui = createAndroidComposeRule<CyberReportActivity>()
    @Test fun shareOnceIsRequiredAndPrivateSummaryNeverStartsARequest() {
        assertTrue(android.os.Build.MODEL.startsWith("sdk_") || android.os.Build.FINGERPRINT.contains("generic"))
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val token = ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand("cat /data/local/tmp/saathi-test-token"))
            .bufferedReader().use { it.readText().trim() }
        try {
            ui.runOnUiThread { assertTrue(PracticeGateway.configure(token, true)) }
            val before = PracticeGateway.requestsStarted.get()
            ui.onNode(hasSetTextAction()).performScrollTo().performTextInput("My account is 123456789")
            androidx.test.espresso.Espresso.closeSoftKeyboard()
            ui.onNodeWithText("Ask AI about this issue").performScrollTo().performClick()
            ui.onNodeWithText("Share once").assertDoesNotExist()
            assertEquals(before, PracticeGateway.requestsStarted.get())
            ui.onNode(hasSetTextAction()).performScrollTo().performTextReplacement("Someone pretending to be bank staff tricked me into moving money.")
            androidx.test.espresso.Espresso.closeSoftKeyboard()
            ui.onNodeWithText("Ask AI about this issue").performScrollTo().performClick()
            ui.onNodeWithText("Share this summary for AI assessment?").assertIsDisplayed()
            // Only this synthetic test draft may be captured; production keeps FLAG_SECURE.
            ui.runOnUiThread { ui.activity.window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE) }
            val bitmap = ui.onNode(isDialog()).captureToImage().asAndroidBitmap()
            val directory = java.io.File(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir") ?: ui.activity.filesDir.path).apply { mkdirs() }
            java.io.File(directory, "incident-consent.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            ui.runOnUiThread { ui.activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE) }
            ui.onNodeWithText("Keep private").performClick()
            assertEquals(before, PracticeGateway.requestsStarted.get())
            ui.onNodeWithText("Ask AI about this issue").performClick()
            ui.onNodeWithText("Share once").performClick()
            ui.waitUntil(12000) { ui.onAllNodesWithText("The two AI providers suggest possible financial", substring = true).fetchSemanticsNodes().isNotEmpty() }
            assertEquals(before + 1, PracticeGateway.requestsStarted.get())
            ui.onNodeWithText("The two AI providers suggest possible financial", substring = true).performScrollTo().assertIsDisplayed()
            ui.onNodeWithText("Clear description").performScrollTo().performClick()
            ui.onNodeWithText("The two AI providers suggest possible financial", substring = true).assertDoesNotExist()
            ui.onNodeWithText("Someone pretending", substring = true).assertDoesNotExist()
        } finally { ui.runOnUiThread { PracticeGateway.disable() } }
    }
    @Test fun unconfiguredAssessmentKeepsOfflineAdviceAvailable() {
        ui.runOnUiThread { PracticeGateway.disable() }
        val before = PracticeGateway.requestsStarted.get()
        ui.onNodeWithText("Ask AI about this issue").performScrollTo().performClick()
        ui.onNodeWithText("Share once").assertDoesNotExist()
        ui.onNodeWithText("Show reporting advice").performScrollTo().performClick()
        ui.onNodeWithText("You can report without describing", substring = true).performScrollTo().assertIsDisplayed()
        assertEquals(before, PracticeGateway.requestsStarted.get())
    }
}
