package com.saathi.core

/** Local-only challenge classification. Never solves challenges or retains entered values. */
object ScreenInterruption {
    enum class Reason { PRIVATE, CAPTCHA }
    private val challenge = Regex("(?i)captcha|recaptcha|hcaptcha|verify (that )?you are human|i.?m not a robot|मानव सत्यापन|कैप्चा")
    fun reason(nodes: List<UiNode>): Reason? {
        // The private flag survives masking even when the field label/value has been removed.
        if (nodes.any { it.isSensitive || it.isPassword }) return Reason.PRIVATE
        if (nodes.any { node -> listOfNotNull(node.text, node.description, node.hint, node.resourceId)
                .any { challenge.containsMatchIn(it) } }) return Reason.CAPTCHA
        return null
    }
    fun message(reason: Reason, language: String): String = when (reason) {
        Reason.PRIVATE -> when (language) {
            "hi-IN" -> "इस स्क्रीन पर निजी जानकारी है। मार्गदर्शन और माइक रुके हैं। इसे स्वयं पूरा करें। निजी स्क्रीन हटने पर साथी फिर जाँचकर मदद करेगा।"
            "hinglish" -> "Is screen par private fields hain. Guidance aur mic ruke hain. Ise khud poora karein. Private screen hatne par Saathi dobara check karke madad karega."
            else -> "This screen contains private fields. Guidance and listening are paused here. Finish privately. Saathi will recheck and resume when the private screen is gone."
        }
        Reason.CAPTCHA -> when (language) {
            "hi-IN" -> "कैप्चा स्वयं पूरा करें। मार्गदर्शन और माइक रुके हैं। कैप्चा हटने पर साथी स्क्रीन फिर जाँचकर मदद करेगा।"
            "hinglish" -> "CAPTCHA khud poora karein. Guidance aur mic ruke hain. CAPTCHA hatne par Saathi screen dobara check karke madad karega."
            else -> "Complete the CAPTCHA yourself. Guidance and listening are paused. Saathi will recheck and resume after the challenge is gone."
        }
    }
}
