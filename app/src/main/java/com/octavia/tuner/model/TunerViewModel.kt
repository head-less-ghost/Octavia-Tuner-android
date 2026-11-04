package com.octavia.tuner.model

import android.app.Application
import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.octavia.tuner.audio.AudioEngine
import com.octavia.tuner.audio.AudioFrame
import com.octavia.tuner.core.AccidentalStyle
import com.octavia.tuner.core.NoteMapper
import com.octavia.tuner.core.NoteNaming
import com.octavia.tuner.core.PitchSmoother
import com.octavia.tuner.core.TunerReading
import com.octavia.tuner.core.TuningPreset
import kotlinx.coroutines.launch

sealed interface TunerStatus {
    object Idle : TunerStatus
    object Requesting : TunerStatus
    object Listening : TunerStatus
    object Denied : TunerStatus
    data class Error(val message: String) : TunerStatus
}

/** Bridges the audio engine to Compose. All state is mutated on the main thread. */
class TunerViewModel(app: Application) : AndroidViewModel(app) {

    var status by mutableStateOf<TunerStatus>(TunerStatus.Idle)
        private set
    var reading by mutableStateOf<TunerReading?>(null)
        private set
    var isHolding by mutableStateOf(false)
        private set
    var level by mutableDoubleStateOf(0.0)
        private set

    var referencePitch by mutableDoubleStateOf(440.0)
        private set
    var preset by mutableStateOf(TuningPreset.chromatic)
        private set
    var noteNaming by mutableStateOf(NoteNaming.LETTERS)
        private set
    var accidentalStyle by mutableStateOf(AccidentalStyle.SHARPS)
        private set
    var transposeSemitones by mutableStateOf(0)
        private set
    var gateDB by mutableDoubleStateOf(-50.0)
        private set

    private val prefs = app.getSharedPreferences("tuner_prefs", Context.MODE_PRIVATE)
    private val engine = AudioEngine()
    private val smoother = PitchSmoother()
    private var lastHeard = 0.0
    private val holdDuration = 0.6 // keep the last reading briefly between plucks

    init {
        val ref = prefs.getFloat(K_REF, 440f).toDouble()
        referencePitch = if (ref in 400.0..480.0) ref else 440.0
        preset = TuningPreset.withId(prefs.getString(K_PRESET, null)) ?: TuningPreset.chromatic
        gateDB = prefs.getFloat(K_GATE, -50f).toDouble()
        noteNaming = NoteNaming.fromKey(prefs.getString(K_NAMING, null))
        accidentalStyle = AccidentalStyle.fromKey(prefs.getString(K_ACC, null))
        transposeSemitones = prefs.getInt(K_TRANSPOSE, 0)
        engine.gateDB = gateDB.toFloat()
        engine.onFrame = { frame -> viewModelScope.launch { handle(frame) } }
        engine.onError = { msg -> viewModelScope.launch { status = TunerStatus.Error(msg) } }
    }

    // ---- Settings -------------------------------------------------------------------------

    fun setReferencePitch(v: Double) {
        if (v == referencePitch) return
        referencePitch = v
        prefs.edit().putFloat(K_REF, v.toFloat()).apply()
        smoother.reset() // smoothed value lives in the old reference's MIDI space
    }

    fun setPreset(p: TuningPreset) {
        prefs.edit().putString(K_PRESET, p.id).apply()
        if (p == preset) return
        preset = p
        reading = null; isHolding = false; smoother.reset()
    }

    fun setNoteNaming(v: NoteNaming) { noteNaming = v; prefs.edit().putString(K_NAMING, v.key).apply() }
    fun setAccidentalStyle(v: AccidentalStyle) { accidentalStyle = v; prefs.edit().putString(K_ACC, v.key).apply() }
    fun setTranspose(v: Int) {
        transposeSemitones = v.coerceIn(-12, 12)
        prefs.edit().putInt(K_TRANSPOSE, transposeSemitones).apply()
    }
    fun setGate(v: Double) {
        gateDB = v
        engine.gateDB = v.toFloat()
        prefs.edit().putFloat(K_GATE, v.toFloat()).apply()
    }

    fun noteLabel(midi: Int): String = NoteMapper.formattedName(midi, noteNaming, accidentalStyle)

    // ---- Permission + lifecycle -------------------------------------------------------------

    fun markRequesting() { status = TunerStatus.Requesting }
    fun markDenied() { status = TunerStatus.Denied }
    fun resetStatus() { if (status != TunerStatus.Listening) status = TunerStatus.Idle }

    /** Call only when RECORD_AUDIO has been granted. */
    fun start() {
        if (engine.isRunning) { status = TunerStatus.Listening; return }
        try {
            engine.start()
            status = TunerStatus.Listening
        } catch (e: SecurityException) {
            status = TunerStatus.Denied
        } catch (e: Exception) {
            status = TunerStatus.Error(e.message ?: "Couldn't start listening.")
        }
    }

    fun stop() {
        engine.stop()
        reading = null; isHolding = false; level = 0.0
        smoother.reset()
        if (status == TunerStatus.Listening) status = TunerStatus.Idle
    }

    override fun onCleared() {
        engine.stop()
        super.onCleared()
    }

    // ---- Frame handling --------------------------------------------------------------------

    private fun handle(frame: AudioFrame) {
        if (status != TunerStatus.Listening) return
        // Meter: instant attack, slow release. Maps -80…0 dBFS onto 0…1.
        val normalised = ((frame.levelDB + 80.0) / 80.0).coerceIn(0.0, 1.0)
        level = if (normalised > level) normalised else level * 0.85 + normalised * 0.15

        val now = SystemClock.uptimeMillis() / 1000.0
        val pitch = frame.pitch
        if (pitch != null) {
            lastHeard = now
            isHolding = false
            val midi = NoteMapper.fractionalMidi(pitch.frequency, referencePitch)
            val smoothed = smoother.add(midi)
           reading = NoteMapper.readingFromMidi(smoothed, referencePitch, preset.strings)
        } else if (reading != null) {
            if (now - lastHeard > holdDuration) {
                reading = null; isHolding = false; smoother.reset()
            } else if (now - lastHeard > 0.15) {
                isHolding = true
            }
        }
    }

    private companion object {
        const val K_REF = "referencePitch"
        const val K_PRESET = "presetID"
        const val K_GATE = "gateDB"
        const val K_NAMING = "noteNaming"
        const val K_ACC = "accidentalStyle"
        const val K_TRANSPOSE = "transposeSemitones"
    }
}
