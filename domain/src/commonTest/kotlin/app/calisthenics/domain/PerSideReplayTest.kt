package app.calisthenics.domain

import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.feedback.Feedback
import app.calisthenics.domain.feedback.Execution
import app.calisthenics.domain.feedback.SessionEvidence
import app.calisthenics.domain.feedback.deriveEvidence
import app.calisthenics.domain.feedback.resolveFeedback
import app.calisthenics.domain.model.BlockType
import app.calisthenics.domain.model.Catalog
import app.calisthenics.domain.model.Side
import app.calisthenics.domain.model.StrengthFocus
import app.calisthenics.domain.model.TimelineBlock
import app.calisthenics.domain.model.WorkoutPlan
import app.calisthenics.domain.progression.ProgressionEngine
import app.calisthenics.domain.progression.UserAction
import app.calisthenics.domain.progression.VariationProgress
import kotlin.test.Test

/**
 * 9.3 L1: making an exercise ALTERNATING changes how a number is shown, never how history replays. Levels, stars and streaks come from each
 * session's own plan (its prescribed level and its policy snapshot), so a history recorded under the old definition replays the same under the new.
 * The one exception to "numbers unchanged" is the plank shoulder tap: 9.2 stored TOTAL taps (10 to 20), 9.3 stores them per side (5 to 10).
 */
class PerSideReplayTest {
    private val catalog = parseCatalog(fixtureText("content/starter/catalog.json"))

    /** The definitions before 9.3: no side mode for the lunge and the twist; totals for the shoulder tap. */
    private val oldCatalog: Catalog = catalog.copy(
        variations = catalog.variations.map { if (it.id in OLD_NONE) it.copy(sides = null) else it },
        policies = catalog.policies.map { p ->
            if (p.variationId == TAP) p.copy(tiers = p.tiers.mapIndexed { i, t -> t.copy(target = t.target.copy(value = OLD_TAP_TOTALS[i])) }) else p
        },
    )

    private fun plan(source: Catalog, vid: String, tier: Int, day: Int): WorkoutPlan {
        val policy = source.policyForVariation(vid)!!
        val t = policy.tier(tier)
        val base = WorkoutPlan("p$vid$day", "usual", 1, 1, 0, "home", 180, 180, setOf(StrengthFocus.FULL_BODY), false, null, 3, emptyList(), emptyList(),
            false, false, emptyList(), progressionEvidenceVersion = 1, progressionPolicies = mapOf(vid to policy))
        return base.copy(blocks = List(3) { i ->
            TimelineBlock("w${i + 1}", BlockType.WORK, maxOf(60, t.workWindowSeconds), i + 1, "x", vid, side = Side.NONE, target = t.target,
                prescriptionTier = tier, freeTarget = false, alternating = source.variation(vid)!!.alternating)
        })
    }

    private fun session(source: Catalog, vid: String, tier: Int, day: Int, reps: Int = source.policyForVariation(vid)!!.tier(tier).target.value): SessionEvidence {
        val p = plan(source, vid, tier, day)
        val work = p.blocks.filter { it.type == BlockType.WORK }
        val outcomes = resolveFeedback(p, work.associate { it.id to Execution.COMPLETED }, work.associate { it.id to Feedback(actualReps = reps) })
        return deriveEvidence("s$vid$day", day, p, outcomes, { source.variation(it)?.familyId ?: it },
            { v, t -> source.policyForVariation(v)?.tiers?.firstOrNull { it.index == t }?.minQualifyingBlocks ?: 2 },
            loggedBlockIds = work.map { it.id }.toSet(), effectivePolicyOf = { source.policyForVariation(it) }).single()
    }

    private fun replay(engineCatalog: Catalog, vid: String, evidence: List<SessionEvidence>): VariationProgress =
        ProgressionEngine(engineCatalog).replay(evidence, listOf(UserAction.SelfAssessment(0, vid, 1))).variations.getValue(vid)

    /** Several weeks at levels 1 to 3 with a pending streak at the end: three days at 1, three at 2, two at 3. */
    private fun weeks(source: Catalog, vid: String): List<SessionEvidence> =
        (1..3).map { session(source, vid, 1, it) } + (4..6).map { session(source, vid, 2, it) } + (7..8).map { session(source, vid, 3, it) }

