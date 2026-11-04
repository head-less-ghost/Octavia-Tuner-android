package com.octavia.tuner.core

import kotlin.math.max
import kotlin.math.min

data class PitchEstimate(val frequency: Double, val confidence: Double)

/**
 * Monophonic pitch detector based on the YIN algorithm (de Cheveigné & Kawahara, 2002).
 * Pure DSP: no Android APIs, no threading. Owns scratch buffers, so use one instance per thread.
 */
class YinPitchDetector(val sampleRate: Double, val frameSize: Int) {
    var threshold = 0.15f
    var minFrequency = 30.0
    var maxFrequency = 1500.0

    private val half = frameSize / 2
    private val work = FloatArray(frameSize)
    private val cumulativeEnergy = DoubleArray(frameSize + 1)
    private val cmnd = FloatArray(frameSize / 2)

    init {
        require(frameSize >= 256 && frameSize % 2 == 0)
    }

    /** Returns the estimated fundamental, or null if the frame isn't clearly periodic. */
    fun detect(samples: FloatArray): PitchEstimate? {
        if (samples.size != frameSize) return null
        val n = frameSize
        val w = half

        // 1. Remove DC offset.
        var sum = 0.0
        for (v in samples) sum += v
        val mean = (sum / n).toFloat()
        for (i in 0 until n) work[i] = samples[i] - mean

        // 2. Prefix sums of x².
        cumulativeEnergy[0] = 0.0
        for (i in 0 until n) cumulativeEnergy[i + 1] = cumulativeEnergy[i] + work[i].toDouble() * work[i]
        val e0 = cumulativeEnergy[w]

        val maxTau = min(w - 1, (sampleRate / minFrequency).toInt())
        val minTau = max(2, (sampleRate / maxFrequency).toInt())
        if (maxTau <= minTau + 2 || e0 <= 1e-9) return null

        // 3. Difference function + cumulative mean normalised difference.
        var running = 0.0
        cmnd[0] = 1f
        for (tau in 1..maxTau) {
            var dot = 0f
            for (j in 0 until w) dot += work[j] * work[j + tau]
            val shifted = cumulativeEnergy[w + tau] - cumulativeEnergy[tau]
            val d = max(0.0, e0 + shifted - 2.0 * dot)
            running += d
            cmnd[tau] = if (running > 0) (d * tau / running).toFloat() else 1f
        }

        // 4. First lag below the threshold, then slide down to the local minimum.
        var tau = minTau
        var best = -1
        while (tau < maxTau) {
            if (cmnd[tau] < threshold) {
                while (tau + 1 < maxTau && cmnd[tau + 1] < cmnd[tau]) tau++
                best = tau
                break
            }
            tau++
        }
        if (best <= 0) return null

        // 5. Parabolic interpolation for sub-sample lag precision.
        val s0 = cmnd[best - 1]
        val s1 = cmnd[best]
        val s2 = cmnd[best + 1]
        val denom = 2 * (2 * s1 - s2 - s0)
        val shift = if (denom != 0f) ((s2 - s0) / denom).toDouble().coerceIn(-1.0, 1.0) else 0.0
        val lag = best + shift
        return PitchEstimate(sampleRate / lag, (1 - s1).toDouble().coerceIn(0.0, 1.0))
    }
}
