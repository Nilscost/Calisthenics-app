// U03 (F11, Q4): the words shown and spoken for a block. Pure, so it can be tested.
// With stretch on the recovery IS the stretch: no block and no cue is ever called "Rest".
// The 5 s position change is "Get ready: <next exercise>".
package app.calisthenics.domain.session

import app.calisthenics.domain.model.BlockType
import app.calisthenics.domain.model.Side
import app.calisthenics.domain.model.TimelineBlock
import app.calisthenics.domain.model.WorkoutPlan

object CueText {
    /** Title of the block. [names] maps variation id -> display name. */
    fun label(plan: WorkoutPlan, blockId: String, names: Map<String, String>): String {
        val i = plan.blocks.indexOfFirst { it.id == blockId }
        if (i < 0) return ""
        val b = plan.blocks[i]
        val name = b.variationId?.let { names[it] ?: it }
        val text = when (b.type) {
            BlockType.PASSIVE_RECOVERY -> if (plan.stretchOn) "Recover" else "Rest"
            BlockType.TRANSITION -> nextExercise(plan, i)?.let {
                "Prepare: ${(names[it.variationId] ?: it.variationId)}${sideLabel(it.side)}"
            } ?: "Prepare"
            BlockType.STRETCH -> "Stretch: " + (name ?: "")
            BlockType.COOLDOWN -> name?.let { "Cool-down: $it" } ?: "Relax"
            else -> name ?: "Go"
        }.trim()
        return if (b.type == BlockType.TRANSITION) text else "$text${sideLabel(b.side)}".trim()
    }

    /** What to announce as "Next": skips the short get-ready block so the user hears the exercise itself. */
    fun nextLabel(plan: WorkoutPlan, blockId: String, names: Map<String, String>): String? {
        val i = plan.blocks.indexOfFirst { it.id == blockId }
        if (plan.blocks.getOrNull(i)?.type == BlockType.TRANSITION) return null
        val n = plan.blocks.drop(i + 1).firstOrNull { it.type != BlockType.TRANSITION } ?: return null
        return label(plan, n.id, names)
    }

    private fun nextExercise(plan: WorkoutPlan, from: Int): TimelineBlock? =
        plan.blocks.drop(from + 1).firstOrNull { it.variationId != null && it.type != BlockType.TRANSITION }

    private fun sideLabel(side: Side): String = when (side) {
        Side.LEFT -> " — Left side"
        Side.RIGHT -> " — Right side"
        else -> ""
    }
}
