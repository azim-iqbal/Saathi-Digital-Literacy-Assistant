package com.saathi.gateway

import com.saathi.core.*
import com.saathi.language.GuidanceLanguage

/** Strictly bounded metadata only; provider bodies and secrets never become display text. */
internal object ConnectionCodec {
    fun decode(body: String, expectedId: String?): GatewayResult = try {
        val d = MockGatewayCodec.parse(body)
        require(d.keys == setOf("status", "mode", "request_id", "reason", "providers"))
        require(d["status"] == "connection" && d["request_id"] == expectedId)
        val mode = d["mode"] as String
        require(mode in setOf("mock", "dual_ai"))
        val rows = d["providers"] as List<*>
        require(rows.size == 2)
        val identities = rows.map { (it as Map<*, *>)["provider"] }.toSet()
        require(identities == if (mode == "mock") setOf("mock-a", "mock-b") else setOf("gemini", "groq"))
        fun count(value: Any?): Long? = if (value == null) null else (value as Long).also { require(it in 0..Int.MAX_VALUE.toLong()) }
        val report = buildString {
            appendLine(if (mode == "mock") "Server reached · Simulated providers only" else "Server reached · Gemini + Groq mode")
            appendLine("This is the running server's configuration. Editing a file requires restarting it.")
            for (item in rows) {
                val row = item as Map<*, *>
                require(row.keys == setOf("provider", "model", "attempts", "configured", "last"))
                val model = row["model"] as String
                require(model.matches(Regex("[A-Za-z0-9._/-]{1,100}")))
                val configured = row["configured"] as Boolean
                val attempts = count(row["attempts"])!!
                appendLine("\n${row["provider"]} · $model")
                appendLine("Attempts admitted this server session: $attempts (not billed usage)")
                if (!configured) appendLine("Not configured. Restore the server-side key and model, then restart the server.")
                require(row["last"] == null || row["last"] is Map<*, *>)
                val last = row["last"] as? Map<*, *>
                if (last == null) appendLine(if (mode == "mock") "No external API request." else "No API evidence for this check. Use Check APIs after configuring both providers.")
                else {
                    require(last.keys == setOf("request_id", "outcome", "real_api", "http_received", "checked_at_ms", "elapsed_ms", "input_tokens", "output_tokens", "total_tokens"))
                    val id = last["request_id"] as String
                    require(id.matches(Regex("[A-Za-z0-9_-]{1,64}")) && (expectedId == null || id == expectedId))
                    val real = last["real_api"] as Boolean
                    val received = last["http_received"] as Boolean
                    val at = last["checked_at_ms"] as Long
                    require(at in 1..Long.MAX_VALUE)
                    val outcome = last["outcome"] as String
                    require(outcome in setOf("in_progress", "succeeded", "provider_auth", "provider_request", "provider_model", "provider_rate_limited", "provider_unavailable", "provider_timeout", "invalid_response", "cancelled"))
                    appendLine(if (real) "Real HTTPS adapter · " + if (received) "API response received" else "No HTTP response confirmed" else "Test fixture · does not verify a real API")
                    appendLine(when (outcome) {
                        "succeeded" -> "Response parsed successfully. This alone does not verify guidance quality."
                        "in_progress" -> "Still running. Refresh server status; do not start another check yet."
                        "provider_auth" -> "Access rejected. Check key, account permissions and model access."
                        "provider_model" -> "Model endpoint not found. Check the configured model and its availability."
                        "provider_request" -> "Provider rejected the request. Check model support for the requested response format."
                        "provider_rate_limited" -> "Provider limit reached. Check account quota before retrying."
                        "invalid_response" -> "API replied, but its answer did not match the required format."
                        "provider_timeout" -> "Provider timed out. Check connectivity and refresh status."
                        "cancelled" -> "Cancelled. A request already sent may still count toward usage."
                        else -> "Provider unavailable. Check connectivity and the configured model."
                    })
                    appendLine("Input / output / total tokens: ${count(last["input_tokens"]) ?: "unavailable"} / ${count(last["output_tokens"]) ?: "unavailable"} / ${count(last["total_tokens"]) ?: "unavailable"}")
                    appendLine("Request: $id · ${count(last["elapsed_ms"])} ms")
                    appendLine("Checked: ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date(at))}")
                }
            }
            val reason = d["reason"] as String
            appendLine(when (reason) {
                "accepted" -> "\nPaired synthetic guidance passed. No complaint was sent."
                "mock_mode" -> "\nSwitch the server to dual_ai and restart it to check actual APIs."
                "not_requested" -> "\nStatus refresh makes no model call."
                else -> "\nCheck result: " + GatewayRecovery.message(reason, GuidanceLanguage.ENGLISH)
            })
            append("Provider dashboards are separate. Missing usage metadata means unavailable, never zero. Records clear when the server restarts.")
        }
        GatewayResult.Connection(report)
    } catch (_: Exception) { GatewayResult.Rejected("invalid_response") }
}
