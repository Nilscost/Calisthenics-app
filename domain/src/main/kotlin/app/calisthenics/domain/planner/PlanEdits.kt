// V16 (R10-R15, D1, D3, D6): what the person changes in the Preview, today only. Plain data so a saved routine can keep it (V18).
package app.calisthenics.domain.planner

import app.calisthenics.domain.equipment.isAvailable
import app.calisthenics.domain.model.*
import app.calisthenics.domain.tree.TreeTab
import app.calisthenics.domain.tree.TreeTabs
import kotlinx.serialization.Serializable

/** An exercise added with the "+" between blocks: it goes right after [afterSlotId] (null = at the start). */
@Serializable
data class AddedSlot(val afterSlotId: String?, val slot: RoutineSlot)

@Serializable
data class PlanEdits(
    /** slot -> exercise (a swap; a different movement type triggers the "not the same movement" warning). */
    val swaps: Map<String, String> = emptyMap(),
    /** slot -> selected app level 1..5, the - / + stepper. Current-version confirmed results can establish the level actually demonstrated. */
    val tierOverrides: Map<String, Int> = emptyMap(),
    val removed: Set<String> = emptySet(),
    val added: List<AddedSlot> = emptyList(),
    /** slot -> a fixed stretch for its break in every set. */
    val stretchPicks: Map<String, String> = emptyMap(),
    /** slot -> extra stretches after its break. */
    val extraStretches: Map<String, List<String>> = emptyMap(),
    /** Detailed edit (D6): a stretch for one set only, key "<set>:<slot>". */
    val roundStretchPicks: Map<String, String> = emptyMap(),
    /** Detailed edit (D3): a typed number for one set ("<set>:<slot>") or every set ("<slot>"). Legacy arbitrary targets remain ineligible for demonstrated-level evidence. */
    val freeTargets: Map<String, Int> = emptyMap(),
    /** V21: the progression rule chosen in the Preview (null = the routine's own). */
    val rule: ProgressionRule? = null,
    /** R7-T13: minutes of a timed cool-down chosen in the Preview (null = the setting). */
    val cooldownMinutes: Int? = null,
    /** R7-T13: the cool-down stretches chosen by hand in the Preview, in order (null = automatic). */
    val cooldownStretches: List<String>? = null,
    /** R7.2-T04 (F5): cool-down stretches the person removed (applied after the automatic or hand-picked list). */
    val cooldownRemoved: Set<String> = emptySet(),
    /** R7.2-T04 (F5): cool-down stretches the person added with the +, in order, after the others. */
    val cooldownAdded: List<String> = emptyList(),
    /** R8.2-T03 (G3): the conditioning finisher's minutes (null = the setting) and the exercises removed from it. */
    val conditioningMinutes: Int? = null,
    val conditioningRemoved: Set<String> = emptySet(),
    /** R8.2-T03 (G3): warm-up items the person removed in the Preview. */
    val warmupRemoved: Set<String> = emptySet(),
    /** Deliberate app-level-5 extensions, keyed by slot (or set:slot), unlike arbitrary legacy free targets. */
    val levelFiveTargets: Map<String, Int> = emptyMap(),
    /** Mode revision at which these extras were last explicitly edited (old drafts decode as revision 0). */
    val extraStretchModeRevision: Long = 0L,
    /** slot -> the exercise a manual level / typed number was set on; the override is ignored for any other exercise in that slot (a rotated alternate). Entries without it (saved routines) apply as before. */
    val overrideVariations: Map<String, String> = emptyMap(),
    /** The day (epoch day) a manual level / typed number was last set; 0 = none or unknown (an older draft). */
    val overrideEpochDay: Long = 0L,
) {
    fun isEmpty() = this == PlanEdits()
    fun hasLevelOverrides() = tierOverrides.isNotEmpty() || levelFiveTargets.isNotEmpty() || freeTargets.isNotEmpty()
}

/** A manual level or typed number is for the next workout only: [expireLevelOverrides] clears them when set on an earlier day (or on an unknown day). */
fun PlanEdits.expireLevelOverrides(todayEpochDay: Long): PlanEdits =
    if (!hasLevelOverrides() || overrideEpochDay == todayEpochDay) this
    else copy(tierOverrides = emptyMap(), levelFiveTargets = emptyMap(), freeTargets = emptyMap(), overrideVariations = emptyMap(), overrideEpochDay = 0L)

/** Records that the manual level / typed number of [slotId] was set on [variationId] today. */
fun PlanEdits.stampOverride(slotId: String, variationId: String?, todayEpochDay: Long): PlanEdits =
    copy(overrideVariations = if (variationId != null) overrideVariations + (slotId to variationId) else overrideVariations, overrideEpochDay = todayEpochDay)

/**
 * Swaps the cool-down stretch at [index] of [current] (the stretches the plan shows now, in order) for [pick], in place.
 * The list becomes the person's own, and neither the swapped-away stretch nor the pick is left in the added or removed sets,
 * so the old one cannot come back (it would otherwise survive in [PlanEdits.cooldownAdded]) and the new one cannot be listed twice.
 */
fun PlanEdits.swapCooldownStretch(current: List<String>, index: Int, pick: String): PlanEdits {
    if (index !in current.indices) return this
    val old = current[index]
    val list = current.toMutableList().also { it[index] = pick }.distinct()
    return copy(cooldownStretches = list, cooldownAdded = cooldownAdded.filter { it != old && it != pick }, cooldownRemoved = cooldownRemoved - pick)
}

