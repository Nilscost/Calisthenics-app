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
    /** slot -> level 1..5, the - / + stepper. Counts towards progress only when it equals your own level. */
    val tierOverrides: Map<String, Int> = emptyMap(),
    val removed: Set<String> = emptySet(),
    val added: List<AddedSlot> = emptyList(),
    /** slot -> a fixed stretch for its break in every set. */
    val stretchPicks: Map<String, String> = emptyMap(),
    /** slot -> extra stretches after its break. */
    val extraStretches: Map<String, List<String>> = emptyMap(),
) {
    fun isEmpty() = this == PlanEdits()
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
    (v.kind == Kind.REPS || v.kind == Kind.HOLD) && catalog.policyFor(v) != null && isAvailable(v, profile)

/** D1: the whole progression of the exercise's own family, easier to harder, usable with this equipment. The current one is included. */
fun swapFamilyOptions(catalog: Catalog, currentId: String, profile: EquipmentProfile): List<ExerciseVariation> {
    val cur = catalog.variation(currentId) ?: return emptyList()
    return catalog.variations.filter { it.familyId == cur.familyId && (usableStrength(catalog, it, profile) || it.id == currentId) }
        .sortedWith(compareBy({ it.difficultyRank }, { it.id }))
}

/** D1: "Other types": every other movement type (the Progress tabs) with its whole progression, usable with this equipment. */
fun swapOtherTypes(catalog: Catalog, currentId: String, profile: EquipmentProfile): List<Pair<TreeTab, List<ExerciseVariation>>> {
    val cur = catalog.variation(currentId) ?: return emptyList()
    return TreeTabs.all.filter { it.familyIds.isNotEmpty() }.mapNotNull { tab ->
        val list = catalog.variations.filter { it.familyId in tab.familyIds && it.familyId != cur.familyId && usableStrength(catalog, it, profile) }.sortedWith(compareBy({ it.familyId }, { it.difficultyRank }, { it.id }))
        if (list.isEmpty()) null else tab to list
    }
}

/** R14: exercises that can be added (not already in the plan), grouped by movement type. */
fun addableExercises(catalog: Catalog, profile: EquipmentProfile, inPlan: Set<String>): List<Pair<TreeTab, List<ExerciseVariation>>> =
    TreeTabs.all.filter { it.familyIds.isNotEmpty() }.mapNotNull { tab ->
        val list = catalog.variations.filter { it.familyId in tab.familyIds && it.id !in inPlan && usableStrength(catalog, it, profile) }.sortedWith(compareBy({ it.familyId }, { it.difficultyRank }, { it.id }))
        if (list.isEmpty()) null else tab to list
    }

/** R14: stretches that can be picked for a break or added after it (STRETCH kind, usable with this equipment). */
fun stretchChoices(catalog: Catalog, profile: EquipmentProfile): List<ExerciseVariation> =
    catalog.variations.filter { it.kind == Kind.STRETCH && isAvailable(it, profile) }.sortedBy { it.name }

/** D3: the level stepper moves between levels 1..5 of the exercise and never leaves them. */
fun stepLevel(current: Int, delta: Int, levels: Int = 5): Int = (current + delta).coerceIn(1, levels)
