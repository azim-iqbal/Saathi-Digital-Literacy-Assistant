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

/** Process-only credentials. Release destinations are fixed at build time and require HTTPS. */
object PracticeGateway {
    internal val requestsAttempted = java.util.concurrent.atomic.AtomicInteger()
    internal val requestsCancelled = java.util.concurrent.atomic.AtomicInteger()
    internal val requestsRejectedLocally = java.util.concurrent.atomic.AtomicInteger()
    internal val requestsStarted = java.util.concurrent.atomic.AtomicInteger()
    @Volatile private var token: String? = null
    @Volatile private var ai = false
    private val generation = java.util.concurrent.atomic.AtomicLong()
    private val pending = java.util.concurrent.ConcurrentHashMap<String, GatewayCancellation>()
    private val defaultEndpoint = GatewayEndpoint.resolve(com.saathi.BuildConfig.BACKEND_URL, com.saathi.BuildConfig.DEBUG)
    @Volatile private var activeEndpoint: String? = defaultEndpoint
    private val io = Executors.newFixedThreadPool(2)
    private val slots = Semaphore(2)
    private val cancellationIo = Executors.newFixedThreadPool(2)
    private val cancellationSlots = Semaphore(2)
    private val main = Handler(Looper.getMainLooper())
    fun enabled() = token != null && !ai
    fun aiEnabled() = token != null && ai
    fun available() = defaultEndpoint != null || activeEndpoint != null
    fun serverLabel() = activeEndpoint ?: defaultEndpoint ?: "Server not configured for this build"
    fun setCustomEndpoint(url: String?): Boolean {
        if (!com.saathi.BuildConfig.DEBUG) return false
        val replacement = if (url.isNullOrBlank()) defaultEndpoint else GatewayEndpoint.debugOverride(url) ?: return false
        if (replacement != activeEndpoint) {
            invalidateConnection()
            com.saathi.orchestrator.SaathiSession.stop()
            token = null; ai = false
            activeEndpoint = replacement
        }
        return true
    }
    private fun emulator() = android.os.Build.FINGERPRINT.startsWith("generic") ||
        android.os.Build.FINGERPRINT.startsWith("google/sdk_gphone") ||
        android.os.Build.MODEL.startsWith("sdk_") || android.os.Build.MODEL.startsWith("sdk_gphone")
    private val transport = GatewayHttpTransport()

    fun openSetup(context: Context) { context.startActivity(Intent(context, GatewaySetupActivity::class.java)) }
    fun configure(value: String, enableAi: Boolean = false): Boolean {
        if (!available() || (!com.saathi.BuildConfig.DEBUG && !enableAi)) return false
        if (value.length !in 32..256 || value.startsWith("REPLACE_") || value.any { it.code !in 33..126 }) return false
        invalidateConnection()
        com.saathi.orchestrator.SaathiSession.stop()
        token = value; ai = enableAi
        return true
    }
    private fun invalidateConnection() {
        generation.incrementAndGet()
        pending.values.toList().forEach { it.cancel() }
        pending.clear()
    }
    fun disable() { invalidateConnection(); com.saathi.orchestrator.SaathiSession.stop(); token = null; ai = false; activeEndpoint = defaultEndpoint }

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

    /** Call only after the person approves the displayed summary for this request. */
    fun assessIncident(snapshot: IncidentAssessmentRequest, consent: Boolean, callback: (GatewayResult) -> Unit): GatewayCancellation =
        submit("/v1/incident-assessment", snapshot.requestId, IncidentGatewayCodec.encode(snapshot),
            if (consent && aiEnabled() && IncidentAssessmentPolicy.allowed(snapshot.summary) &&
                snapshot.concern in setOf("MONEY", "OTHER", "UNSURE")) token else null,
            11000, { IncidentGatewayCodec.decode(it, snapshot) }, callback)

    fun connectionStatus(callback: (GatewayResult) -> Unit): GatewayCancellation =
        submit("/v1/connection-status", java.util.UUID.randomUUID().toString(), "{}", token, 2500,
            { ConnectionCodec.decode(it, null) }, callback)

    fun checkProviders(consent: Boolean, callback: (GatewayResult) -> Unit): GatewayCancellation {
        val id = java.util.UUID.randomUUID().toString()
        return submit("/v1/provider-check", id, JSONObject().put("request_id", id).put("consent", consent).toString(),
            if (consent) token else null, 11000, { ConnectionCodec.decode(it, id) }, callback)
    }

    private fun researchText(value: String, limit: Int) = value.isNotBlank() && value.length <= limit &&
        value.none { it.code < 32 } && !com.saathi.accessibility.SensitiveContent.isSensitive(false, value)

    fun research(goal: String, jurisdiction: String, locale: String, consent: Boolean, claimType: String = "requirements",
                 callback: (GatewayResult) -> Unit): GatewayCancellation {
        val id = java.util.UUID.randomUUID().toString()
        val body = JSONObject().put("request_id", id).put("goal", goal).put("jurisdiction", jurisdiction)
            .put("locale", locale).put("claim_type", claimType).put("consent", consent).toString()
        return submit("/v1/research", id, body,
            if (consent && aiEnabled() && researchText(goal, 160) && researchText(jurisdiction, 80)) token else null,
            8000, { ResearchCodec.decode(it, id, false, locale) }, callback)
    }

