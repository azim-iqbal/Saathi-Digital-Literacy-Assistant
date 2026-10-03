package com.saathi.ui

import android.os.ParcelFileDescriptor
import android.view.WindowManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saathi.gateway.GatewaySetupActivity
import com.saathi.gateway.PracticeGateway
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConnectionScreenUiTest {
    @get:Rule val ui = createAndroidComposeRule<GatewaySetupActivity>()
    @Test fun checkButtonUsesRealLocalTransportAndShowsSeparateProviderEvidence() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val token = ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand("cat /data/local/tmp/saathi-test-token")).bufferedReader().use { it.readText().trim() }
        try {
            ui.onNodeWithText("Use AI navigation in other apps").performScrollTo().performClick()
            ui.onNode(hasSetTextAction()).performScrollTo().performTextInput(token)
            androidx.test.espresso.Espresso.closeSoftKeyboard()
            ui.onNodeWithText("Enable AI navigation").performScrollTo().performClick()
            ui.onNodeWithText("Check APIs").performScrollTo().performClick()
            ui.onNodeWithText("Run check").performClick()
            ui.waitUntil(15000) { ui.onAllNodesWithText("Paired synthetic guidance passed", substring = true).fetchSemanticsNodes().isNotEmpty() }
            ui.onNodeWithText("Paired synthetic guidance passed", substring = true).performScrollTo().assertExists()
            ui.runOnUiThread { ui.activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
            val out = java.io.File(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir") ?: ui.activity.filesDir.path)
            out.mkdirs()
            java.io.File(out, "connection-synthetic-evidence.png").outputStream().use {
                ui.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
            ui.runOnUiThread { ui.activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE) }
        } finally { ui.runOnUiThread { PracticeGateway.disable() } }
    }
}
