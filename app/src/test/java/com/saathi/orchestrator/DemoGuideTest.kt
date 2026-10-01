package com.saathi.orchestrator

import android.graphics.Rect
import com.saathi.core.UiNode
import org.junit.Assert.assertEquals
import org.junit.Test

class DemoGuideTest {
    @Test fun wrongCategoryReturnsToChoicesInsteadOfFillingAnotherBill() {
        val step = DemoGuide.next("Pay my water bill",
            listOf(node("practice_electricity"), node("practice_back"), node("account_input")), "en-IN", false)
        assertEquals("practice_back", step.target?.description)
        org.junit.Assert.assertNotNull(step.correctionNote)
        org.junit.Assert.assertFalse(step.goalComplete)
    }
    @Test fun wrongCategorySuccessDoesNotCompleteSelectedTask() {
        val step = DemoGuide.next("Pay my water bill",
            listOf(node("practice_dth"), node("practice_back"), node("success_title")), "en-IN", false)
        assertEquals("practice_back", step.target?.description)
        org.junit.Assert.assertFalse(step.goalComplete)
    }
    @Test fun missingRecoveryTargetNeverThrowsOrInventsCoordinates() {
        val step = DemoGuide.next("Pay my water bill", listOf(node("practice_detour")), "hi-IN", false)
        org.junit.Assert.assertNull(step.target)
        org.junit.Assert.assertNotNull(step.correctionNote)
    }
    @Test fun backFromDetourResolvesTheOriginalCategory() {
        val goal = "Recharge my DTH"
        val detour = DemoGuide.next(goal, listOf(node("practice_detour"), node("practice_back")), "hinglish", false)
        assertEquals("practice_back", detour.target?.description)
        assertEquals("dth_biller", DemoGuide.next(goal, billerScreen(), "hinglish", false).target?.description)
    }
    @Test fun partialBillerTreeHasNoCrashingOrGuessedTarget() {
        val step = DemoGuide.next("Pay my water bill", listOf(node("electricity_biller")), "en-IN", false)
        org.junit.Assert.assertNull(step.target)
    }
    @Test
    fun `water goal highlights the water category`() {
        val step = DemoGuide.next("Pay my water bill", billerScreen(), "en-US", previousFailed = false)

        assertEquals("water_biller", step.target?.description)
    }

    @Test
    fun `dth goal highlights the DTH category`() {
        val step = DemoGuide.next("Recharge my DTH", billerScreen(), "en-US", previousFailed = false)

        assertEquals("dth_biller", step.target?.description)
    }

    @Test
    fun `masked filled account field advances without reading its value`() {
        val nodes = listOf(node("account_input").copy(isSensitive = true, hasValue = true), node("amount_input"))
        val step = DemoGuide.next("Pay bill", nodes, "en-US", false)
        assertEquals("amount_input", step.target?.description)
    }

    @Test
    fun `synthetic success never claims a real payment`() {
        val step = DemoGuide.next("Pay bill", listOf(node("success_title")), "en-US", false)
        assertEquals("Practice complete. No real payment was made.", step.speechText)
    }

    private fun billerScreen() = listOf(
        node("electricity_biller"),
        node("water_biller"),
        node("dth_biller")
    )

    private fun node(id: String) = UiNode(
        bounds = Rect(0, 0, 100, 60),
        text = null,
        description = null,
        hint = null,
        resourceId = "com.saathi:id/$id",
        className = "android.widget.LinearLayout",
        isPassword = false,
        isEnabled = true,
        isClickable = true
    )
}
