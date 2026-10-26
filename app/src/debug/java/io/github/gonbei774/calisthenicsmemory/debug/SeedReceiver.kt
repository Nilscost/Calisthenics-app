// V00b (debug builds only): loads a fixture backup + self-assessed levels so the screenshot flows can show
// History and Progress with data. Trigger:
//   adb shell am broadcast -n app.calisthenics.personal/io.github.gonbei774.calisthenicsmemory.debug.SeedReceiver \
//       -a app.calisthenics.personal.debug.SEED -f 0x20
// Times in the fixture are rebased so its newest session is about two hours old.
// Optional extra `--ei keep N` (R7.2-T08): seed only the N newest sessions, to show the History chart with one, two or seven dots.
package io.github.gonbei774.calisthenicsmemory.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import app.calisthenics.domain.backup.ImportResult
import app.calisthenics.domain.backup.importBackup
import io.github.gonbei774.calisthenicsmemory.ui.screens.BackupApply
import io.github.gonbei774.calisthenicsmemory.ui.screens.LevelStore
import io.github.gonbei774.calisthenicsmemory.ui.screens.OnboardingStore
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class SeedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext
        Thread {
            var code = 0
            try {
                val text = app.assets.open("seed/backup.json").use { it.readBytes().decodeToString() }
                val payload = (importBackup(text) as? ImportResult.Ok)?.payload ?: error("seed backup rejected")
                val newest = payload.sessions.maxOf { it.startedAtEpochMs }
                val shift = System.currentTimeMillis() - 2 * 3_600_000L - newest
                val keep = intent.getIntExtra("keep", 0)
                val kept = if (keep > 0) payload.sessions.sortedByDescending { it.startedAtEpochMs }.take(keep).map { it.sessionId }.toSet() else payload.sessions.map { it.sessionId }.toSet()
                val shifted = payload.copy(
                    sessions = payload.sessions.filter { it.sessionId in kept }.map { it.copy(startedAtEpochMs = it.startedAtEpochMs + shift, endedAtEpochMs = it.endedAtEpochMs?.plus(shift)) },
                    feedback = payload.feedback.filter { it.sessionId in kept }.map { it.copy(createdAtEpochMs = it.createdAtEpochMs + shift) },
                    events = payload.events.map { it.copy(atEpochMs = it.atEpochMs + shift) },
                )
                val added = runBlocking { BackupApply.restore(app, shifted) }
                val levels = Json.decodeFromString(MapSerializer(String.serializer(), Int.serializer()),
                    app.assets.open("seed/levels.json").use { it.readBytes().decodeToString() })
                LevelStore.save(app, levels, undated = true)
                OnboardingStore.setDone(app)
                Log.i("SeedReceiver", "seeded: $added sessions, ${levels.size} levels")
                code = 1
            } catch (e: Throwable) {
                Log.e("SeedReceiver", "seed failed", e)
            } finally {
                pending.resultCode = code
                pending.finish()
            }
        }.start()
    }
}
