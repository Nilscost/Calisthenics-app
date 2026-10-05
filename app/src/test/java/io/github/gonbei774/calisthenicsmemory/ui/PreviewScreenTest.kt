package io.github.gonbei774.calisthenicsmemory.ui

import android.app.Application
import android.content.Context
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
        rule.onNodeWithTag("swap_$slot").performClick(); rule.waitForIdle()
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
}
