// V18 (R13, D5): routines the person saves from the Preview. A saved routine stores the slots with the chosen exercises, the level and
// free-number overrides, the stretch picks, the format, the progression rule, the rest type (stretch or plain rest) and the sets.
package app.calisthenics.domain.routine

import app.calisthenics.domain.model.*
import app.calisthenics.domain.planner.PlanEdits
import app.calisthenics.domain.planner.TrainSettings
import app.calisthenics.domain.planner.applyEdits
import kotlinx.serialization.Serializable

@Serializable
data class SavedRoutine(
    val id: String,
    val name: String,
    val revision: Int,
    /** The slots with the chosen exercises already in place (swaps, removals and additions are applied). */
    val routine: Routine,
    /** What stays an overlay: level and free-number overrides, stretch picks and extra stretches. */
    val edits: PlanEdits = PlanEdits(),
    val format: WorkoutFormat = WorkoutFormat.CIRCUIT,
    val rule: ProgressionRule = ProgressionRule(),
    val stretchOn: Boolean = true,
    val sets: Int = 4,
    val createdAtEpochMs: Long = 0L,
    val updatedAtEpochMs: Long = 0L,
    /** V22: the warm-up switch the routine was saved with (the RR comes with its warm-up on). */
    val warmupOn: Boolean = false,
    /** V22: where a ready-made routine comes from (shown under its name); null for the person's own routines. */
    val credit: String? = null,
)

/** Builds what is saved from the Preview's state. [base] is the routine the plan was made from, [edits] the Preview's changes. */
fun savedFrom(id: String, name: String, revision: Int, base: Routine, edits: PlanEdits, settings: TrainSettings, rule: ProgressionRule, now: Long, createdAt: Long = now): SavedRoutine {
    val applied = applyEdits(base, edits)
    val slots = applied.slots.map { s -> edits.swaps[s.id]?.let { s.copy(preferredVariationId = it) } ?: s }
    val overlay = edits.copy(swaps = emptyMap(), removed = emptySet(), added = emptyList(), rule = null)
    return SavedRoutine(id, name.trim().ifBlank { "My routine" }, revision, applied.copy(id = id, revision = revision, name = name.trim().ifBlank { "My routine" }, slots = slots, rule = rule),
        overlay, settings.format, rule, settings.stretchOn, settings.rounds, createdAt, now, settings.warmupOn)
}

/** Everything the planner needs to rebuild the saved workout: the routine, the Train settings and the Preview edits it was saved with. */
data class ResolvedRoutine(val routine: Routine, val settings: TrainSettings, val edits: PlanEdits)

fun resolveSaved(saved: SavedRoutine, today: TrainSettings): ResolvedRoutine = ResolvedRoutine(
    saved.routine.copy(rule = saved.rule),
    today.copy(goalId = "general", focus = null, routineId = saved.id),   // the warm-up stays what is set today (settingsFor copies the routine's own when it is chosen)
    saved.edits.copy(rule = saved.rule),
)

/** Choosing a saved routine on Train copies its format, rest type and sets into today's settings (the person can still change them there). */
fun settingsFor(saved: SavedRoutine, today: TrainSettings): TrainSettings =
    today.copy(goalId = "general", format = saved.format, stretchOn = saved.stretchOn, rounds = saved.sets, focus = null, routineId = saved.id, warmupOn = saved.warmupOn)

/** D5: "Update this routine" keeps the id and bumps the revision (older plans keep their own snapshot); "Save as new" gets a new id. */
fun updateSaved(old: SavedRoutine, new: SavedRoutine) = new.copy(id = old.id, revision = old.revision + 1, createdAtEpochMs = old.createdAtEpochMs, routine = new.routine.copy(id = old.id, revision = old.revision + 1))

fun upsertSaved(list: List<SavedRoutine>, r: SavedRoutine): List<SavedRoutine> = list.filter { it.id != r.id } + r
fun deleteSaved(list: List<SavedRoutine>, id: String): List<SavedRoutine> = list.filter { it.id != id }
/** Restoring a backup merges: unknown routines are added, a routine that exists is replaced only by a higher revision. */
fun mergeSaved(local: List<SavedRoutine>, incoming: List<SavedRoutine>): List<SavedRoutine> {
    var out = local
    for (r in incoming) { val have = out.firstOrNull { it.id == r.id }; if (have == null || r.revision > have.revision) out = upsertSaved(out, r) }
    return out
}
