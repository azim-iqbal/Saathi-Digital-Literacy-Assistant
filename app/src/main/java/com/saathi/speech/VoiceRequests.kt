package com.saathi.speech

import com.saathi.orchestrator.LiveGuide

/** Explicit option requests only. Ordinary conversation must not silently replace the user's task. */
object VoiceRequests {
    private val prefix = Regex("^(?:please\\s+)?(?:find|show|open|खोजो|दिखाओ|खोलो)\\s+(.+)$", RegexOption.IGNORE_CASE)
    private val suffix = Regex("^(.+?)\\s+(?:खोजो|दिखाओ|खोलो|dhundo|dikhao|kholo)$", RegexOption.IGNORE_CASE)

    fun option(text: String): String? {
        if (text.length > 120 || text.any { it == '\n' || it == '\r' }) return null
        val value = text.trim().trimEnd('.', '!', '?', '।').trim()
        val requested = (prefix.matchEntire(value) ?: suffix.matchEntire(value))?.groupValues?.get(1) ?: return null
        return LiveGuide.label(requested)
    }
}
