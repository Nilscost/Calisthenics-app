// V18: the routines saved from the Preview, as JSON in app-private prefs (the backup carries them too).
package app.calisthenics.data.settings


import app.calisthenics.domain.routine.SavedRoutine
import app.calisthenics.domain.routine.deleteSaved
import app.calisthenics.domain.routine.upsertSaved
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

object SavedRoutineStore {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val ser = ListSerializer(SavedRoutine.serializer())
    private fun p(ctx: SettingsProvider) = ctx.legacy("saved_routines")

    fun load(ctx: SettingsProvider): List<SavedRoutine> = try { p(ctx).getString("list", null)?.let { json.decodeFromString(ser, it) } ?: emptyList() } catch (_: Exception) { emptyList() }
    fun get(ctx: SettingsProvider, id: String?): SavedRoutine? = id?.let { i -> load(ctx).firstOrNull { it.id == i } }
    fun saveAll(ctx: SettingsProvider, list: List<SavedRoutine>) { p(ctx).edit().putString("list", json.encodeToString(ser, list)).commit() }
    fun upsert(ctx: SettingsProvider, r: SavedRoutine) = saveAll(ctx, upsertSaved(load(ctx), r))
    fun delete(ctx: SettingsProvider, id: String) = saveAll(ctx, deleteSaved(load(ctx), id))
}
