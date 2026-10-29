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
fun selfAssessmentFor(variationId: String, stored: Int, day: Int = 0): UserAction.SelfAssessment =
    if (stored in 11..14) UserAction.SelfAssessment(day, variationId, stored - 10 + 1, stored - 10) else UserAction.SelfAssessment(day, variationId, stored)

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
    val baselineTier: Int?, // starting level, self-reported or inferred; completed tiers remain separate (C5)
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
    val targetRevision: Int = 0,
    /** Effective policy from the immutable v1 plan snapshot; absent for historical v0 evidence. */
    val effectivePolicySnapshot: ProgressionPolicy? = null,
    /** Highest higher tier established by complete actual results, separate from self-assessment estimates. */
    val demonstratedTier: Int? = null,
    /** Personal-target revision under which [demonstratedTier] was established. */
    val demonstratedTargetRevision: Int? = null,
    /** W2: after a session fell short, the per-set target for the next one is the lowest logged value; it builds back up to the level's own target. Applies to level [reducedTargetTier] only. */
    val reducedTarget: Int? = null,
    val reducedTargetTier: Int? = null,
    /** W2: at level 5 of a route with no next exercise, the automatically raised level-5 target (+1 rep / +5 s after each qualifying streak); null = the level's own target. */
    val topTarget: Int? = null,
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

/** [easierVariationId] at [easierAcceptedTier] is the easier prescription (W2: chosen automatically after discomfort); [steps] counts how often discomfort stepped it down. */
data class FamilyHold(val familyId: String, val sinceDay: Int, val easierAcceptedTier: Int?, val easierVariationId: String?, val steps: Int = 0)

data class ProgressEvent(val day: Int, val variationId: String, val message: String)

