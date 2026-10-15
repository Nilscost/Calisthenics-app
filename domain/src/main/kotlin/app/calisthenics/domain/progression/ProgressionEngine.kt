// T13 — stars and bounded automatic progression (PROG-01, PROG-03, PROG-04, PROG-06; ADR 0002 §C).
//
// Design: progress is NEVER stored as mutable counters. It is recomputed by replaying the
// full evidence history + explicit user actions in day order. That makes late feedback edits
// deterministic and idempotent by construction (no tier can be awarded twice from the same
// exposures) and keeps every change explainable.
//
// Advancement is evaluated only right after a qualifying session — never from calendar
// passage alone (PROG-04). The pacing gate itself is the approved seed function
// app.calisthenics.domain.evaluateAdvancement (ADR 0002 §C3).
package app.calisthenics.domain.progression

import app.calisthenics.domain.AdvancementInput
import app.calisthenics.domain.AdvancementResult
import app.calisthenics.domain.QualifyingExposure
import app.calisthenics.domain.evaluateAdvancement
import app.calisthenics.domain.feedback.SessionEvidence
import app.calisthenics.domain.load.formatKg
import app.calisthenics.domain.load.isLoaded
import app.calisthenics.domain.model.Catalog
import app.calisthenics.domain.model.ProgressionPolicy
import app.calisthenics.domain.model.Target
import app.calisthenics.domain.Tier as SeedTier
import app.calisthenics.domain.ProgressionPolicy as SeedPolicy

const val REENTRY_DAYS = 14 // ADR 0002 C8
const val BELOW_SUGGEST_AFTER = 2 // ADR 0002 C6
/** V02 / D7, changed by R7.2-T06 (F7, owner decision O1, 2026-10-13): one session rated "too easy" already offers a move on (a card the person accepts, never applied without the user). */
const val ABOVE_SUGGEST_AFTER = 1

/** R7-T04 (B1): a self-assessed tier above the top step = the whole ladder of that exercise is achieved (five stars) and its successor starts at level 1. */
const val ALL_TIERS_DONE = 6

/** R7-T11 (D5): stored level of a hand edit "levels 1..n are done": 10 + n (n = 1..4); n = 5 is [ALL_TIERS_DONE]. Plain values are the usual self-assessed tier. */
fun storedForLevelsDone(n: Int): Int = if (n >= 5) ALL_TIERS_DONE else 10 + n.coerceAtLeast(1)

/** The self-assessment a stored level stands for. */
fun selfAssessmentFor(variationId: String, stored: Int): UserAction.SelfAssessment =
    if (stored in 11..14) UserAction.SelfAssessment(0, variationId, stored - 10 + 1, stored - 10) else UserAction.SelfAssessment(0, variationId, stored)

/** Explicit user actions that change progression (all optional; silence never clears a hold). */
sealed interface UserAction {
    val day: Int
    /** Initial self-assessment / manual pick of a familiar variation (ONB-01). */
    data class SelfAssessment(override val day: Int, val variationId: String, val tier: Int, /** R7-T11 (D5): levels 1..this are done (stars); 0 = the plain self-assessment (levels below [tier] count). */ val achievedUpTo: Int = 0) : UserAction
    /** Accept the app's lower-target suggestion or the easier adjustment after discomfort. */
    /** D7: the user confirms the "too easy" suggestion and moves up to [tier]. Never created by the app itself. */
    data class AcceptHarder(override val day: Int, val variationId: String, val tier: Int) : UserAction
    /** R7-T05 (B9): at the top level, two "too easy" in a row offered the next exercise and the person accepted: this one counts as done, [toVariationId] starts at level 1. */
    data class AcceptNextExercise(override val day: Int, val variationId: String, val toVariationId: String) : UserAction
    data class AcceptEasier(override val day: Int, val variationId: String, val tier: Int) : UserAction
    /** "I'm comfortable progressing again" for a family (C7). Does not advance immediately. */
    data class ComfortClearance(override val day: Int, val familyId: String) : UserAction
    /** Choice after a 14-day break (C8). */
    data class ReentryChoice(override val day: Int, val variationId: String, val tier: Int) : UserAction
    /** Manual easier/harder override from Today/Progress. Easier pauses auto-advancement. */
    data class ManualOverride(override val day: Int, val variationId: String, val tier: Int) : UserAction
    /** Return to the normal (non-overridden) prescription; resumes auto-advancement. */
    data class ResumeNormal(override val day: Int, val variationId: String) : UserAction
}

