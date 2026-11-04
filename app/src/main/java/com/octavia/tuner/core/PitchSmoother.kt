package com.octavia.tuner.core

import kotlin.math.abs

/**
 * Smooths a stream of fractional MIDI values (1 unit = 100 cents):
 * median filter -> exponential moving average, with immediate snap on a sustained note jump.
 */
class PitchSmoother {
    var smoothing = 0.3
    var medianWindow = 5
    var jumpThreshold = 0.7
    var jumpsRequired = 3

    private val history = ArrayList<Double>()
    private var value: Double? = null
    private var pendingJumps = 0

    fun add(midi: Double): Double {
        val current = value
        if (current == null) {
            history.clear(); history.add(midi); value = midi
            return midi
        }
        if (abs(midi - current) > jumpThreshold) {
            pendingJumps++
            if (pendingJumps >= jumpsRequired) {
                history.clear(); history.add(midi); value = midi; pendingJumps = 0
                return midi
            }
            return current // ignore a possible glitch
        }
        pendingJumps = 0
        history.add(midi)
        if (history.size > medianWindow) history.removeAt(0)
        val median = history.sorted()[history.size / 2]
        val next = current + smoothing * (median - current)
        value = next
        return next
    }

    fun reset() {
        history.clear(); value = null; pendingJumps = 0
    }
}
