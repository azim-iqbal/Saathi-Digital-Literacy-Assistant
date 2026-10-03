package com.saathi.core

/** Optional reviewed summary only; never screen trees, evidence files or microphone audio. */
data class IncidentAssessmentRequest(
    val requestId: String = java.util.UUID.randomUUID().toString(),
    val sessionId: String = "incident-" + java.util.UUID.randomUUID(),
    val revision: Long = 1,
    val observedAtMs: Long = System.currentTimeMillis(),
    val locale: String,
    val summary: String,
    val concern: String
)
data class IncidentAssessment(val category: String, val signals: Set<String>)
object IncidentAssessmentPolicy {
    val categories = setOf("POSSIBLE_FINANCIAL", "POSSIBLE_OTHER", "UNCLEAR")
    val signals = setOf("UNAUTHORISED_TRANSACTION", "DECEPTIVE_REQUEST", "ACCOUNT_ACCESS", "THREAT_OR_HARASSMENT", "INSUFFICIENT_CONTEXT")
    private val privatePattern = Regex("(?i)https?://|www\\.|@|\\b(?:password|pin|otp|secret|token)\\s*(?:is|was|=|:)\\s*\\S+")
    fun allowed(summary: String) = summary.length in 12..1200 && summary.isNotBlank() &&
        summary.none { it.code < 32 || it.isSurrogate() || it.isDigit() } && !privatePattern.containsMatchIn(summary)
    fun description(result: IncidentAssessment): String = when (result.category) {
        "POSSIBLE_FINANCIAL" -> "The two AI providers suggest possible financial cyber fraud. This is not a confirmed finding. If money was lost to suspected fraud or a transaction was unauthorised, call 1930 and your bank now. Use the official reporting steps below; recovery is not guaranteed."
        "POSSIBLE_OTHER" -> "The two AI providers suggest possible cybercrime or online harm. This is not a confirmed finding. Review the relevant complaint category on the official portal or contact local police."
        else -> "The two AI providers could not determine a clear reporting category. This does not mean the incident is safe or that no crime occurred. You can still use the reporting checklist or contact local police. If money was lost to suspected fraud, call 1930 and your bank now."
    }
}
