package com.saathi.guardrails

enum class GuardrailDecision { ALLOW, REFUSE }
data class GuardrailResult(val decision: GuardrailDecision, val category: String?, val reason: String)

/** Local-only scope check. It never sends the goal or its audit records off device. */
object GuardrailEngine {
    private val allowed = mapOf(
        "bill_payment" to listOf("bill", "electricity", "recharge", "water bill", "pay bill", "बिल", "बिजली", "रिचार्ज", "bill bhar", "bijli"),
        "payment_navigation" to listOf("upi", "payment", "pay ", "phonepe", "gpay", "google pay", "send money", "digital payment", "भुगतान", "पेमेंट", "पैसा भेज", "paise bhej"),
        "form_filling" to listOf("form", "application", "apply", "registration", "fill", "फॉर्म", "आवेदन", "भरना", "form fill", "apply kar"),
        "ticket_booking" to listOf("ticket", "train", "bus", "flight", "booking", "बुकिंग", "टिकट", "train ticket", "bus ticket"),
        "public_scheme" to listOf("government", "scheme", "portal", "aadhaar", "ration", "pension", "सरकार", "योजना", "सरकारी", "sarkari", "yojana"),
        "essential_settings" to listOf("accessibility", "permission", "settings", "phone setting", "सेटिंग", "अनुमति", "setting kholo")
    )
    private val denied = listOf(
        "weather", "capital", "who is", "what is", "tell me about", "joke", "song", "movie", "game", "story", "poem", "write", "essay", "recipe", "relationship", "medical advice", "stock price", "crypto", "gossip",
        "मौसम", "राजधानी", "चुटकुला", "गाना", "फिल्म", "कहानी", "कविता", "लिखो", "सलाह", "खेल", "share bazaar", "gaana", "joke sunao", "kahani", "likh do"
    )
    private val unsafeScreenTerms = listOf("casino", "bet", "betting", "rummy", "teen patti", "porn", "adult", "loan instantly", "instant loan", "unlicensed lending")

    fun classify(goal: String): GuardrailResult {
        val normal = goal.trim().lowercase()
        if (normal.isBlank()) return refuse("empty_goal")
        if (denied.any(normal::contains)) return refuse("off_topic")
        val match = allowed.entries.firstOrNull { (_, phrases) -> phrases.any(normal::contains) }
        return if (match != null) GuardrailResult(GuardrailDecision.ALLOW, match.key, "matched_allow_intent") else refuse("not_a_concrete_screen_task")
    }

    fun screenIsUnsafe(visibleText: Sequence<String?>): Boolean = visibleText.filterNotNull().any { value ->
        val normal = value.lowercase(); unsafeScreenTerms.any(normal::contains)
    }
    private fun refuse(reason: String) = GuardrailResult(GuardrailDecision.REFUSE, null, reason)
}

/** Memory-only audit trail: labels only, never user goals, screen text, recordings, or screenshots. */
object GuardrailAuditLog {
    data class Entry(val timestampMs: Long, val decision: GuardrailDecision, val reason: String)
    private val entries = ArrayDeque<Entry>()
    fun record(result: GuardrailResult) { synchronized(entries) { entries.addLast(Entry(System.currentTimeMillis(), result.decision, result.reason)); while (entries.size > 50) entries.removeFirst() } }
    fun snapshot(): List<Entry> = synchronized(entries) { entries.toList() }
}
