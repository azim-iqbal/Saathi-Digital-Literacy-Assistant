package com.saathi.intake

/**
 * Fast, device-local preparation for the apps we can identify confidently. This provides a
 * useful starting checklist without sending the user's task to a third party. It does not
 * claim to inspect a person's account or browse a live service; the visible screen remains
 * the source of truth for each actual instruction.
 */
object FlowResearchCoordinator {
    data class Briefing(
        val source: String,
        val summary: String,
        val likelySteps: List<String>
    )

    fun prepare(brief: TaskBrief): Briefing {
        val app = brief.appOrWebsite.orEmpty().lowercase()
        val knownFlow = when {
            app.contains("provider") || app.contains("website") -> listOf("Find the provider's official bill-payment page", "Review account and amount details", "Complete final payment approval yourself")
            app.contains("irctc") -> listOf("Choose journey details", "Review train and passenger details", "Confirm the booking yourself")
            app.contains("demo bill") -> listOf("Open Recharge & Pay Bills", "Choose Electricity, Water, or DTH", "Enter details and confirm yourself")
            else -> emptyList()
        }

        return if (knownFlow.isNotEmpty()) {
            Briefing("On-device app profile", "This is a general checklist, not a verified workflow for ${brief.appOrWebsite}. Live guidance is currently limited to local practice.", knownFlow)
        } else {
            Briefing("Live screen preparation", "This app or website has not been verified. Live guidance is currently limited to local practice.", emptyList())
        }
    }
}
