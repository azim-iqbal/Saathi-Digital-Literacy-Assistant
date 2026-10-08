package com.saathi.core
import org.junit.Assert.*
import org.junit.Test

class EvidencePlanTest {
    private fun plan() = EvidencePlan("Original task", listOf(
        EvidenceStep("s1", "Apply", listOf("s2"), "Source quote", "e1"),
        EvidenceStep("s2", "Register", listOf("s3"), "Source quote", "e1"),
        EvidenceStep("s3", "Verify", emptyList(), "Source quote", "e1")),
        listOf(EvidenceCriterion("c1", "Condition quote", "e1")), 1000)
    @Test fun originalGoalReturnsAfterTwoPrerequisitesWithExplicitConfirmations() {
        val p = plan(); assertNull(p.next(10)); assertTrue(p.review(10))
        assertFalse(p.confirm("s1",10))
        for (id in listOf("s3","s2","s1")) { assertEquals(id,p.next(10)?.id); assertTrue(p.confirm(id,10)) }
        assertTrue(p.complete(10)); assertEquals("Original task",p.originalGoal)
    }
    @Test fun leavingContextAndExpiredSourcesBlockProgress() {
        val p=plan(); p.review(10); p.invalidateContext(); assertNull(p.next(10))
        assertFalse(p.confirm("s3",10)); assertFalse(p.review(1000)); assertNull(p.next(1000))
    }
    @Test fun booleanFactsNeverEstablishDefiniteEligibility() {
        val p=plan(); assertEquals("NOT_EVALUATED",p.eligibility(10)); p.review(10)
        assertEquals("INSUFFICIENT_INFORMATION",p.eligibility(10))
        p.setFact("c1",true); assertEquals("POSSIBLY_ELIGIBLE",p.eligibility(10))
        p.setFact("c1",false); assertEquals("CONDITION_NOT_MET",p.eligibility(10))
        p.setFact("c1",null); assertEquals("INSUFFICIENT_INFORMATION",p.eligibility(10))
        assertThrows(IllegalArgumentException::class.java) { p.setFact("password",true) }
    }
    @Test fun clockRollbackBeforeRetrievedEvidenceInvalidatesReviewAndProgress() {
        val p=EvidencePlan("Task",listOf(EvidenceStep("s1","Read",emptyList(),"Quote","e1")),emptyList(),1000, retrievedAtMs=500)
        assertTrue(p.review(600)); assertNotNull(p.next(600))
        assertNull(p.next(499)); assertFalse(p.confirm("s1",499)); assertFalse(p.review(499))
        assertEquals("NOT_EVALUATED",p.eligibility(499)); assertFalse(p.complete(499))
        assertTrue(p.review(600)); assertTrue(p.confirm("s1",600)); assertTrue(p.complete(600))
        assertFalse(p.complete(1000))
    }
    @Test fun cyclesAndMissingDependenciesAreRejected() {
        for (dependency in listOf("s1", "missing")) assertThrows(IllegalArgumentException::class.java) {
            EvidencePlan("Task",listOf(EvidenceStep("s1","Step",listOf(dependency),"Quote","e1")),emptyList(),1000)
        }
    }
}
