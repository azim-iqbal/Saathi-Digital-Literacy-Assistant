package com.saathi.core

/**
 * A transport-free contract for a future provider gateway. This module does not make network
 * calls, collect usage, or select a model. It exists so failure behavior is testable before a
 * provider is ever connected to a user screen.
 */
data class SanitizedScreenSnapshot(
    val sessionId: Long,
    val screenRevision: Long,
    val packageName: String,
    val windowId: Int,
    val locale: String,
    val eligibleNodeIds: Set<String>,
    val observedAtMs: Long = System.currentTimeMillis(),
    val requestId: String = java.util.UUID.randomUUID().toString(),
    val task: SanitizedTaskCategory = SanitizedTaskCategory.WATER_BILL
) {
    companion object {
        /** Exposes identifiers only. Visible text, hints, bounds and sensitive controls stay local. */
        fun from(ticket: ObservationGate.Ticket, locale: String, nodes: List<UiNode>,
                 task: SanitizedTaskCategory = SanitizedTaskCategory.WATER_BILL, observedAtMs: Long = System.currentTimeMillis()) = SanitizedScreenSnapshot(
            task = task, observedAtMs = observedAtMs,
            sessionId = ticket.session,
            screenRevision = ticket.revision,
            packageName = ticket.packageName,
            windowId = ticket.windowId,
            locale = locale,
            eligibleNodeIds = nodes.asSequence()
                .filter { it.isEnabled && !it.isSensitive && !it.isPassword }
                .mapNotNull { it.resourceId }.filter { it in PracticeGatewayContract.publicIds }
                .toSet()
        )
    }
}

enum class ProposedAction { HIGHLIGHT, HANDOVER, COMPLETE }

/** Only reviewed categories may enter a future proposal request; raw user wording is excluded. */
enum class SanitizedTaskCategory { ELECTRICITY_BILL, WATER_BILL, DTH_RECHARGE }

data class GuidanceProposal(
    val sessionId: Long,
    val screenRevision: Long,
    val packageName: String,
    val windowId: Int,
    val action: ProposedAction,
    val targetResourceId: String?,
    val explanation: String,
    val expectedOutcome: String,
    val completionEvidence: Set<String> = emptySet(),
    val uncertainty: List<String> = emptyList()
)

/** Each provider has a bounded, explicit result; a failure is never silently converted to advice. */
sealed interface ProviderProposalResult {
    data class Proposal(val value: GuidanceProposal) : ProviderProposalResult
    data object TimedOut : ProviderProposalResult
    data object QuotaExhausted : ProviderProposalResult
    data object Unavailable : ProviderProposalResult
    data object Malformed : ProviderProposalResult
}

interface GuidanceProposalProvider {
    val id: String
    fun propose(snapshot: SanitizedScreenSnapshot, task: SanitizedTaskCategory): ProviderProposalResult
}

enum class GuidanceRejection {
    PROVIDER_UNAVAILABLE,
    MALFORMED,
    STALE,
    DISAGREEMENT,
    INVALID_TARGET,
    UNOBSERVED_COMPLETION
}

sealed interface DualGuidanceDecision {
    data class Accepted(val proposal: GuidanceProposal) : DualGuidanceDecision
    data class Rejected(val reason: GuidanceRejection) : DualGuidanceDecision
}

/**
 * Accepts guidance only if two independently supplied proposals agree exactly on the action and
 * local target. The caller must still re-check its live ObservationGate before presentation.
 */
object DualProposalValidator {
    fun decide(
        current: SanitizedScreenSnapshot,
        first: ProviderProposalResult,
        second: ProviderProposalResult,
        nowMs: Long = System.currentTimeMillis()
    ): DualGuidanceDecision {
        if (first !is ProviderProposalResult.Proposal || second !is ProviderProposalResult.Proposal) {
            return DualGuidanceDecision.Rejected(
                if (first is ProviderProposalResult.Malformed || second is ProviderProposalResult.Malformed) {
                    GuidanceRejection.MALFORMED
                } else {
                    GuidanceRejection.PROVIDER_UNAVAILABLE
                }
            )
        }
        val a = first.value
        val b = second.value
        validate(current, a, nowMs)?.let { return DualGuidanceDecision.Rejected(it) }
        validate(current, b, nowMs)?.let { return DualGuidanceDecision.Rejected(it) }
        if (a.action != b.action || a.targetResourceId != b.targetResourceId) {
            return DualGuidanceDecision.Rejected(GuidanceRejection.DISAGREEMENT)
        }
        return DualGuidanceDecision.Accepted(a)
    }

    /** Also used for a strictly decoded, paired gateway decision; it does not manufacture a second vote. */
    fun validate(current: SanitizedScreenSnapshot, proposal: GuidanceProposal, nowMs: Long = System.currentTimeMillis()): GuidanceRejection? {
        if (current.observedAtMs > nowMs || current.observedAtMs < nowMs - 15_000 || !proposal.matches(current)) return GuidanceRejection.STALE
        if (current.packageName != "com.saathi" || current.windowId < 0 || current.screenRevision < 0 ||
            current.locale !in setOf("en-IN", "hi-IN", "hinglish") || current.eligibleNodeIds.isEmpty() ||
            !PracticeGatewayContract.publicIds.containsAll(current.eligibleNodeIds) || !proposal.isWellFormed() ||
            proposal.uncertainty.isNotEmpty()) return GuidanceRejection.MALFORMED
        if (proposal.action == ProposedAction.HIGHLIGHT &&
            (proposal.targetResourceId !in current.eligibleNodeIds || proposal.targetResourceId !in PracticeGatewayContract.actionIds)) return GuidanceRejection.INVALID_TARGET
        if (proposal.action == ProposedAction.COMPLETE) {
            val marker = PracticeGatewayContract.marker(current.task)
            val expected = setOf("com.saathi:id/success_title", marker)
            if (proposal.completionEvidence != expected || !current.eligibleNodeIds.containsAll(expected) ||
                current.eligibleNodeIds.intersect(PracticeGatewayContract.categoryIds) != setOf(marker)) return GuidanceRejection.UNOBSERVED_COMPLETION
        } else if (proposal.completionEvidence.isNotEmpty()) return GuidanceRejection.MALFORMED
        return null
    }

    private fun GuidanceProposal.matches(snapshot: SanitizedScreenSnapshot) =
        sessionId == snapshot.sessionId &&
            screenRevision == snapshot.screenRevision &&
            packageName == snapshot.packageName &&
            windowId == snapshot.windowId

    private fun GuidanceProposal.isWellFormed(): Boolean {
        if (explanation.isBlank() || expectedOutcome.isBlank()) return false
        if (explanation.length > MAX_TEXT || expectedOutcome.length > MAX_TEXT) return false
        return when (action) {
            ProposedAction.HIGHLIGHT -> !targetResourceId.isNullOrBlank()
            ProposedAction.HANDOVER, ProposedAction.COMPLETE -> targetResourceId == null
        }
    }

    private const val MAX_TEXT = 240
}
