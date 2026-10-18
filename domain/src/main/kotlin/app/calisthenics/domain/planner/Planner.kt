// T10 + T11 — deterministic duration-budget planner with stretch allocation.
// Spec: docs/archive/02-coding-specification.md §4 (algorithm order 1–10), ADR 0002 §B.
// Pure and deterministic: same input -> same plan and explanations. No clock, no randomness.
package app.calisthenics.domain.planner

import app.calisthenics.domain.equipment.isAvailable
import app.calisthenics.domain.equipment.missingFor
import app.calisthenics.domain.load.formatKg
import app.calisthenics.domain.load.isLoaded
import app.calisthenics.domain.load.loadEquipmentId
import app.calisthenics.domain.load.loadGramsFor
import app.calisthenics.domain.model.*
import app.calisthenics.domain.progression.ProgressSnapshot
import app.calisthenics.domain.routine.SessionDraft
import app.calisthenics.domain.routine.normalizeStretchAreas
import app.calisthenics.domain.routine.slotsForFocus

// ---- G0 defaults (ADR 0002 §B). Tunables are marked; only B1–B4 are owner-approved. ----
const val MIN_DURATION_SECONDS = 10 * 60
const val MAX_DURATION_SECONDS = 90 * 60
const val UNDERFILL_TOLERANCE_SECONDS = 60
const val TRANSITION_SECONDS = 5
/** NOT in the ADR: cap so a 90-minute request does not produce a 12-round circuit. Needs owner review at G2. */
const val MAX_ROUNDS = 6
/** Owner decision 2026-10-05: no minimum rounds; an explicit rounds setting may go up to this. */
const val MAX_EXPLICIT_ROUNDS = 10
const val TIMED_WORK_SECONDS = 60
/** Rest block length; the 5 s transition that follows is inside the 60 s a user experiences. */
const val TIMED_REST_SECONDS = 55
/** NOT in the ADR: shortest stretch segment worth announcing. Needs owner review at G2. */
const val MIN_STRETCH_SEGMENT_SECONDS = 10
/** R8.1-T07 (F8): one stretch between sets may appear this many times in one workout. */
const val MAX_STRETCH_USES = 2
/** R7-T13 (D10): the stretching cool-down aims at 5 minutes and stays within 8. */
const val COOLDOWN_TARGET_SECONDS = 5 * 60
const val COOLDOWN_MAX_SECONDS = 8 * 60

data class PlanInput(
    val planId: String,
    val createdAtEpochMs: Long,
    val nowDay: Int,
    val catalog: Catalog,
    val routine: Routine,
    val draft: SessionDraft,
    /** Profile AFTER any session-only availability overlay. */
    val profile: EquipmentProfile,
    val excludedVariationIds: Set<String> = emptySet(),
    val progress: ProgressSnapshot = ProgressSnapshot(emptyMap(), emptyMap(), emptyMap(), emptyList()),
)

data class ConstraintFailure(val code: String, val message: String)
data class PlanOption(val id: String, val description: String)

sealed interface PlanResult {
    data class Ready(val plan: WorkoutPlan) : PlanResult
    data class Infeasible(val reasons: List<ConstraintFailure>, val alternatives: List<PlanOption>) : PlanResult
}

private data class Chosen(val slot: RoutineSlot, val variation: ExerciseVariation, val tier: Tier, val loadGrams: Int? = null, val inferredStartTier: Int? = null)

fun generate(input: PlanInput): PlanResult = Planner(input).run()

private class Planner(val input: PlanInput) {
    val cat = input.catalog
    val draft = input.draft
    val explain = mutableListOf<String>()
    val warn = mutableListOf<String>()
    var needsAcceptance = false

    private fun fail(code: String, msg: String, alts: List<PlanOption> = emptyList()) =
        PlanResult.Infeasible(listOf(ConstraintFailure(code, msg)), alts)

