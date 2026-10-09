package com.saathi.core

/** Local-only challenge classification. Never solves challenges or retains entered values. */
object ScreenInterruption {
    enum class Reason { PRIVATE, CAPTCHA }
    private val challenge = Regex("(?i)captcha|recaptcha|hcaptcha|verify (that )?you are human|i.?m not a robot|मानव सत्यापन|कैप्चा")
    fun reason(nodes: List<UiNode>): Reason? {
        // The private flag survives masking even when the field label/value has been removed.
        if (nodes.any { it.isSensitive || it.isPassword } || PrivateContextPolicy.blocksCloud(nodes)) return Reason.PRIVATE
        if (nodes.any { node -> listOfNotNull(node.text, node.description, node.hint, node.resourceId)
                .any { challenge.containsMatchIn(it) } }) return Reason.CAPTCHA
        return null
    }
    /** Local bounds only: no value, ID or field label enters the target/instruction. */
    fun privateTarget(nodes: List<UiNode>): GuideTarget? {
        if (PrivateContextPolicy.blocksCloud(nodes) || BrowserSafetyPolicy.present(nodes) ||
            PaymentSafety.state(nodes) != null || nodes.any { node ->
                listOfNotNull(node.text, node.description, node.hint).any { challenge.containsMatchIn(it) }
            }) return null
        val candidates = nodes.withIndex().filter { it.value.structuralPrivateField }
        val candidate = candidates.singleOrNull() ?: return null
        val node = candidate.value
        if (!node.isEnabled || !node.isEditable || node.bounds.isEmpty) return null
        return GuideTarget(android.graphics.Rect(node.bounds), null, "Private field", candidate.index)
    }
    fun message(reason: Reason, language: String): String = when (reason) {
        Reason.PRIVATE -> when (language) {
            "hi-IN" -> "इस चरण में निजी जानकारी चाहिए। इसे स्वयं भरें। साथी इसे AI को नहीं भेजेगा या सहेजेगा। सुरक्षित स्क्रीन आने पर मदद फिर शुरू होगी। माइक दोबारा स्वयं चालू करें।"
            "hinglish" -> "Is step mein private jaankari chahiye. Ise khud bharein. Saathi ise AI ko nahi bhejega ya save karega. Safe screen par guidance phir shuru hogi. Mic dobara khud chalu karein."
            else -> "This step needs private information. Enter it yourself. Saathi won't send it to AI or save it. Guidance resumes on a safe screen. Turn the microphone back on yourself."
        }
        Reason.CAPTCHA -> when (language) {
            "hi-IN" -> "कैप्चा स्वयं पूरा करें। मार्गदर्शन और माइक रुके हैं। कैप्चा हटने पर साथी स्क्रीन फिर जाँचकर मदद करेगा।"
            "hinglish" -> "CAPTCHA khud poora karein. Guidance aur mic ruke hain. CAPTCHA hatne par Saathi screen dobara check karke madad karega."
            else -> "Complete the CAPTCHA yourself. Guidance and listening are paused. Saathi will recheck and resume after the challenge is gone."
        }
    }
}
