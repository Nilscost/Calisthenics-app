package io.github.gonbei774.calisthenicsmemory.session

import android.media.AudioAttributes
import app.calisthenics.domain.session.CueKind

/** The short bundled samples used by the session service. */
enum class CueSample { START, WARNING, FINISHED }

/** Keep cues on the media route/volume group without ever requesting audio focus. */
internal fun cueAudioAttributes(): AudioAttributes = AudioAttributes.Builder()
    .setUsage(AudioAttributes.USAGE_MEDIA)
    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
    .build()

/** Minimal adapter around SoundPool so loading and playback can be observed and tested. */
internal interface CueAudioSink {
    /** Returns the sample id or 0 when the platform cannot start loading it. */
    fun load(sample: CueSample): Int

    /** Returns SoundPool's stream id, or 0 when playback could not be started. */
    fun play(sampleId: Int, leftGain: Float, rightGain: Float): Int
}

internal sealed interface CueAudioDiagnostic {
    data class LoadRequested(val sample: CueSample, val sampleId: Int) : CueAudioDiagnostic
    data class LoadComplete(val sample: CueSample, val sampleId: Int) : CueAudioDiagnostic
    data class LoadFailed(val sample: CueSample, val sampleId: Int, val status: Int) : CueAudioDiagnostic
    data class PlayRequested(val cue: CueKind, val sample: CueSample, val streamId: Int, val gain: Float) : CueAudioDiagnostic
    data class PlayFailed(val cue: CueKind, val sample: CueSample, val gain: Float) : CueAudioDiagnostic
    data class NotReady(val cue: CueKind, val sample: CueSample) : CueAudioDiagnostic
    data class Muted(val cue: CueKind) : CueAudioDiagnostic
}

/**
 * Maps reducer cues to samples and records the per-sample SoundPool load/play outcome.
 * Unavailable audio never blocks session time; callers keep the reducer/service running.
 */
internal class CuePlaybackPipeline(
    private val sink: CueAudioSink,
    private val diagnostic: (CueAudioDiagnostic) -> Unit = {},
) {
    private enum class LoadState { PENDING, LOADED, FAILED }

    private val sampleIds = mutableMapOf<CueSample, Int>()
    private val samplesById = mutableMapOf<Int, CueSample>()
    private val loadStates = mutableMapOf<CueSample, LoadState>()

    @Synchronized fun loadAll() {
        CueSample.values().forEach { sample ->
            try {
                val sampleId = sink.load(sample)
                if (sampleId > 0) {
                    sampleIds[sample] = sampleId
                    samplesById[sampleId] = sample
                    loadStates[sample] = LoadState.PENDING
                    diagnostic(CueAudioDiagnostic.LoadRequested(sample, sampleId))
                } else {
                    loadStates[sample] = LoadState.FAILED
                    diagnostic(CueAudioDiagnostic.LoadFailed(sample, sampleId, -1))
                }
            } catch (_: Exception) {
                loadStates[sample] = LoadState.FAILED
                diagnostic(CueAudioDiagnostic.LoadFailed(sample, 0, -1))
            }
        }
    }

    /** SoundPool reports status 0 only when this particular sample loaded successfully. */
    @Synchronized fun onLoadComplete(sampleId: Int, status: Int) {
        val sample = samplesById[sampleId] ?: return
        if (status == 0) {
            loadStates[sample] = LoadState.LOADED
            diagnostic(CueAudioDiagnostic.LoadComplete(sample, sampleId))
        } else {
            loadStates[sample] = LoadState.FAILED
            diagnostic(CueAudioDiagnostic.LoadFailed(sample, sampleId, status))
        }
    }

    @Synchronized fun deliver(cue: CueKind, volumePercent: Int): Boolean {
        val sample = cue.sampleOrNull() ?: return false
        val gain = appAudioGain(volumePercent)
        if (gain <= 0f) {
            diagnostic(CueAudioDiagnostic.Muted(cue))
            return false
        }
        val sampleId = sampleIds[sample]
        if (sampleId == null || loadStates[sample] != LoadState.LOADED) {
            diagnostic(CueAudioDiagnostic.NotReady(cue, sample))
            return false
        }
        return try {
            val streamId = sink.play(sampleId, gain, gain)
            if (streamId > 0) {
                diagnostic(CueAudioDiagnostic.PlayRequested(cue, sample, streamId, gain))
                true
            } else {
                diagnostic(CueAudioDiagnostic.PlayFailed(cue, sample, gain))
                false
            }
        } catch (_: Exception) {
            diagnostic(CueAudioDiagnostic.PlayFailed(cue, sample, gain))
            false
        }
    }

    private fun CueKind.sampleOrNull(): CueSample? = when (this) {
        CueKind.START, CueKind.ROUND -> CueSample.START
        CueKind.COUNTDOWN_3, CueKind.COUNTDOWN_2, CueKind.COUNTDOWN_1 -> CueSample.WARNING
        CueKind.FINISHED -> CueSample.FINISHED
        CueKind.PREVIEW_NEXT -> null
    }
}
