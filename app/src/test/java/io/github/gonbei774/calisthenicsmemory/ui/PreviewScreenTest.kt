package io.github.gonbei774.calisthenicsmemory.ui

import android.app.Application
import android.content.Context
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import app.calisthenics.domain.model.WorkoutPlan
import io.github.gonbei774.calisthenicsmemory.session.WorkoutSessionService
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalisthenicsMemoryTheme
import io.github.gonbei774.calisthenicsmemory.ui.train.PreviewScreen
import kotlinx.serialization.json.Json
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
class PreviewScreenTest {
    @get:Rule val rule = createComposeRule()
    private val app: Application get() = ApplicationProvider.getApplicationContext()

    @Before fun clean() {
        for (n in listOf("profiles", "mode", "planner_prefs", "train", "goal", "levels", "routine"))
            app.getSharedPreferences(n, Context.MODE_PRIVATE).edit().clear().commit()
    }

    // Touch injection does not reach the bottom-sheet window under Robolectric, so taps inside the sheet use the semantic click.
    private fun openSwap(slot: String) {
        rule.onNodeWithTag("swap_$slot").performScrollTo().performClick(); rule.waitForIdle()
        rule.mainClock.advanceTimeBy(1_000); rule.waitForIdle() // let the sheet finish sliding in before tapping inside it
    }

    private fun show(onStarted: () -> Unit = {}) = rule.setContent {
        CalisthenicsMemoryTheme(darkTheme = false) { PreviewScreen(onBack = {}, onStarted = onStarted) }
    }

    @Test fun headerSaysRoundsMinutesAndStyle() {
        show(); rule.waitForIdle()
        rule.onNodeWithTag("preview_header").assertTextContains("4 rounds", substring = true)
        rule.onNodeWithTag("preview_header").assertTextContains("Reps", substring = true)
    }

    @Test fun oneCardPerExerciseOfTheCircuit() {
        show(); rule.waitForIdle()
        for (slot in listOf("push", "squat", "pull", "core", "hinge", "core2")) rule.onNodeWithTag("exercise_$slot").assertExists()
        rule.onAllNodes(hasTestTag("exercise_push")).assertCountEquals(1)
        rule.onNodeWithTag("exercise_name_push").assertTextEquals("Incline Push-Up")
    }

