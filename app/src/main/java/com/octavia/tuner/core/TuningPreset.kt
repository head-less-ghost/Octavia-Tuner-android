package com.octavia.tuner.core

/** An instrument tuning. `strings == null` means chromatic. Strings are MIDI numbers, lowest first. */
data class TuningPreset(val id: String, val name: String, val strings: List<Int>?) {
    companion object {
        val chromatic = TuningPreset("chromatic", "Chromatic", null)

        val all = listOf(
            chromatic,
            TuningPreset("guitar-standard", "Guitar – Standard (EADGBE)", listOf(40, 45, 50, 55, 59, 64)),
            TuningPreset("guitar-dropd", "Guitar – Drop D", listOf(38, 45, 50, 55, 59, 64)),
            TuningPreset("guitar-dadgad", "Guitar – DADGAD", listOf(38, 45, 50, 55, 57, 62)),
            TuningPreset("guitar-openg", "Guitar – Open G", listOf(38, 43, 50, 55, 59, 62)),
            TuningPreset("guitar-halfdown", "Guitar – Half step down", listOf(39, 44, 49, 54, 58, 63)),
            TuningPreset("bass-standard", "Bass – Standard (EADG)", listOf(28, 33, 38, 43)),
            TuningPreset("ukulele", "Ukulele (GCEA)", listOf(67, 60, 64, 69)),
            TuningPreset("violin", "Violin (GDAE)", listOf(55, 62, 69, 76)),
        )

        fun withId(id: String?): TuningPreset? = all.firstOrNull { it.id == id }
    }
}