    fun planResearch(researchId: String, consent: Boolean, locale: String = "en-IN", callback: (GatewayResult) -> Unit): GatewayCancellation {
        val id = java.util.UUID.randomUUID().toString()
        val body = JSONObject().put("request_id", id).put("research_id", researchId).put("consent", consent).toString()
        return submit("/v1/task-plan", id, body, if (consent && aiEnabled()) token else null,
            11000, { ResearchCodec.decode(it, id, true, locale) }, callback)
    }

    private fun submit(path: String, requestId: String, encoded: String, secret: String?, timeoutMs: Int,
                       decode: (String) -> GatewayResult, callback: (GatewayResult) -> Unit): GatewayCancellation {
        requestsAttempted.incrementAndGet()
        val cancelled = AtomicBoolean(false)
        val ticket = generation.get()
        val origin = activeEndpoint ?: defaultEndpoint
        fun current() = !cancelled.get() && generation.get() == ticket
        fun deliver(result: GatewayResult) {
            if (!current()) return
            com.saathi.accessibility.ObservationDiagnostics.gatewayResult(
                (result as? GatewayResult.Rejected)?.reason ?: "accepted")
            callback(result)
        }
        val rejection = when {
            secret == null || origin == null -> "not_configured"
            encoded.toByteArray().size > 8192 -> "invalid_request"
            !slots.tryAcquire() -> "busy"
            else -> null
        }
        if (rejection != null) {
            requestsRejectedLocally.incrementAndGet()
            main.post { deliver(GatewayResult.Rejected(rejection)) }
            return GatewayCancellation { cancelled.set(true) }
        }
        val credential = requireNotNull(secret)
        val cancelSent = AtomicBoolean(false)
        val activeConnection = java.util.concurrent.atomic.AtomicReference<HttpURLConnection?>()
        val dispatchedOrigin = java.util.concurrent.atomic.AtomicReference<String?>()
        fun sendCancel() {
            val target = dispatchedOrigin.get() ?: return
            if (!cancelSent.compareAndSet(false, true)) return
            runCatching {
                val cancel = openHttp(target, "/v1/cancel", credential, 1000)
                try {
                    val body = JSONObject().put("request_id", requestId).toString().toByteArray()
                    cancel.setFixedLengthStreamingMode(body.size)
                    cancel.outputStream.use { it.write(body) }; cancel.inputStream.close()
                } finally { cancel.disconnect() }
            }
        }
        val cancellation = GatewayCancellation {
            val firstCancellation = cancelled.compareAndSet(false, true)
            if (firstCancellation) requestsCancelled.incrementAndGet()
            if (firstCancellation && cancellationSlots.tryAcquire()) {
                cancellationIo.execute {
                    try { activeConnection.get()?.disconnect(); sendCancel() }
                    finally { cancellationSlots.release() }
                }
            }
        }
        pending[requestId] = cancellation
        io.execute {
            val result = try {
                if (!current()) return@execute
                requestsStarted.incrementAndGet()
                val response = transport.post(
                    GatewayEndpoint.candidates(requireNotNull(origin), com.saathi.BuildConfig.DEBUG, emulator()),
                    path, credential, encoded.toByteArray(Charsets.UTF_8), timeoutMs + 1000,
                    ::current, { activeConnection.set(it) }, { dispatchedOrigin.set(it) }
                )
                if (response.status == 200) decode(response.body)
                    else GatewayResult.Rejected(GatewayRecovery.httpStatus(response.status))
            } catch (error: Exception) {
                val reason = when (error) {
                    is javax.net.ssl.SSLException -> "secure_connection_failed"
                    is java.net.SocketTimeoutException -> "timeout"
                    is java.io.IOException -> if (origin != null && GatewayEndpoint.isLocal(origin)) "local_backend_unreachable" else "connection_failed"
                    else -> "invalid_response"
                }
                GatewayResult.Rejected(reason)
            } finally {
                try { com.saathi.accessibility.ObservationDiagnostics.gatewayBeforeCleanup() }
                finally {
                    activeConnection.set(null)
                    pending.remove(requestId, cancellation)
                    if (cancelled.get()) sendCancel()
                    slots.release()
                }
            }
            // The callback may immediately start another request. Release capacity first,
            // while still checking cancellation/generation again on the main thread.
            main.post { deliver(result) }
        }
        // Cancel promptly on a separate bounded lane, even while the response read is blocked.
        // Server tombstones cover cancellation arriving before request admission. No automatic retry.
        return cancellation
    }

    private fun openHttp(base: String, path: String, token: String, timeoutMs: Int) = (URL(base + path).openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"; doOutput = true; useCaches = false; instanceFollowRedirects = false
        connectTimeout = 2500; readTimeout = timeoutMs
        setRequestProperty("Authorization", "Bearer $token"); setRequestProperty("Content-Type", "application/json")
    }
}