/** The routine with the removed slots taken out and the added ones inserted after the slot they were added under. */
fun applyEdits(routine: Routine, e: PlanEdits): Routine {
    if (e.removed.isEmpty() && e.added.isEmpty()) return routine
    val kept = routine.slots.filter { it.id !in e.removed }.toMutableList()
    for (a in e.added) {
        val at = a.afterSlotId?.let { id -> kept.indexOfFirst { it.id == id } }?.takeIf { it >= 0 }?.plus(1) ?: if (a.afterSlotId == null) 0 else kept.size
        kept.add(at.coerceIn(0, kept.size), a.slot)
    }
    return routine.copy(slots = kept)
}

/** A slot for an exercise added today. The id is stable per exercise so the same exercise cannot be added twice by accident. */
fun slotFor(v: ExerciseVariation, n: Int = 1) = RoutineSlot("add-${v.id}" + if (n > 1) "-$n" else "", v.patterns.first(), v.areas.firstOrNull() ?: Area.UPPER_BODY, v.id)

private fun usableStrength(catalog: Catalog, v: ExerciseVariation, profile: EquipmentProfile) =
    !v.routineOnly && (v.kind == Kind.REPS || v.kind == Kind.HOLD) && catalog.policyFor(v) != null && isAvailable(v, profile)

/** Reviewed main spine for the level stepper; Other ways keeps its separate family browser.
 * An alternate occupies its main anchor. Missing equipment uses an explicit same-level
 * alternate or stops the stepper; it never skips a required rung by sorting past it.
 */
fun mainPathOptions(c: Catalog, currentId: String, profile: EquipmentProfile, goalId: String? = null): List<ExerciseVariation> {
    if (c.progressionRoutes.isEmpty()) return swapFamilyOptions(c, currentId, profile)
    val current = c.variation(currentId) ?: return emptyList()
    val route = c.mainRouteFor(currentId, goalId) ?: return listOf(current)
    val anchor = if (currentId in route.mainPath) currentId else route.members.firstOrNull {
        it.variationId == currentId && it.role == ProgressionRole.ALTERNATE
    }?.atVariationId ?: return listOf(current)
    val at = route.mainPath.indexOf(anchor)
    if (at < 0) return listOf(current)
    fun option(id: String): ExerciseVariation? {
        if (id == anchor) return current
        val ids = listOf(id) + route.members.filter { it.role == ProgressionRole.ALTERNATE && it.atVariationId == id }.map { it.variationId }
        return ids.mapNotNull(c::variation).firstOrNull { !it.demonstrationHold && isAvailable(it, profile) && c.policyFor(it) != null }
    }
    val before = route.mainPath.take(at).asReversed().map(::option).takeWhile { it != null }.filterNotNull().asReversed()
    val after = route.mainPath.drop(at + 1).map(::option).takeWhile { it != null }.filterNotNull()
    return before + current + after
}

/** D1: the whole progression of the exercise's own family, easier to harder, usable with this equipment. The current one is included. */
fun swapFamilyOptions(catalog: Catalog, currentId: String, profile: EquipmentProfile): List<ExerciseVariation> {
    val cur = catalog.variation(currentId) ?: return emptyList()
    return catalog.variations.filter { it.familyId == cur.familyId && (usableStrength(catalog, it, profile) || it.id == currentId) }
        .sortedWith(compareBy({ it.difficultyRank }, { it.id }))
}

/** D1 / R7.2-T02 (F3, O3): "Other types": every other movement type (the Progress tabs) with its whole progression, usable with this equipment.
 *  The current exercise's own tab comes first (its other families, for example the kettlebell and dumbbell rows for an inverted row); nothing is dropped. */
fun swapOtherTypes(catalog: Catalog, currentId: String, profile: EquipmentProfile): List<Pair<TreeTab, List<ExerciseVariation>>> {
    val cur = catalog.variation(currentId) ?: return emptyList()
    val groups = TreeTabs.all.filter { it.familyIds.isNotEmpty() }.mapNotNull { tab ->
        val list = catalog.variations.filter { it.familyId in tab.familyIds && it.familyId != cur.familyId && usableStrength(catalog, it, profile) }.sortedWith(compareBy({ it.familyId }, { it.difficultyRank }, { it.id }))
        if (list.isEmpty()) null else tab to list
    }
    return groups.sortedBy { (tab, _) -> if (cur.familyId in tab.familyIds) 0 else 1 }   // stable: the other tabs keep their order
}

/** R14: exercises that can be added (not already in the plan), grouped by movement type. */
fun addableExercises(catalog: Catalog, profile: EquipmentProfile, inPlan: Set<String>): List<Pair<TreeTab, List<ExerciseVariation>>> =
    TreeTabs.all.filter { it.familyIds.isNotEmpty() }.mapNotNull { tab ->
        val list = catalog.variations.filter { it.familyId in tab.familyIds && it.id !in inPlan && usableStrength(catalog, it, profile) }.sortedWith(compareBy({ it.familyId }, { it.difficultyRank }, { it.id }))
        if (list.isEmpty()) null else tab to list
    }

/** R14: stretch and mobility drills that can be picked for a break or added after it, usable with this equipment. */
fun stretchChoices(catalog: Catalog, profile: EquipmentProfile): List<ExerciseVariation> =
    catalog.variations.filter { it.kind in setOf(Kind.STRETCH, Kind.MOBILITY) && !it.demonstrationHold && isAvailable(it, profile) }.sortedBy { it.name }

/** D3: the level stepper moves between levels 1..5 of the exercise and never leaves them. */
fun stepLevel(current: Int, delta: Int, levels: Int = 5): Int = (current + delta).coerceIn(1, levels)
