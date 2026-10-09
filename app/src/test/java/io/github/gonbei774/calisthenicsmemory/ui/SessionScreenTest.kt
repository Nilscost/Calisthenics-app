package io.github.gonbei774.calisthenicsmemory.ui

import android.app.Application
import android.content.Intent
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import app.calisthenics.domain.equipment.SeedProfiles
import app.calisthenics.domain.model.BlockType
import app.calisthenics.domain.planner.PlanResult
import app.calisthenics.domain.planner.TrainSettings
import app.calisthenics.domain.planner.buildTrainPlan
import app.calisthenics.domain.progression.ProgressSnapshot
import app.calisthenics.domain.routine.StarterRoutine
import app.calisthenics.domain.session.*
import io.github.gonbei774.calisthenicsmemory.session.SessionBus
import io.github.gonbei774.calisthenicsmemory.session.WorkoutSessionService
import io.github.gonbei774.calisthenicsmemory.ui.session.SessionScreen
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalisthenicsMemoryTheme
import io.github.gonbei774.calisthenicsmemory.ui.train.loadCatalog
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class SessionScreenTest {
    @get:Rule val rule = createComposeRule()
    private val app: Application get() = ApplicationProvider.getApplicationContext()
    private val catalog by lazy { loadCatalog(app) }
    private val plan by lazy {
        val none = ProgressSnapshot(emptyMap(), emptyMap(), emptyMap(), emptyList())
        (buildTrainPlan(catalog, StarterRoutine.routine, none, SeedProfiles.home, TrainSettings(rounds = 2, stretchOn = true)) as PlanResult.Ready).plan.copy(id = "plan-1")
    }

    @Before fun setUp() { SessionBus.clear(); SessionBus.names = catalog.variations.associate { it.id to it.name } }
    @After fun tearDown() { SessionBus.clear() }

    /** The session at the start of block [index], every earlier block completed (real reducer, no shortcuts). */
    private fun stateAt(index: Int): SessionState {
        var s = reduce(newSession("S", plan), SessionEvent.Start(0)).state
        var now = 0L
        repeat(index) { now += plan.blocks[it].durationSeconds * 1000L; s = reduce(s, SessionEvent.Tick(now)).state }
        return s
    }

    private fun show(s: SessionState, onExit: () -> Unit = {}) {
        SessionBus.publish(s)
        rule.setContent { CalisthenicsMemoryTheme(darkTheme = false) { SessionScreen(onExit = onExit) } }
        rule.waitForIdle()
    }

    private fun firstRecovery() = plan.blocks.indexOfFirst { it.type == BlockType.STRETCH || it.type == BlockType.PASSIVE_RECOVERY }
    private fun lastIntent(): Intent { val i = shadowOf(app).nextStartedService; assertNotNull("no service command was sent", i); return i }

    @Test fun workBlockShowsNameTargetRingAndDoneButNoLogger() {
        show(stateAt(0))
        rule.onNodeWithTag("session_title").assertTextEquals("Incline Push-Up")
        rule.onNodeWithTag("session_target").assertTextContains("reps", substring = true)
        rule.onNodeWithTag("session_time").assertExists()
        rule.onNodeWithTag("session_done").assertIsDisplayed().assertIsEnabled()
        rule.onNodeWithTag("logger").assertDoesNotExist()
        rule.onNodeWithTag("session_next").assertExists()
    }

    @Test fun afterTheWorkTheRecoveryIsTheStretchWithTheLoggerOnTop() {
        val i = firstRecovery()
        assertEquals(BlockType.STRETCH, plan.blocks[i].type) // stretch is the recovery
        show(stateAt(i))
        rule.onNodeWithTag("session_title").assertTextContains("Stretch:", substring = true)
        rule.onNodeWithTag("logger").assertIsDisplayed()
        rule.onNodeWithTag("logger_name").assertTextEquals("Incline Push-Up")
        rule.onNodeWithTag("logger_value").assertTextEquals(plan.blocks[0].target!!.value.toString()) // prefilled with the target
        rule.onNodeWithTag("session_done").assertDoesNotExist()
    }

    @Test fun typingRepsSendsALogCommandForTheWorkBlock() {
        val i = firstRecovery()
        show(stateAt(i))
        val target = plan.blocks[0].target!!.value
        rule.onNodeWithTag("logger_minus").performClick()
        rule.waitForIdle()
        val intent = lastIntent()
        assertEquals(WorkoutSessionService.ACTION_LOG, intent.action)
        assertEquals(plan.blocks[0].id, intent.getStringExtra(WorkoutSessionService.EXTRA_BLOCK_ID))
        assertEquals(target - 1, intent.getIntExtra(WorkoutSessionService.EXTRA_REPS, -2))
        rule.onNodeWithTag("logger_value").assertTextEquals((target - 1).toString())
    }

    @Test fun tooHardAndPainAreOneTapAndKeepTheTypedState() {
        show(stateAt(firstRecovery()))
        rule.onNodeWithTag("logger_too_hard").performClick(); rule.waitForIdle()
        var intent = lastIntent()
        assertTrue(intent.getBooleanExtra(WorkoutSessionService.EXTRA_TOO_HARD, false))
        assertEquals(-1, intent.getIntExtra(WorkoutSessionService.EXTRA_REPS, -2)) // reps untouched = not typed = as planned
        rule.onNodeWithTag("logger_pain").performClick(); rule.waitForIdle()
        intent = lastIntent()
        assertTrue(intent.getBooleanExtra(WorkoutSessionService.EXTRA_PAIN, false))
        assertTrue(intent.getBooleanExtra(WorkoutSessionService.EXTRA_TOO_HARD, false))
    }

    @Test fun tooEasyIsExclusiveWithTooHardAndSendsItsOwnFlag() {
        show(stateAt(firstRecovery()))
        rule.onNodeWithTag("logger_too_hard").performClick(); rule.waitForIdle()
        assertTrue(lastIntent().getBooleanExtra(WorkoutSessionService.EXTRA_TOO_HARD, false))
        rule.onNodeWithTag("logger_too_easy").performClick(); rule.waitForIdle()
        val intent = lastIntent()
        assertTrue(intent.getBooleanExtra(WorkoutSessionService.EXTRA_TOO_EASY, false))
        assertFalse(intent.getBooleanExtra(WorkoutSessionService.EXTRA_TOO_HARD, true))
    }

    @Test fun loggerUsesTheWorkScreenLayoutPlusMinusAndDone() {
        show(stateAt(firstRecovery()))
        val target = plan.blocks[0].target!!.value
        rule.onNodeWithTag("logger_plus").performClick(); rule.waitForIdle()
        assertEquals(target + 1, lastIntent().getIntExtra(WorkoutSessionService.EXTRA_REPS, -2))
        rule.onNodeWithTag("logger_done").assertIsDisplayed().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("logger_summary").assertIsDisplayed() // folded into one line, the stretch timer is untouched
        rule.onNodeWithTag("logger_plus").assertDoesNotExist()
        rule.onNodeWithTag("logger_change").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("logger_value").assertTextEquals((target + 1).toString())
    }

    @Test fun loggedValuesComeBackFromTheSessionState() {
        val s0 = stateAt(firstRecovery())
        val s = reduce(s0, SessionEvent.LogBlock(plan.blocks[0].id, BlockLog(reps = 4, tooHard = true))).state
        show(s)
        rule.onNodeWithTag("logger_value").assertTextEquals("4")
    }

    @Test fun getReadyBlocksNameTheNextExerciseAndNothingIsEverCalledRestWithStretchOn() {
        val sawGetReady = mutableListOf<String>()
        for (i in plan.blocks.indices) {
            rule.runOnIdle { SessionBus.publish(stateAt(i)) }
            if (i == 0) rule.setContent { CalisthenicsMemoryTheme(darkTheme = false) { SessionScreen(onExit = {}) } }
            rule.waitForIdle()
            val title = rule.onNodeWithTag("session_title").fetchSemanticsNode().config.first { it.key.name == "Text" }.value.let { (it as List<*>).first().toString() }
            assertFalse("block $i shows '$title'", title.contains("rest", ignoreCase = true))
            if (plan.blocks[i].type == BlockType.TRANSITION) { assertTrue(title.startsWith("Get ready: ")); sawGetReady += title }
            rule.onAllNodesWithText("Rest", substring = true, ignoreCase = true).assertCountEquals(0)
        }
        assertTrue("the routine has a position change", sawGetReady.isNotEmpty())
    }

    @Test fun pauseShowsResumeAndThePausedLabel() {
        val s = reduce(stateAt(0), SessionEvent.Pause(5_000)).state
        show(s)
        rule.onNodeWithTag("session_resume").assertIsDisplayed()
        rule.onNodeWithTag("session_pause").assertDoesNotExist()
        rule.onNodeWithTag("session_paused").assertIsDisplayed()
        rule.onNodeWithTag("session_done").assertIsNotEnabled()
    }

    @Test fun doneSkipAndPauseSendTheirCommands() {
        show(stateAt(0))
        rule.onNodeWithTag("session_done").performClick(); rule.waitForIdle()
        assertEquals(WorkoutSessionService.ACTION_DONE, lastIntent().action)
        rule.onNodeWithTag("session_skip").performClick(); rule.waitForIdle()
        assertEquals(WorkoutSessionService.ACTION_SKIP, lastIntent().action)
        rule.onNodeWithTag("session_pause").performClick(); rule.waitForIdle()
        assertEquals(WorkoutSessionService.ACTION_PAUSE, lastIntent().action)
    }

    @Test fun endingTheWorkoutNeedsAConfirmation() {
        show(stateAt(0))
        rule.onNodeWithTag("session_more").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("session_end").performSemanticsAction(SemanticsActions.OnClick); rule.waitForIdle()
        assertNull("nothing is sent before the confirmation", shadowOf(app).peekNextStartedService())
        rule.onNodeWithTag("session_end_confirm").performSemanticsAction(SemanticsActions.OnClick); rule.waitForIdle()
        assertEquals(WorkoutSessionService.ACTION_FINISH, lastIntent().action)
    }

    @Test fun theEndScreenIsAShortSummaryWithAnEditLinkInsteadOfALongForm() {
        val s = reduce(stateAt(plan.blocks.size - 1), SessionEvent.Tick(10_000_000)).state
        assertEquals(Phase.COMPLETED, s.phase)
        SessionBus.saved = true
        var exited = false
        show(s) { exited = true }
        rule.onNodeWithTag("end_title").assertTextEquals("Workout complete")
        rule.onNodeWithTag("end_summary").assertTextContains("2 rounds", substring = true)
        rule.onNodeWithTag("end_edit").assertIsDisplayed()
        rule.onNodeWithText("How did it go?", substring = true).assertDoesNotExist() // the long form only opens on demand
        rule.onNodeWithTag("end_done").performClick()
        assertTrue(exited)
    }

    @Test fun theEndScreenEditorHasOneNumberPerRoundAndTooEasyNextToTooHard() {
        val s = reduce(stateAt(plan.blocks.size - 1), SessionEvent.Tick(10_000_000)).state
        SessionBus.saved = true
        show(s)
        rule.onNodeWithTag("end_edit").performClick(); rule.waitForIdle()
        val vid = plan.blocks.first { it.type == BlockType.WORK }.variationId!!
        val rounds = plan.blocks.filter { it.type == BlockType.WORK && it.variationId == vid }.size
        rule.onNodeWithTag("end_fix_$vid").assertExists()
        for (i in 1..rounds) rule.onNodeWithTag("end_${vid}_round${i}_value").performScrollTo().assertExists()
        rule.onNodeWithTag("end_${vid}_too_hard").performScrollTo().assertExists()
        rule.onNodeWithTag("end_${vid}_too_easy").performScrollTo().assertExists()
        rule.onNodeWithTag("end_${vid}_pain").performScrollTo().assertExists()
    }
}
