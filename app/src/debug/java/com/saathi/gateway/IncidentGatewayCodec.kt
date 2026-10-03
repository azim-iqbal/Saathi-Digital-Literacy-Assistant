package com.saathi.gateway

import com.saathi.core.*
import org.json.JSONObject

internal object IncidentGatewayCodec {
    fun encode(s: IncidentAssessmentRequest) = JSONObject().apply {
        put("request_id", s.requestId); put("session_id", s.sessionId); put("screen_revision", s.revision)
        put("observed_at_ms", s.observedAtMs); put("locale", s.locale); put("summary", s.summary)
        put("concern", s.concern); put("consent", true)
    }.toString()
    fun decode(body: String, s: IncidentAssessmentRequest): GatewayResult = try {
        val d = MockGatewayCodec.parse(body)
        require(d["mode"] == "dual_ai")
        if (d["status"] == "rejected") {
            require(d.keys == setOf("status", "mode", "reason"))
            GatewayResult.Rejected(GatewayRecovery.reason(d["reason"]))
        } else {
            require(d["status"] == "accepted" && d.keys == setOf("status", "mode", "request_id", "session_id",
                "screen_revision", "category", "signals", "provenance"))
            require(d["request_id"] == s.requestId && d["session_id"] == s.sessionId && d["screen_revision"] == s.revision)
            require(System.currentTimeMillis() - s.observedAtMs in 0..15_000)
            val identities = d["provenance"] as List<*>
            require(identities.size == 2 && identities.map {
                val identity = it as Map<*, *>
                require(identity.keys == setOf("provider", "model") && (identity["model"] as String).matches(Regex("[A-Za-z0-9._/-]{1,100}")))
                identity["provider"]
            }.toSet() == setOf("gemini", "groq"))
            val category = d["category"] as String
            val signals = (d["signals"] as List<*>).map { it as String }
            require(category in IncidentAssessmentPolicy.categories && signals.size in 1..5 && signals.toSet().size == signals.size &&
                signals.all { it in IncidentAssessmentPolicy.signals })
            require((category == "UNCLEAR") == (signals == listOf("INSUFFICIENT_CONTEXT")))
            require(s.concern != "MONEY" || category == "POSSIBLE_FINANCIAL")
            GatewayResult.Assessed(IncidentAssessment(category, signals.toSet()))
        }
    } catch (_: Exception) { GatewayResult.Rejected("invalid_response") }
}
