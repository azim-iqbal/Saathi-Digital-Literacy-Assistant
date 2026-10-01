package com.saathi.ui.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs

internal val IndicatorPosition = SemanticsPropertyKey<Float>("IndicatorPosition")
internal var SemanticsPropertyReceiver.indicatorPosition by IndicatorPosition
internal val GlassRendering = SemanticsPropertyKey<String>("GlassRendering")
internal var SemanticsPropertyReceiver.glassRendering by GlassRendering

data class NavigationItem(val key: String, val label: String, val glyph: NavigationGlyph)

/** No navigation state is owned here. A cancelled spring continues from its current position/velocity. */
@Composable
fun GlassNavigation(
    items: List<NavigationItem>, selectedIndex: Int, reduceMotion: Boolean,
    backdrop: GraphicsLayer?, backdropOrigin: Offset, dark: Boolean,
    onSelect: (Int) -> Unit, modifier: Modifier = Modifier
) {
    val position = remember { Animatable(selectedIndex.toFloat()) }
    LaunchedEffect(selectedIndex, reduceMotion) {
        if (reduceMotion) position.snapTo(selectedIndex.toFloat())
        else position.animateTo(selectedIndex.toFloat(), spring(dampingRatio = NavigationMetrics.DockDamping, stiffness = NavigationMetrics.DockStiffness))
    }
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(50)
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    var origin by remember { mutableStateOf(Offset.Zero) }
    val blurRadius = with(LocalDensity.current) { NavigationMetrics.BlurRadius.toPx() }
    val blur = remember(blurRadius) { BlurEffect(blurRadius, blurRadius, TileMode.Clamp) }
    val shell = if (dark) colors.surface else Color.White
    val edge = if (dark) Color.White.copy(alpha = .24f) else Color.White.copy(alpha = .95f)
    Box(modifier.testTag("navigation-dock")
        .semantics { indicatorPosition = position.value; glassRendering = if (backdrop != null) "blur" else "opaque" }
        .onGloballyPositioned { origin = it.positionInRoot() }
        .shadow(12.dp, shape, clip = false, ambientColor = Color.Black.copy(alpha = .15f), spotColor = Color.Black.copy(alpha = .18f))
        .clip(shape)) {
        // Only Saathi's own content layer is sampled. Text/icons are drawn afterward, without blur.
        if (backdrop != null) Box(Modifier.matchParentSize().graphicsLayer { renderEffect = blur }.drawWithContent {
            val relative = origin - backdropOrigin
            translate(-relative.x, -relative.y) { drawLayer(backdrop) }
        })
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(
            shell.copy(alpha = if (backdrop == null) 1f else .93f),
            shell.copy(alpha = if (backdrop == null) 1f else .82f)
        ))).border(1.dp, Brush.verticalGradient(listOf(edge, colors.outlineVariant.copy(alpha = .65f))), shape))
        Box(Modifier.padding(NavigationMetrics.DockInset)) {
            Box(Modifier.matchParentSize().drawWithCache {
                val inset = 1.dp.toPx()
                val cell = size.width / items.size
                val base = cell - 2 * inset
                val pill = Brush.verticalGradient(listOf(colors.primaryContainer.copy(alpha = .96f), colors.primaryContainer.copy(alpha = .80f)))
                val rim = Brush.verticalGradient(listOf(edge, colors.primary.copy(alpha = .10f)))
                onDrawBehind {
                    val travel = position.value.coerceIn(0f, items.lastIndex.toFloat())
                    val physical = if (rtl) items.lastIndex - travel else travel
                    val stretch = if (reduceMotion) 0f else (abs(position.velocity) / 22f).coerceIn(0f, 1f)
                    val width = base * (1f + NavigationMetrics.MaxStretch * stretch)
                    val height = (size.height - 2 * inset) * (1f - .035f * stretch)
                    val x = (cell * physical + inset - (width - base) / 2).coerceIn(inset, (size.width - width - inset).coerceAtLeast(inset))
                    val y = (size.height - height) / 2
                    val radius = CornerRadius(height / 2)
                    drawRoundRect(pill, Offset(x, y), Size(width, height), radius)
                    drawRoundRect(rim, Offset(x, y), Size(width, height), radius, style = Stroke(1.dp.toPx()))
                }
            })
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).selectableGroup()) {
                items.forEachIndexed { index, item ->
                    val selected = selectedIndex == index
                    val tint = if (selected) colors.primary else colors.onSurfaceVariant
                    Column(Modifier.weight(1f).fillMaxHeight().heightIn(min = NavigationMetrics.DockMinHeight)
                        .clip(shape).testTag("nav-${item.key}")
                        .selectable(selected, role = Role.Tab, onClick = { if (!selected) onSelect(index) })
                        .padding(horizontal = 4.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        NavigationIcon(item.glyph, tint)
                        Text(item.label, color = tint, textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
                    }
                }
            }
        }
    }
}
