package app.calisthenics.domain

import app.calisthenics.domain.planner.*
import org.junit.Assert.*
import org.junit.Test

/** A manual level or typed number is for the next workout only and only for the exercise it was set on. */
class LevelOverrideScopeTest {
    private val set = PlanEdits(tierOverrides = mapOf("push" to 3), freeTargets = mapOf("1:push" to 9), levelFiveTargets = mapOf("push" to 20))
        .stampOverride("push", "pushup-knee", 100L)

    @Test fun sameDayKeepsOverrides() = assertEquals(set, set.expireLevelOverrides(100L))

    @Test fun laterDayClearsLevelsAndTargetsButNothingElse() {
        val e = set.copy(swaps = mapOf("push" to "pushup-knee")).expireLevelOverrides(101L)
        assertTrue(e.tierOverrides.isEmpty() && e.freeTargets.isEmpty() && e.levelFiveTargets.isEmpty() && e.overrideVariations.isEmpty())
        assertEquals(mapOf("push" to "pushup-knee"), e.swaps)
    }

    @Test fun anOlderDraftWithoutADayIsCleared() =
        assertTrue(PlanEdits(tierOverrides = mapOf("push" to 3)).expireLevelOverrides(100L).tierOverrides.isEmpty())

    @Test fun recordsTheExerciseItWasSetOn() = assertEquals(mapOf("push" to "pushup-knee"), set.overrideVariations)
}
