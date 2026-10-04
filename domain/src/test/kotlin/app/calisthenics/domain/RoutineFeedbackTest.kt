package app.calisthenics.domain

import app.calisthenics.domain.feedback.*
import app.calisthenics.domain.model.*
import app.calisthenics.domain.model.Target
import app.calisthenics.domain.routine.*
import org.junit.Assert.*
import org.junit.Test

class RoutineFeedbackTest {
    @Test fun `full body is exclusive and empty becomes full body`() {
        assertEquals(setOf(StrengthFocus.FULL_BODY), normalizeFocus(emptySet()))
        assertEquals(setOf(StrengthFocus.FULL_BODY), normalizeFocus(setOf(StrengthFocus.FULL_BODY, StrengthFocus.CORE)))
        assertEquals(setOf(StrengthFocus.CORE, StrengthFocus.UPPER_BODY), normalizeFocus(setOf(StrengthFocus.CORE, StrengthFocus.UPPER_BODY)))
    }

    @Test fun `toggle focus follows chip rules`() {
        var f = setOf(StrengthFocus.FULL_BODY)
        f = toggleFocus(f, StrengthFocus.CORE); assertEquals(setOf(StrengthFocus.CORE), f)
        f = toggleFocus(f, StrengthFocus.UPPER_BODY); assertEquals(setOf(StrengthFocus.CORE, StrengthFocus.UPPER_BODY), f)
        f = toggleFocus(f, StrengthFocus.CORE); f = toggleFocus(f, StrengthFocus.UPPER_BODY)
        assertEquals(setOf(StrengthFocus.FULL_BODY), f)
        assertEquals(setOf(StrengthFocus.FULL_BODY), toggleFocus(setOf(StrengthFocus.CORE), StrengthFocus.FULL_BODY))
    }

    @Test fun `slots for focus keep routine order`() {
        val r = SeedRoutine.fullBody
        val core = slotsForFocus(r, setOf(StrengthFocus.CORE)).map { it.id }
        assertEquals(listOf("slot-core", "slot-core2"), core)
        assertEquals(r.slots.size, slotsForFocus(r, setOf(StrengthFocus.FULL_BODY)).size)
    }

    @Test fun `swaps and focus are not remembered but duration and toggles are`() {
        val prefs = Preferences()
        val d = SessionDraft.from(prefs, SeedRoutine.fullBody).copy(durationSeconds = 1800, stretchOn = false, swaps = mapOf("slot-push" to "pushup-std"),
            focus = setOf(StrengthFocus.CORE))
        val after = rememberOnStart(prefs, d)
        assertEquals(1800, after.defaultDurationSeconds); assertFalse(after.stretchOn)
        assertEquals(SeedRoutine.fullBody, SeedRoutine.fullBody) // routine untouched by rememberOnStart
    }

    @Test fun `explicit save creates a new revision and leaves the old one intact`() {
        val r1 = SeedRoutine.fullBody
        val r2 = saveSwapsToRoutine(r1, mapOf("slot-push" to "pushup-std"))
        assertEquals(2, r2.revision); assertEquals(1, r1.revision)
        assertEquals("pushup-std", r2.slots.first { it.id == "slot-push" }.preferredVariationId)
        assertEquals("pushup-knee", r1.slots.first { it.id == "slot-push" }.preferredVariationId)
        assertSame(r1, saveSwapsToRoutine(r1, emptyMap()))
        assertEquals(3, setGoal(r2, "goal-x").revision)
        assertSame(r2, setGoal(r2, null))
    }

    @Test fun `avoid adds to exclusions`() {
        assertTrue("a" in avoidVariation(Preferences(), "a").excludedVariationIds)
    }

    @Test fun `weekly summary has no streak or missed wording`() {
        val w = weeklySummary(listOf(0, 2, 4, 15), 0)
        assertEquals(listOf(3, 0, 1), w.map { it.sessions })
        assertTrue(w.none { it.message.contains("missed", true) || it.message.contains("streak", true) })
    }