    fun run(): PlanResult {
        // 1. validate
        if (draft.durationSeconds !in MIN_DURATION_SECONDS..MAX_DURATION_SECONDS || draft.durationSeconds % 60 != 0) {
            return fail("duration-range", "Duration must be 10–90 minutes in whole minutes.",
                listOf(PlanOption("set-duration", "Choose a duration between 10 and 90 minutes.")))
        }
        if (input.routine.stretchSession.isNotEmpty()) return stretchSession()
        // 2. focus -> slots (order preserved)
        val slots = slotsForFocus(input.routine, draft.focus)
        if (slots.isEmpty()) return fail("no-slots", "The routine has no exercises for the chosen focus.",
            listOf(PlanOption("change-focus", "Choose Full body or another strength focus.")))

        // 3–5. choose variations
        val chosen = mutableListOf<Chosen>()
        for (slot in slots) chooseFor(slot)?.let { candidate ->
            val automaticSlot = slot.id.startsWith("ext-") || app.calisthenics.domain.goals.Goals.isSkillSlot(slot.id)
            val explicitPick = draft.swaps.containsKey(slot.id) || slot.id.startsWith("add-")
            if (automaticSlot && !explicitPick && !routeEligibleForAutomatic(candidate.variation, slot.id, draft)) return@let
            // R8.1-T02 (F2): one exercise per logical route per session (main step, alternate and support alike), whatever kind of slot it is.
            // Only an exercise the person picked or added by hand in the Preview may repeat a progression.
            val repeats = chosen.firstOrNull { (automaticSlot && sameProgressionForSlots(it, candidate)) ||
                cat.sharesLogicalRoute(it.variation.id, candidate.variation.id) }
            if (!explicitPick && repeats != null) {
                explain += "${candidate.variation.name} was left out because this workout already trains its progression (${repeats.variation.name})."
            } else chosen += candidate
        }
        if (chosen.isEmpty()) return fail("nothing-available",
            "No exercise in this routine can be done with the current equipment, exclusions and holds.",
            listOf(PlanOption("change-profile", "Switch equipment profile."), PlanOption("clear-exclusions", "Review excluded exercises.")))

        // 6–7. templates
        val warmIds = Ramp.order(cat, input.profile, input.progress, input.routine.warmup.ifEmpty { cat.warmupTemplate }, chosen.flatMap { it.variation.patterns }).filter { it !in draft.warmupRemoved }   // L12: RAMP order, gated items
        // R8.2-T02 (G2): matched prehab items join the template's warm-up; the whole warm-up stays within the template's own time (an authored routine warm-up is left as written)
        val matchedIds = if (draft.warmupOn && input.routine.warmup.isEmpty())
            Ramp.matched(cat, input.profile, input.progress, chosen.map { it.variation }, warmIds).filter { it !in draft.warmupRemoved } else emptyList()
        val baseWarm = if (draft.warmupOn) template(warmIds, BlockType.WARMUP, "warmup") else emptyList()
        val warm = if (matchedIds.isEmpty()) baseWarm else run {
            val budget = baseWarm.sumOf { it.durationSeconds }
            val matchedBlocks = template(matchedIds, BlockType.WARMUP, "warmup-m").map { it.copy(note = "Warm-up · matched to today's exercises: slow, controlled reps") }
            val keep = mutableListOf<TimelineBlock>(); var used = 0
            for (b in matchedBlocks) { if (used + b.durationSeconds > budget) break; keep += b; used += b.durationSeconds }
            val ids = keep.mapNotNull { it.variationId }.toSet()
            if (ids.isNotEmpty()) explain += "Warm-up matched to today's exercises: ${ids.joinToString { cat.variation(it)?.name ?: it }}."
            // the template items follow in their own RAMP order while the time allows (a unilateral item has both sides in its blocks)
            val rest = mutableListOf<TimelineBlock>()
            for (group in baseWarm.groupBy { it.variationId }.values) { val sec = group.sumOf { it.durationSeconds }; if (used + sec > budget) continue; rest += group; used += sec }
            val orderedRest = baseWarm.filter { it in rest }
            keep + orderedRest
        }
        val cool = if (!draft.cooldownOn) emptyList() else when (draft.cooldownMode) {
            null -> template(cat.cooldownTemplate, BlockType.COOLDOWN, "cooldown")
            CooldownMode.TIMED -> listOf(TimelineBlock("cooldown-relax", BlockType.COOLDOWN, draft.cooldownMinutes.coerceIn(1, 30) * 60, note = "Relax and breathe: in for 4, out for 6. Let the body settle."))
            CooldownMode.STRETCHING -> cooldownStretches(chosen)
        }
        if (draft.warmupOn && warmIds.isEmpty() && draft.warmupRemoved.isEmpty()) return fail("no-warmup-template", "Warm-up is on but no reviewed warm-up template exists.",
            listOf(PlanOption("warmup-off", "Turn warm-up off.")))
        if (draft.cooldownOn && draft.cooldownMode == null && cat.cooldownTemplate.isEmpty()) return fail("no-cooldown-template", "Cooldown is on but no reviewed cooldown template exists.",
            listOf(PlanOption("cooldown-off", "Turn cooldown off.")))
        val fixed = warm + cool
        val fixedSeconds = fixed.sumOf { it.durationSeconds }

        // stretch feasibility (never silent passive rest)
        if (draft.stretchOn) {
            for (c in chosen) {
                if (stretchesFor(c.variation).isEmpty()) {
                    return fail("no-stretch-pairing",
                        "No allowed stretch pairs with ${c.variation.name} for the chosen stretch areas, so its recovery would be passive rest.",
                        listOf(PlanOption("change-stretch-focus", "Choose different stretch areas (or Full body)."),
                            PlanOption("swap-exercise", "Swap ${c.variation.name} for another exercise."),
                            PlanOption("stretch-off", "Turn stretching off (ordinary recovery stays).")))
                }
            }
        }

        // 8–9. fit: keep all slots if a round count lands within tolerance; else drop optional slots from the end.
        val fixedRounds = draft.rounds?.coerceIn(1, MAX_EXPLICIT_ROUNDS)
        val budget = if (fixedRounds != null)
            withTransitions(warm + rounds(chosen, fixedRounds) + cool).sumOf { it.durationSeconds }
        else draft.durationSeconds
        val optionalIdx = chosen.indices.filter { chosen[it].slot.optional }
        data class Cand(val slots: List<Chosen>, val rounds: Int, val blocks: List<TimelineBlock>, val total: Int)
        val cands = mutableListOf<Cand>()
        for (k in 0..(if (fixedRounds != null) 0 else optionalIdx.size)) {
            val dropped = optionalIdx.takeLast(k).toSet()
            val sl = chosen.filterIndexed { i, _ -> i !in dropped }
            var best: Cand? = null
            for (r in (if (fixedRounds != null) fixedRounds..fixedRounds else 1..MAX_ROUNDS)) {
                val b = withTransitions(warm + rounds(sl, r) + cool)
                val t = b.sumOf { it.durationSeconds }
                if (t <= budget) best = Cand(sl, r, b, t) else break
            }
            best?.let { cands += it }
        }
        if (cands.isEmpty()) {
            val mandatory = chosen.filter { !it.slot.optional }.ifEmpty { chosen }
            val minTotal = withTransitions(warm + rounds(mandatory, 1) + cool).sumOf { it.durationSeconds }
            return fail("budget-too-small",
                "Even one round of the required exercises takes ${fmt(minTotal)}, longer than the requested ${fmt(budget)}.",
                listOf(PlanOption("increase-duration", "Increase the duration to at least ${(minTotal + 59) / 60} minutes."),
                    PlanOption("narrow-focus", "Choose a narrower strength focus."),
                    PlanOption("warmup-cooldown-off", "Turn warm-up/cooldown off.")))
        }
        val pick = cands.firstOrNull { it.total >= budget - UNDERFILL_TOLERANCE_SECONDS } ?: cands.maxWith(compareBy({ it.total }, { it.slots.size }))
        val dropped = chosen.filter { c -> pick.slots.none { it.slot.id == c.slot.id } }
        if (dropped.isNotEmpty()) explain += "To fit ${fmt(budget)}, optional exercises were left out: ${dropped.joinToString { it.variation.name }}."
        if (pick.rounds < MAX_ROUNDS || pick.total < budget) explain += "${pick.rounds} round${if (pick.rounds == 1) "" else "s"} of the circuit fit the time; movement speed and recovery were not changed."
        val under = budget - pick.total
        if (under > UNDERFILL_TOLERANCE_SECONDS) {
            needsAcceptance = true
            warn += "This workout takes ${fmt(pick.total)}, ${fmt(under)} less than the ${fmt(budget)} you asked for. No padding was added — accept the real length or change duration, focus or stretching."
        }
        coverageWarning(pick.slots)
        if (!draft.stretchOn) explain += "Stretching is off: ordinary recovery intervals are kept."
        if (draft.warmupOn) explain += "Warm-up included in the time budget."
        if (draft.cooldownOn) explain += "Cooldown included in the time budget."

        // 10. invariants (defensive; failing here is a bug, not a user problem)
        val blocks = pick.blocks
        check(blocks.sumOf { it.durationSeconds } <= budget) { "plan exceeds budget" }
        // R8.1-T07 (F8): with stretching on, plain rest appears only where every compatible stretch had already been used twice
        if (blocks.any { it.type == BlockType.PASSIVE_RECOVERY } && draft.stretchOn) explain += "Some breaks are plain rest because each compatible stretch was already used twice in this workout."
        check(blocks.all { it.type != BlockType.TRANSITION || it.durationSeconds <= TRANSITION_SECONDS })

        val usesDraft = blocks.mapNotNull { it.variationId }.distinct().any { cat.variation(it)?.reviewState == ReviewState.DRAFT }
        return PlanResult.Ready(WorkoutPlan(
            id = input.planId, routineId = input.routine.id, routineRevision = input.routine.revision,
            catalogVersion = cat.catalogVersion, createdAtEpochMs = input.createdAtEpochMs, profileId = input.profile.id,
            requestedDurationSeconds = budget, plannedDurationSeconds = pick.total, focus = draft.focus,
            stretchOn = draft.stretchOn, goalId = draft.goalId, rounds = pick.rounds, timed = draft.timed, format = draft.format, rule = draft.rule, flow = draft.flow,
            changesExplained = explain.distinct(), warnings = warn.distinct(), needsAcceptance = needsAcceptance,
            usesDraftContent = usesDraft, blocks = blocks,
        ))
    }

