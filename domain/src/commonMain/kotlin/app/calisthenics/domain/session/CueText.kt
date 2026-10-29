// U03 (F11, Q4): the words shown and spoken for a block. Pure, so it can be tested.
// With stretch on the recovery IS the stretch: no block and no cue is ever called "Rest".
// The 5 s position change is "Get ready: <next exercise>".
package app.calisthenics.domain.session

import app.calisthenics.domain.model.BlockType
import app.calisthenics.domain.model.Side
import app.calisthenics.domain.model.Target
import app.calisthenics.domain.model.TargetType
import app.calisthenics.domain.model.alternatingTotal
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

    /**
     * What is spoken after the title of a block that works both sides in one set: "12 reps, 6 per side" (holds: "40 seconds, 20 seconds per side").
     * Null for every other block (single-sided blocks announce their side in the title; the rest say nothing extra).
     */
    fun alternatingPhrase(b: TimelineBlock): String? {
        val t = b.target?.takeIf { b.alternating && b.type == BlockType.WORK } ?: return null
        return "${amount(t.alternatingTotal())}, ${if (t.type == TargetType.REPS) t.shownValue().toString() else amount(t)} per side"
    }

    /** The spoken title plus, for an alternating block, its total and per-side numbers. */
    fun spoken(plan: WorkoutPlan, blockId: String, names: Map<String, String>): String {
        val title = label(plan, blockId, names)
        val phrase = plan.blocks.firstOrNull { it.id == blockId }?.let(::alternatingPhrase)
        return if (phrase == null) title else "$title. $phrase"
    }

    private fun amount(t: Target): String {
        val n = t.shownValue()
        return if (t.type == TargetType.REPS) "$n reps" else "$n seconds"
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
