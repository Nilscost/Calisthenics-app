// V27 (R29, D9): the suggestion card in Progress. At most one suggestion at a time, dismissible, never blocking anything.
// Everything here only points at what could come next; the app is complete with bodyweight only (D9), nothing is required.
package app.calisthenics.domain.progression

import app.calisthenics.domain.equipment.isAvailable
import app.calisthenics.domain.equipment.needSatisfied
import app.calisthenics.domain.goals.Goals
import app.calisthenics.domain.load.loadEquipmentId
import app.calisthenics.domain.load.loadGramsFor
import app.calisthenics.domain.load.loadLadder
import app.calisthenics.domain.model.*

sealed interface Suggestion {
    /** Stable key: dismissing a suggestion stores it, so the same one does not come back (a different step or weight is a different suggestion). */
    val id: String

    /** D7: two sessions in a row felt too easy; the next level is offered and only changes when the person accepts. */
    data class RaiseLevel(val variationId: String, val toTier: Int) : Suggestion { override val id get() = "raise:$variationId:$toTier" }
    /** All five stars at the weight in use; the next weight step of the ladder opens more. */
    data class HeavierWeight(val variationId: String, val equipmentId: String, val nextGrams: Int, /** R7-T05: >0 = offered because of "too easy" at the top level; each new pair of "too easy" is a new suggestion. */ val round: Int = 0) : Suggestion {
        override val id get() = "weight:$equipmentId:$nextGrams" + if (round > 0) ":$round" else ""
    }
    /** R7-T05 (B9): at the top level, two "too easy" in a row offer the next exercise of the progression. [round] counts the pairs, so a closed suggestion returns when the count reaches 2 once more. */
    data class NextExercise(val variationId: String, val toVariationId: String, val round: Int) : Suggestion { override val id get() = "next:$variationId:$toVariationId:$round" }
    /** A next step the person has earned the prerequisites for needs equipment the profile does not have. */
    data class GetEquipment(val equipmentId: String, val unlocksVariationId: String) : Suggestion { override val id get() = "equip:$equipmentId" }
}

/** Equipment that opens an exercise without a prerequisite: shown once the named exercise has reached the tier (e.g. dips after a solid push-up). */
private data class Hint(val equipmentId: String, val afterVariation: String, val afterTier: Int, val unlocks: String)
private val hints = listOf(
    Hint("dip-support", "pushup-standard", 4, "dip-support-hold"),
    Hint("pullup-bar", "row-band", 4, "scapular-pull"),
)

private fun mastered(p: ProgressSnapshot, v: String, tier: Int) = p.variations[v]?.let { tier in it.achievedTiers || it.tier > tier } ?: false

/** The equipment ids the cheapest alternative of [v] still lacks (capabilities such as a wall are not equipment to buy and are ignored). */
private fun missingEquipment(v: ExerciseVariation, profile: EquipmentProfile): List<String> =
    v.equipmentAlternatives.map { set -> set.needs.filter { !needSatisfied(it, profile) }.map { it.equipmentId } }.minByOrNull { it.size } ?: emptyList()

/** At most one suggestion; [dismissed] are the ids the person closed. Order: a raise they can accept, a heavier weight, then new equipment. */
fun suggest(c: Catalog, profile: EquipmentProfile, p: ProgressSnapshot, dismissed: Set<String>): Suggestion? {
    // 1. D7
    p.variations.values.filter { it.raiseSuggested && it.tier < 5 }.sortedBy { it.variationId }
        .map { Suggestion.RaiseLevel(it.variationId, it.tier + 1) }.firstOrNull { it.id !in dismissed }?.let { return it }
    // 1b. R7-T05 (B9): top level and two "too easy" in a row: the next exercise (or the next weight step of a loaded one)
    for (vp in p.variations.values.sortedBy { it.variationId }) {
        if (vp.tier < 5 || vp.consecutiveAbove < ABOVE_SUGGEST_AFTER) continue
        val v = c.variation(vp.variationId) ?: continue
        val round = vp.consecutiveAbove / ABOVE_SUGGEST_AFTER
        val eq = v.loadEquipmentId()
        if (eq != null) {
            val inUse = vp.loadGrams ?: profile.loadGramsFor(v) ?: continue
            val next = loadLadder(eq).firstOrNull { it > inUse } ?: continue
            if ((profile.loadGramsFor(v) ?: 0) > inUse) continue
            Suggestion.HeavierWeight(v.id, eq, next, round).takeIf { it.id !in dismissed }?.let { return it }
        } else {
            val to = c.policyFor(v)?.nextVariationIds.orEmpty().firstOrNull { n -> c.variation(n)?.let { isAvailable(it, profile) } == true && Goals.unmet(c, n, p).filter { it.variationId != v.id }.isEmpty() } ?: continue
            Suggestion.NextExercise(v.id, to, round).takeIf { it.id !in dismissed }?.let { return it }
        }
    }
    // 2. a heavier weight when every level is earned at the weight in use
    for (v in c.variations.filter { it.kind == Kind.REPS || it.kind == Kind.HOLD }.sortedBy { it.id }) {
        val eq = v.loadEquipmentId() ?: continue
        val vp = p.variations[v.id] ?: continue
        if (vp.earnedStars() < 5) continue
        val inUse = vp.loadGrams ?: profile.loadGramsFor(v) ?: continue
        if ((profile.loadGramsFor(v) ?: 0) > inUse) continue   // already owns a heavier one
        val next = loadLadder(eq).firstOrNull { it > inUse } ?: continue
        Suggestion.HeavierWeight(v.id, eq, next).takeIf { it.id !in dismissed }?.let { return it }
    }
    // 3. equipment that opens the step the person has earned
    val candidates = mutableListOf<Pair<Int, Suggestion.GetEquipment>>()
    for (v in c.variations.filter { (it.kind == Kind.REPS || it.kind == Kind.HOLD) && !isAvailable(it, profile) }) {
        val missing = missingEquipment(v, profile).distinct()
        if (missing.size != 1) continue                                     // one thing to get, not a shopping list
        if (v.id in p.variations) continue                                  // already started: it does not need to be pointed at
        val pol = c.policyFor(v) ?: continue
        val earned = pol.prerequisiteRule.allOf.isNotEmpty() && Goals.unmet(c, v.id, p).isEmpty()
        val hinted = hints.firstOrNull { it.unlocks == v.id && mastered(p, it.afterVariation, it.afterTier) }
        if (earned || hinted != null) candidates += v.difficultyRank to Suggestion.GetEquipment(missing.single(), v.id)
    }
    return candidates.sortedWith(compareBy({ it.first }, { it.second.unlocksVariationId })).map { it.second }.firstOrNull { it.id !in dismissed }
}
