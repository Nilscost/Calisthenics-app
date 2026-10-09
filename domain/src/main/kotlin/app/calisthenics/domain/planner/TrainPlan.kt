// U05: everything the Train tab asks the user, and the one function that turns it into a plan.
// Train (live minutes estimate) and Preview (the draft) both call buildTrainPlan, so they always agree.
package app.calisthenics.domain.planner

import app.calisthenics.domain.equipment.isAvailable
import app.calisthenics.domain.goals.Goals
import app.calisthenics.domain.model.*
import app.calisthenics.domain.progression.ProgressSnapshot
import app.calisthenics.domain.routine.SessionDraft
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
) {
    fun effectiveFocus(): Set<StrengthFocus> = focus ?: Goals.focusFor(goalId)
}

/** The routine the plan is built from: the usual plan, with the goal's next step in front. */
fun trainRoutine(catalog: Catalog, baseRoutine: Routine, progress: ProgressSnapshot, goalId: String): Routine =
    Goals.routineFor(catalog, baseRoutine, goalId, progress)

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
    val routine = applyEdits(trainRoutine(catalog, baseRoutine, progress, s.goalId), edits)
    val draft = SessionDraft.from(prefs.copy(stretchOn = s.stretchOn, selectedProfileId = profile.id), routine)
        .copy(focus = s.effectiveFocus(), swaps = swaps + edits.swaps, rounds = s.rounds.coerceIn(MIN_TRAIN_ROUNDS, MAX_EXPLICIT_ROUNDS), timed = s.timed,
            tierOverrides = edits.tierOverrides, stretchPicks = edits.stretchPicks, extraStretches = edits.extraStretches)
    return generate(PlanInput("draft", nowEpochMs, nowDay, catalog, routine, draft, profile, progress = progress))
}
