package com.saathi.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saathi.CyberReportActivity
import com.saathi.gateway.GatewaySetupActivity
import com.saathi.gateway.PracticeGateway
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Response deliberately arrives after Pause and before Stop. Loopback only. */
@RunWith(AndroidJUnit4::class)
class PausedRequestUiTest {
    @get:Rule val ui = createEmptyComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private class Server : AutoCloseable {
        val listener = ServerSocket(0, 4, InetAddress.getByName("127.0.0.1"))
        val arrived = CountDownLatch(1)
        val release = CountDownLatch(1)
        val replied = CountDownLatch(1)
        private val worker = Thread {
            runCatching {
                listener.accept().use { socket ->
                    socket.soTimeout = 3000
                    val input = socket.getInputStream().bufferedReader()
                    input.readLine()
                    var length = 0
                    while (true) {
                        val line = input.readLine() ?: return@use
                        if (line.isEmpty()) break
                        if (line.startsWith("Content-Length:", true)) length = line.substringAfter(':').trim().toInt()
                    }
                    repeat(length) { if (input.read() < 0) return@use }
                    arrived.countDown()
                    check(release.await(10, TimeUnit.SECONDS))
                    socket.getOutputStream().write("HTTP/1.1 503 Service Unavailable\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray())
                }
            }
            replied.countDown()
        }.apply { isDaemon = true; start() }
        override fun close() { release.countDown(); listener.close(); worker.join(2000) }
    }
    private fun configure(server: Server) = instrumentation.runOnMainSync {
        assertTrue(PracticeGateway.setCustomEndpoint("http://127.0.0.1:${server.listener.localPort}"))
        assertTrue(PracticeGateway.configure("synthetic-paused-request-token-0000", true))
    }
    private fun <T : android.app.Activity> pauseThenReply(scenario: ActivityScenario<T>, server: Server) {
        lateinit var original: T
        scenario.onActivity { original = it }
        assertTrue(server.arrived.await(3, TimeUnit.SECONDS))
        scenario.moveToState(Lifecycle.State.STARTED)
        assertEquals(Lifecycle.State.STARTED, scenario.state)
        server.release.countDown()
        assertTrue(server.replied.await(3, TimeUnit.SECONDS))
        android.os.SystemClock.sleep(300)
        instrumentation.waitForIdleSync()
        // A stopped task is not guaranteed to gain foreground focus through a synthetic
        // lifecycle move. Return through Android's task navigation, keeping this instance.
        instrumentation.targetContext.startActivity(android.content.Intent(original, original.javaClass)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP))
        val deadline = android.os.SystemClock.uptimeMillis() + 5000
        while (scenario.state != Lifecycle.State.RESUMED && android.os.SystemClock.uptimeMillis() < deadline)
            android.os.SystemClock.sleep(50)
        assertEquals(Lifecycle.State.RESUMED, scenario.state)
        scenario.onActivity { assertSame("Return must preserve the cancelled request screen", original, it) }
        ui.waitForIdle()
    }
    @Test fun connectionCheckCannotPublishAfterPause() {
        Server().use { server ->
            configure(server)
            try {
                ActivityScenario.launch(GatewaySetupActivity::class.java).use { scenario ->
                    ui.onNodeWithText("Refresh server status").performScrollTo().performClick()
                    pauseThenReply(scenario, server)
                    ui.onNodeWithText("Check cancelled when you left", substring = true).assertExists()
                    ui.onNodeWithText("Cancel check").assertDoesNotExist()
                }
            } finally { instrumentation.runOnMainSync { PracticeGateway.disable() } }
        }
    }
    @Test fun incidentAssessmentCannotPublishAfterPause() {
        Server().use { server ->
            configure(server)
            try {
                ActivityScenario.launch(CyberReportActivity::class.java).use { scenario ->
                    ui.onNode(hasSetTextAction()).performScrollTo().performTextInput("Someone impersonating bank staff tricked me into moving money.")
                    androidx.test.espresso.Espresso.closeSoftKeyboard()
                    ui.onNodeWithText("Ask AI about this issue").performScrollTo().performClick()
                    ui.onNodeWithText("Share once").performClick()
                    pauseThenReply(scenario, server)
                    ui.onNodeWithText("AI assessment cancelled when you left", substring = true).assertExists()
                    ui.onNodeWithText("Cancel assessment").assertDoesNotExist()
                }
            } finally { instrumentation.runOnMainSync { PracticeGateway.disable() } }
        }
    }
}
