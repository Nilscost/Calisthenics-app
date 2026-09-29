package app.calisthenics.domain

/**
 * Progression domain seed (T03 scaffold; full engine lands in T12/T13).
 *
 * Pure values only — no Android, no wall clock: callers inject day numbers
 * (spec §2: deterministic tests with fixed values). Defaults implement ADR
 * 0002 §C (owner-approved 2026-09-28) and spec §5:
 *  - an "exposure" is one qualifying session for the variation (C2);
 *  - 3 qualifying exposures spanning >= 7 days advance the prescription by
 *    ONE tier (C3);
 *  - at most one automatic advancement per rolling 7-day period (C3);
 *  - first advancement also needs 7 days since enrollment at the tier (C3).
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

/**
 * Evaluate advancement under the owner-approved defaults (ADR 0002 §C3):
 *  - need >= 3 qualifying exposures for the variation;
 *  - exposures must span at least 7 days (maxDay - minDay >= 7);
 *  - at most one automatic advancement per rolling 7-day window;
 *  - first advancement requires 7 days since enrollment at the tier.
 * ABOVE ratings, extra rounds and same-day sessions never accelerate this.
 */
fun evaluateAdvancement(input: AdvancementInput): AdvancementResult {
    val p = input.policy
    val next = p.nextTierIndex(input.currentTier) ?: return AdvancementResult.NoChange

    val relevant = input.exposures.filter { it.variationId == p.variationId }
    if (relevant.size < 3) return AdvancementResult.NoChange

    val days = relevant.map { it.day }.distinct().sorted()
    val span = days.last() - days.first()
    if (span < 7) return AdvancementResult.NoChange // e.g. days 0/2/6 -> no

    if (input.nowDay - input.enrolledAtDay < 7) return AdvancementResult.NoChange

    input.lastAutoAdvancementDay?.let { last ->
        if (input.nowDay - last < 7) return AdvancementResult.NoChange
    }

    return AdvancementResult.Advance(input.currentTier, next, days)
}
