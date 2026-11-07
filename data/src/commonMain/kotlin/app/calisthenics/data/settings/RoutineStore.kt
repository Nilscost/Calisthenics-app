// Usual plan, app-private JSON. Saving swaps creates a NEW revision (old plan snapshots are untouched).
package app.calisthenics.data.settings


import app.calisthenics.domain.model.Routine
import app.calisthenics.domain.routine.StarterRoutine
import kotlinx.serialization.json.Json

object RoutineStore {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    fun load(ctx: SettingsProvider): Routine = try {
        ctx.legacy("routine").getString("r", null)
            ?.let { json.decodeFromString(Routine.serializer(), it) } ?: StarterRoutine.routine
    } catch (_: Exception) { StarterRoutine.routine }
    fun save(ctx: SettingsProvider, r: Routine) {
        ctx.legacy("routine").edit().putString("r", json.encodeToString(Routine.serializer(), r)).apply()
    }
}
