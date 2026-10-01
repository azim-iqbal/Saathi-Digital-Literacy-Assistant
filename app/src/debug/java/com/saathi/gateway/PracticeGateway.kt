package com.saathi.gateway

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.saathi.core.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicBoolean

/** Debug source set only. Credentials live in process memory; destination is fixed device loopback. */
object PracticeGateway {
    internal val requestsStarted = java.util.concurrent.atomic.AtomicInteger()
    private var token: String? = null
    private var ai = false
    private val io = Executors.newFixedThreadPool(2)
    private val slots = Semaphore(2)
    private val main = Handler(Looper.getMainLooper())
    fun enabled() = token != null && !ai
    fun aiEnabled() = token != null && ai
    fun openSetup(context: Context) { context.startActivity(Intent(context, GatewaySetupActivity::class.java)) }
    fun configure(value: String, enableAi: Boolean = false): Boolean {
        if (value.length !in 32..256 || value.startsWith("REPLACE_") || value.any { it.code !in 33..126 }) return false
        com.saathi.orchestrator.SaathiSession.stop()
        token = value; ai = enableAi
        return true
    }
    fun disable() { com.saathi.orchestrator.SaathiSession.stop(); token = null; ai = false }

    fun request(snapshot: SanitizedScreenSnapshot, callback: (GatewayResult) -> Unit): GatewayCancellation {
        val validation = DualProposalValidator.validate(snapshot, GuidanceProposal(snapshot.sessionId, snapshot.screenRevision,
            snapshot.packageName, snapshot.windowId, ProposedAction.HANDOVER, null, "Check practice", "Observe practice"))
        return submit("/v1/proposals", snapshot.requestId, MockGatewayCodec.encode(snapshot),
            if (enabled() && validation == null) token else null, 2500, { MockGatewayCodec.decode(it, snapshot) }, callback)
    }
    fun requestLive(snapshot: LiveAiSnapshot, callback: (GatewayResult) -> Unit): GatewayCancellation =
        submit("/v1/live-proposals", snapshot.requestId, LiveGatewayCodec.encode(snapshot),
            if (aiEnabled() && LiveAiPolicy.allowed(snapshot.goal) && snapshot.controls.size in 1..32 &&
                snapshot.controls.all { LiveAiPolicy.allowed(it.label, 80) }) token else null,
            11000, { LiveGatewayCodec.decode(it, snapshot) }, callback)

    private fun submit(path: String, requestId: String, encoded: String, secret: String?, timeoutMs: Int,
                       decode: (String) -> GatewayResult, callback: (GatewayResult) -> Unit): GatewayCancellation {
        val cancelled = AtomicBoolean(false)
        if (secret == null || encoded.toByteArray().size > 8192 || !slots.tryAcquire()) {
            main.post { if (!cancelled.get()) callback(GatewayResult.Rejected("unavailable")) }
            return GatewayCancellation { cancelled.set(true) }
        }
        io.execute {
            val deadline = android.os.SystemClock.elapsedRealtime() + timeoutMs + 1000
            var connection: HttpURLConnection? = null
            try {
                if (cancelled.get()) return@execute
                requestsStarted.incrementAndGet()
                connection = connection(path, secret).apply { readTimeout = timeoutMs }
                val body = encoded.toByteArray(Charsets.UTF_8)
                connection.setFixedLengthStreamingMode(body.size)
                connection.outputStream.use { it.write(body) }
                val result = if (connection.responseCode == 200) {
                    require(connection.contentType?.substringBefore(';') == "application/json")
                    val data = connection.inputStream.use { input ->
                        val out = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(1024)
                        while (true) {
                            require(!cancelled.get() && android.os.SystemClock.elapsedRealtime() < deadline)
                            val size = input.read(buffer); if (size < 0) break
                            require(out.size() + size <= 8192); out.write(buffer, 0, size)
                        }
                        out.toString("UTF-8")
                    }
                    decode(data)
                } else GatewayResult.Rejected("connection_failed")
                main.post { if (!cancelled.get()) callback(result) }
            } catch (_: Exception) {
                main.post { if (!cancelled.get()) callback(GatewayResult.Rejected("unavailable")) }
            } finally {
                connection?.disconnect()
                // Best effort server cancellation after this bounded operation. Never enqueue a retry.
                if (cancelled.get()) runCatching {
                    val cancel = connection("/v1/cancel", secret)
                    try {
                        val body = JSONObject().put("request_id", requestId).toString().toByteArray()
                        cancel.setFixedLengthStreamingMode(body.size)
                        cancel.outputStream.use { it.write(body) }; cancel.inputStream.close()
                    } finally { cancel.disconnect() }
                }
                slots.release()
            }
        }
        // Drop callbacks immediately. Socket reads remain bounded and never block the main thread.
        return GatewayCancellation { cancelled.set(true) }
    }

    private fun connection(path: String, token: String) = (URL("http://127.0.0.1:8765$path").openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"; doOutput = true; useCaches = false; instanceFollowRedirects = false
        connectTimeout = 1000; readTimeout = 2500
        setRequestProperty("Authorization", "Bearer $token"); setRequestProperty("Content-Type", "application/json")
    }
}
