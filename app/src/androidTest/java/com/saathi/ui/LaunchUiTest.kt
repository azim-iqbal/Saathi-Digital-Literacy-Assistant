package com.saathi.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saathi.LaunchActivity
import com.saathi.R
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class LaunchUiTest {
    @get:Rule val ui = createAndroidComposeRule<LaunchActivity>()

    @Test fun launcherOpensDirectlyAndSurvivesRecreation() {
        ui.onNodeWithText("Saathi").assertIsDisplayed()
        ui.activityRule.scenario.recreate()
        ui.onNodeWithText("Saathi").assertIsDisplayed()
        val icon = ui.activity.packageManager.getApplicationIcon(ui.activity.packageName)
        assertTrue("Installed launcher should use its maskable vector icon", icon is AdaptiveIconDrawable)
        val output = File(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir") ?: ui.activity.filesDir.path)
        output.mkdirs()
        for ((name, drawable) in listOf("launcher-icon" to icon, "vector-mark" to ui.activity.getDrawable(R.drawable.ic_saathi_mark)!!)) {
            val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
            drawable.setBounds(0, 0, 512, 512)
            drawable.draw(Canvas(bitmap))
            File(output, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }

    @Test fun returningFromBackgroundKeepsContentUsable() {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        fun shell(command: String) {
            android.os.ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command)).use { it.readBytes() }
        }
        // Exercise the actual Home/launcher route; forcing RESUMED cannot bring a background task forward.
        shell("input keyevent KEYCODE_HOME")
        ui.waitUntil(10_000) { ui.activityRule.scenario.state == Lifecycle.State.CREATED }
        shell("am start -W -n com.saathi/.LaunchActivity -f 0x10020000")
        ui.waitUntil(10_000) { ui.activityRule.scenario.state == Lifecycle.State.RESUMED }
        ui.onNodeWithText("Saathi").assertIsDisplayed()
    }
}
