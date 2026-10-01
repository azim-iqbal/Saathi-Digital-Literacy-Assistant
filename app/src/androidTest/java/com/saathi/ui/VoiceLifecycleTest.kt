package com.saathi.ui

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saathi.MainActivity
import com.saathi.language.GuidanceLanguage
import com.saathi.orchestrator.SaathiSession
import com.saathi.speech.VoiceConversationService
import com.saathi.speech.VoicePhase
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Exercises the real foreground service in a waiting state; never feeds or records speech. */
@RunWith(AndroidJUnit4::class)
class VoiceLifecycleTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context get() = instrumentation.targetContext
    private fun shell(command: String): String =
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command))
            .bufferedReader().use { it.readText() }
    private fun waitUntil(condition: () -> Boolean) {
        val end = SystemClock.uptimeMillis() + 10_000
        while (!condition() && SystemClock.uptimeMillis() < end) SystemClock.sleep(50)
        assertTrue("Expected service transition within 10 seconds", condition())
    }
    private fun main(action: () -> Unit) = instrumentation.runOnMainSync(action)

    @Test fun backgroundWaitingAndOldNotificationCannotStopReplacement() {
        val prior = shell("appops get com.saathi SYSTEM_ALERT_WINDOW")
        val mode = Regex("SYSTEM_ALERT_WINDOW: (allow|deny|ignore|default)").find(prior)?.groupValues?.get(1) ?: "default"
        shell("appops set com.saathi SYSTEM_ALERT_WINDOW allow")
        instrumentation.uiAutomation.grantRuntimePermission("com.saathi", Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 33) instrumentation.uiAutomation.grantRuntimePermission("com.saathi", Manifest.permission.POST_NOTIFICATIONS)
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                main {
                    SaathiSession.start(context, "Pay my water bill", GuidanceLanguage.ENGLISH)
                    SaathiSession.startConversation(context)
                }
                val supported = VoiceConversationService.supported(context)
                val output = File(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir") ?: context.filesDir.path)
                output.mkdirs()
                File(output, "voice-capability.txt").writeText("onDeviceRecognizerAvailable=$supported; SDK=${Build.VERSION.SDK_INT}; no audio input/output quality tested")
                if (supported) {
                    waitUntil { VoiceConversationService.phase.value == VoicePhase.WAITING }
                    val notification = context.getSystemService(NotificationManager::class.java)
                    waitUntil { notification.activeNotifications.any { it.id == 41 } }
                    val oldStop = notification.activeNotifications.first { it.id == 41 }.notification.actions.last().actionIntent
                    shell("input keyevent KEYCODE_HOME")
                    waitUntil { scenario.state == Lifecycle.State.CREATED }
                    assertEquals(VoicePhase.WAITING, VoiceConversationService.phase.value)
                    assertTrue(SaathiSession.isActive())
                    main { SaathiSession.stop() }
                    waitUntil { !VoiceConversationService.isRunning() }
                    // MainActivity is deliberately non-exported; use the app's own return intent.
                    main { context.startActivity(android.content.Intent(context, MainActivity::class.java)
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)) }
                    waitUntil { scenario.state == Lifecycle.State.RESUMED }
                    main {
                        SaathiSession.start(context, "Pay my water bill", GuidanceLanguage.ENGLISH)
                        SaathiSession.startConversation(context)
                    }
                    waitUntil { VoiceConversationService.phase.value == VoicePhase.WAITING }
                    oldStop.send()
                    instrumentation.waitForIdleSync()
                    assertTrue("Old Stop must not end the replacement session", SaathiSession.isActive())
                    assertEquals(VoicePhase.WAITING, VoiceConversationService.phase.value)
                } else {
                    waitUntil { VoiceConversationService.phase.value == VoicePhase.UNAVAILABLE }
                    assertTrue("Visual session survives unavailable recognition", SaathiSession.isActive())
                }
                main { SaathiSession.stop() }
                waitUntil { !VoiceConversationService.isRunning() }
                assertEquals(VoicePhase.OFF, VoiceConversationService.phase.value)
            }
        } finally {
            main { SaathiSession.stop() }
            shell("appops set com.saathi SYSTEM_ALERT_WINDOW $mode")
        }
    }
}
