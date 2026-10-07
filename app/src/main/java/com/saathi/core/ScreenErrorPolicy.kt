package com.saathi.core

/** Recognizes public error states locally; it does not infer their cause or transmit the text. */
object ScreenErrorPolicy {
    private val status = Regex("(?i)^(service unavailable|temporarily unavailable|something went wrong|try again later|network error|सेवा उपलब्ध नहीं है|कुछ गलत हो गया|बाद में कोशिश करें|service uplabdh nahin|baad mein koshish karein)[.!। ]*$")
    fun present(nodes: List<UiNode>) = nodes.any { node -> !node.isEditable && !node.isSensitive &&
        listOfNotNull(node.text,node.description).any { status.matches(it.trim()) } }
    fun message(locale: String) = when(locale) {
        "hi-IN" -> "स्क्रीन पर समस्या दिख रही है, पर कारण तय नहीं है। साथी खोलकर सेवा की समस्या खोज सकते हैं। आपकी सहमति के बिना स्क्रीन की जानकारी नहीं भेजी जाएगी; भुगतान दोबारा न करें।"
        "hinglish" -> "Screen par samasya dikh rahi hai, par wajah tay nahin hai. Saathi kholkar service ki samasya khoj sakte hain. Aapki sahmati ke bina screen ki jaankari nahin bheji jayegi; payment dobara na karein."
        else -> "An error is visible, but its cause is unknown. Open Saathi to research the service issue. No screen text will be sent without your review; do not repeat a payment."
    }
    fun isNotice(value: String) = listOf("en-IN","hi-IN","hinglish").any { message(it)==value }
}
