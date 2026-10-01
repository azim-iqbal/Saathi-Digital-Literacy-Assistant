package com.saathi.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** One palette for the existing app and its assistant entry points. */
internal fun saathiColorScheme(dark: Boolean) = if (dark) darkColorScheme(
        primary = Color(0xFFA4EE99), onPrimary = Color(0xFF131513),
        background = Color(0xFF131513), surface = Color(0xFF202420),
        onBackground = Color(0xFFF6F6F6), onSurface = Color(0xFFF6F6F6),
        onSurfaceVariant = Color(0xFFB8C3B6), outlineVariant = Color(0xFF3E493C),
        primaryContainer = Color(0xFF253D22), onPrimaryContainer = Color(0xFFF6F6F6)
    ) else lightColorScheme(
        primary = Color(0xFF087900), onPrimary = Color.White,
        background = Color.White, surface = Color.White,
        onBackground = Color(0xFF171A17), onSurface = Color(0xFF171A17),
        onSurfaceVariant = Color(0xFF596259), outlineVariant = Color(0xFFDCE3DA),
        primaryContainer = Color(0xFFD7FFD4), onPrimaryContainer = Color(0xFF171A17)
    )
