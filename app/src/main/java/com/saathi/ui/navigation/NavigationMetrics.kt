package com.saathi.ui.navigation

import androidx.compose.ui.unit.dp

/** Shared navigation dimensions; mirrored in the Figma navigation tokens. */
internal object NavigationMetrics {
    val DockInset = 6.dp
    val DockMinHeight = 72.dp
    val BlurRadius = 14.dp
    val TabMinWidth = 132.dp
    val TabMinHeight = 112.dp
    const val DockDamping = .86f
    const val DockStiffness = 650f
    const val MaxStretch = .12f
}
