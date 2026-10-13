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
    val routine = trainRoutine(catalog, baseRoutine, progress, s.goalId, profile)
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
    if (count != null) withGoal = withExerciseCount(catalog, withGoal, fillFocusOf(withGoal, s), count, profile, progress, s.stretchOn)
    val routine = applyEdits(withGoal, edits)
    val draft = SessionDraft.from(prefs.copy(stretchOn = s.stretchOn, selectedProfileId = profile.id, warmupOn = s.warmupOn), routine)
        .copy(cooldownOn = s.cooldownOn, cooldownMode = s.cooldownMode, cooldownMinutes = edits.cooldownMinutes ?: s.cooldownMinutes, cooldownStretches = edits.cooldownStretches,
            focus = s.effectiveFocus(), swaps = swaps + edits.swaps, rounds = (if ((edits.rule ?: routine.rule).isRange) (edits.rule ?: routine.rule).sets else s.rounds).coerceIn(MIN_TRAIN_ROUNDS, MAX_EXPLICIT_ROUNDS), timed = s.timed, format = s.format,
            tierOverrides = edits.tierOverrides, stretchPicks = edits.stretchPicks, extraStretches = edits.extraStretches,
            roundStretchPicks = edits.roundStretchPicks, freeTargets = edits.freeTargets, rule = edits.rule ?: routine.rule)
    return generate(PlanInput("draft", nowEpochMs, nowDay, catalog, routine, draft, profile, progress = progress))
}

/** The routine as the plan uses it before hand edits: the goal's step in front and the exercises per round brought to [TrainSettings.exercisesPerRound]. A saved routine keeps exactly this list. */
fun routineForPlan(catalog: Catalog, baseRoutine: Routine, progress: ProgressSnapshot, profile: EquipmentProfile, s: TrainSettings): Routine {
    val withGoal = trainRoutine(catalog, baseRoutine, progress, s.goalId, profile)
    return s.exercisesPerRound?.let { withExerciseCount(catalog, withGoal, fillFocusOf(withGoal, s), it, profile, progress, s.stretchOn) } ?: withGoal
}

/** The exercises one round has in [plan]: what the stepper shows. */
fun exercisesPerRoundOf(plan: WorkoutPlan): Int = plan.blocks.filter { it.type == BlockType.WORK }.mapNotNull { it.slotId }.distinct().size
