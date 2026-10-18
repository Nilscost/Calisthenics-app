// U05: everything the Train tab asks the user, and the one function that turns it into a plan.
// Train (live minutes estimate) and Preview (the draft) both call buildTrainPlan, so they always agree.
package app.calisthenics.domain.planner

import app.calisthenics.domain.equipment.isAvailable
import app.calisthenics.domain.goals.Goals
import app.calisthenics.domain.model.*
import app.calisthenics.domain.progression.ProgressSnapshot
import app.calisthenics.domain.routine.SessionDraft
import app.calisthenics.domain.routine.slotsForFocus
import kotlinx.serialization.Serializable

const val MIN_TRAIN_ROUNDS = 1
const val DEFAULT_TRAIN_ROUNDS = 4
const val DEFAULT_CONDITIONING_MINUTES = 6
const val MIN_CONDITIONING_MINUTES = 5
const val MAX_CONDITIONING_MINUTES = 10

@Serializable
data class TrainSettings(
    val goalId: String = "general",
    val profileId: String = "home",
    val rounds: Int = DEFAULT_TRAIN_ROUNDS,
    val timed: Boolean = false,
    val stretchOn: Boolean = true,
    /** null = follow the goal ("Full body" for most goals, "Upper body" for the upper-body goal, ...). */
    val focus: Set<StrengthFocus>? = null,
    /** V19: the workout format (Circuit, Pairs, Straight sets); `rounds` are then the sets. */
    val format: WorkoutFormat = WorkoutFormat.CIRCUIT,
    /** V18: a saved routine is chosen as the objective (null = a body part or a skill). */
    val routineId: String? = null,
    /** V22/V23: the warm-up block before the workout (the routine's own warm-up, else the catalog's). */
    val warmupOn: Boolean = false,
    /** R7-T08 (D1): exercises per round. null = the default: the routine's own, or (for a body part or skill) the smallest count that makes the session at least 30 minutes. */
    val exercisesPerRound: Int? = null,
    /** R7-T13 (D10): the cool-down switch and its mode (stretches matched to the session, or a timed relax block) and the timed minutes. */
    val cooldownOn: Boolean = false,
    val cooldownMode: CooldownMode = CooldownMode.STRETCHING,
    val cooldownMinutes: Int = 5,
    /** R8 T07: selected catalog-backed fixed program (null = existing objective/routine). */
    val fixedProgramId: String? = null,
    /** R8 T07: user overrides for each program's contract default flow setting. */
    val fixedProgramFlowOverrides: Map<String, Boolean> = emptyMap(),
    /** Opt-in route pools (POWER, CONDITIONING, PREHAB, MOBILITY); empty by default. */
    val additionalPools: Set<String> = emptySet(),
    /** R8.1-T06 (F7): Flow, a Preview option for every workout: no rest between exercises; rest between sets stays. A program's authored flow is only the starting value. */
    val flow: Boolean = false,
    /** R8.2-T03 (G3): the "Include" row: an optional conditioning finisher of [conditioningMinutes] (5 to 10) at the end of the workout. */
    val conditioningOn: Boolean = false,
    val conditioningMinutes: Int = DEFAULT_CONDITIONING_MINUTES,
) {
    fun effectiveFocus(): Set<StrengthFocus> = focus ?: Goals.focusFor(goalId)
}

/** The routine the plan is built from: the usual plan, with the goal's next step in front. */
fun trainRoutine(catalog: Catalog, baseRoutine: Routine, progress: ProgressSnapshot, goalId: String, profile: EquipmentProfile? = null): Routine =
    Goals.routineFor(catalog, baseRoutine, goalId, progress, profile)

/** The areas to fill a session from: a skill session is filled with exercises of the areas its own exercises use (front lever: pull and core), else the chosen focus. */
fun fillFocusOf(routine: Routine, s: TrainSettings): Set<StrengthFocus> {
    val skill = routine.slots.filter { Goals.isSkillSlot(it.id) }
    if (skill.isEmpty()) return s.effectiveFocus()
    val areas = skill.map { it.area }.toSet()
    return areas.map { when (it) { Area.UPPER_BODY -> StrengthFocus.UPPER_BODY; Area.LOWER_BODY -> StrengthFocus.LOWER_BODY; Area.CORE -> StrengthFocus.CORE } }.toSet()
}

/**
 * Exercises that can stand in for the slot today: same movement and area, usable with this equipment, strength kinds only.
 * Always contains [currentId] first so the sheet can show what is selected.
 */
