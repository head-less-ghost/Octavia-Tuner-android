package com.octavia.tuner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.octavia.tuner.core.AccidentalStyle
import com.octavia.tuner.core.NoteNaming
import com.octavia.tuner.core.TuningPreset
import com.octavia.tuner.model.TunerViewModel
import java.util.Locale

@Composable
fun SettingsScreen(vm: TunerViewModel, onClose: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text("Settings", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TuningCard(vm)
            PitchCard(vm)
            DisplayCard(vm)
            InputCard(vm)
        }
    }
}

@Composable
private fun TuningCard(vm: TunerViewModel) {
    val all = TuningPreset.all
    val groups = listOf(
        "Chromatic" to all.filter { it.strings == null },
        "Guitar" to all.filter { it.id.startsWith("guitar") },
        "Bass" to all.filter { it.id.startsWith("bass") },
        "Other instruments" to all.filter { it.strings != null && !it.id.startsWith("guitar") && !it.id.startsWith("bass") },
    ).filter { it.second.isNotEmpty() }

    SettingsCard("Tuning", "🎸", Color(0xFFFF9500)) {
        groups.forEach { (title, presets) ->
            if (title != "Chromatic") {
                Text(title.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.1.sp, color = faint(0.6f))
            }
            presets.forEach { preset ->
                val selected = vm.preset == preset
                val short = preset.name.substringAfter(" – ")
                val subtitle = preset.strings?.joinToString(" ") { vm.noteLabel(it).dropLastWhile { c -> c.isDigit() || c == '-' } }
                    ?: "Nearest note, any instrument"
                val accent = MaterialTheme.colorScheme.primary
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (selected) accent.copy(alpha = 0.14f) else faint(0.035f))
                        .border(1.2.dp, if (selected) accent.copy(alpha = 0.55f) else Color.Transparent, RoundedCornerShape(11.dp))
                        .clickable { vm.updatePreset(preset) }
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(short, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(subtitle, fontSize = 12.sp, color = faint(0.6f))
                    }
                    if (selected) {
                        Icon(Icons.Default.CheckCircle, null, tint = accent, modifier = Modifier.size(20.dp))
                    } else {
                        Box(Modifier.size(18.dp).border(1.5.dp, faint(0.3f), CircleShape))
                    }
                }
            }
        }
    }
}

@Composable
private fun PitchCard(vm: TunerViewModel) {
    SettingsCard("Pitch", "🎵", Color(0xFF0A84FF)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Reference pitch", fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Text(String.format(Locale.US, "A4 = %.1f Hz", vm.referencePitch), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            TextButton({ vm.updateReferencePitch(440.0) }, enabled = vm.referencePitch != 440.0) { Text("Reset") }
        }
        Slider(
            value = vm.referencePitch.toFloat(),
            onValueChange = { vm.updateReferencePitch(it.toDouble()) },
            valueRange = 415f..466f, steps = 101,
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Transposition", fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            TextButton({ vm.updateTranspose(vm.transposeSemitones - 1) }, enabled = vm.transposeSemitones > -12) { Text("−", fontSize = 20.sp) }
            Text(
                if (vm.transposeSemitones == 0) "0" else String.format(Locale.US, "%+d", vm.transposeSemitones),
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
            )
            TextButton({ vm.updateTranspose(vm.transposeSemitones + 1) }, enabled = vm.transposeSemitones < 12) { Text("+", fontSize = 20.sp) }
        }
        Text(
            if (vm.transposeSemitones == 0) "Concert pitch — notes are shown as they sound."
            else "Shows the note to read for a transposing instrument (e.g. B♭ clarinet = +2).",
            fontSize = 12.sp, color = faint(0.6f),
        )
    }
}

@Composable
private fun DisplayCard(vm: TunerViewModel) {
    SettingsCard("Display", "Aa", Color(0xFFAF52DE)) {
        Text("Note names", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        SegmentedChoice(NoteNaming.values().map { it to it.title }, vm.noteNaming, { vm.updateNoteNaming(it) })
        Text("Accidentals", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        SegmentedChoice(AccidentalStyle.values().map { it to it.title }, vm.accidentalStyle, { vm.updateAccidentalStyle(it) })
    }
}

@Composable
private fun InputCard(vm: TunerViewModel) {
    SettingsCard("Input", "🎤", Color(0xFF34C759)) {
        Row {
            Text("Noise gate", fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Text("${vm.gateDB.toInt()} dB", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = faint(0.6f))
        }
        Slider(vm.gateDB.toFloat(), { vm.updateGate(it.toDouble()) }, valueRange = -75f..-25f)
        LevelMeter(vm.level, gateDB = vm.gateDB)
        Text(
            "Sounds quieter than the marker are ignored. Raise it if background noise triggers false notes; lower it for quiet instruments.",
            fontSize = 12.sp, color = faint(0.6f),
        )
    }
}

@Composable
private fun SettingsCard(title: String, icon: String, tint: Color, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(faint(0.045f))
            .border(1.dp, faint(0.07f), shape)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.size(28.dp).clip(RoundedCornerShape(7.dp)).background(tint),
                contentAlignment = Alignment.Center,
            ) { Text(icon, fontSize = 13.sp, color = Color.White) }
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        content()
    }
}
