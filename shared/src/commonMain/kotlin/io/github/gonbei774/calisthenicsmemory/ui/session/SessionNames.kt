package io.github.gonbei774.calisthenicsmemory.ui.session

import app.calisthenics.domain.model.Side
import app.calisthenics.domain.model.WorkoutPlan

private const val PER_SIDE_SUFFIX = " (per side)"

/**
 * The name as the workout screen shows it. When the side is shown next to it ("Left side") a trailing " (per side)" in the catalog name would say
 * the same thing twice, so it is dropped here; the catalog name itself is not changed.
 */
fun sessionDisplayName(name: String, side: Side): String =
    if ((side == Side.LEFT || side == Side.RIGHT) && name.endsWith(PER_SIDE_SUFFIX)) name.removeSuffix(PER_SIDE_SUFFIX) else name

/** [names] with the " (per side)" suffix removed for every exercise the [plan] works one side at a time. */
fun sessionDisplayNames(plan: WorkoutPlan, names: Map<String, String>): Map<String, String> {
    val sided = plan.blocks.filter { it.side == Side.LEFT || it.side == Side.RIGHT }.mapNotNull { it.variationId }.toSet()
    if (sided.isEmpty()) return names
    return names.mapValues { (id, name) -> if (id in sided) sessionDisplayName(name, Side.LEFT) else name }
}
