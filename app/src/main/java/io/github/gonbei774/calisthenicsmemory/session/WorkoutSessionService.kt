// T15: production foreground service. Since Phase 4a it is a thin Android host: the shared SessionRunner owns the timer
// (monotonic clock), cues, checkpointing and persistence of the finished session; this class adds what only Android has —
// the foreground notification, the wake lock, the SoundPool/TextToSpeech cue player, the call-state check and the Intents.
package io.github.gonbei774.calisthenicsmemory.session

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import app.calisthenics.domain.session.BlockLog
import app.calisthenics.domain.session.CueText
import app.calisthenics.domain.session.Phase
import app.calisthenics.domain.session.SessionState
import io.github.gonbei774.calisthenicsmemory.MainActivity
import io.github.gonbei774.calisthenicsmemory.R
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class WorkoutSessionService : Service(), SessionHost {
    companion object {
        const val ACTION_START = SessionServiceContract.ACTION_START
        const val ACTION_PAUSE = SessionServiceContract.ACTION_PAUSE
        const val ACTION_RESUME = SessionServiceContract.ACTION_RESUME
        const val ACTION_SKIP = SessionServiceContract.ACTION_SKIP
        const val ACTION_FINISH = SessionServiceContract.ACTION_FINISH
        const val ACTION_DONE = SessionServiceContract.ACTION_DONE
        const val ACTION_LOG = SessionServiceContract.ACTION_LOG
        const val EXTRA_BLOCK_ID = SessionServiceContract.EXTRA_BLOCK_ID
        const val EXTRA_REPS = SessionServiceContract.EXTRA_REPS
        const val EXTRA_TOO_HARD = SessionServiceContract.EXTRA_TOO_HARD
        const val EXTRA_PAIN = SessionServiceContract.EXTRA_PAIN
        const val EXTRA_TOO_EASY = SessionServiceContract.EXTRA_TOO_EASY
        const val ACTION_RECOVER = SessionServiceContract.ACTION_RECOVER
        const val EXTRA_RESUME = SessionServiceContract.EXTRA_RESUME
        const val EXTRA_PLAN = SessionServiceContract.EXTRA_PLAN
        const val EXTRA_SESSION_ID = SessionServiceContract.EXTRA_SESSION_ID
        const val EXTRA_SPEAK = SessionServiceContract.EXTRA_SPEAK
        private const val CHANNEL = "workout_session"
        private const val NOTIF = 201
    }

    private var exec: ExecutorService? = null
    private var runner: SessionRunner? = null
    private var wake: PowerManager.WakeLock? = null
    override val cues: CuePlayer by lazy { AndroidCuePlayer(this) }

    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val r = runner
        when (intent?.action) {
            ACTION_START -> start(intent)
            ACTION_RECOVER -> recover(intent)
            ACTION_PAUSE -> r?.pause()
            ACTION_RESUME -> r?.resume()
            ACTION_SKIP -> r?.skip()
            ACTION_FINISH -> r?.finishEarly()
            ACTION_DONE -> r?.earlyDone()
            ACTION_LOG -> intent.getStringExtra(EXTRA_BLOCK_ID)?.let { id ->
                r?.log(id, BlockLog(intent.getIntExtra(EXTRA_REPS, -1).takeIf { it >= 0 },
                    intent.getBooleanExtra(EXTRA_TOO_HARD, false), intent.getBooleanExtra(EXTRA_PAIN, false),
                    intent.getBooleanExtra(EXTRA_TOO_EASY, false)))
            }
            else -> if (r == null) { SessionBus.abortStarting(); stopSelf() }
        }
        return START_NOT_STICKY
    }

    private fun recover(intent: Intent) {
        if (runner != null) return
        val c = CheckpointStore.read(this)
        if (c == null) { SessionBus.abortStarting(); stopSelf(); return }
        newRunner().recover(c, intent.getBooleanExtra(EXTRA_RESUME, true))
    }

    private fun start(intent: Intent) {
        if (runner != null) return // one session at a time
        val plan = intent.getStringExtra(EXTRA_PLAN)!!
        val sid = intent.getStringExtra(EXTRA_SESSION_ID)!!
        newRunner().start(plan, sid)
    }

    private fun newRunner(): SessionRunner {
        createChannel()
        ServiceCompat.startForeground(this, NOTIF, notification("Starting…", false), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        val e = Executors.newSingleThreadScheduledExecutor()
        exec = e
        return SessionRunner(this, this, e.asCoroutineDispatcher(), android.os.SystemClock::elapsedRealtime).also { runner = it }
    }

    // ---- SessionHost ----
    override fun keepAwakeStart(maxMs: Long) {
        wake = (getSystemService(Context.POWER_SERVICE) as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "workout:session")
        wake?.acquire(maxMs)
    }

    override fun keepAwakeEnsure() { if (wake?.isHeld == false) wake?.acquire(4 * 3600_000L) }

    override fun audioInterrupted(): Boolean {
        val mode = (getSystemService(Context.AUDIO_SERVICE) as AudioManager).mode
        return mode == AudioManager.MODE_IN_CALL || mode == AudioManager.MODE_IN_COMMUNICATION || mode == AudioManager.MODE_RINGTONE
    }

    override fun onBlockChanged(s: SessionState) = updateNotification(s)

    override fun onFinished() {
        exec?.shutdown()
        releaseAll()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun logWarn(message: String) { android.util.Log.w("WorkoutSession", message) }
    override fun logError(message: String, t: Throwable?) { android.util.Log.e("WorkoutSession", message, t) }

    private fun releaseAll() {
        try { if (wake?.isHeld == true) wake?.release() } catch (_: Exception) {}
        if (runner != null) cues.release()
    }

    override fun onDestroy() { runner?.close(); exec?.shutdownNow(); releaseAll(); super.onDestroy() }

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
        val t = "${CueText.label(s.plan, b.id, SessionBus.names)}  (${s.blockIndex + 1}/${s.plan.blocks.size})" + if (s.phase == Phase.PAUSED) " — paused" else ""
        getSystemService(NotificationManager::class.java).notify(NOTIF, notification(t, s.phase == Phase.PAUSED))
    }
}
