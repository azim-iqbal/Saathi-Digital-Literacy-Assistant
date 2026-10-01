package com.saathi.ui.navigation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp

/** Original 24-unit outline drawings; labels on the containing tabs provide semantics. */
enum class NavigationGlyph { Home, Practice, Settings, Electricity, Water, Television }

@Composable
fun NavigationIcon(glyph: NavigationGlyph, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(26.dp)) {
        val stroke = Stroke(width = 1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        scale(size.width / 24f, size.height / 24f, pivot = androidx.compose.ui.geometry.Offset.Zero) {
            val p = Path()
            when (glyph) {
                NavigationGlyph.Home -> {
                    p.moveTo(3f, 10f); p.lineTo(10f, 3.5f); p.quadraticTo(12f, 2f, 14f, 3.5f)
                    p.lineTo(21f, 10f); p.lineTo(21f, 19f); p.quadraticTo(21f, 21f, 19f, 21f)
                    p.lineTo(15f, 21f); p.lineTo(15f, 14f); p.lineTo(9f, 14f); p.lineTo(9f, 21f)
                    p.lineTo(5f, 21f); p.quadraticTo(3f, 21f, 3f, 19f); p.close()
                }
                NavigationGlyph.Practice -> {
                    p.moveTo(12f, 6f); p.cubicTo(9f, 3f, 5f, 3f, 2.5f, 4f); p.lineTo(2.5f, 19f)
                    p.cubicTo(6f, 18f, 9f, 18f, 12f, 21f); p.cubicTo(15f, 18f, 18f, 18f, 21.5f, 19f)
                    p.lineTo(21.5f, 4f); p.cubicTo(18f, 3f, 15f, 3f, 12f, 6f); p.lineTo(12f, 21f)
                }
                NavigationGlyph.Settings -> {
                    p.moveTo(4f, 6f); p.lineTo(20f, 6f); p.moveTo(4f, 12f); p.lineTo(20f, 12f)
                    p.moveTo(4f, 18f); p.lineTo(20f, 18f)
                    drawPath(p, tint, style = stroke)
                    listOf(8f to 6f, 16f to 12f, 10f to 18f).forEach { (x, y) ->
                        drawCircle(tint, 2.6f, androidx.compose.ui.geometry.Offset(x, y))
                    }
                    return@scale
                }
                NavigationGlyph.Electricity -> {
                    p.moveTo(14f, 2f); p.lineTo(5f, 13f); p.lineTo(11f, 13f); p.lineTo(10f, 22f)
                    p.lineTo(20f, 10f); p.lineTo(13f, 10f); p.close()
                }
                NavigationGlyph.Water -> {
                    p.moveTo(12f, 2f); p.cubicTo(10f, 6f, 5f, 10f, 5f, 14f)
                    p.cubicTo(5f, 24f, 19f, 24f, 19f, 14f); p.cubicTo(19f, 10f, 14f, 6f, 12f, 2f)
                    p.close(); p.moveTo(8f, 15f); p.quadraticTo(8f, 18f, 11f, 18f)
                }
                NavigationGlyph.Television -> {
                    p.moveTo(8f, 2f); p.lineTo(12f, 6f); p.lineTo(16f, 2f)
                    p.moveTo(5f, 6f); p.lineTo(19f, 6f); p.quadraticTo(22f, 6f, 22f, 9f)
                    p.lineTo(22f, 18f); p.quadraticTo(22f, 21f, 19f, 21f); p.lineTo(5f, 21f)
                    p.quadraticTo(2f, 21f, 2f, 18f); p.lineTo(2f, 9f); p.quadraticTo(2f, 6f, 5f, 6f)
                }
            }
            drawPath(p, tint, style = stroke)
        }
    }
}
