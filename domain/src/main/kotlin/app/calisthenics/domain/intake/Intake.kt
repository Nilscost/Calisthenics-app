// Maps "what I do today" (reps or seconds for one comfortable set) to a starting step. Conservative: highest step whose target
// the person already meets, never above it. Weighted or unlisted variants are NOT credited extra: the caller says so to the user.
package app.calisthenics.domain.intake

import app.calisthenics.domain.model.Tier

/** Highest tier index whose target value is <= [value]; 1 when below the first step. */
fun suggestStartingStep(tiers: List<Tier>, value: Int): Int =
    tiers.filter { it.target.value <= value }.maxOfOrNull { it.index } ?: tiers.minOf { it.index }

/** True when the person already exceeds the top step, so the app should say the ladder is too easy for them. */
fun exceedsLadder(tiers: List<Tier>, value: Int): Boolean = value > tiers.maxOf { it.target.value }
