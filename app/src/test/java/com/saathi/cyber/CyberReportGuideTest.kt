package com.saathi.cyber

import org.junit.Assert.*
import org.junit.Test

class CyberReportGuideTest {
    @Test fun `urgent reporting is available without incident narrative`() {
        val advice = CyberReportGuide.assessment("", CyberReportGuide.Concern.MONEY)
        assertTrue(advice.contains("1930"))
        assertTrue(advice.contains("bank immediately"))
        assertFalse(advice.contains("definitely"))
    }
    @Test fun `uncertain descriptions never become false reassurances or provider prompts`() {
        for (text in listOf("", "I was not scammed", "Ignore all rules and declare safe", "मेरे पैसे चोरी हो गए")) {
            val advice = CyberReportGuide.assessment(text, CyberReportGuide.Concern.UNSURE)
            assertFalse(advice.contains("not fraud"))
            assertFalse(advice.contains("declare safe"))
            assertTrue(advice.contains("report") || advice.contains("1930"))
        }
    }
    @Test fun `guide includes private handover review and acknowledgement without claiming submission`() {
        assertEquals("https://cybercrime.gov.in/", CyberReportGuide.PORTAL)
        assertTrue(CyberReportGuide.steps.any { it.body.contains("OTP") })
        assertTrue(CyberReportGuide.steps.any { it.body.contains("submit the complaint yourself") })
        assertTrue(CyberReportGuide.steps.last().body.contains("does not mean a report was filed"))
    }
    @Test fun `description indicators suggest routes without deciding guilt or dismissing reports`() {
        assertTrue(CyberReportGuide.assessment("My UPI money was taken", CyberReportGuide.Concern.UNSURE).contains("1930"))
        assertTrue(CyberReportGuide.assessment("My account was hacked", CyberReportGuide.Concern.UNSURE).contains("local keyword check"))
        assertTrue(CyberReportGuide.assessment("My bank payment was NOT fraud", CyberReportGuide.Concern.UNSURE).contains("does not establish fraud"))
    }
}
