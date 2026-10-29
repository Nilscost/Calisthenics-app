package app.calisthenics.domain

/**
 * Progression domain seed (T03 scaffold; full engine lands in T12/T13).
 *
 * Pure values only — no Android, no wall clock: callers inject day numbers
 * (spec §2: deterministic tests with fixed values). Defaults implement ADR
 * 0002 §C (owner-approved 2026-09-28) and spec §5:
 *  - an "exposure" is one qualifying session for the variation (C2);
 *  - R8.1-T04 (owner 2026-10-17, supersedes C3): 3 qualifying exposures within any rolling 14-day window advance the
 *    prescription by ONE tier; the earlier "spanning >= 7 days / one per 7 days / 7 days at the tier" pacing is gone.
 */

/** One reviewed prescription tier. Targets are authored per variation; no
 *  universal formula (spec §3). */
data class Tier(
    val index: Int, // 1..5
    val target: Int, // target reps OR hold seconds
    val workWindowSeconds: Int,
    val minRecoverySeconds: Int,
)

/** A variation's reviewed progression policy: exactly five ordered tiers. */
data class ProgressionPolicy(
    val variationId: String,
    val version: Int,
    val tiers: List<Tier>,
) {
    init {
        require(tiers.size == 5) { "policy must have exactly five tiers" }
        require(tiers.map { it.index } == listOf(1, 2, 3, 4, 5)) { "tiers must be 1..5 in order" }
    }

    fun tier(index: Int): Tier =
        tiers.firstOrNull { it.index == index }
            ?: error("no tier $index for $variationId")

    /** Next tier index, or null at the top (tier 5). */
    fun nextTierIndex(current: Int): Int? = if (current in 1..4) current + 1 else null
}

/** One qualifying exposure for a variation on a local day (C2).
 *  [qualifying] already encodes the C2 conditions (min completed blocks,
 *  no skip/partial, no BELOW/discomfort, no easier override). */
data class QualifyingExposure(
    val variationId: String,
    val day: Int, // local day number, caller-injected (deterministic)
)

/** Result of evaluating advancement for one variation at one point in time. */
sealed interface AdvancementResult {
    data object NoChange : AdvancementResult
    data class Advance(
        val fromTier: Int,
        val toTier: Int,
        val evidenceDays: List<Int>,
    ) : AdvancementResult
}

data class AdvancementInput(
    val policy: ProgressionPolicy,
    val currentTier: Int,
    val enrolledAtDay: Int, // day the current tier was enrolled
    val nowDay: Int,
    val exposures: List<QualifyingExposure>,
    val lastAutoAdvancementDay: Int?, // null = never
) {
    init {
        require(policy.tiers.any { it.index == currentTier }) { "currentTier must be a policy tier" }
    }
}

/** R8.1-T04 (F4, owner decision 2026-10-17): a level rises after this many qualifying sessions ... */
const val LEVEL_UP_SESSIONS = 3
/** ... that fall inside any rolling window of this many days (a window holds 14 calendar days, so the first and the last session are at most 13 days apart). */
const val LEVEL_UP_WINDOW_DAYS = 14
/** Slower path for once-a-week trainers (owner decision): this many qualifying sessions (distinct days) ... */
const val LEVEL_UP_SLOW_SESSIONS = 4
/** ... inside any rolling window of this many days (first and last at most 29 days apart). */
const val LEVEL_UP_SLOW_WINDOW_DAYS = 30

/**
 * Evaluate advancement under the owner decision of 2026-10-17 (R8.1-T04, F4), which supersedes ADR 0002 §C3 ("3 exposures spanning at
 * least 7 days, one advancement per 7 days, 7 days at the tier"):
 *  - a level rises after [LEVEL_UP_SESSIONS] qualifying sessions (distinct days) inside any rolling [LEVEL_UP_WINDOW_DAYS]-day window;
 *  - or, additively, [LEVEL_UP_SLOW_SESSIONS] qualifying sessions inside any rolling [LEVEL_UP_SLOW_WINDOW_DAYS]-day window (weekly cadence levels up after about a month);
 *  - sessions before the day the tier was enrolled do not count; a longer gap between sessions never costs anything already earned;
 *  - ABOVE ratings, extra rounds and same-day sessions never accelerate this (each day counts once).
 * [AdvancementInput.lastAutoAdvancementDay] is kept for callers but no longer limits the pace.
 */
fun evaluateAdvancement(input: AdvancementInput): AdvancementResult {
    val p = input.policy
    val next = p.nextTierIndex(input.currentTier) ?: return AdvancementResult.NoChange

    val days = input.exposures.filter { it.variationId == p.variationId && it.day >= input.enrolledAtDay }.map { it.day }.distinct().sorted()
    fun within(n: Int, window: Int) = (0..days.size - n).any { days[it + n - 1] - days[it] < window }
    val inWindow = within(LEVEL_UP_SESSIONS, LEVEL_UP_WINDOW_DAYS) || within(LEVEL_UP_SLOW_SESSIONS, LEVEL_UP_SLOW_WINDOW_DAYS)
    if (!inWindow) return AdvancementResult.NoChange
    return AdvancementResult.Advance(input.currentTier, next, days)
}