    /**
     * L11b ("Starting To Stretch", r/flexibility): for every stretch (each side of a one-sided one) three rounds of
     * 10 gentle bumps in and out, then a hold of 10, 20 and 30 s, going a little deeper each time. Stretches only, never before strength work.
     */
    private fun stretchSession(): PlanResult {
        val out = mutableListOf<TimelineBlock>()
        for ((i, id) in input.routine.stretchSession.withIndex()) {
            val v = cat.variation(id)?.takeIf { it.kind in setOf(Kind.STRETCH, Kind.MOBILITY) && !it.demonstrationHold && isAvailable(it, input.profile) } ?: continue
            val sides = if (v.unilateral) listOf(Side.LEFT, Side.RIGHT) else listOf(Side.BOTH)
            for (side in sides) for ((k, hold) in listOf(10, 20, 30).withIndex()) {
                val tag = "ss$i-${side.name.first()}-${k + 1}"
                out += TimelineBlock("$tag-bump", BlockType.STRETCH, 10, null, "ss$i", id, side, mediaId = v.mediaId, groupIndex = i + 1,
                    note = "Gently ease in and out of the stretch 10 times: small and never forced. Skip the bumping with an acute injury.")
                out += TimelineBlock("$tag-hold", BlockType.STRETCH, hold, null, "ss$i", id, side, mediaId = v.mediaId, groupIndex = i + 1,
                    note = "Hold for $hold seconds" + if (k > 0) ", a little deeper than before." else ".")
            }
        }
        if (out.isEmpty()) return fail("nothing-available", "None of the stretches in this session can be done with the current equipment.", listOf(PlanOption("change-profile", "Switch equipment profile.")))
        val blocks = withTransitions(out)
        val total = blocks.sumOf { it.durationSeconds }
        return PlanResult.Ready(WorkoutPlan(
            id = input.planId, routineId = input.routine.id, routineRevision = input.routine.revision, catalogVersion = cat.catalogVersion,
            createdAtEpochMs = input.createdAtEpochMs, profileId = input.profile.id, requestedDurationSeconds = total, plannedDurationSeconds = total,
            focus = draft.focus, stretchOn = true, goalId = null, rounds = 1, timed = false, format = WorkoutFormat.CIRCUIT, rule = ProgressionRule(),
            changesExplained = listOf("Stretch session: 10 gentle bumps, then holds of 10, 20 and 30 s for each stretch. Not for before a workout."),
            warnings = emptyList(), needsAcceptance = false, usesDraftContent = blocks.mapNotNull { it.variationId }.distinct().any { cat.variation(it)?.reviewState == ReviewState.DRAFT }, blocks = blocks,
        ))
    }

