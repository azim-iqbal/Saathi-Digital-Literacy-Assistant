package com.saathi.core
import android.graphics.Rect
import org.junit.Assert.*
import org.junit.Test
class BrowserConsentPolicyTest {
    @Test fun consentCannotBeChosenEvenWhenRequestedOptionExistsBehindIt() {
        fun node(text:String)=UiNode(Rect(),text,null,null,null,"Button",false,true,true)
        for ((text,locale) in listOf("Cookie preferences" to "en-IN","कुकी प्राथमिकताएं" to "hi-IN","cookie pasand" to "hinglish")) {
            val nodes=listOf(node(text),node("Help"))
            val plan=com.saathi.orchestrator.LiveGuide.plan("Help",nodes,locale,true)
            assertFalse(plan.useCloud); assertNull(plan.local.target)
            assertNull(LiveAiPolicy.snapshot(ObservationGate.Ticket(1,1,"browser.test",1),nodes,"Help",locale,1,emptyList()))
        }
    }
}
