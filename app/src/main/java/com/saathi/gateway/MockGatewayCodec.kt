package com.saathi.gateway

import android.util.JsonReader
import android.util.JsonToken
import com.saathi.core.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.StringReader

/** Exact local mock wire contract. Reject duplicates, coercions, unknown fields and oversized data. */
internal object MockGatewayCodec {
    fun encode(s: SanitizedScreenSnapshot): String = JSONObject().apply {
        put("request_id", s.requestId); put("session_id", s.sessionId.toString())
        put("screen_revision", s.screenRevision); put("observed_at_ms", s.observedAtMs)
        put("package_name", s.packageName); put("window_id", s.windowId); put("locale", s.locale)
        put("task", s.task.name); put("eligible_node_ids", JSONArray(s.eligibleNodeIds.sorted()))
    }.toString()

    fun decode(body: String, snapshot: SanitizedScreenSnapshot): GatewayResult = try {
        val data = parse(body)
        require(data["mode"] == "mock")
        if (data["status"] == "rejected") {
            require(data.keys == setOf("status", "reason", "mode"))
            require(data["reason"] is String && (data["reason"] as String).length in 1..80)
            GatewayResult.Rejected(GatewayRecovery.reason(data["reason"]))
        } else {
            require(data["status"] == "accepted" && data.keys == setOf("status", "mode", "request_id", "session_id",
                "screen_revision", "package_name", "window_id", "action", "target_id", "explanation",
                "expected_outcome", "completion_evidence", "provenance"))
            require(data["request_id"] == snapshot.requestId && data["session_id"] == snapshot.sessionId.toString())
            val provenance = data["provenance"] as? List<*> ?: error("provenance")
            require(provenance.size == 2 && provenance.map { entry ->
                val p = entry as? Map<*, *> ?: error("identity")
                require(p.keys == setOf("provider", "model") && p["model"] == "deterministic-fixture-v1")
                p["provider"]
            }.toSet() == setOf("mock-a", "mock-b"))
            val evidence = data["completion_evidence"] as? List<*> ?: error("evidence")
            require(evidence.all { it is String } && evidence.size == evidence.toSet().size)
            require(data["target_id"] == null || data["target_id"] is String)
            val window = data["window_id"] as? Long ?: error("window")
            require(window in 0..Int.MAX_VALUE)
            val proposal = GuidanceProposal(snapshot.sessionId, data["screen_revision"] as Long,
                data["package_name"] as String, window.toInt(), ProposedAction.valueOf(data["action"] as String),
                data["target_id"] as String?, data["explanation"] as String, data["expected_outcome"] as String,
                evidence.map { it as String }.toSet())
            if (DualProposalValidator.validate(snapshot, proposal) == null) GatewayResult.Accepted(proposal)
            else GatewayResult.Rejected("invalid_decision")
        }
    } catch (_: Exception) { GatewayResult.Rejected("malformed") }

    internal fun parse(body: String, research: Boolean = false): Map<*, *> {
        require(body.toByteArray(Charsets.UTF_8).size <= if (research) 65536 else 8192)
        return JsonReader(StringReader(body)).use { reader ->
            reader.isLenient = false
            val value = read(reader, 0, research)
            require(reader.peek() == JsonToken.END_DOCUMENT)
            value as? Map<*, *> ?: error("object")
        }
    }

    private fun read(r: JsonReader, depth: Int, research: Boolean): Any? {
        require(depth <= 6)
        return when (r.peek()) {
            JsonToken.BEGIN_OBJECT -> linkedMapOf<String, Any?>().apply {
                r.beginObject()
                while (r.hasNext()) {
                    require(size < 20)
                    val key = r.nextName(); require(key.length <= 80 && !containsKey(key))
                    put(key, read(r, depth + 1, research))
                }
                r.endObject()
            }
            JsonToken.BEGIN_ARRAY -> mutableListOf<Any?>().apply {
                r.beginArray(); while (r.hasNext()) { require(size < 20); add(read(r, depth + 1, research)) }; r.endArray()
            }
            JsonToken.STRING -> r.nextString().also { require(it.length <= if (research) 4000 else 1024) }
            JsonToken.NUMBER -> r.nextString().let { require(it.matches(Regex("0|[1-9][0-9]*"))); it.toLong() }
            JsonToken.BOOLEAN -> r.nextBoolean()
            JsonToken.NULL -> { r.nextNull(); null }
            else -> error("Unexpected JSON type")
        }
    }
}
