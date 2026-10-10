package com.saathi.core

/** Content from a different window cannot mutate the observed root. Window/focus
 * transitions and unknown roots still invalidate immediately; IDs are never cached.
 */
object ObservationEventPolicy {
    fun relevant(type: Int, eventWindow: Int, currentRootWindow: Int?): Boolean {
        val contentOnly = type in setOf(1, 8, 16, 2048, 4096, 8192) // clicked, text, content, scrolled
        return !contentOnly || eventWindow < 0 || currentRootWindow == null || currentRootWindow < 0 || eventWindow == currentRootWindow
    }
}
