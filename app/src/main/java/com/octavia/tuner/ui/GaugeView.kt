package com.octavia.tuner.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import com.octavia.tuner.core.NoteMapper
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Semicircular gauge. Left = flat, right = sharp, centre = in tune.
 * ±100 % (= ±50 cents) maps to ±SWEEP degrees.
 */
@Composable
fun GaugeView(percent: Double, isActive: Boolean, tint: Color, modifier: Modifier = Modifier) {
    val sweep = 58f
    val targetAngle = (percent.coerceIn(-100.0, 100.0) / 100.0 * sweep).toFloat()
    val angle by animateFloatAsState(targetAngle, spring(dampingRatio = 0.72f, stiffness = 500f), label = "needle")
    val needleAlpha by animateFloatAsState(if (isActive) 1f else 0.3f, label = "needleAlpha")

    val measurer = rememberTextMeasurer()
    val onSurface = MaterialTheme.colorScheme.onSurface
    val secondary = onSurface.copy(alpha = 0.6f)
    val rail = onSurface.copy(alpha = 0.08f)
    val bg = MaterialTheme.colorScheme.background
    val green = TunerPalette.color(0.0)

    Canvas(modifier.fillMaxWidth().aspectRatio(1.7f)) {
        val w = size.width
        val h = size.height
        val pivot = Offset(w / 2, h * 0.92f)
        val radius = min(w * 0.47f, h * 0.86f)

        fun point(deg: Float, r: Float): Offset {
            val a = Math.toRadians(deg.toDouble())
            return Offset(pivot.x + r * sin(a).toFloat(), pivot.y - r * cos(a).toFloat())
        }
        fun arc(r: Float, from: Float, to: Float): Path {
            val p = Path()
            var d = from
            var first = true
            while (d <= to + 0.001f) {
                val pt = point(d, r)
                if (first) { p.moveTo(pt.x, pt.y); first = false } else p.lineTo(pt.x, pt.y)
                d += 1f
            }
            return p
        }
        fun label(text: String, at: Offset, sizePx: Float) {
            val result = measurer.measure(
                text, TextStyle(fontSize = sizePx.toSp(), color = secondary, fontWeight = FontWeight.Medium)
            )
            drawText(result, topLeft = Offset(at.x - result.size.width / 2f, at.y - result.size.height / 2f))
        }

        // Faint rail behind the ticks.
        drawPath(arc(radius * 0.74f, -sweep, sweep), rail,
            style = Stroke(width = radius * 0.02f, cap = StrokeCap.Round))

        // Highlighted centre "in tune" zone.
        val zone = (NoteMapper.inTunePercent / 100 * sweep).toFloat()
        val inTune = isActive && abs(percent) <= NoteMapper.inTunePercent
        drawPath(arc(radius * 0.74f, -zone, zone), green.copy(alpha = if (inTune) 0.95f else 0.4f),
            style = Stroke(width = radius * 0.03f, cap = StrokeCap.Round))

        // Ticks every 5 %, major every 25 %; ticks between the centre and the needle light up.
        for (p in -100..100 step 5) {
            val major = p % 25 == 0
            val deg = p / 100f * sweep
            val lit = isActive && (if (p >= 0) p <= percent else p >= percent)
            val color = TunerPalette.color(abs(p) / 100.0 * NoteMapper.TOLERANCE_CENTS)
            drawLine(
                color.copy(alpha = if (lit) 1f else 0.28f),
                start = point(deg, radius * 0.84f),
                end = point(deg, radius * if (major) 1.0f else 0.93f),
                strokeWidth = if (major) radius * 0.014f else radius * 0.007f,
                cap = StrokeCap.Round,
            )
        }

        label("♭", point(-sweep, radius * 1.1f), radius * 0.1f)
        label("♯", point(sweep, radius * 1.1f), radius * 0.1f)
        label("0", point(0f, radius * 1.1f), radius * 0.07f)

        // Needle: drawn pointing straight up, rotated about the pivot.
        rotate(angle, pivot) {
            val needle = Path().apply {
                moveTo(pivot.x, pivot.y - radius * 0.9f)
                lineTo(pivot.x + radius * 0.02f, pivot.y)
                lineTo(pivot.x - radius * 0.02f, pivot.y)
                close()
            }
            drawPath(needle, tint.copy(alpha = needleAlpha))
            drawCircle(tint.copy(alpha = needleAlpha), radius * 0.055f, pivot)
            drawCircle(bg, radius * 0.0225f, pivot)
        }
    }
}
