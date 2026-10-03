package com.saathi.accessibility

/** Test observer only: no labels, text, credentials or default storage. */
internal object ObservationDiagnostics {
    var observer: ((Int, Int, Int, Boolean) -> Unit)? = null
    fun event(type: Int, window: Int, changes: Int, ownOverlay: Boolean) { observer?.invoke(type, window, changes, ownOverlay) }
}
