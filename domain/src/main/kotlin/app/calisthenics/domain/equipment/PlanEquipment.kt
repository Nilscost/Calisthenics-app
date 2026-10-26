// Owner 2026-10-26: the Preview lists what to prepare before a session. Pure.
package app.calisthenics.domain.equipment

import app.calisthenics.domain.model.Catalog
import app.calisthenics.domain.model.EquipmentProfile
import app.calisthenics.domain.model.ExerciseVariation

/** The equipment ids this exercise needs with [profile]: its first alternative the profile satisfies (the one the planner uses),
 *  else its first alternative. Empty for a bodyweight exercise. */
fun equipmentNeededFor(v: ExerciseVariation, profile: EquipmentProfile): List<String> {
    val alt = v.equipmentAlternatives.firstOrNull { setSatisfied(it, profile) } ?: v.equipmentAlternatives.firstOrNull() ?: return emptyList()
    return alt.needs.map { it.equipmentId }
}

/** What the exercises of a plan need together (warm-up, work, stretches and cool-down alike), each once, in the shared checklist order. */
fun planEquipment(catalog: Catalog, variationIds: Collection<String>, profile: EquipmentProfile): List<String> =
    variationIds.distinct().mapNotNull { catalog.variation(it) }.flatMap { equipmentNeededFor(it, profile) }.distinct().sortedBy { EquipmentOptions.orderOf(it) }
