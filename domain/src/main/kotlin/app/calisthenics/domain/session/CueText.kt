// U03 (F11, Q4): the words shown and spoken for a block. Pure, so it can be tested.
// With stretch on the recovery IS the stretch: no block and no cue is ever called "Rest".
// The 5 s position change is "Get ready: <next exercise>".
package app.calisthenics.domain.session

import app.calisthenics.domain.model.BlockType
import app.calisthenics.domain.model.TimelineBlock
import app.calisthenics.domain.model.WorkoutPlan

object CueText {
    /** Title of the block. [names] maps variation id -> display name. */
    fun label(plan: WorkoutPlan, blockId: String, names: Map<String, String>): String {
        val i = plan.blocks.indexOfFirst { it.id == blockId }
        if (i < 0) return ""
        val b = plan.blocks[i]
        val name = b.variationId?.let { names[it] ?: it }
        return when (b.type) {
            BlockType.PASSIVE_RECOVERY -> if (plan.stretchOn) "Recover" else "Rest"
            BlockType.TRANSITION -> nextExercise(plan, i)?.let { "Get ready: ${names[it.variationId] ?: it.variationId}" } ?: "Get ready"
            BlockType.STRETCH -> "Stretch: " + (name ?: "")
            else -> name ?: "Go"
        }.trim()
    }

    /** What to announce as "Next": skips the short get-ready block so the user hears the exercise itself. */
    fun nextLabel(plan: WorkoutPlan, blockId: String, names: Map<String, String>): String? {
        val i = plan.blocks.indexOfFirst { it.id == blockId }
        val n = plan.blocks.drop(i + 1).firstOrNull { it.type != BlockType.TRANSITION } ?: return null
        return label(plan, n.id, names)
    }

    private fun nextExercise(plan: WorkoutPlan, from: Int): TimelineBlock? =
        plan.blocks.drop(from + 1).firstOrNull { it.variationId != null && it.type != BlockType.TRANSITION }
}
