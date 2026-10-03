package com.saathi.core

import org.junit.Assert.*
import org.junit.Test

class IncidentAssessmentPolicyTest {
    @Test fun summariesRejectCommonIdentifiersWithoutBlockingDiscussionOfScams() {
        assertTrue(IncidentAssessmentPolicy.allowed("I shared an OTP and somebody accessed my account."))
        listOf("My number is ९८७६५४३२१०", "Write to private@example.com", "my password is secretword",
            "Please check https://example.com", " ".repeat(1300), "Tiny").forEach {
            assertFalse(it, IncidentAssessmentPolicy.allowed(it))
        }
    }
    @Test fun unclearDoesNotDeclareUserSafe() {
        val text = IncidentAssessmentPolicy.description(IncidentAssessment("UNCLEAR", setOf("INSUFFICIENT_CONTEXT")))
        assertTrue(text.contains("does not mean")); assertTrue(text.contains("1930"))
    }
}
