package com.saathi.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saathi.core.*
import com.saathi.gateway.LiveGatewayCodec
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Strict contract coverage using synthetic JSON; no network or credentials. */
@RunWith(AndroidJUnit4::class)
class LiveGatewayCodecTest {
    private fun snapshot() = LiveAiSnapshot("r", 1, 2, System.currentTimeMillis(), "example.browser", 3,
        "en-IN", "Open help", listOf(LiveAiControl("n1", "Help", 0)), emptyList())
    private fun response(s: LiveAiSnapshot, policy: String? = "primary", providers: List<String> = listOf("gemini")) = JSONObject().apply {
        put("status", "accepted"); put("mode", "dual_ai"); put("request_id", s.requestId)
        put("session_id", s.sessionId.toString()); put("screen_revision", s.screenRevision)
        put("package_name", s.packageName); put("window_id", s.windowId)
        put("action", "HIGHLIGHT"); put("target_id", "n1"); put("explanation", "Open Help.")
        put("expected_outcome", "A new page appears."); put("completion_evidence", JSONArray())
        put("provenance", JSONArray(providers.map { JSONObject().put("provider", it).put("model", "fixture") }))
        if (policy != null) put("decision_policy", policy)
    }
    @Test fun primaryFallbackAndLegacyPairedContractsRemainGrounded() {
        val s = snapshot()
        for (reply in listOf(response(s), response(s, "fallback", listOf("groq")), response(s, null, listOf("gemini", "groq")))) {
            assertTrue(LiveGatewayCodec.decode(reply.toString(), s) is GatewayResult.Accepted)
        }
    }
    @Test fun mismatchedPolicyProvenanceAndInventedTargetsAreRejected() {
        val s = snapshot()
        for (reply in listOf(response(s, "unknown"), response(s, "primary", listOf("gemini", "groq")),
            response(s, null), response(s, "fallback", listOf("unknown")), response(s).put("target_id", "n9"),
            response(s).put("screen_revision", 1), response(s).put("action", "COMPLETE"),
            response(s).put("decision_policy", JSONObject.NULL))) {
            assertTrue(LiveGatewayCodec.decode(reply.toString(), s) is GatewayResult.Rejected)
        }
    }
}
