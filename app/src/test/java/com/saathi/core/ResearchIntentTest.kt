package com.saathi.core

import org.junit.Assert.*
import org.junit.Test

class ResearchIntentTest {
    @Test fun changingClaimsRouteEquivalentlyWithoutServiceRules() {
        val cases = mapOf(
            "requirements" to listOf("Which documents are required to apply for a service?", "आवेदन के लिए जरूरी दस्तावेज", "apply karne ke liye zaroori dastavez"),
            "eligibility" to listOf("Am I eligible?", "मेरी पात्रता क्या है?", "meri patrata kya hai"),
            "outage" to listOf("Service down error", "सेवा बंद है", "service kaam nahin kar rahi"),
            "pricing" to listOf("Current fees", "आज का शुल्क", "aaj ka shulk"),
            "deadline" to listOf("Last date", "अंतिम तारीख", "aakhri tareekh"))
        cases.forEach { (type, goals) -> goals.forEach { assertEquals(type, ResearchIntent.claimType(it)) } }
        for (goal in listOf("Help", "Find To", "01/10/2026", "Settings", "Support")) assertNull(ResearchIntent.claimType(goal))
    }
}
