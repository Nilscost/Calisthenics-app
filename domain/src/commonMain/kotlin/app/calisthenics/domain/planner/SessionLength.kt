// R7-T08 (D1): how many exercises one round has. The default is the largest count that keeps the session between 30 and 45 minutes
// for the chosen rounds, format and warm-up; focus sessions (upper, lower, core) are filled with more exercises of that area.
// Pure and deterministic, like the planner.
package app.calisthenics.domain.planner

import app.calisthenics.domain.equipment.isAvailable
import app.calisthenics.domain.goals.Goals
import app.calisthenics.domain.model.*
import app.calisthenics.domain.progression.ProgressSnapshot
import app.calisthenics.domain.routine.areasFor
import app.calisthenics.domain.routine.slotsForFocus

/** A default session is at least this long (owner D1)... */
const val MIN_SESSION_SECONDS = 30 * 60
/** ...and at most this long (owner decision 2026-10: 30 to 45 minutes); the person can still raise the exercise count by hand. */
const val MAX_SESSION_SECONDS = 45 * 60
/** The stepper's top; the pool of exercises of the focus may end earlier. */
const val MAX_EXERCISES_PER_ROUND = 12

/** Which movement types to add first for each area; later ones follow once the earlier families are in. */
private val extensionPatterns: Map<Area, List<Pattern>> = mapOf(
    Area.UPPER_BODY to listOf(Pattern.PUSH_VERTICAL, Pattern.PULL_HORIZONTAL, Pattern.PULL_VERTICAL, Pattern.PUSH_HORIZONTAL),
    Area.LOWER_BODY to listOf(Pattern.LUNGE, Pattern.HINGE, Pattern.CALF_RAISE, Pattern.SQUAT),
    Area.CORE to listOf(Pattern.CORE_ANTI_ROTATION, Pattern.CORE_ANTI_LATERAL, Pattern.CORE_ANTI_EXTENSION),
)

private fun usable(c: Catalog, v: ExerciseVariation, profile: EquipmentProfile) =
    !v.routineOnly && (v.kind == Kind.REPS || v.kind == Kind.HOLD) && c.policyFor(v) != null && isAvailable(v, profile)

/**
 * The alternates of [rung] (a main step of [route]) that have their own progress while the main step has none: what the person answered at the start
 * or trains by itself. Empty when the main step itself has progress (then the normal rotation applies).
 */
fun ownAlternatesAtRung(c: Catalog, route: ProgressionRoute, rung: String, progress: ProgressSnapshot): List<ExerciseVariation> {
    if (rung !in route.mainPath || rung in progress.variations) return emptyList()
    return route.members.filter { it.role == ProgressionRole.ALTERNATE && it.atVariationId == rung && it.variationId in progress.variations }
        .mapNotNull { c.variation(it.variationId) }
}

/** The planner refuses an exercise whose recovery would be passive rest while stretching is on; this is the same test, so a filler never makes the plan fail. */
private fun hasBetweenSetsStretch(c: Catalog, v: ExerciseVariation) =
    v.compatibleStretchIds.mapNotNull { c.variation(it) }.any { it.kind == Kind.STRETCH && !it.optIn }

/**
 * Exercises that can be added to [routine] for [focus], best first, one per family: the entry exercise of each family at the
 * person's level (the planner then follows their own progress in that family). Families already in the routine are skipped, so nothing repeats.
 */
