package com.saathi.cyber

import org.junit.Assert.*
import org.junit.Test

class ComplaintDraftTest {
    @Test fun unknownFactsAreOmittedRatherThanInvented() {
        val facts = ComplaintFacts(account = "A caller asked me to send money. I did not send any.")
        val fields = facts.fields()
        assertEquals(1, fields.size)
        assertEquals("What happened:\nA caller asked me to send money. I did not send any.", fields.single().text)
        assertEquals(5, facts.missing().size)
        assertFalse(fields.single().text.contains("fraud"))
    }
    @Test fun preservesContradictionsAndExactSuppliedFiguresForReview() {
        val facts = ComplaintFacts(account = "I first thought ₹500; my record says ₹50.", occurred = "Maybe Tuesday; time unknown", impact = "₹50, not yet verified")
        val fields = facts.fields()
        assertEquals(3, fields.size)
        assertTrue(fields.first().text.contains(facts.account))
        assertEquals(facts.occurred, fields[1].text)
        assertEquals(facts.impact, fields[2].text)
    }
    @Test fun blankAccountCannotCreateAComplaintAndClearingRemovesAllFields() {
        assertTrue(ComplaintFacts(occurred = "Yesterday").fields().isEmpty())
        assertTrue(ComplaintFacts().fields().isEmpty())
    }
    @Test fun filledFollowupsProduceOnlyKnownSectionsInOrder() {
        val facts = ComplaintFacts("My account", "Morning", "Phone call", "No loss known", "Called bank", "Kept messages")
        assertTrue(facts.missing().isEmpty())
        val text = facts.fields().first().text
        assertTrue(text.indexOf("My account") < text.indexOf("Phone call"))
        assertTrue(text.endsWith("Kept messages"))
    }
}
