package com.saathi.cyber

/** Local assembly, not model-generated testimony. Never add a fact the person did not supply. */
data class ComplaintFacts(
    val account: String = "", val occurred: String = "", val contact: String = "",
    val impact: String = "", val response: String = "", val evidence: String = ""
) {
    fun fields(): List<ComplaintField> {
        val sections = listOf("What happened" to account, "When it happened (as recalled)" to occurred,
            "How contact happened" to contact, "Loss or impact" to impact,
            "Steps already taken" to response, "Evidence available" to evidence)
        val narrative = sections.filter { it.second.isNotBlank() }.joinToString("\n\n") { "${it.first}:\n${it.second.trim()}" }
        return if (account.isBlank()) emptyList() else buildList {
            add(ComplaintField("Incident description", narrative,
                "Use only in the official form's incident-description or narrative box. Check the visible label and character limit first. This is your account, not an AI finding."))
            if (occurred.isNotBlank()) add(ComplaintField("Date and time notes", occurred.trim(),
                "Use as reference for the date/time requested by the form. A calendar or numeric date field may need manual entry in its displayed format; do not paste these notes into an unrelated field."))
            if (impact.isNotBlank()) add(ComplaintField("Loss or impact notes", impact.trim(),
                "Use only in a matching description box. Enter amounts or transaction details separately in the form's required format. Never paste a paragraph into an amount, account or OTP field."))
        }
    }
    fun missing(): List<String> = listOf("When did it happen?" to occurred, "How were you contacted?" to contact,
        "What loss or other impact did you experience?" to impact, "What have you already done?" to response,
        "What evidence have you kept?" to evidence).filter { it.second.isBlank() }.map { it.first }
}
data class ComplaintField(val title: String, val text: String, val instruction: String)