fun swapOptions(catalog: Catalog, routine: Routine, slotId: String, profile: EquipmentProfile, currentId: String): List<ExerciseVariation> {
    val slot = routine.slots.firstOrNull { it.id == slotId } ?: return emptyList()
    val ok = catalog.variations.filter {
        (it.kind == Kind.REPS || it.kind == Kind.HOLD) && slot.area in it.areas && slot.intent in it.patterns &&
            isAvailable(it, profile) && catalog.policyFor(it) != null
    }.sortedWith(compareBy({ it.difficultyRank }, { it.id }))
    val current = catalog.variation(currentId)
    return if (current != null) listOf(current) + ok.filter { it.id != currentId } else ok
}

/**
 * [swaps] are today-only slot -> exercise replacements (Preview). The plan id is a placeholder: the caller gives the
 * plan its real unique id when the workout starts (`plan.copy(id = ...)`), because history keeps one snapshot per id.
 */
fun buildTrainPlan(
    catalog: Catalog,
    baseRoutine: Routine,
    progress: ProgressSnapshot,
    profile: EquipmentProfile,
    s: TrainSettings,
    swaps: Map<String, String> = emptyMap(),
    nowEpochMs: Long = 0L,
    nowDay: Int = 0,
    prefs: Preferences = Preferences(),
    edits: PlanEdits = PlanEdits(),
): PlanResult {
    return planFor(catalog, baseRoutine, progress, profile, s, swaps, nowEpochMs, nowDay, prefs, edits, s.exercisesPerRound)
}

/**
 * R7-T08 (D1): the default exercises per round for a body part or a skill: the smallest count that makes the session at least 30 minutes
 * at the chosen rounds, format and warm-up (null = not applicable: a saved or ready-made routine, a stretch session, or exercises added or removed by hand,
 * which keep their own list). Never below the routine's own count; the pool of fillers may end before 30 minutes are reached.
 */
fun defaultExercisesPerRound(
    catalog: Catalog, baseRoutine: Routine, progress: ProgressSnapshot, profile: EquipmentProfile, s: TrainSettings,
    swaps: Map<String, String> = emptyMap(), prefs: Preferences = Preferences(), edits: PlanEdits = PlanEdits(),
): Int? {
    if (s.routineId != null || baseRoutine.stretchSession.isNotEmpty() || edits.removed.isNotEmpty() || edits.added.isNotEmpty()) return null
    val routine = withAdditionalPools(catalog, trainRoutine(catalog, baseRoutine, progress, s.goalId, profile), progress, profile, s.goalId, s.additionalPools)
    val focus = fillFocusOf(routine, s)
    val base = slotsForFocus(routine, focus).size
    val top = maxExercisesPerRound(catalog, routine, focus, profile, progress, s.stretchOn)
    fun build(n: Int) = planFor(catalog, baseRoutine, progress, profile, s, swaps, 0L, 0, prefs, edits, n)
    var best = base
    for (n in base..maxOf(base, top)) {
        val r = build(n)
        if (r !is PlanResult.Ready) break   // a filler the planner cannot place: keep the last good count
        best = n
        if (r.plan.plannedDurationSeconds >= MIN_SESSION_SECONDS) break
    }
    return best
}

/** [s] with the default exercises per round filled in when the person has not set a number. */
fun withDefaultLength(
    catalog: Catalog, baseRoutine: Routine, progress: ProgressSnapshot, profile: EquipmentProfile, s: TrainSettings,
    swaps: Map<String, String> = emptyMap(), prefs: Preferences = Preferences(), edits: PlanEdits = PlanEdits(),
): TrainSettings = if (s.exercisesPerRound != null) s else s.copy(exercisesPerRound = defaultExercisesPerRound(catalog, baseRoutine, progress, profile, s, swaps, prefs, edits))

