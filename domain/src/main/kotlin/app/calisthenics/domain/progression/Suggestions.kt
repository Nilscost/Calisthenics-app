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

    /** D7, R7.2-T06: a session felt too easy; a jump of two levels (or to the top) is offered and only changes when the person accepts. [round] counts further "too easy" sessions, so a closed card returns. */
    data class RaiseLevel(val variationId: String, val toTier: Int, val round: Int = 1) : Suggestion { override val id get() = "raise:$variationId:$toTier" + if (round > 1) ":$round" else "" }
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

/** The move on offered after "too easy" (R7.2-T06), or null when there is none. */
private fun moveOn(c: Catalog, profile: EquipmentProfile, p: ProgressSnapshot, v: ExerciseVariation, vp: VariationProgress): Suggestion? {
    val pol = c.policyFor(v)
    val top = pol?.tiers?.maxOfOrNull { it.index } ?: 5
    val round = vp.consecutiveAbove.coerceAtLeast(1)
    fun next(): String? = if (c.exerciseGroups.any { v.id in it.alternativeVariationIds }) null else pol?.progressionSuccessors().orEmpty().firstOrNull { n -> c.variation(n)?.let { isAvailable(it, profile) } == true && Goals.unmet(c, n, p).filter { it.variationId != v.id }.isEmpty() }
    if (vp.ruleAbove) return next()?.let { Suggestion.NextExercise(v.id, it, round) }        // a Rep range rule has no levels
    val eq = v.loadEquipmentId()
    if (vp.tier + 2 <= top) return Suggestion.RaiseLevel(v.id, vp.tier + 2, round)
    if (eq != null) {
        if (vp.tier < top) return Suggestion.RaiseLevel(v.id, top, round)                    // weighted: 4 -> 5
        val inUse = vp.loadGrams ?: profile.loadGramsFor(v) ?: return null
        val next = loadLadder(eq).firstOrNull { it > inUse } ?: return null
        if ((profile.loadGramsFor(v) ?: 0) > inUse) return null
        return Suggestion.HeavierWeight(v.id, eq, next, round)
    }
    next()?.let { return Suggestion.NextExercise(v.id, it, round) }
    return if (vp.tier < top) Suggestion.RaiseLevel(v.id, top, round) else null              // no next exercise: 4 -> 5, nothing at 5
}

/** At most one suggestion; [dismissed] are the ids the person closed. Order: a raise they can accept, a heavier weight, then new equipment. */
fun suggest(c: Catalog, profile: EquipmentProfile, p: ProgressSnapshot, dismissed: Set<String>): Suggestion? {
    // 1. D7 / R7.2-T06 (F7, O1): one "too easy" offers a move on. Level 1 -> 3, 2 -> 4, 3 -> 5; level 4 or 5 -> level 1 of the next exercise.
    //    No next exercise: 4 -> 5 and nothing at 5. Weighted exercises: 4 -> 5, and at 5 the heavier weight. Under a Rep range rule (no levels): the next exercise.
    for (vp in p.variations.values.filter { it.raiseSuggested }.sortedBy { it.variationId }) {
        val v = c.variation(vp.variationId) ?: continue
        if (p.activeInFamily[v.familyId]?.let { it != v.id } == true) continue         // already moved on to the successor
        moveOn(c, profile, p, v, vp)?.takeIf { it.id !in dismissed }?.let { return it }
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
