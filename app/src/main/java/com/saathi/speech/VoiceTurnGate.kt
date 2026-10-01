package com.saathi.speech

/** Main-thread identity for asynchronous audio callbacks. Cancellation never resumes a newer turn. */
class VoiceTurnGate {
    private var revision = 0L
    private var active = false
    fun begin(): Long { active = true; return ++revision }
    fun cancel() { active = false; revision++ }
    fun accepts(token: Long) = active && revision == token
}

enum class VoiceCommand { STOP, PAUSE, REPEAT, HELP, ACKNOWLEDGE, UNKNOWN }

object VoiceCommands {
    fun parse(text: String): VoiceCommand {
        val value = text.trim().lowercase().replace(Regex("[.!?।]+$"), "").trim()
        return when (value) {
            "stop", "cancel", "stop guidance", "रद्द", "बंद करो", "रुको", "band karo", "cancel karo" -> VoiceCommand.STOP
            "pause", "pause guidance", "रोकें", "थोड़ा रुको", "thoda ruko", "pause karo" -> VoiceCommand.PAUSE
            "repeat", "repeat please", "say again", "दोबारा", "फिर बोलो", "phir bolo", "dobara" -> VoiceCommand.REPEAT
            "help", "help me", "what next", "where do i tap", "मदद", "अब क्या", "madad", "ab kya" -> VoiceCommand.HELP
            "okay", "ok", "understood", "done", "yes", "समझ गया", "ठीक है", "हो गया", "samajh gaya", "theek hai", "ho gaya" -> VoiceCommand.ACKNOWLEDGE
            else -> VoiceCommand.UNKNOWN
        }
    }
}