    @Test fun cardsShowTheMusclesWorked() {
        show(); rule.waitForIdle()
        rule.onNodeWithTag("muscles_push").assertExists()
        // V09 (R9): the body figure replaces the muscle names on the card; the names stay in its screen-reader description
        fun desc(tag: String) = rule.onNodeWithTag(tag).fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.ContentDescription].joinToString()
        assertTrue(desc("muscles_push"), desc("muscles_push").contains("Chest"))
        assertTrue(desc("muscles_core"), desc("muscles_core").contains("Abs"))
        rule.onNodeWithTag("thumb_pushup-incline", useUnmergedTree = true).assertExists()
    }

    @Test fun swapChangesTheCardAndOffersOnlySameMovementOptions() {
        show(); rule.waitForIdle()
        openSwap("push")
        rule.onNodeWithTag("swap_option_pushup-incline").assertExists() // the current one is listed
        rule.onNodeWithTag("swap_option_squat-air").assertDoesNotExist()
        rule.onNodeWithTag("swap_option_pushup-knee").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick); rule.waitForIdle()
        rule.onNodeWithTag("exercise_name_push").assertTextEquals("Knee Push-Up")
        rule.onNodeWithTag("exercise_name_squat").assertTextEquals("Air Squat") // other cards untouched
    }

    @Test fun keepForNextTimeSavesASwapToTheUsualPlanOtherwiseItIsTodayOnly() {
        show(); rule.waitForIdle()
        openSwap("push")
        rule.onNodeWithTag("swap_keep").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        rule.onNodeWithTag("swap_option_pushup-knee").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick); rule.waitForIdle()
        val saved = io.github.gonbei774.calisthenicsmemory.ui.screens.RoutineStore.load(app)
        assertEquals("pushup-knee", saved.slots.first { it.id == "push" }.preferredVariationId)
        assertEquals(2, saved.revision) // a new revision, never an edit of the old one
    }

    @Test fun todayOnlySwapLeavesTheUsualPlanAlone() {
        show(); rule.waitForIdle()
        openSwap("push")
        rule.onNodeWithTag("swap_option_pushup-knee").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick); rule.waitForIdle()
        assertEquals("pushup-incline", io.github.gonbei774.calisthenicsmemory.ui.screens.RoutineStore.load(app).slots.first { it.id == "push" }.preferredVariationId)
    }

    @Test fun kettlebellCardsShowTheBellWeightAndBodyweightCardsDoNot() {
        show(); rule.waitForIdle()
        openSwap("hinge")
        rule.onNodeWithTag("swap_other_types").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick); rule.waitForIdle()
        rule.onNodeWithTag("swap_option_kettlebell-deadlift").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick); rule.waitForIdle()
        rule.onNodeWithTag("exercise_name_hinge").assertTextEquals("Controlled Kettlebell Deadlift")
        rule.onNodeWithTag("exercise_target_hinge").assertTextContains("· 12 kg", substring = true) // Home profile seed bell
        rule.onNodeWithTag("exercise_target_push").assertTextEquals("6 reps")
    }

    @Test fun timelineIsCollapsedUntilOpened() {
        show(); rule.waitForIdle()
        rule.onNodeWithTag("timeline_list").assertDoesNotExist()
        rule.onNodeWithTag("timeline_toggle").performScrollTo().performClick()
        rule.onNodeWithTag("timeline_list").assertExists()
    }

    @Test fun warningsAreOneCompactRowThatExpands() {
        show(); rule.waitForIdle()
        rule.onNodeWithTag("info_row").assertExists().assertTextContains("unreviewed draft", substring = true)
    }

    @Test fun startLaunchesTheServiceWithAFreshPlanId() {
        var started = false
        show { started = true }; rule.waitForIdle()
        rule.onNodeWithTag("start_button").assertIsEnabled().performClick()
        assertTrue(started)
        val intent = shadowOf(app).nextStartedService
        assertNotNull("no service was started", intent)
        assertEquals(WorkoutSessionService.ACTION_START, intent.action)
        val plan = Json { ignoreUnknownKeys = true }.decodeFromString(WorkoutPlan.serializer(), intent.getStringExtra(WorkoutSessionService.EXTRA_PLAN)!!)
        assertNotEquals("draft", plan.id); assertNotEquals("preview", plan.id)
        assertEquals(4, plan.rounds)
    }

    // ---- V16: the editor
    private val click = androidx.compose.ui.semantics.SemanticsActions.OnClick

    @Test fun theLevelStepperChangesTheTargetAndStaysWithinOneToFive() {
        show(); rule.waitForIdle()
        val before = rule.onNodeWithTag("exercise_target_push").fetchSemanticsNode().config.first { it.key.name == "Text" }.value.let { (it as List<*>).first().toString() }
        rule.onNodeWithTag("level_plus_push").performClick(); rule.waitForIdle()
        val after = rule.onNodeWithTag("exercise_target_push").fetchSemanticsNode().config.first { it.key.name == "Text" }.value.let { (it as List<*>).first().toString() }
        assertNotEquals(before, after)
        rule.onNodeWithTag("level_push").assertTextContains("Level 2 of 5")
        rule.onNodeWithTag("level_minus_push").performClick(); rule.onNodeWithTag("level_minus_push").assertIsNotEnabled()
        rule.onNodeWithTag("exercise_target_squat").assertExists() // other exercises untouched
    }

    @Test fun longPressRemovesAnExerciseAfterConfirmationAndUndoRestoresIt() {
        show(); rule.waitForIdle()
        rule.onNodeWithTag("exercise_core").performScrollTo().performTouchInput { longClick() }; rule.waitForIdle()
        rule.onNodeWithTag("remove_confirm").assertExists()
        rule.onNodeWithTag("remove_confirm").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("exercise_core").assertDoesNotExist()
        rule.onNodeWithTag("edits_reset").performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("exercise_core").assertExists()
    }

    @Test fun setTabsShowOneSetAtATimeWithTheTimeOfThatSet() {
        show(); rule.waitForIdle()
        rule.onNodeWithTag("set_tab_1").assertIsSelected()
        for (i in 1..4) rule.onNodeWithTag("set_tab_$i").assertExists()
        rule.onNodeWithTag("set_tab_3").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("set_tab_3").assertIsSelected()
        rule.onNodeWithTag("set_time").assertExists()
        rule.onNodeWithText("Changes apply to all sets", ignoreCase = true).assertExists()
    }

    @Test fun breakRowsShowTheStretchAndCanBeChangedAndAFixedStretchIsUsed() {
        show(); rule.waitForIdle()
        rule.onNodeWithTag("break_push").assertExists()
        rule.onNodeWithTag("break_push").performScrollTo().performClick(); rule.waitForIdle(); rule.mainClock.advanceTimeBy(1_000); rule.waitForIdle()
        rule.onNodeWithTag("break_option_stretch-pigeon").performSemanticsAction(click); rule.waitForIdle()
        rule.onNodeWithTag("break_push").assertTextContains("Pigeon", substring = true)
    }

    @Test fun thePlusBetweenBlocksAddsAnExerciseAfterThatSlot() {
        show(); rule.waitForIdle()
        rule.onNodeWithTag("add_after_squat").performScrollTo().performClick(); rule.waitForIdle(); rule.mainClock.advanceTimeBy(1_000); rule.waitForIdle()
        rule.onNodeWithTag("add_exercise").performSemanticsAction(click); rule.waitForIdle()
        rule.onNodeWithTag("add_option_pushup-diamond").performSemanticsAction(click); rule.waitForIdle()
        rule.onNodeWithTag("exercise_add-pushup-diamond").assertExists()
        val order = mutableListOf<String>()
        fun walk(n: androidx.compose.ui.semantics.SemanticsNode) { n.config.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.TestTag)?.let { order += it }; n.children.forEach { walk(it) } }
        walk(rule.onRoot().fetchSemanticsNode())
        assertTrue(order.indexOf("exercise_squat") < order.indexOf("exercise_add-pushup-diamond") && order.indexOf("exercise_add-pushup-diamond") < order.indexOf("exercise_pull"))
    }

    @Test fun theSwapSheetShowsTheWholeFamilyAndOtherTypesBehindAButton() {
        show(); rule.waitForIdle()
        openSwap("push")
        for (id in listOf("pushup-incline", "pushup-knee", "pushup-standard", "pushup-diamond")) rule.onNodeWithTag("swap_option_$id").assertExists()
        rule.onNodeWithTag("swap_option_squat-air").assertDoesNotExist()
        rule.onNodeWithTag("swap_other_types").performSemanticsAction(click); rule.waitForIdle()
        rule.onNodeWithTag("swap_option_squat-air").assertExists()
    }

    // ---- V17: Detailed edit
    @Test fun detailedEditListsEverySetAndAStretchCanDifferPerSet() {
        show(); rule.waitForIdle()
        rule.onNodeWithTag("detailed_switch").performClick(); rule.waitForIdle()
        for (st in 1..4) rule.onNodeWithTag("set_header_$st").performScrollTo().assertExists()
        rule.onNodeWithTag("set_tab_1").assertDoesNotExist()
        rule.onNodeWithTag("break_push_s2").performScrollTo().performClick(); rule.waitForIdle(); rule.mainClock.advanceTimeBy(1_000); rule.waitForIdle()
        rule.onNodeWithTag("break_option_stretch-frog").performSemanticsAction(click); rule.waitForIdle()
        rule.onNodeWithTag("break_push_s2").performScrollTo().assertTextContains("Frog", substring = true)
        rule.onNodeWithTag("break_push_s3").performScrollTo().assertTextContains("Shoulder", substring = true) // the other sets keep their own stretch
    }

    // The "type a number" dialog has a text field. Robolectric's Compose clock never goes idle while a dialog with a text field is open
    // (the test hangs for minutes), so the dialog itself is not driven here. What it produces is covered by the planner tests
    // (DetailedEditTest: the free number is marked, shown, and excluded from progression) and the target cell's tag is checked.
    @Test fun inTheDetailedEditEachSetsNumberIsATapTarget() {
        show(); rule.waitForIdle()
        rule.onNodeWithTag("detailed_switch").performClick(); rule.waitForIdle()
        for (st in 1..4) rule.onNodeWithTag("exercise_target_push_s$st").performScrollTo().assertHasClickAction()
        rule.onNodeWithTag("level_push_s2").performScrollTo().assertTextContains("Level", substring = true)
        rule.onNodeWithTag("exercise_target_push").assertDoesNotExist() // the plain Preview tag only exists outside the Detailed edit
    }
}
