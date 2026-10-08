package com.saathi.core

/** Browser warnings are a user boundary, never an invitation to bypass TLS or malware checks. */
object BrowserSafetyPolicy {
    private val warning = Regex("(?i)your connection is not private|connection (?:is )?not secure|(?:net::)?err_(?:cert|ssl)_[a-z_]+|deceptive site ahead|dangerous site|suspected phishing|आपका कनेक्शन निजी नहीं है|कनेक्शन सुरक्षित नहीं है|धोखाधड़ी वाली साइट|connection surakshit nahin hai")
    fun present(nodes: List<UiNode>) = nodes.any { node -> !node.isEditable && !node.isSensitive &&
        listOfNotNull(node.text,node.description).any { warning.containsMatchIn(it.take(300)) } }
    fun message(locale:String) = when(locale) {
        "hi-IN" -> "ब्राउज़र सुरक्षा चेतावनी दिखा रहा है। चेतावनी को पार न करें और निजी जानकारी न भरें। वापस जाएँ और स्वतंत्र रूप से आधिकारिक पता जाँचें।"
        "hinglish" -> "Browser suraksha warning dikha raha hai. Warning ko paar na karein aur niji jaankari na bharein. Wapas jaayein aur alag se official pata jaanchein."
        else -> "The browser shows a security warning. Do not bypass it or enter private information. Go back and independently verify the official address."
    }
}
