// L01 (docs/15, report "One gate, five tiers"): five tiers per step from one shared vocabulary instead of hand-written numbers,
// and the RR rule as one function: all sets reach the gate -> move on to the next step and start again at tier 1; cannot reach tier 1 -> one step back.
// Only the endpoints have sources (RR 3x5 start, 3x8 gate, 30 s holds, 10 s dip descents, 8-12 core range); the numbers between are DRAFT.
package app.calisthenics.domain.progression

import app.calisthenics.domain.model.TierProfile

/** One tier: [sets] sets of [value] reps (or seconds for holds). [seconds] = seconds per rep of a negative's descent. */
data class TierSpec(val sets: Int, val value: Int, val seconds: Int? = null)

private fun reps(vararg v: Int) = v.map { TierSpec(3, it) }

/** The five tiers of a profile, easiest first. */
fun TierProfile.specs(): List<TierSpec> = when (this) {
    TierProfile.R, TierProfile.RU -> reps(5, 6, 7, 8, 10)
    TierProfile.C -> reps(8, 9, 10, 11, 12)
    TierProfile.H30 -> reps(10, 15, 20, 25, 30)
    TierProfile.H60 -> listOf(TierSpec(6, 10), TierSpec(4, 15), TierSpec(3, 20), TierSpec(2, 30), TierSpec(1, 60))   // accumulate one minute
    TierProfile.N -> listOf(TierSpec(3, 3, 3), TierSpec(3, 4, 5), TierSpec(3, 5, 5), TierSpec(3, 5, 8), TierSpec(3, 5, 10))
    TierProfile.E -> reps(15, 18, 20, 22, 25)
    TierProfile.M -> List(5) { TierSpec(1, 10) }   // McGill: 5-3-1 reps of 8-10 s holds, maintenance only: no progression
}

val TierProfile.isHold: Boolean get() = this == TierProfile.H30 || this == TierProfile.H60 || this == TierProfile.M

/** The tier whose numbers move you on (R: tier 4, the RR 3x8; the others: tier 5). Null = maintenance, no gate. */
val TierProfile.gateTier: Int? get() = when (this) { TierProfile.R, TierProfile.RU -> 4; TierProfile.M -> null; else -> 5 }

sealed interface TierVerdict {
    /** All sets reached the gate: move on to the next step of the progression and start again at tier 1. */
    data object MoveOn : TierVerdict
    /** Stay at this step and tier. */
    data object Stay : TierVerdict
    /** Tier 1 was not reached: one step back (or a lower tier of the step before). */
    data object StepBack : TierVerdict
}

/**
 * The RR rule as one function. [tier] is the tier the sets were done at, [values] the number reached in each set (unlogged sets already
 * resolved, see Rule.kt). At the gate tier with every set at the gate number: [TierVerdict.MoveOn]. At tier 1 with any set below tier 1: [TierVerdict.StepBack].
 */
fun tierVerdict(profile: TierProfile, tier: Int, values: List<Int>): TierVerdict {
    val specs = profile.specs()
    val spec = specs.getOrNull(tier - 1) ?: return TierVerdict.Stay
    val done = values.size >= spec.sets
    val gate = profile.gateTier
    if (gate != null && tier >= gate && done && values.take(spec.sets).all { it >= spec.value }) return TierVerdict.MoveOn
    if (tier == 1 && (!done || values.take(spec.sets).any { it < spec.value })) return TierVerdict.StepBack
    return TierVerdict.Stay
}
