package io.github.gonbei774.calisthenicsmemory.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.calisthenics.domain.model.*
import app.calisthenics.domain.model.Target
import io.github.gonbei774.calisthenicsmemory.data.*
import io.github.gonbei774.calisthenicsmemory.ui.screens.*
import io.github.gonbei774.calisthenicsmemory.ui.train.loadCatalog
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId

/** W2 (owner): redoing the questionnaire restarts the levels from the new answers; the history stays but its evidence no longer counts. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class QuestionnaireResetTest {
    private val ctx: Context get() = ApplicationProvider.getApplicationContext()
    private fun ms(day: Int) = LocalDate.of(2026, 9, 1).plusDays(day.toLong()).atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Before fun clean() {
        for (n in listOf("levels", "suggestions", "planner_prefs", "onboarding")) ctx.getSharedPreferences(n, Context.MODE_PRIVATE).edit().clear().commit()
        runBlocking(kotlinx.coroutines.Dispatchers.IO) { AppDatabase.getDatabase(ctx).clearAllTables() }
    }

    private fun addSession(i: Int, day: Int, tier: Int) = runBlocking {
        val dao = AppDatabase.getDatabase(ctx).historyDao()
        val blocks = (1..3).map { r -> TimelineBlock("r$r-push-work", BlockType.WORK, 60, r, "push", "pushup-standard", target = Target(TargetType.REPS, 5 * tier), prescriptionTier = tier) }
        val plan = WorkoutPlan("p$i", "r", 1, 1, ms(day), "home", 600, 180, setOf(StrengthFocus.FULL_BODY), true, null, 3, emptyList(), emptyList(), false, false, blocks)
        dao.insertPlan(PlanSnapshotEntity("p$i", ms(day), "r", 1, 1, "home", Json.encodeToString(WorkoutPlan.serializer(), plan)))
        dao.insertSession(WorkoutSessionEntity("s$i", "p$i", ms(day), ms(day) + 600_000, "COMPLETED"))
        dao.insertBlockResults(blocks.map { BlockResultEntity(sessionId = "s$i", blockId = it.id, outcome = "MET", actualSeconds = 60, achievedValue = null) })
    }

    @Test fun historyBeforeTheResetNoLongerRaisesLevelsButLaterSessionsDo() {
        val catalog = loadCatalog(ctx)
        LevelStore.save(ctx, mapOf("pushup-standard" to 2), undated = true)
        // three qualifying level-2 sessions: the old history carried this exercise up
        listOf(0, 3, 6).forEachIndexed { i, d -> addSession(i, d, 2) }
        val before = runBlocking { ProgressLoader.load(ctx, catalog) }
        assertEquals(3, before.tierFor("pushup-standard"))

        // the questionnaire is redone with a lower answer (level 1): the new answer wins
        LevelStore.save(ctx, mapOf("pushup-standard" to 1), today = LocalDate.of(2026, 9, 11).toEpochDay().toInt(), restart = true); LevelStore.markReset(ctx, ms(10))
        val after = runBlocking { ProgressLoader.load(ctx, catalog) }
        assertEquals(1, after.tierFor("pushup-standard"))
        assertTrue(after.variations.getValue("pushup-standard").achievedTiers.isEmpty())
        assertTrue(after.variations.getValue("pushup-standard").streakDays.isEmpty())
        assertEquals(0, runBlocking { AppDatabase.getDatabase(ctx).historyDao().sessions() }.count { it.startedAtEpochMs < ms(10) } - 3)   // History is untouched

        // sessions after the reset count again, from the new level
        listOf(11, 13, 15).forEachIndexed { i, d -> addSession(10 + i, d, 1) }
        assertEquals(2, runBlocking { ProgressLoader.load(ctx, catalog) }.tierFor("pushup-standard"))
    }

    @Test fun savingTheQuestionnaireRecordsTheResetAndClearsOldAcceptedRaises() {
        SuggestionStore.accept(ctx, "pushup-standard", 4, 3)
        assertEquals(0L, LevelStore.resetAtMs(ctx))
        io.github.gonbei774.calisthenicsmemory.ui.onboarding.OnboardingSave.save(ctx, loadCatalog(ctx),
            io.github.gonbei774.calisthenicsmemory.ui.onboarding.OnboardingResult("general", app.calisthenics.domain.equipment.SeedProfiles.home, emptyList(), null, false, 3, true), nowMs = 123_456L)
        assertEquals(123_456L, LevelStore.resetAtMs(ctx))
        assertTrue(SuggestionStore.accepted(ctx).isEmpty())
    }

    @Test fun handEditedLevelsAreDatedAndUnchangedOnesKeepTheirDay() {
        LevelStore.save(ctx, mapOf("a" to 2, "b" to 3), today = 100)
        LevelStore.save(ctx, mapOf("a" to 2, "b" to 1), today = 120)
        assertEquals(mapOf("a" to 100, "b" to 120), LevelStore.days(ctx))
        assertEquals(listOf(100, 120), LevelStore.actions(ctx).sortedBy { (it as app.calisthenics.domain.progression.UserAction.SelfAssessment).variationId }.map { it.day })
        LevelStore.save(ctx, mapOf("a" to 2, "b" to 1), today = 130, restart = true)
        assertEquals(mapOf("a" to 130, "b" to 130), LevelStore.days(ctx))
    }
}
