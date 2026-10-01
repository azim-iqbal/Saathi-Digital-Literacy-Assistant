package com.saathi.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saathi.VoiceSetupActivity
import com.saathi.orchestrator.SaathiSession
import com.saathi.speech.VoiceConversationService
import com.saathi.speech.VoicePhase
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class VoiceSetupUiTest {
    @get:Rule val ui = createAndroidComposeRule<VoiceSetupActivity>()
    @Test fun availabilityCheckNeverStartsMicrophoneOrGuidance() {
        val prior = Preferences(ui.activity).theme
        try {
            ui.runOnUiThread { Preferences(ui.activity).theme = "Light" }
            ui.activityRule.scenario.recreate()
            ui.onNodeWithText("Check availability").performScrollTo().performClick()
            ui.waitUntil(25_000) {
                ui.onAllNodes(hasText("Your selected language", substring = true) or
                    hasText("The speech service", substring = true) or hasText("This device cannot", substring = true))
                    .fetchSemanticsNodes().isNotEmpty()
            }
            assertEquals(VoicePhase.OFF, VoiceConversationService.phase.value)
            assertFalse(SaathiSession.isActive())
            ui.onNodeWithText("Download selected language").performScrollTo().assertIsEnabled()
            screenshot("voice-setup-light")
            ui.runOnUiThread { Preferences(ui.activity).theme = "Dark" }
            ui.activityRule.scenario.recreate()
            ui.onNodeWithText("Offline voice setup").assertExists()
            screenshot("voice-setup-dark")
        } finally { ui.runOnUiThread { Preferences(ui.activity).theme = prior } }
    }
    private fun screenshot(name: String) {
        val directory = File(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir") ?: ui.activity.filesDir.path).apply { mkdirs() }
        File(directory, "$name.png").outputStream().use {
            ui.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
