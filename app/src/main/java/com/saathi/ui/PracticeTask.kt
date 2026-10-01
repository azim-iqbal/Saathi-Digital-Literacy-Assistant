package com.saathi.ui

/** Only routes to synthetic fixtures; it does not turn a real task into claimed live support. */
object PracticeTask {
    fun parse(text: String): String? {
        val words = text.lowercase()
        return when {
            listOf("water", "पानी", "paani").any(words::contains) -> "Pay my water bill"
            listOf("dth", "tv", "recharge", "टीवी", "रिचार्ज").any(words::contains) -> "Recharge my DTH"
            listOf("electricity", "electric", "बिजली", "bijli").any(words::contains) -> "Pay my electricity bill"
            else -> null
        }
    }
}
