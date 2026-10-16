package io.github.gonbei774.calisthenicsmemory.ui

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import io.github.gonbei774.calisthenicsmemory.ui.screens.ProfileStore
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalisthenicsMemoryTheme
import io.github.gonbei774.calisthenicsmemory.ui.train.ProgramsScreen
import io.github.gonbei774.calisthenicsmemory.ui.train.TrainSettingsStore
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class ProgramsScreenTest {
    @get:Rule val rule = createComposeRule()
    private val ctx: Context get() = ApplicationProvider.getApplicationContext()

    @Before fun clean() {
        listOf("profiles", "mode", "planner_prefs", "train", "goal", "levels").forEach { ctx.getSharedPreferences(it, 0).edit().clear().commit() }
    }

    private fun show() = rule.setContent { CalisthenicsMemoryTheme(darkTheme = false) { ProgramsScreen(onBack = {}, onStarted = {}) } }

    @Test fun catalogProgramsAreListedExactlyOnceAndFlowChoicePersistsPerProgram() {
        show(); rule.waitForIdle()
        val catalog = io.github.gonbei774.calisthenicsmemory.ui.train.loadCatalog(ctx)
        assertEquals(8, catalog.fixedPrograms.size)
        catalog.fixedPrograms.forEach { rule.onNodeWithTag("program_${it.id}").performScrollTo().assertExists() }
        val program = catalog.fixedPrograms.first { it.flow }
        rule.onNodeWithTag("program_open_${program.id}").performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("program_flow_${program.id}").performScrollTo()
        val initial = rule.onNodeWithTag("program_flow_${program.id}").fetchSemanticsNode().config
        rule.onNodeWithTag("program_flow_${program.id}").performClick(); rule.waitForIdle()
        val after = TrainSettingsStore.load(ctx).fixedProgramFlowOverrides[program.id]
        assertNotEquals("flow override is stored instead of changing the contract default", program.flow, after)
        assertNotEquals(initial, rule.onNodeWithTag("program_flow_${program.id}").fetchSemanticsNode().config)
    }

    @Test fun programListIsCompactAndDetailsKeepTheAuthoredSequence() {
        show(); rule.waitForIdle()
        val catalog = io.github.gonbei774.calisthenicsmemory.ui.train.loadCatalog(ctx)
        val program = catalog.fixedPrograms.first()
        rule.onNodeWithTag("program_summary_${program.id}").assertExists()
        rule.onNodeWithTag("program_line_${program.id}_0").assertDoesNotExist()
        rule.onNodeWithTag("program_open_${program.id}").performClick(); rule.waitForIdle()
        program.items.forEachIndexed { index, item ->
            rule.onNodeWithTag("program_line_${program.id}_$index").assertExists()
        }
        rule.onNodeWithTag("program_start_${program.id}").assertIsEnabled()
    }

    @Test fun displayedProgramDurationMatchesTheReadyPlan() {
        val catalog = io.github.gonbei774.calisthenicsmemory.ui.train.loadCatalog(ctx)
        val profile = ProfileStore.selected(ctx)
        val pair = catalog.fixedPrograms.firstNotNullOfOrNull { program ->
            val result = app.calisthenics.domain.planner.buildFixedProgramPlan(catalog, program, profile)
            (result as? app.calisthenics.domain.planner.FixedProgramPlanResult.Ready)?.let { program to it }
        } ?: return
        assertEquals(pair.second.plan.plannedDurationSeconds, io.github.gonbei774.calisthenicsmemory.ui.train.fixedProgramDurationSeconds(catalog, pair.first, pair.second))
        show(); rule.waitForIdle()
        val minutes = (pair.second.plan.plannedDurationSeconds + 59) / 60
        rule.onNodeWithTag("program_summary_meta_${pair.first.id}", useUnmergedTree = true).assertTextContains("$minutes min", substring = true)
    }

    @Test fun selectedProfileBlocksAnUnavailableProgramAsAWhole() {
        ProfileStore.select(ctx, "travel")
        show(); rule.waitForIdle()
        val catalog = io.github.gonbei774.calisthenicsmemory.ui.train.loadCatalog(ctx)
        val pilates = catalog.fixedPrograms.first { it.id == "pilates-mat-beginner" }
        rule.onNodeWithTag("program_open_${pilates.id}").performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("program_unavailable_${pilates.id}").performScrollTo().assertExists()
        rule.onNodeWithTag("program_start_${pilates.id}").performScrollTo().assertIsNotEnabled()
        val army = catalog.fixedPrograms.first { it.id == "army-hsd" }
        rule.onNodeWithTag("program_open_${army.id}").performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("program_start_${army.id}").performScrollTo().assertIsEnabled()
    }
}
