package io.github.gonbei774.calisthenicsmemory.ui.screens

import android.content.Context
import app.calisthenics.domain.model.Catalog
import app.calisthenics.domain.model.PersonalLevelTargets
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

object PersonalTargetStore {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val serializer = MapSerializer(String.serializer(), PersonalLevelTargets.serializer())
    fun load(ctx: Context): Map<String, PersonalLevelTargets> = runCatching {
        json.decodeFromString(serializer, ctx.getSharedPreferences("personal_targets", Context.MODE_PRIVATE).getString("values", "{}")!!)
    }.getOrDefault(emptyMap())
    fun saveAll(ctx: Context, values: Map<String, PersonalLevelTargets>) {
        ctx.getSharedPreferences("personal_targets", Context.MODE_PRIVATE).edit().putString("values", json.encodeToString(serializer, values)).apply()
    }
    fun save(ctx: Context, exerciseId: String, values: List<Int>): PersonalLevelTargets {
        val authored = ctx.assets.open("catalog.json").bufferedReader().use { app.calisthenics.domain.content.parseCatalog(it.readText()) }
        val cap = authored.policyForVariation(exerciseId)?.maxRepsPerSet
        require(cap == null || values.all { it <= cap }) { "This exercise has a prescribed limit of $cap reps" }
        val old = load(ctx)
        val target = PersonalLevelTargets((old[exerciseId]?.revision ?: 0) + 1, values, System.currentTimeMillis())
        saveAll(ctx, old + (exerciseId to target))
        return target
    }
    /** Reset remains a revision so old streaks cannot satisfy the restored definition. */
    fun reset(ctx: Context, catalog: Catalog, exerciseId: String) {
        val authored = catalog.copy(personalTargets = emptyMap()).policyForVariation(exerciseId) ?: return
        save(ctx, exerciseId, authored.tiers.sortedBy { it.index }.map { it.target.value })
    }
    fun effective(ctx: Context, catalog: Catalog): Catalog = catalog.copy(personalTargets = load(ctx))
}
