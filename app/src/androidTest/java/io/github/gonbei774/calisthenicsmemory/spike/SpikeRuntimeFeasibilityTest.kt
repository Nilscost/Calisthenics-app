// T04 runtime-feasibility spike — instrumented driver (per docs/03-workplan.md T04).
//
// Runs a SHORT, fully service-owned timer on-device (no UI), waits for the
// service to write its completion marker, then reports completion + cue drift.
// This is a disposable probe for G1: on the S21 (Do Not Disturb, screen off)
// it answers "did the foreground-service timer survive the lock, and how much
// did it drift?" A timeout here (no marker within the window) is itself the
// evidence G1 needs — it means the service was not kept alive in that state.
package io.github.gonbei774.calisthenicsmemory.spike

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class SpikeRuntimeFeasibilityTest {

    // Short probe: 3 work blocks of 1.5s with 1s rest between -> 3*1500 + 2*1000 = 6500ms.
    private val blocks = 3
    private val workMs = 1_500L
    private val restMs = 1_000L

    @Test
    fun serviceOwnedTimerCompletesWithBoundedDrift() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()

        // Record the notification permission state so the run log shows whether
        // the foreground notification can be displayed. (A test cannot grant a
        // runtime permission to itself on Android 13+; the real grant happens
        // via the app UI or system Settings.)
        val notifPerm = android.content.pm.PackageManager.PERMISSION_GRANTED ==
            androidx.core.content.ContextCompat.checkSelfPermission(
                ctx,
                "android.permission.POST_NOTIFICATIONS"
            )
        System.out.println("[spike] POST_NOTIFICATIONS granted=$notifPerm")

        // Remove any stale marker/log so we measure this run only.
        val dir = File(ctx.getExternalFilesDir(null), SpikeSessionService.LOG_DIR_NAME)
        dir.mkdirs()
        File(dir, SpikeSessionService.DONE_MARKER).delete()

        val intent = android.content.Intent(ctx, SpikeSessionService::class.java)
            .setAction(SpikeSessionService.ACTION_RUN)
        intent.putExtra(SpikeSessionService.EXTRA_BLOCKS, blocks)
        intent.putExtra(SpikeSessionService.EXTRA_WORK_MS, workMs)
        intent.putExtra(SpikeSessionService.EXTRA_REST_MS, restMs)
        ContextCompat.startForegroundService(ctx, intent)

        // Wait for the completion marker (generous: probe is ~6.5s + 1s).
        val marker = File(dir, SpikeSessionService.DONE_MARKER)
        val deadline = System.currentTimeMillis() + 30_000L
        var markerText: String? = null
        while (System.currentTimeMillis() < deadline) {
            if (marker.exists()) { markerText = marker.readText(); break }
            Thread.sleep(250)
        }

        Assert.assertTrue(
            "T04 spike did not complete on-device within 30s. On the S21 this is " +
            "evidence the foreground service was not kept alive in the tested " +
            "background/lock state (see docs/adr/0003-runtime.md).",
            markerText != null
        )

        // Find the CSV the service wrote.
        val log = dir.listFiles { f -> f.name.startsWith("spike_") && f.name.endsWith(".csv") }
            ?.maxByOrNull { it.lastModified() }
        Assert.assertNotNull("No spike CSV log produced", log)

        val rows = log!!.readLines().drop(1) // skip header
        val boundary = rows.filter { it.startsWith("TOTAL") == false }
        val total = rows.firstOrNull { it.startsWith("TOTAL") }

        val deltas = boundary.mapNotNull {
            val p = it.split(",")
            if (p.size < 5) null else p[4].toLongOrNull()
        }

        val maxAbs = deltas.maxOfOrNull { Math.abs(it) } ?: 0L
        val mean = if (deltas.isEmpty()) 0L else deltas.sum() / deltas.size

        System.out.println("[spike] completed. marker=${markerText}")
        System.out.println("[spike] boundary cues=${deltas.size}, maxAbsDeltaMs=$maxAbs, meanDeltaMs=$mean")
        System.out.println("[spike] total row=$total")

        // The probe should emit every planned boundary. Expected = blocks work +
        // (blocks-1) rest cues.
        val expectedCues = blocks + (blocks - 1)
        Assert.assertEquals("expected $expectedCues boundary cues", expectedCues, deltas.size)

        // Loose liveness bound: a healthy service-owned timer should not drift
        // more than a couple seconds per 1.5s boundary under normal conditions.
        // A larger drift (or the timeout above) is the signal G1 records.
        Assert.assertTrue(
            "maxAbsDeltaMs=$maxAbs exceeds loose liveness bound",
            maxAbs < 4_000L
        )
    }
}
