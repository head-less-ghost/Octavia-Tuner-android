package com.octavia.tuner.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/** Generates a short, clean sine tone for the reference-note player. */
class NotePlayer {
    var playingMidi by mutableStateOf<Int?>(null)
        private set

    private var track: AudioTrack? = null
    private val main = Handler(Looper.getMainLooper())

    fun play(midi: Int, reference: Double = 440.0, volume: Float = 0.65f) {
        stop()
        val frequency = reference * 2.0.pow((midi - 69) / 12.0)
        val sampleRate = 44_100
        val count = sampleRate * 2 // 2 seconds
        val pcm = ShortArray(count) { i ->
            val fadeIn = min(1.0, i / 500.0)
            val fadeOut = min(1.0, (count - i) / 4000.0)
            val sample = sin(2.0 * PI * frequency * i / sampleRate) * 0.72 * fadeIn * fadeOut
            (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
        try {
            val t = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(count * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            t.write(pcm, 0, count)
            t.setVolume(volume)
            t.setNotificationMarkerPosition(count - 1)
            t.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(track: AudioTrack?) {
                    if (this@NotePlayer.track === t) stop()
                }
                override fun onPeriodicNotification(track: AudioTrack?) {}
            }, main)
            track = t
            t.play()
            playingMidi = midi
        } catch (e: Exception) {
            track = null
            playingMidi = null
        }
    }

    fun stop() {
        track?.let {
            runCatching { it.stop() }
            it.release()
        }
        track = null
        playingMidi = null
    }
}
