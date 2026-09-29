package app.calisthenics.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Progression fixtures — approved policy (ADR 0002 §C3, spec §5),
 * scenario IDs G3-P* (docs/04-verification.md "Required numeric progression
 * fixtures"). Enrolled at tier 2 on day 0; exposures are qualifying per C2.
 */
class ProgressionTest {

    private val policy = ProgressionPolicy(
        variationId = "pushup-standard",
        version = 1,
        tiers = (1..5).map { Tier(index = it, target = 10 * it, workWindowSeconds = 40, minRecoverySeconds = 20) },
    )

    private fun input(
        current: Int = 2,
        enrolledAt: Int = 0,
        now: Int,
        days: List<Int>,
        lastAuto: Int? = null,
    ) = AdvancementInput(
        policy = policy,
        currentTier = current,
        enrolledAtDay = enrolledAt,
        nowDay = now,
        exposures = days.map { QualifyingExposure(policy.variationId, it) },
        lastAutoAdvancementDay = lastAuto,
    )

    @Test
    fun `G3-P01 three qualifying exposures on days 0 3 7 advance by one tier`() {
        val r = evaluateAdvancement(input(now = 7, days = listOf(0, 3, 7)))
        assertTrue("expected Advance, got $r", r is AdvancementResult.Advance)
        r as AdvancementResult.Advance
        assertEquals(2, r.fromTier)
        assertEquals(3, r.toTier)
        assertEquals(listOf(0, 3, 7), r.evidenceDays)
    }

    @Test
    fun `G3-P02 days 0 2 6 do not span seven days and do not advance`() {
        val r = evaluateAdvancement(input(now = 6, days = listOf(0, 2, 6)))
        assertEquals(AdvancementResult.NoChange, r)
    }

    @Test
    fun `G3-P03 extra rounds or same-day sessions do not accelerate`() {
        // 4 exposures but only 3 distinct days spanning 7 -> still one advance, no more.
        val r = evaluateAdvancement(input(now = 7, days = listOf(0, 0, 3, 7)))
        assertTrue(r is AdvancementResult.Advance)
        // A second advancement immediately after is refused (rolling 7-day cap).
        val r2 = evaluateAdvancement(
            AdvancementInput(
                policy = policy,
                currentTier = 3,
                enrolledAtDay = 7, // enrolled tier 3 on day 7 (the advancement day)
                nowDay = 8,
                exposures = listOf(
                    QualifyingExposure(policy.variationId, 7),
                    QualifyingExposure(policy.variationId, 7),
                    QualifyingExposure(policy.variationId, 8),
                ),
                lastAutoAdvancementDay = 7,
            )
        )
        assertEquals(AdvancementResult.NoChange, r2)
    }

    @Test
    fun `G3-P04 fewer than three exposures do not advance`() {
        assertEquals(AdvancementResult.NoChange, evaluateAdvancement(input(now = 10, days = listOf(0, 7))))
    }

    @Test
    fun `G3-P05 at the top tier there is no automatic advancement`() {
        assertEquals(AdvancementResult.NoChange, evaluateAdvancement(input(current = 5, enrolledAt = 0, now = 30, days = listOf(0, 10, 20))))
    }

    @Test
    fun `G3-P06 another advancement within seven days of the previous one is refused`() {
        val r = evaluateAdvancement(
            AdvancementInput(
                policy = policy,
                currentTier = 3,
                enrolledAtDay = 7,
                nowDay = 13,
                exposures = listOf(
                    QualifyingExposure(policy.variationId, 8),
                    QualifyingExposure(policy.variationId, 10),
                    QualifyingExposure(policy.variationId, 13),
                ),
                lastAutoAdvancementDay = 7,
            )
        )
        assertEquals(AdvancementResult.NoChange, r)
    }

    @Test
    fun `G3-P07 first advancement needs seven days at the tier`() {
        // Enrolled tier 2 only 5 days ago; even 3 qualifying exposures spanning 7 -> no.
        assertEquals(AdvancementResult.NoChange, evaluateAdvancement(input(enrolledAt = 3, now = 8, days = listOf(0, 3, 8))))
    }

    @Test
    fun `policy must have exactly five ordered tiers`() {
        val e = runCatching {
            ProgressionPolicy("x", 1, (1..4).map { Tier(it, it, 40, 20) })
        }.exceptionOrNull()
        assertTrue(e is IllegalArgumentException)
    }
}
