package com.octavia.tuner.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.octavia.tuner.audio.NotePlayer
import com.octavia.tuner.model.TunerStatus
import com.octavia.tuner.model.TunerViewModel
import java.util.Locale
import kotlin.math.abs

@Composable
fun TunerApp(vm: TunerViewModel, onRetry: () -> Unit, onOpenAppSettings: () -> Unit) {
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var tab by rememberSaveable { mutableStateOf(0) }
    val reading = vm.reading
    val bgTint = reading?.let { TunerPalette.color(abs(it.cents)) } ?: Color.Transparent
    val tint = reading?.let { TunerPalette.color(abs(it.cents)) } ?: faint(0.6f)

    BackgroundView(bgTint) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            when (val s = vm.status) {
                TunerStatus.Denied -> MessageScreen(
                    "🎙️", "Microphone access is off",
                    "Tuner needs the microphone to hear your instrument. Enable it in the app's system settings, then come back.",
                    primaryLabel = "Open Settings", onPrimary = onOpenAppSettings,
                    secondaryLabel = "Try Again", onSecondary = onRetry,
                )
                is TunerStatus.Error -> MessageScreen(
                    "⚠️", "Couldn't start listening", s.message,
                    primaryLabel = "Try Again", onPrimary = onRetry,
                )
                else -> if (showSettings) {
                    BackHandler { showSettings = false }
                    SettingsScreen(vm, onClose = { showSettings = false })
                } else {
                    TunerContent(vm, tab, { tab = it }, tint, onSettings = { showSettings = true })
                }
            }
        }
    }
}

@Composable
private fun TunerContent(vm: TunerViewModel, tab: Int, onTab: (Int) -> Unit, tint: Color, onSettings: () -> Unit) {
    val reading = vm.reading
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val scale = (maxWidth.value / 400f).coerceIn(0.8f, 1.3f)
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .alpha(if (vm.status == TunerStatus.Listening) 1f else 0.6f),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Tuner", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        String.format(Locale.US, "%s · A4 = %.1f Hz", vm.preset.name, vm.referencePitch),
                        fontSize = 12.sp, color = faint(0.6f),
                    )
                }
                IconButton(onSettings) { Icon(Icons.Default.Settings, contentDescription = "Settings") }
            }

            SegmentedChoice(
                listOf(0 to "Tuner", 1 to "Note Player"), tab, onTab,
                Modifier.widthIn(max = 360.dp),
            )

            if (tab == 1) {
                val player = remember { NotePlayer() }
                DisposableEffect(Unit) { onDispose { player.stop() } }
                NotePlayerScreen(vm, player, Modifier.weight(1f))
            } else {
                Spacer(Modifier.weight(1f))
                NoteDisplay(
                    reading = reading,
                    displayName = reading?.let { vm.noteLabel(it.midi) },
                    transposedName = if (vm.transposeSemitones == 0) null
                    else reading?.let { vm.noteLabel(it.midi + vm.transposeSemitones) },
                    isHolding = vm.isHolding,
                    scale = scale,
                )
                GaugeView(reading?.percent ?: 0.0, reading != null, tint)
                PercentReadout(reading, tint, scale)
                ReadoutRow(reading, scale)
                StringRow(vm.preset, reading?.midi, tint, vm.noteNaming, vm.accidentalStyle)
                Spacer(Modifier.weight(1f))
                LevelMeter(vm.level)
            }
        }
    }
}

@Composable
private fun MessageScreen(
    emoji: String, title: String, body: String,
    primaryLabel: String, onPrimary: () -> Unit,
    secondaryLabel: String? = null, onSecondary: () -> Unit = {},
) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(emoji, fontSize = 54.sp)
        Spacer(Modifier.padding(8.dp))
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Spacer(Modifier.padding(6.dp))
        Text(body, color = faint(0.6f), textAlign = TextAlign.Center)
        Spacer(Modifier.padding(12.dp))
        Button(onPrimary) { Text(primaryLabel) }
        if (secondaryLabel != null) {
            Spacer(Modifier.padding(4.dp))
            OutlinedButton(onSecondary) { Text(secondaryLabel) }
        }
    }
}
