// V04a (R21, R22): per-round corrections. A correction is a NEW feedback revision keyed to one work block; nothing is overwritten.
// This file turns the stored revisions of one session into the feedback the progression evidence uses.
package app.calisthenics.domain.feedback

import app.calisthenics.domain.model.BlockType
import app.calisthenics.domain.model.TargetType
import app.calisthenics.domain.model.WorkoutPlan

/** One stored feedback revision. [blockId] null = the whole exercise (the original row), else one round. */
data class FeedbackRow(
    val variationId: String,
    val blockId: String?,
    val revision: Int,
    val rating: Rating?,
    val discomfort: Boolean,
    /** Reps (or, for old hold rows, seconds). */
    val actualReps: Int?,
    val actualHoldSeconds: Int? = null,
)

/**
 * The feedback per exercise that [resolveFeedback] should see.
 *
 * - No round corrections for an exercise: the latest whole-exercise revision, exactly as before.
 * - With round corrections: every done round is judged on its own number, the latest correction for that block, else the
 *   value logged during the workout ([achieved], blockId -> value, null = not typed). The exercise gets the lowest number,
 *   BELOW if any round is below target or rated below, else ABOVE if any round is "too easy", else MET. A "too hard" flag on the
 *   original row cannot be attributed to a round once corrections exist, so the editors write every round together.
 */
fun effectiveFeedback(plan: WorkoutPlan, achieved: Map<String, Int?>, rows: List<FeedbackRow>): Map<String, Feedback> {
    val out = mutableMapOf<String, Feedback>()
    for ((vid, list) in rows.groupBy { it.variationId }) {
        val work = plan.blocks.filter { it.type == BlockType.WORK && it.variationId == vid }
        val hold = work.any { it.target?.type == TargetType.HOLD_SECONDS }
        fun valueOf(r: FeedbackRow): Int? = if (hold) r.actualHoldSeconds ?: r.actualReps else r.actualReps
        val rowRev = list.filter { it.blockId == null }.maxByOrNull { it.revision }
        val overrides = list.filter { it.blockId != null }.groupBy { it.blockId!! }.mapValues { (_, v) -> v.maxBy { it.revision } }
        val revision = list.maxOf { it.revision }
        if (overrides.isEmpty()) {
            val r = rowRev ?: continue
            val v = valueOf(r)
            out[vid] = Feedback(rating = r.rating, actualReps = if (hold) null else v, actualHoldSeconds = if (hold) v else null,
                discomfort = r.discomfort, revision = r.revision)
            continue
        }
        val done = work.filter { it.id in achieved || it.id in overrides }
        val ratings = done.map { b ->
            val ov = overrides[b.id]
            val value = if (ov != null) valueOf(ov) else achieved[b.id]
            val target = b.target?.value
            val rating = ov?.rating ?: when {
                value != null && target != null && value < target -> Rating.BELOW
                rowRev?.rating == Rating.ABOVE -> Rating.ABOVE
                else -> Rating.MET
            }
            Triple(value, rating, ov?.discomfort ?: false)
        }
        val lowest = ratings.mapNotNull { it.first }.minOrNull()
        val rating = when {
            ratings.any { it.second == Rating.BELOW } -> Rating.BELOW
            ratings.any { it.second == Rating.ABOVE } -> Rating.ABOVE
            else -> Rating.MET
        }
        val covered = done.all { it.id in overrides }
        val discomfort = ratings.any { it.third } || (!covered && rowRev?.discomfort == true)
        out[vid] = Feedback(rating = rating, actualReps = if (hold) null else lowest, actualHoldSeconds = if (hold) lowest else null,
            discomfort = discomfort, revision = revision)
    }
    return out
}
