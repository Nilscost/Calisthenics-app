// T15: production foreground service. Single owner of the timer (monotonic clock), cues, wake lock and
// persistence of the finished session. The pure reducer decides; this class only performs effects.
package io.github.gonbei774.calisthenicsmemory.session

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.feedback.Execution
import app.calisthenics.domain.model.BlockType
import app.calisthenics.domain.model.WorkoutPlan
import app.calisthenics.domain.session.*
import io.github.gonbei774.calisthenicsmemory.MainActivity
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.*
import io.github.gonbei774.calisthenicsmemory.ui.screens.PrefsStore
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class WorkoutSessionService : Service() {
    companion object {
        const val ACTION_START = "wss.START"
        const val ACTION_PAUSE = "wss.PAUSE"
        const val ACTION_RESUME = "wss.RESUME"
        const val ACTION_SKIP = "wss.SKIP"
        const val ACTION_FINISH = "wss.FINISH"
        const val ACTION_DONE = "wss.DONE"
        const val ACTION_LOG = "wss.LOG"
        const val EXTRA_BLOCK_ID = "block_id"
        const val EXTRA_REPS = "reps"      // -1 = not typed
        const val EXTRA_TOO_HARD = "too_hard"
        const val EXTRA_PAIN = "pain"
        const val EXTRA_TOO_EASY = "too_easy"
        const val ACTION_RECOVER = "wss.RECOVER"
        const val EXTRA_RESUME = "resume"
        const val EXTRA_PLAN = "plan_json"
        const val EXTRA_SESSION_ID = "session_id"
        const val EXTRA_SPEAK = "speak"
        private const val CHANNEL = "workout_session"
        private const val NOTIF = 201
        private const val TICK_MS = 200L
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    }

    private var exec: ScheduledExecutorService? = null
    private var state: SessionState? = null
    private val lock = Any()
    private var wake: PowerManager.WakeLock? = null
    private var sound: SoundPool? = null
    private var cuePlayer: CuePlaybackPipeline? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    @Volatile private var speak = true
    @Volatile private var appVolumePercent = 30
    private var preferencesListener: SharedPreferences.OnSharedPreferenceChangeListener? = null
    private var startedAt = 0L

    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> start(intent)
            ACTION_RECOVER -> recover(intent)
            ACTION_PAUSE -> send(SessionEvent.Pause(now()))
            ACTION_RESUME -> { refreshAudioPreferences(); send(SessionEvent.Resume(now())) }
            ACTION_SKIP -> send(SessionEvent.Skip(now()))
            ACTION_FINISH -> send(SessionEvent.FinishEarly(now()))
            ACTION_DONE -> send(SessionEvent.EarlyDone(now()))
            ACTION_LOG -> intent.getStringExtra(EXTRA_BLOCK_ID)?.let { id ->
                send(SessionEvent.LogBlock(id, BlockLog(intent.getIntExtra(EXTRA_REPS, -1).takeIf { it >= 0 },
                    intent.getBooleanExtra(EXTRA_TOO_HARD, false), intent.getBooleanExtra(EXTRA_PAIN, false),
                    intent.getBooleanExtra(EXTRA_TOO_EASY, false))))
            }
            else -> if (state == null) { SessionBus.abortStarting(); stopSelf() }
        }
        return START_NOT_STICKY
    }

    private fun now() = SystemClock.elapsedRealtime()

    private fun recover(intent: Intent) {
        if (state != null) return
        val c = CheckpointStore.read(this)
        if (c == null) { SessionBus.abortStarting(); stopSelf(); return }
        val resume = intent.getBooleanExtra(EXTRA_RESUME, true)
        speak = PrefsStore.load(this).audioEnabled
        startedAt = c.startedAtEpochMs
        val rec = reduce(CheckpointCodec.toState(c), SessionEvent.ProcessRecovered).state
        val chosen = reduce(rec, SessionEvent.RecoveryChoice(resume)).state
        boot(c.plan, c.sessionId, chosen)
    }

    private fun start(intent: Intent) {
        if (state != null) return // one session at a time
        val plan = json.decodeFromString(WorkoutPlan.serializer(), intent.getStringExtra(EXTRA_PLAN)!!)
        val sid = intent.getStringExtra(EXTRA_SESSION_ID)!!
        speak = intent.getBooleanExtra(EXTRA_SPEAK, true)
        boot(plan, sid, null)
    }

    private fun boot(plan: WorkoutPlan, sid: String, restored: SessionState?) {
        createChannel()
        ServiceCompat.startForeground(this, NOTIF, notification("Starting…", false), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        SessionBus.clear()
        SessionBus.startedAtEpochMs = startedAt
        SessionBus.names = try {
            assets.open("catalog.json").bufferedReader().use { parseCatalog(it.readText()) }.variations.associate { it.id to it.name }
        } catch (_: Exception) { emptyMap() }
        sound = SoundPool.Builder().setMaxStreams(4).setAudioAttributes(cueAudioAttributes()).build()
        val loaded = java.util.concurrent.CountDownLatch(3)
        val pool = sound!!
        cuePlayer = CuePlaybackPipeline(object : CueAudioSink {
            override fun load(sample: CueSample): Int = pool.load(this@WorkoutSessionService, when (sample) {
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
        refreshAudioPreferences()
        val total = plan.blocks.sumOf { it.durationSeconds } * 1000L
        wake = (getSystemService(Context.POWER_SERVICE) as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "workout:session")
        wake?.acquire(total + 30 * 60_000L)
        exec = Executors.newSingleThreadScheduledExecutor()
        exec!!.execute {
            if (!loaded.await(5, TimeUnit.SECONDS)) android.util.Log.w("WorkoutAudio", "SoundPool load wait timed out; session timer continues")
            if (restored == null) {
                startedAt = System.currentTimeMillis()
                SessionBus.startedAtEpochMs = startedAt
                synchronized(lock) { state = newSession(sid, plan) }
                send(SessionEvent.Start(now()))
            } else {
                synchronized(lock) { state = restored }
                if (restored.isTerminal) SessionBus.endedAtEpochMs = System.currentTimeMillis()
                SessionBus.publish(restored)
                updateNotification(restored)
                if (restored.isTerminal) finish(restored)
            }
            exec!!.scheduleWithFixedDelay({
                val mode = (getSystemService(Context.AUDIO_SERVICE) as AudioManager).mode
                val inCall = mode == AudioManager.MODE_IN_CALL || mode == AudioManager.MODE_IN_COMMUNICATION || mode == AudioManager.MODE_RINGTONE
                if (inCall && state?.phase == Phase.RUNNING) send(SessionEvent.AudioInterrupted(now()))
                send(SessionEvent.Tick(now()))
            }, TICK_MS, TICK_MS, TimeUnit.MILLISECONDS)
        }
    }

    private fun send(e: SessionEvent) {
        val ex = exec ?: return
        ex.execute { apply(e) }
    }

    private fun apply(e: SessionEvent) {
        val fx: List<SessionEffect>
        val s: SessionState
        synchronized(lock) {
            val cur = state ?: return
            val r = reduce(cur, e)
            state = r.state; fx = r.effects; s = r.state
        }
        if (s.isTerminal && SessionBus.endedAtEpochMs == 0L) SessionBus.endedAtEpochMs = System.currentTimeMillis() // before publish: the end screen counts minutes from it
        SessionBus.publish(s)
        fx.forEach { perform(it, s) }
        checkpoint(s, force = fx.any { it is SessionEffect.Persist })
        if (fx.any { it is SessionEffect.ShowBlock } || e is SessionEvent.Pause || e is SessionEvent.Resume) updateNotification(s)
        if (s.isTerminal) finish(s)
    }

    private var lastCkptMs = 0L
    private fun checkpoint(s: SessionState, force: Boolean) {
        if (s.isTerminal) return
        val n = now()
        if (!force && n - lastCkptMs < 5_000) return
        lastCkptMs = n
        try { CheckpointStore.write(this, CheckpointCodec.encode(s, startedAt, n, System.currentTimeMillis())) } catch (e: Exception) { android.util.Log.e("WorkoutSession", "checkpoint failed", e) }
    }

    private fun label(blockId: String, s: SessionState): String = CueText.label(s.plan, blockId, SessionBus.names)

    private fun perform(f: SessionEffect, s: SessionState) {
        when (f) {
            is SessionEffect.Cue -> when (f.kind) {
                CueKind.START, CueKind.ROUND -> { playCue(f.kind); say(label(f.blockId, s)) }
                CueKind.PREVIEW_NEXT -> {
                    CueText.nextLabel(s.plan, f.blockId, SessionBus.names)?.let { say("Next: $it") }
                }
                CueKind.COUNTDOWN_3, CueKind.COUNTDOWN_2, CueKind.COUNTDOWN_1 -> playCue(f.kind)
                CueKind.FINISHED -> { playCue(f.kind); say("Workout complete") }
            }
            SessionEffect.AcquireWakeLock -> if (wake?.isHeld == false) wake?.acquire(4 * 3600_000L)
            SessionEffect.ReleaseWakeLock -> {}
            is SessionEffect.ShowBlock, is SessionEffect.Persist -> {} // the checkpoint is written by checkpoint() after every event; these effects need no extra work here
        }
    }

    private fun playCue(kind: CueKind) { cuePlayer?.deliver(kind, appVolumePercent) }

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
        val manager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).joinToString(",") { device ->
            "${device.productName}/type-${device.type}"
        }.ifBlank { "none" }
    }.getOrDefault("unavailable")

    private fun registerAudioPreferenceListener() {
        if (preferencesListener != null) return
        val prefs = getSharedPreferences("planner_prefs", Context.MODE_PRIVATE)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "prefs") refreshAudioPreferences()
        }
        preferencesListener = listener
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    private fun refreshAudioPreferences() {
        val preferences = PrefsStore.load(this)
        appVolumePercent = preferences.appVolumePercent.coerceIn(0, 100)
        speak = preferences.audioEnabled
        if (!speak) {
            tts?.shutdown(); tts = null; ttsReady = false
        } else if (tts == null) {
            tts = runCatching {
                TextToSpeech(this) { status ->
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

    private fun say(text: String) {
        if (!shouldSpeakCue(speak, ttsReady, text, appVolumePercent)) return
        val voiceGain = appAudioGain(appVolumePercent)
        val params = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, voiceGain) }
        runCatching { tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "u${System.nanoTime()}") }
    }

    private fun finish(s: SessionState) {
        if (SessionBus.saveStatus.value != SaveStatus.NONE) return
        SessionBus.saving()
        try {
            val db = AppDatabase.getDatabase(this)
            val planJson = json.encodeToString(WorkoutPlan.serializer(), s.plan)
            val blocks = s.plan.blocks.filter { it.type == BlockType.WORK || it.type == BlockType.STRETCH }.map { b ->
                val ex = s.executions[b.id] ?: Execution.NOT_STARTED
                BlockResultEntity(sessionId = s.sessionId, blockId = b.id,
                    outcome = app.calisthenics.domain.history.outcomeOf(ex),
                    actualSeconds = ((s.activeMs[b.id] ?: 0L) / 1000L).toInt(), achievedValue = s.logged[b.id]?.reps)
            }
            // What the user logged during the workout becomes the first feedback revision of each exercise, in the same transaction.
            val now = System.currentTimeMillis()
            val feedback = rowLogsFrom(s.plan, s.logged).map { r ->
                FeedbackRevisionEntity(sessionId = s.sessionId, variationId = r.variationId, revision = 1, rating = r.rating.name,
                    discomfort = r.discomfort, assumedMet = false, createdAtEpochMs = now, actualReps = r.actualReps)
            }
            val status = if (s.phase == Phase.COMPLETED) "COMPLETED" else "PARTIAL_FINISHED"
            runBlocking {
                db.historyDao().saveFinishedSession(
                    PlanSnapshotEntity(s.plan.id, s.plan.createdAtEpochMs, s.plan.routineId, s.plan.routineRevision, s.plan.catalogVersion, s.plan.profileId, planJson),
                    WorkoutSessionEntity(s.sessionId, s.plan.id, startedAt, SessionBus.endedAtEpochMs.takeIf { it > 0L } ?: System.currentTimeMillis(), status), blocks, feedback)
            }
            CheckpointStore.clear(this) // only after the session row is safely in Room
            SessionBus.saved = true
        } catch (e: Exception) { SessionBus.saveFailed(); android.util.Log.e("WorkoutSession", "save failed", e) }
        exec?.shutdown()
        releaseAll()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun releaseAll() {
        preferencesListener?.let { getSharedPreferences("planner_prefs", Context.MODE_PRIVATE).unregisterOnSharedPreferenceChangeListener(it) }
        preferencesListener = null
        try { if (wake?.isHeld == true) wake?.release() } catch (_: Exception) {}
        sound?.release(); sound = null; cuePlayer = null
        tts?.shutdown(); tts = null
    }

    override fun onDestroy() { exec?.shutdownNow(); releaseAll(); super.onDestroy() }

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Workout in progress", NotificationManager.IMPORTANCE_LOW))
    }

    private fun act(a: String, label: String): NotificationCompat.Action {
        val pi = PendingIntent.getService(this, a.hashCode(), Intent(this, WorkoutSessionService::class.java).setAction(a), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Action(0, label, pi)
    }

    private fun notification(text: String, paused: Boolean): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL).setSmallIcon(R.mipmap.ic_launcher).setContentTitle("Workout").setContentText(text)
            .setOngoing(true).setContentIntent(open)
            .addAction(if (paused) act(ACTION_RESUME, "Resume") else act(ACTION_PAUSE, "Pause"))
            .addAction(act(ACTION_FINISH, "Finish")).build()
    }

    private fun updateNotification(s: SessionState) {
        val b = s.currentBlock ?: return
        val t = "${label(b.id, s)}  (${s.blockIndex + 1}/${s.plan.blocks.size})" + if (s.phase == Phase.PAUSED) " — paused" else ""
        getSystemService(NotificationManager::class.java).notify(NOTIF, notification(t, s.phase == Phase.PAUSED))
    }
}
