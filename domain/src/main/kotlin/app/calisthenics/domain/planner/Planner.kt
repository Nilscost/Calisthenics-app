// T10 + T11 — deterministic duration-budget planner with stretch allocation.
// Spec: docs/02-coding-specification.md §4 (algorithm order 1–10), ADR 0002 §B.
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

private data class Chosen(val slot: RoutineSlot, val variation: ExerciseVariation, val tier: Tier, val loadGrams: Int? = null)

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
        // 2. focus -> slots (order preserved)
        val slots = slotsForFocus(input.routine, draft.focus)
        if (slots.isEmpty()) return fail("no-slots", "The routine has no exercises for the chosen focus.",
            listOf(PlanOption("change-focus", "Choose Full body or another strength focus.")))

        // 3–5. choose variations
        val chosen = mutableListOf<Chosen>()
        for (slot in slots) chooseFor(slot)?.let { chosen += it }
        if (chosen.isEmpty()) return fail("nothing-available",
            "No exercise in this routine can be done with the current equipment, exclusions and holds.",
            listOf(PlanOption("change-profile", "Switch equipment profile."), PlanOption("clear-exclusions", "Review excluded exercises.")))
        if (draft.goalId != null) warn += "A skill goal is selected, but goal-slot planning is not implemented yet — the plan ignores it."

        // 6–7. templates
        val warmIds = input.routine.warmup.ifEmpty { cat.warmupTemplate }
        val warm = if (draft.warmupOn) template(warmIds, BlockType.WARMUP, "warmup") else emptyList()
        val cool = if (draft.cooldownOn) template(cat.cooldownTemplate, BlockType.COOLDOWN, "cooldown") else emptyList()
        if (draft.warmupOn && warmIds.isEmpty()) return fail("no-warmup-template", "Warm-up is on but no reviewed warm-up template exists.",
            listOf(PlanOption("warmup-off", "Turn warm-up off.")))
        if (draft.cooldownOn && cat.cooldownTemplate.isEmpty()) return fail("no-cooldown-template", "Cooldown is on but no reviewed cooldown template exists.",
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
        if (draft.stretchOn) check(blocks.none { it.type == BlockType.PASSIVE_RECOVERY }) { "passive recovery with stretch on" }
        check(blocks.all { it.type != BlockType.TRANSITION || it.durationSeconds <= TRANSITION_SECONDS })

        val usesDraft = blocks.mapNotNull { it.variationId }.distinct().any { cat.variation(it)?.reviewState == ReviewState.DRAFT }
        return PlanResult.Ready(WorkoutPlan(
            id = input.planId, routineId = input.routine.id, routineRevision = input.routine.revision,
            catalogVersion = cat.catalogVersion, createdAtEpochMs = input.createdAtEpochMs, profileId = input.profile.id,
            requestedDurationSeconds = budget, plannedDurationSeconds = pick.total, focus = draft.focus,
            stretchOn = draft.stretchOn, goalId = draft.goalId, rounds = pick.rounds, timed = draft.timed, format = draft.format, rule = draft.rule,
            changesExplained = explain.distinct(), warnings = warn.distinct(), needsAcceptance = needsAcceptance,
            usesDraftContent = usesDraft, blocks = blocks,
        ))
    }

    // ---------- variation choice ----------

    private fun usable(v: ExerciseVariation) =
        v.id !in input.excludedVariationIds && isAvailable(v, input.profile) &&
            (v.kind == Kind.REPS || v.kind == Kind.HOLD)

    private fun chooseFor(slot: RoutineSlot): Chosen? {
        val prefId = draft.swaps[slot.id] ?: slot.preferredVariationId
        val prefOrNull = cat.variation(prefId)
        if (prefOrNull == null) { warn += "${slot.id}: exercise '$prefId' is not in the catalog; slot skipped."; needsAcceptance = true; return null }
        var pref: ExerciseVariation = prefOrNull
        // progression successor in the same family
        val active = input.progress.activeInFamily[pref.familyId]
        if (active != null && active != pref.id && draft.swaps[slot.id] == null) {
            cat.variation(active)?.takeIf { usable(it) }?.let {
                explain += "${it.name} replaces ${pref.name} because you completed all tiers of ${pref.name}."
                pref = it
            }
        }
        val family = pref.familyId
        val hold = input.progress.holdFor(family)
        if (hold != null) {
            val easier = hold.easierVariationId?.let { cat.variation(it) }
            if (easier == null || !usable(easier)) {
                warn += "${pref.name} is paused because of reported discomfort. Choose an easier option, swap, skip it or finish — it will not be prescribed at a harder level."
                needsAcceptance = true
                return null
            }
            pref = easier
        }
        var v: ExerciseVariation = pref
        if (!usable(v)) {
            val sub = substitute(slot, v)
            if (sub == null) {
                warn += "${v.name} was left out: it needs ${missingFor(v, input.profile)} (or is excluded) and no suitable replacement exists. No other exercise was labelled as ${slot.intent.name.lowercase().replace('_', ' ')}."
                needsAcceptance = true
                return null
            }
            val (sv, equivalent) = sub
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
        if (tierIdx == null) { tierIdx = 1; explain += "${v.name}: no assessed level yet — starting at the easiest target (tier 1)." }
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
        return Chosen(slot, v, pol.tier(tierIdx!!), load)
    }

    private fun substitute(slot: RoutineSlot, original: ExerciseVariation): Pair<ExerciseVariation, Boolean>? {
        val pool = cat.variations.filter { usable(it) && it.id != original.id && cat.policyFor(it) != null }
        fun rank(list: List<ExerciseVariation>) = list.sortedWith(compareBy({ kotlin.math.abs(it.difficultyRank - original.difficultyRank) }, { it.id }))
        rank(pool.filter { slot.intent in it.patterns && slot.area in it.areas }).firstOrNull()?.let { return it to true }
        rank(pool.filter { slot.area in it.areas }).firstOrNull()?.let { return it to false }
        return null
    }

    // ---------- blocks ----------

    private fun stretchesFor(v: ExerciseVariation): List<ExerciseVariation> {
        val areas = normalizeStretchAreas(draft.stretchAreas)
        return v.compatibleStretchIds.mapNotNull { cat.variation(it) }
            .filter { it.kind == Kind.STRETCH && it.id !in input.excludedVariationIds &&
                (StretchArea.FULL_BODY in areas || it.stretchAreas.any { a -> a in areas || a == StretchArea.FULL_BODY }) }
            .sortedBy { it.id }
    }

    /** One entry per (set, exercise, group) in the order of the chosen format (V19). */
    private fun sequenceOf(n: Int, sets: Int, groups: List<Int?> = emptyList()): List<Triple<Int, Int, Int?>> = when (draft.format) {
        WorkoutFormat.CIRCUIT -> (1..sets).flatMap { r -> (0 until n).map { Triple(r, it, null) } }
        WorkoutFormat.STRAIGHT -> (0 until n).flatMap { i -> (1..sets).map { r -> Triple(r, i, i + 1) } }
        // V22: slots with an explicit group (RR: three pairs and a core triplet) alternate inside their group; a slot without one stands alone
        WorkoutFormat.PAIRS -> if (groups.any { it != null }) {
            val keys = (0 until n).map { groups.getOrNull(it) ?: (1000 + it) }.distinct()
            keys.flatMapIndexed { gi, key ->
                val members = (0 until n).filter { (groups.getOrNull(it) ?: (1000 + it)) == key }
                (1..sets).flatMap { r -> members.map { Triple(r, it, gi + 1) } }
            }
        } else
        // two exercises alternate for all their sets, then the next pair; an odd last exercise is a straight exercise on its own
        (0 until n step 2).flatMap { a ->
            val pair = if (a + 1 < n) listOf(a, a + 1) else listOf(a)
            (1..sets).flatMap { r -> pair.map { Triple(r, it, a / 2 + 1) } }
        }
    }

    private fun rounds(sl: List<Chosen>, rounds: Int): List<TimelineBlock> {
        val out = mutableListOf<TimelineBlock>()
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
                val target = if (free != null) Target(t.target.type, free.coerceIn(1, 999)) else ruleTarget ?: t.target
                val dur = if ((free != null || ruleTarget != null) && isHold) maxOf(secs, (target.max ?: target.value) + 3) else secs
                out += TimelineBlock(id, BlockType.WORK, dur, r, c.slot.id, v.id, side, target, t.index,
                    mediaId = v.mediaId, earlyCompletionStretchId = t.earlyCompletionStretchId, loadGrams = c.loadGrams, freeTarget = free != null, groupIndex = group)
            }
            val isLast = pos == order.lastIndex
            fun extras() {
                draft.extraStretches[c.slot.id].orEmpty().forEachIndexed { i, id ->
                    val x = cat.variation(id)?.takeIf { it.kind == Kind.STRETCH } ?: return@forEachIndexed
                    val secs = x.defaultSeconds ?: 30
                    if (x.unilateral) for (side in listOf(Side.LEFT, Side.RIGHT)) out += TimelineBlock("r$r-${c.slot.id}-x$i-${side.name.first()}", BlockType.STRETCH, secs, r, c.slot.id, x.id, side, recoveryForBlockIds = workIds, mediaId = x.mediaId, groupIndex = group)
                    else out += TimelineBlock("r$r-${c.slot.id}-x$i", BlockType.STRETCH, secs, r, c.slot.id, x.id, Side.BOTH, recoveryForBlockIds = workIds, mediaId = x.mediaId, groupIndex = group)
                }
            }
            val restSecs = c.slot.restSeconds ?: t.minRecoverySeconds   // V22: a routine can set its own rest (0 = none)
            if (isLast || restSecs <= 0) { extras(); continue }
            // Timed mode keeps one 60 s cycle per exercise: the time a short hold does not use goes to the recovery (the stretch when stretch is on).
            val window = if (draft.timed) TIMED_REST_SECONDS + (TIMED_WORK_SECONDS - workTotal).coerceAtLeast(0) else restSecs
            if (!draft.stretchOn) {
                out += TimelineBlock("r$r-${c.slot.id}-rec", BlockType.PASSIVE_RECOVERY, window, r, c.slot.id, recoveryForBlockIds = workIds, groupIndex = group)
            } else {
                val cands = stretchesFor(v)
                val s = (draft.roundStretchPicks["$r:${c.slot.id}"] ?: draft.stretchPicks[c.slot.id])?.let { cat.variation(it) }?.takeIf { it.kind == Kind.STRETCH } ?: cands[(r - 1 + si) % cands.size]
                if (s.unilateral) {
                    val seg = (window + 1) / 2 // never below the reviewed minimum recovery
                    for (side in listOf(Side.LEFT, Side.RIGHT)) out += TimelineBlock("r$r-${c.slot.id}-rec-${side.name.first()}", BlockType.STRETCH, seg, r, c.slot.id, s.id, side,
                        recoveryForBlockIds = workIds, mediaId = s.mediaId, groupIndex = group)
                } else out += TimelineBlock("r$r-${c.slot.id}-rec", BlockType.STRETCH, window, r, c.slot.id, s.id, Side.BOTH, recoveryForBlockIds = workIds, mediaId = s.mediaId, groupIndex = group)
            }
            extras()
        }
        return out
    }

    private fun template(ids: List<String>, type: BlockType, prefix: String): List<TimelineBlock> = ids.flatMapIndexed { i, id ->
        val v = cat.variation(id) ?: return@flatMapIndexed emptyList()
        val secs = v.defaultSeconds ?: 30
        if (v.unilateral) listOf(Side.LEFT, Side.RIGHT).map { TimelineBlock("$prefix-$i-${it.name.first()}", type, secs, variationId = id, side = it, mediaId = v.mediaId) }
        else listOf(TimelineBlock("$prefix-$i", type, secs, variationId = id, side = Side.BOTH, mediaId = v.mediaId))
    }

    /** 5 s setup only when the body position / equipment actually changes; 0 s otherwise (B3). */
    private fun withTransitions(blocks: List<TimelineBlock>): List<TimelineBlock> {
        val out = mutableListOf<TimelineBlock>(); var n = 0
        var prevPos: String? = null
        for (b in blocks) {
            val pos = b.variationId?.let { cat.variation(it)?.position }
            if (prevPos != null && pos != null && pos != prevPos) {
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