data class VariationProgress(
    val variationId: String,
    val familyId: String,
    val tier: Int,
    val enrolledAtDay: Int,
    val baselineTier: Int?, // self-reported, shown separately (C5)
    val achievedTiers: Set<Int>,
    val lastAutoAdvanceDay: Int?,
    val streakDays: List<Int>,
    val streakAssumed: Int,
    val consecutiveBelow: Int,
    val lowerTargetSuggested: Boolean,
    val autoPaused: Boolean,
    val lastTrainedDay: Int?,
    val reentryResolvedDay: Int?,
    /** Kettlebell weight the current tier is earned with (null = bodyweight / not recorded yet). */
    val loadGrams: Int? = null,
    /** Stars kept from lighter kettlebells: grams -> stars earned at that weight. */
    val starsByLoad: Map<Int, Int> = emptyMap(),
    /** V02: sessions in a row rated "too easy" at this tier. */
    val consecutiveAbove: Int = 0,
    /** V02 / D7: a move on is suggested (R7.2-T06: after one "too easy"); it only changes when the user accepts. */
    val raiseSuggested: Boolean = false,
    /** R7.2-T06: the "too easy" session was under a Rep range / Custom rule (no levels): the move on is the next exercise. */
    val ruleAbove: Boolean = false,
) {
    /** Highest consecutive achieved tier counted from the tier the variation was started at. */
    fun earnedStars(): Int {
        val start = baselineTier ?: 1
        var t = start
        while (t in achievedTiers) t++
        // D8 / R26: levels below the one you start at count as stars too (one kind of star, earned or assumed).
        return t - 1
    }
}

data class FamilyHold(val familyId: String, val sinceDay: Int, val easierAcceptedTier: Int?, val easierVariationId: String?)

data class ProgressEvent(val day: Int, val variationId: String, val message: String)

data class ProgressSnapshot(
    val variations: Map<String, VariationProgress>,
    /** familyId -> variation the user currently trains in that family (moves on successor transitions). */
    val activeInFamily: Map<String, String>,
    val holds: Map<String, FamilyHold>,
    val events: List<ProgressEvent>,
    /** Distinct workouts with completed work in a family, rebuilt from evidence rather than stored counters. */
    val familyWorkoutCounts: Map<String, Int> = emptyMap(),
) {
    fun tierFor(variationId: String): Int? = variations[variationId]?.tier
    fun holdFor(familyId: String): FamilyHold? = holds[familyId]

    /** Re-entry needed when the variation was trained before and not for >= 14 days (C8). */
    fun reentryNeeded(variationId: String, nowDay: Int): Int? {
        val p = variations[variationId] ?: return null
        val last = maxOf(p.lastTrainedDay ?: return null, p.reentryResolvedDay ?: Int.MIN_VALUE)
        return if (nowDay - last >= REENTRY_DAYS) maxOf(1, p.tier - 1) else null
    }
}

class ProgressionEngine(private val catalog: Catalog) {

    private fun policy(vid: String): ProgressionPolicy? = catalog.policyForVariation(vid)
    private fun family(vid: String): String = catalog.variation(vid)?.familyId ?: vid
    private fun isRotationAlternative(vid: String) = catalog.exerciseGroups.any { vid in it.alternativeVariationIds }
    private fun mainFor(vid: String) = catalog.exerciseGroups.firstOrNull { vid in it.alternativeVariationIds }?.mainVariationId ?: vid

