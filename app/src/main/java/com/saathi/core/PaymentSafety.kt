package com.saathi.core

/** Observed labels are clues, never bank confirmation. No transition authorizes a retry. */
object PaymentSafety {
    enum class State { NOT_STARTED, AWAITING_USER_PAYMENT, PROCESSING, SUCCESS, FAILED, PENDING, UNKNOWN }
    private val context = Regex("(?i)\\b(payment|transaction)\\b|भुगतान|लेनदेन|bhugtan|len.?den")
    private val pending = Regex("(?i)\\bpending\\b|लंबित|प्रतीक्षा|lambit|intezaar")
    private val processing = Regex("(?i)processing|in progress|प्रक्रिया जारी|process ho")
    private val failure = Regex("(?i)failed|declined|unsuccessful|विफल|असफल|asafal|fail ho")
    private val success = Regex("(?i)successful|completed|सफल|poora hua")
    fun state(nodes: List<UiNode>): State? {
        val text = nodes.mapNotNull { it.text ?: it.description }.joinToString(" ").take(12000)
        if (!context.containsMatchIn(text)) return null
        return when {
            pending.containsMatchIn(text) -> State.PENDING
            processing.containsMatchIn(text) -> State.PROCESSING
            failure.containsMatchIn(text) -> State.FAILED
            success.containsMatchIn(text) -> State.SUCCESS
            Regex("(?i)status|unknown|uncertain|स्थिति|sthiti").containsMatchIn(text) -> State.UNKNOWN
            else -> null
        }
    }
    fun explanation(state: State, locale: String): String = when(locale) {
        "hi-IN" -> "भुगतान की स्थिति स्वतंत्र रूप से सत्यापित नहीं है। दोबारा भुगतान न करें—दोहरा शुल्क लग सकता है। आधिकारिक लेनदेन इतिहास या सहायता से जाँचें।"
        "hinglish" -> "Payment ki sthiti independently verify nahin hui. Dobara payment na karein—double charge ho sakta hai. Official transaction history ya support se jaanchein."
        else -> "The observed payment status (${state.name.lowercase()}) is not independently verified. Do not pay again: duplicate charges are possible. Check the official transaction history or support."
    }
}