fun extensionSlots(c: Catalog, routine: Routine, focus: Set<StrengthFocus>, profile: EquipmentProfile, progress: ProgressSnapshot, stretchOn: Boolean = true): List<RoutineSlot> {
    val areas = areasFor(focus)
    val used = routine.slots.mapNotNull { c.routeById(it.progressionRouteId)?.progressionGroup ?: c.progressionGroupOf(it.preferredVariationId) }.toMutableSet()
    val usedIds = routine.slots.map { it.preferredVariationId }.toSet()
    // per area: per pattern, one current main rung from each eligible progression.
    fun lists(area: Area): List<List<RoutineSlot>> = extensionPatterns.getValue(area).map { p ->
        val candidates = c.variations.filter { p in it.patterns && area in it.areas && !it.demonstrationHold &&
            (c.progressionRoutes.isEmpty() || c.routesFor(it.id).any { route -> it.id in route.mainPath && route.defaultPool }) &&
            usable(c, it, profile) && it.id !in usedIds && c.progressionGroupOf(it.id) !in used && (!stretchOn || hasBetweenSetsStretch(c, it)) && Goals.unmet(c, it.id, progress).isEmpty() }
        // the rung the person is on in a route: the active main step, else the furthest started one, else the first
        fun currentRung(route: ProgressionRoute): String? {
            val active = progress.activeInFamily.values.firstOrNull { id -> id in route.mainPath }
            val started = route.mainPath.filter { it in progress.variations }
            return active ?: started.lastOrNull { 5 !in progress.variations.getValue(it).achievedTiers }
                ?: started.lastOrNull()?.let { last -> route.mainPath.getOrNull(route.mainPath.indexOf(last) + 1) }
                ?: route.mainPath.firstOrNull()
        }
        // what the person answered at a rung (an alternate with its own progress, e.g. the knee push-up) stands for the main step, also when that step is still locked
        fun answeredAt(route: ProgressionRoute): ExerciseVariation? = currentRung(route)?.let { rung ->
            ownAlternatesAtRung(c, route, rung, progress).filter { p in it.patterns && area in it.areas && !it.demonstrationHold && it.id !in usedIds &&
                usable(c, it, profile) && (!stretchOn || hasBetweenSetsStretch(c, it)) }   // it has progress of its own, so its prerequisites are no gate
                .sortedWith(compareBy({ -(progress.tierFor(it.id) ?: 0) }, { it.id })).firstOrNull()
        }
        val selected = if (c.progressionRoutes.isEmpty()) {
            candidates.groupBy { c.progressionGroupOf(it.id) }.values.map { group -> group.minWith(compareBy({ it.difficultyRank }, { it.id })) }
                .sortedWith(compareBy({ it.difficultyRank }, { it.id }))
        } else {
            val fromMain = candidates.groupBy { c.progressionGroupOf(it.id) }.values.mapNotNull { group ->
                val route = group.asSequence().flatMap { c.routesFor(it.id).asSequence() }.firstOrNull { it.defaultPool }
                    ?: return@mapNotNull null
                answeredAt(route) ?: group.firstOrNull { it.id == currentRung(route) }
            }
            val groupsDone = fromMain.mapNotNull { c.progressionGroupOf(it.id) }.toSet()
            val fromAnswer = c.progressionRoutes.filter { it.defaultPool && it.mainPath.isNotEmpty() && it.progressionGroup !in groupsDone && it.progressionGroup !in used }
                .mapNotNull(::answeredAt).distinctBy { c.progressionGroupOf(it.id) }
            (fromMain + fromAnswer).sortedWith(compareBy({ it.difficultyRank }, { it.id }))
        }
        selected.map { v ->
            val routeId = c.mainRouteFor(v.id)?.takeIf { it.defaultPool }?.id
            RoutineSlot("ext-${c.progressionGroupOf(v.id)}", p, area, v.id, progressionRouteId = routeId)
        }
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
            if (taken.add(c.progressionGroupOf(cand.preferredVariationId)!!)) out += cand
        }
        if (!any) break
        pass++
    }
    // A shorter pool is preferable to adding another rung of a progression
    // already trained in this workout (owner feedback, 2026-10-15).
    // What the person answered at the start (or trains) comes before families they have no level in.
    val withProgress = progress.variations.keys.mapNotNull { c.progressionGroupOf(it) }.toSet()
    return out.sortedBy { if (c.progressionGroupOf(it.preferredVariationId) in withProgress) 0 else 1 }
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
        // what the person answered at the start is dropped last: first the optional exercises they have no level in, then the optional answered ones, then the mandatory ones
        val withProgress = progress.variations.keys.mapNotNull { c.progressionGroupOf(it) }.toSet()
        fun answered(slot: RoutineSlot) = (c.routeById(slot.progressionRouteId)?.progressionGroup ?: c.progressionGroupOf(slot.preferredVariationId)) in withProgress
        val optional = inFocus.filter { it.optional }.reversed()
        val dropOrder = optional.filter { !answered(it) } + optional.filter { answered(it) } + inFocus.filter { !it.optional }.reversed()
        val drop = dropOrder.take(inFocus.size - want).map { it.id }.toSet()
        return routine.copy(slots = routine.slots.filter { it.id !in drop })
    }
    val extra = extensionSlots(c, routine, focus, profile, progress, stretchOn).take(want - inFocus.size)
    return routine.copy(slots = routine.slots + extra)
}

/** How many exercises the stepper can reach: what the routine has in this focus plus every filler that exists, up to [MAX_EXERCISES_PER_ROUND]. */
fun maxExercisesPerRound(c: Catalog, routine: Routine, focus: Set<StrengthFocus>, profile: EquipmentProfile, progress: ProgressSnapshot, stretchOn: Boolean = true): Int =
    (slotsForFocus(routine, focus).size + extensionSlots(c, routine, focus, profile, progress, stretchOn).size).coerceIn(1, MAX_EXERCISES_PER_ROUND)
