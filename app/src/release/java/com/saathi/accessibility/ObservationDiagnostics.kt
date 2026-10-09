package com.saathi.accessibility

/** Test observer only: no labels, text, credentials or default storage. */
internal object ObservationDiagnostics {
    fun gatewayBeforeCleanup() = Unit
    fun observationFailure(reason: String) = Unit
    fun gatewayResult(reason: String) = Unit
    fun snapshot(elapsedMs: Long) = Unit
    fun event(type: Int, window: Int, changes: Int, ownOverlay: Boolean) = Unit
}
