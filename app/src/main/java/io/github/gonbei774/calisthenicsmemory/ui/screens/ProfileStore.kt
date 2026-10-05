// U04 (F5): equipment profiles, stored as JSON in app-private prefs. Seeds (Home, Travel) are created on first run.
package io.github.gonbei774.calisthenicsmemory.ui.screens

import android.content.Context
import app.calisthenics.domain.equipment.ProfileEditResult
import app.calisthenics.domain.equipment.ProfileOps
import app.calisthenics.domain.equipment.SeedProfiles
import app.calisthenics.domain.model.EquipmentItem
import app.calisthenics.domain.model.EquipmentProfile
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

object ProfileStore {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val ser = ListSerializer(EquipmentProfile.serializer())
    private fun p(ctx: Context) = ctx.getSharedPreferences("profiles", Context.MODE_PRIVATE)

    fun load(ctx: Context): List<EquipmentProfile> {
        val stored = try { p(ctx).getString("list", null)?.let { json.decodeFromString(ser, it) } } catch (_: Exception) { null }
        if (!stored.isNullOrEmpty()) return stored
        // First run (or unreadable): seeds. The old loose "high bar" toggle becomes a real item on Home.
        val seeds = if (ModeStore.takeLegacyHighBar(ctx))
            SeedProfiles.all.map { if (it.id == "home") it.copy(items = it.items + EquipmentItem("high-bar")) else it }
        else SeedProfiles.all
        save(ctx, seeds)
        return seeds
    }

    fun save(ctx: Context, list: List<EquipmentProfile>) {
        p(ctx).edit().putString("list", json.encodeToString(ser, list)).commit()
    }

    fun upsert(ctx: Context, profile: EquipmentProfile): List<EquipmentProfile> =
        ProfileOps.upsert(load(ctx), profile).also { save(ctx, it) }

    /** Refuses to delete the last profile; if the selected profile goes, the first remaining one is selected. */
    fun delete(ctx: Context, id: String): ProfileEditResult {
        val prefs = PrefsStore.load(ctx)
        val r = ProfileOps.delete(load(ctx), id, prefs.selectedProfileId)
        if (r is ProfileEditResult.Ok) {
            save(ctx, r.profiles)
            if (r.selectedId != prefs.selectedProfileId) PrefsStore.save(ctx, prefs.copy(selectedProfileId = r.selectedId))
        }
        return r
    }

    /** The selected profile; falls back to the first one if the saved id no longer exists. */
    fun selected(ctx: Context): EquipmentProfile {
        val list = load(ctx)
        val id = PrefsStore.load(ctx).selectedProfileId
        return list.firstOrNull { it.id == id } ?: list.first()
    }

    fun select(ctx: Context, id: String) { PrefsStore.save(ctx, PrefsStore.load(ctx).copy(selectedProfileId = id)) }
}
