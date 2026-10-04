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
    fun snapshot(catalog: Catalog, levels: Map<String, Int>): ProgressSnapshot =
        ProgressionEngine(catalog).replay(emptyList(), levels.map { (v, t) -> UserAction.SelfAssessment(0, v, t) })
}
