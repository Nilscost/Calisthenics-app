// V21 (O2, owner answer Q1): judging a session under the "Rep range" and "Custom" rules.
package app.calisthenics.domain.progression

import app.calisthenics.domain.model.ProgressionRule

/**
 * The number per set for one exercise: what was logged, and for a set that was not logged the number of the same set last time
 * (no progress, no regression: owner answer to question 1), or the bottom of the range when there is no last time.
 */
fun resolveSetValues(logged: List<Int?>, last: List<Int?>?, bottom: Int): List<Int> =
    logged.mapIndexed { i, v -> v ?: last?.getOrNull(i) ?: bottom }

/** The top of the range for this kind of exercise. */
fun ruleTop(rule: ProgressionRule, isHold: Boolean) = if (isHold) rule.holdTo else rule.to
fun ruleBottom(rule: ProgressionRule, isHold: Boolean) = if (isHold) rule.holdFrom else rule.from

/** A session meets the rule when the planned number of sets was done and every set reached the top of the range. */
fun ruleMet(rule: ProgressionRule, isHold: Boolean, values: List<Int>): Boolean =
    rule.isRange && values.size >= rule.sets && values.take(rule.sets).all { it >= ruleTop(rule, isHold) }
