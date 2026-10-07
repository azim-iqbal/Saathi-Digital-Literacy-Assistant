package com.saathi.core
import android.graphics.Rect
import org.junit.Assert.*
import org.junit.Test
class ScreenErrorPolicyTest {
    private fun node(label:String)=UiNode(Rect(),label,null,null,null,"TextView",false,true,true)
    @Test fun publicErrorPromptsResearchWithoutSendingTextOrInferringCause() {
        for ((text,locale) in listOf("Service unavailable" to "en-IN","सेवा उपलब्ध नहीं है" to "hi-IN","service uplabdh nahin" to "hinglish")) {
            val nodes=listOf(node(text),node("Help"))
            val plan=com.saathi.orchestrator.LiveGuide.plan("Help",nodes,locale,true)
            assertFalse(plan.useCloud);assertNull(plan.local.target);assertFalse(plan.local.goalComplete)
            assertTrue(ScreenErrorPolicy.isNotice(plan.local.speechText))
            assertNull(LiveAiPolicy.snapshot(ObservationGate.Ticket(1,1,"browser",1),nodes,"Open Help",locale,1,emptyList()))
        }
        assertFalse(ScreenErrorPolicy.present(listOf(node("Error reporting help"))))
        assertFalse(ScreenErrorPolicy.present(listOf(node("Service unavailable").copy(isEditable=true))))
    }
}
