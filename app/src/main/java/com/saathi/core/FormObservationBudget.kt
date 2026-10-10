package com.saathi.core

/** One finite settling window per relevant external event. Main-thread owned. */
class FormObservationBudget {
    private var remaining = 0
    fun onExternalEvent() { remaining = 8 }
    fun pending(eligible: Boolean): Boolean {
        if (!eligible) remaining = 0
        return remaining > 0
    }
    fun take(eligible: Boolean): Boolean {
        if (!pending(eligible)) return false
        remaining--
        return true
    }
}
