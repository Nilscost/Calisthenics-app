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
import app.calisthenics.domain.progression.LevelUpBy
import app.calisthenics.domain.progression.ProgressSnapshot
import app.calisthenics.domain.progression.Suggestion
import app.calisthenics.domain.progression.UserAction
import app.calisthenics.domain.routine.StarterRoutine
import app.calisthenics.domain.session.*
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.session.SessionBus
import io.github.gonbei774.calisthenicsmemory.ui.session.EndProgress
import io.github.gonbei774.calisthenicsmemory.ui.session.EndScreen
import io.github.gonbei774.calisthenicsmemory.platform.todayEpochDay
import io.github.gonbei774.calisthenicsmemory.ui.screens.SuggestionStore
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
        rule.onNodeWithTag("end_edit_${vids[0]}").assertDoesNotExist()
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

    @Test fun levelUpsAndMasteryFoldIntoTheExerciseCardAndNewExercisesStayUnlocked() {
        SessionBus.saved = true
        val a = vids[0]; val b = vids[1]; val n = catalog.variations.first { it.id !in vids }.id
        show(finished(), progressWith(
            ExerciseProgress(a, true, tier = 4, stars = 3, starsBefore = 2, leveledUpTo = 4, progress = LevelProgress(3, 3, false, 4, finished = true), levelUpBy = LevelUpBy.STREAK),
            ExerciseProgress(b, true, tier = 5, stars = 5, starsBefore = 4, mastered = true, masteredAlready = true),
            newExercises = listOf(NewExercise(n, b))))
        rule.onNodeWithTag("end_unlocked_caption").assertExists()
        rule.onNodeWithTag("end_unlock_$a").assertTextContains("Level 4 unlocked")
        inside("end_card_$a", "Level 4 unlocked")   // one card per exercise: the level-up lives on the exercise's own card
        rule.onNodeWithTag("end_unlock_$b").assertTextContains("Mastered", substring = true)
        inside("end_unlock_$n", SessionBus.names.getValue(n))
        inside("end_unlock_$n", "New exercise unlocked", ignoreCase = true)
        inside("end_card_$b", "Mastered")
        rule.onNodeWithTag("end_progress_$b").assertDoesNotExist()
    }

    @Test fun aLevelUpAloneHasNoUnlockedSection() {
        SessionBus.saved = true
        val a = vids[0]
        show(finished(), progressWith(ExerciseProgress(a, true, tier = 4, stars = 3, starsBefore = 2, leveledUpTo = 4, progress = LevelProgress(3, 3, false, 4, finished = true), levelUpBy = LevelUpBy.STREAK)))
        rule.onNodeWithTag("end_unlocked_caption").assertDoesNotExist()
        rule.onNodeWithTag("end_card_$a").assert(hasAnyDescendant(hasContentDescription("3 of 5 stars", substring = true)))
        inside("end_card_$a", "Level 4 unlocked")
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
        rule.onNodeWithTag("end_edit_${vids[0]}").assertIsDisplayed()
        rule.onNodeWithTag("end_done").performScrollTo().performClick()
        assertTrue(exited)
    }

    @Test fun theEditorStillOpensAfterTheCards() {
        SessionBus.saved = true
        val a = vids[0]
        show(finished(), progressWith(ExerciseProgress(a, true, tier = 2, stars = 1)))
        rule.onNodeWithTag("end_${a}_too_hard").performScrollTo().assertExists()
        rule.onNodeWithTag("end_${a}_too_easy").assertExists()
        rule.onNodeWithTag("end_${a}_pain").assertExists()
        rule.onNodeWithTag("end_${a}_round1_value").assertDoesNotExist()
        rule.onNodeWithTag("end_edit_$a").performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("end_${a}_round1_value").performScrollTo().assertExists()
        rule.onNodeWithTag("end_fix_$a").assertDoesNotExist()   // one card per exercise: no separate edit card
        rule.onNodeWithTag("end_edit").assertDoesNotExist()
    }

    @Test fun aLevelReachedByThreeGoodSessionsShowsTheFinishedStreakAtTheOldLevel() {
        SessionBus.saved = true
        val a = vids[0]
        show(finished(), progressWith(ExerciseProgress(a, true, tier = 2, stars = 1, starsBefore = 0, leveledUpTo = 2, levelUpBy = LevelUpBy.STREAK,
            progress = LevelProgress(3, 3, false, 2, finished = true))))
        rule.onNodeWithTag("end_progress_$a").assertTextContains("3 of 3 good sessions at Level 1")
        rule.onNodeWithTag("end_demonstrated_$a").assertDoesNotExist()
    }

    @Test fun theWeeklyPathFinishesAsFourOfFour() {
        SessionBus.saved = true
        val a = vids[0]
        show(finished(), progressWith(ExerciseProgress(a, true, tier = 2, stars = 1, leveledUpTo = 2, levelUpBy = LevelUpBy.STREAK,
            progress = LevelProgress(4, 4, true, 2, finished = true))))
        rule.onNodeWithTag("end_progress_$a").assertTextContains("4 of 4 weekly sessions at Level 1")
    }

    @Test fun aLevelReachedByRepsSaysSoAndKeepsTheDotsTowardsTheNextLevel() {
        SessionBus.saved = true
        val a = vids[0]
        show(finished(), progressWith(ExerciseProgress(a, true, tier = 2, stars = 1, leveledUpTo = 2, levelUpBy = LevelUpBy.DEMONSTRATED,
            progress = LevelProgress(1, 3, false, 3))))
        rule.onNodeWithTag("end_demonstrated_$a").assertTextEquals("You did the Level 2 reps today")
        rule.onNodeWithTag("end_progress_$a").assertTextContains("1 of 3 good sessions to Level 3")
    }

    private fun finishedWithRating(tooHard: Boolean = false, tooEasy: Boolean = false): SessionState {
        val s = finished()
        val work = plan.blocks.filter { it.type == BlockType.WORK && it.variationId == vids[0] }
        return s.copy(logged = s.logged + work.associate { it.id to BlockLog(tooHard = tooHard, tooEasy = tooEasy) })
    }

    private val actions get() = SuggestionStore.accepted(app)

    @Before fun clearMoves() { SuggestionStore.clearAll(app) }

    @Test fun tooHardAboveLevelOneOffersGoingDownAndOneTapAppliesItNothingBefore() {
        SessionBus.saved = true
        val a = vids[0]
        show(finishedWithRating(tooHard = true), progressWith(ExerciseProgress(a, true, tier = 3, stars = 2)))
        rule.onNodeWithTag("end_${a}_act").performScrollTo().assertTextEquals("Go down to Level 2")
        assertTrue("nothing changes without the tap", actions.isEmpty())
        rule.onNodeWithTag("end_${a}_act").performClick(); rule.waitForIdle()
        val easier = actions.single() as UserAction.AcceptEasier
        assertEquals(a, easier.variationId); assertEquals(2, easier.tier)
        rule.onNodeWithTag("end_${a}_act").assertDoesNotExist()
        rule.onNodeWithTag("end_${a}_acted").assertTextEquals("Level 2 from next time")
    }

    @Test fun tooHardAtLevelOneOffersNoMoveDown() {
        SessionBus.saved = true
        val a = vids[0]
        show(finishedWithRating(tooHard = true), progressWith(ExerciseProgress(a, true, tier = 1, stars = 0)))
        rule.onNodeWithTag("end_${a}_too_hard").performScrollTo().assertExists()
        rule.onNodeWithTag("end_${a}_act").assertDoesNotExist()
    }

    @Test fun noRatingNoButton() {
        SessionBus.saved = true
        val a = vids[0]
        show(finished(), progressWith(ExerciseProgress(a, true, tier = 3, stars = 2)))
        rule.onNodeWithTag("end_${a}_act").assertDoesNotExist()
    }

    @Test fun tooEasyOffersWhatTheSuggestionAlreadyOffersAndOneTapAppliesIt() {
        SessionBus.saved = true
        val a = vids[0]
        val raise = EndProgress(SessionProgressSummary(listOf(ExerciseProgress(a, true, tier = 2, stars = 1)), emptyList()), SessionBus.names,
            mapOf(a to Suggestion.RaiseLevel(a, 4)))
        show(finishedWithRating(tooEasy = true), raise)
        rule.onNodeWithTag("end_${a}_act").performScrollTo().assertTextEquals("Go up to Level 4")
        assertTrue(actions.isEmpty())
        rule.onNodeWithTag("end_${a}_act").performClick(); rule.waitForIdle()
        val harder = actions.single() as UserAction.AcceptHarder
        assertEquals(4, harder.tier)
        rule.onNodeWithTag("end_${a}_acted").assertTextEquals("Level 4 from next time")
    }

    @Test fun tooEasyAtTheTopOffersTheNextExercise() {
        SessionBus.saved = true
        val a = vids[0]; val n = catalog.variations.first { it.id !in vids }.id
        val next = EndProgress(SessionProgressSummary(listOf(ExerciseProgress(a, true, tier = 5, stars = 4)), emptyList()), SessionBus.names + (n to "Next one"),
            mapOf(a to Suggestion.NextExercise(a, n, 1)))
        show(finishedWithRating(tooEasy = true), next)
        rule.onNodeWithTag("end_${a}_act").performScrollTo().assertTextEquals("Switch to Next one")
        rule.onNodeWithTag("end_${a}_act").performClick(); rule.waitForIdle()
        assertEquals(UserAction.AcceptNextExercise(todayEpochDay().toInt(), a, n), actions.single())
        rule.onNodeWithTag("end_${a}_acted").assertTextEquals("Next one from next time")
    }

    @Test fun tooEasyWithoutAMoveOnOffersNothing() {
        SessionBus.saved = true
        val a = vids[0]
        show(finishedWithRating(tooEasy = true), progressWith(ExerciseProgress(a, true, tier = 5, stars = 5, masteredAlready = true)))
        rule.onNodeWithTag("end_${a}_act").assertDoesNotExist()
    }

    @Test fun aMasteredExerciseShowsItsNextStepUnderADividerWithItsThumb() {
        SessionBus.saved = true
        val a = vids[0]; val n = catalog.variations.first { it.id !in vids }.id
        val p = EndProgress(SessionProgressSummary(listOf(ExerciseProgress(a, true, tier = 5, stars = 5, starsBefore = 4, mastered = true, masteredAlready = true, nextVariationId = n)),
            emptyList()), SessionBus.names)
        show(finished(), p)
        rule.onNodeWithTag("end_next_$a").performScrollTo().assertTextContains("Next: ${SessionBus.names.getValue(n)} · Level 1")
    }

    @Test fun whenTheNextExerciseHasItsOwnCardOnlyItsNameIsShown() {
        SessionBus.saved = true
        val a = vids[0]; val b = vids[1]
        val p = EndProgress(SessionProgressSummary(listOf(
            ExerciseProgress(a, true, tier = 5, stars = 5, masteredAlready = true, nextVariationId = b), ExerciseProgress(b, true, tier = 1, stars = 0)), emptyList()), SessionBus.names)
        show(finished(), p)
        rule.onNodeWithTag("end_next_$a").performScrollTo().assertTextEquals("Next: ${SessionBus.names.getValue(b)}")
    }

    @Test fun anExerciseWithoutAMasteredStepShowsNoNextRow() {
        SessionBus.saved = true
        val a = vids[0]
        show(finished(), progressWith(ExerciseProgress(a, true, tier = 5, stars = 5, masteredAlready = true)))
        rule.onNodeWithTag("end_next_$a").assertDoesNotExist()
    }
}
