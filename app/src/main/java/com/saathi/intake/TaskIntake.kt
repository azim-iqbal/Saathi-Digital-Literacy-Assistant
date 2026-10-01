package com.saathi.intake

import com.saathi.guardrails.GuardrailDecision
import com.saathi.guardrails.GuardrailEngine

enum class TaskKind(val title: String, val examples: List<String>) {
    BILL_PAYMENT("Pay a bill", listOf("Electricity bill", "Water bill", "DTH recharge")),
    PAYMENT("Make a payment", listOf("UPI payment", "Send money", "Scan and pay")),
    FORM("Fill a form", listOf("Government form", "College application", "Registration")),
    TICKET("Book a ticket", listOf("Train ticket", "Bus ticket", "Flight booking")),
    SETTINGS("Phone settings", listOf("Accessibility", "Permissions", "Essential settings"))
}

data class TaskBrief(
    val kind: TaskKind,
    val goal: String,
    val appOrWebsite: String? = null
)

sealed interface IntakeResult {
    data class AskForApp(val brief: TaskBrief, val message: String) : IntakeResult
    data class Ready(val brief: TaskBrief, val preparation: PreparationNote) : IntakeResult
    data class Refused(val message: String) : IntakeResult
}

data class PreparationNote(
    val title: String,
    val detail: String,
    val suggestedFirstStep: String
)

/**
 * Converts a short user intent into a bounded Saathi task. Network research is intentionally
 * not assumed here: an optional online provider can enrich this later, while the on-screen
 * accessibility context remains the source of truth for every actual next instruction.
 */
object TaskIntake {
    fun understandGoal(goal: String): IntakeResult {
        if (GuardrailEngine.classify(goal).decision != GuardrailDecision.ALLOW) {
            return IntakeResult.Refused(
                "I can help with a task on your screen, like a bill, form, payment, ticket, or essential setting. What would you like to complete?"
            )
        }

        val kind = classifyKind(goal)
        return IntakeResult.AskForApp(
            TaskBrief(kind = kind, goal = goal),
            "I can help with ${kind.title.lowercase()}. Which app, brand, or website are you using?"
        )
    }

    fun prepare(brief: TaskBrief, appOrWebsite: String): IntakeResult.Ready {
        val completedBrief = brief.copy(appOrWebsite = appOrWebsite.trim())
        val note = when (completedBrief.kind) {
            TaskKind.BILL_PAYMENT -> PreparationNote(
                title = "Ready to pay a bill with ${completedBrief.appOrWebsite}",
                detail = "For the clearest guidance, use the provider's official website for bill details. If it later sends you to a payment app, you complete that final approval yourself.",
                suggestedFirstStep = "Open ${completedBrief.appOrWebsite} and show its bill-payment page."
            )
            TaskKind.PAYMENT -> PreparationNote(
                title = "Payment flow prepared",
                detail = "I can guide an official website route. Payment apps may deliberately hide their screens, so you always choose the recipient and approve any payment yourself.",
                suggestedFirstStep = "Open ${completedBrief.appOrWebsite} and tell me when you are ready."
            )
            TaskKind.FORM -> PreparationNote(
                title = "Form assistance prepared",
                detail = "I will help you find each section and explain what belongs there. Sensitive fields stay private.",
                suggestedFirstStep = "Open ${completedBrief.appOrWebsite} and show the form's first page."
            )
            TaskKind.TICKET -> PreparationNote(
                title = "Ticket booking prepared",
                detail = "I will help you find the journey details and review choices. You confirm every booking yourself.",
                suggestedFirstStep = "Open ${completedBrief.appOrWebsite} and show the booking screen."
            )
            TaskKind.SETTINGS -> PreparationNote(
                title = "Settings assistance prepared",
                detail = "I will point out the relevant setting and explain why it is needed.",
                suggestedFirstStep = "Open ${completedBrief.appOrWebsite} or Android Settings."
            )
        }
        return IntakeResult.Ready(completedBrief, note)
    }

    private fun classifyKind(goal: String): TaskKind {
        val value = goal.lowercase()
        return when {
            listOf("form", "application", "registration", "apply", "आवेदन", "फॉर्म", "form fill").any(value::contains) -> TaskKind.FORM
            listOf("ticket", "train", "bus", "flight", "booking", "टिकट", "बुकिंग").any(value::contains) -> TaskKind.TICKET
            listOf("setting", "permission", "accessibility", "सेटिंग", "अनुमति").any(value::contains) -> TaskKind.SETTINGS
            listOf("upi", "send money", "phonepe", "gpay", "google pay", "payment", "पेमेंट", "भुगतान").any(value::contains) -> TaskKind.PAYMENT
            else -> TaskKind.BILL_PAYMENT
        }
    }

}
