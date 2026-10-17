package io.github.gonbei774.calisthenicsmemory.ui

import android.app.Application
import android.content.Intent
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import app.calisthenics.domain.equipment.SeedProfiles
import app.calisthenics.domain.model.BlockType
import app.calisthenics.domain.model.Catalog
import app.calisthenics.domain.model.EquipmentItem
import app.calisthenics.domain.model.EquipmentProfile
import app.calisthenics.domain.model.TargetType
import app.calisthenics.domain.model.TimelineBlock
import app.calisthenics.domain.model.WorkoutPlan
import app.calisthenics.domain.routine.Presets
import app.calisthenics.domain.routine.resolveSaved
import app.calisthenics.domain.routine.settingsFor
import app.calisthenics.domain.model.WorkoutFormat
import app.calisthenics.domain.planner.PlanResult
import app.calisthenics.domain.planner.TrainSettings
import app.calisthenics.domain.planner.buildTrainPlan
import app.calisthenics.domain.planner.buildFixedProgramPlan
import app.calisthenics.domain.planner.FixedProgramPlanResult
import app.calisthenics.domain.progression.ProgressSnapshot
import app.calisthenics.domain.routine.StarterRoutine
import app.calisthenics.domain.session.*
import io.github.gonbei774.calisthenicsmemory.session.SessionBus
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.session.WorkoutSessionService
import io.github.gonbei774.calisthenicsmemory.ui.session.SessionScreen
import io.github.gonbei774.calisthenicsmemory.ui.session.LiveScreen
import io.github.gonbei774.calisthenicsmemory.ui.session.loggerBlock
import io.github.gonbei774.calisthenicsmemory.ui.session.repLoggerBlock
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

    /** The session at the start of block [index] of [p]. */
    private fun stateAt(p: WorkoutPlan, index: Int): SessionState {
        var s = reduce(newSession("S", p), SessionEvent.Start(0)).state
        var now = 0L
        repeat(index) { now += p.blocks[it].durationSeconds * 1000L; s = reduce(s, SessionEvent.Tick(now)).state }
        return s
    }

    private fun show(s: SessionState, onExit: () -> Unit = {}) {
        SessionBus.publish(s)
        rule.setContent { CalisthenicsMemoryTheme(darkTheme = false) { SessionScreen(onExit = onExit) } }
        rule.waitForIdle()
    }

    private fun showLive(s: SessionState, catalogOverride: Catalog) {
        rule.setContent { CalisthenicsMemoryTheme(darkTheme = false) { LiveScreen(s, 0L, Modifier, catalogOverride) } }
        rule.waitForIdle()
    }

    private fun firstRecovery() = plan.blocks.indexOfFirst { it.type == BlockType.STRETCH || it.type == BlockType.PASSIVE_RECOVERY }

    @Test fun savedWorkoutWithSyntheticHeldDemonstrationShowsWrittenInstructionsInsteadOfItsClip() {
        val v = catalog.variation("pushup-standard")!!.copy(
            demonstrationHold = true,
            instructions = listOf("Synthetic cue one", "Synthetic cue two"),
        )
        app.assets.open("demos/${v.id}.mp4").use { assertTrue("fixture needs the real bundled clip", it.read() >= 0) }
        val syntheticCatalog = catalog.copy(variations = catalog.variations.map { if (it.id == v.id) v else it })
        val savedPlan = plan.copy(
            blocks = listOf(TimelineBlock("held-work", BlockType.WORK, 30, variationId = v.id)),
            plannedDurationSeconds = 30,
            rounds = 1,
        )
        showLive(stateAt(savedPlan, 0), syntheticCatalog)
        rule.onNodeWithTag("session_demo_held").assertExists()
        rule.onNodeWithText("Synthetic cue one").assertExists()
        rule.onNodeWithText("Synthetic cue two").assertExists()
        rule.onNodeWithContentDescription(app.getString(R.string.demo_clip_description)).assertDoesNotExist()
    }
    private fun lastIntent(): Intent { val i = shadowOf(app).nextStartedService; assertNotNull("no service command was sent", i); return i }

    @Test fun workBlockShowsNameTargetRingDoneAndTheRepLogger() {
        show(stateAt(0))
        rule.onNodeWithTag("session_title").assertTextEquals("Incline Push-Up")
        rule.onNodeWithTag("session_target").assertTextContains("reps", substring = true)
        rule.onNodeWithTag("session_time").assertExists()
        rule.onNodeWithTag("session_done").assertIsDisplayed().assertIsEnabled()
        // R7-T03 (D3): a rep exercise is logged on its own work screen, prefilled with the target; no break DONE here
        rule.onNodeWithTag("logger").assertIsDisplayed()
        rule.onNodeWithTag("logger_value").assertTextEquals(plan.blocks[0].target!!.shownValue().toString())
        rule.onNodeWithTag("logger_unit").assertTextEquals("x")   // R7.2-T07 (F12): "8 x"
        rule.onNodeWithTag("session_next").assertExists()
    }

    @Test fun flowProgramHasTimerAndPauseButNoPerItemLoggerDoneOrSkip() {
        val program = catalog.fixedPrograms.first { it.id == "army-hsd" }
        val p = (buildFixedProgramPlan(catalog, program, SeedProfiles.home, flow = true) as FixedProgramPlanResult.Ready).plan
        val state = stateAt(p, 0)
        assertNull(repLoggerBlock(state))
        assertNull(loggerBlock(state))
        show(state)
        rule.onNodeWithTag("session_time").assertExists()
        rule.onNodeWithTag("session_pause").assertExists()
        rule.onNodeWithTag("logger").assertDoesNotExist()
        rule.onNodeWithTag("session_done").assertDoesNotExist()
        rule.onNodeWithTag("session_skip").assertDoesNotExist()
    }

    @Test fun nonFlowFourCoreLogsItsHoldOnTheCurrentExercise() {
        val program = catalog.fixedPrograms.first { it.id == "army-4c" }
        val p = (buildFixedProgramPlan(catalog, program, SeedProfiles.home, flow = false) as FixedProgramPlanResult.Ready).plan
        val firstHold = p.blocks.indexOfFirst { it.target?.type == TargetType.HOLD_SECONDS }
        assertTrue(firstHold >= 0)
        show(stateAt(p, firstHold))
        rule.onNodeWithTag("logger").assertIsDisplayed()
        rule.onNodeWithTag("logger_unit").assertTextEquals("s")
        rule.onNodeWithTag("session_done").assertIsDisplayed()
    }

    // R7-T03: holds keep logging on the break that follows (the rep exercises are logged on the work block)
    private val gym = EquipmentProfile("gym", "Gym", listOf(EquipmentItem("pullup-bar"), EquipmentItem("low-bar"), EquipmentItem("dip-support"),
        EquipmentItem("foot-anchor"), EquipmentItem("mat")), setOf("floor-space", "wall"))
    private val holdPlan by lazy {
        val none = ProgressSnapshot(emptyMap(), emptyMap(), emptyMap(), emptyList())
        val rr = Presets.recommended(catalog, gym)
        val r = resolveSaved(rr, settingsFor(rr, TrainSettings()))
        (buildTrainPlan(catalog, r.routine, none, gym, r.settings, edits = r.edits) as PlanResult.Ready).plan.copy(id = "plan-hold")
    }
    private val holdIdx get() = holdPlan.blocks.indexOfFirst { it.type == BlockType.WORK && it.target?.type == TargetType.HOLD_SECONDS }

    @Test fun afterAHoldTheBreakCarriesTheLoggerOnTop() {
        val i = holdIdx
        assertTrue(i >= 0)
        SessionBus.publish(stateAt(holdPlan, i + 1))
        rule.setContent { CalisthenicsMemoryTheme(darkTheme = false) { SessionScreen(onExit = {}) } }
        rule.waitForIdle()
        rule.onNodeWithTag("logger").assertIsDisplayed()
        rule.onNodeWithTag("logger_name").assertTextContains(SessionBus.names[holdPlan.blocks[i].variationId]!!, substring = true)
        rule.onNodeWithTag("logger_value").assertTextEquals(holdPlan.blocks[i].target!!.shownValue().toString())
        rule.onNodeWithTag("logger_unit").assertTextEquals("s")   // R7.2-T07 (F12): "30 s"
        rule.onNodeWithTag("session_done").assertDoesNotExist()
    }

    @Test fun afterRepsTheBreakHasNoLoggerBecauseTheNumberWasLoggedOnTheWorkBlock() {
        show(stateAt(firstRecovery()))
        rule.onNodeWithTag("logger").assertDoesNotExist()
        rule.onNodeWithTag("break_done").assertIsDisplayed()
    }

    @Test fun typingRepsSendsALogCommandForTheWorkBlock() {
        show(stateAt(0))
        val target = plan.blocks[0].target!!.shownValue()
        rule.onNodeWithTag("logger_minus").performClick()
        rule.waitForIdle()
        val intent = lastIntent()
        assertEquals(WorkoutSessionService.ACTION_LOG, intent.action)
        assertEquals(plan.blocks[0].id, intent.getStringExtra(WorkoutSessionService.EXTRA_BLOCK_ID))
        assertEquals(target - 1, intent.getIntExtra(WorkoutSessionService.EXTRA_REPS, -2))
        rule.onNodeWithTag("logger_value").assertTextEquals((target - 1).toString())
    }

    // R7-T03 (D3): DONE on a rep block saves the number, then ends the set
    @Test fun doneOnARepBlockSavesTheNumberThenEndsTheSet() {
        show(stateAt(0))
        val target = plan.blocks[0].target!!.shownValue()
        rule.onNodeWithTag("logger_plus").performClick(); rule.waitForIdle()
        shadowOf(app).clearStartedServices()
        rule.onNodeWithTag("session_done").performClick(); rule.waitForIdle()
        val intents = generateSequence { shadowOf(app).nextStartedService }.toList()
        assertEquals(listOf(WorkoutSessionService.ACTION_LOG, WorkoutSessionService.ACTION_DONE), intents.map { it.action })
        assertEquals(target + 1, intents[0].getIntExtra(WorkoutSessionService.EXTRA_REPS, -2))
    }

    // R7-T03 (D3, B9): "Too easy" next to the stepper feeds the same tooEasy flag as the end screen
    @Test fun tooEasyNextToTheStepperIsSentWithTheLog() {
        show(stateAt(0))
        rule.onNodeWithTag("logger_too_easy").performClick(); rule.waitForIdle()
        val intent = lastIntent()
        assertEquals(WorkoutSessionService.ACTION_LOG, intent.action)
        assertTrue(intent.getBooleanExtra(WorkoutSessionService.EXTRA_TOO_EASY, false))
        assertFalse(intent.getBooleanExtra(WorkoutSessionService.EXTRA_TOO_HARD, true))
    }

    @Test fun noHoldBreakLoggerForRepsAndHoldsKeepTheirLoggerOnTheBreak() {
        val i = holdIdx
        SessionBus.publish(stateAt(holdPlan, i))
        rule.setContent { CalisthenicsMemoryTheme(darkTheme = false) { SessionScreen(onExit = {}) } }
        rule.waitForIdle()
        rule.onNodeWithTag("logger").assertDoesNotExist() // a hold is not logged while it runs
    }

    @Test fun noRatingChipsDuringTheWorkoutExceptTooEasy() {
        // doc 17 §2.3: Too hard / Pain stay on the end screen; R7-T03: Too easy is next to the stepper
        show(stateAt(0))
        for (t in listOf("logger_too_hard", "logger_pain")) rule.onNodeWithTag(t).assertDoesNotExist()
        rule.onNodeWithTag("logger_minus").assertExists(); rule.onNodeWithTag("logger_plus").assertExists(); rule.onNodeWithTag("logger_too_easy").assertExists(); rule.onNodeWithTag("session_done").assertExists()
    }

    @Test fun theBottomRowIsPauseDoneSkipWithDoneInTheMiddle() {
        show(stateAt(0))
        val x = { t: String -> rule.onNodeWithTag(t).fetchSemanticsNode().boundsInRoot.center.x }
        assertTrue(x("session_pause") < x("session_done") && x("session_done") < x("session_skip"))
        val w = { t: String -> rule.onNodeWithTag(t).fetchSemanticsNode().boundsInRoot.width }
        assertTrue("DONE is the widest", w("session_done") > w("session_pause") && w("session_done") > w("session_skip"))
        rule.onNodeWithTag("session_done").assertTextContains("DONE")
    }

    @Test fun theTimerHeaderAndSetMarkersAreShown() {
        show(stateAt(0))
        rule.onNodeWithTag("session_round").assertTextContains("ROUND 1 OF 2")
        rule.onNodeWithTag("session_markers").assertExists(); rule.onNodeWithTag("session_elapsed").assertExists(); rule.onNodeWithTag("session_time").assertExists()
    }

    @Test fun loggedValuesComeBackFromTheSessionState() {
        val s0 = stateAt(0)
        val s = reduce(s0, SessionEvent.LogBlock(plan.blocks[0].id, BlockLog(reps = 4, tooHard = true))).state
        show(s)
        rule.onNodeWithTag("logger_value").assertTextEquals("4")
    }

    // R8.1-T03 (F3): the next set of an exercise starts at the number just logged, and that number is recorded without touching the stepper
    @Test fun theNextSetStartsAtTheLastLoggedNumberAndRecordsIt() {
        val firstWork = plan.blocks.first { it.type == BlockType.WORK && it.target?.type == TargetType.REPS }
        val second = plan.blocks.first { it.type == BlockType.WORK && it.variationId == firstWork.variationId && it.id != firstWork.id }
        val s0 = reduce(stateAt(plan.blocks.indexOf(second)), SessionEvent.LogBlock(firstWork.id, BlockLog(reps = 3))).state
        show(s0)
        rule.onNodeWithTag("logger_value").assertTextEquals("3")
        rule.waitForIdle()
        val log = generateSequence { shadowOf(app).nextStartedService }.toList().last { it.action == WorkoutSessionService.ACTION_LOG }
        assertEquals(second.id, log.getStringExtra(WorkoutSessionService.EXTRA_BLOCK_ID))
        assertEquals(3, log.getIntExtra(WorkoutSessionService.EXTRA_REPS, -2))
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
        // R7-T03: DONE on a rep block first saves the number (ACTION_LOG), then ends the set
        assertEquals(WorkoutSessionService.ACTION_DONE, generateSequence { shadowOf(app).nextStartedService }.last().action)
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

    private fun headerFor(f: WorkoutFormat, text: String) {
        val none = ProgressSnapshot(emptyMap(), emptyMap(), emptyMap(), emptyList())
        val p = (buildTrainPlan(catalog, StarterRoutine.routine, none, SeedProfiles.home, TrainSettings(rounds = 2, stretchOn = true, format = f)) as PlanResult.Ready).plan.copy(id = "plan-$f")
        SessionBus.names = catalog.variations.associate { it.id to it.name }
        SessionBus.publish(reduce(newSession("S", p), SessionEvent.Start(0)).state)
        rule.setContent { CalisthenicsMemoryTheme(darkTheme = false) { SessionScreen(onExit = {}) } }
        rule.waitForIdle()
        rule.onNodeWithTag("session_round").assertTextContains(text)
        rule.onNodeWithTag("session_markers").assertExists()
    }

    @Test fun pairsNameThePairAndTheSet() = headerFor(WorkoutFormat.PAIRS, "PAIR 1 · SET 1 OF 2")

    // R7-T01 (B2): a hold under the Recommended Routine's 10-30 s range shows "30 s" and the logger starts at 30
    @Test fun aSupportHoldUnderTheRecommendedRoutineShowsThirtySecondsAndPrefillsThirty() {
        val gym = EquipmentProfile("gym", "Gym", listOf(
            EquipmentItem("pullup-bar"), EquipmentItem("low-bar"), EquipmentItem("dip-support"),
            EquipmentItem("foot-anchor"), EquipmentItem("mat")), setOf("floor-space", "wall"))
        val none = ProgressSnapshot(emptyMap(), emptyMap(), emptyMap(), emptyList())
        val rr = Presets.recommended(catalog, gym)
        val r = resolveSaved(rr, settingsFor(rr, TrainSettings()))
        val p = (buildTrainPlan(catalog, r.routine, none, gym, r.settings, edits = r.edits) as PlanResult.Ready).plan.copy(id = "plan-rr")
        val i = p.blocks.indexOfFirst { it.type == BlockType.WORK && it.variationId == "dip-support-hold" }
        assertTrue(i >= 0)
        assertEquals(10, p.blocks[i].target!!.value); assertEquals(30, p.blocks[i].target!!.max)
        SessionBus.names = catalog.variations.associate { it.id to it.name }
        SessionBus.publish(stateAt(p, i))
        rule.setContent { CalisthenicsMemoryTheme(darkTheme = false) { SessionScreen(onExit = {}) } }
        rule.waitForIdle()
        rule.onNodeWithTag("session_target").assertTextEquals("30 s")
        rule.runOnIdle { SessionBus.publish(stateAt(p, i + 1)) }
        rule.waitForIdle()
        rule.onNodeWithTag("logger_value").assertTextEquals("30")
    }

    // R7-T02 (B4): DONE in a break saves the pending log, then skips to the next block
    @Test fun doneInABreakWithTheLoggerSavesTheLogAndSkips() {
        SessionBus.publish(stateAt(holdPlan, holdIdx + 1))
        rule.setContent { CalisthenicsMemoryTheme(darkTheme = false) { SessionScreen(onExit = {}) } }
        rule.waitForIdle()
        rule.onNodeWithTag("logger_minus").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("logger_done").performClick(); rule.waitForIdle()
        val actions = generateSequence { shadowOf(app).nextStartedService }.map { it.action }.toList()
        assertEquals(WorkoutSessionService.ACTION_SKIP, actions.last())
        assertTrue("the log is saved before the skip", actions.dropLast(1).contains(WorkoutSessionService.ACTION_LOG))
    }

    @Test fun aBreakWithoutALoggerStillOffersDoneAndSkips() {
        val none = ProgressSnapshot(emptyMap(), emptyMap(), emptyMap(), emptyList())
        val p = (buildTrainPlan(catalog, StarterRoutine.routine, none, SeedProfiles.home, TrainSettings(rounds = 2, stretchOn = true, warmupOn = true)) as PlanResult.Ready).plan.copy(id = "plan-w2")
        SessionBus.publish(stateAt(p, p.blocks.indexOfFirst { it.type == BlockType.WARMUP }))
        rule.setContent { CalisthenicsMemoryTheme(darkTheme = false) { SessionScreen(onExit = {}) } }
        rule.waitForIdle()
        rule.onNodeWithTag("logger").assertDoesNotExist()
        rule.onNodeWithTag("break_done").assertIsDisplayed().performClick(); rule.waitForIdle()
        assertEquals(WorkoutSessionService.ACTION_SKIP, lastIntent().action)
    }

    // R7-T02 (B5): the caption is readable and a stretch block names the stretch
    @Test fun aStretchBlockCaptionNamesTheStretch() {
        val i = firstRecovery()
        show(stateAt(i))
        val name = SessionBus.names[plan.blocks[i].variationId]!!
        rule.onNodeWithTag("session_caption").assertTextContains("STRETCH · $name")
    }

    // R7-T02 (B6): WARM-UP n OF N counts the warm-up blocks only
    @Test fun theWarmupHeaderCountsOnlyWarmupBlocks() {
        val none = ProgressSnapshot(emptyMap(), emptyMap(), emptyMap(), emptyList())
        val p = (buildTrainPlan(catalog, StarterRoutine.routine, none, SeedProfiles.home, TrainSettings(rounds = 2, stretchOn = true, warmupOn = true)) as PlanResult.Ready).plan.copy(id = "plan-w")
        val n = p.blocks.count { it.type == BlockType.WARMUP }
        assertTrue(n >= 2)
        SessionBus.publish(stateAt(p, p.blocks.withIndex().filter { it.value.type == BlockType.WARMUP }[1].index))
        rule.setContent { CalisthenicsMemoryTheme(darkTheme = false) { SessionScreen(onExit = {}) } }
        rule.waitForIdle()
        rule.onNodeWithTag("session_round").assertTextContains("WARM-UP 2 OF $n")
    }
}


