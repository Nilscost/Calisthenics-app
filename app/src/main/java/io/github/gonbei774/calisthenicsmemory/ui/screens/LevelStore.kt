// Self-assessed starting steps (ONB-01): variationId -> tier, app-private. Turned into SelfAssessment actions and replayed.
package io.github.gonbei774.calisthenicsmemory.ui.screens

import android.content.Context
import app.calisthenics.domain.model.Catalog
import app.calisthenics.domain.progression.ProgressSnapshot
import app.calisthenics.domain.progression.ProgressionEngine
import app.calisthenics.domain.progression.UserAction
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

object LevelStore {
    private val ser = MapSerializer(String.serializer(), Int.serializer())
    fun load(ctx: Context): Map<String, Int> = try {
        ctx.getSharedPreferences("levels", Context.MODE_PRIVATE).getString("v", null)
            ?.let { Json.decodeFromString(ser, it) } ?: emptyMap()
    } catch (_: Exception) { emptyMap() }
    fun save(ctx: Context, m: Map<String, Int>) {
        ctx.getSharedPreferences("levels", Context.MODE_PRIVATE).edit().putString("v", Json.encodeToString(ser, m)).apply()
    }
    /**
     * W2 (owner): redoing the starting questionnaire restarts the levels from the new answers. This is the moment (epoch ms) of the last save of the
     * questionnaire: the history before it stays visible in History, but no longer raises levels or counts toward streaks. 0 = never reset.
     */
    fun resetAtMs(ctx: Context): Long = try { ctx.getSharedPreferences("levels", Context.MODE_PRIVATE).getLong("resetAtMs", 0L) } catch (_: Exception) { 0L }
    fun markReset(ctx: Context, atMs: Long) { ctx.getSharedPreferences("levels", Context.MODE_PRIVATE).edit().putLong("resetAtMs", atMs).commit() }
    fun snapshot(catalog: Catalog, levels: Map<String, Int>): ProgressSnapshot =
        ProgressionEngine(catalog).replay(emptyList(), levels.map { (v, t) -> app.calisthenics.domain.progression.selfAssessmentFor(v, t) })
}
