package com.saathi.core

/**
 * The pilot may guide only the app-owned synthetic Bill Pay fixture. A familiar-looking control
 * in another app is never sufficient to make that screen eligible.
 */
object PracticeSurfacePolicy {
    private val supportedIds = setOf(
        "recharge_bills",
        "electricity_biller",
        "water_biller",
        "dth_biller",
        "account_input",
        "amount_input",
        "pin_input",
        "pay_button",
        "success_title", "practice_detour", "practice_back"
    )

    fun isEligible(packageName: String, appPackageName: String, nodes: List<UiNode>): Boolean {
        if (packageName != appPackageName) return false
        val prefix = "$appPackageName:id/"
        return nodes.any { node ->
            node.resourceId?.takeIf { it.startsWith(prefix) }?.removePrefix(prefix) in supportedIds
        }
    }
}
