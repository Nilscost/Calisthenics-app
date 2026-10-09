package io.github.gonbei774.calisthenicsmemory.ui

import android.content.Context
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import io.github.gonbei774.calisthenicsmemory.ui.progress.ProgressScreen
import io.github.gonbei774.calisthenicsmemory.ui.screens.GoalStore
import io.github.gonbei774.calisthenicsmemory.ui.screens.LevelStore
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalisthenicsMemoryTheme
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class ProgressScreenTest {
    @get:Rule val rule = createComposeRule()
    private val ctx: Context get() = ApplicationProvider.getApplicationContext()

    @Before fun clean() {
        for (n in listOf("profiles", "planner_prefs", "goal", "levels", "train"))
            ctx.getSharedPreferences(n, Context.MODE_PRIVATE).edit().clear().commit()
    }

    private fun show() { rule.setContent { CalisthenicsMemoryTheme(darkTheme = false) { ProgressScreen() } }; rule.waitForIdle() }
    private fun desc(tag: String) = rule.onNodeWithTag(tag).fetchSemanticsNode().config.first { it.key.name == "ContentDescription" }.value.let { (it as List<*>).first().toString() }

    @Test fun pushTreeShowsNodesWithStarsAndStates() {
        LevelStore.save(ctx, mapOf("pushup-standard" to 3))
        show()
        assertTrue(desc("tree_node_pushup-standard").contains("2 of 5 stars")) // D8: the two levels below the starting level count
        assertTrue(desc("tree_node_pushup-standard").contains("Training now"))
        assertTrue(desc("tree_node_pushup-incline").contains("Ready to start") || desc("tree_node_pushup-incline").contains("Needs equipment"))
        assertTrue(desc("tree_node_pushup-one-arm").contains("Locked"))
        rule.onNodeWithTag("tree_node_planche-lean").assertExists() // the planche chain sits under the push-ups
    }

    @Test fun theGoalPicksTheFamilyTabAndOutlinesItsChain() {
        GoalStore.save(ctx, "hspu")
        show()
        rule.onNodeWithTag("tree_node_hspu-wall").assertExists()
        rule.onNodeWithTag("tree_node_pike-pushup").assertExists()
        rule.onNodeWithText("Goal: Handstand push-up").assertIsDisplayed()
    }

    @Test fun familyPickerSwitchesTrees() {
        show()
        rule.onNodeWithTag("tab_chip_squat").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("tree_node_squat-air").assertExists()
        rule.onNodeWithTag("tree_node_pushup-standard").assertDoesNotExist()
        rule.onNodeWithTag("tab_chip_row").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("tree_node_archer-row").assertExists() // F15: the band row now leads somewhere
        rule.onNodeWithTag("tab_chip_core").performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("tree_node_dead-bug").assertExists()
    }

    @Test fun firstTapSelectsTheNodeAndOpensThePanelAboveTheTabBar() {
        LevelStore.save(ctx, mapOf("pushup-standard" to 3))
        show()
        rule.onNodeWithTag("node_panel").assertDoesNotExist()
        rule.onNodeWithTag("tree_node_pushup-standard").performSemanticsAction(SemanticsActions.OnClick); rule.waitForIdle()
        rule.onNodeWithTag("node_panel").assertIsDisplayed()
        rule.onNodeWithTag("panel_title").assertTextEquals("Standard Push-Up")
        rule.onNodeWithTag("panel_level").assertTextContains("Level 3 of 5", substring = true)
        rule.onNodeWithTag("panel_unlocks").assertExists()
        rule.onNodeWithTag("panel_open_detail").assertIsDisplayed()
        rule.onNodeWithTag("panel_close").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("node_panel").assertDoesNotExist()
    }

    @Test fun theExerciseDetailShowsTheFiveLevelsUnlocksAndCautionsWithoutActionButtons() {
        LevelStore.save(ctx, mapOf("pushup-standard" to 3))
        show()
        rule.onNodeWithTag("tree_node_pushup-standard").performSemanticsAction(SemanticsActions.OnClick); rule.waitForIdle()
        rule.onNodeWithTag("panel_open_detail").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("exercise_detail").assertIsDisplayed()
        rule.onNodeWithTag("detail_title").assertTextEquals("STANDARD PUSH-UP")
        rule.onNodeWithTag("detail_breadcrumb").assertTextContains("STEP 3 OF 5", substring = true, ignoreCase = true)
        (1..5).forEach { rule.onNodeWithTag("detail_level_$it").performScrollTo().assertExists() }
        rule.onNodeWithTag("detail_level_3").assertTextContains("NOW", substring = true)
        rule.onNodeWithTag("detail_level_1").assertTextContains("DONE", substring = true)
        rule.onNodeWithTag("detail_unlocks").performScrollTo().assertExists()
        rule.onNodeWithTag("detail_caution").performScrollTo().assertExists()
        rule.onNodeWithTag("detail_body_map").performScrollTo().assertExists()
        rule.onNodeWithText("Swap", substring = true, ignoreCase = true).assertDoesNotExist()
        rule.onNodeWithTag("detail_back").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("exercise_detail").assertDoesNotExist()
        rule.onNodeWithTag("tree_node_pushup-standard").assertExists()
    }

    @Test fun byTypeAndBySkillAreSwitchedUnderTheTitleAndSkillsListTheirChain() {
        show()
        rule.onNodeWithTag("progress_title").assertTextEquals("PROGRESS")
        rule.onNodeWithTag("progress_mode_type").assertIsSelected()
        rule.onNodeWithTag("progress_mode_skill").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("progress_mode_skill").assertIsSelected()
        rule.onNodeWithTag("skill_chip_muscle-up").performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("tree_node_pullup-band-assisted").assertExists()
        rule.onNodeWithTag("tree_node_muscle-up-bar").assertExists()
        rule.onNodeWithTag("tree_node_pushup-standard").assertDoesNotExist()
        rule.onNodeWithTag("tree_node_muscle-up-bar").performSemanticsAction(SemanticsActions.OnClick); rule.waitForIdle()
        rule.onNodeWithTag("panel_title").assertTextEquals("Bar Muscle-Up")
    }

    @Test fun exercisesBelowTheCurrentOneShowFiveStars() {
        LevelStore.save(ctx, mapOf("pushup-standard" to 2, "pushup-feet-elevated" to 1))
        show()
        assertTrue(desc("tree_node_pushup-standard"), desc("tree_node_pushup-standard").contains("5 of 5 stars"))
        assertTrue(desc("tree_node_pushup-knee").contains("5 of 5 stars"))
    }

    @Test fun treeNodesAreVerticalSoLowerExercisesAreBelowEasierOnes() {
        show()
        val y = { t: String -> rule.onNodeWithTag(t).fetchSemanticsNode().boundsInRoot.top }
        assertTrue("${y("tree_node_pushup-incline")} < ${y("tree_node_pushup-standard")}", y("tree_node_pushup-incline") < y("tree_node_pushup-standard"))
        assertTrue("${y("tree_node_pushup-standard")} < ${y("tree_node_pushup-feet-elevated")}", y("tree_node_pushup-standard") < y("tree_node_pushup-feet-elevated"))
    }

    @Test fun stretchesHaveTheirOwnTab() {
        show()
        rule.onNodeWithTag("tab_chip_stretches").performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("stretch_stretch-hamstring").assertExists()
    }
}
