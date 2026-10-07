package com.saathi.core

/** Risk cues never establish a site's authority. Until a browser origin can be
 * bound to reviewed provenance, high-risk cross-app guidance fails closed.
 */
object DestinationPolicy {
    private val highRisk = Regex("(?i)\\b(government|banking|bank account|credit|loan|identity verification|health benefit|scholarship|passport|licence application|license application)\\b|सरकारी|बैंक खाता|ऋण|पहचान सत्यापन|छात्रवृत्ति|sarkari|bank khata|pehchaan satyapan")
    fun requiresProvenance(goal: String, nodes: List<UiNode> = emptyList()) = highRisk.containsMatchIn(goal) ||
        nodes.any { n -> listOfNotNull(n.text,n.description,n.hint).any { highRisk.containsMatchIn(it.take(300)) } }
    fun explanation(locale: String) = when(locale) {
        "hi-IN" -> "इस काम में सही आधिकारिक सेवा की पुष्टि ज़रूरी है। साथी मौजूदा ऐप या वेबसाइट का स्रोत सत्यापित नहीं कर पाया, इसलिए कोई विकल्प नहीं दिखाएगा। पहले साथी में स्रोत खोजें और स्वतंत्र रूप से जाँचें।"
        "hinglish" -> "Is kaam mein sahi official service ki pushti zaroori hai. Saathi maujooda app ya website ka source verify nahin kar paya, isliye koi option nahin dikhayega. Pehle Saathi mein sources khojein aur alag se jaanchein."
        else -> "This task needs a verified official destination. Saathi has not established the current app or website's provenance, so I won't highlight a step here. Research the sources in Saathi and independently verify the destination first."
    }
}