/** R7.2-T05 (F6): at text size 1.3 "Resume" stays on one line and is the filled gold button. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi", fontScale = 1.3f)
class SessionPausedFontScaleTest {
    @get:Rule val rule = createComposeRule()
    private val app: Application get() = ApplicationProvider.getApplicationContext()
    @After fun tearDown() { SessionBus.clear() }

    @Test fun resumeIsOneLineAndFilledGold() {
        val catalog = loadCatalog(app)
        SessionBus.clear(); SessionBus.names = catalog.variations.associate { it.id to it.name }
        val none = ProgressSnapshot(emptyMap(), emptyMap(), emptyMap(), emptyList())
        val plan = (buildTrainPlan(catalog, StarterRoutine.routine, none, SeedProfiles.home, TrainSettings(rounds = 2, stretchOn = true)) as PlanResult.Ready).plan.copy(id = "plan-1")
        val s = reduce(reduce(newSession("S", plan), SessionEvent.Start(0)).state, SessionEvent.Pause(5_000)).state
        SessionBus.publish(s)
        rule.setContent { CalisthenicsMemoryTheme(darkTheme = false) { SessionScreen(onExit = {}) } }
        rule.waitForIdle()
        val node = rule.onNodeWithTag("session_resume", useUnmergedTree = true).assertIsDisplayed()
        val results = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        val textNode = rule.onNode(hasText("Resume", ignoreCase = true) and hasAnyAncestor(hasTestTag("session_resume")), useUnmergedTree = true)
        textNode.fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        assertEquals("Resume on one line", 1, results.single().lineCount)
        node.assertHasClickAction()
    }
}
