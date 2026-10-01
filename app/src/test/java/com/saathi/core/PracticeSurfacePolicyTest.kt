package com.saathi.core

import android.graphics.Rect
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeSurfacePolicyTest {
    @Test fun `all supported synthetic bill pay controls are eligible`() {
        listOf("recharge_bills", "electricity_biller", "water_biller", "dth_biller", "account_input", "amount_input", "pin_input", "pay_button", "success_title").forEach { id ->
            assertTrue(PracticeSurfacePolicy.isEligible("com.saathi", "com.saathi", listOf(node(id))))
        }
    }

    @Test fun `matching ids in another app never enable guidance`() {
        assertFalse(PracticeSurfacePolicy.isEligible("com.example.copy", "com.saathi", listOf(node("pay_button"))))
    }

    @Test fun `same package screens without a registered practice control remain ineligible`() {
        assertFalse(PracticeSurfacePolicy.isEligible("com.saathi", "com.saathi", listOf(node("send_money"))))
    }

    private fun node(id: String) = UiNode(
        bounds = Rect(0, 0, 100, 60), text = null, description = null, hint = null,
        resourceId = "com.saathi:id/$id", className = "android.view.View", isPassword = false,
        isEnabled = true, isClickable = true
    )
}
