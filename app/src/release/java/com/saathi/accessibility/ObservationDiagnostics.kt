package com.saathi.accessibility

/** Test observer only: no labels, text, credentials or default storage. */
internal object ObservationDiagnostics {
    fun event(type: Int, window: Int, changes: Int, ownOverlay: Boolean) = Unit
}
