package com.saathi.ui

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saathi.gateway.PracticeGateway
import com.saathi.orchestrator.SaathiSession
import com.saathi.overlay.HighlightOverlayService
import com.saathi.speech.VoiceConversationService
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Reuses actual service/HTTP assertions in one process to expose cross-cycle state leaks.
 * No remote server or model accounts; memory samples do not certify absence of leaks.
 */
@RunWith(AndroidJUnit4::class)
class MixedLifecycleEnduranceTest {
    @Test fun permissionRecreationIncompleteScreensAndNetworkRecoveryStayIsolated() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        assertTrue(android.os.Build.MODEL.startsWith("sdk_") || android.os.Build.FINGERPRINT.contains("generic"))
        val args = InstrumentationRegistry.getArguments()
        val rounds = args.getString("mixed_rounds")?.toIntOrNull()?.coerceIn(1, 1000) ?: 4
        val minimumMs = (args.getString("mixed_minutes")?.toLongOrNull()?.coerceIn(0, 120) ?: 0) * 60_000
        val broad = args.getString("mixed_broad") == "true"
        val output = File(args.getString("additionalTestOutputDir") ?: instrumentation.targetContext.filesDir.path).apply { mkdirs() }
        val samples = JSONArray()
        val started = SystemClock.elapsedRealtime()
        val requests = PracticeGateway.requestsStarted.get()
        val cancellations = PracticeGateway.requestsCancelled.get()
        var phase = "starting"
        fun save(completed: Int, status: String) {
            File(output, "mixed-endurance.json").writeText(JSONObject()
                .put("status", status).put("requested_rounds", rounds).put("completed_rounds", completed)
                .put("elapsed_ms", SystemClock.elapsedRealtime() - started).put("samples", samples)
                .put("minimum_duration_ms", minimumMs).put("phase", phase).put("broad", broad)
                .put("http_requests", PracticeGateway.requestsStarted.get() - requests)
                .put("cancellations", PracticeGateway.requestsCancelled.get() - cancellations)
                .put("genuine_provider_calls", 0)
                .put("scope", "Emulator service recreation, incomplete-tree recovery, loopback loss/recovery and obsolete-response rejection; no actual speech or physical-device certification.")
                .toString(2))
        }
        var completed = 0
        try {
            while (completed < rounds || SystemClock.elapsedRealtime() - started < minimumMs) {
                val round = completed
                check(round < 1000) { "Duration target exceeded bounded round capacity" }
                phase = "incomplete_tree"; save(completed, "RUNNING")
                ObservationFailureIntegrationTest().oversizedTreeCannotHidePrivateFieldAndFreshCompleteTreeRecoversOriginalTask()
                phase = "permission_recreation"; save(completed, "RUNNING")
                PermissionLossIntegrationTest().overlayAndAccessibilityRevocationStopAndNeverAutoResume()
                phase = "network_recovery"; save(completed, "RUNNING")
                GatewayTransportIntegrationTest().disconnectedPhoneStyleLoopbackRecoversWhenServerReturnsWithoutReconfiguration()
                phase = "endpoint_replacement"; save(completed, "RUNNING")
                GatewayTransportIntegrationTest().endpointChangeCancelsAtOldServerAndSuppressesOldResult()
                if (broad) {
                    phase = "private_captcha_pause"; save(completed, "RUNNING")
                    PauseResumeIntegrationTest().manualPauseAndPrivateChallengeRoundTripsRequireFreshObservations()
                    phase = "webview_detour"; save(completed, "RUNNING")
                    LiveAccessibilityIntegrationTest().realServiceTracksExternalDetourReturnAndWebChangeThenStops()
                    phase = "voice_lifecycle"; save(completed, "RUNNING")
                    VoiceLifecycleTest().backgroundWaitingAndOldNotificationCannotStopReplacement()
                }
                instrumentation.runOnMainSync {
                    assertFalse("No session survives round $round", SaathiSession.isActive())
                    assertNull(SaathiSession.presentationKey())
                    assertFalse(HighlightOverlayService.hasTarget())
                    assertFalse(VoiceConversationService.isRunning())
                }
                val runtime = Runtime.getRuntime()
                samples.put(JSONObject().put("round", round + 1)
                    .put("elapsed_ms", SystemClock.elapsedRealtime() - started)
                    .put("pss_kb", android.os.Debug.getPss())
                    .put("heap_used_bytes", runtime.totalMemory() - runtime.freeMemory())
                    .put("open_fds", File("/proc/self/fd").list()?.size ?: -1)
                    .put("live_threads", Thread.getAllStackTraces().keys.count { it.isAlive }))
                completed++
                save(completed, "RUNNING")
            }
            phase = "complete"; save(completed, "PASS")
        } catch (failure: Throwable) {
            save(completed, "FAIL")
            throw failure
        } finally {
            instrumentation.runOnMainSync { PracticeGateway.disable(); SaathiSession.stop() }
        }
    }
}
