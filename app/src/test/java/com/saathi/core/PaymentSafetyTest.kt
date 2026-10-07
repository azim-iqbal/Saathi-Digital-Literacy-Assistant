package com.saathi.core
import android.graphics.Rect
import org.junit.Assert.*
import org.junit.Test
class PaymentSafetyTest {
    private fun node(text: String) = UiNode(Rect(),text,null,null,null,"Button",false,true,true)
    @Test fun pendingAndUnknownNeverBecomeAnotherPaymentAttempt() {
        for ((text,locale) in listOf("Payment pending" to "en-IN", "भुगतान लंबित" to "hi-IN", "Payment ka intezaar" to "hinglish")) {
            val nodes=listOf(node(text),node("Help"))
            assertEquals(PaymentSafety.State.PENDING,PaymentSafety.state(nodes))
            val plan=com.saathi.orchestrator.LiveGuide.plan("Help",nodes,locale,true)
            assertFalse(plan.useCloud); assertNull(plan.local.target); assertFalse(plan.local.goalComplete)
        }
    }
    @Test fun contradictionsAndFailuresDoNotProveSuccessOrAuthorizeRetry() {
        assertEquals(PaymentSafety.State.PENDING,PaymentSafety.state(listOf(node("Payment successful pending"))))
        assertEquals(PaymentSafety.State.FAILED,PaymentSafety.state(listOf(node("Payment unsuccessful"))))
        assertEquals(PaymentSafety.State.UNKNOWN,PaymentSafety.state(listOf(node("Payment status"))))
        assertNull(PaymentSafety.state(listOf(node("Travel prices"))))
        assertNull(PaymentSafety.state(listOf(node("Payment options"),node("Help"))))
    }
}
