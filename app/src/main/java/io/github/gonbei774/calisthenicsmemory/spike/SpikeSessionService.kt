// T04 runtime-feasibility spike (disposable, per docs/03-workplan.md T04).
//
// WHY A SERVICE-OWNED TIMER:
//   In the real app the countdown + cues are owned by the Compose UI
//   (WorkoutScreen / *ExecutionScreen) and WorkoutTimerService only holds a
//   PARTIAL_WAKE_LOCK + the foreground notification. When the screen is locked
//   AND another app is foreground (exactly A02), the UI composition is no
//   longer active. Whether a timer survives in that state is precisely what
//   G1 must prove empirically. This spike isolates the question by putting the
//   timer ENTIRELY in a foreground service (no UI), with a monotonic clock and
//   a per-cue timestamp log, so the S21 session can measure actual drift.
//
// This is NOT production content. It is a throwaway feasibility probe for G1
// and must not be mistaken for a working workout.
package io.github.gonbei774.calisthenicsmemory.spike

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import io.github.gonbei774.calisthenicsmemory.MainActivity
import io.github.gonbei774.calisthenicsmemory.R
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class SpikeSessionService : Service() {

    companion object {
        const val CHANNEL_ID = "spike_session_channel"
        const val NOTIF_ID = 101

        const val ACTION_RUN = "io.github.gonbei774.calisthenicsmemory.spike.RUN"
        const val ACTION_STOP = "io.github.gonbei774.calisthenicsmemory.spike.STOP"

        // Defaults for the short automated probe. The operator can override via
        // intent extras for a longer manual A02-style run.
        const val EXTRA_BLOCKS = "spike.blocks"          // Int: number of work blocks
        const val EXTRA_WORK_MS = "spike.workMs"         // Long: work duration per block
        const val EXTRA_REST_MS = "spike.restMs"         // Long: rest between blocks
        const val EXTRA_TAG = "spike.tag"                // String: run label for the log

        const val LOG_DIR_NAME = "spike"
        const val DONE_MARKER = "spike_done.txt"
    }

    private var executor: java.util.concurrent.ScheduledExecutorService? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var soundPool: SoundPool? = null
    private var cueId = 0
    private var t0Elapsed = 0L
    private var logFile: File? = null
    private val pending: MutableList<ScheduledFuture<*>> = ArrayList()

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { stopRun(); stopSelf(); return START_NOT_STICKY }
            ACTION_RUN, null -> {
                val blocks = intent?.getIntExtra(EXTRA_BLOCKS, 3) ?: 3
                val workMs = intent?.getLongExtra(EXTRA_WORK_MS, 3_000L) ?: 3_000L
                val restMs = intent?.getLongExtra(EXTRA_REST_MS, 2_000L) ?: 2_000L
                val tag = intent?.getStringExtra(EXTRA_TAG) ?: "auto"
                startForeground(
                    NOTIF_ID,
                    buildNotification(tag),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
                startRun(blocks, workMs, restMs, tag)
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): android.os.IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        releaseAll()
    }

    private fun startRun(blocks: Int, workMs: Long, restMs: Long, tag: String) {
        // Fresh log file per run.
        val dir = File(getExternalFilesDir(null), LOG_DIR_NAME)
        dir.mkdirs()
        val stamp = SystemClock.elapsedRealtime()
        logFile = File(dir, "spike_${stamp}_${tag}.csv")
        logFile!!.writeText("run_tag,block_index,phase,expected_elapsed_ms,observed_elapsed_ms,delta_ms\n")

        // Monotonic baseline. elapsedRealtime() is unaffected by wall-clock/NTP.
        t0Elapsed = SystemClock.elapsedRealtime()

        // Audio cues, loaded once.
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder().setMaxStreams(4).setAudioAttributes(attrs).build()
        cueId = soundPool!!.load(this, R.raw.start_cue, 1)

        // Keep the CPU awake so the background thread is not frozen mid-run.
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SpikeSessionService::timer")
        wakeLock?.acquire(blocks * (workMs + restMs) + 60_000L)

        // Run the schedule on a background thread, independent of any UI.
        executor = Executors.newSingleThreadScheduledExecutor()
        val ex = executor!!
        val planned = ArrayList<Triple<Int, String, Long>>() // (index, phase, offsetMs)
        var t = 0L
        for (i in 0 until blocks) {
            planned.add(Triple(i, "work", t))
            t += workMs
            if (i != blocks - 1) {
                planned.add(Triple(i, "rest", t))
                t += restMs
            }
        }
        for (item in planned) {
            val idx = item.first
            val phase = item.second
            val offset = item.third
            pending.add(
                ex.schedule({ onBoundary(idx, phase, offset) }, offset, TimeUnit.MILLISECONDS)
            )
        }
        // Completion marker a little after the final cue.
        pending.add(ex.schedule({ finishRun(blocks, workMs, restMs, tag) }, t + 1_000L, TimeUnit.MILLISECONDS))
    }

    private fun onBoundary(index: Int, phase: String, expectedOffset: Long) {
        val observed = SystemClock.elapsedRealtime() - t0Elapsed
        val delta = observed - expectedOffset
        logFile?.appendText("$index,$phase,$expectedOffset,$observed,$delta\n")
        // Audible cue so the operator hears transitions (A02 expects cues to continue).
        soundPool?.play(cueId, 1f, 1f, 1, 0, 1f)
    }

    private fun finishRun(blocks: Int, workMs: Long, restMs: Long, tag: String) {
        val totalMs = blocks * workMs + (blocks - 1).coerceAtLeast(0) * restMs
        val observedTotal = SystemClock.elapsedRealtime() - t0Elapsed
        val f = logFile
        f?.appendText("TOTAL,final,$totalMs,$observedTotal,${observedTotal - totalMs}\n")
        File(f?.parentFile, DONE_MARKER).writeText(
            "tag=$tag\nblocks=$blocks\nworkMs=$workMs\nrestMs=$restMs\n" +
            "log=${f?.absolutePath}\ntotalPlannedMs=$totalMs\nobservedTotalMs=$observedTotal\ndone\n"
        )
        releaseAll()
        stopForeground(ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    private fun stopRun() {
        pending.forEach { it.cancel(false) }
        pending.clear()
        releaseAll()
        stopForeground(ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    private fun releaseAll() {
        try { executor?.shutdownNow() } catch (_: Exception) {}
        executor = null
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        try { soundPool?.release() } catch (_: Exception) {}
        soundPool = null
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "T04 runtime feasibility spike (disposable)",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Foreground service that keeps the T04 timer alive. Disposable probe, not a workout."
            setShowBadge(false)
        }
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    private fun buildNotification(tag: String): Notification {
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stop = PendingIntent.getService(
            this, 1,
            Intent(this, SpikeSessionService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("T04 spike running ($tag)")
            .setContentText("Service-owned timer active. Disposable probe — not a workout.")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(open)
            .addAction(0, "Stop spike", stop)
            .build()
    }
}
