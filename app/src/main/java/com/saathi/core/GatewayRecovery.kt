package com.saathi.core

import com.saathi.language.GuidanceLanguage

/** Only reviewed reason codes can affect UI copy. Never display a server/provider body. */
object GatewayRecovery {
    private val reasons = setOf("unavailable", "not_configured", "unauthorized", "provider_auth", "provider_request", "provider_model", "provider_rate_limited",
        "provider_unavailable", "provider_timeout", "timeout", "connection_failed", "busy", "session_capacity",
        "quota_exhausted", "budget_unavailable", "circuit_open", "cancelled", "stale", "duplicate_request",
        "invalid_request", "invalid_response", "invalid_target", "invalid_decision", "malformed", "uncertain",
        "disagreement", "not_verified", "unobserved_completion")
    fun reason(value: Any?): String = (value as? String)?.takeIf { it in reasons } ?: "not_verified"
    fun httpStatus(status: Int): String = when (status) {
        401, 403 -> "unauthorized"
        408, 504 -> "timeout"
        429 -> "busy"
        400, 413, 415 -> "invalid_request"
        else -> "unavailable"
    }
    fun message(reason: String, language: GuidanceLanguage): String {
        val copy = when (reason(reason)) {
            "not_configured", "provider_auth", "unauthorized" -> arrayOf(
                "AI is not connected. Check the local backend setup and credentials. You can switch to finding a visible option in Saathi.",
                "AI जुड़ा नहीं है। स्थानीय बैकएंड सेटअप और पहुँच की जानकारी जाँचें। साथी में दिख रहे विकल्प को खोजने वाला मोड चुन सकते हैं।",
                "AI connected nahi hai. Local backend setup aur credentials check karein. Saathi mein visible option dhoondhne ka mode chun sakte hain.")
            "quota_exhausted", "budget_unavailable", "circuit_open" -> arrayOf(
                "AI requests are paused by the backend. Check its budget, storage or provider status before trying again. You can use visible-option help meanwhile.",
                "बैकएंड ने AI अनुरोध रोके हैं। फिर कोशिश करने से पहले बजट, स्टोरेज या प्रदाता की स्थिति जाँचें। तब तक दिख रहे विकल्प की मदद ले सकते हैं।",
                "Backend ne AI requests roke hain. Dobara try karne se pehle budget, storage ya provider status check karein. Tab tak visible-option help use kar sakte hain.")
            "busy", "session_capacity", "provider_rate_limited" -> arrayOf(
                "The backend is busy. Wait a moment, then pause and resume guidance to try again. No step was verified.",
                "बैकएंड व्यस्त है। थोड़ी देर बाद मार्गदर्शन रोककर फिर शुरू करें। कोई कदम सत्यापित नहीं हुआ।",
                "Backend busy hai. Thoda ruk kar guidance pause aur resume karein. Koi step verify nahi hua.")
            "timeout", "provider_timeout", "provider_unavailable", "connection_failed", "unavailable" -> arrayOf(
                "The backend could not respond. Check the connection, then pause and resume guidance. You can use visible-option help if it stays unavailable.",
                "बैकएंड जवाब नहीं दे सका। कनेक्शन जाँचें, फिर मार्गदर्शन रोककर शुरू करें। समस्या रहने पर दिख रहे विकल्प की मदद लें।",
                "Backend jawab nahi de saka. Connection check karein, phir guidance pause aur resume karein. Problem rahe to visible-option help lein.")
            "cancelled", "stale", "duplicate_request" -> arrayOf(
                "That screen is no longer current. Wait for the new screen, or pause and resume guidance. No old marker is shown.",
                "वह स्क्रीन अब वर्तमान नहीं है। नई स्क्रीन का इंतज़ार करें या मार्गदर्शन रोककर फिर शुरू करें। पुराना निशान नहीं दिखेगा।",
                "Woh screen ab current nahi hai. Nayi screen ka wait karein ya guidance pause aur resume karein. Purana marker nahi dikhaya.")
            else -> arrayOf(
                "I could not verify a clear next step. No marker is shown. Describe a more specific task or use visible-option help.",
                "अगला कदम स्पष्ट नहीं हुआ। कोई निशान नहीं दिखाया गया। काम को और स्पष्ट बताएँ या दिख रहे विकल्प की मदद लें।",
                "Clear next step verify nahi hua. Koi marker nahi dikhaya. Task aur clearly batayein ya visible-option help lein.")
        }
        return copy[when (language) { GuidanceLanguage.ENGLISH -> 0; GuidanceLanguage.HINDI -> 1; GuidanceLanguage.HINGLISH -> 2 }]
    }
}
