// V18: the routines saved from the Preview, as JSON in app-private prefs (the backup carries them too).
package io.github.gonbei774.calisthenicsmemory.ui.screens

import android.content.Context
import app.calisthenics.domain.routine.SavedRoutine
import app.calisthenics.domain.routine.deleteSaved
import app.calisthenics.domain.routine.upsertSaved
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

object SavedRoutineStore {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val ser = ListSerializer(SavedRoutine.serializer())
    private fun p(ctx: Context) = ctx.getSharedPreferences("saved_routines", Context.MODE_PRIVATE)

    fun load(ctx: Context): List<SavedRoutine> = try { p(ctx).getString("list", null)?.let { json.decodeFromString(ser, it) } ?: emptyList() } catch (_: Exception) { emptyList() }
    fun get(ctx: Context, id: String?): SavedRoutine? = id?.let { i -> load(ctx).firstOrNull { it.id == i } }
    fun saveAll(ctx: Context, list: List<SavedRoutine>) { p(ctx).edit().putString("list", json.encodeToString(ser, list)).commit() }
    fun upsert(ctx: Context, r: SavedRoutine) = saveAll(ctx, upsertSaved(load(ctx), r))
    fun delete(ctx: Context, id: String) = saveAll(ctx, deleteSaved(load(ctx), id))
}
