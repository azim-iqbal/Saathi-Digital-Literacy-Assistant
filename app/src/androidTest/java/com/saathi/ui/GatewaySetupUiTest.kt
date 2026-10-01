package com.saathi.ui

import android.view.WindowManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saathi.gateway.GatewaySetupActivity
import com.saathi.gateway.PracticeGateway
import com.saathi.orchestrator.SaathiSession
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GatewaySetupUiTest {
    @get:Rule val ui = createAndroidComposeRule<GatewaySetupActivity>()
    @Test fun aiConnectionRequiresSeparateChoiceAndForgetsToken() {
        try {
            assertTrue(ui.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
            assertFalse(PracticeGateway.aiEnabled())
            val before = PracticeGateway.requestsStarted.get()
            ui.onNodeWithText("Use AI navigation in other apps").performScrollTo().performClick().assertIsSelected()
            ui.onNode(hasText("By enabling AI navigation", substring = true)).assertExists()
            ui.onNode(hasSetTextAction()).performScrollTo().performTextInput("synthetic-ui-token-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")
            androidx.test.espresso.Espresso.closeSoftKeyboard()
            ui.onNodeWithText("Enable AI navigation").performScrollTo().performClick()
            ui.runOnIdle { assertTrue(PracticeGateway.aiEnabled()); assertFalse(SaathiSession.isActive()) }
            ui.onNodeWithText("Disable and forget token").performScrollTo().performClick()
            ui.runOnIdle {
                assertFalse(PracticeGateway.aiEnabled()); assertFalse(PracticeGateway.enabled())
                assertEquals("Setup alone sends no model request", before, PracticeGateway.requestsStarted.get())
            }
        } finally { ui.runOnUiThread { PracticeGateway.disable() } }
    }
}
