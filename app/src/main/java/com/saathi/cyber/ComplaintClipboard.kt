package com.saathi.cyber

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.PersistableBundle
import android.os.SystemClock

/** Explicit per-field write only. Never inspect the existing clipboard. */
object ComplaintClipboard {
    fun copy(context: Context, field: ComplaintField): Boolean = runCatching {
        require(field.text.isNotBlank() && field.text.length <= 7000)
        val clip = ClipData.newPlainText(field.title, field.text)
        clip.description.extras = PersistableBundle().apply { putBoolean("android.content.extra.IS_SENSITIVE", true) }
        context.getSystemService(ClipboardManager::class.java).setPrimaryClip(clip)
    }.isSuccess
}

/** Process-memory handoff, never Intent extras, saved state, disk, analytics or cloud. */
object ComplaintHelperHandoff {
    private var fields: List<ComplaintField> = emptyList()
    private var expires = 0L
    @Synchronized fun prepare(value: List<ComplaintField>) {
        require(value.size in 1..3 && value.all { it.text.isNotBlank() && it.text.length <= 7000 })
        fields = value.toList(); expires = SystemClock.elapsedRealtime() + 120_000
    }
    @Synchronized fun take(): List<ComplaintField> {
        val value = if (SystemClock.elapsedRealtime() < expires) fields else emptyList()
        clear(); return value
    }
    @Synchronized fun clear() { fields = emptyList(); expires = 0 }
}
