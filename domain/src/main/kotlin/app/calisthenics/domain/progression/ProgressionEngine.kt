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
import app.calisthenics.domain.model.Catalog
import app.calisthenics.domain.model.ProgressionPolicy
import app.calisthenics.domain.model.Target
import app.calisthenics.domain.Tier as SeedTier
import app.calisthenics.domain.ProgressionPolicy as SeedPolicy

const val REENTRY_DAYS = 14 // ADR 0002 C8
const val BELOW_SUGGEST_AFTER = 2 // ADR 0002 C6

/** Explicit user actions that change progression (all optional; silence never clears a hold). */
sealed interface UserAction {
    val day: Int
    /** Initial self-assessment / manual pick of a familiar variation (ONB-01). */
    data class SelfAssessment(override val day: Int, val variationId: String, val tier: Int) : UserAction
    /** Accept the app's lower-target suggestion or the easier adjustment after discomfort. */
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
) {
    /** Highest consecutive achieved tier counted from the tier the variation was started at. */
    fun earnedStars(): Int {
        val start = baselineTier ?: 1
        var t = start
        while (t in achievedTiers) t++
        return if (t == start) 0 else t - 1
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

    private fun seed(p: ProgressionPolicy) = SeedPolicy(
        variationId = p.variationId,
        version = p.version,
        tiers = p.tiers.map { SeedTier(it.index, it.target.value, it.workWindowSeconds, it.minRecoverySeconds) },
    )

    fun replay(evidence: List<SessionEvidence>, actions: List<UserAction>): ProgressSnapshot {
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
            active.putIfAbsent(p.familyId, vid)
            val hold = holds[p.familyId]
            when {
                ev.qualifying && hold == null && !p.autoPaused && ev.prescribedTier == p.tier -> {
                    val days = (p.streakDays + ev.day).distinct().sorted()
                    p = p.copy(streakDays = days, streakAssumed = p.streakAssumed + if (ev.confirmedBlocks == 0) 1 else 0,
                        consecutiveBelow = 0, lowerTargetSuggested = false)
                    p = tryAdvance(p, ev.day, events, active, vars)
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
                    val p = ensure(a.variationId, a.day, a.tier, a.tier)
                    vars[a.variationId] = p.copy(tier = a.tier, baselineTier = a.tier, enrolledAtDay = a.day)
                    active.putIfAbsent(p.familyId, a.variationId)
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
        return ProgressSnapshot(vars.toMap(), active.toMap(), holds.toMap(), events.toList())
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
            val successor = pol.nextVariationIds.firstOrNull { prerequisitesMet(it, vars) }
            if (successor != null) {
                active[achieved.familyId] = successor
                events += ProgressEvent(day, p0.variationId,
                    "All five tiers earned. Moving on to ${catalog.variation(successor)?.name ?: successor} (tier 1); your five stars here are kept. You can switch back anytime.$assumedNote")
            } else {
                val blocked = pol.nextVariationIds.firstOrNull()
                events += ProgressEvent(day, p0.variationId,
                    if (blocked != null) "All five tiers earned. The harder variation ${catalog.variation(blocked)?.name ?: blocked} isn't available with your equipment/prerequisites yet, so you stay here."
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
