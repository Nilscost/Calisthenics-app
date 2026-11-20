// 9.4 L4 (F7): the "Mix it up" button in the Preview. Pure: which 2 exercises of today's plan are swapped for a different type, and what is stored.
package app.calisthenics.domain.planner

import app.calisthenics.domain.equipment.isAvailable
import app.calisthenics.domain.model.Catalog
import app.calisthenics.domain.model.EquipmentProfile
import app.calisthenics.domain.model.ExerciseVariation
import app.calisthenics.domain.model.Kind
import app.calisthenics.domain.model.ProgressionRoute
import app.calisthenics.domain.progression.ProgressSnapshot
import app.calisthenics.domain.progression.ProgressionEngine
import app.calisthenics.domain.session.levelProgress

/** A movement type Mix it up can bring in. Each is defined once, as route ids, family ids and single exercise ids of the catalog. */
data class MixType(
    val id: String,
    /** Progression routes: the exercise brought in is the person's active step (else the route's entry). */
    val routes: List<String> = emptyList(),
    /** Families without a route of their own: the exercise the person trains, else the easiest usable one. */
    val families: List<String> = emptyList(),
    /** Single exercises that are not the main path of a route. */
    val exercises: List<String> = emptyList(),
)

object MixTypes {
    /** In this order; it also breaks ties between types trained equally long ago. */
    val all: List<MixType> = listOf(
        MixType("back-extension", routes = listOf("extension"), families = listOf("reverse-hyper")),               // superman hold, reverse hyper
        MixType("anti-rotation", routes = listOf("pallof-hold", "pallof-press")),                                  // pallof hold / press
        MixType("calves", routes = listOf("calf")),
        MixType("hip-hinge", routes = listOf("rdl-hinge")),                                                        // bodyweight RDL, single-leg RDL
        MixType("lateral", exercises = listOf("lunge-lateral", "copenhagen-side-plank-short", "copenhagen-side-plank")),   // lateral lunge, Copenhagen side plank
    )
}

/** One exercise of today's (unmixed) plan, in plan order. */
data class MixSlot(val slotId: String, val variationId: String)

/** [slotId]'s exercise [fromVariationId] is replaced by [toVariationId], an exercise of [typeId]. */
data class MixPick(val slotId: String, val fromVariationId: String, val toVariationId: String, val typeId: String)

private fun routeIds(r: ProgressionRoute) = r.mainPath + r.members.map { it.variationId }

/** Every exercise that counts as [type] (for "is the type in today's plan" and "when was it trained last"). */
private fun typeMembers(catalog: Catalog, type: MixType): Set<String> = buildSet {
    type.routes.mapNotNull(catalog::routeById).forEach { addAll(routeIds(it)) }
    type.families.forEach { f -> catalog.variations.filter { it.familyId == f }.forEach { add(it.id) } }
    addAll(type.exercises)
}

private fun usable(catalog: Catalog, v: ExerciseVariation, profile: EquipmentProfile, excluded: Set<String>) =
    !v.routineOnly && !v.demonstrationHold && (v.kind == Kind.REPS || v.kind == Kind.HOLD) && v.id !in excluded &&
        catalog.policyFor(v) != null && isAvailable(v, profile)

/** The step of [route] the person trains now, else its entry (the first step). Null when that cannot be done (equipment, excluded list). */
private fun routeExercise(catalog: Catalog, route: ProgressionRoute, progress: ProgressSnapshot, ok: (ExerciseVariation) -> Boolean): ExerciseVariation? {
    val path = route.mainPath.mapNotNull(catalog::variation)
    val active = path.firstNotNullOfOrNull { progress.activeInFamily[it.familyId]?.takeIf { id -> id in route.mainPath } }
        ?: route.mainPath.lastOrNull { it in progress.variations }?.let { latest ->
            if (5 in progress.variations.getValue(latest).achievedTiers) route.mainPath.getOrNull(route.mainPath.indexOf(latest) + 1) ?: latest else latest
        }
    return active?.let(catalog::variation)?.takeIf(ok) ?: path.firstOrNull()?.takeIf(ok)   // never skips a rung: an entry that cannot be done leaves the route out
}

private fun lastTrained(progress: ProgressSnapshot, id: String): Int? = progress.variations[id]?.lastTrainedDay

