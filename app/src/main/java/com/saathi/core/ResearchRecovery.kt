package com.saathi.core

/** Fixed, local recovery text. Never renders a provider's exception or retries a paid call. */
object ResearchRecovery {
    private val retryable = setOf("busy", "rate_limited", "timeout", "provider_timeout", "provider_unavailable",
        "unavailable", "connection_failed", "local_backend_unreachable", "circuit_open", "provider_rate_limited", "research_unavailable")
    fun canRetryPlan(reason: String?) = reason in retryable
    fun message(reason: String?, locale: String): String {
        fun local(en: String, hi: String, hinglish: String) = when(locale) { "hi-IN" -> hi; "hinglish" -> hinglish; else -> en }
        return when(reason) {
            "research_expired", "research_cancelled", "cancelled", "stale" -> local(
                "This research is expired or was cancelled. Find sources again and review them before requesting another plan.",
                "यह खोज पुरानी हो गई या रद्द हुई थी। फिर स्रोत खोजें और दूसरी योजना माँगने से पहले उन्हें जाँचें।",
                "Yeh khoj purani ho gayi ya radd hui thi. Phir sources khojein aur doosra plan maangne se pehle unhein jaanchein.")
            "research_not_configured" -> local("Research sources are not configured on this server. No model call was made.", "सर्वर पर स्रोत तय नहीं हैं। किसी AI सेवा को नहीं बुलाया गया।", "Server par sources tay nahin hain. Kisi AI service ko nahin bulaya gaya.")
            "not_configured", "unauthorized", "provider_auth", "provider_model", "provider_request" -> local(
                "Check the backend connection and provider configuration. You can read any sources already shown here; no plan was accepted.",
                "बैकएंड कनेक्शन और AI सेवा की सेटिंग जाँचें। यहाँ पहले से दिख रहे स्रोत पढ़ सकते हैं; कोई योजना स्वीकार नहीं हुई।",
                "Backend connection aur AI service ki settings jaanchein. Yahan pehle se dikh rahe sources padh sakte hain; koi plan sweekar nahin hua.")
            "secure_connection_failed" -> local(
                "The secure connection could not be verified. Check the certificate and device date; do not bypass the warning. Read the sources already shown here.",
                "सुरक्षित कनेक्शन सत्यापित नहीं हुआ। प्रमाणपत्र और डिवाइस की तारीख जाँचें; चेतावनी पार न करें। यहाँ दिख रहे स्रोत पढ़ें।",
                "Secure connection verify nahin hua. Certificate aur device ki tareekh jaanchein; warning paar na karein. Yahan dikh rahe sources padhein.")
            "quota_exhausted", "budget_unavailable", "retrieval_budget_exhausted" -> local(
                "The backend's request budget or storage is unavailable. Stop retrying until it is restored. You can read the sources already shown, but they may expire.",
                "बैकएंड का अनुरोध बजट या स्टोरेज उपलब्ध नहीं है। बहाल होने तक दोबारा कोशिश न करें। दिख रहे स्रोत पढ़ सकते हैं, पर वे पुराने हो सकते हैं।",
                "Backend ka request budget ya storage available nahin hai. Bahal hone tak dobara koshish na karein. Dikh rahe sources padh sakte hain, par woh purane ho sakte hain.")
            in retryable -> local(
                "The request could not finish. Check the connection or wait, then choose whether to request it again. Nothing will retry automatically. Read the available sources meanwhile.",
                "अनुरोध पूरा नहीं हुआ। कनेक्शन जाँचें या प्रतीक्षा करें, फिर खुद तय करें कि दोबारा अनुरोध करना है। अपने आप दोबारा कोशिश नहीं होगी। तब तक उपलब्ध स्रोत पढ़ें।",
                "Request poori nahin hui. Connection jaanchein ya intezaar karein, phir khud tay karein ki dobara request karni hai. Apne aap retry nahin hoga. Tab tak available sources padhein.")
            "source_unverified", "evidence_missing", "disagreement", "jurisdiction_mismatch" -> local(
                "The evidence is insufficient or the proposals disagree. No plan was accepted. Review the sources and region, then refresh the research if needed.",
                "प्रमाण अधूरे हैं या प्रस्ताव अलग हैं। कोई योजना स्वीकार नहीं हुई। स्रोत और क्षेत्र जाँचें, फिर ज़रूरत हो तो दोबारा खोजें।",
                "Pramaan adhoore hain ya proposals alag hain. Koi plan sweekar nahin hua. Sources aur region jaanchein, phir zaroorat ho toh dobara khojein.")
            else -> local("Research could not be verified. Review the sources manually; this does not establish eligibility or completion.", "खोज सत्यापित नहीं हुई। स्रोत खुद जाँचें; इससे पात्रता या काम पूरा होना तय नहीं होता।", "Khoj verify nahin hui. Sources khud jaanchein; isse patrata ya kaam poora hona tay nahin hota.")
        }
    }
}
