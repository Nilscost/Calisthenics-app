// V20 (doc 17 §2.3): the big line at the top of the workout screen. Circuit: ROUND 2 OF 4. Pairs: PAIR 1 · SET 2 OF 3.
// Straight sets: EXERCISE 2 · SET 1 OF 3. Pure, so it can be tested without a screen.
package app.calisthenics.domain.session

import app.calisthenics.domain.model.TimelineBlock
import app.calisthenics.domain.model.WorkoutFormat
import app.calisthenics.domain.model.WorkoutPlan

enum class HeaderKind { ROUND, PAIR, EXERCISE, STEP }

/** [group] is the pair or exercise number (null for a circuit), [set] the current set and [sets] how many sets there are. */
data class HeaderInfo(val kind: HeaderKind, val group: Int?, val set: Int, val sets: Int)

fun headerFor(plan: WorkoutPlan, b: TimelineBlock, blockIndex: Int): HeaderInfo {
    val set = b.roundIndex ?: return HeaderInfo(HeaderKind.STEP, null, blockIndex + 1, plan.blocks.size) // warm-up / cool-down: "step n of N"
    return when (plan.format) {
        WorkoutFormat.CIRCUIT -> HeaderInfo(HeaderKind.ROUND, null, set, plan.rounds)
        WorkoutFormat.PAIRS -> HeaderInfo(HeaderKind.PAIR, b.groupIndex, set, plan.rounds)
        WorkoutFormat.STRAIGHT -> HeaderInfo(HeaderKind.EXERCISE, b.groupIndex, set, plan.rounds)
    }
}
