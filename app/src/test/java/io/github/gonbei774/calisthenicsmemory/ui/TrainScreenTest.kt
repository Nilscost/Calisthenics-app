package io.github.gonbei774.calisthenicsmemory.ui

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import io.github.gonbei774.calisthenicsmemory.ui.nav.AppNav
import io.github.gonbei774.calisthenicsmemory.ui.screens.OnboardingStore
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalisthenicsMemoryTheme
import io.github.gonbei774.calisthenicsmemory.ui.train.TrainScreen
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi") // Galaxy S21 logical size
class TrainScreenTest {
    @get:Rule val rule = createComposeRule()
    private val ctx: Context get() = ApplicationProvider.getApplicationContext()

    @Before fun clean() {
        for (n in listOf("profiles", "mode", "planner_prefs", "train", "goal", "levels", "onboarding"))
            ctx.getSharedPreferences(n, Context.MODE_PRIVATE).edit().clear().commit()
        OnboardingStore.setDone(ctx)
    }

    private fun minutes(): Int = rule.onNodeWithTag("minutes").fetchSemanticsNode().config
        .first { it.key.name == "Text" }.value.let { (it as List<*>).first().toString() }.filter { c -> c.isDigit() }.toInt()

    private fun show(onPreview: () -> Unit = {}) = rule.setContent {
        CalisthenicsMemoryTheme(darkTheme = false) { TrainScreen(onPreview = onPreview, onStarted = {}, onEditProfile = {}) }
    }

    @Test fun everythingFitsOnOneScreenWithoutScrolling() {
        show()
        rule.waitForIdle()
        for (tag in listOf("goal_field", "profile_field", "rounds_plus", "between_rest", "minutes", "exercise_count", "preview_button", "train_start_button"))
            rule.onNodeWithTag(tag).assertIsDisplayed()
    }

    @Test fun previewButtonIsEnabledAndOpensThePreview() {
        var opened = false
        show { opened = true }
        rule.onNodeWithTag("preview_button").assertIsEnabled().performClick()
        assertTrue(opened)
    }

    @Test fun roundsStepperUpdatesTheEstimatedMinutes() {
        show()
        rule.waitForIdle()
        val at4 = minutes()
        assertTrue("4 rounds = $at4 min", at4 in 40..50)
        rule.onNodeWithTag("rounds_plus").performClick(); rule.waitForIdle()
        assertTrue(minutes() > at4)
        rule.onNodeWithTag("rounds_minus").performClick(); rule.onNodeWithTag("rounds_minus").performClick(); rule.waitForIdle()
        assertTrue(minutes() < at4)
    }

    @Test fun betweenSetsStretchOrRestPersists() {
        show()
        rule.onNodeWithTag("between_stretch").assertIsSelected()
        rule.onNodeWithTag("between_rest").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("between_rest").assertIsSelected()
        rule.onNodeWithTag("between_stretch").assertIsNotSelected()
    }

    @Test fun objectiveHasBodyPartAndSkillButRoutineIsHiddenUntilPresetsExist() {
        show()
        rule.onNodeWithTag("objective_type_BODY_PART").assertIsSelected()
        rule.onNodeWithTag("objective_type_SKILL").assertIsDisplayed()
        rule.onNodeWithTag("objective_type_ROUTINE").assertDoesNotExist()
    }

    @Test fun pickingTheSkillTypeChoosesASkillAndTheDropdownListsOnlySkills() {
        show()
        rule.onNodeWithTag("objective_type_SKILL").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("objective_type_SKILL").assertIsSelected()
        rule.onNodeWithTag("goal_field").performClick()
        rule.onNodeWithTag("goal_planche").assertIsDisplayed()
        rule.onNodeWithTag("goal_body-core").assertDoesNotExist()
    }

    @Test fun bodyPartDropdownSwitchesTheFocusThroughTheGoal() {
        show()
        rule.onNodeWithTag("goal_field").performClick()
        rule.onNodeWithTag("goal_body-core").performClick(); rule.waitForIdle()
        rule.onNodeWithText("Focus: core", substring = true).assertExists()
        assertEquals("body-core", ctx.getSharedPreferences("goal", Context.MODE_PRIVATE).all.values.firstOrNull { it == "body-core" })
    }

    @Test fun equipmentIsADropdownWithANewProfileEntry() {
        show()
        rule.onNodeWithTag("profile_field").assertIsDisplayed()
        rule.onNodeWithTag("profile_field").performClick()
        rule.onNodeWithTag("profile_chip_travel").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("profile_field").assertTextContains("Travel")
        rule.onNodeWithTag("profile_field").performClick()
        rule.onNodeWithTag("profile_plus").assertIsDisplayed()
    }

    @Test fun summaryTilesShowTimeAndExercisesAndStartIsNextToTheirPreview() {
        show(); rule.waitForIdle()
        assertTrue(minutes() in 40..50)
        rule.onNodeWithTag("exercise_count").assertIsDisplayed()
        rule.onNodeWithTag("train_start_button").assertIsEnabled().assertTextContains("START WORKOUT")
        rule.onNodeWithTag("preview_button").assertIsEnabled().assertTextContains("Workout preview")
        val sx = rule.onNodeWithTag("train_start_button").fetchSemanticsNode().boundsInRoot
        val px = rule.onNodeWithTag("preview_button").fetchSemanticsNode().boundsInRoot
        assertTrue("START is about twice as wide as the preview button", sx.width > px.width * 1.5f)
    }

    @Test fun trainIsTheDefaultTabAndPreviewIsReachable() {
        rule.setContent { CalisthenicsMemoryTheme(darkTheme = false) { AppNav() } }
        rule.onNodeWithTag("preview_button").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("start_button").assertIsDisplayed()
        rule.onNodeWithTag("preview_header").assertIsDisplayed()
    }
}