    private fun seed(p: ProgressionPolicy) = SeedPolicy(
        variationId = p.variationId,
        version = p.version,
        tiers = p.tiers.map { SeedTier(it.index, it.target.value, it.workWindowSeconds, it.minRecoverySeconds) },
    )

    /**
     * [autoProgression] is the global D4 switch: with it off a session still counts as trained (last trained day, streak) but never
     * moves a level or switches to the successor exercise; only the owner's own actions (self-assessment, manual override, accepting a suggestion) do.
     */
    fun replay(evidence: List<SessionEvidence>, actions: List<UserAction>, autoProgression: Boolean = true): ProgressSnapshot {
        val vars = sortedMapOf<String, VariationProgress>()
        val active = sortedMapOf<String, String>()
        val holds = sortedMapOf<String, FamilyHold>()
        val events = mutableListOf<ProgressEvent>()

        fun ensure(vid: String, day: Int, tier: Int = 1, baseline: Int? = null): VariationProgress =
            vars.getOrPut(vid) {
                VariationProgress(vid, family(vid), tier, day, baseline, emptySet(), null, emptyList(), 0, 0,
                    false, false, null, null)
            }

        // Timeline items: sessions at their day; late discomfort reports at the report day;
        // actions at their day. Stable order: day, then sessions before actions, then ids.
        data class Item(val day: Int, val order: Int, val key: String, val run: () -> Unit)
        val items = mutableListOf<Item>()

        for (ev in evidence) items += Item(ev.day, 0, ev.sessionId + ev.variationId) {
            val vid = ev.variationId
            var p = ensure(vid, ev.day, tier = ev.prescribedTier ?: 1)
            p = p.copy(lastTrainedDay = maxOf(p.lastTrainedDay ?: ev.day, ev.day))
            val start = p
            active.putIfAbsent(p.familyId, mainFor(vid))
            val hold = holds[p.familyId]
            // Kettlebell weight: levels are earned per weight (owner 2026-10-08).
            val load = ev.loadGrams
            val recorded = p.loadGrams
            var lighter = false
            if (load != null) when {
                recorded == null -> p = p.copy(loadGrams = load)
                load > recorded -> {
                    val kept = p.earnedStars()
                    p = p.copy(tier = 1, baselineTier = null, achievedTiers = emptySet(), enrolledAtDay = ev.day,
                        lastAutoAdvanceDay = null, streakDays = emptyList(), streakAssumed = 0, consecutiveBelow = 0,
                        lowerTargetSuggested = false, loadGrams = load,
                        starsByLoad = p.starsByLoad + (recorded to maxOf(kept, p.starsByLoad[recorded] ?: 0)))
                    events += ProgressEvent(ev.day, vid,
                        "Heavier kettlebell (${formatKg(load)} kg): ${catalog.variation(vid)?.name ?: vid} starts again at tier 1. Your $kept stars at ${formatKg(recorded)} kg are kept.")
                }
                load < recorded -> lighter = true
            }
            if (lighter) {
                events += ProgressEvent(ev.day, vid,
                    "Done with a lighter kettlebell (${formatKg(load!!)} kg instead of ${formatKg(recorded!!)} kg) — logged, but it does not count towards your next tier.")
            } else when {
                // V21: Rep range / Custom rule: every set at the top of the range, in a row, moves you on to the next exercise (tier-free).
                ev.ruleMet != null -> {
                    if (ev.ruleMet && hold == null && !p.autoPaused) {
                        val days = (p.streakDays + ev.day).distinct().sorted()
                        p = p.copy(streakDays = days)
                        if (autoProgression && days.size >= ev.ruleSessions) p = moveOnByRule(p, ev.day, events, active, vars)
                    } else if (!ev.ruleMet) p = p.copy(streakDays = emptyList(), streakAssumed = 0)
                }
                ev.qualifying && hold == null && !p.autoPaused && ev.prescribedTier == p.tier -> {
                    val days = (p.streakDays + ev.day).distinct().sorted()
                    p = p.copy(streakDays = days, streakAssumed = p.streakAssumed + if (ev.confirmedBlocks == 0) 1 else 0,
                        consecutiveBelow = 0, lowerTargetSuggested = false)
                    if (autoProgression) p = tryAdvance(p, ev.day, events, active, vars)
                }
                ev.anyBelow && !ev.discomfort -> {
                    val below = p.consecutiveBelow + 1
                    p = p.copy(streakDays = emptyList(), streakAssumed = 0, consecutiveBelow = below)
                    if (below >= BELOW_SUGGEST_AFTER && !p.lowerTargetSuggested && p.tier > 1) {
                        p = p.copy(lowerTargetSuggested = true)
                        events += ProgressEvent(ev.day, vid, "Two sessions in a row below target — a lower target (tier ${p.tier - 1}) is suggested. Nothing changes unless you accept.")
                    } else {
                        events += ProgressEvent(ev.day, vid, "Below target — difficulty held at tier ${p.tier}; the qualifying streak restarts.")
                    }
                }
                ev.skippedOrPartial || ev.easierOverride -> {
                    p = p.copy(streakDays = emptyList(), streakAssumed = 0)
                }
                else -> Unit
            }
            // V02 / D7, R7.2-T06 (F7, O1): "too easy" (once is enough) suggests a move on; the tier itself never changes here. What the move is (two levels, or the next exercise) is decided in [suggest].
            val byRule = ev.ruleMet != null
            val tooEasy = ev.anyAbove && (byRule || ev.prescribedTier == start.tier) && !ev.anyBelow && !ev.discomfort && !ev.skippedOrPartial && !lighter
            val above = if (tooEasy && p.tier == start.tier) p.consecutiveAbove + 1 else 0
            val suggest = above >= ABOVE_SUGGEST_AFTER && !p.raiseSuggested && p.tier == start.tier
            p = p.copy(consecutiveAbove = above, raiseSuggested = if (p.tier != start.tier) false else p.raiseSuggested || suggest, ruleAbove = if (above > 0) byRule else false)
            if (suggest) events += ProgressEvent(ev.day, vid, "Marked too easy — a move on is suggested. Nothing changes unless you accept.")
            vars[vid] = p
        }
        for (ev in evidence.filter { it.discomfort }) {
            val reportDay = ev.discomfortReportedDay ?: ev.day
            items += Item(reportDay, 1, "d" + ev.sessionId + ev.variationId) {
                val fam = family(ev.variationId)
                if (holds[fam] == null) {
                    holds[fam] = FamilyHold(fam, reportDay, null, null)
                    events += ProgressEvent(reportDay, ev.variationId,
                        "Discomfort reported — automatic progression paused for the whole ${fam} family. An easier option will be offered; it stays paused until you say you're comfortable again.")
                }
                vars[ev.variationId]?.let { vars[ev.variationId] = it.copy(streakDays = emptyList(), streakAssumed = 0) }
            }
        }
        for ((i, a) in actions.withIndex()) items += Item(a.day, 2, "a%06d".format(i)) {
            when (a) {
                is UserAction.SelfAssessment -> {
                    val top = policy(a.variationId)?.tiers?.maxOfOrNull { it.index } ?: 5
                    if (a.tier > top) {
                        // R7-T04 (B1): above the top step: the top step is achieved too (five stars) and the successor is active at level 1
                        val p = ensure(a.variationId, a.day, top, top)
                        vars[a.variationId] = p.copy(tier = top, baselineTier = top, enrolledAtDay = a.day, achievedTiers = (1..top).toSet())
                        active.putIfAbsent(p.familyId, mainFor(a.variationId))
                        val next = if (isRotationAlternative(a.variationId)) null else policy(a.variationId)?.progressionSuccessors().orEmpty().firstOrNull { catalog.variation(it) != null && prerequisitesMet(it, vars) }
                        if (next != null) {
                            if (!vars.containsKey(next)) vars[next] = VariationProgress(next, family(next), 1, a.day, null, emptySet(), null, emptyList(), 0, 0, false, false, null, null)
                            active[p.familyId] = next
                        }
                    } else if (a.achievedUpTo > 0) {
                        // R7-T11 (D5): set by hand: levels 1..achievedUpTo are done, [tier] is the one trained next
                        val p = ensure(a.variationId, a.day, a.tier, 1)
                        vars[a.variationId] = p.copy(tier = a.tier, baselineTier = 1, enrolledAtDay = a.day, achievedTiers = (1..a.achievedUpTo).toSet())
                        active.putIfAbsent(p.familyId, mainFor(a.variationId))
                    } else {
                        val p = ensure(a.variationId, a.day, a.tier, a.tier)
                        vars[a.variationId] = p.copy(tier = a.tier, baselineTier = a.tier, enrolledAtDay = a.day)
                        active.putIfAbsent(p.familyId, mainFor(a.variationId))
                    }
                }
                is UserAction.AcceptHarder -> {
                    val p = ensure(a.variationId, a.day)
                    // R7.2-T06 (F7): the levels jumped over count as done (stars), the same way the hand edit (D5) does
                    val skipped = if (a.tier > p.tier) (p.tier until a.tier).toSet() else emptySet()
                    vars[a.variationId] = p.copy(tier = a.tier, enrolledAtDay = a.day, streakDays = emptyList(),
                        streakAssumed = 0, consecutiveAbove = 0, raiseSuggested = false, ruleAbove = false, achievedTiers = p.achievedTiers + skipped)
                    events += ProgressEvent(a.day, a.variationId, "You accepted a harder target (tier ${a.tier}).")
                }
                is UserAction.AcceptNextExercise -> {
                    val p = ensure(a.variationId, a.day)
                    val top = policy(a.variationId)?.tiers?.maxOfOrNull { it.index } ?: 5
                    vars[a.variationId] = p.copy(achievedTiers = (1..top).toSet(), streakDays = emptyList(), streakAssumed = 0, consecutiveAbove = 0, raiseSuggested = false, ruleAbove = false)
                    if (!vars.containsKey(a.toVariationId)) vars[a.toVariationId] = VariationProgress(a.toVariationId, family(a.toVariationId), 1, a.day, null, emptySet(), null, emptyList(), 0, 0, false, false, null, null)
                    if (!isRotationAlternative(a.variationId)) active[p.familyId] = a.toVariationId
                    events += ProgressEvent(a.day, a.variationId, "You accepted the next exercise: ${catalog.variation(a.toVariationId)?.name ?: a.toVariationId} (level 1).")
                }
                is UserAction.AcceptEasier -> {
                    val p = ensure(a.variationId, a.day)
                    vars[a.variationId] = p.copy(tier = a.tier, enrolledAtDay = a.day, streakDays = emptyList(),
                        streakAssumed = 0, consecutiveBelow = 0, lowerTargetSuggested = false)
                    holds[p.familyId]?.let { holds[p.familyId] = it.copy(easierAcceptedTier = a.tier, easierVariationId = a.variationId) }
                    events += ProgressEvent(a.day, a.variationId, "You accepted an easier target (tier ${a.tier}).")
                }
                is UserAction.ComfortClearance -> {
                    if (holds.remove(a.familyId) != null) {
                        vars.replaceAll { _, v -> if (v.familyId == a.familyId) v.copy(streakDays = emptyList(), streakAssumed = 0, enrolledAtDay = a.day) else v }
                        events += ProgressEvent(a.day, active[a.familyId] ?: a.familyId,
                            "You confirmed you're comfortable again — automatic progression resumes from new sessions (no immediate jump).")
                    }
                }
                is UserAction.ReentryChoice -> {
                    val p = ensure(a.variationId, a.day)
                    vars[a.variationId] = p.copy(tier = a.tier, enrolledAtDay = a.day, streakDays = emptyList(),
                        streakAssumed = 0, reentryResolvedDay = a.day)
                    events += ProgressEvent(a.day, a.variationId, "Welcome back — you chose tier ${a.tier} to re-enter. Earned stars are kept.")
                }
                is UserAction.ManualOverride -> {
                    val p = ensure(a.variationId, a.day)
                    val easier = a.tier < p.tier
                    vars[a.variationId] = p.copy(tier = a.tier, enrolledAtDay = a.day, streakDays = emptyList(),
                        streakAssumed = 0, autoPaused = easier)
                    events += ProgressEvent(a.day, a.variationId,
                        if (easier) "Manual easier choice (tier ${a.tier}) — automatic increases paused until you return to normal."
                        else "Manual harder choice (tier ${a.tier}).")
                }
                is UserAction.ResumeNormal -> {
                    vars[a.variationId]?.let { vars[a.variationId] = it.copy(autoPaused = false) }
                }
            }
        }

        items.sortedWith(compareBy({ it.day }, { it.order }, { it.key })).forEach { it.run() }
        val counts = evidence.filter { it.completedWorkBlocks > 0 }.groupBy { it.familyId }
            .mapValues { (_, sessions) -> sessions.map { it.sessionId }.distinct().size }
        return ProgressSnapshot(vars.toMap(), active.toMap(), holds.toMap(), events.toList(), counts)
    }