/** The exercise [type] brings in: within the type the one the person already trains (most recently), else the first one in the type's own order. Null when nothing fits. */
private fun typeExercise(catalog: Catalog, type: MixType, progress: ProgressSnapshot, profile: EquipmentProfile, excluded: Set<String>): ExerciseVariation? {
    val ok = { v: ExerciseVariation -> usable(catalog, v, profile, excluded) }
    val candidates = buildList {
        type.routes.mapNotNull(catalog::routeById).forEach { routeExercise(catalog, it, progress, ok)?.let(::add) }
        type.families.forEach { f ->
            val inFamily = catalog.variations.filter { it.familyId == f && ok(it) }
            (inFamily.filter { it.id in progress.variations }.maxByOrNull { lastTrained(progress, it.id) ?: Int.MIN_VALUE }
                ?: inFamily.minWithOrNull(compareBy({ it.difficultyRank }, { it.id })))?.let(::add)
        }
        val own = type.exercises.mapNotNull(catalog::variation).filter(ok)
        (own.filter { it.id in progress.variations }.maxByOrNull { lastTrained(progress, it.id) ?: Int.MIN_VALUE } ?: own.firstOrNull())?.let(::add)
    }
    val trained = candidates.filter { lastTrained(progress, it.id) != null }
    return trained.maxByOrNull { lastTrained(progress, it.id)!! } ?: candidates.firstOrNull()
}

/**
 * The swaps of tap number [tap] (0 = the first tap, 1 = the next pair, ...). [plan] is today's plan as it is WITHOUT any Mix it up swap, in plan order;
 * [handSlotIds] are the slots the person swapped or added by hand (never picked). Each tap proposes a pair afresh from [plan]; the pair after the last one wraps round.
 *
 * Which exercises: nothing left to earn (mastered) first, then the fewest good sessions towards the next level (the end screen's dots), then plan order, last first.
 * What replaces them: a type missing from [plan], the one trained longest ago first (never trained is the oldest); the pair takes the next types, wrapping round.
 */
fun mixItUp(
    catalog: Catalog, progress: ProgressSnapshot, profile: EquipmentProfile,
    plan: List<MixSlot>, handSlotIds: Set<String>, tap: Int, excluded: Set<String> = emptySet(),
): List<MixPick> {
    val engine = ProgressionEngine(catalog)
    val ranked = plan.withIndex().filter { (_, s) -> s.slotId !in handSlotIds && !s.slotId.startsWith("add-") }.sortedWith(
        compareByDescending<IndexedValue<MixSlot>> { (_, s) -> progress.variations[s.variationId]?.let { 5 in it.achievedTiers } == true }
            .thenBy { (_, s) -> progress.variations[s.variationId]?.let { levelProgress(engine.qualifyingDays(progress, s.variationId), it.tier).have } ?: 0 }
            .thenByDescending { it.index }
    ).map { it.value }
    val inPlan = plan.map { it.variationId }.toSet()
    val types = MixTypes.all.withIndex().mapNotNull { (order, t) ->
        if (typeMembers(catalog, t).any { it in inPlan }) return@mapNotNull null
        val ex = typeExercise(catalog, t, progress, profile, excluded) ?: return@mapNotNull null
        Triple(t, ex, order)
    }.sortedWith(compareBy({ (t, _, _) -> typeMembers(catalog, t).maxOfOrNull { lastTrained(progress, it) ?: Int.MIN_VALUE } ?: Int.MIN_VALUE }, { it.third }))
    val count = minOf(2, ranked.size, types.size)
    return (0 until count).map { i ->
        val from = ranked[(2 * tap + i) % ranked.size]
        val (type, ex, _) = types[(2 * tap + i) % types.size]
        MixPick(from.slotId, from.variationId, ex.id, type.id)
    }
}

// ---- what is stored in PlanEdits ----

/** The swaps still in place that came from Mix it up (a hand swap made on the same slot afterwards takes it over and is no longer listed). */
fun PlanEdits.mixOwned(): Map<String, String> = mixSwaps.filter { (slot, id) -> swaps[slot] == id }

/** The slots the person changed by hand: their own swaps (including the range-rule stepper) and everything added with the +. Mix it up never picks these. */
fun PlanEdits.handSlotIds(): Set<String> = (swaps.keys - mixOwned().keys) + added.map { it.slot.id }

/** Undo: the plan as it was before any Mix it up swap. Hand edits stay. */
fun PlanEdits.withoutMix(): PlanEdits =
    if (mixSwaps.isEmpty() && mixTaps == 0 && mixEpochDay == 0L) this
    else copy(swaps = swaps - mixOwned().keys, mixSwaps = emptyMap(), mixTaps = 0, mixEpochDay = 0L)

/** A tap: [picks] replace whatever the last tap swapped, and the next tap takes the next pair. Stamped with [todayEpochDay]. */
fun PlanEdits.withMix(picks: List<MixPick>, todayEpochDay: Long): PlanEdits {
    val base = withoutMix()
    val swapsNow = picks.associate { it.slotId to it.toVariationId }
    return base.copy(swaps = base.swaps + swapsNow, mixSwaps = swapsNow, mixTaps = mixTaps + 1, mixEpochDay = todayEpochDay)
}

/** A mix is for one workout on one day: from the next day on it is dropped and the usual plan is back. */
fun PlanEdits.expireMix(todayEpochDay: Long): PlanEdits =
    if ((mixSwaps.isEmpty() && mixTaps == 0) || mixEpochDay == todayEpochDay) this else withoutMix()
