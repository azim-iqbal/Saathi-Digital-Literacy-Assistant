package com.saathi.core

/** Main-thread-owned identity. An old tree must never become current after Stop or navigation. */
class ObservationGate {
    data class Ticket(val session: Long, val revision: Long, val packageName: String, val windowId: Int)
    // Pending notification intents may outlive process death. Do not reuse "session 1" on restart.
    private var session = java.util.UUID.randomUUID().mostSignificantBits
    private var revision = 0L
    private var active = false
    private var current: Ticket? = null

    fun start() { session++; active = true; invalidate() }
    fun stop() { active = false; invalidate() }
    fun invalidate() { revision++; current = null }
    fun observe(packageName: String, windowId: Int): Ticket? {
        if (!active) return null
        invalidate()
        return Ticket(session, revision, packageName, windowId).also { current = it }
    }
    fun sessionKey(): String? = if (active) session.toString() else null
    fun matchesSession(key: String?): Boolean = key != null && key == sessionKey()
    fun currentKey(): String? = if (active) current?.toString() else null
    fun matchesPresentation(key: String?): Boolean = key != null && key == currentKey()
    fun accepts(ticket: Ticket): Boolean = active && current == ticket
}
