// Web owner of a running workout (Phase 4a): the shared SessionRunner driven in-page, with Web Audio cues and the
// Screen Wake Lock. Timers come from stored end times (the reducer's deadlines on the sleep-aware clock), so a hidden
// tab or locked phone shows the right time when it returns; the checkpoint is kept in web settings storage.
package io.github.gonbei774.calisthenicsmemory.session

import app.calisthenics.domain.session.CueKind
import app.calisthenics.domain.session.SessionState
import io.github.gonbei774.calisthenicsmemory.platform.PlatformContext
import io.github.gonbei774.calisthenicsmemory.ui.screens.PrefsStore
import kotlinx.coroutines.Dispatchers

internal class WebCuePlayer(private val ctx: PlatformContext) : CuePlayer {
    private var speak = true
    private var volumePercent = 30

    override fun load() { webInstallAudioUnlock() }

    override fun refreshPreferences() {
        val p = PrefsStore.load(ctx)
        volumePercent = p.appVolumePercent.coerceIn(0, 100)
        speak = p.audioEnabled
    }

    override fun cue(kind: CueKind) {
        val gain = appAudioGain(volumePercent)
        if (gain <= 0f) return
        val sample = when (kind) {
            CueKind.START, CueKind.ROUND -> 0
            CueKind.COUNTDOWN_3, CueKind.COUNTDOWN_2, CueKind.COUNTDOWN_1 -> 1
            CueKind.FINISHED -> 2
            CueKind.PREVIEW_NEXT -> return
        }
        webBeep(sample, gain.toDouble())
    }

    override fun say(text: String) {
        if (!shouldSpeakCue(speak, webSpeechAvailable(), text, volumePercent)) return
        webSpeak(text, appAudioGain(volumePercent).toDouble())
    }

    override fun release() {}
}

internal class WebSessionHost(ctx: PlatformContext) : SessionHost {
    override val cues: CuePlayer = WebCuePlayer(ctx)
    override fun keepAwakeStart(maxMs: Long) = webWakeLock(true)
    override fun keepAwakeEnsure() = webWakeLock(true)
    override fun onFinished() = webWakeLock(false)
    override fun logWarn(message: String) = println("WorkoutSession: $message")
    override fun logError(message: String, t: Throwable?) = println("WorkoutSession: $message ${t ?: ""}")
}

/** The single workout of this page. */
internal object WebSession {
    private var runner: SessionRunner? = null

    private fun running() = runner?.takeIf { it.isRunning }

    private fun newRunner(ctx: PlatformContext): SessionRunner {
        webOnLifecycle(onVisible = { runner?.tick() }, onHide = { runner?.checkpointNow() })
        return SessionRunner(ctx, WebSessionHost(ctx), Dispatchers.Default).also { runner = it }
    }

    fun start(ctx: PlatformContext, planJson: String, sessionId: String) {
        if (running() != null) return // one session at a time
        newRunner(ctx).start(planJson, sessionId)
    }

    fun recover(ctx: PlatformContext, resume: Boolean) {
        if (running() != null) return
        val c = CheckpointStore.read(ctx)
        if (c == null) { SessionBus.abortStarting(); return }
        newRunner(ctx).recover(c, resume)
    }

    /** Commands and logs go to the live runner; nothing happens (like a service that is not running) without one. */
    fun runner(): SessionRunner? = running()
}
