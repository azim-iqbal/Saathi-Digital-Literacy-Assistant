package com.saathi.ui.glass

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.saathi.ui.navigation.glassRendering

/** Shared material and interaction dimensions. Native practice uses the same palette. */
object GlassTokens {
    const val Forest = 0xFF075E19
    const val Mint = 0xFFA4EE99
    const val TransitionMillis = 220
    val Blur = 18.dp
    val Pill = RoundedCornerShape(50)
    val Card = RoundedCornerShape(28.dp)
    val Input = RoundedCornerShape(24.dp)
}

internal data class GlassEnvironment(
    val backdrop: GraphicsLayer? = null, val origin: Offset = Offset.Zero,
    val dark: Boolean = false, val reducedMotion: Boolean = true
)
internal val LocalGlass = staticCompositionLocalOf { GlassEnvironment() }

/** Samples only the decorative background: never itself, labels, forms or sensitive content. */
@Composable
internal fun GlassPanel(
    modifier: Modifier = Modifier, shape: Shape = GlassTokens.Card,
    primary: Boolean = false, focused: Boolean = false, enabled: Boolean = true,
    emphasis: Float = 0f, content: @Composable BoxScope.() -> Unit
) {
    val environment = LocalGlass.current
    val colors = MaterialTheme.colorScheme
    var origin by remember { mutableStateOf(Offset.Zero) }
    val radius = with(LocalDensity.current) { GlassTokens.Blur.toPx() }
    val blur = remember(radius) { BlurEffect(radius, radius, TileMode.Clamp) }
    val base = if (primary && enabled) Color(GlassTokens.Forest) else colors.surface
    val translucent = environment.backdrop != null
    val opacity = if (!translucent) 1f else if (primary && enabled) .88f else if (environment.dark) .70f else .46f
    val top = lerp(base, Color.White, if (environment.dark) .07f else .025f)
    val rim = if (focused) colors.primary else if (environment.dark || primary) Color.White.copy(alpha = .46f) else Color.White
    Box(modifier.onGloballyPositioned { origin = it.positionInRoot() }
        .semantics { glassRendering = if (translucent) "blur" else "opaque" }
        .shadow(if (enabled) 2.dp else 0.dp, shape, clip = false)
        .clip(shape)) {
        if (translucent) Box(Modifier.matchParentSize().graphicsLayer { renderEffect = blur }.drawWithContent {
            val relative = origin - environment.origin
            translate(-relative.x, -relative.y) { drawLayer(environment.backdrop!!) }
        })
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(
            top.copy(alpha = opacity), base.copy(alpha = if (translucent) (opacity - .08f).coerceAtLeast(.6f) else 1f)
        ))).background(Color.White.copy(alpha = emphasis.coerceIn(0f, .08f)))
            .border(if (focused) 2.dp else 1.dp,
                Brush.verticalGradient(listOf(rim, colors.outlineVariant.copy(alpha = .6f), rim.copy(alpha = .22f))), shape))
        content()
    }
}

@Composable
internal fun GlassButton(
    label: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    primary: Boolean = true, enabled: Boolean = true, compact: Boolean = false, arrow: Boolean = false
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val focused by interaction.collectIsFocusedAsState()
    val still = LocalGlass.current.reducedMotion
    val scale by animateFloatAsState(if (pressed && enabled) .975f else 1f, tween(if (still) 0 else GlassTokens.TransitionMillis), label = "glass-press")
    val lift by animateFloatAsState(if (hovered && enabled && !pressed) -1f else 0f, tween(if (still) 0 else GlassTokens.TransitionMillis), label = "glass-hover")
    val colors = MaterialTheme.colorScheme
    val tint = if (!enabled) colors.onSurface.copy(alpha = .46f) else if (primary) Color.White else colors.onSurface
    GlassPanel(modifier.heightIn(min = if (compact) 48.dp else 64.dp)
        .graphicsLayer { scaleX = scale; scaleY = scale; translationY = if (still) 0f else lift.dp.toPx() }
        .clip(GlassTokens.Pill).hoverable(interaction, enabled)
        .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick),
        shape = GlassTokens.Pill, primary = primary, focused = focused, enabled = enabled,
        emphasis = if (hovered && enabled) .06f else 0f) {
        Row(Modifier.width(IntrinsicSize.Max)
            .background(Color.Black.copy(alpha = if (pressed && enabled) .09f else 0f))
            .heightIn(min = if (compact) 48.dp else 64.dp)
            .padding(horizontal = if (compact) 18.dp else 24.dp, vertical = if (compact) 10.dp else 18.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text(label, modifier = Modifier.weight(1f),
                color = tint, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            if (arrow) { Spacer(Modifier.width(16.dp)); GlassChevron(tint) }
        }
    }
}

@Composable
internal fun GlassChevron(color: Color, modifier: Modifier = Modifier) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Canvas(modifier.size(22.dp)) {
        val path = Path().apply {
            moveTo(size.width * .38f, size.height * .23f)
            lineTo(size.width * .65f, size.height * .5f)
            lineTo(size.width * .38f, size.height * .77f)
        }
        if (rtl) withTransform({ scale(-1f, 1f, center) }) { drawPath(path, color, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)) }
        else drawPath(path, color, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
