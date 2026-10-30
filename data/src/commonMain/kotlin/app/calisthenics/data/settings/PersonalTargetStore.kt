package app.calisthenics.data.settings


import app.calisthenics.domain.model.Catalog
import app.calisthenics.domain.model.PersonalLevelTargets
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

object PersonalTargetStore {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val serializer = MapSerializer(String.serializer(), PersonalLevelTargets.serializer())
    fun load(ctx: SettingsProvider): Map<String, PersonalLevelTargets> = runCatching {
        json.decodeFromString(serializer, ctx.legacy("personal_targets").getString("values", "{}")!!)
    }.getOrDefault(emptyMap())
    fun saveAll(ctx: SettingsProvider, values: Map<String, PersonalLevelTargets>) {
        ctx.legacy("personal_targets").edit().putString("values", json.encodeToString(serializer, values)).apply()
    }
    fun save(ctx: SettingsProvider, exerciseId: String, values: List<Int>, authored: Catalog, now: Long): PersonalLevelTargets {
        val cap = authored.policyForVariation(exerciseId)?.maxRepsPerSet
        require(cap == null || values.all { it <= cap }) { "This exercise has a prescribed limit of $cap reps" }
        val old = load(ctx)
        val target = PersonalLevelTargets((old[exerciseId]?.revision ?: 0) + 1, values, now)
        saveAll(ctx, old + (exerciseId to target))
        return target
    }
    /** Reset remains a revision so old streaks cannot satisfy the restored definition. */
    fun reset(ctx: SettingsProvider, catalog: Catalog, exerciseId: String, now: Long) {
        val authored = catalog.copy(personalTargets = emptyMap()).policyForVariation(exerciseId) ?: return
        save(ctx, exerciseId, authored.tiers.sortedBy { it.index }.map { it.target.value }, catalog.copy(personalTargets = emptyMap()), now)
    }
    fun effective(ctx: SettingsProvider, catalog: Catalog): Catalog = catalog.copy(personalTargets = load(ctx))
}
