package io.github.gonbei774.calisthenicsmemory.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import app.calisthenics.domain.equipment.SeedProfiles
import app.calisthenics.domain.model.WorkoutFormat
import app.calisthenics.domain.model.BetweenSetMode
import app.calisthenics.domain.model.BlockType
import app.calisthenics.domain.planner.FixedProgramPlanResult
import app.calisthenics.domain.planner.buildFixedProgramPlan
import app.calisthenics.domain.planner.buildProgramPlan
import app.calisthenics.domain.planner.programContentOf
import app.calisthenics.domain.routine.settingsForProgram
import io.github.gonbei774.calisthenicsmemory.ui.screens.ProfileStore
import io.github.gonbei774.calisthenicsmemory.ui.screens.SavedRoutineStore
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalisthenicsMemoryTheme
import io.github.gonbei774.calisthenicsmemory.ui.train.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** R8.1-T08 (F9): a program opens in the Preview as a starting point; everything can be edited; saving never changes the program. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class ProgramPreviewTest {
    @get:Rule val rule = createComposeRule()
    private val ctx: Context get() = ApplicationProvider.getApplicationContext()
    private val catalog by lazy { loadCatalog(ctx) }

    @Before fun clean() {
        listOf("profiles", "mode", "planner_prefs", "train", "goal", "levels", "saved_routines", "preview_draft", "routine", "personal_targets").forEach { ctx.getSharedPreferences(it, 0).edit().clear().commit() }
    }

    /** A program the Home profile can do. */
    private val program get() = catalog.fixedPrograms.first { it.id == "army-ssd" }   // a short building block keeps the Robolectric screen small; the merged sessions have the same screen
    private fun open(p: app.calisthenics.domain.model.FixedProgram = program) {
        TrainSettingsStore.save(ctx, settingsForProgram(p, TrainSettingsStore.load(ctx)))
        rule.setContent { CalisthenicsMemoryTheme(darkTheme = false) { PreviewScreen(onBack = {}, onStarted = {}) } }
        rule.waitForIdle()
    }

    @Test fun theProgramOpensAsAListWithItsDescriptionAndTheSameControlsAsAnyWorkout() {
        open()
        rule.onNodeWithTag("program_title").assertTextEquals(program.name)
        rule.onNodeWithTag("program_start_point").assertExists()
        program.items.indices.forEach { rule.onNodeWithTag("program_item_$it").performScrollTo().assertExists() }
        for (f in listOf("CIRCUIT", "PAIRS", "STRAIGHT")) rule.onNodeWithTag("preview_format_$f").performScrollTo().assertExists()
        rule.onNodeWithTag("preview_flow_switch").assertDoesNotExist()
        rule.onNodeWithTag("preparation_seconds_value").performScrollTo().assertExists()
        rule.onNodeWithTag("start_button").assertIsEnabled()
        assertEquals(program.flow, TrainSettingsStore.load(ctx).flow)
    }

    @Test fun programPreviewModeEditsStaySynchronizedAndBuildMatchingBlocks() {
        open()
        val profile = ProfileStore.selected(ctx)
        for (mode in BetweenSetMode.entries) {
            rule.onNodeWithTag("between_${mode.name.lowercase()}").performScrollTo().performClick(); rule.waitForIdle()
            assertEquals(mode, TrainSettingsStore.load(ctx).effectiveBetweenSetMode())
            assertEquals(mode, PreviewDraftStore.get(ctx, draftKey(TrainSettingsStore.load(ctx)))?.betweenSetMode)
            val content = PreviewDraftStore.get(ctx, draftKey(TrainSettingsStore.load(ctx)))?.program ?: programContentOf(program)
            val result = buildProgramPlan(catalog, content, profile, mode == BetweenSetMode.FLOW,
                1, WorkoutFormat.CIRCUIT, 1L, mode, 5) as FixedProgramPlanResult.Ready
            assertEquals(mode, result.plan.betweenSetMode)
            when (mode) {
                BetweenSetMode.REST -> assertTrue(result.plan.blocks.any { it.type == BlockType.PASSIVE_RECOVERY })
                BetweenSetMode.STRETCH -> assertTrue(result.plan.blocks.any { it.type == BlockType.STRETCH })
                BetweenSetMode.FLOW -> assertFalse(result.plan.blocks.any { it.type == BlockType.PASSIVE_RECOVERY })
            }
        }
    }

    @Test fun targetsCanBeChangedItemsRemovedAndAddedAndTheProgramInTheCatalogStaysAsItWas() {
        open()
        val first = program.items.first()
        val before = (first.reps ?: first.seconds)!!
        rule.onNodeWithTag("program_target_0_plus").performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("program_target_0_value").assertTextEquals(if (first.reps != null) "12 x" else "${before + 5} s")
        rule.onNodeWithTag("program_remove_1").performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("program_item_${program.items.size - 1}").assertDoesNotExist()
        rule.onNodeWithTag("program_add_after_0").performScrollTo().performClick(); rule.waitForIdle(); rule.mainClock.advanceTimeBy(1_000); rule.waitForIdle()
        rule.onNodeWithTag("add_cat_stretches").performSemanticsAction(SemanticsActions.OnClick); rule.waitForIdle()
        val stretch = catalog.variations.first { it.kind == app.calisthenics.domain.model.Kind.STRETCH && !it.optIn && it.id !in program.items.map { i -> i.exerciseId } && app.calisthenics.domain.equipment.isAvailable(it, SeedProfiles.home) }
        rule.onNodeWithTag("add_stretch_${stretch.id}").performSemanticsAction(SemanticsActions.OnClick); rule.waitForIdle()
        rule.onNodeWithTag("program_item_name_1").assertTextEquals(stretch.name)
        rule.onNodeWithTag("edits_reset").performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("program_target_0_value").assertTextEquals("$before x")
        assertEquals(first, catalog.fixedPrograms.first { it.id == program.id }.items.first())
    }

    @Test fun programStepperUsesAppLevelsThenExtendsLevelFiveAndReturnsAtTheBoundary() {
        open()
        repeat(3) { rule.onNodeWithTag("program_target_0_plus").performScrollTo().performClick(); rule.waitForIdle() }
        rule.onNodeWithTag("program_target_0_value").assertTextEquals("20 x")
        rule.onNodeWithTag("program_target_0_plus").performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("program_target_0_value").assertTextEquals("21 x")
        rule.onNodeWithTag("program_level_0").assertTextContains("Level 5", substring = true)
        rule.onNodeWithTag("program_target_0_plus").performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("program_target_0_value").assertTextEquals("22 x")
        rule.onNodeWithTag("program_target_0_minus").performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("program_target_0_value").assertTextEquals("21 x")
        rule.onNodeWithTag("program_target_0_minus").performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("program_target_0_value").assertTextEquals("20 x")
        rule.onNodeWithTag("program_target_0_minus").performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("program_target_0_value").assertTextEquals("15 x")
    }

    @Test fun programHoldExtensionUsesFiveSecondStepsAndReturnsToLevelFourAtTheBoundary() {
        val timed = catalog.fixedPrograms.first { it.id == "army-prt" }
        open(timed)
        val index = timed.items.indexOfFirst { it.exerciseId == "bent-leg-raise-army" }
        assertTrue(index >= 0)
        rule.onNodeWithTag("program_target_${index}_plus").performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("program_target_${index}_value").assertTextEquals("65 s")
        rule.onNodeWithTag("program_target_${index}_minus").performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("program_target_${index}_value").assertTextEquals("60 s")
        rule.onNodeWithTag("program_target_${index}_minus").performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("program_target_${index}_value").assertTextEquals("45 s")
    }

    @Test fun savedProgramExtensionDisplaysTheNewPersonalBoundaryAfterTargetsAreRaised() {
        val p = program
        val first = p.items.first()
        io.github.gonbei774.calisthenicsmemory.ui.screens.PersonalTargetStore.save(ctx, first.exerciseId, listOf(10, 15, 20, 25, 30))
        val settings = settingsForProgram(p, TrainSettingsStore.load(ctx))
        val content = app.calisthenics.domain.planner.programContentOf(p).copy(items = listOf(first.copy(reps = 21, appLevel = 5, levelFiveTarget = 21)))
        PreviewDraftStore.put(ctx, io.github.gonbei774.calisthenicsmemory.ui.train.PreviewDraft(draftKey(settings), program = content))
        open(p)
        rule.onNodeWithTag("program_target_0_value").performScrollTo().assertTextEquals("30 x")
        rule.onNodeWithTag("program_target_0_plus").performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("program_target_0_value").assertTextEquals("31 x")
    }

    @Test fun formatSetsPreparationAndProgramRestAreAdjustable() {
        open()
        rule.onNodeWithTag("preview_format_STRAIGHT").performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("preview_rounds_plus").performScrollTo().performClick(); rule.waitForIdle()
        val s = TrainSettingsStore.load(ctx)
        assertEquals(WorkoutFormat.STRAIGHT, s.format); assertEquals(2, s.rounds)
        val minutesBefore = rule.onNodeWithTag("preview_header").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.Text].first().text
        rule.onNodeWithTag("preparation_seconds_plus").performScrollTo().performClick(); rule.waitForIdle()
        assertEquals(6, TrainSettingsStore.load(ctx).preparationSeconds)
        rule.onNodeWithTag("program_rest_plus").performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("program_rest_value").assertTextEquals("0:25")
        assertNotNull(minutesBefore)
    }

    @Test fun editsSurviveLeavingAndComingBackAndTheProgramInTheCatalogIsUntouched() {
        var visible by mutableStateOf(true)
        TrainSettingsStore.save(ctx, settingsForProgram(program, TrainSettingsStore.load(ctx)))
        rule.setContent { CalisthenicsMemoryTheme(darkTheme = false) { if (visible) PreviewScreen(onBack = {}, onStarted = {}) else androidx.compose.foundation.layout.Box {} } }
        rule.waitForIdle()
        rule.onNodeWithTag("program_remove_0").performScrollTo().performClick(); rule.waitForIdle()
        visible = false; rule.waitForIdle(); visible = true; rule.waitForIdle()
        rule.onNodeWithTag("program_item_${program.items.size - 1}").assertDoesNotExist()   // still one item fewer
        // saving (the dialog window does not idle under Robolectric; the saved content and the settings switch are in ProgramPreviewPlanTest and the Maestro flow)
        rule.onNodeWithTag("save_routine_button").assertIsEnabled()
        assertEquals(program.items.size - 1, PreviewDraftStore.get(ctx, draftKey(TrainSettingsStore.load(ctx)))!!.program!!.items.size)
        assertEquals(program.items.size, catalog.fixedPrograms.first { it.id == program.id }.items.size)   // the program is untouched
    }

    @Test fun anItemTheEquipmentCannotDoBlocksStartUntilItIsRemoved() {
        ProfileStore.select(ctx, "travel")
        val pilates = catalog.fixedPrograms.first { it.id == "pilates-mat-beginner" }
        open(pilates)
        rule.onNodeWithTag("program_unavailable").performScrollTo().assertExists()
        rule.onNodeWithTag("start_button").assertIsNotEnabled()
        val missing = (buildFixedProgramPlan(catalog, pilates, SeedProfiles.travel) as FixedProgramPlanResult.Unavailable).items.map { it.exerciseId }.toSet()
        pilates.items.forEachIndexed { i, item -> if (item.exerciseId in missing) rule.onNodeWithTag("program_item_unavailable_$i").performScrollTo().assertExists() }
    }
}