private fun planFor(
    catalog: Catalog, baseRoutine: Routine, progress: ProgressSnapshot, profile: EquipmentProfile, s: TrainSettings,
    swaps: Map<String, String>, nowEpochMs: Long, nowDay: Int, prefs: Preferences, edits: PlanEdits, count: Int?,
): PlanResult {
    var withGoal = trainRoutine(catalog, baseRoutine, progress, s.goalId, profile)
    withGoal = withAdditionalPools(catalog, withGoal, progress, profile, s.goalId, s.additionalPools)
    if (count != null) withGoal = withExerciseCount(catalog, withGoal, fillFocusOf(withGoal, s), count, profile, progress, s.stretchOn)
    val routine = applyEdits(withGoal, edits)
    val draft = SessionDraft.from(prefs.copy(stretchOn = s.stretchOn, selectedProfileId = profile.id, warmupOn = s.warmupOn), routine)
        .copy(cooldownOn = s.cooldownOn, cooldownMode = s.cooldownMode, cooldownMinutes = edits.cooldownMinutes ?: s.cooldownMinutes, cooldownStretches = edits.cooldownStretches, cooldownRemoved = edits.cooldownRemoved, cooldownAdded = edits.cooldownAdded,
            focus = s.effectiveFocus(), swaps = swaps + edits.swaps, rounds = (if ((edits.rule ?: routine.rule).isRange) (edits.rule ?: routine.rule).sets else s.rounds).coerceIn(MIN_TRAIN_ROUNDS, MAX_EXPLICIT_ROUNDS), timed = s.timed, format = s.format,
            tierOverrides = edits.tierOverrides, stretchPicks = edits.stretchPicks, extraStretches = edits.extraStretches,
            roundStretchPicks = edits.roundStretchPicks, freeTargets = edits.freeTargets, rule = edits.rule ?: routine.rule, flow = s.flow, warmupRemoved = edits.warmupRemoved)
    val generated = withTimedExtras(catalog, generate(PlanInput("draft", nowEpochMs, nowDay, catalog, routine, draft, profile, progress = progress)), progress, profile, s.additionalPools)
    return if (s.conditioningOn) withConditioningFinisher(catalog, generated, progress, profile, edits.conditioningMinutes ?: s.conditioningMinutes, edits.conditioningRemoved) else generated
}

/** The routine as the plan uses it before hand edits: the goal's step in front and the exercises per round brought to [TrainSettings.exercisesPerRound]. A saved routine keeps exactly this list. */
fun routineForPlan(catalog: Catalog, baseRoutine: Routine, progress: ProgressSnapshot, profile: EquipmentProfile, s: TrainSettings): Routine {
    val withGoal = withAdditionalPools(catalog, trainRoutine(catalog, baseRoutine, progress, s.goalId, profile), progress, profile, s.goalId, s.additionalPools)
    return s.exercisesPerRound?.let { withExerciseCount(catalog, withGoal, fillFocusOf(withGoal, s), it, profile, progress, s.stretchOn) } ?: withGoal
}

/** Power joins strength work only by deliberate selection; the other extras are timed blocks. */
private fun withAdditionalPools(c: Catalog, routine: Routine, progress: ProgressSnapshot, profile: EquipmentProfile, goalId: String, pools: Set<String>): Routine {
    if ("POWER" !in pools || c.progressionRoutes.isEmpty()) return routine
    val used = routine.slots.mapNotNull { c.progressionGroupOf(it.preferredVariationId) }.toMutableSet()
    val additions = c.progressionRoutes.filter { it.focus == "POWER" }.mapNotNull { route ->
        if (!used.add(route.progressionGroup)) return@mapNotNull null
        val trained = route.mainPath.filter { progress.variations.containsKey(it) }
        val start = trained.lastOrNull { 5 !in progress.variations.getValue(it).achievedTiers }
            ?: trained.lastOrNull()?.let { route.mainPath.getOrNull(route.mainPath.indexOf(it) + 1) }
            ?: route.mainPath.firstOrNull()
        val candidates = listOfNotNull(start) + route.members.filter { it.role == ProgressionRole.ALTERNATE && it.atVariationId == start }.map { it.variationId }
        val v = candidates.mapNotNull(c::variation).firstOrNull { !it.demonstrationHold && !it.routineOnly && c.policyFor(it) != null && isAvailable(it, profile) && Goals.unmet(c, it.id, progress).isEmpty() }
            ?: return@mapNotNull null
        RoutineSlot("pool-power-${route.id}", v.patterns.first(), v.areas.firstOrNull() ?: Area.CORE, v.id, progressionRouteId = route.id)
    }
    return routine.copy(slots = routine.slots + additions)
}