    // ---------- variation choice ----------

    private fun usable(v: ExerciseVariation) =
        v.id !in input.excludedVariationIds && isAvailable(v, input.profile) &&
            (v.kind == Kind.REPS || v.kind == Kind.HOLD)

    private fun sameProgressionForSlots(a: Chosen, b: Chosen): Boolean {
        val ag = cat.routeById(a.slot.progressionRouteId)?.progressionGroup ?: cat.progressionGroupOf(a.variation.id)
        val bg = cat.routeById(b.slot.progressionRouteId)?.progressionGroup ?: cat.progressionGroupOf(b.variation.id)
        return ag == bg
    }

    private fun chooseFor(slot: RoutineSlot): Chosen? {
        val prefId = draft.swaps[slot.id] ?: slot.preferredVariationId
        val prefOrNull = cat.variation(prefId)
        if (prefOrNull == null) { warn += "${slot.id}: exercise '$prefId' is not in the catalog; slot skipped."; needsAcceptance = true; return null }
        var pref: ExerciseVariation = prefOrNull
        // Use persisted family activity only when it belongs to this route/progression group.
        // A legacy family may now contain several independent routes (plank, hollow and L-sit).
        var route = cat.routeById(slot.progressionRouteId)?.takeIf { selected -> pref.id in selected.mainPath || selected.members.any { it.variationId == pref.id } }
            ?: cat.mainRouteFor(pref.id, draft.goalId)
        val group = cat.progressionGroupOf(pref.id)
        val savedActive = input.progress.activeInFamily[pref.familyId]
        val active = savedActive?.takeIf { candidate ->
            (route != null && candidate in route.mainPath) || (route == null && cat.progressionGroupOf(candidate) == group)
        } ?: route?.let { selected ->
            val started = selected.mainPath.filter { it in input.progress.variations }
            val latest = started.lastOrNull()
            if (latest == null) null else if (5 in input.progress.variations.getValue(latest).achievedTiers)
                selected.mainPath.getOrNull(selected.mainPath.indexOf(latest) + 1) ?: latest else latest
        }
        if (active != null && active != pref.id && draft.swaps[slot.id] == null) {
            // R8.1-T01 (F1): the route position is kept even when its exercise needs equipment that is missing today; the substitution below then stays at or above this rung
            cat.variation(active)?.takeIf { (route?.let { r -> it.id in r.mainPath } ?: (cat.progressionGroupOf(it.id) == group)) && (usable(it) || route != null) }?.let {
                if (usable(it)) explain += "${it.name} replaces ${pref.name} because you completed all tiers of ${pref.name}."
                pref = it
            }
        }
        val family = pref.familyId
        val hold = input.progress.holdFor(family)
        if (hold != null) {
            val easier = hold.easierVariationId?.let { cat.variation(it) }
            val inRoute = easier?.let { candidate -> route?.let { r -> candidate.id in r.mainPath || r.members.any { m -> m.variationId == candidate.id && m.role == ProgressionRole.ALTERNATE } } ?: (cat.progressionGroupOf(candidate.id) == cat.progressionGroupOf(pref.id)) } == true
            if (easier == null || !usable(easier) || !inRoute) {
                warn += "${pref.name} is paused because of reported discomfort. Choose an easier option, swap, skip it or finish — it will not be prescribed at a harder level."
                needsAcceptance = true
                return null
            }
            pref = easier
        }
        var inferredTier: Int? = null
        if (hold == null && draft.swaps[slot.id] == null) {
            val rotated = rotatedExercise(cat, pref, input.progress, input.profile, input.excludedVariationIds, route?.id)
            if (rotated.id != pref.id && !draft.rule.isRange && !draft.timed && draft.freeTargets.isEmpty()) {
                val anchor = route?.members?.firstOrNull { it.variationId == rotated.id && it.role == ProgressionRole.ALTERNATE }
                    ?.atVariationId?.let(cat::variation) ?: pref
                inferredTier = inferredAlternativeTier(cat, anchor, rotated, input.progress, route?.id)
            }
            if (rotated.id != pref.id) explain += "${rotated.name} is today's alternative to ${pref.name}; each exercise keeps its own progress."
            pref = rotated
        }
        var v: ExerciseVariation = pref
        if (!usable(v)) {
            val sub = substitute(slot, v, route)
            if (sub == null) {
                warn += "${v.name} was left out: it needs ${missingFor(v, input.profile)} (or is excluded) and no suitable replacement exists. No other exercise was labelled as ${slot.intent.name.lowercase().replace('_', ' ')}."
                needsAcceptance = true
                return null
            }
            val (sv, equivalent) = sub
            if (!equivalent && cat.progressionRoutes.isNotEmpty()) {
                route = cat.mainRouteFor(sv.id, draft.goalId)
            }
            if (equivalent) explain += "${sv.name} replaces ${v.name} (same movement, ${v.equipmentAlternatives.firstOrNull()?.let { "needs ${missingFor(v, input.profile)}" } ?: "unavailable"})."
            else {
                warn += "${sv.name} replaces ${v.name}, but it is NOT the same movement (${slot.intent.name.lowercase().replace('_', ' ')} is missing from this workout). Confirm before starting."
                needsAcceptance = true
            }
            v = sv
        }
        if (draft.swaps[slot.id] != null && slot.intent !in v.patterns) {
            warn += "${v.name} is not the same movement as the slot it replaces (${slot.intent.name.lowercase().replace('_', ' ')}). Confirm before starting."
            needsAcceptance = true
        }
        val pol = cat.policyFor(v) ?: run { warn += "${v.name} has no progression policy; skipped."; return null }
        var tierIdx = input.progress.tierFor(v.id)
        if (tierIdx == null) {
            tierIdx = inferredTier ?: 1
            explain += if (inferredTier != null) "${v.name}: estimated starting target from the main exercise, reduced for the first workout. Your own results will guide this exercise from here."
                else "${v.name}: no assessed level yet — starting at the easiest target (tier 1)."
        }
        input.progress.reentryNeeded(v.id, input.nowDay)?.let { lower ->
            tierIdx = lower; needsAcceptance = true
            warn += "Welcome back — you haven't trained ${v.name} for ${app.calisthenics.domain.progression.REENTRY_DAYS}+ days. Planned at tier $lower; choose your own re-entry level before starting. Earned stars are kept."
        }
        // Kettlebell weight (owner 2026-10-08): a heavier bell than the one the level was earned with restarts at tier 1.
        val load = input.profile.loadGramsFor(v)
        val recorded = input.progress.variations[v.id]?.loadGrams
        val loadName = when (v.loadEquipmentId()) { app.calisthenics.domain.load.DUMBBELL_EQUIPMENT_ID -> "dumbbells"; app.calisthenics.domain.load.BARBELL_EQUIPMENT_ID -> "barbell"; app.calisthenics.domain.load.VEST_EQUIPMENT_ID -> "weighted vest"; else -> "kettlebell" }
        if (load != null && recorded != null && load > recorded) {
            tierIdx = 1
            explain += "${v.name}: heavier $loadName (${formatKg(load)} kg instead of ${formatKg(recorded)} kg) — starting again at tier 1. Your stars at ${formatKg(recorded)} kg are kept."
        } else if (load != null && recorded != null && load < recorded) {
            warn += "${v.name}: your $loadName here is ${formatKg(load)} kg, lighter than the ${formatKg(recorded)} kg you train with — this session is logged but does not count towards your next tier."
        }
        draft.tierOverrides[slot.id]?.let { o ->
            val t = o.coerceIn(pol.tiers.minOf { it.index }, pol.tiers.maxOf { it.index })
            if (t != tierIdx) explain += "${v.name}: level set to $t for today (it only counts towards progress at your own level)."
            tierIdx = t
        }
        return Chosen(slot.copy(progressionRouteId = route?.id), v, pol.tier(tierIdx!!), load,
            inferredTier?.takeIf { it == tierIdx && slot.id !in draft.tierOverrides })
    }

