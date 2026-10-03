package com.saathi.gateway

import com.saathi.core.*
import org.json.JSONArray
import org.json.JSONObject

internal object LiveGatewayCodec {
    fun encode(s: LiveAiSnapshot) = JSONObject().apply {
        put("request_id", s.requestId); put("session_id", s.sessionId.toString()); put("screen_revision", s.screenRevision)
        put("observed_at_ms", s.observedAtMs); put("package_name", s.packageName); put("window_id", s.windowId)
        put("locale", s.locale); put("goal", s.goal); put("previous_steps", JSONArray(s.previousSteps))
        put("controls", JSONArray(s.controls.map { JSONObject().put("id", it.id).put("label", it.label) }))
    }.toString()
    fun decode(body: String, s: LiveAiSnapshot): GatewayResult = try {
        val d = MockGatewayCodec.parse(body)
        require(d["mode"] == "dual_ai")
        if (d["status"] == "rejected") {
            require(d.keys == setOf("status", "mode", "reason")); GatewayResult.Rejected(GatewayRecovery.reason(d["reason"]))
        } else {
            require(d["status"] == "accepted" && d.keys == setOf("status", "mode", "request_id", "session_id", "screen_revision",
                "package_name", "window_id", "action", "target_id", "explanation", "expected_outcome", "completion_evidence", "provenance"))
            require(d["request_id"] == s.requestId && d["session_id"] == s.sessionId.toString())
            val identities = d["provenance"] as List<*>
            require(identities.size == 2 && identities.map {
                val identity = it as Map<*, *>
                require(identity.keys == setOf("provider", "model") && (identity["model"] as String).matches(Regex("[A-Za-z0-9._/-]{1,100}")))
                identity["provider"]
            }.toSet() == setOf("gemini", "groq"))
            require((d["completion_evidence"] as List<*>).isEmpty())
            val window = d["window_id"] as Long; require(window in 0..Int.MAX_VALUE)
            val p = GuidanceProposal(s.sessionId, d["screen_revision"] as Long, d["package_name"] as String, window.toInt(),
                ProposedAction.valueOf(d["action"] as String), d["target_id"] as String?, d["explanation"] as String, d["expected_outcome"] as String)
            if (s.valid(p)) GatewayResult.Accepted(p) else GatewayResult.Rejected("invalid_decision")
        }
    } catch (_: Exception) { GatewayResult.Rejected("malformed") }
}
