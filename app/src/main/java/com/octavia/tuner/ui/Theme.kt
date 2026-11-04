package com.octavia.tuner.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import kotlin.math.abs

private val DarkColors = darkColorScheme(
    primary = Color(0xFF0A84FF),
    background = Color(0xFF1C1C1E),
    surface = Color(0xFF3A3A3C),
    onBackground = Color.White,
    onSurface = Color.White,
)
private val LightColors = lightColorScheme(
    primary = Color(0xFF007AFF),
    background = Color(0xFFF2F2F7),
    surface = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black,
)

@Composable
fun TunerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors, content = content)
}

/** The theme's text colour at a given opacity — used for subtle fills and secondary text. */
@Composable
fun faint(alpha: Float): Color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)

object TunerPalette {
    /**
     * Red → amber → green as the pitch approaches centre.
     * Green inside the in-tune zone (≤5¢), amber around 22¢, red from 40¢ outward.
     */
    fun color(absCents: Double): Color {
        fun lerp(a: Double, b: Double, t: Double) = a + (b - a) * t
        val c = abs(absCents)
        val hue = when {
            c <= 5 -> 0.36
            c <= 22 -> lerp(0.36, 0.11, (c - 5) / 17)
            c <= 40 -> lerp(0.11, 0.0, (c - 22) / 18)
            else -> 0.0
        }
        return Color.hsv((hue * 360).toFloat(), 0.82f, 0.93f)
    }
}
