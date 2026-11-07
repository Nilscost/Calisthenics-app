// V20 (doc 17 §2.3): the big line at the top of the workout screen. Circuit: ROUND 2 OF 4. Pairs: PAIR 1 · SET 2 OF 3.
// Straight sets: EXERCISE 2 · SET 1 OF 3. Pure, so it can be tested without a screen.
package app.calisthenics.domain.session

import app.calisthenics.domain.model.BlockType
import app.calisthenics.domain.model.TimelineBlock
import app.calisthenics.domain.model.WorkoutFormat
import app.calisthenics.domain.model.WorkoutPlan

/** R7-T02 (B6): WARMUP and COOLDOWN count only the blocks of their own type: WARM-UP 3 OF 8. */
enum class HeaderKind { ROUND, PAIR, EXERCISE, STEP, WARMUP, COOLDOWN }

/** [group] is the pair or exercise number (null for a circuit), [set] the current set and [sets] how many sets there are. */
data class HeaderInfo(val kind: HeaderKind, val group: Int?, val set: Int, val sets: Int)

fun headerFor(plan: WorkoutPlan, b: TimelineBlock, blockIndex: Int): HeaderInfo {
    // R7-T02 (B6): the warm-up and the cool-down count their own blocks only; a 5 s get-ready inside them belongs to the next block
    val region = when (b.type) {
        BlockType.WARMUP, BlockType.COOLDOWN -> b.type
        BlockType.TRANSITION -> if (b.roundIndex == null) plan.blocks.drop(blockIndex + 1).firstOrNull { it.type != BlockType.TRANSITION }?.type?.takeIf { it == BlockType.WARMUP || it == BlockType.COOLDOWN } else null
        else -> null
    }
    if (region != null) {
        val total = plan.blocks.count { it.type == region }
        val before = plan.blocks.withIndex().count { (i, x) -> x.type == region && i <= blockIndex }
        val n = (if (b.type == region) before else before + 1).coerceIn(1, maxOf(total, 1))
        return HeaderInfo(if (region == BlockType.WARMUP) HeaderKind.WARMUP else HeaderKind.COOLDOWN, null, n, maxOf(total, 1))
    }
    val set = b.roundIndex ?: return HeaderInfo(HeaderKind.STEP, null, blockIndex + 1, plan.blocks.size) // anything else without a set: "step n of N"
    return when (plan.format) {
        WorkoutFormat.CIRCUIT -> HeaderInfo(HeaderKind.ROUND, null, set, plan.rounds)
        WorkoutFormat.PAIRS -> HeaderInfo(HeaderKind.PAIR, b.groupIndex, set, plan.rounds)
        WorkoutFormat.STRAIGHT -> HeaderInfo(HeaderKind.EXERCISE, b.groupIndex, set, plan.rounds)
    }
}
