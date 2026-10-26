package io.github.gonbei774.calisthenicsmemory.ui

import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import app.calisthenics.domain.model.*
import io.github.gonbei774.calisthenicsmemory.ui.components.targetLabel
import io.github.gonbei774.calisthenicsmemory.ui.train.circuitOf
import io.github.gonbei774.calisthenicsmemory.ui.train.programSideText
import io.github.gonbei774.calisthenicsmemory.ui.train.targetText
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Owner 2026-10-26: totals with the per-side number in parentheses for one-block exercises; "left, then right" for two-block ones; never a lone "per side". */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class SideDisplayTest {
    @get:Rule val rule = createComposeRule()

    private fun shows(expected: String, text: @androidx.compose.runtime.Composable () -> String) {
        rule.setContent { Text(text(), Modifier.testTag("t")) }
        rule.onNodeWithTag("t").assertTextEquals(expected)
    }

    private val reps6 = Target(TargetType.REPS, 6)

    @Test fun alternatingRepsShowTheTotalWithPerSideInParentheses() = shows("12 reps (6 per side)") { targetLabel(reps6, alternating = true) }
    @Test fun alternatingHoldShowsTotalSecondsWithPerSide() = shows("40 s (20 s per side)") { targetLabel(Target(TargetType.HOLD_SECONDS, 20), alternating = true) }
    @Test fun alternatingRangeDoublesBothEnds() = shows("10–16 reps (5–8 per side)") { targetLabel(Target(TargetType.REPS, 5, 8), alternating = true) }
    @Test fun plainTargetIsUnchanged() = shows("6 reps") { targetLabel(reps6, alternating = false) }
    @Test fun previewRowOfASplitExerciseSaysLeftThenRight() = shows("6 reps · left, then right") { targetText(reps6, perSide = true) }
    @Test fun previewRowOfAnAlternatingExerciseShowsTheTotal() = shows("12 reps (6 per side)") { targetText(reps6, perSide = false, alternating = true) }
    @Test fun previewRowWithLoadStillAppendsIt() = shows("12 reps (6 per side) · 12 kg") { targetText(reps6, perSide = false, loadGrams = 12000, alternating = true) }

    @Test fun circuitEntriesCarrySplitAndAlternatingApart() {
        val c = app.calisthenics.domain.content.parseCatalog(java.io.File(System.getProperty("user.dir"), "src/main/assets/catalog.json").readText())
        val content = app.calisthenics.domain.planner.ProgramContent("t", "t", listOf(
            FixedProgramItem("lunge-reverse", reps = 6, perSide = true, estSeconds = 24),
            FixedProgramItem("pushup-archer", reps = 6, perSide = true, estSeconds = 24),
            FixedProgramItem("walking-lunge", reps = 10, estSeconds = 48)))
        val plan = (app.calisthenics.domain.planner.buildProgramPlan(c, content, app.calisthenics.domain.equipment.SeedProfiles.home, flow = false)
            as app.calisthenics.domain.planner.FixedProgramPlanResult.Ready).plan
        val entries = circuitOf(plan)
        assertEquals(listOf(true, false, false), entries.map { it.perSide })
        assertEquals(listOf(false, true, false), entries.map { it.alternating })
    }

    @Test fun programRowDescribesWhatItsNumberMeans() {
        val c = app.calisthenics.domain.content.parseCatalog(java.io.File(System.getProperty("user.dir"), "src/main/assets/catalog.json").readText())
        fun item(id: String, reps: Int? = 6, seconds: Int? = null) = FixedProgramItem(id, reps = reps, seconds = seconds, perSide = true)
        rule.setContent {
            Text(programSideText(c.variation("lunge-reverse"), item("lunge-reverse"), 6), Modifier.testTag("split"))
            Text(programSideText(c.variation("pushup-archer"), item("pushup-archer"), 6), Modifier.testTag("alt"))
            Text(programSideText(c.variation("walking-lunge"), FixedProgramItem("walking-lunge", reps = 10), 10), Modifier.testTag("none"))
        }
        rule.onNodeWithTag("split").assertTextEquals("left, then right")
        rule.onNodeWithTag("alt").assertTextEquals("12 reps in all (6 per side)")
        rule.onNodeWithTag("none").assertTextEquals("")
    }
}
