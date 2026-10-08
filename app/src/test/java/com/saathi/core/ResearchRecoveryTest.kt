package com.saathi.core
import org.junit.Assert.*
import org.junit.Test
class ResearchRecoveryTest {
    @Test fun onlyExplicitTransientPlanFailuresCanOfferAnotherRequest() {
        for (reason in listOf("busy","timeout","connection_failed","provider_unavailable")) assertTrue(ResearchRecovery.canRetryPlan(reason))
        for (reason in listOf("research_expired","cancelled","quota_exhausted","secure_connection_failed","disagreement","provider_auth","unknown")) assertFalse(ResearchRecovery.canRetryPlan(reason))
    }
    @Test fun fixedCopyNeverReflectsServerTextAndAllLocalesHaveRecovery() {
        for (locale in listOf("en-IN","hi-IN","hinglish")) {
            val unknown=ResearchRecovery.message("private arbitrary server exception",locale)
            assertFalse(unknown.contains("private arbitrary"))
            for (reason in listOf("research_expired","research_cancelled","research_not_configured","unauthorized","secure_connection_failed","quota_exhausted","timeout","disagreement"))
                assertNotEquals(unknown,ResearchRecovery.message(reason,locale))
        }
    }
}