/** Timed extras have no achievement target and cannot advance a strength ladder. */
private fun withTimedExtras(c: Catalog, result: PlanResult, progress: ProgressSnapshot, profile: EquipmentProfile, pools: Set<String>): PlanResult {
    if (result !is PlanResult.Ready || pools.isEmpty() || c.progressionRoutes.isEmpty()) return result
    val plan = result.plan
    val workIds = plan.blocks.filter { it.type == BlockType.WORK }.mapNotNull { it.variationId }.toSet()
    val usedGroups = workIds.mapNotNull(c::progressionGroupOf).toSet()
    fun supportsWork(anchor: String?): Boolean = anchor != null && (anchor in workIds || c.progressionRoutes.any { route ->
        val at = route.mainPath.indexOf(anchor)
        at >= 0 && workIds.any { route.mainPath.indexOf(it) >= at }
    })
    val support = if ("PREHAB" in pools) c.progressionRoutes.filter { it.focus == "PREHAB" }.flatMap { route ->
        route.members.filter { it.role == ProgressionRole.SUPPORT && supportsWork(it.atVariationId) }.map { it.variationId }
    }.distinct().mapNotNull(c::variation).filter { !it.demonstrationHold && !it.routineOnly && isAvailable(it, profile) && Goals.unmet(c, it.id, progress).isEmpty() }.take(2) else emptyList()
    val compatible = workIds.flatMap { c.variation(it)?.compatibleStretchIds.orEmpty() }.toSet()
    val mobility = if ("MOBILITY" in pools) compatible.mapNotNull(c::variation)
        .filter { !it.demonstrationHold && !it.optIn && !it.routineOnly && isAvailable(it, profile) }.sortedBy { it.id }.take(3) else emptyList()
    val conditioning = if ("CONDITIONING" in pools) c.progressionRoutes.filter { it.focus == "CONDITIONING" }.flatMap { route -> route.members.map { it.variationId } + route.mainPath }
        .distinct().mapNotNull(c::variation).filter { !it.demonstrationHold && !it.routineOnly && (it.kind == Kind.REPS || it.kind == Kind.HOLD) && c.progressionGroupOf(it.id) !in usedGroups && isAvailable(it, profile) && Goals.unmet(c, it.id, progress).isEmpty() }.sortedBy { it.id } else emptyList()
    val completed = progress.familyWorkoutCounts.values.sum()
    val interval = conditioning.getOrNull(if (conditioning.isEmpty()) 0 else completed % conditioning.size)
    fun timed(v: ExerciseVariation, prefix: String, type: BlockType, duration: Int): List<TimelineBlock> {
        val sides = if (v.unilateral) listOf(Side.LEFT, Side.RIGHT) else listOf(Side.NONE)
        return sides.flatMapIndexed { index, side ->
            (if (index > 0) listOf(TimelineBlock("$prefix-${v.id}-side-ready", BlockType.TRANSITION, SIDE_CHANGE_PREPARATION_SECONDS)) else emptyList()) +
                TimelineBlock("$prefix-${v.id}-${side.name}", type, duration, variationId = v.id, side = side, mediaId = v.mediaId, note = "Use controlled movement for the timed interval.")
        }
    }
    val before = support.flatMap { timed(it, "prep", BlockType.WARMUP, 30) }
    val finish = if (interval == null) emptyList() else (0..2).flatMap { n ->
        timed(interval, "finisher-$n", BlockType.WORK, 30) + if (n < 2) listOf(TimelineBlock("finisher-rest-$n", BlockType.PASSIVE_RECOVERY, 30)) else emptyList()
    }
    val after = mobility.flatMap { timed(it, "mobility", BlockType.COOLDOWN, it.defaultSeconds ?: 30) }
    val lastWork = plan.blocks.indexOfLast { it.type == BlockType.WORK }
    val insert = if (lastWork >= 0) lastWork + 1 else plan.blocks.size
    val blocks = before + plan.blocks.take(insert) + finish + plan.blocks.drop(insert) + after
    return PlanResult.Ready(plan.copy(blocks = blocks, plannedDurationSeconds = blocks.sumOf { it.durationSeconds },
        usesDraftContent = plan.usesDraftContent || (support + mobility + listOfNotNull(interval)).any { it.reviewState == ReviewState.DRAFT },
        changesExplained = plan.changesExplained + listOfNotNull(if (before.isNotEmpty()) "Preparation added for today's exercises." else null, if (finish.isNotEmpty()) "Conditioning finisher: three 30-second intervals, with 30-second rests." else null, if (after.isNotEmpty()) "Mobility added after training." else null)))
}

/** The exercises one round has in [plan]: what the stepper shows. */
fun exercisesPerRoundOf(plan: WorkoutPlan): Int = plan.blocks.filter { it.type == BlockType.WORK }.mapNotNull { it.slotId }.filter { !it.startsWith("cond") }.distinct().size   // R8.2-T03: the conditioning finisher is not an exercise of the round
