package com.saathi.ui

import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import com.saathi.ui.navigation.IndicatorPosition
import com.saathi.ui.navigation.GlassRendering
import org.junit.Assert.assertTrue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saathi.MainActivity
import com.saathi.language.GuidanceLanguage
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class SaathiUiTest {
    @Test fun speechSpeedPersistsWithoutStartingConversation() {
        launch()
        ui.onNodeWithTag("nav-settings").performClick()
        ui.onNodeWithText("Spoken guidance").performScrollTo().performClick()
        ui.onNodeWithText("Slow").performScrollTo().performClick()
        ui.onNodeWithText("Preview voice").performScrollTo().assertHasClickAction()
        assertEquals(.75f, Preferences(ui.activity).speechRate)
        assertEquals(com.saathi.speech.VoicePhase.OFF, com.saathi.speech.VoiceConversationService.phase.value)
        screenshot("voice-settings")
        ui.activityRule.scenario.recreate()
        assertEquals(.75f, Preferences(ui.activity).speechRate)
        ui.onNodeWithTag("nav-settings").performClick()
        ui.onNodeWithText("Slow").performScrollTo().assertIsSelected()
    }
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    private fun launch(dark: Boolean = false, hindi: Boolean = false, scale: Float = 1f, welcome: Boolean = false, motion: Boolean = false, narrow: Boolean = false) {
        val generation = System.nanoTime()
        ui.runOnUiThread {
            Preferences(ui.activity).apply {
                welcomed = !welcome
                language = if (hindi) GuidanceLanguage.HINDI else GuidanceLanguage.ENGLISH
                theme = if (dark) "Dark" else "Light"
                reducedMotion = !motion
                reducedTransparency = false
                speech = false
            }
            ui.activity.setContent {
                key(generation) {
                    CompositionLocalProvider(LocalDensity provides Density(ui.activity.resources.displayMetrics.density, scale)) {
                        Box(if (narrow) Modifier.width(320.dp) else Modifier) { SaathiApp() }
                    }
                }
            }
        }
        ui.waitForIdle()
    }
    private fun screenshot(name: String, dialog: Boolean = false) {
        val output = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
        val directory = output?.let(::File) ?: ui.activity.filesDir
        directory.mkdirs()
        val file = File(directory, "$name.png")
        file.outputStream().use { (if (dialog) ui.onNode(isDialog()) else ui.onRoot()).captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    @Test fun lightHomeAndTaskRouting() {
        launch()
        ui.onNodeWithText("Start help").assertIsDisplayed().performClick()
        ui.onAllNodesWithText("Your practice task", useUnmergedTree = true).onFirst().assertExists()
        ui.onNode(hasSetTextAction()).performTextInput("Practise water bill")
        ui.onNodeWithText("Continue to setup").performScrollTo().performClick()
        ui.onNodeWithText("Screen guidance").assertExists()
        ui.onNodeWithText("Start guided practice").performScrollTo().assertIsNotEnabled()
    }
    @Test fun homeScreenshots() {
        launch(); screenshot("home-light")
        ui.onNodeWithText("Settings").performClick()
        ui.onNodeWithText("Dark").performScrollTo().performClick()
        ui.onNodeWithText("Home").performClick()
        ui.waitForIdle()
        assertEquals("Dark", Preferences(ui.activity).theme)
        assertEquals(android.graphics.Color.rgb(19, 21, 19), ui.onRoot().captureToImage().asAndroidBitmap().getPixel(4, 4))
        screenshot("home-dark")
    }
    @Test fun hindiLargeTextRemainsScrollable() {
        launch(hindi = true, scale = 2f)
        ui.onNodeWithText("मदद शुरू करें").performScrollTo().assertIsDisplayed()
        screenshot("home-hindi-200")
        ui.onNodeWithText("मदद शुरू करें").performClick()
        ui.onNode(hasSetTextAction()).assertExists()
    }
    @Test fun welcomeLanguageAndPrivacyDeletion() {
        launch(welcome = true)
        ui.onNodeWithText("हिन्दी").performScrollTo().performClick()
        ui.onNodeWithText("शुरू करें").performScrollTo().performClick()
        ui.onNodeWithText("सेटिंग").performClick()
        ui.onNodeWithText("गोपनीयता और नियंत्रण").performScrollTo().performClick()
        ui.onNodeWithText("स्थानीय डेटा मिटाएँ").performScrollTo().performClick()
        ui.onAllNodesWithText("स्थानीय डेटा मिटाएँ").onLast().performClick()
        ui.onNodeWithText("Choose your language").assertExists()
    }
    @Test fun bottomSelectionFollowsNestedRoutesAndBack() {
        launch()
        ui.onNodeWithTag("nav-home").assertIsSelected()
        ui.onNodeWithText("Start help").performClick()
        ui.onNodeWithTag("nav-practice").assertIsSelected()
        ui.onNodeWithText("Back").performClick()
        ui.onNodeWithTag("nav-home").assertIsSelected()
        ui.onNodeWithTag("nav-settings").performClick()
        ui.onNodeWithText("Privacy & control").performScrollTo().performClick()
        ui.onNodeWithTag("nav-settings").assertIsSelected()
    }

    @Test fun bottomIndicatorSlidesAndRapidTapRetargets() {
        launch(motion = true)
        ui.mainClock.autoAdvance = false
        try {
            ui.onNodeWithTag("nav-settings").performClick()
            ui.mainClock.advanceTimeBy(64)
            val halfway = ui.onNodeWithTag("navigation-dock").fetchSemanticsNode().config[IndicatorPosition]
            assertTrue("Indicator should physically move before settling: $halfway", halfway > 0f && halfway < 2f)
            ui.onNodeWithTag("nav-practice").performClick()
            ui.mainClock.advanceTimeBy(1500)
            assertEquals(1f, ui.onNodeWithTag("navigation-dock").fetchSemanticsNode().config[IndicatorPosition], .001f)
            ui.onNodeWithTag("nav-practice").assertIsSelected()
        } finally { ui.mainClock.autoAdvance = true }
    }

    @Test fun categoryIndicatorTracksSwipeAndRestoresSelection() {
        launch(motion = true)
        ui.onNodeWithTag("nav-practice").performClick()
        ui.onNodeWithTag("category-electricity").assertIsSelected()
        screenshot("practice-light")
        ui.onNodeWithTag("practice-pager").performTouchInput {
            down(center)
            moveTo(Offset(center.x - width * .36f, center.y), delayMillis = 300)
        }
        val dragged = ui.onNodeWithTag("practice-tabs").fetchSemanticsNode().config[IndicatorPosition]
        assertTrue("Selection follows an unfinished drag: $dragged", dragged > .1f && dragged < 1f)
        ui.onNodeWithTag("practice-pager").performTouchInput { up() }
        ui.waitForIdle()
        ui.onNodeWithTag("category-water").performClick()
        ui.onNodeWithTag("category-water").assertIsSelected()
        ui.onNodeWithTag("nav-settings").performClick()
        ui.onNodeWithText("Dark").performScrollTo().performClick()
        ui.onNodeWithTag("nav-practice").performClick()
        ui.onNodeWithTag("category-water").assertIsSelected()
        screenshot("practice-dark")
        ui.onNodeWithTag("choose-water").performScrollTo().performClick()
        ui.onNodeWithText("Screen guidance").assertExists()
        ui.onNodeWithTag("nav-practice").assertIsSelected()
    }

    @Test fun reducedTransparencyPersistsThroughRecreation() {
        launch()
        ui.onNodeWithTag("nav-settings").performClick()
        ui.onNodeWithText("Reduce transparency").performScrollTo().performClick()
        ui.onNodeWithText("Reduce transparency").assertIsOn()
        assertEquals("opaque", ui.onNodeWithTag("navigation-dock").fetchSemanticsNode().config[GlassRendering])
        ui.activityRule.scenario.recreate()
        ui.waitForIdle()
        assertTrue(Preferences(ui.activity).reducedTransparency)
        assertEquals("opaque", ui.onNodeWithTag("navigation-dock").fetchSemanticsNode().config[GlassRendering])
        screenshot("navigation-opaque")
    }

    @Test fun reducedMotionIsImmediateAndHindiNarrowTabsAreReachable() {
        launch(hindi = true, scale = 2f, narrow = true)
        ui.onNodeWithTag("nav-practice").performClick()
        assertEquals(1f, ui.onNodeWithTag("navigation-dock").fetchSemanticsNode().config[IndicatorPosition], .001f)
        ui.onNodeWithTag("category-television").performScrollTo().performClick()
        ui.onNodeWithTag("category-television").assertIsSelected()
        ui.onNodeWithTag("page-Practice").performTouchInput { swipeUp() }
        ui.onNodeWithTag("choose-television").assertIsDisplayed()
        screenshot("practice-hindi-200-narrow")
        ui.onNodeWithTag("choose-television").performClick()
        ui.onNodeWithTag("nav-practice").assertIsSelected()
    }

    @Test fun glassActionsKeepSemanticsAndOpaqueFallback() {
        launch(dark = true)
        ui.onNodeWithText("Start help").assertHasClickAction().performClick()
        ui.onNode(hasSetTextAction()).performTextInput("Practise water bill")
        screenshot("glass-intake-dark")
        ui.onNodeWithText("Continue to setup").performScrollTo().performClick()
        ui.onNodeWithText("Start guided practice").performScrollTo().assertIsNotEnabled()
        screenshot("glass-disabled-dark")
        ui.onNodeWithTag("nav-settings").performClick()
        ui.onNodeWithText("Reduce transparency").performScrollTo().performClick()
        ui.onNodeWithTag("nav-home").performClick()
        ui.onNodeWithText("Start help").assert(SemanticsMatcher.expectValue(GlassRendering, "opaque"))
        screenshot("glass-home-opaque")
        ui.onNodeWithTag("nav-settings").performClick()
        ui.onNodeWithText("Privacy & control").performScrollTo().performClick()
        ui.onNodeWithText("Clear local data").performScrollTo().performClick()
        screenshot("glass-clear-dialog", dialog = true)
        ui.onNodeWithText("Cancel").performClick()
        ui.onNodeWithTag("nav-settings").assertIsSelected()
    }

    @Test fun navigationMotionEvidence() {
        launch(motion = true)
        val samples = java.util.Collections.synchronizedList(mutableListOf<Long>())
        val worker = android.os.HandlerThread("navigation-frame-metrics").apply { start() }
        val window = ui.activity.window
        val listener = android.view.Window.OnFrameMetricsAvailableListener { _, metrics, _ ->
            samples.add(metrics.getMetric(android.view.FrameMetrics.TOTAL_DURATION))
        }
        ui.runOnUiThread { window.addOnFrameMetricsAvailableListener(listener, android.os.Handler(worker.looper)) }
        try {
            repeat(4) {
                ui.onNodeWithTag("nav-practice").performClick()
                ui.onNodeWithTag("category-water").performClick()
                ui.onNodeWithTag("category-television").performClick()
                ui.onNodeWithTag("category-electricity").performClick()
                ui.onNodeWithTag("nav-settings").performClick()
                ui.onNodeWithTag("nav-home").performClick()
            }
            ui.waitForIdle()
        } finally {
            ui.runOnUiThread { window.removeOnFrameMetricsAvailableListener(listener) }
            worker.quitSafely(); worker.join(2000)
        }
        val frames = synchronized(samples) { samples.sorted() }
        assertTrue("Frame metrics should be available", frames.isNotEmpty())
        val report = org.json.JSONObject().apply {
            put("device", android.os.Build.MODEL)
            put("sdk", android.os.Build.VERSION.SDK_INT)
            put("frames", frames.size)
            put("p50_ms", frames[frames.size / 2] / 1_000_000.0)
            put("p95_ms", frames[((frames.size - 1) * .95).toInt()] / 1_000_000.0)
            put("over_16_67ms", frames.count { it > 16_666_667 })
            put("note", "Debug build on a software-rendered emulator, not physical-device jank certification. Includes page composition, gesture, and test synchronization work.")
        }
        val output = File(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir") ?: ui.activity.filesDir.path)
        output.mkdirs(); File(output, "navigation-frame-metrics.json").writeText(report.toString(2))
    }

}
