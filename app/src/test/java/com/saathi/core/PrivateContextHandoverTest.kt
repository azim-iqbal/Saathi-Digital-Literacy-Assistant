package com.saathi.core
import android.graphics.Rect
import org.junit.Assert.*
import org.junit.Test
class PrivateContextHandoverTest {
    private fun node(label:String)=UiNode(Rect(),label,null,null,null,"TextView",false,true,true)
    @Test fun privateMessagesPauseLocalGuidanceAndListeningWithoutNumericSecrets() {
        for (label in listOf("Inbox", "Message thread", "Document editor", "Personal details", "निजी जानकारी", "niji jaankari", "Incident description", "Complaint description", "Report narrative", "घटना का विवरण", "ghatna ka vivaran")) {
            val nodes=listOf(node(label),node("Help"))
            assertEquals(label,ScreenInterruption.Reason.PRIVATE,ScreenInterruption.reason(nodes))
            val result=com.saathi.orchestrator.LiveGuide.plan("Help",nodes,"en-IN",true)
            assertFalse(result.useCloud); assertNull(result.local.target)
            assertTrue(result.local.speechText.contains("won't send it to AI or save it"))
            assertTrue(result.local.speechText.contains("Turn the microphone back on yourself"))
        }
        assertNull(ScreenInterruption.reason(listOf(node("Public information"),node("Help"))))
    }
}