    /**
     * R8.1-T01 (F1): the highest rung of [route] the person has shown, counting the route's active main step and any main step or
     * same-rung alternate with earned stars. -1 = nothing yet. Nothing below it is ever prescribed.
     */
    private fun routeFloor(route: ProgressionRoute): Int {
        var floor = input.progress.activeInFamily.values.maxOfOrNull { route.mainPath.indexOf(it) } ?: -1
        for ((id, vp) in input.progress.variations) {
            if (vp.earnedStars() < 1) continue
            val role = route.members.firstOrNull { it.variationId == id }?.role
            if (id !in route.mainPath && role != ProgressionRole.ALTERNATE) continue
            val anchor = cat.routeAnchorFor(id, route) ?: continue
            floor = maxOf(floor, route.mainPath.indexOf(anchor))
        }
        return floor
    }

    private fun substitute(slot: RoutineSlot, original: ExerciseVariation, route: ProgressionRoute?): Pair<ExerciseVariation, Boolean>? {
        val floor = route?.let { routeFloor(it) } ?: -1
        fun belowFloor(c: ExerciseVariation): Boolean {
            if (route == null || floor < 0) return false
            val idx = cat.routeAnchorFor(c.id, route)?.let { route.mainPath.indexOf(it) } ?: return false
            return idx in 0 until floor
        }
        val pool = cat.variations.filter { !it.demonstrationHold && !it.routineOnly && usable(it) && it.id != original.id && cat.policyFor(it) != null && !belowFloor(it) && app.calisthenics.domain.goals.Goals.unmet(cat, it.id, input.progress).isEmpty() }
        fun rank(list: List<ExerciseVariation>) = list.sortedWith(compareBy({ kotlin.math.abs(it.difficultyRank - original.difficultyRank) }, { it.id }))
        val sameRungAlternatives = if (route == null || cat.progressionRoutes.isEmpty()) pool else pool.filter { candidate ->
            route.members.any { it.variationId == candidate.id && it.role == ProgressionRole.ALTERNATE && it.atVariationId == original.id } &&
                cat.exerciseGroups.any { it.mainVariationId == original.id && candidate.id in it.alternativeVariationIds }
        }
        rank(sameRungAlternatives.filter { slot.intent in it.patterns && slot.area in it.areas }).firstOrNull()?.let { return it to true }
        // still the same route: a step or alternate of this progression at or above the rung already shown, never below it
        if (route != null && floor >= 0) {
            rank(pool.filter { c ->
                slot.intent in c.patterns && slot.area in c.areas &&
                    (c.id in route.mainPath || route.members.any { it.variationId == c.id && it.role == ProgressionRole.ALTERNATE }) && cat.routeAnchorFor(c.id, route) != null
            }).firstOrNull()?.let { return it to true }
        }
        rank(pool.filter { slot.area in it.areas }).firstOrNull()?.let { return it to false }
        return null
    }

