package com.saathi.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saathi.core.GatewayResult
import com.saathi.gateway.PracticeGateway
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Real Android HTTP against device-local synthetic servers. No provider calls or host secrets. */
@RunWith(AndroidJUnit4::class)
class GatewayTransportIntegrationTest {
    @Test fun callbackCannotAnnounceCompletionBeforeReleasingRequestCapacity() {
        Server().use { server ->
            val cleaning = CountDownLatch(1)
            val release = CountDownLatch(1)
            val delivered = CountDownLatch(1)
            try {
                configure(server.origin)
                com.saathi.accessibility.ObservationDiagnostics.gatewayCleanupObserver = {
                    cleaning.countDown()
                    release.await(4, TimeUnit.SECONDS)
                }
                main { PracticeGateway.connectionStatus { delivered.countDown() } }
                assertTrue("Worker reached cleanup", cleaning.await(3, TimeUnit.SECONDS))
                // Simulate the OS descheduling a finished worker before its finally block.
                assertFalse("Completion must not race the capacity release", delivered.await(200, TimeUnit.MILLISECONDS))
                release.countDown()
                assertTrue("Result follows resource release", delivered.await(3, TimeUnit.SECONDS))
                assertTrue(status() is GatewayResult.Connection)
            } finally {
                release.countDown()
                com.saathi.accessibility.ObservationDiagnostics.gatewayCleanupObserver = null
                main { PracticeGateway.disable() }
            }
        }
    }
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val token = "synthetic-connection-regression-token-0000"
    private fun main(block: () -> Unit) = instrumentation.runOnMainSync(block)
    private fun configure(origin: String) = main {
        assertTrue(PracticeGateway.setCustomEndpoint(origin))
        assertTrue(PracticeGateway.configure(token, true))
    }
    private fun status(): GatewayResult {
        val done = CountDownLatch(1); var result: GatewayResult? = null
        main { PracticeGateway.connectionStatus { result = it; done.countDown() } }
        assertTrue("Connection callback", done.await(6, TimeUnit.SECONDS))
        return requireNotNull(result)
    }
    private class Server(port: Int = 0, private val blocked: Boolean = false) : AutoCloseable {
        val listener = ServerSocket(port, 8, InetAddress.getByName("127.0.0.1"))
        val origin = "http://127.0.0.1:${listener.localPort}"
        val arrived = CountDownLatch(1); val release = CountDownLatch(1)
        val cancels = AtomicInteger(); val statuses = AtomicInteger()
        private val workers = Executors.newFixedThreadPool(4)
        private val accept = Thread {
            while (!listener.isClosed) {
                val socket = try { listener.accept() } catch (_: Exception) { break }
                workers.execute { runCatching { serve(socket) } }
            }
        }.apply { isDaemon = true; start() }
        private fun serve(socket: Socket) = socket.use {
            socket.soTimeout = 4000
            val input = socket.getInputStream().bufferedReader()
            val path = input.readLine().split(' ')[1]
            var length = 0
            while (true) {
                val line = input.readLine() ?: return@use
                if (line.isEmpty()) break
                if (line.startsWith("Content-Length:", true)) length = line.substringAfter(':').trim().toInt()
            }
            repeat(length) { if (input.read() < 0) return@use }
            val body = if (path == "/v1/cancel") {
                cancels.incrementAndGet(); "{\"cancelled\":true,\"mode\":\"mock\"}"
            } else {
                statuses.incrementAndGet(); arrived.countDown()
                if (blocked) release.await(3, TimeUnit.SECONDS)
                """{"status":"connection","mode":"mock","request_id":null,"reason":"not_requested","providers":[{"provider":"mock-a","model":"fixture","attempts":0,"configured":true,"last":null},{"provider":"mock-b","model":"fixture","attempts":0,"configured":true,"last":null}]}"""
            }
            val bytes = body.toByteArray()
            socket.getOutputStream().apply {
                write("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".toByteArray())
                write(bytes); flush()
            }
        }
        override fun close() { release.countDown(); listener.close(); accept.join(1000); workers.shutdownNow(); workers.awaitTermination(4, TimeUnit.SECONDS) }
    }

    @Test fun disconnectedPhoneStyleLoopbackRecoversWhenServerReturnsWithoutReconfiguration() {
        val port = ServerSocket(0).use { it.localPort }
        try {
            configure("http://127.0.0.1:$port")
            assertEquals("local_backend_unreachable", (status() as GatewayResult.Rejected).reason)
            Server(port).use { server ->
                val rounds = InstrumentationRegistry.getArguments().getString("transport_rounds")?.toIntOrNull()?.coerceIn(1,1000) ?: 10
                repeat(rounds) {
                    val result = status()
                    assertTrue("Recovery request $it: ${(result as? GatewayResult.Rejected)?.reason ?: result.javaClass.simpleName}", result is GatewayResult.Connection)
                }
                assertEquals(rounds, server.statuses.get())
            }
            assertEquals("local_backend_unreachable", (status() as GatewayResult.Rejected).reason)
        } finally { main { PracticeGateway.disable() } }
    }

    @Test fun endpointChangeCancelsAtOldServerAndSuppressesOldResult() {
        Server(blocked = true).use { old -> Server().use { fresh ->
            try {
                configure(old.origin)
                val obsolete = CountDownLatch(1)
                main { PracticeGateway.connectionStatus { obsolete.countDown() } }
                assertTrue(old.arrived.await(3, TimeUnit.SECONDS))
                configure(fresh.origin)
                assertTrue(status() is GatewayResult.Connection)
                old.release.countDown()
                assertFalse(obsolete.await(1200, TimeUnit.MILLISECONDS))
                val end = System.nanoTime() + TimeUnit.SECONDS.toNanos(3)
                while (old.cancels.get() == 0 && System.nanoTime() < end) Thread.sleep(20)
                assertEquals(1, old.cancels.get()); assertEquals(0, fresh.cancels.get())
            } finally { main { PracticeGateway.disable() } }
        } }
    }

    @Test fun aiOptInDoesNotEnableNetworkForLocalPracticeAndBadOriginKeepsConnection() {
        Server().use { server ->
            try {
                configure(server.origin)
                main {
                    assertTrue(PracticeGateway.aiEnabled()); assertFalse(PracticeGateway.enabled())
                    assertFalse(PracticeGateway.setCustomEndpoint("http://untrusted.example/path"))
                    assertEquals(server.origin, PracticeGateway.serverLabel())
                }
                assertTrue(status() is GatewayResult.Connection)
            } finally { main { PracticeGateway.disable() } }
        }
    }
}
