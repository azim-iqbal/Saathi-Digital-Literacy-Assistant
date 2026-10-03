package com.saathi.cyber

/** Offline reporting aid, not a legal finding or a substitute for the official complaint. */
object CyberReportGuide {
    const val PORTAL = "https://cybercrime.gov.in/"
    const val VERIFIED = "3 October 2026"
    enum class Concern(val title: String) {
        MONEY("Money lost / unauthorised transaction"),
        OTHER("Account misuse / threats / other harm"),
        UNSURE("I'm not sure")
    }
    data class Step(val title: String, val body: String)
    private val financialSignals = Regex("(?i)\\b(money|payment|transaction|bank|upi|investment|refund|loan|debit|credit|rupees)\\b|पैस|बैंक|लेनदेन|भुगतान|निवेश")
    private val accountSignals = Regex("(?i)\\b(hack(?:ed|ing)?|impersonat\\w*|blackmail\\w*|threat\\w*|harass\\w*|phish\\w*|account|password|otp)\\b|धमकी|उत्पीड़न|खाता|ओटीपी")
    fun assessment(description: String, concern: Concern): String {
        if (concern == Concern.MONEY) return "Possible financial cyber fraud based on the category you selected. Call 1930 and notify your bank immediately using its official contact. Do not wait for Saathi's assessment. Recovery is not guaranteed."
        if (concern == Concern.OTHER) return "This may be a cybercrime even if no money was lost. The reporting portal can help route your complaint. Saathi cannot confirm whether a crime occurred."
        if (financialSignals.containsMatchIn(description)) return "Your description mentions a money or banking issue. This alone does not establish fraud. If money was lost to suspected fraud or a transaction was unauthorised, call 1930 and notify your bank immediately. Review the financial-fraud route on the official portal. This is a local keyword check, not an AI or legal finding."
        if (accountSignals.containsMatchIn(description)) return "Your description mentions account access, deception or personal harm. These may need a cybercrime complaint, even without a financial loss. Review the relevant category on the official portal; contact local police if you are unsure. This is a local keyword check and cannot determine whether a crime occurred."
        return if (description.isBlank()) "You can report without describing the incident to Saathi. Choose the closest category on the official portal; if unsure, contact local police."
        else "I cannot reliably determine whether this is fraud from this description. This offline aid does not use a verified AI assessment. If money was lost or a transaction was unauthorised, call 1930 and your bank now. You can still use the reporting steps below."
    }
    val steps = listOf(
        Step("Act promptly", "For suspected financial cyber fraud, call 1930 and contact your bank through its official app, website or card contact. If the line is unavailable, use the portal and contact local police. Do not wait for this checklist to finish."),
        Step("Keep the evidence", "Keep the incident date and time, a brief sequence of events, relevant messages, website addresses, screenshots and transaction references. Keep financial and identity details for the official form only. Do not enter OTPs, passwords, full account numbers or evidence into Saathi."),
        Step("Open the official portal", "Open a browser such as Chrome or Brave. Use Open official portal below, or explicitly copy the link, long-press the browser address bar, paste it and press Go. Check that the address is https://cybercrime.gov.in/ before entering information. Clipboard copying is optional. If the page fails, do not bypass a certificate warning. For suspected financial fraud, call 1930 and your bank without waiting for the site."),
        Step("Choose the complaint route", "On the mobile portal, open the menu and Register a Complaint, then choose the category matching the incident. For financial fraud, choose FINANCIAL FRAUD and its Register a Complaint button. The next public page explains filing a complaint; read it before choosing File a complaint yourself. Labels can change. Reporting a suspect identifier alone is not the same as filing a victim complaint."),
        Step("Register or sign in privately", "Follow the portal's own registration and sign-in instructions. Read its terms yourself. Enter personal information, CAPTCHA and OTP only on the official portal. Saathi must pause marking and listening on private screens; return here for the checklist."),
        Step("Describe what happened", "Follow the official form: select the relevant category and provide the incident details requested, an accurate sequence of events, and suspect or transaction details if known. Use only facts you know. Upload relevant evidence within the portal's displayed limits."),
        Step("Review and submit yourself", "Check every field and attachment. Read any declaration, then submit the complaint yourself. This checklist cannot submit a report or verify that the portal accepted it. If the page fails, check for an acknowledgement before trying again; seek help from local police if needed."),
        Step("Save and follow up", "Save the acknowledgement/reference supplied by the portal or helpline and follow any SMS instructions to complete your report. Use the official portal's complaint-status facility for updates. Finishing this checklist does not mean a report was filed or money recovered.")
    )
}
