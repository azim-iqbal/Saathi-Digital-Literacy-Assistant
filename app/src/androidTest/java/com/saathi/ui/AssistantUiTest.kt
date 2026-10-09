package com.saathi.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saathi.AssistantActivity
import com.saathi.orchestrator.SaathiSession
import com.saathi.speech.VoiceConversationService
import com.saathi.speech.VoicePhase
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AssistantUiTest {
    @get:Rule val ui = createAndroidComposeRule<AssistantActivity>()
    @Test fun pausedTaskIsVisibleAndCanBeDiscardedWithoutRestartingListening() {
        val priorTheme = Preferences(ui.activity).theme
        try {
            ui.runOnUiThread {
                Preferences(ui.activity).theme = "Dark"
                SaathiSession.startLive(ui.activity, "Help", com.saathi.language.GuidanceLanguage.ENGLISH, false)
                SaathiSession.pause()
            }
            ui.activityRule.scenario.recreate()
            ui.onNode(hasSetTextAction()).assertTextContains("Help")
            ui.onNodeWithText("Resume guidance").performScrollTo().assertExists()
            assertFalse(SaathiSession.isActive())
            assertEquals(VoicePhase.OFF, VoiceConversationService.phase.value)
            screenshot("assistant-paused")
            ui.onNodeWithText("Discard paused task").performScrollTo().performClick()
            ui.onNodeWithText("Resume guidance").assertDoesNotExist()
            assertFalse(SaathiSession.canResume())
        } finally { ui.runOnUiThread { SaathiSession.stop(); Preferences(ui.activity).theme = priorTheme } }
    }
    @Test fun taskIntakeDefaultsToTextAndDoesNotStartListening() {
        val priorTheme = Preferences(ui.activity).theme
        try {
            ui.runOnUiThread { Preferences(ui.activity).theme = "Light" }
            ui.activityRule.scenario.recreate()
            ui.onNodeWithText("Text only").assertIsSelected()
            ui.onNode(hasSetTextAction()).performTextInput("Find Help")
            ui.onNodeWithText("Text + voice").performScrollTo().performClick().assertIsSelected()
            ui.onNodeWithText("Text only").performClick().assertIsSelected()
            assertEquals(VoicePhase.OFF, VoiceConversationService.phase.value)
            assertFalse(SaathiSession.isActive())
            ui.onNodeWithText("Speak your request").performScrollTo().assertHasClickAction()
            androidx.test.espresso.Espresso.closeSoftKeyboard()
            screenshot("assistant-light")
            ui.runOnUiThread { Preferences(ui.activity).theme = "Dark" }
            ui.activityRule.scenario.recreate()
            ui.onNodeWithText("Text only").performScrollTo().assertIsSelected()
            ui.onNodeWithText("Speak your request").performScrollTo().assertHasClickAction()
            screenshot("assistant-dark")
        } finally {
            ui.runOnUiThread { Preferences(ui.activity).theme = priorTheme }
        }
    }
    private fun screenshot(name: String) {
        val output = java.io.File(androidx.test.platform.app.InstrumentationRegistry.getArguments().getString("additionalTestOutputDir") ?: ui.activity.filesDir.path)
        output.mkdirs()
        val image = ui.onRoot().captureToImage()
        java.io.File(output, "$name.png").outputStream().use {
            image.asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }
    @Test fun stopClearsLiveSessionAndTextChoiceCancelsSpeechPreference() {
        ui.runOnUiThread {
            // No service is started: mode selection must not start a session itself.
            SaathiSession.setSpokenGuidance(true)
        }
        ui.onNodeWithText("Text only").performScrollTo().performClick()
        assertFalse(SaathiSession.hasSpokenGuidance())
        assertFalse(SaathiSession.isActive())
    }
}
