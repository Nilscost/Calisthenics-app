package app.calisthenics.domain

import app.calisthenics.domain.feedback.SessionEvidence
import app.calisthenics.domain.model.Preferences
import app.calisthenics.domain.progression.*
import org.junit.Assert.*
import org.junit.Test

/** V14 / D4: the global "Automatic progression" switch. */
class AutoProgressionTest {
    private val engine = ProgressionEngine(Fx.catalog())
    private val V = "pushup-knee"
    private fun ev(day: Int, tier: Int = 2) = SessionEvidence("s$day", V, "pushup", day, tier, 2, false, false, false, null, false, 2, 0, 2)
    private val start = UserAction.SelfAssessment(0, V, 2)
    private val sessions = listOf(ev(0), ev(3), ev(7))

    @Test fun onIsTheDefaultAndAdvancesAsBefore() {
        assertTrue(Preferences().autoProgression)
        assertEquals(3, engine.replay(sessions, listOf(start)).tierFor(V))
        assertEquals(3, engine.replay(sessions, listOf(start), autoProgression = true).tierFor(V))
    }

    @Test fun offFreezesTheLevelEvenAfterManyQualifyingSessions() {
        val s = engine.replay(sessions + listOf(ev(10), ev(14), ev(21)), listOf(start), autoProgression = false)
        assertEquals(2, s.tierFor(V)); assertTrue(s.variations.getValue(V).achievedTiers.isEmpty())
        assertEquals(setOf(V), s.activeInFamily.values.toSet())          // no successor either
        assertEquals(21, s.variations.getValue(V).lastTrainedDay)         // the sessions still count as trained
    }

    @Test fun offNeverMovesToTheSuccessorAtTheTopLevel() {
        val top = UserAction.SelfAssessment(0, V, 5)
        val s = engine.replay(listOf(ev(0, 5), ev(3, 5), ev(7, 5)), listOf(top), autoProgression = false)
        assertEquals(V, s.activeInFamily["pushup"]); assertTrue(s.events.none { it.message.contains("Moving on") })
        val on = engine.replay(listOf(ev(0, 5), ev(3, 5), ev(7, 5)), listOf(top), autoProgression = true)
        assertEquals("pushup-std", on.activeInFamily["pushup"])
    }

    @Test fun theOwnersManualChangesStillWorkWhenOff() {
        val s = engine.replay(sessions, listOf(start, UserAction.ManualOverride(8, V, 4)), autoProgression = false)
        assertEquals(4, s.tierFor(V))
    }

    @Test fun anOldBackupWithoutTheFieldStillDecodes() {
        val j = kotlinx.serialization.json.Json { ignoreUnknownKeys = false }
        assertTrue(j.decodeFromString(Preferences.serializer(), """{"defaultDurationSeconds":2700}""").autoProgression)
    }
}
