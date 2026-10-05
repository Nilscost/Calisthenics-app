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
        assertTrue(desc("tree_node_pushup-standard").contains("0 of 5 stars"))
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

    @Test fun tappingANodeOpensTheSheetWithTheFiveSteps() {
        LevelStore.save(ctx, mapOf("pushup-standard" to 3))
        show()
        rule.onNodeWithTag("tree_node_pushup-standard").performSemanticsAction(SemanticsActions.OnClick); rule.waitForIdle()
        rule.mainClock.advanceTimeBy(1000); rule.waitForIdle()
        rule.onNodeWithTag("sheet_title").assertTextEquals("Standard Push-Up")
        (1..5).forEach { rule.onNodeWithTag("sheet_step_$it").assertExists() }
        rule.onNodeWithTag("sheet_step_3").assertTextContains("you are here", substring = true)
        rule.onNodeWithText("Cautions").assertExists()
    }

    @Test fun stretchesHaveTheirOwnTab() {
        show()
        rule.onNodeWithTag("tab_chip_stretches").performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("stretch_stretch-hamstring").assertExists()
    }
}
