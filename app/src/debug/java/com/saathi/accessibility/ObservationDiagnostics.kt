package com.saathi.accessibility

/** Test observer only: no labels, text, credentials or default storage. */
internal object ObservationDiagnostics {
    @Volatile var gatewayCleanupObserver: (() -> Unit)? = null
    fun gatewayBeforeCleanup() { gatewayCleanupObserver?.invoke() }
    var failureObserver: ((String) -> Unit)? = null
    fun observationFailure(reason: String) { failureObserver?.invoke(reason.takeIf { it in setOf("Incomplete observation", "Expired observation", "Missing observation branch") } ?: "Observation unavailable") }
    var gatewayObserver: ((String) -> Unit)? = null
    fun gatewayResult(reason: String) { gatewayObserver?.invoke(if (reason == "accepted") reason else com.saathi.core.GatewayRecovery.reason(reason)) }
    var observer: ((Int, Int, Int, Boolean) -> Unit)? = null
    var snapshotObserver: ((Long) -> Unit)? = null
    fun snapshot(elapsedMs: Long) { snapshotObserver?.invoke(elapsedMs) }
    fun event(type: Int, window: Int, changes: Int, ownOverlay: Boolean) { observer?.invoke(type, window, changes, ownOverlay) }
}
