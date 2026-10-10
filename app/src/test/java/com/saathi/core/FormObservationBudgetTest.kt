package com.saathi.core

import org.junit.Assert.*
import org.junit.Test

class FormObservationBudgetTest {
    @Test fun fallbackCannotReplenishItselfAndStopsAtEight() {
        val budget = FormObservationBudget()
        assertFalse(budget.take(true))
        budget.onExternalEvent()
        repeat(8) { assertTrue(budget.take(true)) }
        repeat(100) { assertFalse(budget.take(true)) }
    }
    @Test fun PrivatePauseStopOrMissingScreenClosesWindowUntilNewEvent() {
        val budget = FormObservationBudget()
        budget.onExternalEvent()
        assertTrue(budget.take(true))
        assertFalse(budget.pending(false))
        assertFalse(budget.take(true))
        budget.onExternalEvent()
        repeat(6) { assertTrue(budget.take(true)) }
        budget.onExternalEvent() // Replace the window; do not add to its remaining budget.
        repeat(8) { assertTrue(budget.take(true)) }
        assertFalse(budget.take(true))
    }
}
