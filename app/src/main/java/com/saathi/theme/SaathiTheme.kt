package com.saathi.theme

import android.content.Context
import android.content.res.Configuration
import com.saathi.R

object SaathiTheme {
    private const val PREFS = "saathi_theme"; private const val KEY = "mode"
    enum class Preference { SYSTEM, LIGHT, DARK }
    fun isDark(context: Context): Boolean = when (runCatching { Preference.valueOf(context.getSharedPreferences(PREFS, 0).getString(KEY, "SYSTEM")!!) }.getOrDefault(Preference.SYSTEM)) {
        Preference.DARK -> true; Preference.LIGHT -> false
        Preference.SYSTEM -> context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }
    fun toggle(context: Context) {
        val next = if (isDark(context)) Preference.LIGHT else Preference.DARK
        context.getSharedPreferences(PREFS, 0).edit().putString(KEY, next.name).apply()
    }
    fun color(context: Context, resource: Int): Int {
        val dark = isDark(context)
        val colors = if (dark) mapOf(
            R.color.surface to 0xFF0E1614.toInt(), R.color.surface_elevated to 0xFF16211D.toInt(), R.color.primary to 0xFF4FD8B0.toInt(), R.color.primary_container to 0xFF1B3A33.toInt(),
            R.color.text_primary to 0xFFEAF3EF.toInt(), R.color.text_secondary to 0xFF9FB8AF.toInt(), R.color.border to 0xFF26433B.toInt(), R.color.border_subtle to 0xFF1E2C27.toInt(),
            R.color.chat_user_bg to 0xFF145C4C.toInt(), R.color.chat_user_text to 0xFFF2FBF8.toInt(), R.color.chat_assistant_bg to 0xFF16211D.toInt(), R.color.chat_assistant_text to 0xFFEAF3EF.toInt()
        ) else mapOf(
            R.color.surface to 0xFFF7F8F4.toInt(), R.color.surface_elevated to 0xFFFFFFFF.toInt(), R.color.primary to 0xFF0B6B5A.toInt(), R.color.primary_container to 0xFFDDF1EB.toInt(),
            R.color.text_primary to 0xFF13251F.toInt(), R.color.text_secondary to 0xFF51625C.toInt(), R.color.border to 0xFFC7DED6.toInt(), R.color.border_subtle to 0xFFE1EAE5.toInt(),
            R.color.chat_user_bg to 0xFF0B6B5A.toInt(), R.color.chat_user_text to 0xFFFFFFFF.toInt(), R.color.chat_assistant_bg to 0xFFFFFFFF.toInt(), R.color.chat_assistant_text to 0xFF13251F.toInt()
        )
        return colors[resource] ?: context.resources.getColor(resource, context.theme)
    }
}
