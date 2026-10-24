package io.github.gonbei774.calisthenicsmemory.session

/** Converts the saved percentage to the linear gain accepted by Android audio APIs. */
internal fun appAudioGain(volumePercent: Int): Float = volumePercent.coerceIn(0, 100) / 100f

/** Voice preference and TTS readiness are deliberately scoped to speech; SoundPool never calls this. */
internal fun shouldSpeakCue(audioEnabled: Boolean, ttsReady: Boolean, text: String, volumePercent: Int): Boolean =
    audioEnabled && ttsReady && text.isNotBlank() && appAudioGain(volumePercent) > 0f