    private fun routeEligibleForAutomatic(v: ExerciseVariation, slotId: String, draft: SessionDraft): Boolean {
        if (v.demonstrationHold) return false
        if (cat.progressionRoutes.isEmpty()) return true
        val slot = input.routine.slots.firstOrNull { it.id == slotId }
        val selected = cat.routeById(slot?.progressionRouteId)
            ?: cat.mainRouteFor(v.id, draft.goalId)
        val routes = cat.routesFor(v.id).filter { route ->
            val role = route.members.firstOrNull { it.variationId == v.id }?.role
            val roleOkay = v.id in route.mainPath || (role == ProgressionRole.ALTERNATE && cat.exerciseGroups.any { v.id in it.alternativeVariationIds })
            roleOkay && (route.id == selected?.id || (slotId.startsWith("ext-") && route.defaultPool))
        }
        return routes.isNotEmpty()
    }

    // ---------- blocks ----------

    private fun stretchesFor(v: ExerciseVariation): List<ExerciseVariation> {
        val areas = normalizeStretchAreas(draft.stretchAreas)
        return v.compatibleStretchIds.mapNotNull { cat.variation(it) }
            .filter { it.kind == Kind.STRETCH && !it.demonstrationHold && it.id !in input.excludedVariationIds && !it.optIn &&   // R7-T12 (D8): deep (cool-down) stretches may go between sets again; only opt-in ones are picked by hand
                (StretchArea.FULL_BODY in areas || it.stretchAreas.any { a -> a in areas || a == StretchArea.FULL_BODY }) }
            .sortedBy { it.id }
    }

    /** One entry per (set, exercise, group) in the order of the chosen format (V19). */
    private fun sequenceOf(n: Int, sets: Int, groups: List<Int?> = emptyList()): List<Triple<Int, Int, Int?>> = orderedEntries(draft.format, n, sets, groups)

