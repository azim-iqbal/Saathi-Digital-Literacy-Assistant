package com.saathi.core

import com.saathi.accessibility.SensitiveContent

data class LiveAiControl(val id: String, val label: String, val nodeIndex: Int)
data class LiveAiSnapshot(
    val requestId: String, val sessionId: Long, val screenRevision: Long, val observedAtMs: Long,
    val packageName: String, val windowId: Int, val locale: String, val goal: String,
    val controls: List<LiveAiControl>, val previousSteps: List<String>
) {
    fun valid(proposal: GuidanceProposal, now: Long = System.currentTimeMillis()): Boolean =
        now >= observedAtMs && now - observedAtMs <= 15_000 && proposal.sessionId == sessionId &&
            proposal.screenRevision == screenRevision && proposal.packageName == packageName && proposal.windowId == windowId &&
            proposal.completionEvidence.isEmpty() && proposal.uncertainty.isEmpty() &&
            proposal.explanation.length in 1..240 && proposal.expectedOutcome.length in 1..240 &&
            when (proposal.action) {
                ProposedAction.HIGHLIGHT -> controls.any { it.id == proposal.targetResourceId }
                ProposedAction.HANDOVER -> proposal.targetResourceId == null
                ProposedAction.COMPLETE -> false // Arbitrary goals have no reviewed completion predicate.
            }
}

object LiveAiPolicy {
    private val consequential = Regex("(?i)\\b(pay|purchase|buy|send|transfer|delete|remove|confirm|submit|install|allow|approve|accept|agree|reset|erase)\\b|भुगतान|भेज|मिटा|स्वीकार|अनुमति")
    fun allowed(text: String, limit: Int = 160) = text.isNotBlank() && text.length <= limit &&
        text.none { it.code < 32 } && !SensitiveContent.isSensitive(false, text) && !consequential.containsMatchIn(text)

    fun snapshot(ticket: ObservationGate.Ticket, nodes: List<UiNode>, goal: String, locale: String,
                 observedAtMs: Long, previous: List<String>): LiveAiSnapshot? {
        if (!allowed(goal) || ResearchIntent.claimType(goal) != null || ScreenInterruption.reason(nodes) != null || ScreenErrorPolicy.present(nodes) || BrowserSafetyPolicy.present(nodes) || PrivateContextPolicy.blocksCloud(nodes) || PaymentSafety.state(nodes) != null || BrowserConsentPolicy.present(nodes) || DestinationPolicy.requiresProvenance(goal, nodes) ||
            !com.saathi.orchestrator.LiveGuide.allowedPackage(ticket.packageName, "com.saathi")) return null
        val controls = nodes.mapIndexedNotNull { index, node ->
            // Text-entry values, including browser address bars, never become cloud controls.
            if (!node.isEnabled || node.isEditable || node.className.orEmpty().contains("EditText") ||
                (!node.isClickable && node.clickableAncestorBounds == null)) return@mapIndexedNotNull null
            val label = node.text?.takeIf { it.isNotBlank() } ?: node.description ?: return@mapIndexedNotNull null
            if (!allowed(label, 80)) return@mapIndexedNotNull null
            LiveAiControl("n$index", label, index)
        }.distinctBy { it.label.trim().lowercase(java.util.Locale.ROOT) }.take(32)
        // Distinct labels at separate tap locations are ambiguous: withhold them rather than pick one.
        val unambiguous = controls.filter { control ->
            val candidate = nodes[control.nodeIndex]
            val bounds = candidate.clickableAncestorBounds.takeUnless { candidate.isClickable } ?: candidate.bounds
            nodes.none { other -> other !== candidate && other.isEnabled &&
                (other.isClickable || other.clickableAncestorBounds != null) &&
                (other.text?.trim()?.equals(control.label.trim(), true) == true || other.description?.trim()?.equals(control.label.trim(), true) == true) &&
                (other.clickableAncestorBounds.takeUnless { other.isClickable } ?: other.bounds) != bounds }
        }
        if (unambiguous.isEmpty()) return null
        return LiveAiSnapshot(java.util.UUID.randomUUID().toString(), ticket.session, ticket.revision, observedAtMs,
            ticket.packageName, ticket.windowId, locale, goal.trim(), unambiguous, previous.filter { allowed(it, 80) }.takeLast(3))
    }
}
