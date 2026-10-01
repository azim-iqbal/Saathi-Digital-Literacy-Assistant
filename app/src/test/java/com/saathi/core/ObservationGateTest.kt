package com.saathi.core

import org.junit.Assert.*
import org.junit.Test

class ObservationGateTest {
    @Test fun `stop rejects a delayed tree and clears presentation identity`() {
        val gate = ObservationGate(); gate.start()
        val ticket = gate.observe("practice", 1)!!
        gate.stop()
        assertFalse(gate.accepts(ticket)); assertNull(gate.currentKey()); assertNull(gate.observe("practice", 1))
    }
    @Test fun `restarting on identical screen cannot reuse old result`() {
        val gate = ObservationGate(); gate.start()
        val old = gate.observe("practice", 1)!!
        gate.stop(); gate.start()
        val fresh = gate.observe("practice", 1)!!
        assertFalse(gate.accepts(old)); assertTrue(gate.accepts(fresh))
    }
    @Test fun `window and package changes invalidate previous work`() {
        val gate = ObservationGate(); gate.start()
        val first = gate.observe("first", 1)!!
        val window = gate.observe("first", 2)!!
        assertFalse(gate.accepts(first))
        val other = gate.observe("second", 2)!!
        assertFalse(gate.accepts(window)); assertTrue(gate.accepts(other))
    }
    @Test fun `movement invalidates before replacement snapshot finishes`() {
        val gate = ObservationGate(); gate.start()
        val old = gate.observe("practice", 1)!!
        gate.invalidate()
        assertFalse(gate.accepts(old)); assertNull(gate.currentKey())
    }
    @Test fun `latest revision wins even when old worker completes last`() {
        val gate = ObservationGate(); gate.start()
        val old = gate.observe("practice", 1)!!
        val fresh = gate.observe("practice", 1)!!
        assertTrue(gate.accepts(fresh)); assertFalse(gate.accepts(old))
    }
    @Test fun `overlay failure can only stop its current presentation`() {
        val gate = ObservationGate(); gate.start()
        gate.observe("practice", 1)
        val first = gate.currentKey()
        assertTrue(gate.matchesPresentation(first))
        gate.invalidate()
        assertFalse(gate.matchesPresentation(first))
        assertFalse(gate.matchesPresentation(null))
        gate.observe("practice", 1)
        val replacement = gate.currentKey()
        assertFalse(gate.matchesPresentation(first))
        assertTrue(gate.matchesPresentation(replacement))
        gate.stop(); gate.start(); gate.observe("practice", 1)
        assertFalse(gate.matchesPresentation(replacement))
        assertTrue(gate.matchesPresentation(gate.currentKey()))
    }

    @Test fun `notification identity survives screen changes but never a session restart`() {
        val gate = ObservationGate()
        assertFalse(gate.matchesSession(null))
        gate.start()
        val first = gate.sessionKey()
        assertTrue(gate.matchesSession(first))
        gate.observe("practice", 1); gate.invalidate(); gate.observe("practice", 2)
        assertTrue(gate.matchesSession(first))
        gate.stop()
        assertFalse(gate.matchesSession(first))
        gate.start()
        assertFalse(gate.matchesSession(first))
        assertTrue(gate.matchesSession(gate.sessionKey()))
    }
}
