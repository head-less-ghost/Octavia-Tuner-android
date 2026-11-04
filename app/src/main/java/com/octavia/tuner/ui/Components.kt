package com.octavia.tuner.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.octavia.tuner.core.AccidentalStyle
import com.octavia.tuner.core.NoteMapper
import com.octavia.tuner.core.NoteNaming
import com.octavia.tuner.core.TunerReading
import com.octavia.tuner.core.TuningPreset
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

@Composable
fun BackgroundView(tint: Color, content: @Composable () -> Unit) {
    val animated by animateColorAsState(tint, tween(350), label = "bgTint")
    val dark = MaterialTheme.colorScheme.background.red < 0.5f
    val strength = if (dark) 0.24f else 0.16f
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        listOf(animated.copy(alpha = animated.alpha * strength), Color.Transparent),
                        center = Offset(size.width / 2, 0f),
                        radius = size.width * 1.2f,
                    )
                )
            }
    ) { content() }
}

private fun splitName(value: String): Pair<String, String> {
    val octave = value.takeLastWhile { it.isDigit() || it == '-' }
    return value.dropLast(octave.length) to octave
}

@Composable
fun NoteDisplay(
    reading: TunerReading?,
    displayName: String?,
    transposedName: String?,
    isHolding: Boolean,
    scale: Float,
) {
    val original = splitName(displayName ?: "–")
    val target = transposedName?.let(::splitName)
    val primary = MaterialTheme.colorScheme.onSurface
    val secondary = faint(0.6f)
    val bigSize = (if (target == null) 92 else 62) * scale
    val glow by animateFloatAsState(if (reading?.isInTune == true && !isHolding) 1f else 0f, tween(300), label = "glow")
    val glowColor = TunerPalette.color(0.0)

    Box(
        Modifier
            .height((128 * scale).dp)
            .alpha(if (isHolding) 0.55f else 1f),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size((190 * scale).dp)
                .alpha(glow)
                .background(Brush.radialGradient(listOf(glowColor.copy(alpha = 0.35f), Color.Transparent)), CircleShape)
        )
        Row {
            val dim = target != null
            Text(
                original.first, fontSize = bigSize.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false,
                color = when { dim -> secondary.copy(alpha = 0.52f); reading == null -> secondary; else -> primary },
                modifier = Modifier.alignByBaseline(),
            )
            Text(
                original.second, fontSize = (bigSize * 0.37f).sp, fontWeight = FontWeight.Medium, maxLines = 1,
                color = faint(if (dim) 0.3f else 0.45f), modifier = Modifier.alignByBaseline(),
            )
            if (target != null) {
                Text(
                    " » ", fontSize = (bigSize * 0.45f).sp, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary, modifier = Modifier.alignByBaseline(),
                )
                Text(
                    target.first, fontSize = bigSize.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false,
                    color = if (reading == null) secondary else primary, modifier = Modifier.alignByBaseline(),
                )
                Text(
                    target.second, fontSize = (bigSize * 0.37f).sp, fontWeight = FontWeight.Medium, maxLines = 1,
                    color = faint(0.45f), modifier = Modifier.alignByBaseline(),
                )
            }
        }
    }
}

@Composable
fun PercentReadout(reading: TunerReading?, tint: Color, scale: Float) {
    val text = when {
        reading == null -> "––"
        abs(reading.cents) < 0.5 -> "0¢"
        else -> String.format(Locale.US, "%+.0f¢", reading.cents)
    }
    val caption = when {
        reading == null -> "Play a note"
        reading.isInTune -> "In tune"
        reading.cents < 0 -> "Flat"
        else -> "Sharp"
    }
    Row(horizontalArrangement = Arrangement.spacedBy((14 * scale).dp), verticalAlignment = Alignment.CenterVertically) {
        Badge("♭", (reading?.percent ?: 0.0) < -NoteMapper.inTunePercent, tint, scale)
        Column(Modifier.widthIn(min = (120 * scale).dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text, fontSize = (46 * scale).sp, fontWeight = FontWeight.SemiBold,
                color = if (reading == null) faint(0.6f) else tint,
            )
            Text(
                caption.uppercase(), fontSize = (13 * scale).sp, fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.6.sp, color = faint(0.6f),
            )
        }
        Badge("♯", (reading?.percent ?: 0.0) > NoteMapper.inTunePercent, tint, scale)
    }
}

@Composable
private fun Badge(symbol: String, lit: Boolean, tint: Color, scale: Float) {
    Box(
        Modifier
            .size((44 * scale).dp)
            .clip(CircleShape)
            .background(if (lit) tint.copy(alpha = 0.18f) else faint(0.05f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, fontSize = (26 * scale).sp, fontWeight = FontWeight.SemiBold, color = if (lit) tint else faint(0.35f))
    }
}

@Composable
fun ReadoutRow(reading: TunerReading?, scale: Float, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(faint(0.05f))
            .padding(vertical = (12 * scale).dp)
    ) {
        Stat("Detected", reading?.let { String.format(Locale.US, "%.1f Hz", it.frequency) } ?: "–")
        Stat("Target", reading?.let { String.format(Locale.US, "%.2f Hz", it.targetFrequency) } ?: "–")
        Stat("Deviation", reading?.let { String.format(Locale.US, "%+.1f ¢", it.cents) } ?: "–")
    }
}

@Composable
private fun RowScope.Stat(title: String, value: String) {
    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp, color = faint(0.6f))
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
    }
}

/** Shows the strings of the selected preset and highlights the one being tuned. */
@Composable
fun StringRow(
    preset: TuningPreset,
    activeMidi: Int?,
    tint: Color,
    naming: NoteNaming,
    accidental: AccidentalStyle,
) {
    val strings = preset.strings ?: return
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        strings.forEach { midi ->
            val active = midi == activeMidi
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(if (active) tint.copy(alpha = 0.25f) else faint(0.06f))
                    .border(1.5.dp, if (active) tint else Color.Transparent, CircleShape)
                    .padding(horizontal = 11.dp, vertical = 6.dp)
            ) {
                Text(NoteMapper.formattedName(midi, naming, accidental), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun LevelMeter(level: Double, modifier: Modifier = Modifier, gateDB: Double? = null) {
    val track = faint(0.08f)
    val marker = faint(0.75f)
    Canvas(modifier.fillMaxWidth().height(6.dp).clip(CircleShape)) {
        drawRect(track)
        val fillW = max(6.dp.toPx(), size.width * level.toFloat())
        drawRect(
            Brush.horizontalGradient(listOf(Color(0xFF34C759), Color(0xFFFFCC00), Color(0xFFFF9500)), endX = size.width),
            size = Size(fillW, size.height),
        )
        if (gateDB != null) {
            val x = size.width * ((gateDB + 80) / 80).toFloat()
            drawRect(marker, topLeft = Offset(x - 1f, 0f), size = Size(2f, size.height))
        }
    }
}

@Composable
fun <T> SegmentedChoice(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(faint(0.06f))
            .padding(4.dp)
    ) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { onSelect(value) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else faint(0.6f),
                )
            }
        }
    }
}
