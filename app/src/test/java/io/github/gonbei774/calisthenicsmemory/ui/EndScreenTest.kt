package io.github.gonbei774.calisthenicsmemory.ui

import android.app.Application
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import app.calisthenics.domain.equipment.SeedProfiles
import app.calisthenics.domain.model.BlockType
import app.calisthenics.domain.model.WorkoutFormat
import app.calisthenics.domain.planner.PlanResult
import app.calisthenics.domain.planner.TrainSettings
import app.calisthenics.domain.planner.buildTrainPlan
import app.calisthenics.domain.progression.ProgressSnapshot
import app.calisthenics.domain.routine.StarterRoutine
import app.calisthenics.domain.session.*
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.session.SessionBus
import io.github.gonbei774.calisthenicsmemory.ui.session.EndProgress
import io.github.gonbei774.calisthenicsmemory.ui.session.EndScreen
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalisthenicsMemoryTheme
import io.github.gonbei774.calisthenicsmemory.ui.train.loadCatalog
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class EndScreenTest {
    @get:Rule val rule = createComposeRule()
    private val app: Application get() = ApplicationProvider.getApplicationContext()
    private val catalog by lazy { loadCatalog(app) }
    private val plan by lazy {
        val none = ProgressSnapshot(emptyMap(), emptyMap(), emptyMap(), emptyList())
        (buildTrainPlan(catalog, StarterRoutine.routine, none, SeedProfiles.home, TrainSettings(rounds = 2, stretchOn = true)) as PlanResult.Ready).plan.copy(id = "plan-1")
    }
    private val vids by lazy { plan.blocks.filter { it.type == BlockType.WORK }.mapNotNull { it.variationId }.distinct() }

    @Before fun setUp() { SessionBus.clear(); SessionBus.names = catalog.variations.associate { it.id to it.name } }
    @After fun tearDown() { SessionBus.clear() }

    private fun finished(p: app.calisthenics.domain.model.WorkoutPlan = plan): SessionState {
        var s = reduce(newSession("S", p), SessionEvent.Start(0)).state
        s = reduce(s, SessionEvent.Tick(10_000_000)).state
        assertEquals(Phase.COMPLETED, s.phase)
        return s
    }

    private fun show(s: SessionState, progress: EndProgress? = null, loadFails: Boolean = false, onExit: () -> Unit = {}) {
        SessionBus.publish(s)
        rule.setContent { CalisthenicsMemoryTheme(darkTheme = false) {
            EndScreen(s, Modifier, onExit, loadProgress = { if (loadFails) error("no history") else progress })
        } }
        rule.waitForIdle()
    }

    private fun inside(tag: String, text: String, ignoreCase: Boolean = false) =
        rule.onNodeWithTag(tag).assert(hasAnyDescendant(hasText(text, substring = true, ignoreCase = ignoreCase)))

    private fun progressWith(vararg cards: ExerciseProgress, newExercises: List<NewExercise> = emptyList()) =
        EndProgress(SessionProgressSummary(cards.toList(), newExercises), SessionBus.names)

    @Test fun theHeaderHasThreeTilesWithTheSameNumbersAsHistory() {
        SessionBus.saved = true
        show(finished())
        rule.onNodeWithTag("end_title").assertTextEquals("Workout complete")
        rule.onNodeWithTag("end_tile_rounds").assertExists()
        rule.onNodeWithTag("end_tile_minutes").assertExists()
        rule.onNodeWithTag("end_tile_exercises").assertExists()
        rule.onNodeWithTag("end_tile_rounds").assertTextContains("2")
        rule.onNodeWithTag("end_tile_rounds").assertTextContains("ROUNDS")
        rule.onNodeWithTag("end_save_status").assertTextEquals(app.getString(R.string.end_saved))
    }

    @Test fun aStraightSetsWorkoutSaysSetsNotRounds() {
        SessionBus.saved = true
        show(finished(plan.copy(format = WorkoutFormat.STRAIGHT)))
        rule.onNodeWithTag("end_tile_rounds").assertTextContains("SETS", substring = true)
    }

    @Test fun savingThenFailedKeepsTheSaveStatusAndHidesCardsAndEditor() {
        SessionBus.saving()
        show(finished(), progressWith(ExerciseProgress(vids[0], true, tier = 2, stars = 1)))
        rule.onNodeWithTag("end_save_status").assertTextEquals(app.getString(R.string.end_saving))
        rule.onNodeWithTag("end_card_${vids[0]}").assertDoesNotExist()
        SessionBus.saveFailed(); rule.waitForIdle()
        rule.onNodeWithTag("end_save_status").assertTextEquals(app.getString(R.string.end_save_failed))
        rule.onNodeWithTag("end_edit").assertDoesNotExist()
    }

    @Test fun oneCardPerExerciseShowsStarsLevelAndProgressDots() {
        SessionBus.saved = true
        val a = vids[0]
        show(finished(), progressWith(ExerciseProgress(a, true, tier = 3, stars = 2, starsBefore = 2, progress = LevelProgress(2, 3, false, 4))))
        rule.onNodeWithTag("end_card_$a").assertExists()
        inside("end_card_$a", "Level 3 of 5")
        rule.onNodeWithTag("end_progress_$a").assertTextContains("2 of 3 good sessions to Level 4")
        rule.onNodeWithTag("end_card_$a").assert(hasAnyDescendant(hasContentDescription("2 of 5 stars", substring = true)))
        rule.onNodeWithTag("end_unlocked_caption").assertDoesNotExist()   // nothing unlocked: no section
    }

    @Test fun theWeeklyPathHasItsOwnWording() {
        SessionBus.saved = true
        val a = vids[0]
        show(finished(), progressWith(ExerciseProgress(a, true, tier = 3, stars = 2, progress = LevelProgress(3, 4, true, 4))))
        rule.onNodeWithTag("end_progress_$a").assertTextContains("3 of 4 weekly sessions to Level 4")
    }

    @Test fun unlockedSectionShowsLevelUpsMasteryAndNewExercises() {
        SessionBus.saved = true
        val a = vids[0]; val b = vids[1]; val n = catalog.variations.first { it.id !in vids }.id
        show(finished(), progressWith(
            ExerciseProgress(a, true, tier = 4, stars = 3, starsBefore = 2, leveledUpTo = 4, progress = LevelProgress(0, 3, false, 5)),
            ExerciseProgress(b, true, tier = 5, stars = 5, starsBefore = 4, mastered = true, masteredAlready = true),
            newExercises = listOf(NewExercise(n, b))))
        rule.onNodeWithTag("end_unlocked_caption").assertExists()
        inside("end_unlock_$a", "Level 4 unlocked")
        inside("end_unlock_$b", "Mastered")
        inside("end_unlock_$n", SessionBus.names.getValue(n))
        inside("end_unlock_$n", "New exercise unlocked", ignoreCase = true)
        inside("end_card_$b", "Mastered")
        rule.onNodeWithTag("end_progress_$b").assertDoesNotExist()
    }

    @Test fun chipsSayEasierNextTimeTheNextTargetAndLevelKept() {
        SessionBus.saved = true
        val a = vids[0]; val b = vids[1]
        show(finished(), progressWith(
            ExerciseProgress(a, true, tier = 2, stars = 1, easierNextTime = true),
            ExerciseProgress(b, true, tier = 2, stars = 1, nextTarget = 5, levelKept = true)))
        rule.onNodeWithTag("end_chip_${a}_easier").assertTextEquals("Easier next time")
        rule.onNodeWithTag("end_chip_${b}_target").assertTextEquals("Next target: 5 reps")
        rule.onNodeWithTag("end_chip_${b}_kept").assertTextEquals("Level kept")
    }

    @Test fun exercisesWithoutLevelsAreAPlainRow() {
        SessionBus.saved = true
        val a = vids[0]
        show(finished(), progressWith(ExerciseProgress(a, false)))
        rule.onNodeWithTag("end_plain_$a").assertExists()
        rule.onNodeWithTag("end_card_$a").assertDoesNotExist()
    }

    @Test fun whenHistoryCannotBeReadTheHeaderEditorAndDoneStillWork() {
        SessionBus.saved = true
        var exited = false
        show(finished(), loadFails = true) { exited = true }
        rule.onNodeWithTag("end_tile_rounds").assertExists()
        rule.onNodeWithTag("end_card_${vids[0]}").assertDoesNotExist()
        rule.onNodeWithTag("end_edit").assertIsDisplayed()
        rule.onNodeWithTag("end_done").performClick()
        assertTrue(exited)
    }

    @Test fun theEditorStillOpensAfterTheCards() {
        SessionBus.saved = true
        val a = vids[0]
        show(finished(), progressWith(ExerciseProgress(a, true, tier = 2, stars = 1)))
        rule.onNodeWithTag("end_edit").performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("end_fix_$a").performScrollTo().assertExists()
    }
}
