// U05: everything the Train tab asks the user, and the one function that turns it into a plan.
// Train (live minutes estimate) and Preview (the draft) both call buildTrainPlan, so they always agree.
package app.calisthenics.domain.planner

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
): PlanResult {
    val routine = Goals.routineFor(catalog, baseRoutine, s.goalId, progress)
    val draft = SessionDraft.from(prefs.copy(stretchOn = s.stretchOn, selectedProfileId = profile.id), routine)
        .copy(focus = s.effectiveFocus(), swaps = swaps, rounds = s.rounds.coerceIn(MIN_TRAIN_ROUNDS, MAX_EXPLICIT_ROUNDS), timed = s.timed)
    return generate(PlanInput("draft", nowEpochMs, nowDay, catalog, routine, draft, profile, progress = progress))
}
