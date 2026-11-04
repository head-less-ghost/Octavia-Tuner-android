# Tuner for Android

Kotlin + Jetpack Compose port of the SwiftUI macOS chromatic tuner. Same features: YIN pitch
detection, median+EMA smoothing, gauge, tuning presets (guitar, bass, ukulele, violin),
reference pitch (415–466 Hz), transposition, sharps/flats, solfège, noise gate, note player.

Package `com.octavia.tuner` · minSdk 24 · targetSdk 34.

## Build the APK

**Android Studio (easiest):** File → Open this folder → wait for Gradle sync →
Build → Build Bundle(s) / APK(s) → Build APK(s).
Output: `app/build/outputs/apk/debug/app-debug.apk`

**Command line** (JDK 17 + Android SDK installed, `ANDROID_HOME` set, Gradle 8.9):
`gradle assembleDebug`

**No installs, via GitHub:** push this folder to a GitHub repo → Actions tab → "Build APK" →
download the `tuner-apk` artifact.

## Install on a phone
Copy `app-debug.apk` to the phone, open it, and allow "install unknown apps" when asked.
Or with USB debugging: `adb install app-debug.apk`.

## Mapping from the Swift app
| Swift | Kotlin |
|---|---|
| Core/NoteMapping, PitchSmoother, TuningPreset | core/ (near line-for-line ports) |
| Core/PitchDetector (vDSP) | core/PitchDetector.kt (plain Kotlin loops) |
| Audio/AudioEngine (AVAudioEngine) | audio/AudioEngine.kt (AudioRecord, 44.1 kHz) |
| Audio/NotePlayer (AVAudioPlayer) | audio/NotePlayer.kt (AudioTrack) |
| Model/TunerViewModel | model/TunerViewModel.kt (SharedPreferences) |
| Views/* (SwiftUI) | ui/* (Compose) |