    /** The rule was met: this exercise counts as passed (all five stars) and the routine continues with the next exercise at its bottom. */
    private fun moveOnByRule(p: VariationProgress, day: Int, events: MutableList<ProgressEvent>, active: MutableMap<String, String>, vars: MutableMap<String, VariationProgress>): VariationProgress {
        val done = p.copy(achievedTiers = (1..5).toSet(), streakDays = emptyList(), streakAssumed = 0, lastAutoAdvanceDay = day)
        val name = catalog.variation(p.variationId)?.name ?: p.variationId
        val next = if (isRotationAlternative(p.variationId)) null else policy(p.variationId)?.progressionSuccessors().orEmpty().firstOrNull { catalog.variation(it) != null && prerequisitesMet(it, vars + (p.variationId to done)) }
        if (next == null) {
            events += ProgressEvent(day, p.variationId, if (isRotationAlternative(p.variationId))
                "Every set reached the top of the range. Progress on this alternative is kept; the main exercise controls the next main step."
                else "Every set reached the top of the range - no eligible next exercise is available yet.")
            return done
        }
        active[done.familyId] = next
        if (!vars.containsKey(next)) vars[next] = VariationProgress(next, family(next), 1, day, null, emptySet(), null, emptyList(), 0, 0, false, false, null, null)
        events += ProgressEvent(day, p.variationId, "Every set of $name reached the top of the range. Moving on to ${catalog.variation(next)?.name ?: next}, starting again at the bottom of the range.")
        return done
    }

