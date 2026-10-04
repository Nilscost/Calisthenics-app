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
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.media.SoundPool
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import io.github.gonbei774.calisthenicsmemory.MainActivity
import io.github.gonbei774.calisthenicsmemory.R
import java.io.File
import java.util.Locale
import java.util.concurrent.CountDownLatch
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
        const val EXTRA_SPEAK = "spike.speak"            // Boolean: speak each cue with offline TTS
        const val EXTRA_DUCK = "spike.duck"              // Boolean: request MAY_DUCK focus around each cue

        /** G1 pass thresholds, single source of truth (spec A02): each cue <= 1 s late, total <= 2 s. */
        const val MAX_CUE_DELTA_MS = 1_000L
        const val MAX_TOTAL_DELTA_MS = 2_000L

        const val LOG_DIR_NAME = "spike"
        const val DONE_MARKER = "spike_done.txt"
    }

    private var executor: java.util.concurrent.ScheduledExecutorService? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var soundPool: SoundPool? = null
    private var cueId = 0
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var speak = false
    private var duck = false
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null
    private val deltas = java.util.Collections.synchronizedList(ArrayList<Long>())
    private val ttsOk = java.util.concurrent.atomic.AtomicInteger()
    private val ttsFail = java.util.concurrent.atomic.AtomicInteger()
    private val focusDenied = java.util.concurrent.atomic.AtomicInteger()
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
                speak = intent?.getBooleanExtra(EXTRA_SPEAK, false) ?: false
                duck = intent?.getBooleanExtra(EXTRA_DUCK, false) ?: false
                // Pass the type via ServiceCompat (minSdk 26 < the API-29
                // 3-arg overload): on targetSdk 34+ a missing/mismatched
                // foregroundServiceType throws
                // MissingForegroundServiceTypeException and would kill the
                // timer the moment it starts (G1 blocker).
                ServiceCompat.startForeground(
                    this,
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

        deltas.clear(); ttsOk.set(0); ttsFail.set(0); focusDenied.set(0)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Audio cue. The spike used to start t=0 before the SoundPool finished loading,
        // so the first beep could be silent. Now t=0 starts only after load completes.
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val loaded = CountDownLatch(1)
        var loadOk = false
        soundPool = SoundPool.Builder().setMaxStreams(4).setAudioAttributes(attrs).build().also { sp ->
            sp.setOnLoadCompleteListener { _, _, status -> loadOk = status == 0; loaded.countDown() }
            cueId = sp.load(this, R.raw.start_cue, 1)
        }

        // Offline TTS preflight (owner-approved extra G1 check).
        val ttsInit = CountDownLatch(1)
        var ttsStatus = -1
        if (speak) {
            tts = TextToSpeech(this) { st -> ttsStatus = st; ttsInit.countDown() }
        } else ttsInit.countDown()

        // Keep the CPU awake so the background thread is not frozen mid-run.
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SpikeSessionService::timer")
        wakeLock?.acquire(blocks * (workMs + restMs) + 60_000L)

        executor = Executors.newSingleThreadScheduledExecutor()
        val ex = executor!!
        ex.execute {
            loaded.await(5, TimeUnit.SECONDS)
            ttsInit.await(5, TimeUnit.SECONDS)
            var preflight = "PREFLIGHT,soundLoaded=$loadOk"
            if (speak) {
                val engine = tts
                ttsReady = ttsStatus == TextToSpeech.SUCCESS && engine != null
                val avail = if (ttsReady) engine!!.isLanguageAvailable(Locale.US) else -99
                val offlineVoice = if (ttsReady) {
                    try {
                        engine!!.voices.orEmpty().any {
                            it.locale.language == "en" && !it.isNetworkConnectionRequired &&
                                !it.features.orEmpty().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
                        }
                    } catch (_: Exception) { false }
                } else false
                ttsReady = ttsReady && avail >= TextToSpeech.LANG_AVAILABLE
                if (ttsReady) {
                    engine!!.language = Locale.US
                    engine.setAudioAttributes(attrs)
                    engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(id: String?) {}
                        override fun onDone(id: String?) { ttsOk.incrementAndGet(); logFile?.appendText("TTS,$id,ok\n"); releaseFocus() }
                        @Deprecated("api") override fun onError(id: String?) { ttsFail.incrementAndGet(); logFile?.appendText("TTS,$id,error\n"); releaseFocus() }
                    })
                }
                preflight += ",ttsInit=$ttsStatus,langAvail=$avail,offlineEnglishVoice=$offlineVoice,ttsUsable=$ttsReady"
            }
            preflight += ",duck=$duck"
            logFile?.appendText(preflight + "\n")

            // Monotonic baseline, set AFTER everything is ready.
            t0Elapsed = SystemClock.elapsedRealtime()
            var t = 0L
            val planned = ArrayList<Triple<Int, String, Long>>()
            for (i in 0 until blocks) {
                planned.add(Triple(i, "work", t)); t += workMs
                if (i != blocks - 1) { planned.add(Triple(i, "rest", t)); t += restMs }
            }
            for ((idx, phase, offset) in planned) {
                pending.add(ex.schedule({ onBoundary(idx, phase, offset) }, offset, TimeUnit.MILLISECONDS))
            }
            pending.add(ex.schedule({ finishRun(blocks, workMs, restMs, tag) }, t + 3_000L, TimeUnit.MILLISECONDS))
        }
    }

    private fun requestFocus(): Boolean {
        if (!duck) return true
        val am = audioManager ?: return false
        val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setOnAudioFocusChangeListener { }
            .build()
        focusRequest = req
        val granted = am.requestAudioFocus(req) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        if (!granted) focusDenied.incrementAndGet()
        return granted
    }

    private fun releaseFocus() {
        focusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        focusRequest = null
    }

    private fun onBoundary(index: Int, phase: String, expectedOffset: Long) {
        val observed = SystemClock.elapsedRealtime() - t0Elapsed
        val delta = observed - expectedOffset
        deltas.add(delta)
        val focus = requestFocus()
        logFile?.appendText("$index,$phase,$expectedOffset,$observed,$delta,focus=$focus\n")
        // Audible cue so the operator hears transitions (A02 expects cues to continue).
        soundPool?.play(cueId, 1f, 1f, 1, 0, 1f)
        if (speak && ttsReady) {
            tts?.speak(if (phase == "work") "Go. Block ${index + 1}" else "Rest", TextToSpeech.QUEUE_FLUSH, null, "cue-$index-$phase")
        } else if (duck) {
            // No speech: still hold focus briefly, then release (probes ducking by the beep alone).
            executor?.schedule({ releaseFocus() }, 800, TimeUnit.MILLISECONDS)
        }
    }

    private fun finishRun(blocks: Int, workMs: Long, restMs: Long, tag: String) {
        val totalMs = blocks * workMs + (blocks - 1).coerceAtLeast(0) * restMs
        val observedTotal = SystemClock.elapsedRealtime() - t0Elapsed - 3_000L
        val f = logFile
        f?.appendText("TOTAL,final,$totalMs,$observedTotal,${observedTotal - totalMs}\n")
        val expectedCues = blocks + (blocks - 1).coerceAtLeast(0)
        val snap = synchronized(deltas) { ArrayList(deltas) }
        val maxAbs = snap.maxOfOrNull { Math.abs(it) } ?: 0L
        val late = snap.count { it > MAX_CUE_DELTA_MS }
        val pass = snap.size == expectedCues && maxAbs <= MAX_CUE_DELTA_MS && Math.abs(observedTotal - totalMs) <= MAX_TOTAL_DELTA_MS
        File(f?.parentFile, DONE_MARKER).writeText(
            "tag=$tag\nblocks=$blocks\nworkMs=$workMs\nrestMs=$restMs\n" +
            "log=${f?.absolutePath}\ntotalPlannedMs=$totalMs\nobservedTotalMs=$observedTotal\n" +
            "cues=${snap.size}/$expectedCues\nmaxAbsDeltaMs=$maxAbs\ncuesLateOver1s=$late\n" +
            "ttsSpoken=${ttsOk.get()} ttsFailed=${ttsFail.get()} focusDenied=${focusDenied.get()}\n" +
            "timingVerdict=${if (pass) "PASS" else "FAIL"} (owner decides G1)\ndone\n"
        )
        releaseAll()
        stopForeground(Service.STOP_FOREGROUND_REMOVE)
    }

    private fun stopRun() {
        pending.forEach { it.cancel(false) }
        pending.clear()
        releaseAll()
        stopForeground(Service.STOP_FOREGROUND_REMOVE)
    }

    private fun releaseAll() {
        try { executor?.shutdownNow() } catch (_: Exception) {}
        executor = null
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        try { soundPool?.release() } catch (_: Exception) {}
        soundPool = null
        try { tts?.shutdown() } catch (_: Exception) {}
        tts = null
        releaseFocus()
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
