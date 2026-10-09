package app.calisthenics.domain

import app.calisthenics.domain.feedback.*
import app.calisthenics.domain.model.*
import app.calisthenics.domain.model.Target
import org.junit.Assert.*
import org.junit.Test

/** V04a: a per-round correction is a new revision; the evidence uses the corrected rounds. */
class RoundCorrectionTest {
    private fun work(round: Int, vid: String = "pushup-standard", target: Target = Target(TargetType.REPS, 8)) =
        TimelineBlock("r$round-w-$vid", BlockType.WORK, 60, round, "s", vid, Side.NONE, target, 3)
    private val blocks = listOf(work(1), work(2), work(3))
    private val plan = WorkoutPlan("p", "r", 1, 1, 0L, "home", 0, 180, emptySet(), false, null, 3, emptyList(), emptyList(), false, false, blocks)
    private val exec = blocks.associate { it.id to Execution.COMPLETED }
    private val achieved = mapOf("r1-w-pushup-standard" to 8, "r2-w-pushup-standard" to 5, "r3-w-pushup-standard" to 8)
    private val original = FeedbackRow("pushup-standard", null, 1, Rating.BELOW, false, 5)
    private fun round(n: Int, rev: Int, reps: Int?, rating: Rating = Rating.MET, pain: Boolean = false) =
        FeedbackRow("pushup-standard", "r$n-w-pushup-standard", rev, rating, pain, reps)
    private fun evidence(rows: List<FeedbackRow>, p: WorkoutPlan = plan, ach: Map<String, Int?> = achieved) =
        deriveEvidence("S", 1, p, resolveFeedback(p, p.blocks.associate { it.id to Execution.COMPLETED }, emptyMap(), effectiveFeedback(p, ach, rows)), { it }, { _, _ -> 2 }).single()

    @Test fun withoutRoundCorrectionsTheLatestExerciseRevisionIsUsedAsBefore() {
        val f = effectiveFeedback(plan, achieved, listOf(original, original.copy(revision = 2, rating = Rating.MET, actualReps = 8)))
        assertEquals(Feedback(Rating.MET, actualReps = 8, revision = 2), f["pushup-standard"])
        assertFalse(evidence(listOf(original)).qualifying)
        assertTrue(evidence(listOf(original, original.copy(revision = 2, rating = Rating.MET, actualReps = 8))).qualifying)
    }

    @Test fun correctingTheLowRoundMakesTheSessionQualify() {
        val rows = listOf(original, round(1, 2, 8), round(2, 3, 8), round(3, 4, 8))
        val f = effectiveFeedback(plan, achieved, rows).getValue("pushup-standard")
        assertEquals(8, f.actualReps); assertEquals(Rating.MET, f.rating); assertEquals(4, f.revision)
        assertTrue(evidence(rows).qualifying)
    }

    @Test fun correctingOnlyRoundTwoLeavesTheOthersOnTheirLoggedValues() {
        val rows = listOf(original, round(2, 2, 8)) // rounds 1 and 3 keep their logged 8
        assertTrue(evidence(rows).qualifying)
        val lowered = listOf(original, round(2, 2, 8), round(1, 3, 6, Rating.BELOW))
        assertFalse(evidence(lowered).qualifying)
        assertEquals(6, effectiveFeedback(plan, achieved, lowered).getValue("pushup-standard").actualReps)
    }

    @Test fun theLatestRevisionOfARoundWins() {
        val rows = listOf(original, round(2, 2, 3, Rating.BELOW), round(2, 3, 9, Rating.MET))
        assertEquals(8, effectiveFeedback(plan, achieved, rows).getValue("pushup-standard").actualReps) // r1=8, r2=9, r3=8
    }

    @Test fun tooEasyInOneRoundGivesAboveAndStillQualifies() {
        val rows = listOf(original, round(1, 2, 8), round(2, 3, 8, Rating.ABOVE), round(3, 4, 8))
        val e = evidence(rows)
        assertTrue(e.anyAbove); assertTrue(e.qualifying)
    }

    @Test fun painInACorrectedRoundCountsAndAClearedPainDoesNot() {
        val withPain = listOf(original.copy(discomfort = true), round(1, 2, 8), round(2, 3, 8, pain = true), round(3, 4, 8))
        assertTrue(effectiveFeedback(plan, achieved, withPain).getValue("pushup-standard").discomfort)
        val cleared = listOf(original.copy(discomfort = true), round(1, 2, 8), round(2, 3, 8), round(3, 4, 8))
        assertFalse(effectiveFeedback(plan, achieved, cleared).getValue("pushup-standard").discomfort)
    }

    @Test fun holdsUseSecondsAndFallBackToRepsForOldRows() {
        val t = Target(TargetType.HOLD_SECONDS, 30)
        val hb = listOf(work(1, "plank", t), work(2, "plank", t))
        val hp = plan.copy(blocks = hb)
        val ach = mapOf("r1-w-plank" to 30, "r2-w-plank" to 22)
        val old = FeedbackRow("plank", null, 1, Rating.BELOW, false, actualReps = 22) // before V04a, seconds were stored as reps
        assertEquals(22, effectiveFeedback(hp, ach, listOf(old)).getValue("plank").actualHoldSeconds)
        val fixed = listOf(old, FeedbackRow("plank", "r2-w-plank", 2, Rating.MET, false, null, actualHoldSeconds = 30), FeedbackRow("plank", "r1-w-plank", 3, Rating.MET, false, null, actualHoldSeconds = 30))
        val f = effectiveFeedback(hp, ach, fixed).getValue("plank")
        assertEquals(30, f.actualHoldSeconds); assertEquals(Rating.MET, f.rating); assertNull(f.actualReps)
    }

    @Test fun backupRecordsFromBeforeV04aStillDecode() {
        val j = kotlinx.serialization.json.Json { ignoreUnknownKeys = false }
        val old = j.decodeFromString(app.calisthenics.domain.backup.FeedbackRecord.serializer(),
            """{"sessionId":"s","variationId":"v","revision":1,"rating":"MET","discomfort":false,"assumedMet":true,"createdAtEpochMs":1}""")
        assertNull(old.blockId); assertNull(old.actualHoldSeconds); assertNull(old.actualReps)
    }
}