    private fun tryAdvance(
        p0: VariationProgress,
        day: Int,
        events: MutableList<ProgressEvent>,
        active: MutableMap<String, String>,
        vars: Map<String, VariationProgress>,
    ): VariationProgress {
        val pol = policy(p0.variationId) ?: return p0
        val r = evaluateAdvancement(
            AdvancementInput(
                policy = seed(pol),
                currentTier = p0.tier,
                enrolledAtDay = p0.enrolledAtDay,
                nowDay = day,
                exposures = p0.streakDays.map { QualifyingExposure(pol.variationId, it) },
                lastAutoAdvancementDay = p0.lastAutoAdvanceDay,
            ),
        )
        val assumedNote = if (p0.streakAssumed > 0) " ${p0.streakAssumed} of ${p0.streakDays.size} results were assumed (no feedback given)." else ""
        if (r is AdvancementResult.Advance) {
            val newTarget = pol.tier(r.toTier).target
            events += ProgressEvent(day, p0.variationId,
                "Tier ${r.fromTier} achieved after ${p0.streakDays.size} qualifying sessions over ${p0.streakDays.last() - p0.streakDays.first()} days. Next target: $newTarget (tier ${r.toTier}).$assumedNote")
            return p0.copy(tier = r.toTier, enrolledAtDay = day, achievedTiers = p0.achievedTiers + r.fromTier,
                lastAutoAdvanceDay = day, streakDays = emptyList(), streakAssumed = 0)
        }
        // Top tier: same pacing gate (evaluated as if a 6th tier existed) then successor move.
        if (p0.tier == 5 && 5 !in p0.achievedTiers && gateWouldPass(pol, p0, day)) {
            val achieved = p0.copy(achievedTiers = p0.achievedTiers + 5, lastAutoAdvanceDay = day, streakDays = emptyList(), streakAssumed = 0)
            val successor = if (isRotationAlternative(p0.variationId)) null else pol.progressionSuccessors().firstOrNull { prerequisitesMet(it, vars + (p0.variationId to achieved)) }
            val name = catalog.variation(p0.variationId)?.name ?: p0.variationId
            val loaded = catalog.variation(p0.variationId)?.isLoaded() == true
            val at = p0.loadGrams?.let { " at ${formatKg(it)} kg" }.orEmpty()
            val heavier = if (loaded) " Or keep $name with a heavier kettlebell: set the new weight in your equipment profile and it starts again at tier 1." else ""
            if (successor != null) {
                active[achieved.familyId] = successor
                events += ProgressEvent(day, p0.variationId,
                    "All five tiers earned$at. Moving on to ${catalog.variation(successor)?.name ?: successor} (tier 1)${if (loaded) " with the same kettlebell" else ""}; your five stars here are kept. You can switch back anytime.$heavier$assumedNote")
            } else {
                val blocked = pol.nextVariationIds.firstOrNull()
                events += ProgressEvent(day, p0.variationId,
                    if (blocked != null) "All five tiers earned$at. The harder variation ${catalog.variation(blocked)?.name ?: blocked} isn't available with your equipment/prerequisites yet, so you stay here.$heavier"
                    else if (loaded) "All five tiers earned$at — this is the top of this progression with this weight. To keep progressing, set a heavier kettlebell in your equipment profile; $name then starts again at tier 1."
                    else "All five tiers earned — this is the top of this progression.")
            }
            return achieved
        }
        return p0
    }

