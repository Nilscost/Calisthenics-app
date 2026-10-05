package app.calisthenics.domain

import app.calisthenics.domain.intake.*
import app.calisthenics.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class IntakeTest {
    private fun t(i: Int, v: Int) = Tier(i, Target(TargetType.REPS, v), 60, 60)
    private val tiers = listOf(t(1, 6), t(2, 8), t(3, 10), t(4, 12), t(5, 15))
    @Test fun below_first_gives_step_1() = assertEquals(1, suggestStartingStep(tiers, 3))
    @Test fun exact_match() = assertEquals(4, suggestStartingStep(tiers, 12))
    @Test fun between_rounds_down() = assertEquals(3, suggestStartingStep(tiers, 11))
    @Test fun above_top_caps_and_flags() { assertEquals(5, suggestStartingStep(tiers, 40)); assertTrue(exceedsLadder(tiers, 40)); assertFalse(exceedsLadder(tiers, 15)) }
}