    private fun rounds(sl: List<Chosen>, rounds: Int): List<TimelineBlock> {
        val out = mutableListOf<TimelineBlock>()
        // R8.1-T07 (F8): a stretch between sets appears at most MAX_STRETCH_USES times in a workout; hand picks are the person's own and are only counted
        val stretchUses = mutableMapOf<String, Int>()
        val order = sequenceOf(sl.size, rounds, sl.map { it.slot.group })
        for ((pos, item) in order.withIndex()) {
            val (r, si, group) = item
            val c = sl[si]
            val v = c.variation; val t = c.tier
            val sides = if (v.unilateral) listOf(Side.LEFT, Side.RIGHT) else listOf(Side.NONE)
            val workIds = mutableListOf<String>()
            // F13/Q4: a hold lasts its target plus a short get-ready (tier.workWindowSeconds), also in timed mode. Only rep work fills the 60 s.
            val isHold = t.target.type == TargetType.HOLD_SECONDS
            var workTotal = 0
            for (side in sides) {
                val id = "r$r-${c.slot.id}-work" + if (side == Side.NONE) "" else "-${side.name.first()}"
                workIds += id
                val secs = if (draft.timed && !isHold) (if (side == Side.NONE) TIMED_WORK_SECONDS else TIMED_WORK_SECONDS / 2) else t.workWindowSeconds
                workTotal += secs
                val free = draft.freeTargets["$r:${c.slot.id}"] ?: draft.freeTargets[c.slot.id]
                val rng = draft.rule
                val ruleTarget = if (rng.isRange) (if (isHold) Target(t.target.type, rng.holdFrom, rng.holdTo) else Target(t.target.type, c.slot.repFrom ?: rng.from, maxOf(c.slot.repTo ?: rng.to, c.slot.repFrom ?: rng.from))) else null
                val requested = if (free != null) Target(t.target.type, free.coerceIn(1, 999)) else ruleTarget ?: t.target
                val target = cappedTarget(requested, cat.policyFor(v)!!)
                val dur = if ((free != null || ruleTarget != null) && isHold) maxOf(secs, (target.max ?: target.value) + 3) else secs
                out += TimelineBlock(id, BlockType.WORK, dur, r, c.slot.id, v.id, side, target, t.index,
                    mediaId = v.mediaId, earlyCompletionStretchId = t.earlyCompletionStretchId, loadGrams = c.loadGrams, freeTarget = free != null, groupIndex = group,
                    progressionRouteId = c.slot.progressionRouteId, inferredStartTier = c.inferredStartTier.takeIf { free == null && ruleTarget == null })
            }
            val isLast = pos == order.lastIndex
            fun extras() {
                draft.extraStretches[c.slot.id].orEmpty().forEachIndexed { i, id ->
                    val x = cat.variation(id)?.takeIf { it.kind in setOf(Kind.STRETCH, Kind.MOBILITY) && !it.demonstrationHold && isAvailable(it, input.profile) } ?: return@forEachIndexed
                    val secs = x.defaultSeconds ?: 30
                    if (x.unilateral) for (side in listOf(Side.LEFT, Side.RIGHT)) out += TimelineBlock("r$r-${c.slot.id}-x$i-${side.name.first()}", BlockType.STRETCH, secs, r, c.slot.id, x.id, side, recoveryForBlockIds = workIds, mediaId = x.mediaId, groupIndex = group)
                    else out += TimelineBlock("r$r-${c.slot.id}-x$i", BlockType.STRETCH, secs, r, c.slot.id, x.id, Side.BOTH, recoveryForBlockIds = workIds, mediaId = x.mediaId, groupIndex = group)
                }
            }
            val restSecs = c.slot.restSeconds ?: t.minRecoverySeconds   // V22: a routine can set its own rest (0 = none)
            // R8.1-T06 (F7): Flow drops the rest between exercises; the rest after a set (before the same exercise again, or at the end of a round) stays
            val next = order.getOrNull(pos + 1)
            val betweenExercises = next != null && isBetweenExercises(draft.format, r, si, group, next)
            if (isLast || restSecs <= 0 || (draft.flow && betweenExercises)) { extras(); continue }
            // Timed mode keeps one 60 s cycle per exercise: the time a short hold does not use goes to the recovery (the stretch when stretch is on).
            val window = if (draft.timed) TIMED_REST_SECONDS + (TIMED_WORK_SECONDS - workTotal).coerceAtLeast(0) else restSecs
            if (!draft.stretchOn) {
                out += TimelineBlock("r$r-${c.slot.id}-rec", BlockType.PASSIVE_RECOVERY, window, r, c.slot.id, recoveryForBlockIds = workIds, groupIndex = group)
            } else {
                val cands = stretchesFor(v)
                val hand = (draft.roundStretchPicks["$r:${c.slot.id}"] ?: draft.stretchPicks[c.slot.id])?.let { cat.variation(it) }?.takeIf { it.kind in setOf(Kind.STRETCH, Kind.MOBILITY) && !it.demonstrationHold && isAvailable(it, input.profile) }
                val start = (r - 1 + si) % cands.size
                val s = hand ?: cands.indices.map { cands[(start + it) % cands.size] }.firstOrNull { (stretchUses[it.id] ?: 0) < MAX_STRETCH_USES }
                if (s == null) {
                    // every compatible stretch was used twice already: plain rest instead of repeating one a third time
                    out += TimelineBlock("r$r-${c.slot.id}-rec", BlockType.PASSIVE_RECOVERY, window, r, c.slot.id, recoveryForBlockIds = workIds, groupIndex = group)
                    extras()
                    continue
                }
                stretchUses[s.id] = (stretchUses[s.id] ?: 0) + 1
                if (s.unilateral) {
                    val seg = maxOf((window + 1) / 2, s.defaultSeconds ?: 0) // never below the reviewed minimum recovery; a deep stretch keeps its own time per side (R7-T12)
                    for (side in listOf(Side.LEFT, Side.RIGHT)) out += TimelineBlock("r$r-${c.slot.id}-rec-${side.name.first()}", BlockType.STRETCH, seg, r, c.slot.id, s.id, side,
                        recoveryForBlockIds = workIds, mediaId = s.mediaId, groupIndex = group)
                } else out += TimelineBlock("r$r-${c.slot.id}-rec", BlockType.STRETCH, maxOf(window, s.defaultSeconds ?: 0), r, c.slot.id, s.id, Side.BOTH, recoveryForBlockIds = workIds, mediaId = s.mediaId, groupIndex = group)
            }
            extras()
        }
        return out
    }

    /**
     * R7-T13 (D10): the cool-down stretches matched to the session: the stretches the session's exercises list as compatible (deep ones allowed),
     * those that fit the most exercises first, until about 5 minutes are reached (never more than 8). The person's own list from the Preview wins.
     */
    private fun cooldownStretches(chosen: List<Chosen>): List<TimelineBlock> {
        fun secs(v: ExerciseVariation) = (v.defaultSeconds ?: 30) * (if (v.unilateral) 2 else 1)
        val base: List<ExerciseVariation> = draft.cooldownStretches?.mapNotNull { cat.variation(it)?.takeIf { v -> v.kind in setOf(Kind.STRETCH, Kind.MOBILITY) && !v.demonstrationHold && isAvailable(v, input.profile) } } ?: run {
            val score = chosen.flatMap { it.variation.compatibleStretchIds }.groupingBy { it }.eachCount()
            val out = mutableListOf<ExerciseVariation>(); var total = 0
            for (id in score.keys.sortedWith(compareBy({ -score.getValue(it) }, { it }))) {
                val v = cat.variation(id)?.takeIf { it.kind == Kind.STRETCH && !it.demonstrationHold && !it.optIn && it.id !in input.excludedVariationIds && isAvailable(it, input.profile) } ?: continue
                if (total + secs(v) > COOLDOWN_MAX_SECONDS) continue
                out += v; total += secs(v)
                if (total >= COOLDOWN_TARGET_SECONDS) break
            }
            out
        }
        // R7.2-T04 (F5): the hand removals and additions come last, so they also apply to a hand-picked list
        val kept = base.filter { it.id !in draft.cooldownRemoved }
        val picked = kept + draft.cooldownAdded.distinct().filter { id -> kept.none { it.id == id } }.mapNotNull { cat.variation(it)?.takeIf { v -> v.kind in setOf(Kind.STRETCH, Kind.MOBILITY) && !v.demonstrationHold && isAvailable(v, input.profile) } }
        return picked.flatMapIndexed { i, v ->
            val s = v.defaultSeconds ?: 30
            if (v.unilateral) listOf(Side.LEFT, Side.RIGHT).map { TimelineBlock("cooldown-$i-${it.name.first()}", BlockType.COOLDOWN, s, variationId = v.id, side = it, mediaId = v.mediaId, note = "Cool-down") }
            else listOf(TimelineBlock("cooldown-$i", BlockType.COOLDOWN, s, variationId = v.id, side = Side.BOTH, mediaId = v.mediaId, note = "Cool-down"))
        }
    }

