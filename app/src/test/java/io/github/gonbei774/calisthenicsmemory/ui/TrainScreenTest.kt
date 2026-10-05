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
        for (tag in listOf("goal_field", "profile_plus", "rounds_plus", "style_timed", "focus_CORE", "stretch_switch", "preview_button"))
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

    @Test fun styleStretchAndFocusAreOneTapAndPersist() {
        show()
        rule.onNodeWithTag("style_timed").performClick()
        rule.onNodeWithTag("stretch_switch").performClick()
        rule.onNodeWithTag("focus_UPPER_BODY").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("style_timed").assertIsSelected()
        rule.onNodeWithTag("stretch_switch").assertIsOff()
        rule.onNodeWithTag("focus_FULL_BODY").assertIsOff()
        rule.onNodeWithTag("focus_UPPER_BODY").assertIsOn()
        assertTrue(ctx.getSharedPreferences("mode", Context.MODE_PRIVATE).getBoolean("timed", false))
    }

    @Test fun goalDropdownListsSkillsAndBodyFocusAndSwitchesTheFocus() {
        show()
        rule.onNodeWithTag("goal_field").performClick()
        rule.onNodeWithText("Skills").assertIsDisplayed()
        rule.onNodeWithText("Body focus").assertIsDisplayed()
        rule.onNodeWithTag("goal_body-core").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("focus_CORE").assertIsOn()
        rule.onNodeWithTag("focus_FULL_BODY").assertIsOff()
    }

    @Test fun profilesAreChipsWithAPlus() {
        show()
        rule.onNodeWithTag("profile_chip_home").assertIsSelected()
        rule.onNodeWithTag("profile_chip_travel").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("profile_chip_travel").assertIsSelected()
        rule.onNodeWithTag("profile_plus").assertIsDisplayed()
    }

    @Test fun trainIsTheDefaultTabAndPreviewIsReachable() {
        rule.setContent { CalisthenicsMemoryTheme(darkTheme = false) { AppNav() } }
        rule.onNodeWithTag("preview_button").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("start_button").assertIsDisplayed()
        rule.onNodeWithTag("preview_header").assertIsDisplayed()
    }
}