data class ProgressSnapshot(
    val variations: Map<String, VariationProgress>,
    /** familyId -> variation the user currently trains in that family (moves on successor transitions). */
    val activeInFamily: Map<String, String>,
    val holds: Map<String, FamilyHold>,
    val events: List<ProgressEvent>,
    /** Distinct workouts with completed work in a family, rebuilt from evidence rather than stored counters. */
    val familyWorkoutCounts: Map<String, Int> = emptyMap(),
    /** Distinct workouts completed in a route, including explicitly approved cross-family alternatives. */
    val routeWorkoutCounts: Map<String, Int> = emptyMap(),
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
    private fun selectedRoute(vid: String, routeId: String? = null) = catalog.routeById(routeId)?.takeIf { vid in it.mainPath || it.members.any { m -> m.variationId == vid } }
        ?: catalog.mainRouteFor(vid)
    private fun isRotationAlternative(vid: String, routeId: String? = null): Boolean {
        val route = selectedRoute(vid, routeId)
        val member = route?.members?.firstOrNull { it.variationId == vid }
        if (member != null) return member.role != app.calisthenics.domain.model.ProgressionRole.MAIN
        if (route != null && vid in route.mainPath) return false
        return catalog.exerciseGroups.any { vid in it.alternativeVariationIds }
    }
    private fun mainFor(vid: String, routeId: String? = null): String {
        val route = selectedRoute(vid, routeId)
        val member = route?.members?.firstOrNull { it.variationId == vid }
        if (member != null) return member.atVariationId ?: vid
        if (route != null && vid in route.mainPath) return vid
        return catalog.exerciseGroups.firstOrNull { vid in it.alternativeVariationIds }?.mainVariationId ?: vid
    }
    private fun mainSuccessor(vid: String, routeId: String? = null): String? {
        val route = selectedRoute(vid, routeId)
        if (route != null) {
            val member = route.members.firstOrNull { it.variationId == vid }
            if (member != null && member.role != app.calisthenics.domain.model.ProgressionRole.MAIN) return null
            val anchor = catalog.routeAnchorFor(vid, route) ?: return null
            return route.mainPath.getOrNull(route.mainPath.indexOf(anchor) + 1)
        }
        return policy(vid)?.progressionSuccessors()?.firstOrNull()
    }

    /**
     * R8.1-T01 (F1): the route position follows the highest rung the person has trained, whether on the main step or on an alternate of
     * that rung (split squat after air squat, feet-elevated or diamond after standard). It never moves down; successor transitions still move it up.
     */
    private fun raiseActive(active: MutableMap<String, String>, familyId: String, vid: String, routeId: String?, trained: Boolean) {
        val main = mainFor(vid, routeId)
        val current = active[familyId]
        if (current == null) { active[familyId] = main; return }
        if (!trained || current == main) return
        val route = selectedRoute(vid, routeId) ?: return
        val from = route.mainPath.indexOf(current)
        val to = route.mainPath.indexOf(main)
        if (from >= 0 && to > from) active[familyId] = main
    }

    /** R8.1-T04 (F4): the other exercises of the same rung of the route: the main step and its same-rung alternates. */
    private fun rungMates(vid: String, routeId: String?): List<String> {
        val route = selectedRoute(vid, routeId)
        if (route != null) {
            val anchor = catalog.routeAnchorFor(vid, route) ?: return emptyList()
            return (listOf(anchor) + route.members.filter { it.role == app.calisthenics.domain.model.ProgressionRole.ALTERNATE && it.atVariationId == anchor }.map { it.variationId })
                .filter { it != vid }
        }
        val group = catalog.exerciseGroups.firstOrNull { vid == it.mainVariationId || vid in it.alternativeVariationIds } ?: return emptyList()
        return (listOf(group.mainVariationId) + group.alternativeVariationIds).filter { it != vid }
    }

    /** The qualifying days that count for [p]: its own and the ones of its same-rung exercises since the level was enrolled or last advanced. */
    private fun pooledDays(p: VariationProgress, vars: Map<String, VariationProgress>, routeId: String?, strictLevelMatch: Boolean = false): List<Int> {
        val floor = maxOf(p.enrolledAtDay, (p.lastAutoAdvanceDay ?: Int.MIN_VALUE).let { if (it == Int.MIN_VALUE) it else it + 1 })
        val ownPolicy = p.effectivePolicySnapshot ?: policy(p.variationId)
        val others = rungMates(p.variationId, routeId).flatMap { id ->
            val other = vars[id]
            val otherPolicy = other?.effectivePolicySnapshot ?: policy(id)
            val mine = ownPolicy?.tiers?.firstOrNull { it.index == p.tier }
            val theirs = otherPolicy?.tiers?.firstOrNull { it.index == p.tier }
            val compatible = mine != null && theirs != null && mine.target == theirs.target &&
                mine.minQualifyingBlocks == theirs.minQualifyingBlocks && ownPolicy?.tierProfile == otherPolicy?.tierProfile
            other?.takeIf { it.targetRevision == p.targetRevision && (!strictLevelMatch || (it.tier == p.tier && compatible)) }?.streakDays.orEmpty()
        }.filter { it >= floor }
        return (p.streakDays + others).distinct().sorted()
    }

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
        val vars = mutableMapOf<String, VariationProgress>()
        val active = mutableMapOf<String, String>()
        val holds = mutableMapOf<String, FamilyHold>()
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
            // This is a planned starting estimate, not evidence of completed tiers, including when skipped.
            val inferred = ev.inferredStartTier?.takeIf { it == ev.prescribedTier }
            // Version 1 plans can only establish a higher current tier from explicit demonstrated results.
            // Keep an inferred starting estimate in baselineTier, but never promote an untracked variation
            // merely because its first plan selected a higher prescription.
            val initialTier = if (ev.progressionEvidenceVersion >= 1 && inferred == null) 1 else (ev.prescribedTier ?: 1)
            var p = ensure(vid, ev.day, tier = initialTier, baseline = inferred)
            if (p.targetRevision != ev.targetRevision) p = p.copy(targetRevision = ev.targetRevision, streakDays = emptyList(), streakAssumed = 0, consecutiveAbove = 0, raiseSuggested = false,
                demonstratedTier = null, demonstratedTargetRevision = null,
                reducedTarget = null, reducedTargetTier = null, topTarget = null)
            if (ev.progressionEvidenceVersion >= 1 && ev.effectivePolicySnapshot != null) {
                p = p.copy(effectivePolicySnapshot = ev.effectivePolicySnapshot)
            }
            p = p.copy(lastTrainedDay = maxOf(p.lastTrainedDay ?: ev.day, ev.day))
            val start = p
            raiseActive(active, p.familyId, vid, ev.progressionRouteId, ev.completedWorkBlocks > 0 && !ev.skippedOrPartial)
            var hold = holds[p.familyId]
            // W2: a clean session on the easier prescription ends the hold; normal progression resumes from that (easier) level, this session being the first one.
            if (hold != null && ev.variationId == hold.easierVariationId && ev.day > hold.sinceDay && ev.completedWorkBlocks > 0 && !ev.skippedOrPartial && !ev.discomfort &&
                (ev.prescribedTier == null || hold.easierAcceptedTier == null || ev.prescribedTier <= hold.easierAcceptedTier)) {
                holds.remove(p.familyId)
                val route = selectedRoute(vid, ev.progressionRouteId)
                if (!isRotationAlternative(vid, ev.progressionRouteId) && (route == null || vid in route.mainPath)) active[p.familyId] = vid
                p = p.copy(streakDays = emptyList(), streakAssumed = 0, consecutiveBelow = 0, lowerTargetSuggested = false)
                events += ProgressEvent(ev.day, vid, "Easier option done without discomfort. You now move up as usual from level ${p.tier} of ${catalog.variation(vid)?.name ?: vid}.")
                hold = null
            }
            val definition = catalog.personalTargets[vid]
            val staleDefinition = definition != null && ev.targetRevision != definition.revision &&
                definition.modifiedAtEpochMs > 0 && ev.recordedAtEpochMs >= definition.modifiedAtEpochMs
            // Kettlebell weight: levels are earned per weight (owner 2026-10-08).
            val load = ev.loadGrams
            val recorded = p.loadGrams
            var lighter = false
            var heavierTrackReset = false
            if (load != null) when {
                recorded == null -> p = p.copy(loadGrams = load)
                load > recorded -> {
                    heavierTrackReset = true
                    val kept = p.earnedStars()
                    p = p.copy(tier = 1, baselineTier = null, achievedTiers = emptySet(), enrolledAtDay = ev.day,
                        lastAutoAdvanceDay = null, streakDays = emptyList(), streakAssumed = 0, consecutiveBelow = 0,
                        lowerTargetSuggested = false, loadGrams = load,
                        demonstratedTier = null, demonstratedTargetRevision = null,
                        starsByLoad = p.starsByLoad + (recorded to maxOf(kept, p.starsByLoad[recorded] ?: 0)))
                    events += ProgressEvent(ev.day, vid,
                        "Heavier kettlebell (${formatKg(load)} kg): ${catalog.variation(vid)?.name ?: vid} starts again at level 1. Your $kept stars at ${formatKg(recorded)} kg are kept.")
                }
                load < recorded -> lighter = true
            }
            val v1 = ev.progressionEvidenceVersion >= 1
            val unknownWeightOnTrack = v1 && recorded != null && load == null
            if (lighter) {
                events += ProgressEvent(ev.day, vid,
                    "Done with a lighter kettlebell (${formatKg(load!!)} kg instead of ${formatKg(recorded!!)} kg) — logged, but it does not count towards your next level.")
            } else {
                val canAdopt = v1 && autoProgression && !unknownWeightOnTrack && !heavierTrackReset &&
                    !staleDefinition && hold == null && !p.autoPaused && ev.demonstratedQualifying &&
                    (ev.demonstratedTier ?: p.tier) > p.tier && (ev.demonstratedTier ?: 0) <= 5
                if (canAdopt) {
                    val adopted = ev.demonstratedTier!!
                    p = p.copy(
                        tier = adopted,
                        baselineTier = maxOf(p.baselineTier ?: 1, adopted),
                        enrolledAtDay = ev.day,
                        streakDays = listOf(ev.day),
                        streakAssumed = 0,
                        consecutiveBelow = 0,
                        lowerTargetSuggested = false,
                        consecutiveAbove = 0,
                        raiseSuggested = false,
                        ruleAbove = false,
                        demonstratedTier = maxOf(p.demonstratedTier ?: 0, adopted),
                        demonstratedTargetRevision = ev.targetRevision,
                        reducedTarget = null, reducedTargetTier = null, topTarget = null,
                    )
                    events += ProgressEvent(ev.day, vid,
                        "You met the level $adopted targets, so level $adopted is now your level. 1 of 3 good workouts toward the next level.")
                } else when {
                staleDefinition -> Unit
                unknownWeightOnTrack -> Unit
                // V21: Rep range / Custom rule: every set at the top of the range, in a row, moves you on to the next exercise (tier-free).
                ev.ruleMet != null -> {
                    if (ev.ruleMet && hold == null && !p.autoPaused) {
                        val days = (p.streakDays + ev.day).distinct().sorted()
                        p = p.copy(streakDays = days)
                        if (autoProgression && days.size >= ev.ruleSessions) p = moveOnByRule(p, ev.day, events, active, vars, ev.progressionRouteId)
                    } else if (!ev.ruleMet) p = p.copy(streakDays = emptyList(), streakAssumed = 0)
                }
                (if (v1) {
                    (ev.qualifying && ev.prescribedTier == p.tier) || (ev.demonstratedQualifying && ev.demonstratedTier != null && ev.demonstratedTier >= p.tier)
                } else {
                    ev.qualifying && (ev.prescribedTier == p.tier || (ev.levelFiveEligible && ev.prescribedTier == 5))
                }) && hold == null && !p.autoPaused && (!v1 || (!heavierTrackReset && !unknownWeightOnTrack)) -> {
                    // Above-threshold evidence trains level 5, with the normal multi-session gate.
                    // It cannot reuse a lower-tier streak or create a sixth level.
                    if (!v1 && autoProgression && ev.levelFiveEligible && ev.prescribedTier == 5 && p.tier < 5) p = p.copy(tier = 5, enrolledAtDay = ev.day, streakDays = emptyList(), streakAssumed = 0)
                    val days = (p.streakDays + ev.day).distinct().sorted()
                    p = p.copy(streakDays = days, streakAssumed = p.streakAssumed + if (ev.confirmedBlocks == 0) 1 else 0,
                        consecutiveBelow = 0, lowerTargetSuggested = false)
                    if (autoProgression) {
                        // R8.1-T04 (F4): the same session also counts for the exercises of the same rung (feet-elevated and diamond count together).
                        // Everything is evaluated on the same snapshot, so one exercise's advance does not take the days away from the other.
                        vars[vid] = p
                        val snapshot = vars.toMap()
                        val mates = rungMates(vid, ev.progressionRouteId).mapNotNull { mate ->
                            val m = snapshot[mate]?.takeIf { !it.autoPaused && holds[it.familyId] == null && it.targetRevision == p.targetRevision && (!v1 || it.tier == p.tier) && (!ev.levelFiveEligible || it.tier == 5) } ?: return@mapNotNull null
                            mate to tryAdvance(m, ev.day, events, active, snapshot, ev.progressionRouteId, pooledDays(m, snapshot, ev.progressionRouteId, strictLevelMatch = v1))
                        }
                        p = tryAdvance(p, ev.day, events, active, snapshot, ev.progressionRouteId,
                            pooledDays(p, snapshot, ev.progressionRouteId, strictLevelMatch = v1),
                            levelFiveCompletion = ev.levelFiveEligible,
                            demonstratedLevelFive = v1 && ev.demonstratedTier == 5)
                        mates.forEach { (k, v) -> vars[k] = v }
                    }
                }
                ev.anyBelow && !ev.discomfort -> {
                    // R8.1-T04 (F4): a session below target is simply not a qualifying one; it no longer wipes the qualifying sessions already inside the 14-day window
                    val below = p.consecutiveBelow + 1
                    p = p.copy(consecutiveBelow = below)
                    if (below >= BELOW_SUGGEST_AFTER && !p.lowerTargetSuggested && p.tier > 1) {
                        p = p.copy(lowerTargetSuggested = true)
                        events += ProgressEvent(ev.day, vid, "Below target twice in a row. An easier level (level ${p.tier - 1}) is suggested. Nothing changes unless you accept.")
                    } else {
                        events += ProgressEvent(ev.day, vid, "Below target today. You stay at level ${p.tier} and build up again.")
                    }
                }
                ev.skippedOrPartial || ev.easierOverride -> Unit   // R8.1-T04 (F4): not qualifying, not a reset
                else -> Unit
            }
            }
            if (v1 && autoProgression && ev.demonstratedQualifying && !staleDefinition && !heavierTrackReset &&
                !unknownWeightOnTrack && !lighter && hold == null && !p.autoPaused) {
                val demonstrated = ev.demonstratedTier!!
                p = p.copy(
                    demonstratedTier = maxOf(p.demonstratedTier ?: 0, demonstrated),
                    demonstratedTargetRevision = ev.targetRevision,
                )
            }
            // W2 (owner): when logged sets fell short of the target, the lowest logged value is the target for the next session, then it builds back up (+1 rep / +5 s per clean session) to the level's own target.
            if (ev.ruleMet == null && !ev.discomfort && !staleDefinition && !lighter && !unknownWeightOnTrack && !heavierTrackReset) {
                val pol = p.effectivePolicySnapshot ?: policy(vid)
                val levelTarget = pol?.tiers?.firstOrNull { it.index == p.tier }?.target
                val own = if (levelTarget != null) app.calisthenics.domain.planner.cappedTarget(levelTarget, pol).value else null
                val stepUp = if (levelTarget?.type == app.calisthenics.domain.model.TargetType.HOLD_SECONDS) 5 else 1
                when {
                    p.tier != start.tier -> p = p.copy(reducedTarget = null, reducedTargetTier = null)
                    ev.shortfallValue != null && own != null && ev.prescribedTier == p.tier -> {
                        val low = ev.shortfallValue
                        if (low < own) {
                            p = p.copy(reducedTarget = low, reducedTargetTier = p.tier, topTarget = if (p.tier == 5 && (p.topTarget ?: 0) > 0) null else p.topTarget)
                            events += ProgressEvent(ev.day, vid, "Your lowest set was $low. That is your target next time ($low per set), then it builds back up to $own.")
                        } else if (p.tier == 5 && p.topTarget != null && low < p.topTarget!!) {
                            p = p.copy(topTarget = low.takeIf { it > own })
                            events += ProgressEvent(ev.day, vid, "Your lowest set was $low. That is your level 5 target next time.")
                        }
                    }
                    p.reducedTarget != null && p.reducedTargetTier == p.tier && ev.shortfallValue == null && ev.completedWorkBlocks > 0 && !ev.skippedOrPartial && ev.prescribedTier == p.tier -> {
                        val next = p.reducedTarget!! + stepUp
                        val demonstrated = ev.demonstratedQualifying && (ev.demonstratedTier ?: 0) >= p.tier
                        p = if (own == null || next >= own || demonstrated) p.copy(reducedTarget = null, reducedTargetTier = null, consecutiveBelow = 0, lowerTargetSuggested = false)
                            else p.copy(reducedTarget = next, consecutiveBelow = 0, lowerTargetSuggested = false)
                    }
                }
            }
            // V02 / D7, R7.2-T06 (F7, O1): "too easy" (once is enough) suggests a move on; the tier itself never changes here. What the move is (two levels, or the next exercise) is decided in [suggest].
            val byRule = ev.ruleMet != null
            val tooEasy = !staleDefinition && ev.anyAbove && (byRule || ev.prescribedTier == start.tier) && !ev.anyBelow && !ev.discomfort && !ev.skippedOrPartial && !lighter
            val above = if (tooEasy && p.tier == start.tier) p.consecutiveAbove + 1 else 0
            val suggest = above >= ABOVE_SUGGEST_AFTER && !p.raiseSuggested && p.tier == start.tier
            p = p.copy(consecutiveAbove = above, raiseSuggested = if (p.tier != start.tier) false else p.raiseSuggested || suggest, ruleAbove = if (above > 0) byRule else false)
            if (suggest) events += ProgressEvent(ev.day, vid, "You marked this too easy. A step up is suggested. Nothing changes unless you accept.")
            vars[vid] = p
        }
        for (ev in evidence.filter { it.discomfort }) {
            val reportDay = ev.discomfortReportedDay ?: ev.day
            items += Item(reportDay, 1, "d" + ev.sessionId + ev.variationId) {
                val fam = family(ev.variationId)
                val cur = vars[ev.variationId]
                // W2 (owner): discomfort automatically steps the prescription one level easier (never harder), without anything to accept.
                // Repeated discomfort steps down again; a clean session on the easier prescription ends the hold (see the session items).
                val base = minOf(ev.prescribedTier ?: cur?.tier ?: 1, cur?.tier ?: Int.MAX_VALUE)
                val step = easierStep(catalog, ev.variationId, base, ev.progressionRouteId)
                val target = vars[step.variationId] ?: ensure(step.variationId, reportDay, step.tier)
                val tier = if (step.variationId == ev.variationId) step.tier else minOf(target.tier, step.tier)
                vars[step.variationId] = target.copy(tier = tier, enrolledAtDay = reportDay, streakDays = emptyList(), streakAssumed = 0, consecutiveBelow = 0,
                    lowerTargetSuggested = false, consecutiveAbove = 0, raiseSuggested = false, reducedTarget = null, reducedTargetTier = null, topTarget = null)
                val before = holds[fam]
                holds[fam] = FamilyHold(fam, reportDay, tier, step.variationId, (before?.steps ?: 0) + 1)
                val name = catalog.variation(step.variationId)?.name ?: step.variationId
                events += ProgressEvent(reportDay, ev.variationId,
                    "You reported discomfort. The next workouts use an easier option ($name, level $tier). Once you finish it without discomfort, you move up as usual.")
                if (step.variationId != ev.variationId) vars[ev.variationId]?.let { vars[ev.variationId] = it.copy(streakDays = emptyList(), streakAssumed = 0) }
            }
        }
        for ((i, a) in actions.withIndex()) items += Item(a.day, 2, "a" + i.toString().padStart(6, '0')) {
            when (a) {
                is UserAction.SelfAssessment -> {
                    // W2: a dated (day > 0) hand edit / questionnaire answer restarts this exercise from its day: earlier evidence neither raises it above the answer nor leaves stale streaks or suggestions.
                    if (a.day > 0) vars[a.variationId]?.let { old -> vars[a.variationId] = old.copy(achievedTiers = emptySet(), lastAutoAdvanceDay = null, streakDays = emptyList(), streakAssumed = 0,
                        consecutiveBelow = 0, lowerTargetSuggested = false, consecutiveAbove = 0, raiseSuggested = false, ruleAbove = false, demonstratedTier = null,
                        demonstratedTargetRevision = null, reducedTarget = null, reducedTargetTier = null, topTarget = null, autoPaused = false) }
                    val top = policy(a.variationId)?.tiers?.maxOfOrNull { it.index } ?: 5
                    if (a.tier > top) {
                        // R7-T04 (B1): above the top step: the top step is achieved too (five stars) and the successor is active at level 1
                        val p = ensure(a.variationId, a.day, top, top)
                        vars[a.variationId] = p.copy(tier = top, baselineTier = top, enrolledAtDay = a.day, achievedTiers = (1..top).toSet())
                        raiseActive(active, p.familyId, a.variationId, null, true)
                        val next = if (isRotationAlternative(a.variationId)) null else mainSuccessor(a.variationId)?.takeIf { catalog.variation(it) != null && prerequisitesMet(it, vars) }
                        if (next != null) {
                            if (!vars.containsKey(next)) vars[next] = VariationProgress(next, family(next), 1, a.day, null, emptySet(), null, emptyList(), 0, 0, false, false, null, null)
                            active[p.familyId] = next
                        }
                    } else if (a.achievedUpTo > 0) {
                        // R7-T11 (D5): set by hand: levels 1..achievedUpTo are done, [tier] is the one trained next
                        val p = ensure(a.variationId, a.day, a.tier, 1)
                        vars[a.variationId] = p.copy(tier = a.tier, baselineTier = 1, enrolledAtDay = a.day, achievedTiers = (1..a.achievedUpTo).toSet())
                        raiseActive(active, p.familyId, a.variationId, null, true)
                    } else {
                        val p = ensure(a.variationId, a.day, a.tier, a.tier)
                        vars[a.variationId] = p.copy(tier = a.tier, baselineTier = a.tier, enrolledAtDay = a.day)
                        raiseActive(active, p.familyId, a.variationId, null, true)
                    }
                }
                is UserAction.AcceptHarder -> {
                    val p = ensure(a.variationId, a.day)
                    // R7.2-T06 (F7): the levels jumped over count as done (stars), the same way the hand edit (D5) does
                    val skipped = if (a.tier > p.tier) (p.tier until a.tier).toSet() else emptySet()
                    vars[a.variationId] = p.copy(tier = a.tier, enrolledAtDay = a.day, streakDays = emptyList(),
                        streakAssumed = 0, consecutiveAbove = 0, raiseSuggested = false, ruleAbove = false, achievedTiers = p.achievedTiers + skipped)
                    events += ProgressEvent(a.day, a.variationId, "You moved up to level ${a.tier}.")
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
                    events += ProgressEvent(a.day, a.variationId, "You moved down to level ${a.tier}.")
                }
                is UserAction.ComfortClearance -> {
                    if (holds.remove(a.familyId) != null) {
                        for ((id, v) in vars.toMap()) if (v.familyId == a.familyId) vars[id] = v.copy(streakDays = emptyList(), streakAssumed = 0, enrolledAtDay = a.day)
                        events += ProgressEvent(a.day, active[a.familyId] ?: a.familyId,
                            "You said you're comfortable again. You move up as usual from your next workouts.")
                    }
                }
                is UserAction.ReentryChoice -> {
                    val p = ensure(a.variationId, a.day)
                    vars[a.variationId] = p.copy(tier = a.tier, enrolledAtDay = a.day, streakDays = emptyList(),
                        streakAssumed = 0, reentryResolvedDay = a.day)
                    events += ProgressEvent(a.day, a.variationId, "Welcome back. You restart at level ${a.tier}. Your stars are kept.")
                }
                is UserAction.ManualOverride -> {
                    val p = ensure(a.variationId, a.day)
                    val easier = a.tier < p.tier
                    vars[a.variationId] = p.copy(tier = a.tier, enrolledAtDay = a.day, streakDays = emptyList(),
                        streakAssumed = 0, autoPaused = easier)
                    events += ProgressEvent(a.day, a.variationId,
                        if (easier) "You chose an easier level (level ${a.tier}). Moving up is paused until you go back to normal."
                        else "You chose a harder level (level ${a.tier}).")
                }
                is UserAction.ResumeNormal -> {
                    vars[a.variationId]?.let { vars[a.variationId] = it.copy(autoPaused = false) }
                }
            }
        }

        items.sortedWith(compareBy({ it.day }, { it.order }, { it.key })).forEach { it.run() }
        val counts = evidence.filter { it.completedWorkBlocks > 0 }.groupBy { it.familyId }
            .mapValues { (_, sessions) -> sessions.map { it.sessionId }.distinct().size }
        val routeCounts = evidence.filter { it.completedWorkBlocks > 0 && it.progressionRouteId != null }
            .groupBy { it.progressionRouteId!! }.mapValues { (_, sessions) -> sessions.map { it.sessionId }.distinct().size }
        // Keep earned achievements while an edited definition starts a fresh prospective streak.
        for ((id, value) in vars.toMap()) if (value.targetRevision != (catalog.personalTargets[id]?.revision ?: 0)) vars[id] = value.copy(streakDays = emptyList(), streakAssumed = 0)
        return ProgressSnapshot(vars.entries.sortedBy { it.key }.associate { it.toPair() }, active.entries.sortedBy { it.key }.associate { it.toPair() }, holds.entries.sortedBy { it.key }.associate { it.toPair() }, events.toList(), counts, routeCounts)
    }

    /** The qualifying days that count towards the next level of [variationId] in [snapshot]: its own and the ones of its same-rung exercises since the level began. */
    fun qualifyingDays(snapshot: ProgressSnapshot, variationId: String, routeId: String? = null): List<Int> {
        val p = snapshot.variations[variationId] ?: return emptyList()
        return pooledDays(p, snapshot.variations, routeId, strictLevelMatch = true)
    }

    /** The rule was met: this exercise counts as passed (all five stars) and the routine continues with the next exercise at its bottom. */
    private fun moveOnByRule(p: VariationProgress, day: Int, events: MutableList<ProgressEvent>, active: MutableMap<String, String>, vars: MutableMap<String, VariationProgress>, routeId: String? = null): VariationProgress {
        val done = p.copy(achievedTiers = (1..5).toSet(), streakDays = emptyList(), streakAssumed = 0, lastAutoAdvanceDay = day)
        val name = catalog.variation(p.variationId)?.name ?: p.variationId
        val next = if (isRotationAlternative(p.variationId, routeId)) null else mainSuccessor(p.variationId, routeId)?.takeIf { catalog.variation(it) != null && prerequisitesMet(it, vars + (p.variationId to done)) }
        if (next == null) {
            events += ProgressEvent(day, p.variationId, if (isRotationAlternative(p.variationId, routeId))
                "Every set reached the top of the range. Your progress on this alternative is kept. The main exercise decides the next step."
                else "Every set reached the top of the range - no next exercise is available yet.")
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
        routeId: String? = null,
        /** The qualifying days that count (own and same-rung); default: the exercise's own. */
        days: List<Int> = p0.streakDays,
        levelFiveCompletion: Boolean = false,
        demonstratedLevelFive: Boolean = false,
    ): VariationProgress {
        val pol = p0.effectivePolicySnapshot ?: policy(p0.variationId) ?: return p0
        val r = evaluateAdvancement(
            AdvancementInput(
                policy = seed(pol),
                currentTier = p0.tier,
                enrolledAtDay = p0.enrolledAtDay,
                nowDay = day,
                exposures = days.map { QualifyingExposure(pol.variationId, it) },
                lastAutoAdvancementDay = p0.lastAutoAdvanceDay,
            ),
        )
        val assumedNote = if (p0.streakAssumed > 0) " ${p0.streakAssumed} of ${p0.streakDays.size} results were assumed (no feedback given)." else ""
        if (r is AdvancementResult.Advance) {
            val newTarget = pol.tier(r.toTier).target
            events += ProgressEvent(day, p0.variationId,
                "Level ${r.fromTier} done after ${r.evidenceDays.size} good workouts in ${r.evidenceDays.last() - r.evidenceDays.first()} days. Next: level ${r.toTier}, target $newTarget.$assumedNote")
            return p0.copy(tier = r.toTier, enrolledAtDay = day, achievedTiers = p0.achievedTiers + r.fromTier,
                lastAutoAdvanceDay = day, streakDays = emptyList(), streakAssumed = 0)
        }
        // Top tier: same pacing gate (evaluated as if a 6th tier existed) then successor move.
        // W2: back on a rung after an easier step (discomfort) with all five levels already earned: the same gate lets it move on again.
        val returning = p0.tier == 5 && 5 in p0.achievedTiers && active[p0.familyId] == p0.variationId && !isRotationAlternative(p0.variationId, routeId) &&
            mainSuccessor(p0.variationId, routeId)?.takeIf { prerequisitesMet(it, vars) } != null
        if (p0.tier == 5 && (5 !in p0.achievedTiers || returning) && gateWouldPass(pol, p0, day, days)) {
            val earnedAtTop = when {
                demonstratedLevelFive -> setOf(5) // lower stars come from baselineTier, not fabricated achievements
                levelFiveCompletion -> (1..5).toSet() // preserve historical level-five inference
                else -> setOf(5)
            }
            val achieved = p0.copy(achievedTiers = p0.achievedTiers + earnedAtTop, lastAutoAdvanceDay = day, streakDays = emptyList(), streakAssumed = 0)
            val successor = if (isRotationAlternative(p0.variationId, routeId)) null else mainSuccessor(p0.variationId, routeId)?.takeIf { prerequisitesMet(it, vars + (p0.variationId to achieved)) }
            val name = catalog.variation(p0.variationId)?.name ?: p0.variationId
            val loaded = catalog.variation(p0.variationId)?.isLoaded() == true
            val at = p0.loadGrams?.let { " at ${formatKg(it)} kg" }.orEmpty()
            val heavier = if (loaded) " Or keep $name with a heavier kettlebell: set the new weight in your equipment profile and it starts again at level 1." else ""
            if (successor != null) {
                active[achieved.familyId] = successor
                events += ProgressEvent(day, p0.variationId,
                    "All five levels done$at. Moving on to ${catalog.variation(successor)?.name ?: successor} (level 1)${if (loaded) " with the same kettlebell" else ""}; your five stars here are kept. You can switch back anytime.$heavier$assumedNote")
            } else {
                val blocked = mainSuccessor(p0.variationId, routeId)
                events += ProgressEvent(day, p0.variationId,
                    if (blocked != null) "All five tiers earned$at. The harder variation ${catalog.variation(blocked)?.name ?: blocked} isn't available with your equipment yet, so you stay here.$heavier"
                    else if (loaded) "All five levels done$at. This is the top with this weight. To keep progressing, set a heavier kettlebell in your equipment profile; $name then starts again at level 1."
                    else "All five levels done. This is the top of this exercise.")
            }
            return achieved
        }
        // W2 (owner): the top of a route (level 5 earned, no next exercise): keep raising the target after each qualifying streak (+1 rep, +5 s for holds); the planner also keeps alternating same-rung variations.
        if (p0.tier == 5 && 5 in p0.achievedTiers && !routeHasNext(p0.variationId, routeId) && catalog.variation(p0.variationId)?.isLoaded() != true && gateWouldPass(pol, p0, day, days)) {
            val five = pol.tier(5).target
            val current = p0.topTarget ?: five.value
            val raised = current + if (five.type == app.calisthenics.domain.model.TargetType.HOLD_SECONDS) 5 else 1
            val cap = if (five.type == app.calisthenics.domain.model.TargetType.REPS) pol.maxRepsPerSet else null
            if (raised <= 999 && (cap == null || raised <= cap)) {
                val unit = if (five.type == app.calisthenics.domain.model.TargetType.HOLD_SECONDS) "s" else " reps"
                events += ProgressEvent(day, p0.variationId, "Top of the ladder: the level 5 target of ${catalog.variation(p0.variationId)?.name ?: p0.variationId} rises to $raised$unit.$assumedNote")
                return p0.copy(topTarget = raised, lastAutoAdvanceDay = day, streakDays = emptyList(), streakAssumed = 0)
            }
        }
        return p0
    }

    /** W2: does the route of [vid] continue after the rung of [vid]? (No route: does its policy name a successor?) */
    private fun routeHasNext(vid: String, routeId: String?): Boolean {
        val route = selectedRoute(vid, routeId)
        if (route != null) {
            val anchor = catalog.routeAnchorFor(vid, route) ?: return false
            val at = route.mainPath.indexOf(anchor)
            return at >= 0 && at + 1 < route.mainPath.size
        }
        return policy(vid)?.progressionSuccessors()?.isNotEmpty() == true
    }

    private fun gateWouldPass(pol: ProgressionPolicy, p: VariationProgress, day: Int, days: List<Int> = p.streakDays): Boolean {
        // Same pacing gate as every other tier: evaluate it as a 4 -> 5 step (the gate only
        // depends on exposures, enrollment day and the 7-day cap, not on the tier numbers).
        val r = evaluateAdvancement(
            AdvancementInput(seed(pol), 4, p.enrolledAtDay, day, days.map { QualifyingExposure(pol.variationId, it) }, p.lastAutoAdvanceDay),
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