    private fun template(ids: List<String>, type: BlockType, prefix: String): List<TimelineBlock> = ids.flatMapIndexed { i, id ->
        val v = cat.variation(id)?.takeUnless { it.demonstrationHold } ?: return@flatMapIndexed emptyList()
        val secs = v.defaultSeconds ?: 30
        val note = if (type == BlockType.WARMUP) "Warm-up · " + Ramp.phaseOf(id).label.lowercase() else null
        if (v.unilateral) listOf(Side.LEFT, Side.RIGHT).map { TimelineBlock("$prefix-$i-${it.name.first()}", type, secs, variationId = id, side = it, mediaId = v.mediaId, note = note) }
        else listOf(TimelineBlock("$prefix-$i", type, secs, variationId = id, side = Side.BOTH, mediaId = v.mediaId, note = note))
    }

    /** 5 s setup only when the body position / equipment actually changes; 0 s otherwise (B3). */
    private fun withTransitions(blocks: List<TimelineBlock>): List<TimelineBlock> {
        val out = mutableListOf<TimelineBlock>(); var n = 0
        var prevPos: String? = null
        for (b in blocks) {
            val pos = b.variationId?.let { cat.variation(it)?.position }
            if ((prevPos != null && pos != null && pos != prevPos) || needsSideChangePreparation(out.lastOrNull(), b)) {
                out += TimelineBlock("tr-${++n}", BlockType.TRANSITION, TRANSITION_SECONDS, b.roundIndex, b.slotId, groupIndex = b.groupIndex)
            }
            out += b
            if (pos != null) prevPos = pos
        }
        return out
    }

    private fun coverageWarning(sl: List<Chosen>) {
        if (!draft.stretchOn) return
        val used = sl.flatMap { stretchesFor(it.variation) }.flatMap { it.stretchAreas }.filter { it != StretchArea.FULL_BODY }.toSet()
        val wanted = normalizeStretchAreas(draft.stretchAreas)
        val missing = if (StretchArea.FULL_BODY in wanted) StretchArea.entries.filter { it != StretchArea.FULL_BODY && it !in used }
        else wanted.filter { it !in used }
        if (missing.isNotEmpty()) warn += "Stretch coverage is limited: nothing in this session stretches ${missing.joinToString { it.name.lowercase().replace('_', ' ') }}."
    }

    private fun fmt(s: Int) = if (s % 60 == 0) "${s / 60} min" else "${s / 60} min ${s % 60} s"
}

/**
 * One entry per (set, exercise index, group) in the order of [format]: circuit = round by round; straight sets = all sets of one exercise;
 * pairs = two exercises alternate for all their sets (a slot with an explicit group alternates inside its group; an odd last exercise stands alone). V19/V22.
 */
fun orderedEntries(format: WorkoutFormat, n: Int, sets: Int, groups: List<Int?> = emptyList()): List<Triple<Int, Int, Int?>> = when (format) {
    WorkoutFormat.CIRCUIT -> (1..sets).flatMap { r -> (0 until n).map { Triple(r, it, null) } }
    WorkoutFormat.STRAIGHT -> (0 until n).flatMap { i -> (1..sets).map { r -> Triple(r, i, i + 1) } }
    WorkoutFormat.PAIRS -> if (groups.any { it != null }) {
        val keys = (0 until n).map { groups.getOrNull(it) ?: (1000 + it) }.distinct()
        keys.flatMapIndexed { gi, key ->
            val members = (0 until n).filter { (groups.getOrNull(it) ?: (1000 + it)) == key }
            (1..sets).flatMap { r -> members.map { Triple(r, it, gi + 1) } }
        }
    } else
        (0 until n step 2).flatMap { a ->
            val pair = if (a + 1 < n) listOf(a, a + 1) else listOf(a)
            (1..sets).flatMap { r -> pair.map { Triple(r, it, a / 2 + 1) } }
        }
}

/** R8.1-T06 (F7): is the break after the entry (set [r], exercise [si], [group]) a break between exercises (dropped by Flow) rather than between sets? */
fun isBetweenExercises(format: WorkoutFormat, r: Int, si: Int, group: Int?, next: Triple<Int, Int, Int?>): Boolean = when (format) {
    WorkoutFormat.CIRCUIT -> next.first == r
    WorkoutFormat.PAIRS -> next.third != group || (next.first == r && next.second != si)
    WorkoutFormat.STRAIGHT -> next.second != si
}
