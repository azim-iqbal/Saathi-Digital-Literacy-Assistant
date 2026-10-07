package com.saathi.core

/** Public identifiers from the synthetic fixture only. Never labels, input IDs or third-party data. */
object PracticeGatewayContract {
    val actionIds = setOf("recharge_bills", "electricity_biller", "water_biller", "dth_biller", "practice_back", "pay_button")
        .map { "com.saathi:id/$it" }.toSet()
    val categoryIds = setOf("practice_electricity", "practice_water", "practice_dth").map { "com.saathi:id/$it" }.toSet()
    val publicIds = actionIds + categoryIds + setOf("com.saathi:id/practice_detour", "com.saathi:id/success_title")
    fun marker(task: SanitizedTaskCategory) = "com.saathi:id/practice_" + when (task) {
        SanitizedTaskCategory.ELECTRICITY_BILL -> "electricity"
        SanitizedTaskCategory.WATER_BILL -> "water"
        SanitizedTaskCategory.DTH_RECHARGE -> "dth"
    }
}

sealed interface GatewayResult {
    data class Accepted(val proposal: GuidanceProposal) : GatewayResult
    data class Assessed(val assessment: IncidentAssessment) : GatewayResult
    data class Connection(val report: String) : GatewayResult
    data class Research(val requestId: String, val report: String, val hasEvidence: Boolean, val plan: EvidencePlan? = null) : GatewayResult
    data class Rejected(val reason: String) : GatewayResult
}

fun interface GatewayCancellation { fun cancel() }
