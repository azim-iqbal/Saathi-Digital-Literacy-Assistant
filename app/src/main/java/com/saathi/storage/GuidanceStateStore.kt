package com.saathi.storage

import android.content.Context
import com.saathi.intake.TaskBrief
import com.saathi.language.GuidanceLanguage

/** Resume is an explicit user action; process death discards the task and its goal. */
class GuidanceStateStore(context: Context) {
    init { context.getSharedPreferences("saathi_guidance_state", Context.MODE_PRIVATE).edit().clear().apply() }
    @Suppress("UNUSED_PARAMETER")
    fun save(brief: TaskBrief, language: GuidanceLanguage, voiceEnabled: Boolean) { pending = brief }
    fun restore(): TaskBrief? = pending
    fun clear() { pending = null }
    private companion object { var pending: TaskBrief? = null }
}
