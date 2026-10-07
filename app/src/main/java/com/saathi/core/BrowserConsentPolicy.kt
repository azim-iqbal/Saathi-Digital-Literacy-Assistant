package com.saathi.core

/** A consent dialog asks the person to decide. No inferred acceptance or highlighted default. */
object BrowserConsentPolicy {
    private val consent = Regex("(?i)cookie preferences|cookie consent|privacy choices|consent preferences|कुकी प्राथमिकताएं|cookie pasand")
    fun present(nodes: List<UiNode>) = nodes.any { n -> listOfNotNull(n.text,n.description,n.hint).any(consent::containsMatchIn) }
    fun message(locale: String) = when(locale) {
        "hi-IN" -> "यह गोपनीयता की पसंद है। विकल्प पढ़कर स्वयं चुनें; साथी अपने आप सहमति नहीं देगा। इसके बाद स्क्रीन फिर जाँची जाएगी।"
        "hinglish" -> "Yeh privacy ki pasand hai. Options padhkar khud chunein; Saathi apne aap sahmati nahin dega. Phir screen dobara jaanchi jayegi."
        else -> "This is a privacy choice. Read the options and choose yourself; Saathi will not accept for you. I’ll recheck the screen afterward."
    }
}
