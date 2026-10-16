package app.calisthenics.domain.planner

import app.calisthenics.domain.model.*

const val SIDE_CHANGE_PREPARATION_SECONDS = 5

/** Changing sides is setup time in addition to the full prescribed hold. */
fun needsSideChangePreparation(previous: TimelineBlock?, next: TimelineBlock): Boolean =
    previous != null && previous.variationId != null && previous.variationId == next.variationId &&
        previous.type == next.type && previous.side == Side.LEFT && next.side == Side.RIGHT &&
        ((previous.target?.type == TargetType.HOLD_SECONDS && next.target?.type == TargetType.HOLD_SECONDS) ||
            (previous.target == null && next.target == null && previous.type in setOf(BlockType.STRETCH, BlockType.COOLDOWN)))

fun withSideChangePreparation(blocks: List<TimelineBlock>): List<TimelineBlock> = buildList {
    for (block in blocks) {
        if (needsSideChangePreparation(lastOrNull(), block)) add(TimelineBlock(
            id = "side-ready-${block.id}", type = BlockType.TRANSITION,
            durationSeconds = SIDE_CHANGE_PREPARATION_SECONDS,
            roundIndex = block.roundIndex, slotId = block.slotId, groupIndex = block.groupIndex,
        ))
        add(block)
    }
}
