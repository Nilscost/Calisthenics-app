// R7-T08 (D1): how many exercises one round has. The default is the smallest count that makes the session at least 30 minutes
// for the chosen rounds, format and warm-up; focus sessions (upper, lower, core) are filled with more exercises of that area.
// Pure and deterministic, like the planner.
package app.calisthenics.domain.planner

import app.calisthenics.domain.equipment.isAvailable
import app.calisthenics.domain.goals.Goals
import app.calisthenics.domain.model.*
import app.calisthenics.domain.progression.ProgressSnapshot
import app.calisthenics.domain.routine.areasFor
import app.calisthenics.domain.routine.slotsForFocus

/** A session should be at least this long unless the person says otherwise (owner D1). */
const val MIN_SESSION_SECONDS = 30 * 60
/** The stepper's top; the pool of exercises of the focus may end earlier. */
const val MAX_EXERCISES_PER_ROUND = 12

/** Which movement types to add first for each area; later ones follow once the earlier families are in. */
private val extensionPatterns: Map<Area, List<Pattern>> = mapOf(
    Area.UPPER_BODY to listOf(Pattern.PUSH_VERTICAL, Pattern.PULL_HORIZONTAL, Pattern.PULL_VERTICAL, Pattern.PUSH_HORIZONTAL),
    Area.LOWER_BODY to listOf(Pattern.LUNGE, Pattern.HINGE, Pattern.CALF_RAISE, Pattern.SQUAT),
    Area.CORE to listOf(Pattern.CORE_ANTI_ROTATION, Pattern.CORE_ANTI_LATERAL, Pattern.CORE_ANTI_EXTENSION),
)

private fun usable(c: Catalog, v: ExerciseVariation, profile: EquipmentProfile) =
    (v.kind == Kind.REPS || v.kind == Kind.HOLD) && c.policyFor(v) != null && isAvailable(v, profile)

/** The planner refuses an exercise whose recovery would be passive rest while stretching is on; this is the same test, so a filler never makes the plan fail. */
private fun hasBetweenSetsStretch(c: Catalog, v: ExerciseVariation) =
    v.compatibleStretchIds.mapNotNull { c.variation(it) }.any { it.kind == Kind.STRETCH && !it.optIn }

/**
 * Exercises that can be added to [routine] for [focus], best first, one per family: the entry exercise of each family at the
 * person's level (the planner then follows their own progress in that family). Families already in the routine are skipped, so nothing repeats.
 */
fun extensionSlots(c: Catalog, routine: Routine, focus: Set<StrengthFocus>, profile: EquipmentProfile, progress: ProgressSnapshot, stretchOn: Boolean = true): List<RoutineSlot> {
    val areas = areasFor(focus)
    val used = routine.slots.mapNotNull { c.variation(it.preferredVariationId)?.familyId }.toMutableSet()
    val usedIds = routine.slots.map { it.preferredVariationId }.toSet() + routine.slots.mapNotNull { s -> c.variation(s.preferredVariationId)?.let { progress.activeInFamily[it.familyId] } }
    // per area: per pattern, the families that qualify (best entry first)
    fun lists(area: Area): List<List<RoutineSlot>> = extensionPatterns.getValue(area).map { p ->
        c.variations.filter { p in it.patterns && area in it.areas && usable(c, it, profile) && it.id !in usedIds && it.familyId !in used && (!stretchOn || hasBetweenSetsStretch(c, it)) && Goals.unmet(c, it.id, progress).isEmpty() }
            .groupBy { it.familyId }.values.map { fam -> fam.minWith(compareBy({ it.difficultyRank }, { it.id })) }
            .sortedWith(compareBy({ it.difficultyRank }, { it.id }))
            .map { RoutineSlot("ext-${it.familyId}", p, area, it.id) }
    }
    val byArea = Area.entries.filter { it in areas }.map { lists(it) }
    // round robin: first family of each pattern of each area, then the second, ...
    val out = mutableListOf<RoutineSlot>(); val taken = mutableSetOf<String>()
    var pass = 0
    while (true) {
        var any = false
        val maxPatterns = byArea.maxOfOrNull { it.size } ?: 0
        for (pi in 0 until maxPatterns) for (area in byArea) {
            val cand = area.getOrNull(pi)?.getOrNull(pass) ?: continue
            any = true
            if (taken.add(c.variation(cand.preferredVariationId)!!.familyId)) out += cand
        }
        if (!any) break
        pass++
    }
    // Last resort when the new families run out (a small equipment profile): other exercises of the movements already in the session,
    // the ones closest to the level of the exercise they sit beside, so a body-part session can still reach its length. Never an exercise twice.
    val chosenIds = usedIds.toMutableSet().also { ids -> out.forEach { ids += it.preferredVariationId } }
    val anchors = (routine.slots + out).mapNotNull { sl -> c.variation(sl.preferredVariationId)?.takeIf { v -> v.areas.any { it in areas } } }
    repeat(2) {
        for (u in anchors) {
            val cand = c.variations.filter { v ->
                v.familyId == u.familyId && v.id !in chosenIds && usable(c, v, profile) && v.patterns.any { it in u.patterns } &&
                    v.areas.any { it in areas && it in u.areas } && (!stretchOn || hasBetweenSetsStretch(c, v)) && Goals.unmet(c, v.id, progress).isEmpty() &&
                    progress.activeInFamily[v.familyId] != v.id
            }.minWithOrNull(compareBy({ kotlin.math.abs(it.difficultyRank - u.difficultyRank) }, { it.id })) ?: continue
            chosenIds += cand.id
            out += RoutineSlot("ext-${cand.id}", u.patterns.first { it in cand.patterns }, u.areas.first { it in cand.areas && it in areas }, cand.id)
        }
    }
    return out
}

/**
 * The routine with its in-focus slots brought to [count]: more are appended from [extensionSlots], fewer drop the optional ones first (from the end),
 * then the last ones. Slots outside the focus are left alone.
 */
fun withExerciseCount(c: Catalog, routine: Routine, focus: Set<StrengthFocus>, count: Int, profile: EquipmentProfile, progress: ProgressSnapshot, stretchOn: Boolean = true): Routine {
    val inFocus = slotsForFocus(routine, focus)
    val want = count.coerceIn(1, MAX_EXERCISES_PER_ROUND)
    if (want == inFocus.size) return routine
    if (want < inFocus.size) {
        val dropOrder = inFocus.filter { it.optional }.reversed() + inFocus.filter { !it.optional }.reversed()
        val drop = dropOrder.take(inFocus.size - want).map { it.id }.toSet()
        return routine.copy(slots = routine.slots.filter { it.id !in drop })
    }
    val extra = extensionSlots(c, routine, focus, profile, progress, stretchOn).take(want - inFocus.size)
    return routine.copy(slots = routine.slots + extra)
}

/** How many exercises the stepper can reach: what the routine has in this focus plus every filler that exists, up to [MAX_EXERCISES_PER_ROUND]. */
fun maxExercisesPerRound(c: Catalog, routine: Routine, focus: Set<StrengthFocus>, profile: EquipmentProfile, progress: ProgressSnapshot, stretchOn: Boolean = true): Int =
    (slotsForFocus(routine, focus).size + extensionSlots(c, routine, focus, profile, progress, stretchOn).size).coerceIn(1, MAX_EXERCISES_PER_ROUND)
