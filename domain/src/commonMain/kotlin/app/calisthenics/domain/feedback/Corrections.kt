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

/** One done round of an exercise as the screens and the evidence see it after corrections. [value] = reps, or seconds for holds; null = not typed (as planned). */
data class EffectiveRound(val blockId: String, val round: Int, val side: app.calisthenics.domain.model.Side, val value: Int?, val rating: Rating, val discomfort: Boolean, val corrected: Boolean)

/**
 * The done rounds of [vid] (outcome MET or PARTIAL, i.e. a key of [achieved]) with their values after corrections:
 * the latest correction for the block, else the number logged during the workout. Without any round correction the
 * flags of the original whole-exercise row ("too hard", "too easy", pain) are shown on every round; once a round was corrected,
 * the editors have written every round, and an uncorrected round only keeps "below target" / the row's "too easy".
 */
fun effectiveRounds(plan: WorkoutPlan, vid: String, achieved: Map<String, Int?>, rows: List<FeedbackRow>): List<EffectiveRound> {
    val mine = rows.filter { it.variationId == vid }
    val hold = plan.blocks.any { it.type == BlockType.WORK && it.variationId == vid && it.target?.type == TargetType.HOLD_SECONDS }
    fun valueOf(r: FeedbackRow): Int? = if (hold) r.actualHoldSeconds ?: r.actualReps else r.actualReps
    val rowRev = mine.filter { it.blockId == null }.maxByOrNull { it.revision }
    val overrides = mine.filter { it.blockId != null }.groupBy { it.blockId!! }.mapValues { (_, v) -> v.maxBy { it.revision } }
    return plan.blocks.filter { it.type == BlockType.WORK && it.variationId == vid && (it.id in achieved || it.id in overrides) }.map { b ->
        val ov = overrides[b.id]
        val value = if (ov != null) valueOf(ov) else achieved[b.id]
        val target = b.target?.value
        val below = value != null && target != null && value < target
        val rating = ov?.rating ?: when {
            below -> Rating.BELOW
            overrides.isEmpty() && rowRev?.rating == Rating.BELOW -> Rating.BELOW // "too hard" on the original row
            rowRev?.rating == Rating.ABOVE -> Rating.ABOVE
            else -> Rating.MET
        }
        val discomfort = ov?.discomfort ?: (overrides.isEmpty() && rowRev?.discomfort == true)
        EffectiveRound(b.id, b.roundIndex ?: 1, b.side, value, rating, discomfort, ov != null)
    }
}

/**
 * The feedback per exercise that [resolveFeedback] should see.
 *
 * - No round corrections for an exercise: the latest whole-exercise revision, exactly as before.
 * - With round corrections: [effectiveRounds]; the exercise gets the lowest number, BELOW if any round is below target or rated
 *   below, else ABOVE if any round is "too easy", else MET; pain follows the rounds. A "too hard" flag on the original row cannot be
 *   attributed to a round once corrections exist, so the editors write every round together.
 */
fun effectiveFeedback(plan: WorkoutPlan, achieved: Map<String, Int?>, rows: List<FeedbackRow>): Map<String, Feedback> {
    val out = mutableMapOf<String, Feedback>()
    for ((vid, list) in rows.groupBy { it.variationId }) {
        val hold = plan.blocks.any { it.type == BlockType.WORK && it.variationId == vid && it.target?.type == TargetType.HOLD_SECONDS }
        val rowRev = list.filter { it.blockId == null }.maxByOrNull { it.revision }
        val hasRoundCorrections = list.any { it.blockId != null }
        val revision = list.maxOf { it.revision }
        if (!hasRoundCorrections) {
            val r = rowRev ?: continue
            val v = if (hold) r.actualHoldSeconds ?: r.actualReps else r.actualReps
            out[vid] = Feedback(rating = r.rating, actualReps = if (hold) null else v, actualHoldSeconds = if (hold) v else null,
                discomfort = r.discomfort, revision = r.revision)
            continue
        }
        val rounds = effectiveRounds(plan, vid, achieved, list)
        val lowest = rounds.mapNotNull { it.value }.minOrNull()
        val rating = when {
            rounds.any { it.rating == Rating.BELOW } -> Rating.BELOW
            rounds.any { it.rating == Rating.ABOVE } -> Rating.ABOVE
            else -> Rating.MET
        }
        val covered = rounds.all { it.corrected }
        val discomfort = rounds.any { it.discomfort } || (!covered && rowRev?.discomfort == true)
        out[vid] = Feedback(rating = rating, actualReps = if (hold) null else lowest, actualHoldSeconds = if (hold) lowest else null,
            discomfort = discomfort, revision = revision)
    }
    return out
}
