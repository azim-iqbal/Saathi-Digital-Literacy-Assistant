package com.saathi.ui.glass

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.widget.Button

/** Opaque frosted fallback for the legacy native practice surface (no backdrop capture). */
internal fun Button.applySaathiGlass() {
    val density = resources.displayMetrics.density
    fun material(top: Int, bottom: Int, stroke: Int, width: Int = 1) = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(top, bottom)
    ).apply { cornerRadius = 999 * density; setStroke((width * density).toInt(), stroke) }
    val states = StateListDrawable().apply {
        addState(intArrayOf(-android.R.attr.state_enabled), material(0xFFE4E9E3.toInt(), 0xFFD5DED3.toInt(), Color.WHITE))
        addState(intArrayOf(android.R.attr.state_focused), material(0xFF166D2A.toInt(), GlassTokens.Forest.toInt(), GlassTokens.Mint.toInt(), 3))
        addState(intArrayOf(), material(0xFF237A36.toInt(), GlassTokens.Forest.toInt(), 0xFF94BE9A.toInt()))
    }
    backgroundTintList = null
    background = RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), states, material(Color.WHITE, Color.WHITE, Color.WHITE))
    setTextColor(ColorStateList(arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()), intArrayOf(0xFF697167.toInt(), Color.WHITE)))
    isAllCaps = false
    minHeight = (64 * density).toInt()
    setPadding((24 * density).toInt(), (16 * density).toInt(), (24 * density).toInt(), (16 * density).toInt())
    textSize = 17f
    elevation = 2 * density
}