    @Test fun walkingLungeAndRussianTwistReplayIdenticallyUnderTheOldAndTheNewSideMode() {
        for (vid in listOf("walking-lunge", "russian-twist")) {
            assertFalse(vid, oldCatalog.variation(vid)!!.alternating)
            assertTrue(vid, catalog.variation(vid)!!.alternating)
            val before = replay(oldCatalog, vid, weeks(oldCatalog, vid))
            val after = replay(catalog, vid, weeks(catalog, vid))
            assertEquals(vid, before, after)
            // and the old history itself, replayed by the new definitions
            assertEquals(vid, before, replay(catalog, vid, weeks(oldCatalog, vid)))
            assertEquals(vid, 3, after.tier)
            assertEquals(vid, setOf(1, 2), after.achievedTiers)
            assertEquals(vid, 2, after.earnedStars())
            assertEquals(vid, listOf(7, 8), after.streakDays)
        }
    }

    @Test fun walkingLungeCountsWithTheSameGoodStreaksAfterTheChange() {
        val vid = "walking-lunge"
        val old = weeks(oldCatalog, vid)
        val third = session(catalog, vid, 3, 9)   // the third good session at level 3, done after the update
        val result = replay(catalog, vid, old + third)
        assertEquals(4, result.tier)
        assertEquals(setOf(1, 2, 3), result.achievedTiers)
        assertEquals(result, replay(oldCatalog, vid, old + session(oldCatalog, vid, 3, 9)))
    }

    @Test fun shoulderTapTotalsEraHistoryReplaysToTheSameLevelAndStars() {
        val before = replay(oldCatalog, TAP, weeks(oldCatalog, TAP))
        val after = replay(catalog, TAP, weeks(oldCatalog, TAP))
        assertEquals(before, after)
        assertEquals(3, after.tier)
        assertEquals(2, after.earnedStars())
        assertEquals(listOf(7, 8), after.streakDays)   // 2 of 3 good sessions: nothing resets
    }

    @Test fun anOldTotalIsNeverReadAsPerSide() {
        // 9.2 level 2 session (prescribed 12 taps in total) where the owner logged 16 taps per set: that shows level 4 of the totals ladder.
        val old = session(oldCatalog, TAP, 2, 1, reps = 16)
        assertEquals(4, old.demonstratedTier)
        assertEquals(4, replay(oldCatalog, TAP, listOf(old)).tier)
        // replayed after the update it still shows level 4, because the session carries its own policy snapshot (10, 12, 14, 16, 20) ...
        assertEquals(old.effectivePolicySnapshot!!.tiers.map { it.target.value }, OLD_TAP_TOTALS)
        assertEquals(4, replay(catalog, TAP, listOf(old)).tier)
        // ... whereas the same 16 against the per-side ladder (5, 6, 7, 8, 10) would have looked like level 5, a jump the update must not cause
        assertEquals(5, session(catalog, TAP, 2, 1, reps = 16).demonstratedTier)
    }

    @Test fun shoulderTapContinuesFromTheTotalsEraWithoutAJump() {
        val old = weeks(oldCatalog, TAP)
        // two good level-3 sessions in the totals era (14 taps), then the first one after the update: 7 per side is the same 14 in total
        val newEra = session(catalog, TAP, 3, 9)
        assertEquals(listOf(14, 14, 14), plan(oldCatalog, TAP, 3, 9).blocks.map { it.target!!.value })
        assertEquals(listOf(7, 7, 7), plan(catalog, TAP, 3, 9).blocks.map { it.target!!.value })
        val result = replay(catalog, TAP, old + newEra)
        assertEquals(4, result.tier)   // the natural third good session, not more
        assertEquals(setOf(1, 2, 3), result.achievedTiers)
        assertEquals(3, result.earnedStars())
        // a first new-era session at level 1 does not credit level 3 either
        val fresh = replay(catalog, TAP, listOf(session(catalog, TAP, 1, 1, reps = 5)))
        assertEquals(1, fresh.tier)
    }

    companion object {
        const val TAP = "plank-shoulder-tap"
        val OLD_NONE = setOf("walking-lunge", "russian-twist")
        val OLD_TAP_TOTALS = listOf(10, 12, 14, 16, 20)
    }
}
