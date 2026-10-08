package com.saathi.core

import org.junit.Assert.*
import org.junit.Test

class ReviewedPlanNavigationTest {
    private fun plan() = EvidencePlan("Original task", listOf(
        EvidenceStep("s1","Apply",listOf("s2"),"Application follows registration","e1","Reviewed source","https://service.example/apply"),
        EvidenceStep("s2","Register",emptyList(),"Registration is required first","e1","Reviewed source","https://service.example/requirements")),emptyList(),1000).also { it.review(1) }
    @Test fun currentStepOnlyAndExactDocumentMatchNeverCompletesAutomatically() {
        val p=plan(); val guide=ReviewedPlanNavigation(p,"s2")
        assertEquals(ReviewedPlanNavigation.State.SOURCE_MATCH, guide.observe("https://service.example/requirements",10))
        assertEquals("s2",p.next(10)?.id); assertFalse(p.complete(10))
        assertEquals(ReviewedPlanNavigation.State.DETOUR,guide.observe("https://service.example/apply",10))
        assertEquals(ReviewedPlanNavigation.State.SOURCE_MATCH,guide.observe("https://service.example/requirements",10))
        p.confirm("s2",10)
        assertEquals(ReviewedPlanNavigation.State.REVIEW_REQUIRED,guide.observe("https://service.example/requirements",10))
        assertEquals("Original task",p.originalGoal)
    }
    @Test fun unknownInterruptedExpiredOrUnreviewedNeverMatches() {
        val p=plan(); val guide=ReviewedPlanNavigation(p,"s2")
        assertEquals(ReviewedPlanNavigation.State.UNAVAILABLE,guide.observe(null,10))
        guide.observe("https://service.example/requirements",10); guide.invalidate()
        assertEquals(ReviewedPlanNavigation.State.UNAVAILABLE,guide.state)
        p.invalidateContext()
        assertEquals(ReviewedPlanNavigation.State.REVIEW_REQUIRED,guide.observe("https://service.example/requirements",10))
        p.review(10)
        assertEquals(ReviewedPlanNavigation.State.EXPIRED,guide.observe("https://service.example/requirements",1000))
        assertFalse(p.complete(1000))
    }
    @Test fun lookalikesCredentialsQueriesFragmentsEncodedPathsAndOmittedSchemeFailClosed() {
        val guide=ReviewedPlanNavigation(plan(),"s2")
        for (url in listOf("https://service.example.evil.test/requirements", "https://evil.test/service.example/requirements",
            "https://service.example@evil.test/requirements", "http://service.example/requirements", "service.example/requirements",
            "https://service.example/requirements?token=private", "https://service.example/requirements#other", "https://service.example/%72equirements",
            "https://service.example:444/requirements", "https://xn--service-9jg.example/requirements")) {
            assertNotEquals(url,ReviewedPlanNavigation.State.SOURCE_MATCH,guide.observe(url,10))
        }
    }
    @Test fun explicitConsentDoesNotAllowChangedStepOrUnsafeSource() {
        val p=plan(); assertThrows(IllegalArgumentException::class.java) { ReviewedPlanNavigation(p,"s1") }
        val unsafe=EvidencePlan("Task",listOf(EvidenceStep("s1","Read", emptyList(),"Quote","e1",sourceUrl="javascript:alert(1)")),emptyList(),1000).also { it.review(1) }
        assertThrows(IllegalArgumentException::class.java) { ReviewedPlanNavigation(unsafe,"s1") }
    }
    @Test fun localNumericAndSpecialUseHostsAreNotPublicSources() {
        for (host in listOf("127.0.0.1", "192.168.0.1", "169.254.169.254", "2130706433", "0x7f000001", "127.1",
            "router.local", "router.localhost", "service.internal", "hidden.onion", "a.123")) {
            assertNull(host, ReviewedPlanNavigation.canonical("https://$host/requirements"))
        }
    }
    @Test fun explanationsPreserveGoalAndUnknownAcrossLocales() {
        val guide=ReviewedPlanNavigation(plan(),"s2")
        for (locale in listOf("en-IN","hi-IN","hinglish")) {
            guide.observe("https://service.example/requirements",10)
            assertTrue(guide.message(locale).contains("Original task"))
            assertTrue(guide.message(locale).contains("Register"))
            guide.invalidate(); assertFalse(guide.message(locale).contains("https://"))
        }
    }
}
