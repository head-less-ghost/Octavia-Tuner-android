package com.octavia.tuner.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.octavia.tuner.core.PitchEstimate
import com.octavia.tuner.core.YinPitchDetector
import kotlin.math.ceil
import kotlin.math.log10
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

data class AudioFrame(
    val levelDB: Float,          // RMS level in dBFS of the analysed window
    val pitch: PitchEstimate?,   // null = below the noise gate or not periodic
)

/**
 * Captures microphone audio with AudioRecord and runs YIN pitch detection on a background thread.
 * Frames are delivered on that thread via [onFrame]; consumers must hop to the main thread.
 * The caller must hold the RECORD_AUDIO permission before calling [start].
 */
class AudioEngine {
    @Volatile var onFrame: ((AudioFrame) -> Unit)? = null
    @Volatile var onError: ((String) -> Unit)? = null
    @Volatile var gateDB: Float = -50f

    private var record: AudioRecord? = null
    private var thread: Thread? = null
    @Volatile private var running = false

    val isRunning: Boolean get() = running

    @SuppressLint("MissingPermission")
    fun start() {
        if (running) return
        val sampleRate = 44_100 // the one rate every Android device must support
        // ~70 ms analysis window rounded up to a power of two (4096 @ 44.1 kHz).
        val frameSize = 2.0.pow(ceil(log2(sampleRate * 0.07))).toInt()

        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) throw IllegalStateException("No audio input device is available.")

        val rec = AudioRecord(
            MediaRecorder.AudioSource.MIC, sampleRate,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
            max(minBuffer, frameSize * 4)
        )
        if (rec.state != AudioRecord.STATE_INITIALIZED) {
            rec.release()
            throw IllegalStateException("Couldn't open the microphone.")
        }
        val detector = YinPitchDetector(sampleRate.toDouble(), frameSize)
        record = rec
        running = true
        rec.startRecording()
        thread = Thread({ loop(rec, detector, frameSize) }, "tuner-dsp").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    fun stop() {
        running = false
        thread?.let { runCatching { it.join(400) } }
        thread = null
        record?.let {
            runCatching { it.stop() }
            it.release()
        }
        record = null
    }

    private fun loop(rec: AudioRecord, detector: YinPitchDetector, frameSize: Int) {
        val hop = frameSize / 4 // 75 % overlap -> a new reading every ~23 ms
        val shorts = ShortArray(hop)
        val ring = FloatArray(frameSize)
        var filled = 0
        var since = 0

        while (running) {
            val n = rec.read(shorts, 0, hop)
            if (n < 0) {
                if (running) onError?.invoke("Audio input stopped unexpectedly.")
                break
            }
            if (n == 0) continue

            // Slide the window left and append the new samples.
            System.arraycopy(ring, n, ring, 0, frameSize - n)
            for (i in 0 until n) ring[frameSize - n + i] = shorts[i] / 32768f
            filled = min(frameSize, filled + n)
            since += n
            if (filled < frameSize || since < hop) continue
            since = 0

            var sumSq = 0.0
            for (v in ring) sumSq += v.toDouble() * v
            val rms = sqrt(sumSq / frameSize).toFloat()
            val db = 20f * log10(max(rms, 1e-7f))

            var pitch: PitchEstimate? = null
            // Noise gate first: skipping YIN for quiet input saves CPU/battery.
            if (db >= gateDB) {
                val estimate = detector.detect(ring)
                if (estimate != null && estimate.confidence >= 0.8) pitch = estimate
            }
            onFrame?.invoke(AudioFrame(db, pitch))
        }
    }
}
