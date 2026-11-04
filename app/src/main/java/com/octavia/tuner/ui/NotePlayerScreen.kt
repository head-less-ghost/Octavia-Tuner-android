package com.octavia.tuner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.octavia.tuner.audio.NotePlayer
import com.octavia.tuner.core.NoteMapper
import com.octavia.tuner.model.TunerViewModel
import java.util.Locale

@Composable
fun NotePlayerScreen(vm: TunerViewModel, player: NotePlayer, modifier: Modifier = Modifier) {
    var octave by rememberSaveable { mutableStateOf(4) }
    var volume by rememberSaveable { mutableStateOf(0.65f) }
    val playing = player.playingMidi

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("NOTE PLAYER", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp, color = faint(0.6f))
            Text(playing?.let { vm.noteLabel(it) } ?: "Choose a note", fontSize = 44.sp, fontWeight = FontWeight.Bold)
            Text(
                playing?.let {
                    String.format(Locale.US, "%.2f Hz", NoteMapper.frequency(it.toDouble(), vm.referencePitch))
                } ?: "Play a reference tone",
                fontSize = 14.sp, color = faint(0.6f),
            )
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Octave", fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            TextButton({ octave = (octave - 1).coerceAtLeast(0) }) { Text("−", fontSize = 20.sp) }
            Text("$octave", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            TextButton({ octave = (octave + 1).coerceAtMost(8) }) { Text("+", fontSize = 20.sp) }
        }

        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            for (row in 0 until 4) {
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    for (col in 0 until 3) {
                        val pitch = row * 3 + col
                        val midi = (octave + 1) * 12 + pitch
                        val isPlaying = playing == midi
                        Column(
                            Modifier
                                .weight(1f)
                                .height(62.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(if (isPlaying) MaterialTheme.colorScheme.primary else faint(0.055f))
                                .clickable { player.play(midi, vm.referencePitch, volume) },
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                vm.noteLabel(midi).dropLastWhile { it.isDigit() || it == '-' },
                                fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                                color = if (isPlaying) Color.White else MaterialTheme.colorScheme.onSurface,
                            )
                            Text("Play", fontSize = 11.sp, color = if (isPlaying) Color.White.copy(alpha = 0.8f) else faint(0.6f))
                        }
                    }
                }
            }
        }

        Column {
            Row(Modifier.fillMaxWidth()) {
                Text("Volume", fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text("${(volume * 100).toInt()}%", fontSize = 14.sp, color = faint(0.6f))
            }
            Slider(volume, { volume = it }, valueRange = 0f..1f)
        }

        OutlinedButton({ player.stop() }, enabled = playing != null, modifier = Modifier.fillMaxWidth()) {
            Text("■  Stop playback")
        }
        Spacer(Modifier.weight(1f))
        Text("Reference notes are generated on your device.", fontSize = 12.sp, color = faint(0.4f),
            modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}
