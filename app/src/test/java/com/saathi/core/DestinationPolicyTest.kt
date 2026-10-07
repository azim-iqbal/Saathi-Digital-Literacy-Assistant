package com.saathi.core
import android.graphics.Rect
import org.junit.Assert.*
import org.junit.Test
class DestinationPolicyTest {
    @Test fun governmentLookalikeCannotGainAuthorityFromBrandingOrVisibleApplyButton() {
        val nodes=listOf(UiNode(Rect(),"Government portal",null,null,null,"TextView",false,true,false),
            UiNode(Rect(),"Help",null,null,null,"Button",false,true,true))
        val plan=com.saathi.orchestrator.LiveGuide.plan("Help",nodes,"en-IN",true)
        assertNull(plan.local.target); assertFalse(plan.useCloud)
    }
    @Test fun unrelatedHighRiskGoalsFailClosedAcrossLanguagesWithoutTaskSpecificDependencies() {
        for (goal in listOf("Government services","Bank account help","Credit application","सरकारी सेवा","sarkari seva"))
            assertTrue(DestinationPolicy.requiresProvenance(goal))
        assertFalse(DestinationPolicy.requiresProvenance("Help with a public travel choice"))
    }
}
