// T12 — feedback resolution and assumed-met evidence (PROG-05, spec §3/§5).
package app.calisthenics.domain.feedback

import app.calisthenics.domain.model.BlockType
import app.calisthenics.domain.model.TargetType
import app.calisthenics.domain.model.WorkoutPlan
import app.calisthenics.domain.model.ProgressionPolicy
import app.calisthenics.domain.model.RuleKind
import app.calisthenics.domain.model.Side
import app.calisthenics.domain.progression.specs

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
    /** V21: under a rep-range rule: did every set reach the top of the range (null = App levels). */
    val ruleMet: Boolean? = null,
    /** V21: how many such sessions in a row move you on. */
    val ruleSessions: Int = 1,
    /** Route used by the work blocks, null for historical plans and context-free evidence. */
    val progressionRouteId: String? = null,
    /** Initial estimate from a related main exercise; no completed tiers are transferred. */
    val inferredStartTier: Int? = null,
    val levelFiveEligible: Boolean = false,
    val targetRevision: Int = 0,
    val recordedAtEpochMs: Long = 0L,
    /** App-level tier fully demonstrated by explicit results in a v1 plan; derived, never persisted. */
    val demonstratedTier: Int? = null,
    /** Immutable plan semantics boundary; historical evidence remains version 0. */
    val progressionEvidenceVersion: Int = 0,
    /** The policy snapshot carried by a v1 plan, used to replay its original target definition. */
    val effectivePolicySnapshot: ProgressionPolicy? = null,
    /** W2 (owner: lowest set becomes the next target): the lowest number logged when at least one logged set fell short of its block target; null = nothing fell short. */
    val shortfallValue: Int? = null,
    /** W2: the session was prescribed less than the level's own (capped) target, i.e. a lowered target after a shortfall: it is done and kept, but it is not a qualifying session for the level. */
    val reducedPrescription: Boolean = false,
) {
    /** Spec §5 / ADR C2 exposure definition. */
    val qualifying: Boolean
        get() = prescribedTier != null && completedWorkBlocks >= minQualifyingBlocks && !skippedOrPartial &&
            !anyBelow && !discomfort && !easierOverride && !reducedPrescription

    /** Strict actual-result qualification, independent of whether the plan mixed prescribed tiers. */
    val demonstratedQualifying: Boolean
        get() = demonstratedTier != null && completedWorkBlocks >= minQualifyingBlocks && !skippedOrPartial &&
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
    /** V21: per exercise, did the session meet the plan's rep-range rule? null = App levels. */
    ruleMetOf: (String) -> Boolean? = { null },
    recordedAtEpochMs: Long = plan.createdAtEpochMs,
    /** App adapters supply real per-block log/correction provenance; a row minimum cannot invent logs. */
    loggedBlockIds: Set<String>? = null,
    /** Effective exercise policy, including personal targets. Null policy means no demonstrated-tier evidence. */
    effectivePolicyOf: (String) -> ProgressionPolicy? = { null },
): List<SessionEvidence> {
    // Flow programs record a completed session but provide no per-exercise result. Do not
    // turn timer completion into assumed strength evidence for any item in the routine.
    val flowProgram = plan.fixedProgramId != null && plan.flow
    val tierByVariation = plan.blocks.filter { it.type == BlockType.WORK && it.variationId != null }
        // Free/mixed prescriptions have no ordinary prescribed-tier evidence; version-1 explicit actuals may independently demonstrate a full level.
        .groupBy { it.variationId!! }.mapValues { (_, bs) -> if (bs.any { it.freeTarget && !it.levelFiveEligible } || bs.map { it.prescriptionTier }.distinct().size != 1) null else bs.firstOrNull()?.prescriptionTier }
    val loadByVariation = plan.blocks.filter { it.type == BlockType.WORK && it.variationId != null }
        .groupBy { it.variationId!! }.mapValues { (_, bs) -> bs.mapNotNull { it.loadGrams }.maxOrNull() }
    val routeByVariation = plan.blocks.filter { it.type == BlockType.WORK && it.variationId != null }
        .groupBy { it.variationId!! }.mapValues { (_, bs) -> bs.mapNotNull { it.progressionRouteId }.firstOrNull() }

    fun demonstratedTier(vid: String, work: List<app.calisthenics.domain.model.TimelineBlock>, os: List<ResolvedOutcome>, policy: ProgressionPolicy?): Int? {
        if (plan.progressionEvidenceVersion < 1 || plan.rule.kind != RuleKind.APP_LEVELS) return null
        policy ?: return null
        if (work.isEmpty() || loggedBlockIds == null || !work.all { it.id in loggedBlockIds }) return null
        if (work.map { it.targetRevision }.distinct().size != 1 || work.map { it.loadGrams }.distinct().size > 1) return null
        if (os.size != work.size || os.any { it.execution != Execution.COMPLETED || it.discomfort || it.rating == Rating.BELOW }) return null
        val byId = os.associateBy { it.blockId }
        val explicit = work.associate { block ->
            val result = byId[block.id] ?: return null
            val value = when (block.target?.type) {
                TargetType.REPS -> result.actualReps
                TargetType.HOLD_SECONDS -> result.actualHoldSeconds
                null -> null
            } ?: return null
            block.id to value
        }
        val profileSpecs = policy.tierProfile?.specs()
        // M is deliberately a maintenance profile, not a five-step strength ladder.
        if (policy.tierProfile == app.calisthenics.domain.model.TierProfile.M) return null
        return policy.tiers.sortedByDescending { it.index }.firstOrNull { tier ->
            val expected = tier.target
            val spec = profileSpecs?.getOrNull(tier.index - 1)
            val requiredSets = maxOf(tier.minQualifyingBlocks, spec?.sets ?: 0)
            val sides = work.map { it.side }.filter { it == Side.LEFT || it == Side.RIGHT }.toSet()
            val requiresBothSides = policy.tierProfile == app.calisthenics.domain.model.TierProfile.RU || sides.isNotEmpty()
            val enoughBlocks = if (requiresBothSides) {
                sides == setOf(Side.LEFT, Side.RIGHT) && setOf(Side.LEFT, Side.RIGHT).all { side ->
                    work.count { it.side == side } >= requiredSets
                }
            } else work.size >= requiredSets
            val tempoSupported = if (spec?.seconds == null) true else {
                val prescribed = work.mapNotNull { it.prescriptionTier }.minOrNull()
                val prescribedSeconds = prescribed?.let { profileSpecs?.getOrNull(it - 1)?.seconds }
                prescribedSeconds != null && spec.seconds <= prescribedSeconds
            }
            enoughBlocks && tempoSupported && work.all { block ->
                val actualType = when {
                    block.target?.type == TargetType.REPS -> TargetType.REPS
                    block.target?.type == TargetType.HOLD_SECONDS -> TargetType.HOLD_SECONDS
                    else -> null
                }
                val requiredValue = expected.shownValue()
                actualType == expected.type && (explicit[block.id] ?: Int.MIN_VALUE) >= requiredValue
            }
        }?.index
    }

    return outcomes.groupBy { it.variationId }.toSortedMap().mapNotNull { (vid, os) ->
        val work = plan.blocks.filter { it.type == BlockType.WORK && it.variationId == vid }
        val policySnapshot = plan.progressionPolicies[vid] ?: effectivePolicyOf(vid)
        val explicitlyLogged = os.isNotEmpty() && work.all { loggedBlockIds == null || it.id in loggedBlockIds } && os.all { o ->
            when (work.firstOrNull { it.id == o.blockId }?.target?.type) {
                TargetType.REPS -> o.actualReps != null
                TargetType.HOLD_SECONDS -> o.actualHoldSeconds != null
                null -> false
            }
        }
        if (flowProgram && !explicitlyLogged) return@mapNotNull null
        val tier = tierByVariation[vid].takeUnless { (flowProgram || work.any { it.levelFiveEligible }) && !explicitlyLogged }
        val demonstrated = demonstratedTier(vid, work, os, policySnapshot)
        val evidenceTier = demonstrated ?: tier
        val policyMinBlocks = evidenceTier?.let { t -> policySnapshot?.tiers?.firstOrNull { it.index == t }?.minQualifyingBlocks }
        val disc = os.any { it.discomfort }
        fun explicitActual(o: ResolvedOutcome): Boolean = plan.progressionEvidenceVersion >= 1 &&
            loggedBlockIds?.contains(o.blockId) == true && when (work.firstOrNull { it.id == o.blockId }?.target?.type) {
                TargetType.REPS -> o.actualReps != null
                TargetType.HOLD_SECONDS -> o.actualHoldSeconds != null
                null -> false
            }
        val actualOf = { o: ResolvedOutcome, b: app.calisthenics.domain.model.TimelineBlock? ->
            when (b?.target?.type) { TargetType.REPS -> o.actualReps; TargetType.HOLD_SECONDS -> o.actualHoldSeconds; null -> null }
        }
        val workById = work.associateBy { it.id }
        val shortfall = if (plan.rule.kind != RuleKind.APP_LEVELS || disc) null else {
            val logged = os.filter { it.execution == Execution.COMPLETED }.mapNotNull { o ->
                val b = workById[o.blockId]; val a = actualOf(o, b); if (b?.target != null && a != null) b.target.value to a else null }
            if (logged.any { (target, actual) -> actual < target }) logged.minOf { it.second }.coerceAtLeast(1) else null
        }
        val reduced = plan.rule.kind == RuleKind.APP_LEVELS && policySnapshot != null && work.isNotEmpty() && work.any { b ->
            val t = b.target; val level = b.prescriptionTier?.let { i -> policySnapshot.tiers.firstOrNull { it.index == i } }
            !b.freeTarget && !b.levelFiveEligible && t != null && level != null && level.target.type == t.type &&
                t.value < app.calisthenics.domain.planner.cappedTarget(level.target, policySnapshot).value
        }
        SessionEvidence(
            sessionId = sessionId,
            variationId = vid,
            familyId = familyOf(vid),
            day = day,
            prescribedTier = tier,
            completedWorkBlocks = os.count { it.execution == Execution.COMPLETED },
            skippedOrPartial = os.any { it.execution != Execution.COMPLETED } || os.size != work.size,
            anyBelow = os.any { it.rating == Rating.BELOW },
            discomfort = disc,
            discomfortReportedDay = if (disc) maxOf(day, feedbackDay ?: day) else null,
            easierOverride = vid in easierOverrides,
            assumedBlocks = os.count { it.origin == RatingOrigin.ASSUMED && !explicitActual(it) },
            confirmedBlocks = os.count { it.origin == RatingOrigin.USER_BLOCK || it.origin == RatingOrigin.USER_EXERCISE || explicitActual(it) },
            minQualifyingBlocks = policyMinBlocks ?: minQualifyingBlocksOf(vid, evidenceTier),
            loadGrams = loadByVariation[vid],
            anyAbove = os.any { it.rating == Rating.ABOVE },
            ruleMet = ruleMetOf(vid),
            ruleSessions = plan.rule.sessions,
            progressionRouteId = routeByVariation[vid],
            levelFiveEligible = work.isNotEmpty() && work.all { it.levelFiveEligible } && explicitlyLogged,
            recordedAtEpochMs = recordedAtEpochMs,
            targetRevision = work.maxOfOrNull { it.targetRevision } ?: 0,
            inferredStartTier = plan.blocks.firstOrNull { it.type == BlockType.WORK && it.variationId == vid && it.inferredStartTier == tier }?.inferredStartTier?.takeIf { tier != null },
            demonstratedTier = demonstrated,
            progressionEvidenceVersion = plan.progressionEvidenceVersion,
            effectivePolicySnapshot = policySnapshot,
            shortfallValue = shortfall,
            reducedPrescription = reduced,
        )
    }
}
