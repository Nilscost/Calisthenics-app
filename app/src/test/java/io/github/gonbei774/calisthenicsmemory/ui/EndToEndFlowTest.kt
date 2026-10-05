package io.github.gonbei774.calisthenicsmemory.ui

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import io.github.gonbei774.calisthenicsmemory.session.SessionBus
import io.github.gonbei774.calisthenicsmemory.session.WorkoutSessionService
import io.github.gonbei774.calisthenicsmemory.ui.nav.AppNav
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalisthenicsMemoryTheme
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** The whole first-run path through the real navigation: questionnaire -> Train -> Preview -> Start. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class EndToEndFlowTest {
    @get:Rule val rule = createComposeRule()
    private val app: Application get() = ApplicationProvider.getApplicationContext()

    @Before fun fresh() {
        for (n in listOf("profiles", "mode", "planner_prefs", "train", "goal", "levels", "onboarding", "routine"))
            app.getSharedPreferences(n, Context.MODE_PRIVATE).edit().clear().commit()
        SessionBus.clear()
    }

    private fun settle() { rule.waitForIdle(); rule.mainClock.advanceTimeBy(800); rule.waitForIdle() }

    @Test fun freshInstallToAWorkoutThatStarts() {
        rule.setContent { CalisthenicsMemoryTheme(darkTheme = false) { AppNav() } }
        settle()
        // questionnaire: welcome, goal, equipment, six families answered with "Normal", style, summary
        rule.onNodeWithTag("onb_title").assertTextEquals("Welcome")
        repeat(3) { rule.onNodeWithTag("onb_next").performClick(); settle() }
        repeat(6) {
            rule.onNodeWithTag("onb_dont_know").performClick(); rule.onNodeWithTag("onb_guess_NORMAL").performClick(); rule.onNodeWithTag("onb_next").performClick(); settle()
        }
        rule.onNodeWithTag("onb_next").performClick(); settle()   // style page
        rule.onNodeWithTag("onb_finish").performClick(); settle()
        // Train tab with the four tabs, the default plan, one Preview button
        rule.onNodeWithTag("bottom_bar").assertIsDisplayed()
        rule.onNodeWithTag("preview_button").assertIsEnabled().performClick(); settle()
        rule.onNodeWithTag("preview_header").assertIsDisplayed()
        rule.onNodeWithTag("start_button").performClick(); settle()
        val intent = shadowOf(app).nextStartedService
        assertNotNull(intent); assertEquals(WorkoutSessionService.ACTION_START, intent.action)
        rule.onNodeWithText("Starting…").assertIsDisplayed()   // the session screen waits for the service
        assertFalse("the tab bar is hidden during a workout", rule.onAllNodesWithTag("bottom_bar").fetchSemanticsNodes().isNotEmpty())
    }

    @Test fun anExistingInstallGoesStraightToTrainAndKeepsItsLevels() {
        io.github.gonbei774.calisthenicsmemory.ui.screens.LevelStore.save(app, mapOf("pushup-standard" to 4))
        rule.setContent { CalisthenicsMemoryTheme(darkTheme = false) { AppNav() } }
        settle()
        rule.onNodeWithTag("preview_button").assertIsDisplayed()
        rule.onAllNodesWithTag("onb_title").assertCountEquals(0)
        assertEquals(4, io.github.gonbei774.calisthenicsmemory.ui.screens.LevelStore.load(app)["pushup-standard"])
    }
}
