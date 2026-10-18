package io.github.gonbei774.calisthenicsmemory.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import app.calisthenics.domain.model.WorkoutPlan
import app.calisthenics.domain.model.BlockType
import io.github.gonbei774.calisthenicsmemory.session.WorkoutSessionService
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalisthenicsMemoryTheme
import io.github.gonbei774.calisthenicsmemory.ui.train.PreviewScreen
import io.github.gonbei774.calisthenicsmemory.ui.train.TrainData
import io.github.gonbei774.calisthenicsmemory.ui.train.rememberTrainData
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

    // R7-T13 (D10): the cool-down is part of the Preview: timed minutes with - / +, or stretches that can be swapped
    @Test fun aTimedCooldownShowsItsMinutesAndTheyCanBeChanged() {
        app.getSharedPreferences("train", Context.MODE_PRIVATE).edit().putBoolean("cooldown", true).putString("cooldown_mode", "TIMED").commit()
        show(); rule.waitForIdle()
        rule.onNodeWithTag("cooldown_timed").performScrollTo().assertExists()
        rule.onNodeWithText("Relax and breathe · 5 min").assertExists()
        rule.onNodeWithTag("cooldown_minutes_plus").performClick(); rule.waitForIdle()
        rule.onNodeWithText("Relax and breathe · 6 min").assertExists()
    }

    @Test fun aCooldownStretchCanBeRemovedAndOneAddedWithThePlus() {   // R7.2-T04 (F5)
        app.getSharedPreferences("train", Context.MODE_PRIVATE).edit().putBoolean("cooldown", true).putString("cooldown_mode", "STRETCHING").commit()
        show(); rule.waitForIdle()
        fun count() = (0..20).count { rule.onAllNodesWithTag("cooldown_stretch_$it").fetchSemanticsNodes().isNotEmpty() }
        val before = count()
        assertTrue(before >= 2)
        rule.onNodeWithTag("cooldown_remove_0").performScrollTo().performClick(); rule.waitForIdle()
        assertEquals(before - 1, count())
        rule.onNodeWithTag("cooldown_stretch_0").performScrollTo().performTouchInput { longClick() }; rule.waitForIdle()   // long press removes too
        assertEquals(before - 2, count())
        rule.onNodeWithTag("cooldown_add").performScrollTo().performClick(); rule.waitForIdle(); rule.mainClock.advanceTimeBy(1_000); rule.waitForIdle()
        rule.onNodeWithTag("break_option_stretch-frog").performSemanticsAction(click); rule.waitForIdle()
        assertEquals(before - 1, count())
        rule.onNodeWithTag("cooldown_add").assertExists()
    }

    @Test fun aStretchingCooldownListsStretchesWithASwapButton() {
        app.getSharedPreferences("train", Context.MODE_PRIVATE).edit().putBoolean("cooldown", true).putString("cooldown_mode", "STRETCHING").commit()
        show(); rule.waitForIdle()
        rule.onNodeWithTag("cooldown_stretch_0").performScrollTo().assertExists()
        rule.onNodeWithTag("cooldown_swap_0").assertExists()
    }

    // R8.1-T05 (F5): Straight sets / Pairs / Circuit and the number of sets are chosen in the Preview too
    @Test fun theSetFormatAndTheNumberOfSetsAreChosenInThePreview() {
        show(); rule.waitForIdle()
        for (f in listOf("CIRCUIT", "PAIRS", "STRAIGHT")) rule.onNodeWithTag("preview_format_$f").performScrollTo().assertExists()
        rule.onNodeWithTag("preview_format_CIRCUIT").assertIsSelected()
        rule.onNodeWithTag("preview_format_STRAIGHT").performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("preview_format_STRAIGHT").assertIsSelected()
        assertEquals("STRAIGHT", app.getSharedPreferences("train", Context.MODE_PRIVATE).getString("format", null))
        rule.onNodeWithTag("preview_rounds_plus").performScrollTo().performClick(); rule.waitForIdle()
        assertEquals(5, app.getSharedPreferences("train", Context.MODE_PRIVATE).getInt("rounds", 0))
        rule.onNodeWithTag("preview_header").assertTextContains("5 rounds", substring = true)
    }

    // R8.1-T06 (F7): Flow is one toggle in the Preview, next to the rest controls, for every workout
    @Test fun flowIsOneToggleInThePreviewAndShortensTheWorkout() {
        show(); rule.waitForIdle()
        fun minutes() = rule.onNodeWithTag("preview_header").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.Text].first().text.split("·")[1].filter { it.isDigit() }.toInt()
        val before = minutes()
        rule.onNodeWithTag("preview_flow_switch").performScrollTo().assertIsOff().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("preview_flow_switch").assertIsOn()
        assertTrue("$before -> ${minutes()}", minutes() < before)
        assertTrue(app.getSharedPreferences("train", Context.MODE_PRIVATE).getBoolean("flow", false))
        rule.onNodeWithTag("preview_flow_hint").assertExists()
    }

    // R8.1-T05 (F6): every change survives going back to Train and returning; only another objective resets it
    @Test fun editsSurviveLeavingThePreviewAndComingBack() {
        io.github.gonbei774.calisthenicsmemory.ui.screens.PrefsStore.save(app, io.github.gonbei774.calisthenicsmemory.ui.screens.PrefsStore.load(app).copy(stretchOn = false))   // plain rest, so the rest steppers exist
        var visible by mutableStateOf(true)
        rule.setContent { CalisthenicsMemoryTheme(darkTheme = false) { if (visible) PreviewScreen(onBack = {}, onStarted = {}) else androidx.compose.foundation.layout.Box {} } }
        rule.waitForIdle()
        openSwap("push")
        rule.onNodeWithTag("swap_option_pushup-knee").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick); rule.waitForIdle()
        rule.onNodeWithTag("level_plus_squat").performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("rest_pull_plus").performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("preview_format_PAIRS").performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("exercise_core2").performScrollTo().performTouchInput { longClick() }; rule.waitForIdle()
        rule.onNodeWithTag("remove_confirm").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick); rule.waitForIdle()
        val level = rule.onNodeWithTag("level_squat").fetchSemanticsNode().config.first { it.key.name == "Text" }.value.toString()
        fun rest() = rule.onNodeWithTag("rest_pull_value").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.ContentDescription].joinToString()
        val rest = rest()
        visible = false; rule.waitForIdle()
        visible = true; rule.waitForIdle()
        rule.onNodeWithTag("exercise_name_push").assertTextEquals("Knee Push-Up")
        rule.onNodeWithTag("exercise_core2").assertDoesNotExist()
        assertEquals(level, rule.onNodeWithTag("level_squat").fetchSemanticsNode().config.first { it.key.name == "Text" }.value.toString())
        assertEquals(rest, rest())
        rule.onNodeWithTag("preview_format_PAIRS").assertIsSelected()
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
        // R8.1-T08 (F10): was asserting the "unreviewed draft" bubble; the row now only points at the notes, and no draft bubble exists anywhere in the Preview
        rule.onNodeWithTag("info_row").assertExists().assertTextContains("Check the notes", substring = true)
        rule.onNodeWithText("unreviewed draft", substring = true).assertDoesNotExist()
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

    @Test fun restDurationCanBeChangedAndTheStartedPlanUsesTheUpdatedTime() {
        val prefs = io.github.gonbei774.calisthenicsmemory.ui.screens.PrefsStore.load(app)
        io.github.gonbei774.calisthenicsmemory.ui.screens.PrefsStore.save(app, prefs.copy(stretchOn = false))
        show(); rule.waitForIdle()
        val original = rule.onNodeWithTag("rest_push_value").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.ContentDescription].joinToString()
        val beforeHeader = rule.onNodeWithTag("preview_header").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.Text].joinToString()
        val originalSeconds = original.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
        val otherRestSeconds = listOf("squat", "pull", "core", "hinge", "core2").associateWith { slot ->
            val clock = rule.onNodeWithTag("rest_${slot}_value").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.ContentDescription].joinToString()
            clock.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
        }
        val updatedLabel = "%d:%02d".format((originalSeconds + 15) / 60, (originalSeconds + 15) % 60)
        rule.onNodeWithTag("rest_push_plus").performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("rest_push_value").assertContentDescriptionEquals(updatedLabel)
        val afterHeader = rule.onNodeWithTag("preview_header").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.Text].joinToString()
        assertNotEquals(beforeHeader, afterHeader)
        rule.onNodeWithTag("start_button").performClick()
        val intent = shadowOf(app).nextStartedService
        val plan = Json { ignoreUnknownKeys = true }.decodeFromString(WorkoutPlan.serializer(), intent.getStringExtra(WorkoutSessionService.EXTRA_PLAN)!!)
        val pushRests = plan.blocks.filter { it.type == BlockType.PASSIVE_RECOVERY && it.slotId == "push" }
        assertTrue(pushRests.isNotEmpty())
        assertTrue(pushRests.all { it.durationSeconds == originalSeconds + 15 })
        otherRestSeconds.forEach { (slot, seconds) ->
            assertTrue(plan.blocks.filter { it.type == BlockType.PASSIVE_RECOVERY && it.slotId == slot }.all { it.durationSeconds == seconds })
        }
        assertEquals(plan.blocks.sumOf { it.durationSeconds }, plan.plannedDurationSeconds)
    }

    @Test fun aProgressRefreshDoesNotDiscardPreviewRestEdits() {
        val prefs = io.github.gonbei774.calisthenicsmemory.ui.screens.PrefsStore.load(app)
        io.github.gonbei774.calisthenicsmemory.ui.screens.PrefsStore.save(app, prefs.copy(stretchOn = false))
        lateinit var firstData: TrainData
        lateinit var updateData: (TrainData) -> Unit
        rule.setContent {
            CalisthenicsMemoryTheme(darkTheme = false) {
                val loaded = rememberTrainData().value
                var data by remember { mutableStateOf(loaded) }
                SideEffect { firstData = data; updateData = { data = it } }
                PreviewScreen(onBack = {}, onStarted = {}, dataOverride = data)
            }
        }
        rule.waitForIdle()
        val original = rule.onNodeWithTag("rest_push_value").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.ContentDescription].joinToString()
        val originalSeconds = original.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
        val changed = "%d:%02d".format((originalSeconds + 15) / 60, (originalSeconds + 15) % 60)
        rule.onNodeWithTag("rest_push_plus").performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("rest_push_value").assertContentDescriptionEquals(changed)

        rule.runOnUiThread { updateData(firstData.copy(routine = firstData.routine.copy(name = "Refreshed routine"))) }
        rule.waitForIdle()
        rule.onNodeWithTag("rest_push_value").assertContentDescriptionEquals(changed)
        rule.onNodeWithTag("edits_reset").assertExists()
    }

    @Test fun resettingAPreviewRestChoiceRestoresItsOriginalDuration() {
        val prefs = io.github.gonbei774.calisthenicsmemory.ui.screens.PrefsStore.load(app)
        io.github.gonbei774.calisthenicsmemory.ui.screens.PrefsStore.save(app, prefs.copy(stretchOn = false))
        show(); rule.waitForIdle()
        val original = rule.onNodeWithTag("rest_push_value").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.ContentDescription].joinToString()
        rule.onNodeWithTag("rest_push_plus").performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("edits_reset").performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("rest_push_value").assertContentDescriptionEquals(original)
        rule.onNodeWithTag("edits_reset").assertDoesNotExist()
    }

    @Test fun aPlainRestStillOpensItsBreakPickerFromTheChangeButton() {
        val prefs = io.github.gonbei774.calisthenicsmemory.ui.screens.PrefsStore.load(app)
        io.github.gonbei774.calisthenicsmemory.ui.screens.PrefsStore.save(app, prefs.copy(stretchOn = false))
        show(); rule.waitForIdle()
        rule.onNodeWithTag("break_change_push").performScrollTo().performClick(); rule.waitForIdle(); rule.mainClock.advanceTimeBy(1_000); rule.waitForIdle()
        rule.onNodeWithTag("break_option_auto").assertExists()
        rule.onNodeWithTag("break_option_auto").performSemanticsAction(click); rule.waitForIdle()
        rule.onNodeWithTag("rest_push_plus").assertExists()
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

    @Test fun theRoundViewHasNoSetTabsAndOneAllSetsHeader() {
        show(); rule.waitForIdle()
        for (i in 1..4) rule.onNodeWithTag("set_tab_$i").assertDoesNotExist()
        rule.onNodeWithTag("all_sets_header").assertExists().assertTextContains("All sets", substring = true)
        rule.onNodeWithTag("all_sets_header").assertTextContains("min each", substring = true)
        rule.onNodeWithText("Changes apply to all sets", ignoreCase = true).assertExists()
    }

    @Test fun breakRowsShowTheStretchAndCanBeChangedAndAFixedStretchIsUsed() {
        show(); rule.waitForIdle()
        rule.onNodeWithTag("break_push").assertExists()
        rule.onNodeWithTag("break_push").performScrollTo().performClick(); rule.waitForIdle(); rule.mainClock.advanceTimeBy(1_000); rule.waitForIdle()
        rule.onNodeWithTag("break_option_stretch-pigeon").performSemanticsAction(click); rule.waitForIdle()
        rule.onNodeWithTag("break_push").assertTextContains("Pigeon", substring = true)
    }

    @Test fun theBreakAndPlusShareARowAndTheBreakFillsTheRemainingWidth() {   // R7.2-T03 (F4)
        show(); rule.waitForIdle()
        rule.onNodeWithTag("break_push").performScrollTo().assertExists()
        rule.onNodeWithText("change").assertDoesNotExist()                               // the word is gone, the icon stays
        rule.onNodeWithTag("break_push").assertContentDescriptionEquals("change")
        val card = rule.onNodeWithTag("break_push").fetchSemanticsNode().boundsInRoot
        val plus = rule.onNodeWithTag("add_after_push").fetchSemanticsNode().boundsInRoot
        assertTrue("the + shares the card row", kotlin.math.abs(plus.center.y - card.center.y) < 16 * rule.density.density)
        assertTrue("the card fills the available row", card.width > plus.width * 4)
        assertTrue("the + is to the right of the card", plus.left >= card.right)
    }

    @Test fun thePlusBetweenBlocksAddsAnExerciseAfterThatSlot() {
        show(); rule.waitForIdle()
        rule.onNodeWithTag("add_after_squat").performScrollTo().performClick(); rule.waitForIdle(); rule.mainClock.advanceTimeBy(1_000); rule.waitForIdle()
        rule.onNodeWithTag("add_option_pushup-diamond").assertDoesNotExist()   // R7-T10 (D11): nothing is listed before a category is chosen
        rule.onNodeWithTag("add_cat_push").performSemanticsAction(click); rule.waitForIdle()
        rule.onNodeWithTag("add_option_squat-air").assertDoesNotExist()        // only that category's list
        rule.onNodeWithTag("add_option_pushup-diamond").performSemanticsAction(click); rule.waitForIdle()
        rule.onNodeWithTag("exercise_add-pushup-diamond").assertExists()
        val order = mutableListOf<String>()
        fun walk(n: androidx.compose.ui.semantics.SemanticsNode) { n.config.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.TestTag)?.let { order += it }; n.children.forEach { walk(it) } }
        walk(rule.onRoot().fetchSemanticsNode())
        assertTrue(order.indexOf("exercise_squat") < order.indexOf("exercise_add-pushup-diamond") && order.indexOf("exercise_add-pushup-diamond") < order.indexOf("exercise_pull"))
    }

    @Test fun specialistExerciseCategoriesRemainAvailableThroughManualAdd() {
        show(); rule.waitForIdle()
        rule.onNodeWithTag("add_start").performScrollTo().performClick(); rule.waitForIdle(); rule.mainClock.advanceTimeBy(1_000); rule.waitForIdle()
        for (category in listOf("squat", "conditioning", "prehab", "stretches")) {   // R8.2-T01: no Power category, the jumps are under Squat
            rule.onNodeWithTag("add_cat_$category").performScrollTo().assertExists()
        }
        rule.onNodeWithTag("add_cat_power").assertDoesNotExist()
        rule.onNodeWithTag("add_cat_squat").performSemanticsAction(click); rule.waitForIdle()
        rule.onNodeWithTag("add_option_jump-squat").performScrollTo().assertExists()
    }

    @Test fun theSwapSheetShowsTheWholeFamilyAndOtherTypesBehindAButton() {
        show(); rule.waitForIdle()
        openSwap("push")
        for (id in listOf("pushup-incline", "pushup-knee", "pushup-standard", "pushup-diamond")) rule.onNodeWithTag("swap_option_$id").assertExists()
        rule.onNodeWithTag("swap_option_squat-air").assertDoesNotExist()
        rule.onNodeWithTag("swap_other_types").performSemanticsAction(click); rule.waitForIdle()
        rule.onNodeWithTag("swap_option_squat-air").assertExists()
    }

    @Test fun tappingOtherTypesShowsTheSameFamilyGroupFirstInAnAccentBoxAndEveryOtherTabStays() {   // R7.2-T02 (F2, F3, O3)
        show(); rule.waitForIdle()
        openSwap("push")
        rule.onNodeWithTag("swap_same_family").assertDoesNotExist()
        rule.onNodeWithTag("swap_other_types").performSemanticsAction(click); rule.waitForIdle(); rule.mainClock.advanceTimeBy(1_000); rule.waitForIdle()
        rule.onNodeWithTag("swap_same_family").assertExists()
        rule.onNodeWithTag("swap_same_family_caption", useUnmergedTree = true).assertTextContains("SAME FAMILY: PUSH", ignoreCase = true)
        rule.onNodeWithTag("swap_same_family").assertIsDisplayed()       // the sheet scrolled to the first new item
        for (tab in listOf("pull", "row", "squat", "hips", "core", "calves")) rule.onNodeWithTag("swap_group_$tab", useUnmergedTree = true).assertExists()
        rule.onNodeWithTag("swap_option_squat-air").assertExists()
        rule.onNodeWithTag("swap_other_types").performSemanticsAction(click); rule.waitForIdle()    // closes again
        rule.onNodeWithTag("swap_same_family").assertDoesNotExist()
    }

    // ---- R7-T10 (D4): the full timeline is the editor (it replaces the V17 "Detailed edit" switch)
    @Test fun theFullTimelineListsEverySetWithItsNumberAndAStretchCanDifferPerSet() {
        show(); rule.waitForIdle()
        rule.onNodeWithTag("detailed_switch").assertDoesNotExist()
        rule.onNodeWithTag("timeline_toggle").performScrollTo().performClick(); rule.waitForIdle()
        for (st in 1..4) rule.onNodeWithTag("set_header_$st").performScrollTo().assertExists()
        rule.onNodeWithTag("round_badge_push_s3", useUnmergedTree = true).performScrollTo().assertTextEquals("3")
        rule.onNodeWithTag("break_push_s2").performScrollTo().performClick(); rule.waitForIdle(); rule.mainClock.advanceTimeBy(1_000); rule.waitForIdle()
        rule.onNodeWithTag("break_option_stretch-frog").performSemanticsAction(click); rule.waitForIdle()
        rule.onNodeWithTag("break_push_s2").performScrollTo().assertTextContains("Frog", substring = true)
        rule.onNodeWithTag("break_push_s3").performScrollTo().assert(!hasText("Frog", substring = true)) // the other sets keep their own (automatic) stretch
    }

    // The "type a number" dialog has a text field. Robolectric's Compose clock never goes idle while a dialog with a text field is open
    // (the test hangs for minutes), so the dialog itself is not driven here. What it produces is covered by the planner tests
    // (DetailedEditTest: the free number is marked, shown, and excluded from progression) and the target cell's tag is checked.
    @Test fun inTheFullTimelineEachSetsNumberIsATapTargetAndEveryTileHasItsControls() {
        show(); rule.waitForIdle()
        rule.onNodeWithTag("timeline_toggle").performScrollTo().performClick(); rule.waitForIdle()
        for (st in 1..4) rule.onNodeWithTag("exercise_target_push_s$st").performScrollTo().assertHasClickAction()
        rule.onNodeWithTag("level_push_s2").performScrollTo().assertTextContains("Level", substring = true)
        for (t in listOf("swap_push_s2", "level_plus_push_s2", "level_minus_push_s2")) rule.onNodeWithTag(t).performScrollTo().assertExists()
    }

    // R7-T10 (O1, D12): under a range rule the stepper steps through the exercises of the chain and the numbers stay the range
    @Test fun underARepRangeTheStepperMovesToTheNextExerciseOfTheChainAndKeepsTheRange() {
        show(); rule.waitForIdle()
        rule.onNodeWithTag("rule_REP_RANGE").performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("rule_range_note").assertExists()
        val nameBefore = rule.onNodeWithTag("exercise_name_push").fetchSemanticsNode().config.first { it.key.name == "Text" }.value.let { (it as List<*>).first().toString() }
        val targetBefore = rule.onNodeWithTag("exercise_target_push").fetchSemanticsNode().config.first { it.key.name == "Text" }.value.let { (it as List<*>).first().toString() }
        rule.onNodeWithTag("level_plus_push").performScrollTo().performClick(); rule.waitForIdle()
        val nameAfter = rule.onNodeWithTag("exercise_name_push").fetchSemanticsNode().config.first { it.key.name == "Text" }.value.let { (it as List<*>).first().toString() }
        assertEquals("Incline Push-Up", nameBefore)
        assertEquals("Standard Push-Up", nameAfter)
        assertNotEquals(nameBefore, nameAfter)
        val targetAfter = rule.onNodeWithTag("exercise_target_push").fetchSemanticsNode().config.first { it.key.name == "Text" }.value.let { (it as List<*>).first().toString() }
        assertTrue(targetBefore.contains("5–8 reps"))
        assertTrue(targetAfter.contains("5–8 reps"))
        assertFalse("standard push-ups use a bilateral target", targetAfter.endsWith("per side"))
        rule.onNodeWithTag("level_push").assertTextContains("Exercise", substring = true)
    }

    // ---- V21: the progression rule picker
    @Test fun theRuleDefaultsToAppLevelsAndRepRangeShowsRangesAndThreeSets() {
        show(); rule.waitForIdle()
        rule.onNodeWithTag("rule_APP_LEVELS").performScrollTo().assertIsSelected()
        rule.onNodeWithTag("rule_REP_RANGE").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("rule_REP_RANGE").assertIsSelected()
        rule.onNodeWithTag("rule_hint").assertTextContains("3 sets of 5–8 reps", substring = true)
        rule.onNodeWithTag("exercise_target_squat").performScrollTo().assertTextContains("5–8 reps", substring = true)
        rule.onNodeWithTag("preview_header").assertTextContains("3 rounds", substring = true)   // the rule's sets
        rule.onNodeWithTag("rule_sets_value").assertDoesNotExist()
    }

    @Test fun customHasEditableSetsRangeAndSessions() {
        show(); rule.waitForIdle()
        rule.onNodeWithTag("rule_CUSTOM").performScrollTo().performClick(); rule.waitForIdle()
        for (t in listOf("rule_sets", "rule_from", "rule_to", "rule_sessions")) rule.onNodeWithTag("${t}_value").assertExists()
        rule.onNodeWithTag("rule_to_plus").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("rule_hint").assertTextContains("5–9", substring = true)
        rule.onNodeWithTag("rule_sessions_plus").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("rule_hint").assertTextContains("2 session", substring = true)
        rule.onNodeWithTag("rule_APP_LEVELS").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("rule_sets_value").assertDoesNotExist()
    }
}
