package com.saathi.ui.navigation

import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.saathi.language.GuidanceLanguage
import com.saathi.ui.glass.GlassButton
import com.saathi.ui.Copy
import com.saathi.ui.text
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.max

internal data class PracticeCategory(val key: String, val copy: Copy, val goal: String, val glyph: NavigationGlyph)
internal val practiceCategories = listOf(
    PracticeCategory("electricity", Copy.ELECTRICITY, "Pay my electricity bill", NavigationGlyph.Electricity),
    PracticeCategory("water", Copy.WATER, "Pay my water bill", NavigationGlyph.Water),
    PracticeCategory("television", Copy.DTH, "Recharge my DTH", NavigationGlyph.Television)
)

/** Selected shoulders blend into the content surface. Normalized coordinates adapt to measured size. */
internal fun sculptedTabPath(width: Float, height: Float): Path = Path().apply {
    moveTo(0f, height)
    cubicTo(width * .13f, height, width * .10f, height * .72f, width * .15f, height * .29f)
    cubicTo(width * .17f, height * .08f, width * .27f, 0f, width * .39f, 0f)
    lineTo(width * .61f, 0f)
    cubicTo(width * .73f, 0f, width * .83f, height * .08f, width * .85f, height * .29f)
    cubicTo(width * .90f, height * .72f, width * .87f, height, width, height)
    close()
}

@Composable
internal fun PracticeNavigation(
    language: GuidanceLanguage, selectedCategory: Int, onCategorySelected: (Int) -> Unit,
    reduceMotion: Boolean, onChoose: (String) -> Unit
) {
    val pager = rememberPagerState(initialPage = selectedCategory) { practiceCategories.size }
    val scope = rememberCoroutineScope()
    var selectionJob by remember { mutableStateOf<Job?>(null) }
    val latestSelection by rememberUpdatedState(onCategorySelected)
    LaunchedEffect(pager) { snapshotFlow { pager.settledPage }.collect { latestSelection(it) } }
    val progress by remember { derivedStateOf { pager.currentPage + pager.currentPageOffsetFraction } }
    val colors = MaterialTheme.colorScheme
    val scroll = rememberScrollState()
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        // At larger fonts the strip scrolls instead of compressing Hindi or removing labels.
        val tabWidth = maxOf(maxWidth / 3, NavigationMetrics.TabMinWidth * max(1f, density.fontScale * .8f))
        LaunchedEffect(pager.currentPage, tabWidth, maxWidth, reduceMotion) {
            val target = with(density) { (tabWidth * pager.currentPage - (maxWidth - tabWidth) / 2).roundToPx() }.coerceIn(0, scroll.maxValue)
            if (reduceMotion) scroll.scrollTo(target) else scroll.animateScrollTo(target, spring(stiffness = 650f, dampingRatio = 1f))
        }
        Column {
            Box(Modifier.fillMaxWidth().horizontalScroll(scroll).testTag("practice-tabs")
                .semantics { indicatorPosition = progress }) {
                Row(Modifier.width(tabWidth * practiceCategories.size).height(IntrinsicSize.Min)
                    .selectableGroup().drawWithCache {
                        val width = size.width / practiceCategories.size
                        val path = sculptedTabPath(width, size.height)
                        val inactive = Brush.verticalGradient(listOf(colors.surface, colors.primaryContainer.copy(alpha = .30f)))
                        val active = Brush.verticalGradient(listOf(lerp(colors.surface, colors.primaryContainer, .65f), colors.primaryContainer))
                        val rim = Brush.verticalGradient(listOf(colors.primary.copy(alpha = .35f), colors.outlineVariant.copy(alpha = .18f)))
                        onDrawBehind {
                            repeat(practiceCategories.size) { index -> translate(index * width, 0f) {
                                drawPath(path, inactive); drawPath(path, rim, style = Stroke(1.dp.toPx()))
                            } }
                            val offset = (if (rtl) practiceCategories.lastIndex - progress else progress).coerceIn(0f, practiceCategories.lastIndex.toFloat()) * width
                            translate(offset, 0f) { drawPath(path, active); drawPath(path, rim, style = Stroke(1.dp.toPx())) }
                            drawRect(colors.primaryContainer, Offset(0f, size.height - 2.dp.toPx()), Size(size.width, 2.dp.toPx()))
                        }
                    }) {
                    practiceCategories.forEachIndexed { index, category ->
                        val selected = pager.currentPage == index
                        Column(Modifier.width(tabWidth).fillMaxHeight().heightIn(min = NavigationMetrics.TabMinHeight)
                            .testTag("category-${category.key}").clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                            .selectable(selected, role = Role.Tab, onClick = {
                                selectionJob?.cancel()
                                selectionJob = scope.launch {
                                    if (reduceMotion) pager.scrollToPage(index)
                                    else pager.animateScrollToPage(index, animationSpec = spring(dampingRatio = .92f, stiffness = 550f))
                                }
                            }).padding(horizontal = 14.dp, vertical = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            val tint = if (selected) colors.primary else colors.onSurfaceVariant
                            NavigationIcon(category.glyph, tint, Modifier.size(30.dp))
                            Text(category.copy.text(language), color = tint, textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                        }
                    }
                }
            }
            HorizontalPager(state = pager, modifier = Modifier.fillMaxWidth().testTag("practice-pager")
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)).background(colors.primaryContainer),
                verticalAlignment = Alignment.Top) { page ->
                val category = practiceCategories[page]
                Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(category.copy.text(language), style = MaterialTheme.typography.titleLarge, color = colors.onPrimaryContainer)
                    Text(Copy.SAFETY.text(language), style = MaterialTheme.typography.bodyLarge, color = colors.onPrimaryContainer)
                    GlassButton(Copy.REVIEW.text(language), onClick = { onChoose(category.goal) }, arrow = true,
                        modifier = Modifier.fillMaxWidth().testTag("choose-${category.key}"))
                }
            }
        }
    }
}
