package com.saathi.core
import android.graphics.Rect
import org.junit.Assert.*
import org.junit.Test
class BrowserSafetyPolicyTest {
    private fun node(label:String)=UiNode(Rect(),label,null,null,null,"TextView",false,true,true)
    @Test fun certificateAndMalwareWarningsNeverGuideThroughContinueOrUseCloud() {
        for ((text,locale) in listOf("Your connection is not private" to "en-IN", "NET::ERR_CERT_DATE_INVALID" to "en-IN",
            "आपका कनेक्शन निजी नहीं है" to "hi-IN", "Connection surakshit nahin hai" to "hinglish", "Deceptive site ahead" to "en-IN")) {
            val nodes=listOf(node(text),node("Continue"))
            val plan=com.saathi.orchestrator.LiveGuide.plan("Continue",nodes,locale,true)
            assertFalse(plan.useCloud); assertNull(plan.local.target); assertFalse(plan.local.goalComplete)
            assertTrue(plan.local.speechText.contains("सुरक्षा") || plan.local.speechText.contains("security") || plan.local.speechText.contains("suraksha"))
            assertNull(LiveAiPolicy.snapshot(ObservationGate.Ticket(1,1,"com.android.chrome",1),nodes,"Continue",locale,1,emptyList()))
        }
    }
}
