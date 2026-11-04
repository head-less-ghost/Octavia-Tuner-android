package com.octavia.tuner.core

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

enum class NoteNaming(val key: String, val title: String) {
    LETTERS("letters", "C D E F G A B"),
    SOLFEGE("solfege", "Do Re Mi");

    companion object {
        fun fromKey(key: String?) = values().firstOrNull { it.key == key } ?: LETTERS
    }
}

enum class AccidentalStyle(val key: String, val title: String) {
    SHARPS("sharps", "Sharps (♯)"),
    FLATS("flats", "Flats (♭)");

    companion object {
        fun fromKey(key: String?) = values().firstOrNull { it.key == key } ?: SHARPS
    }
}

/** A fully resolved tuner reading. Pure value type, no UI or audio dependencies. */
data class TunerReading(
    val noteName: String,
    val octave: Int,
    val midi: Int,
    val targetFrequency: Double,
    val frequency: Double,
    val cents: Double,
    val percent: Double,
    val isInTune: Boolean,
) {
    val displayName: String get() = "$noteName$octave"
}

object NoteMapper {
    val sharpNames = listOf("C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B")
    val flatNames = listOf("C", "D♭", "D", "E♭", "E", "F", "G♭", "G", "A♭", "A", "B♭", "B")
    private val solfegeSharp = listOf("Do", "Do♯", "Re", "Re♯", "Mi", "Fa", "Fa♯", "Sol", "Sol♯", "La", "La♯", "Si")
    private val solfegeFlat = listOf("Do", "Re♭", "Re", "Mi♭", "Mi", "Fa", "Sol♭", "Sol", "La♭", "La", "Si♭", "Si")

    const val IN_TUNE_CENTS = 5.0
    const val TOLERANCE_CENTS = 50.0

    /** How far (in semitones) a pitch may be from a string and still be measured against it. */
    const val STRING_CAPTURE_SEMITONES = 2.0
    val inTunePercent: Double get() = IN_TUNE_CENTS / TOLERANCE_CENTS * 100

    fun fractionalMidi(frequency: Double, reference: Double = 440.0): Double =
        69 + 12 * log2(frequency / reference)

    fun frequency(midi: Double, reference: Double = 440.0): Double =
        reference * 2.0.pow((midi - 69) / 12)

    fun name(midi: Int): Pair<String, Int> {
        val index = Math.floorMod(midi, 12)
        return sharpNames[index] to (Math.floorDiv(midi, 12) - 1)
    }

    fun formattedName(midi: Int, naming: NoteNaming, accidental: AccidentalStyle): String {
        val index = Math.floorMod(midi, 12)
        val octave = Math.floorDiv(midi, 12) - 1
        val names = when (naming) {
            NoteNaming.LETTERS -> if (accidental == AccidentalStyle.SHARPS) sharpNames else flatNames
            NoteNaming.SOLFEGE -> if (accidental == AccidentalStyle.SHARPS) solfegeSharp else solfegeFlat
        }
        return "${names[index]}$octave"
    }

    fun reading(fractionalMidi: Double, reference: Double, targets: List<Int>? = null): TunerReading {
        val nearest = targets?.minByOrNull { abs(it - fractionalMidi) }
        val targetMidi = if (nearest != null && abs(nearest - fractionalMidi) <= STRING_CAPTURE_SEMITONES) {
            nearest
        } else {
            // Chromatic mode, or the pitch is nowhere near any string of the preset:
            // fall back to the nearest semitone so the deviation stays within ±50¢.
            fractionalMidi.roundToInt()
        }
        val cents = (fractionalMidi - targetMidi) * 100
        val percent = max(-100.0, min(100.0, cents / TOLERANCE_CENTS * 100))
        val (noteName, octave) = name(targetMidi)
        return TunerReading(
            noteName = noteName,
            octave = octave,
            midi = targetMidi,
            targetFrequency = frequency(targetMidi.toDouble(), reference),
            frequency = frequency(fractionalMidi, reference),
            cents = cents,
            percent = percent,
            isInTune = abs(cents) <= IN_TUNE_CENTS,
        )
    }

    fun reading(frequency: Double, reference: Double = 440.0, targets: List<Int>? = null): TunerReading? {
        if (frequency <= 0 || reference <= 0) return null
        return reading(fractionalMidi(frequency, reference), reference, targets)
    }
}
