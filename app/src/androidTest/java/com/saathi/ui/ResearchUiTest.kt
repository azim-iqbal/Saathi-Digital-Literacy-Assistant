package com.saathi.ui

import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saathi.ResearchActivity
import com.saathi.gateway.PracticeGateway
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic retrieval and plan adapters only. No real websites or models are called. */
@RunWith(AndroidJUnit4::class)
class ResearchUiTest {
    @get:Rule val ui = createAndroidComposeRule<ResearchActivity>()
    @Test fun explicitConsentShowsSourcesAndDependencyChecklist() {
        assertTrue(ui.activity.window.attributes.flags and android.view.WindowManager.LayoutParams.FLAG_SECURE != 0)
        val automation=InstrumentationRegistry.getInstrumentation().uiAutomation
        val token=ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand("cat /data/local/tmp/saathi-test-token"))
            .bufferedReader().use { it.readText().trim() }
        val oldTheme=Preferences(ui.activity).theme
        try {
            ui.runOnUiThread { Preferences(ui.activity).theme="Dark"; assertTrue(PracticeGateway.configure(token,true)) }
            ui.activityRule.scenario.recreate()
            ui.onAllNodes(hasSetTextAction())[0].performTextInput("Understand application requirements")
            ui.onAllNodes(hasSetTextAction())[1].performTextInput("Region A")
            androidx.test.espresso.Espresso.closeSoftKeyboard()
            ui.onNodeWithText("Find sources").performScrollTo().assertIsNotEnabled()
            ui.onNode(isToggleable()).performScrollTo().performClick()
            ui.onNodeWithText("Find sources").performScrollTo().performClick()
            ui.waitUntil(12000) { ui.onAllNodesWithText("Allow AI plan proposal").fetchSemanticsNodes().isNotEmpty() }
            ui.onNodeWithText("Allow AI plan proposal").performScrollTo().performClick()
            ui.waitUntil(12000) { ui.onAllNodesWithText("I reviewed applicability").fetchSemanticsNodes().isNotEmpty() }
            ui.onNodeWithText("I reviewed applicability").performScrollTo().performClick()
            ui.onNodeWithText("Verification").performScrollTo().assertExists()
            ui.onNodeWithText("Read source in browser").performScrollTo().performClick()
            ui.onNodeWithText("Open this source?").assertExists()
            ui.onNodeWithText("Not now").performClick()
            ui.onNodeWithText("Open this source?").assertDoesNotExist()
            assertFalse(com.saathi.orchestrator.SaathiSession.isActive())

            ui.onNodeWithText("I completed this step").performScrollTo().assertIsDisplayed()
            screenshot("research-dependency-dark")
            ui.onNodeWithText("I completed this step").performScrollTo().performClick()
            ui.onNodeWithText("Registration").performScrollTo().assertExists()
            ui.onNodeWithText("I completed this step").performScrollTo().performClick()
            ui.onNodeWithText("Application").performScrollTo().assertExists()
            ui.onNodeWithText("Yes").performScrollTo().performClick()
            ui.onNodeWithText("Possibly eligible based on your answers; official verification and complete criteria are still needed.").performScrollTo().assertExists()
            screenshot("research-eligibility-dark")
        } finally { ui.runOnUiThread { PracticeGateway.disable(); Preferences(ui.activity).theme=oldTheme } }
    }
    @Test fun incidentReviewRequiresSecondConsentAndStaysAHypothesis() {
        assertTrue(ui.activity.window.attributes.flags and android.view.WindowManager.LayoutParams.FLAG_SECURE != 0)
        val automation=InstrumentationRegistry.getInstrumentation().uiAutomation
        val token=ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand("cat /data/local/tmp/saathi-test-token"))
            .bufferedReader().use { it.readText().trim() }
        try {
            ui.runOnUiThread { assertTrue(PracticeGateway.configure(token,true)) }
            // The real server's per-user retrieval spacing remains enabled in the fixture.
            android.os.SystemClock.sleep(10100)
            ui.onAllNodes(hasSetTextAction())[0].performTextInput("Service unavailable")
            ui.onAllNodes(hasSetTextAction())[1].performTextInput("Region A")
            androidx.test.espresso.Espresso.closeSoftKeyboard()
            ui.onNodeWithText("Service issue").performScrollTo().performClick()
            ui.onNode(isToggleable()).performScrollTo().performClick()
            ui.onNodeWithText("Find sources").performScrollTo().performClick()
            ui.waitUntil(12000) { ui.onAllNodesWithText("Allow AI issue review").fetchSemanticsNodes().isNotEmpty() }
            ui.onNodeWithText("Proposed explanation",substring=true).assertDoesNotExist()
            ui.onNodeWithText("Allow AI issue review").performScrollTo().performClick()
            ui.waitUntil(12000) { ui.onAllNodesWithText("Proposed explanation",substring=true).fetchSemanticsNodes().isNotEmpty() }
            ui.onNodeWithText("This hypothesis is not a confirmed diagnosis",substring=true).performScrollTo().assertExists()
            ui.onNodeWithText("I completed this step").assertDoesNotExist()
        } finally { ui.runOnUiThread { PracticeGateway.disable() } }
    }
    @Test fun hindiAtLargeSystemFontKeepsConsentAndActionsReachable() {
        val automation=InstrumentationRegistry.getInstrumentation().uiAutomation
        fun shell(command: String)=ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command))
            .bufferedReader().use { it.readText().trim() }
        val previousScale=shell("settings get system font_scale")
        val preferences=Preferences(ui.activity)
        val previousLanguage=preferences.language
        try {
            ui.runOnUiThread { preferences.language=com.saathi.language.GuidanceLanguage.HINDI }
            shell("settings put system font_scale 2.0")
            ui.activityRule.scenario.recreate()
            ui.onNodeWithText("स्रोत खोजें").performScrollTo().assertIsDisplayed().assertIsNotEnabled()
            val action=ui.onNodeWithText("स्रोत खोजें").fetchSemanticsNode().boundsInRoot
            val root=ui.onRoot().fetchSemanticsNode().boundsInRoot
            assertTrue(action.left>=root.left && action.right<=root.right)
            ui.onNodeWithText("आम शब्दों का मतलब").performScrollTo().performClick()
            ui.onNodeWithText("वापस").performScrollTo().assertIsDisplayed()
            screenshot("research-hindi-large-font")
        } finally {
            ui.runOnUiThread { preferences.language=previousLanguage }
            if (previousScale.matches(Regex("[0-9]+[.][0-9]+"))) shell("settings put system font_scale $previousScale")
            else shell("settings delete system font_scale")
        }
    }
    private fun screenshot(name:String) {
        val file=java.io.File(ui.activity.filesDir,"$name.png")
        // Capture synthetic fixture evidence only; production remains protected.
        ui.runOnUiThread { ui.activity.window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE) }
        try {
            val image=ui.onRoot().captureToImage()
            file.outputStream().use { image.asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
        } finally { ui.runOnUiThread { ui.activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE) } }
    }
}