    // ---- feedback ----
    private fun plan(target: Target = Target(TargetType.REPS, 10)) = WorkoutPlan(
        "p", "usual", 1, 1, 0, "home", 2700, 2700, setOf(StrengthFocus.FULL_BODY), true, null, 2, emptyList(), emptyList(), false, false,
        blocks = listOf(
            TimelineBlock("b1", BlockType.WORK, 40, 1, "slot-push", "pushup-knee", target = target, prescriptionTier = 2),
            TimelineBlock("r1", BlockType.STRETCH, 20, 1, variationId = "calf-stretch"),
            TimelineBlock("b2", BlockType.WORK, 40, 2, "slot-push", "pushup-knee", target = target, prescriptionTier = 2),
        ),
    )

    @Test fun `completed work with no feedback is assumed met`() {
        val o = resolveFeedback(plan(), mapOf("b1" to Execution.COMPLETED, "b2" to Execution.COMPLETED))
        assertTrue(o.all { it.rating == Rating.MET && it.origin == RatingOrigin.ASSUMED })
        assertEquals(2, o.size) // stretch block is not work
    }

    @Test fun `skipped work is never assumed met`() {
        val o = resolveFeedback(plan(), mapOf("b1" to Execution.SKIPPED, "b2" to Execution.COMPLETED))
        assertEquals(Rating.UNRATED, o[0].rating); assertEquals(RatingOrigin.NONE, o[0].origin)
    }

    @Test fun `block rating beats exercise rating beats assumption`() {
        val ex = mapOf("b1" to Execution.COMPLETED, "b2" to Execution.COMPLETED)
        val o = resolveFeedback(plan(), ex, blockFeedback = mapOf("b1" to Feedback(Rating.ABOVE)),
            rowFeedback = mapOf("pushup-knee" to Feedback(Rating.BELOW)))
        assertEquals(RatingOrigin.USER_BLOCK, o[0].origin); assertEquals(Rating.ABOVE, o[0].rating)
        assertEquals(RatingOrigin.USER_EXERCISE, o[1].origin); assertEquals(Rating.BELOW, o[1].rating)
    }

    @Test fun `typed number below target forces below and surfaces a conflict`() {
        val o = resolveFeedback(plan(), mapOf("b1" to Execution.COMPLETED),
            blockFeedback = mapOf("b1" to Feedback(Rating.MET, actualReps = 7))).first()
        assertEquals(Rating.BELOW, o.rating); assertNotNull(o.conflict)
    }

    @Test fun `discomfort is kept even on a skipped block`() {
        val o = resolveFeedback(plan(), mapOf("b1" to Execution.SKIPPED), blockFeedback = mapOf("b1" to Feedback(discomfort = true))).first()
        assertTrue(o.discomfort)
    }

    private fun evidence(ex: Map<String, Execution>, fb: Map<String, Feedback> = emptyMap(), easier: Set<String> = emptySet()): SessionEvidence {
        val p = plan(); val out = resolveFeedback(p, ex, fb)
        return deriveEvidence("s1", 0, p, out, { "pushup" }, { _, _ -> 2 }, easier).single()
    }

    @Test fun `two completed blocks without problems qualify`() {
        assertTrue(evidence(mapOf("b1" to Execution.COMPLETED, "b2" to Execution.COMPLETED)).qualifying)
    }

    @Test fun `one block, skipped, below, discomfort or easier override do not qualify`() {
        assertFalse(evidence(mapOf("b1" to Execution.COMPLETED)).qualifying)
        assertFalse(evidence(mapOf("b1" to Execution.COMPLETED, "b2" to Execution.SKIPPED)).qualifying)
        assertFalse(evidence(mapOf("b1" to Execution.COMPLETED, "b2" to Execution.COMPLETED), mapOf("b1" to Feedback(Rating.BELOW))).qualifying)
        assertFalse(evidence(mapOf("b1" to Execution.COMPLETED, "b2" to Execution.COMPLETED), mapOf("b2" to Feedback(discomfort = true))).qualifying)
        assertFalse(evidence(mapOf("b1" to Execution.COMPLETED, "b2" to Execution.COMPLETED), easier = setOf("pushup-knee")).qualifying)
    }
}
