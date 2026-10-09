package com.saathi.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saathi.ResearchActivity
import com.saathi.gateway.PracticeGateway
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Loopback-only HTTP fixtures: no provider credentials, remote sites or model calls. */
@RunWith(AndroidJUnit4::class)
class ResearchRecoveryUiTest {
    @get:Rule val ui = createAndroidComposeRule<ResearchActivity>()
    private class Server(private val holdPlan: Boolean = false) : AutoCloseable {
        val release = java.util.concurrent.CountDownLatch(1)
        val events = java.util.concurrent.CopyOnWriteArrayList<String>()
        fun record(event: String) { events.add("${android.os.SystemClock.elapsedRealtime()}: $event") }
        val listener = ServerSocket(0, 8, InetAddress.getByName("127.0.0.1"))
        val plans = AtomicInteger()
        val workers = Executors.newFixedThreadPool(2)
        val thread = Thread {
            while (!listener.isClosed) {
                val socket = try { listener.accept() } catch (_: Exception) { break }
                workers.execute { runCatching { socket.use {
                    socket.soTimeout = 3000
                    val input = socket.getInputStream().bufferedReader()
                    val path = input.readLine().split(' ')[1]
                    var length = 0
                    while (true) {
                        val line = input.readLine() ?: return@use
                        if (line.isEmpty()) break
                        if (line.startsWith("Content-Length:", true)) length = line.substringAfter(':').trim().toInt()
                    }
                    val chars = CharArray(length)
                    var offset = 0
                    while (offset < length) { val n = input.read(chars, offset, length-offset); if (n < 0) return@use; offset += n }
                    val request = JSONObject(String(chars))
                    if (path == "/v1/task-plan" && holdPlan) { plans.incrementAndGet(); record("plan_received"); val signalled = release.await(5,TimeUnit.SECONDS); record("plan_released signal=$signalled") }
                    val body = when (path) {
                        "/v1/research" -> """{"status":"researched","request_id":"${request.getString("request_id")}","evidence":[{"evidence_id":"aaaaaaaaaaaaaaaaaaaaaaaa","source_url":"https://fixture.example.test/","source_title":"Synthetic requirements","source_type":"official","authority_basis":"Synthetic test review","retrieved_at_ms":${System.currentTimeMillis()},"published_or_updated":null,"jurisdiction":"Region A","snippet":"Registration requires verification.","content_hash":"${"a".repeat(64)}","claim_type":"requirements","confidence":"retrieved_not_independently_verified"}],"limitations":[]}"""
                        "/v1/task-plan" -> """{"status":"rejected","reason":"${if ((if (holdPlan) plans.get() else plans.incrementAndGet())==1) "provider_timeout" else "research_expired"}"}"""
                        else -> """{"cancelled":true,"mode":"mock"}"""
                    }
                    val bytes = body.toByteArray()
                    socket.getOutputStream().apply {
                        write("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".toByteArray())
                        write(bytes); flush()
                    }
                } } }
            }
        }.apply { isDaemon = true; start() }
        override fun close() { release.countDown(); listener.close(); thread.join(1000); workers.shutdownNow(); workers.awaitTermination(4,TimeUnit.SECONDS) }
    }
    @Test fun timeoutPreservesSourcesAndOnlyExplicitRetryRunsThenExpiryRequiresRefresh() {
        Server().use { server ->
            try {
                ui.runOnUiThread {
                    assertTrue(PracticeGateway.setCustomEndpoint("http://127.0.0.1:${server.listener.localPort}"))
                    assertTrue(PracticeGateway.configure("synthetic-research-recovery-token-0000",true))
                }
                ui.onAllNodes(hasSetTextAction())[0].performTextInput("Understand requirements")
                ui.onAllNodes(hasSetTextAction())[1].performTextInput("Region A")
                androidx.test.espresso.Espresso.closeSoftKeyboard()
                ui.onNode(isToggleable()).performScrollTo().performClick()
                ui.onNodeWithText("Find sources").performScrollTo().performClick()
                ui.waitUntil(10000) { ui.onAllNodesWithText("Allow AI plan proposal").fetchSemanticsNodes().isNotEmpty() }
                ui.onNodeWithText("Allow AI plan proposal").performScrollTo().performClick()
                ui.waitUntil(10000) { ui.onAllNodesWithText("Nothing will retry automatically.",substring=true).fetchSemanticsNodes().isNotEmpty() }
                ui.onNodeWithText("Registration requires verification.",substring=true).assertExists()
                android.os.SystemClock.sleep(1200)
                assertEquals(1,server.plans.get())
                ui.onNodeWithText("Allow AI plan proposal").performScrollTo().performClick()
                ui.waitUntil(10000) { ui.onAllNodesWithText("This research is expired",substring=true).fetchSemanticsNodes().isNotEmpty() }
                assertEquals(2,server.plans.get())
                ui.onNodeWithText("Registration requires verification.",substring=true).assertExists()
                ui.onNodeWithText("Allow AI plan proposal").assertDoesNotExist()
                ui.onNodeWithText("Find sources").performScrollTo().assertIsEnabled()
                ui.onAllNodes(hasSetTextAction())[0].performScrollTo().performTextReplacement("Another task")
                ui.onNodeWithText("Registration requires verification.",substring=true).assertDoesNotExist()
            } finally { ui.runOnUiThread { PracticeGateway.disable() } }
        }
    }
    @Test fun leavingDuringPlanRequestDiscardsLateFailureAndDoesNotRestoreRetry() {
        Server(holdPlan=true).use { server ->
            val callback = androidx.test.runner.lifecycle.ActivityLifecycleCallback { activity, stage ->
                if (activity is ResearchActivity) server.record("activity_$stage")
            }
            val monitor = androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
            ui.runOnUiThread { monitor.addLifecycleCallback(callback) }
            try {
                ui.runOnUiThread {
                    assertTrue(PracticeGateway.setCustomEndpoint("http://127.0.0.1:${server.listener.localPort}"))
                    assertTrue(PracticeGateway.configure("synthetic-research-recovery-token-0000",true))
                }
                ui.onAllNodes(hasSetTextAction())[0].performTextInput("Understand requirements")
                ui.onAllNodes(hasSetTextAction())[1].performTextInput("Region A")
                androidx.test.espresso.Espresso.closeSoftKeyboard()
                ui.onNode(isToggleable()).performScrollTo().performClick()
                ui.onNodeWithText("Find sources").performScrollTo().performClick()
                ui.waitUntil(10000) { ui.onAllNodesWithText("Allow AI plan proposal").fetchSemanticsNodes().isNotEmpty() }
                ui.onNodeWithText("Allow AI plan proposal").performScrollTo().performClick()
                ui.waitUntil(5000) { server.plans.get()==1 }
                server.record("test_observed_plan")
                val automation=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation
                android.os.ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand("input keyevent KEYCODE_HOME")).use { it.readBytes() }
                server.record("home_command_completed")
                android.os.SystemClock.sleep(500)
                server.record("test_releasing_response")
                server.release.countDown()
                android.os.SystemClock.sleep(1200)
                ui.activity.startActivity(android.content.Intent(ui.activity,ResearchActivity::class.java)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                ui.waitForIdle()
                ui.onNodeWithText("Allow AI plan proposal").assertDoesNotExist()
                ui.onNodeWithText("Nothing will retry automatically.",substring=true).assertDoesNotExist()
                ui.onNodeWithText("Find sources").performScrollTo().assertIsEnabled()
                assertEquals(1,server.plans.get())
            } finally {
                ui.runOnUiThread { monitor.removeLifecycleCallback(callback); PracticeGateway.disable() }
                val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                val output = java.io.File(androidx.test.platform.app.InstrumentationRegistry.getArguments().getString("additionalTestOutputDir") ?: instrumentation.targetContext.filesDir.path).apply { mkdirs() }
                java.io.File(output,"research-lifecycle-order.txt").writeText(server.events.joinToString("\n"))
            }
        }
    }

}
