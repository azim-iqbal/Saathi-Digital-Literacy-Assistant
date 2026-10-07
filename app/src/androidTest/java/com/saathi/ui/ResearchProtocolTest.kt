package com.saathi.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saathi.core.GatewayResult
import com.saathi.gateway.ConnectionCodec
import com.saathi.gateway.ResearchCodec
import org.json.JSONObject
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ResearchProtocolTest {
    @Test fun safeValidationMetadataIsCompatibleAndUnknownReasonsAreRejected() {
        val providers=JSONArray()
        for (provider in listOf("gemini","groq")) providers.put(JSONObject()
            .put("provider",provider).put("model","fixture-model").put("attempts",1).put("configured",true)
            .put("last",JSONObject().put("request_id","r").put("session_id","s").put("outcome","succeeded")
                .put("real_api",false).put("http_received",true).put("checked_at_ms",System.currentTimeMillis())
                .put("elapsed_ms",12).put("input_tokens",1).put("output_tokens",2).put("total_tokens",3)
                .put("http_status",200).put("validation_reason","TARGET_NOT_FOUND").put("response_category","HIGHLIGHT")))
        val data=JSONObject().put("status","connection").put("mode","dual_ai").put("request_id","r")
            .put("reason","invalid_target").put("providers",providers)
        assertTrue(ConnectionCodec.decode(data.toString(),"r") is GatewayResult.Connection)
        providers.getJSONObject(0).getJSONObject("last").put("http_status",999)
        assertTrue(ConnectionCodec.decode(data.toString(),"r") is GatewayResult.Rejected)
        providers.getJSONObject(0).getJSONObject("last").put("http_status",200)
        providers.getJSONObject(0).getJSONObject("last").put("validation_reason","arbitrary private response")
        assertTrue(ConnectionCodec.decode(data.toString(),"r") is GatewayResult.Rejected)
    }
    @Test fun researchIdentityDuplicatesAndUnexpectedCommandsFailClosed() {
        val base="""{"status":"researched","request_id":"r","evidence":[],"limitations":[]}"""
        assertTrue(ResearchCodec.decode(base,"r",false) is GatewayResult.Research)
        val hindi=ResearchCodec.decode(base,"r",false,"hi-IN") as GatewayResult.Research
        val hinglish=ResearchCodec.decode(base,"r",false,"hinglish") as GatewayResult.Research
        assertTrue(hindi.report.contains("खोज सीमित थी"))
        assertTrue(hinglish.report.contains("Khoj seemit thi"))
        assertTrue(ResearchCodec.decode(base,"other",false) is GatewayResult.Rejected)
        assertTrue(ResearchCodec.decode(base.replace("\"status\":", "\"status\":\"researched\",\"status\":"),"r",false) is GatewayResult.Rejected)
        assertTrue(ResearchCodec.decode(base.dropLast(1)+",\"tool\":\"paste\"}","r",false) is GatewayResult.Rejected)
        assertTrue(ResearchCodec.decode(" ".repeat(65537),"r",false) is GatewayResult.Rejected)
    }
}