    private fun gateWouldPass(pol: ProgressionPolicy, p: VariationProgress, day: Int): Boolean {
        // Same pacing gate as every other tier: evaluate it as a 4 -> 5 step (the gate only
        // depends on exposures, enrollment day and the 7-day cap, not on the tier numbers).
        val r = evaluateAdvancement(
            AdvancementInput(seed(pol), 4, p.enrolledAtDay, day, p.streakDays.map { QualifyingExposure(pol.variationId, it) }, p.lastAutoAdvanceDay),
        )
        return r is AdvancementResult.Advance
    }

    /** Achievement-based prerequisites (AND of OR groups). Equipment predicates are left to the
     *  planner, which checks the current profile: equipment gates filter eligibility but never erase
     *  achievement (spec §3). */
    private fun prerequisitesMet(vid: String, vars: Map<String, VariationProgress>): Boolean {
        val rule = catalog.policyForVariation(vid)?.prerequisiteRule ?: return true
        return rule.allOf.all { group ->
            group.any { pred ->
                val vt = pred.variationTierMet
                if (vt == null) true
                else vars[vt.variationId]?.let { vt.tier in it.achievedTiers || it.tier > vt.tier } ?: false
            }
        }
    }

    /** Current prescription for a variation given a snapshot (null if not a strength variation). */
    fun currentTarget(snapshot: ProgressSnapshot, variationId: String): Pair<Int, Target>? {
        val pol = policy(variationId) ?: return null
        val tier = snapshot.tierFor(variationId) ?: 1
        return tier to pol.tier(tier).target
    }
}
