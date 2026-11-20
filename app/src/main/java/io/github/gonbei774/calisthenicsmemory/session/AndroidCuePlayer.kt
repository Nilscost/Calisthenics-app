// Android implementation of the shared CuePlayer: the SoundPool beeps and TextToSpeech voice that used to live inside
// WorkoutSessionService, unchanged apart from the move.
package io.github.gonbei774.calisthenicsmemory.session

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import app.calisthenics.domain.session.CueKind
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.ui.screens.PrefsStore
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal class AndroidCuePlayer(private val context: Context) : CuePlayer {
    private var sound: SoundPool? = null
    private var cuePlayer: CuePlaybackPipeline? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    @Volatile private var speak = true
    @Volatile private var appVolumePercent = 30
    private var preferencesListener: SharedPreferences.OnSharedPreferenceChangeListener? = null
    private val loaded = CountDownLatch(3)

    override fun load() {
        sound = SoundPool.Builder().setMaxStreams(4).setAudioAttributes(cueAudioAttributes()).build()
        val pool = sound!!
        cuePlayer = CuePlaybackPipeline(object : CueAudioSink {
            override fun load(sample: CueSample): Int = pool.load(context, when (sample) {
                CueSample.START -> R.raw.start_cue
                CueSample.WARNING -> R.raw.beep_short
                CueSample.FINISHED -> R.raw.set_complete
            }, 1)

            override fun play(sampleId: Int, leftGain: Float, rightGain: Float): Int = pool.play(sampleId, leftGain, rightGain, 1, 0, 1f)
        }, ::logAudioDiagnostic)
        pool.setOnLoadCompleteListener { _, sampleId, status ->
            cuePlayer?.onLoadComplete(sampleId, status)
            loaded.countDown()
        }
        cuePlayer?.loadAll()
        registerAudioPreferenceListener()
    }

    override fun awaitLoaded() {
        if (!loaded.await(5, TimeUnit.SECONDS)) android.util.Log.w("WorkoutAudio", "SoundPool load wait timed out; session timer continues")
    }

    override fun cue(kind: CueKind) { cuePlayer?.deliver(kind, appVolumePercent) }

    private fun logAudioDiagnostic(event: CueAudioDiagnostic) {
        when (event) {
            is CueAudioDiagnostic.LoadRequested -> android.util.Log.d("WorkoutAudio", "load ${event.sample} sample=${event.sampleId}")
            is CueAudioDiagnostic.LoadComplete -> android.util.Log.d("WorkoutAudio", "loaded ${event.sample} sample=${event.sampleId}")
            is CueAudioDiagnostic.LoadFailed -> android.util.Log.e("WorkoutAudio", "load failed ${event.sample} sample=${event.sampleId} status=${event.status}")
            is CueAudioDiagnostic.PlayRequested -> android.util.Log.d("WorkoutAudio", "play ${event.cue} sample=${event.sample} stream=${event.streamId} gain=${event.gain} availableOutputs=${availableAudioOutputs()}")
            is CueAudioDiagnostic.PlayFailed -> android.util.Log.e("WorkoutAudio", "play failed ${event.cue} sample=${event.sample} gain=${event.gain}")
            is CueAudioDiagnostic.NotReady -> android.util.Log.w("WorkoutAudio", "cue skipped; sample unavailable ${event.cue} sample=${event.sample}")
            is CueAudioDiagnostic.Muted -> android.util.Log.d("WorkoutAudio", "cue muted by saved app gain: ${event.cue}")
        }
    }

    private fun availableAudioOutputs(): String = runCatching {
        val manager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).joinToString(",") { device ->
            "${device.productName}/type-${device.type}"
        }.ifBlank { "none" }
    }.getOrDefault("unavailable")

    private fun registerAudioPreferenceListener() {
        if (preferencesListener != null) return
        val prefs = context.getSharedPreferences("planner_prefs", Context.MODE_PRIVATE)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "prefs") refreshPreferences()
        }
        preferencesListener = listener
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    override fun refreshPreferences() {
        val preferences = PrefsStore.load(context)
        appVolumePercent = preferences.appVolumePercent.coerceIn(0, 100)
        speak = preferences.audioEnabled
        if (!speak) {
            tts?.shutdown(); tts = null; ttsReady = false
        } else if (tts == null) {
            tts = runCatching {
                TextToSpeech(context) { status ->
                    ttsReady = status == TextToSpeech.SUCCESS && tts?.let { it.isLanguageAvailable(Locale.US) >= TextToSpeech.LANG_AVAILABLE } == true
                    if (ttsReady) runCatching {
                        tts?.language = Locale.US
                        tts?.setSpeechRate(0.75f)
                        tts?.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(id: String?) {}
                            override fun onDone(id: String?) {}
                            @Deprecated("api") override fun onError(id: String?) {}
                        })
                    }.onFailure { ttsReady = false }
                }
            }.getOrNull()
        }
    }

    override fun say(text: String) {
        if (!shouldSpeakCue(speak, ttsReady, text, appVolumePercent)) return
        val voiceGain = appAudioGain(appVolumePercent)
        val params = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, voiceGain) }
        runCatching { tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "u${System.nanoTime()}") }
    }

    override fun release() {
        preferencesListener?.let { context.getSharedPreferences("planner_prefs", Context.MODE_PRIVATE).unregisterOnSharedPreferenceChangeListener(it) }
        preferencesListener = null
        sound?.release(); sound = null; cuePlayer = null
        tts?.shutdown(); tts = null
    }
}
