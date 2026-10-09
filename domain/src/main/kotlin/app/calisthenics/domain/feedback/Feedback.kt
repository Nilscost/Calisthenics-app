// T12 — feedback resolution and assumed-met evidence (PROG-05, spec §3/§5).
package app.calisthenics.domain.feedback

import app.calisthenics.domain.model.BlockType
import app.calisthenics.domain.model.TargetType
import app.calisthenics.domain.model.WorkoutPlan

enum class Execution { NOT_STARTED, RUNNING, COMPLETED, SKIPPED, PARTIAL }
enum class Rating { BELOW, MET, ABOVE, UNRATED }
enum class RatingOrigin { ASSUMED, USER_BLOCK, USER_EXERCISE, NONE }

/** Optional user input; every field optional. Actual metrics are null unless typed by the user. */
data class Feedback(
    val rating: Rating? = null,
    val actualReps: Int? = null,
    val actualHoldSeconds: Int? = null,
    val discomfort: Boolean = false,
    val revision: Int = 1,
    /** Local day the feedback (revision) was entered — used for late discomfort reports. */
    val enteredDay: Int? = null,
)

data class ResolvedOutcome(
    val blockId: String,
    val variationId: String,
    val execution: Execution,
    val rating: Rating,
    val origin: RatingOrigin,
    val actualReps: Int?,
    val actualHoldSeconds: Int?,
    val discomfort: Boolean,
    /** Set when the user's typed number contradicts their rating; UI asks to correct, never hides. */
    val conflict: String?,
)

/**
 * Precedence (spec §5): per-block user rating > exercise-row rating > ASSUMED/MET.
 * Only COMPLETED work blocks can be assumed met. Actual value below target forces BELOW
 * (with a visible conflict note). Discomfort is recorded wherever it is reported.
 */
fun resolveFeedback(
    plan: WorkoutPlan,
    executions: Map<String, Execution>,
    blockFeedback: Map<String, Feedback> = emptyMap(),
    rowFeedback: Map<String, Feedback> = emptyMap(),
): List<ResolvedOutcome> = plan.blocks.filter { it.type == BlockType.WORK && it.variationId != null }.map { b ->
    val vid = b.variationId!!
    val exec = executions[b.id] ?: Execution.NOT_STARTED
    val bf = blockFeedback[b.id]
    val rf = rowFeedback[vid]
    val discomfort = (bf?.discomfort == true) || (rf?.discomfort == true)
    if (exec != Execution.COMPLETED) {
        return@map ResolvedOutcome(b.id, vid, exec, Rating.UNRATED, RatingOrigin.NONE, bf?.actualReps, bf?.actualHoldSeconds, discomfort, null)
    }
    val (rating, origin) = when {
        bf?.rating != null -> bf.rating to RatingOrigin.USER_BLOCK
        rf?.rating != null -> rf.rating to RatingOrigin.USER_EXERCISE
        else -> Rating.MET to RatingOrigin.ASSUMED
    }
    val reps = bf?.actualReps ?: rf?.actualReps
    val hold = bf?.actualHoldSeconds ?: rf?.actualHoldSeconds
    val target = b.target
    val actual = when (target?.type) {
        TargetType.REPS -> reps
        TargetType.HOLD_SECONDS -> hold
        null -> null
    }
    var finalRating = rating
    var conflict: String? = null
    if (target != null && actual != null && actual < target.value) {
        if (rating != Rating.BELOW) conflict = "You entered $actual but the target was ${target.value} while rating ${rating.name.lowercase()} — counted as below target. Please correct if wrong."
        finalRating = Rating.BELOW
    }
    ResolvedOutcome(b.id, vid, exec, finalRating, if (finalRating != rating) origin else origin, reps, hold, discomfort, conflict)
}

/** Per-variation evidence from one session, the input of the progression replay. */
data class SessionEvidence(
    val sessionId: String,
    val variationId: String,
    val familyId: String,
    val day: Int,
    val prescribedTier: Int?,
    val completedWorkBlocks: Int,
    val skippedOrPartial: Boolean,
    val anyBelow: Boolean,
    val discomfort: Boolean,
    /** Day the discomfort was reported (late edits apply from that day forward). */
    val discomfortReportedDay: Int?,
    val easierOverride: Boolean,
    val assumedBlocks: Int,
    val confirmedBlocks: Int,
    val minQualifyingBlocks: Int,
    /** Kettlebell weight used (null = bodyweight or unknown). Levels count per weight. */
    val loadGrams: Int? = null,
    /** V02: the user rated an exercise of this session "too easy" (Rating.ABOVE). Appended last with a default. */
    val anyAbove: Boolean = false,
) {
    /** Spec §5 / ADR C2 exposure definition. */
    val qualifying: Boolean
        get() = prescribedTier != null && completedWorkBlocks >= minQualifyingBlocks && !skippedOrPartial &&
            !anyBelow && !discomfort && !easierOverride
}

fun deriveEvidence(
    sessionId: String,
    day: Int,
    plan: WorkoutPlan,
    outcomes: List<ResolvedOutcome>,
    familyOf: (String) -> String,
    minQualifyingBlocksOf: (String, Int?) -> Int,
    easierOverrides: Set<String> = emptySet(),
    feedbackDay: Int? = null,
): List<SessionEvidence> {
    val tierByVariation = plan.blocks.filter { it.type == BlockType.WORK && it.variationId != null }
        .groupBy { it.variationId!! }.mapValues { (_, bs) -> bs.mapNotNull { it.prescriptionTier }.maxOrNull() }
    val loadByVariation = plan.blocks.filter { it.type == BlockType.WORK && it.variationId != null }
        .groupBy { it.variationId!! }.mapValues { (_, bs) -> bs.mapNotNull { it.loadGrams }.maxOrNull() }
    return outcomes.groupBy { it.variationId }.toSortedMap().map { (vid, os) ->
        val tier = tierByVariation[vid]
        val disc = os.any { it.discomfort }
        SessionEvidence(
            sessionId = sessionId,
            variationId = vid,
            familyId = familyOf(vid),
            day = day,
            prescribedTier = tier,
            completedWorkBlocks = os.count { it.execution == Execution.COMPLETED },
            skippedOrPartial = os.any { it.execution == Execution.SKIPPED || it.execution == Execution.PARTIAL },
            anyBelow = os.any { it.rating == Rating.BELOW },
            discomfort = disc,
            discomfortReportedDay = if (disc) maxOf(day, feedbackDay ?: day) else null,
            easierOverride = vid in easierOverrides,
            assumedBlocks = os.count { it.origin == RatingOrigin.ASSUMED },
            confirmedBlocks = os.count { it.origin == RatingOrigin.USER_BLOCK || it.origin == RatingOrigin.USER_EXERCISE },
            minQualifyingBlocks = minQualifyingBlocksOf(vid, tier),
            loadGrams = loadByVariation[vid],
            anyAbove = os.any { it.rating == Rating.ABOVE },
        )
    }
}
