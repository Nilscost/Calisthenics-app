// U07 (F10, F14): what the user logs DURING the workout, per work block (= per round and side).
// Nothing typed = assumed met (spec §5). The row feedback written at the end uses the LOWEST round (existing rule).
package app.calisthenics.domain.session

import app.calisthenics.domain.feedback.Rating
import app.calisthenics.domain.model.BlockType
import app.calisthenics.domain.model.WorkoutPlan
import kotlinx.serialization.Serializable

/** [reps] are reps for rep targets and seconds for holds; null = not typed. */
@Serializable
data class BlockLog(val reps: Int? = null, val tooHard: Boolean = false, val discomfort: Boolean = false,
                    /** V03: "Too easy"; appended last with a default so saved checkpoints still decode. */
                    val tooEasy: Boolean = false)

/** Exercise-level feedback derived from the per-block logs of one session. */
data class RowLog(val variationId: String, val rating: Rating, val discomfort: Boolean, val actualReps: Int?)

/** Only exercises the user actually logged get a row; the rest stay "assumed met". */
fun rowLogsFrom(plan: WorkoutPlan, logged: Map<String, BlockLog>): List<RowLog> {
    val work = plan.blocks.filter { it.type == BlockType.WORK && it.variationId != null && it.id in logged }
    return work.groupBy { it.variationId!! }.toSortedMap().map { (vid, blocks) ->
        val logs = blocks.map { it to logged.getValue(it.id) }
        val reps = logs.mapNotNull { it.second.reps }.minOrNull()
        val below = logs.any { (b, l) -> l.tooHard || (l.reps != null && b.target != null && l.reps < b.target.value) }
        val easy = logs.any { it.second.tooEasy }
        RowLog(vid, if (below) Rating.BELOW else if (easy) Rating.ABOVE else Rating.MET, logs.any { it.second.discomfort }, reps)
    }
}
