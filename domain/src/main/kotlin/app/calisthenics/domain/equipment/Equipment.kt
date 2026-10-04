// T08 — equipment profiles, suitability and substitution (spec §3, EQ-01..EQ-04).
package app.calisthenics.domain.equipment

import app.calisthenics.domain.model.EquipmentItem
import app.calisthenics.domain.model.EquipmentNeed
import app.calisthenics.domain.model.EquipmentProfile
import app.calisthenics.domain.model.ExerciseVariation
import app.calisthenics.domain.model.RequirementSet

/** Seed profiles exactly as approved (EQ-01). Suitability flags are NOT invented:
 *  e.g. the Travel chair starts without "stable" until the user confirms it. */
object SeedProfiles {
    val home = EquipmentProfile(
        id = "home",
        name = "Home",
        items = listOf(
            EquipmentItem("pullup-bar"),
            EquipmentItem("kettlebell", quantity = 1, massGrams = 12_000),
            EquipmentItem("weight", quantity = 2, massGrams = 2_500),
            EquipmentItem("mat"),
            EquipmentItem("resistance-band", quantity = 1, strengthLabel = "assorted"),
        ),
        capabilities = setOf("floor-space", "wall"),
    )
    val travel = EquipmentProfile(
        id = "travel",
        name = "Travel",
        items = listOf(EquipmentItem("chair")),
        capabilities = setOf("floor-space", "wall"),
    )
    val all = listOf(home, travel)
}

/** Session-only change of what is available today (EQ-02). Never edits the saved profile. */
data class AvailabilityOverlay(
    val unavailableEquipmentIds: Set<String> = emptySet(),
    val extraItems: List<EquipmentItem> = emptyList(),
) {
    fun applyTo(profile: EquipmentProfile): EquipmentProfile = profile.copy(
        items = profile.items.filter { it.equipmentId !in unavailableEquipmentIds } + extraItems,
    )
}

fun needSatisfied(need: EquipmentNeed, profile: EquipmentProfile): Boolean {
    val matching = profile.items.filter { item ->
        item.equipmentId == need.equipmentId &&
            item.suitability.containsAll(need.suitability) &&
            (need.minMassGrams == null || (item.massGrams != null && item.massGrams >= need.minMassGrams)) &&
            (need.maxMassGrams == null || (item.massGrams != null && item.massGrams <= need.maxMassGrams))
    }
    return matching.sumOf { it.quantity } >= need.quantity
}

/** AND inside the set. */
fun setSatisfied(set: RequirementSet, profile: EquipmentProfile): Boolean =
    profile.capabilities.containsAll(set.capabilities) && set.needs.all { needSatisfied(it, profile) }

/** OR across alternatives. */
fun isAvailable(v: ExerciseVariation, profile: EquipmentProfile): Boolean =
    v.equipmentAlternatives.isEmpty() || v.equipmentAlternatives.any { setSatisfied(it, profile) }

/** Human explanation of what is missing for the cheapest alternative. */
fun missingFor(v: ExerciseVariation, profile: EquipmentProfile): String {
    val best = v.equipmentAlternatives.minByOrNull { set ->
        set.needs.count { !needSatisfied(it, profile) } +
            (set.capabilities - profile.capabilities).size
    } ?: return "nothing"
    val parts = mutableListOf<String>()
    best.needs.filter { !needSatisfied(it, profile) }.forEach { n ->
        val extra = buildList {
            if (n.quantity > 1) add("x${n.quantity}")
            n.minMassGrams?.let { add("≥${it / 1000.0} kg") }
            if (n.suitability.isNotEmpty()) add("confirmed " + n.suitability.joinToString("/"))
        }
        parts += n.equipmentId + if (extra.isEmpty()) "" else " (" + extra.joinToString(", ") + ")"
    }
    (best.capabilities - profile.capabilities).forEach { parts += it }
    return parts.joinToString(" + ")
}

/** Equipment suggestion (EQ-04): only when it unlocks a variation the user cannot do now. */
data class EquipmentSuggestion(val id: String, val equipmentId: String, val unlocks: List<String>, val reason: String)

fun suggestEquipment(
    blockedVariations: List<ExerciseVariation>,
    profile: EquipmentProfile,
    dismissed: Set<String>,
): List<EquipmentSuggestion> {
    val byEquipment = sortedMapOf<String, MutableList<String>>()
    for (v in blockedVariations) {
        val set = v.equipmentAlternatives.minByOrNull { s -> s.needs.count { !needSatisfied(it, profile) } } ?: continue
        val missing = set.needs.filter { !needSatisfied(it, profile) }
        if (missing.size == 1 && (set.capabilities - profile.capabilities).isEmpty()) {
            byEquipment.getOrPut(missing.single().equipmentId) { mutableListOf() } += v.name
        }
    }
    return byEquipment.map { (eq, names) ->
        EquipmentSuggestion(
            id = "suggest-$eq",
            equipmentId = eq,
            unlocks = names.sorted(),
            reason = "Optional: a $eq would unlock ${names.sorted().joinToString(", ")}. Not required — dismiss anytime.",
        )
    }.filter { it.id !in dismissed }
}
