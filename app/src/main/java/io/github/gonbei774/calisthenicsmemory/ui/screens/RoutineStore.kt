// Usual plan, app-private JSON. Saving swaps creates a NEW revision (old plan snapshots are untouched).
package io.github.gonbei774.calisthenicsmemory.ui.screens

import android.content.Context
import app.calisthenics.domain.model.Routine
import app.calisthenics.domain.routine.StarterRoutine
import kotlinx.serialization.json.Json

object RoutineStore {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    fun load(ctx: Context): Routine = try {
        ctx.getSharedPreferences("routine", Context.MODE_PRIVATE).getString("r", null)
            ?.let { json.decodeFromString(Routine.serializer(), it) } ?: StarterRoutine.routine
    } catch (_: Exception) { StarterRoutine.routine }
    fun save(ctx: Context, r: Routine) {
        ctx.getSharedPreferences("routine", Context.MODE_PRIVATE).edit().putString("r", json.encodeToString(Routine.serializer(), r)).apply()
    }
}
