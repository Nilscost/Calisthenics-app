// L4 (release 9.3, doc 43): gentler steps inside a level. The ONE place that decides the prescribed target of a rep or hold exercise at a level.
//
//   target = levelTarget + min(gap - 1, floor(done * gap / need)),   gap = nextLevelTarget - levelTarget
//
// [done] / [need] is the person's progress towards the next level, read from the qualifying days with the same function the end screen uses for its dots
// ([levelProgress]): 0, 1, 2 of 3 on the 14-day path; 0..3 of 4 on the weekly path (4 in 30 days). So the number never reaches the next level's target
// before the level-up itself (the cap gap - 1), on either path. Holds climb in 5 s steps (the addition is rounded down to a multiple of 5 s).
//
// The ramp changes the next prescription only. What counts as a good session is judged against the level's own target ([TimelineBlock.levelTarget]),
// so levels, stars and good-session counts replay exactly as before.
package app.calisthenics.domain.progression

import app.calisthenics.domain.model.ProgressionPolicy
import app.calisthenics.domain.model.Target
import app.calisthenics.domain.model.TargetType
import app.calisthenics.domain.planner.cappedTarget
import app.calisthenics.domain.session.levelProgress

/** Holds climb in steps of this many seconds. */
const val HOLD_RAMP_STEP_SECONDS = 5

/** The result of [prescribedTarget]: what to show and prescribe, and the level's own target when the prescription is above it. */
data class RampedTarget(val target: Target, val levelTarget: Target) {
    /** True when the prescription is above the level's own target (then sets are still judged against [levelTarget]). */
    val ramped: Boolean get() = target.value > levelTarget.value
}

/**
 * The prescribed per-set target of [tier] of [policy] after [goodDays] (the person's qualifying days at this level, own and same-rung, e.g.
 * [ProgressionEngine.qualifyingDays]). Reps and hold seconds only: no ramp for a load, a range (rep-range rule), the top level (no next level) or a gap of 1.
 * For ALTERNATING and SPLIT exercises the stored number is per side and so is the ramp.
 */
fun prescribedTarget(policy: ProgressionPolicy, tier: Int, goodDays: List<Int>): RampedTarget? {
    val level = policy.tiers.firstOrNull { it.index == tier }?.target?.let { cappedTarget(it, policy) } ?: return null
    val none = RampedTarget(level, level)
    if (level.max != null) return none
    val next = policy.tiers.firstOrNull { it.index == tier + 1 }?.target?.let { cappedTarget(it, policy) } ?: return none
    if (next.type != level.type || next.max != null) return none
    val gap = next.value - level.value
    if (gap <= 1) return none
    val progress = levelProgress(goodDays, tier)
    val steps = gap * progress.have / progress.need
    val room = (gap - 1).coerceAtLeast(0)
    var add = minOf(room, steps)
    if (level.type == TargetType.HOLD_SECONDS) add = add / HOLD_RAMP_STEP_SECONDS * HOLD_RAMP_STEP_SECONDS
    return RampedTarget(level.copy(value = level.value + add), level)
}
