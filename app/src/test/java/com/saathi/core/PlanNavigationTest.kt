package com.saathi.core

import android.graphics.Rect
import org.junit.Assert.*
import org.junit.Test

class PlanNavigationTest {
    private fun plan(hint: NavigationHint = NavigationHint("READ_OPTION","Requirements")) = EvidencePlan("Understand requirements",listOf(
        EvidenceStep("s1","Read requirements",emptyList(),"Select “Requirements”.","e1","Source","https://service.example/requirements",hint)),emptyList(),1000,0).also { it.review(1) }
    private fun node(label:String) = UiNode(Rect(),label,null,null,"control","Button",false,true,true)
    @Test fun sourceCurrentStepConsentAndFreshnessAreAllRequired() {
        val p=plan(); val g=ReviewedPlanNavigation(p,"s1",true); val nodes=listOf(node("Requirements"))
        assertEquals(0,g.targetIndex(nodes,"https://service.example/requirements",20,10))
        assertNull(ReviewedPlanNavigation(p,"s1").targetIndex(nodes,"https://service.example/requirements",20,10))
        assertNull(g.targetIndex(nodes,"https://service.example.evil.test/requirements",20,10))
        assertNull(g.targetIndex(nodes,null,20,10))
        assertNull(g.targetIndex(nodes,"https://service.example/requirements",20,21))
        p.invalidateContext(); assertNull(g.targetIndex(nodes,"https://service.example/requirements",20,10))
        p.review(20); assertTrue(p.confirm("s1",20)); assertNull(g.targetIndex(nodes,"https://service.example/requirements",20,10))
    }
    @Test fun movingDuplicateDisabledPrivateAndChallengeControlsNeverReuseATarget() {
        val g=ReviewedPlanNavigation(plan(),"s1",true); val url="https://service.example/requirements"
        assertEquals(0,g.targetIndex(listOf(node("Requirements")),url,20,10))
        for (nodes in listOf(listOf(node("Other")),listOf(node("Requirements"),node("Requirements")),
            listOf(node("Requirements").copy(isEnabled=false)),listOf(node("Requirements").copy(isEditable=true)),
            listOf(node("Requirements"),node("OTP").copy(isSensitive=true)),listOf(node("Requirements"),node("Verify you are human")),
            listOf(node("Requirements"),node("Your connection is not private")),listOf(node("Requirements"),node("Inbox")))) {
            assertNull(g.targetIndex(nodes,url,20,10))
        }
        assertFalse(g.plan.complete(20))
    }
    @Test fun blankPublicFieldUsesHintOnlyAndNeverItsValue() {
        val hint=NavigationHint("FIELD_LABEL","Destination")
        assertTrue(hint.valid("Find field “Destination”."))
        val blank=node("").copy(hint="Destination",isEditable=true,hasValue=false)
        assertEquals(0,hint.targetIndex(listOf(blank)))
        assertNull(hint.targetIndex(listOf(blank.copy(text="Destination",hint=null,hasValue=true))))
        assertNull(hint.targetIndex(listOf(blank.copy(text="private typed value",hasValue=true))))
        assertFalse(NavigationHint("FIELD_LABEL","Email").valid("Find field “Email”."))
        for (label in listOf("Pay","Submit","OTP","Su\u200bbmit","Ｓｕｂｍｉｔ","Bhejein")) assertFalse(NavigationHint("READ_OPTION",label).valid("Select “$label”."))
    }
    @Test fun sourceGrammarWorksInThreeLanguagesWithoutTranslatingLabels() {
        for ((label,quote) in listOf("Requirements" to "Select “Requirements”.","ज़रूरी शर्तें" to "“ज़रूरी शर्तें” चुनें।","Zaroori shartein" to "“Zaroori shartein” chunein.")) {
            assertTrue(NavigationHint("READ_OPTION",label).valid(quote))
        }
        assertFalse(NavigationHint("READ_OPTION","Requirements").valid("Requirements are discussed here."))
    }
}
