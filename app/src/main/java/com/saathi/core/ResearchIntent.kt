package com.saathi.core

/** Routes changing factual questions to consented evidence retrieval, not model memory.
 * General claim categories only; never encodes a service's prerequisite or answer.
 */
object ResearchIntent {
    private val categories = listOf(
        "outage" to "outage|service down|not working|error|सेवा बंद|काम नहीं कर|त्रुटि|kaam nahi|kaam nahin|samasya",
        "eligibility" to "eligib|qualify|qualification|पात्र|योग्यता|patrata|yogya",
        "deadline" to "deadline|last date|अंतिम तारीख|आखिरी तारीख|aakhri tareekh|antim tithi",
        "pricing" to "pricing|fees?|cost|price|शुल्क|कीमत|shulk|keemat",
        "requirements" to "prerequisite|requirement|required document|documents needed|apply for|application process|ज़रूरी दस्तावेज|जरूरी दस्तावेज|पूर्व शर्त|आवेदन|zaroori dastavez|zaruri dastavez|aavedan|apply kar"
    ).map { (kind, words) -> kind to Regex("(?i)(?:$words)") }
    fun claimType(goal: String): String? = categories.firstOrNull { it.second.containsMatchIn(goal.take(160)) }?.first
    fun message(locale: String) = when(locale) {
        "hi-IN" -> "इस सवाल के लिए ताज़ा स्रोत जाँचना ज़रूरी है। साथी खोलें और स्रोत खोजें; सिर्फ स्क्रीन या AI की याद से उत्तर नहीं दिया जाएगा।"
        "hinglish" -> "Is sawaal ke liye taaza sources jaanchna zaroori hai. Saathi kholein aur sources khojein; sirf screen ya AI ki yaad se jawaab nahin diya jayega."
        else -> "This question needs current sources. Open Saathi and research the requirements or issue; screen labels and model memory alone cannot establish the answer."
    }
}
