// W2 (owner decision): after discomfort the next workouts automatically prescribe ONE step easier - never harder, never a dropped exercise.
package app.calisthenics.domain.progression

import app.calisthenics.domain.model.Catalog

/** An easier prescription: [variationId] at level [tier]. */
data class EasierStep(val variationId: String, val tier: Int)

/**
 * One step easier than [variationId] at level [tier]: the level below on the same exercise; at level 1 the previous rung of the same route
 * (at its top level); and when nothing lower exists the same exercise at level 1 (it is kept, not dropped).
 * [available] lets the caller skip rungs it cannot prescribe (equipment, exclusions).
 */
fun easierStep(catalog: Catalog, variationId: String, tier: Int, routeId: String? = null, available: (String) -> Boolean = { true }): EasierStep {
    if (tier > 1) return EasierStep(variationId, tier - 1)
    val route = catalog.routeById(routeId)?.takeIf { variationId in it.mainPath || it.members.any { m -> m.variationId == variationId } }
        ?: catalog.mainRouteFor(variationId)
    val earlier: List<String> = if (route != null) {
        val anchor = catalog.routeAnchorFor(variationId, route)
        val at = anchor?.let { route.mainPath.indexOf(it) } ?: -1
        if (at > 0) route.mainPath.take(at).reversed() else emptyList()
    } else {
        val out = mutableListOf<String>(); var id = variationId
        while (true) {
            val prev = catalog.variations.firstOrNull { v -> catalog.policyForVariation(v.id)?.progressionSuccessors()?.contains(id) == true }?.id ?: break
            if (prev in out || prev == variationId) break
            out += prev; id = prev
        }
        out
    }
    for (candidate in earlier) {
        val pol = catalog.policyForVariation(candidate) ?: continue
        if (catalog.variation(candidate) == null || !available(candidate)) continue
        return EasierStep(candidate, pol.tiers.maxOf { it.index })
    }
    return EasierStep(variationId, 1)
}
