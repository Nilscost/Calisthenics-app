// U04 (F5): editable equipment profiles. The editor shows a checklist of known equipment; this file turns a checklist
// into an EquipmentProfile and back, and holds the list rules (unique ids, never delete the last profile). Pure.
package app.calisthenics.domain.equipment

import app.calisthenics.domain.model.EquipmentItem
import app.calisthenics.domain.model.EquipmentProfile

/** [CAPABILITY] entries (a wall) are not items you own; they go into [EquipmentProfile.capabilities]. */
enum class OptionKind { ITEM, CAPABILITY }

/**
 * One row of the editor checklist. [id] is the catalog equipment id (or capability id).
 * [weighted] rows get a weight field; [future] rows are kept in the profile but no exercise needs them yet.
 */
data class EquipmentOption(
    val id: String,
    val kind: OptionKind = OptionKind.ITEM,
    val weighted: Boolean = false,
    val defaultMassGrams: Int? = null,
    val quantity: Int = 1,
    val suitability: Set<String> = emptySet(),
    val strengthLabel: String? = null,
    val future: Boolean = false,
)

object EquipmentOptions {
    const val FLOOR_SPACE = "floor-space"
    /** Display order of the checklist. Ids match `equipmentId` in the catalog (kettlebell, weight, chair, ...). */
    val all = listOf(
        EquipmentOption("pullup-bar"),
        EquipmentOption("high-bar"),          // hang with the feet clear of the floor (muscle-up, levers)
        EquipmentOption("low-bar"),           // low bar / sturdy table for inverted rows
        // Ticking a band means you can anchor it (a door anchor, a post): row-band needs "stable-anchor".
        EquipmentOption("resistance-band", strengthLabel = "assorted", suitability = setOf("stable-anchor")),
        EquipmentOption("kettlebell", weighted = true, defaultMassGrams = 12_000),
        EquipmentOption("weight", weighted = true, defaultMassGrams = 2_500, quantity = 2), // dumbbells, a pair
        EquipmentOption("chair", suitability = setOf("stable")),
        EquipmentOption("wall", kind = OptionKind.CAPABILITY),
        EquipmentOption("mat"),
        EquipmentOption("barbell", weighted = true, defaultMassGrams = 20_000),          // V27: the bar and plates, in total
        EquipmentOption("weighted-vest", weighted = true, defaultMassGrams = 5_000),     // V27: a load for push-ups, pull-ups, dips and squats
        EquipmentOption("dip-support"),       // V21b: parallel bars, two sturdy chairs or a counter corner (dips, support hold)
        EquipmentOption("foot-anchor"),       // V21b: a sofa or heavy furniture to hook the heels under (Nordic curls)
        EquipmentOption("parallettes", future = true),
    )
    fun byId(id: String) = all.firstOrNull { it.id == id }
}

/** A ticked row. [massGrams] only for weighted options. */
data class EquipmentSelection(val id: String, val massGrams: Int? = null)

fun buildProfile(id: String, name: String, selection: List<EquipmentSelection>): EquipmentProfile {
    val chosen = selection.distinctBy { it.id }.mapNotNull { sel -> EquipmentOptions.byId(sel.id)?.let { it to sel } }
    val items = EquipmentOptions.all.mapNotNull { opt ->
        val sel = chosen.firstOrNull { it.first.id == opt.id }?.second ?: return@mapNotNull null
        if (opt.kind != OptionKind.ITEM) return@mapNotNull null
        EquipmentItem(opt.id, opt.quantity, if (opt.weighted) (sel.massGrams ?: opt.defaultMassGrams) else null, opt.strengthLabel, opt.suitability)
    }
    val caps = setOf(EquipmentOptions.FLOOR_SPACE) + chosen.filter { it.first.kind == OptionKind.CAPABILITY }.map { it.first.id }
    return EquipmentProfile(id, name.trim(), items, caps)
}

fun selectionOf(profile: EquipmentProfile): List<EquipmentSelection> {
    val fromItems = profile.items.filter { EquipmentOptions.byId(it.equipmentId)?.kind == OptionKind.ITEM }.map { EquipmentSelection(it.equipmentId, it.massGrams) }
    val fromCaps = profile.capabilities.filter { EquipmentOptions.byId(it)?.kind == OptionKind.CAPABILITY }.map { EquipmentSelection(it) }
    return fromItems + fromCaps
}

sealed interface ProfileEditResult {
    data class Ok(val profiles: List<EquipmentProfile>, val selectedId: String) : ProfileEditResult
    data class Refused(val reason: String) : ProfileEditResult
}

object ProfileOps {
    /** "Gym" -> "gym"; "Gym" again -> "gym-2". */
    fun newId(name: String, existing: List<EquipmentProfile>): String {
        val base = name.trim().lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifEmpty { "profile" }
        var id = base; var n = 2
        while (existing.any { it.id == id }) id = "$base-${n++}"
        return id
    }

    fun upsert(profiles: List<EquipmentProfile>, p: EquipmentProfile): List<EquipmentProfile> =
        if (profiles.any { it.id == p.id }) profiles.map { if (it.id == p.id) p else it } else profiles + p

    /** Never deletes the last profile. If the selected one goes, the first remaining is selected. */
    fun delete(profiles: List<EquipmentProfile>, id: String, selectedId: String): ProfileEditResult {
        if (profiles.none { it.id == id }) return ProfileEditResult.Refused("unknown profile")
        if (profiles.size <= 1) return ProfileEditResult.Refused("last profile")
        val rest = profiles.filter { it.id != id }
        return ProfileEditResult.Ok(rest, if (selectedId == id || rest.none { it.id == selectedId }) rest.first().id else selectedId)
    }

    fun nameProblem(name: String, profiles: List<EquipmentProfile>, editingId: String?): NameProblem? = when {
        name.isBlank() -> NameProblem.EMPTY
        profiles.any { it.id != editingId && it.name.trim().equals(name.trim(), ignoreCase = true) } -> NameProblem.DUPLICATE
        else -> null
    }
}

enum class NameProblem { EMPTY, DUPLICATE }
