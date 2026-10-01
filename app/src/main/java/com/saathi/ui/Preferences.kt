package com.saathi.ui

import android.content.Context
import com.saathi.language.GuidanceLanguage

/** Preference state only: no goals, transcript, microphone or active-session restoration. */
class Preferences(context: Context) {
    private val store = context.getSharedPreferences("saathi_ui", Context.MODE_PRIVATE)
    var welcomed: Boolean
        get() = store.getBoolean("welcomed", false)
        set(value) { store.edit().putBoolean("welcomed", value).apply() }
    var language: GuidanceLanguage
        get() = GuidanceLanguage.fromStorage(store.getString("language", null))
        set(value) { store.edit().putString("language", value.storageValue).apply() }
    var theme: String
        get() = store.getString("theme", "System") ?: "System"
        set(value) { store.edit().putString("theme", value).apply() }
    var speech: Boolean
        get() = store.getBoolean("speech", false)
        set(value) { store.edit().putBoolean("speech", value).apply() }
    var speechRate: Float
        get() = store.getFloat("speech_rate", com.saathi.speech.TtsManager.DEFAULT_SPEECH_RATE)
            .coerceIn(com.saathi.speech.TtsManager.MIN_SPEECH_RATE, com.saathi.speech.TtsManager.MAX_SPEECH_RATE)
        set(value) { store.edit().putFloat("speech_rate", value.coerceIn(com.saathi.speech.TtsManager.MIN_SPEECH_RATE, com.saathi.speech.TtsManager.MAX_SPEECH_RATE)).apply() }
    var reducedMotion: Boolean
        get() = store.getBoolean("reduced_motion", false)
        set(value) { store.edit().putBoolean("reduced_motion", value).apply() }
    var reducedTransparency: Boolean
        get() = store.getBoolean("reduced_transparency", false)
        set(value) { store.edit().putBoolean("reduced_transparency", value).apply() }
    var haptics: Boolean
        get() = store.getBoolean("haptics", true)
        set(value) { store.edit().putBoolean("haptics", value).apply() }
}
