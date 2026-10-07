package com.saathi.core

import android.graphics.Rect
import org.junit.Assert.*
import org.junit.Test

class LiveAiPolicyTest {
    @Test fun challengeNeverReachesCloudEvenWithAnOtherwiseSafeTarget() {
        val nodes = listOf(node("Help"), node("Verify you are human"))
        assertNull(LiveAiPolicy.snapshot(ticket, nodes, "Open Help", "en-IN", 1, emptyList()))
        val plan = com.saathi.orchestrator.LiveGuide.plan("Help", nodes, "en-IN", true)
        assertFalse(plan.useCloud)
        assertNull(plan.local.target)
    }
    @Test fun privateMessageContextsDoNotNeedNumericSecretsToBlockCloud() {
        for (context in listOf("Inbox", "conversation", "email_subject", "personal information", "निजी जानकारी", "niji jaankari")) {
            val nodes = listOf(node("Help"), node("I have a confidential medical concern").copy(resourceId = context))
            assertNull(context, LiveAiPolicy.snapshot(ticket, nodes, "Open Help", "en-IN", 1, emptyList()))
        }
        assertNotNull(LiveAiPolicy.snapshot(ticket, listOf(node("Help"), node("Public travel choices")), "Open Help", "en-IN", 1, emptyList()))
    }
    private val ticket = ObservationGate.Ticket(1, 2, "app.test", 3)
    private fun node(label: String) = UiNode(Rect(), label, null, null, "private.resource.id", "Button", false, true, true)
    @Test fun whitespaceCannotBypassRequestControlOrHistoryLimits() {
        assertTrue(LiveAiPolicy.allowed("Help" + " ".repeat(76), 80))
        assertFalse(LiveAiPolicy.allowed("Help" + " ".repeat(77), 80))
        assertFalse(LiveAiPolicy.allowed(" ".repeat(80), 80))
        assertNull(LiveAiPolicy.snapshot(ticket, listOf(node("Help")), "Help" + " ".repeat(157), "en-IN", 1, emptyList()))
        val snapshot = LiveAiPolicy.snapshot(ticket, listOf(node("Help"), node("Support" + " ".repeat(80))),
            "Open Help", "en-IN", 1, listOf("Support" + " ".repeat(80), "Help"))!!
        assertEquals(listOf("Help"), snapshot.controls.map { it.label })
        assertEquals(listOf("Help"), snapshot.previousSteps)
    }
    @Test fun inputValuesAndPrivateScreensNeverEnterSnapshot() {
        val values = listOf(node("Help"), node("ordinary typed value").copy(isEditable = true))
        val snapshot = LiveAiPolicy.snapshot(ticket, values, "Open Help", "en-IN", System.currentTimeMillis(), emptyList())!!
        assertEquals(listOf("Help"), snapshot.controls.map { it.label })
        assertNull(LiveAiPolicy.snapshot(ticket, values + node("PIN").copy(isSensitive = true), "Open Help", "en-IN", 1, emptyList()))
        assertNull(LiveAiPolicy.snapshot(ticket.copy(packageName = "com.android.systemui"), values, "Open Help", "en-IN", 1, emptyList()))
    }
    @Test fun consequentialAndSecretTasksOrTargetsAreNotSent() {
        for (goal in listOf("send money", "PIN 123456", "delete account")) assertFalse(LiveAiPolicy.allowed(goal))
        val snapshot = LiveAiPolicy.snapshot(ticket, listOf(node("Help"), node("Pay")), "Open Help", "en-IN", 1, listOf("Help", "Password"))!!
        assertEquals(listOf("Help"), snapshot.controls.map { it.label })
        assertEquals(listOf("Help"), snapshot.previousSteps)
    }
    @Test fun inventedStaleOrCompletionResponsesNeverBecomeGuidance() {
        val now = System.currentTimeMillis()
        val snapshot = LiveAiPolicy.snapshot(ticket, listOf(node("Help")), "Open Help", "en-IN", now, emptyList())!!
        val proposal = GuidanceProposal(1, 2, "app.test", 3, ProposedAction.HIGHLIGHT, "n0", "Help", "Help appears")
        assertTrue(snapshot.valid(proposal, now))
        assertFalse(snapshot.valid(proposal.copy(targetResourceId = "n99"), now))
        assertFalse(snapshot.valid(proposal, now + 15_001))
        assertFalse(snapshot.valid(proposal.copy(action = ProposedAction.COMPLETE, targetResourceId = null), now))
    }
}
