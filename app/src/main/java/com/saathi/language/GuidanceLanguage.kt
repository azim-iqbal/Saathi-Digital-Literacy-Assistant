package com.saathi.language

import java.util.Locale

enum class GuidanceLanguage(val storageValue: String, val apiTag: String) {
    ENGLISH("english", "en-IN"),
    HINDI("hindi", "hi-IN"),
    HINGLISH("hinglish", "hinglish");

    val sttTag: String get() = if (this == ENGLISH) "en-IN" else "hi-IN"
    val ttsLocale: Locale get() = Locale.Builder().setLanguage(if (this == ENGLISH) "en" else "hi").setRegion("IN").build()

    companion object {
        fun fromStorage(value: String?) = entries.firstOrNull { it.storageValue == value } ?: ENGLISH
        fun fromApiTag(value: String) = when (value.lowercase()) {
            "hi-in", "hi", "hindi" -> HINDI
            "hinglish" -> HINGLISH
            else -> ENGLISH
        }
        fun initialForSystemLocale(locale: Locale = Locale.getDefault()) = if (locale.language == "hi") HINDI else ENGLISH
    }
}
