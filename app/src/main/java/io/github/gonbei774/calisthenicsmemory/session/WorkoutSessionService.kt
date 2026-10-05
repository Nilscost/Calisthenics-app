// T15: production foreground service. Single owner of the timer (monotonic clock), cues, wake lock and
// persistence of the finished session. The pure reducer decides; this class only performs effects.
package io.github.gonbei774.calisthenicsmemory.session

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.SoundPool
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
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
    private var sStart = 0; private var sBeep = 0; private var sDone = 0
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var speak = true
    private var am: AudioManager? = null
    private var focus: AudioFocusRequest? = null
    private var startedAt = 0L

    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> start(intent)
            ACTION_RECOVER -> recover(intent)
            ACTION_PAUSE -> send(SessionEvent.Pause(now()))
            ACTION_RESUME -> send(SessionEvent.Resume(now()))
            ACTION_SKIP -> send(SessionEvent.Skip(now()))
            ACTION_FINISH -> send(SessionEvent.FinishEarly(now()))
            else -> if (state == null) stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun now() = SystemClock.elapsedRealtime()

    private fun recover(intent: Intent) {
        if (state != null) return
        val c = CheckpointStore.read(this)
        if (c == null) { stopSelf(); return }
        val resume = intent.getBooleanExtra(EXTRA_RESUME, true)
        speak = true
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
        SessionBus.names = try {
            assets.open("catalog.json").bufferedReader().use { parseCatalog(it.readText()) }.variations.associate { it.id to it.name }
        } catch (_: Exception) { emptyMap() }
        am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val attrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
        sound = SoundPool.Builder().setMaxStreams(4).setAudioAttributes(attrs).build()
        val loaded = java.util.concurrent.CountDownLatch(3)
        sound!!.setOnLoadCompleteListener { _, _, _ -> loaded.countDown() }
        sStart = sound!!.load(this, R.raw.start_cue, 1); sBeep = sound!!.load(this, R.raw.beep_short, 1); sDone = sound!!.load(this, R.raw.set_complete, 1)
        if (speak) tts = TextToSpeech(this) { st ->
            ttsReady = st == TextToSpeech.SUCCESS && tts?.let { it.isLanguageAvailable(Locale.US) >= TextToSpeech.LANG_AVAILABLE } == true
            if (ttsReady) { tts?.language = Locale.US; tts?.setAudioAttributes(attrs)
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(id: String?) {}
                    override fun onDone(id: String?) { releaseFocus() }
                    @Deprecated("api") override fun onError(id: String?) { releaseFocus() }
                }) }
        }
        val total = plan.blocks.sumOf { it.durationSeconds } * 1000L
        wake = (getSystemService(Context.POWER_SERVICE) as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "workout:session")
        wake?.acquire(total + 30 * 60_000L)
        exec = Executors.newSingleThreadScheduledExecutor()
        exec!!.execute {
            loaded.await(5, TimeUnit.SECONDS) // t=0 only after sounds are ready (G1 lesson)
            if (restored == null) {
                startedAt = System.currentTimeMillis()
                synchronized(lock) { state = newSession(sid, plan) }
                send(SessionEvent.Start(now()))
            } else {
                synchronized(lock) { state = restored }
                SessionBus.publish(restored)
                updateNotification(restored)
                if (restored.isTerminal) finish(restored)
            }
            exec!!.scheduleWithFixedDelay({
                val mode = am?.mode
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
                CueKind.START, CueKind.ROUND -> { sound?.play(sStart, 1f, 1f, 1, 0, 1f); say(label(f.blockId, s)) }
                CueKind.PREVIEW_NEXT -> {
                    CueText.nextLabel(s.plan, f.blockId, SessionBus.names)?.let { say("Next: $it") }
                }
                CueKind.COUNTDOWN_3, CueKind.COUNTDOWN_2, CueKind.COUNTDOWN_1 -> sound?.play(sBeep, 1f, 1f, 1, 0, 1f)
                CueKind.FINISHED -> { sound?.play(sDone, 1f, 1f, 1, 0, 1f); say("Workout complete") }
            }
            SessionEffect.AcquireWakeLock -> if (wake?.isHeld == false) wake?.acquire(4 * 3600_000L)
            SessionEffect.ReleaseWakeLock -> {}
            is SessionEffect.ShowBlock, is SessionEffect.Persist -> {} // checkpoint persistence: not built yet (see notes)
        }
    }

    private fun say(text: String) {
        if (!speak || !ttsReady || text.isBlank()) return
        val a = am ?: return
        val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setOnAudioFocusChangeListener { }.build()
        focus = req; a.requestAudioFocus(req) // result ignored on purpose: cues play even if focus is refused
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "u${System.nanoTime()}")
    }

    private fun releaseFocus() { focus?.let { am?.abandonAudioFocusRequest(it) }; focus = null }

    private fun finish(s: SessionState) {
        if (SessionBus.saved) return
        SessionBus.saved = true
        try {
            val db = AppDatabase.getDatabase(this)
            val planJson = json.encodeToString(WorkoutPlan.serializer(), s.plan)
            val blocks = s.plan.blocks.filter { it.type == BlockType.WORK || it.type == BlockType.STRETCH }.map { b ->
                val ex = s.executions[b.id] ?: Execution.NOT_STARTED
                BlockResultEntity(sessionId = s.sessionId, blockId = b.id,
                    outcome = when (ex) { Execution.COMPLETED -> "MET"; Execution.PARTIAL -> "PARTIAL"; else -> "SKIPPED" },
                    actualSeconds = ((s.activeMs[b.id] ?: 0L) / 1000L).toInt(), achievedValue = null)
            }
            val status = if (s.phase == Phase.COMPLETED) "COMPLETED" else "PARTIAL_FINISHED"
            runBlocking {
                db.historyDao().saveFinishedSession(
                    PlanSnapshotEntity(s.plan.id, s.plan.createdAtEpochMs, s.plan.routineId, s.plan.routineRevision, s.plan.catalogVersion, s.plan.profileId, planJson),
                    WorkoutSessionEntity(s.sessionId, s.plan.id, startedAt, System.currentTimeMillis(), status), blocks)
            }
            CheckpointStore.clear(this) // only after the session row is safely in Room
        } catch (e: Exception) { SessionBus.saved = false; android.util.Log.e("WorkoutSession", "save failed", e) }
        exec?.shutdown()
        releaseAll()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun releaseAll() {
        try { if (wake?.isHeld == true) wake?.release() } catch (_: Exception) {}
        sound?.release(); sound = null
        tts?.shutdown(); tts = null
        releaseFocus()
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
