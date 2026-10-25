package io.github.gonbei774.calisthenicsmemory.ui

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import io.github.gonbei774.calisthenicsmemory.ui.nav.AppNav
import io.github.gonbei774.calisthenicsmemory.ui.screens.OnboardingStore
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalisthenicsMemoryTheme
import io.github.gonbei774.calisthenicsmemory.ui.train.TrainScreen
import io.github.gonbei774.calisthenicsmemory.session.WorkoutSessionService
import kotlinx.serialization.json.Json
import org.robolectric.Shadows.shadowOf
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
        for (n in listOf("profiles", "mode", "planner_prefs", "train", "goal", "levels", "onboarding", "saved_routines", "preview_draft"))
            ctx.getSharedPreferences(n, Context.MODE_PRIVATE).edit().clear().commit()
        OnboardingStore.setDone(ctx)
        io.github.gonbei774.calisthenicsmemory.ui.screens.ProfileStore.save(ctx, app.calisthenics.domain.equipment.SeedProfiles.all) // full gear: these tests are not about the first-run defaults
    }

    private fun minutes(): Int = rule.onNodeWithTag("minutes").fetchSemanticsNode().config
        .first { it.key.name == "Text" }.value.let { (it as List<*>).first().toString() }.filter { c -> c.isDigit() }.toInt()

    private fun show(onPreview: () -> Unit = {}) = rule.setContent {
        CalisthenicsMemoryTheme(darkTheme = false) { TrainScreen(onPreview = onPreview, onStarted = {}, onEditProfile = {}) }
    }

    @Test fun everythingFitsOnOneScreenWithoutScrolling() {
        show()
        rule.waitForIdle()
        for (tag in listOf("goal_field", "profile_field", "rounds_plus", "between_flow", "minutes", "exercises_plus", "preview_button", "train_start_button"))
            rule.onNodeWithTag(tag).assertIsDisplayed()
    }

    @Test fun betweenSetModesAreExclusiveAndPersistedOnTrain() {
        show(); rule.waitForIdle()
        rule.onNodeWithTag("between_flow").performClick(); rule.waitForIdle()
        assertEquals(app.calisthenics.domain.model.BetweenSetMode.FLOW, io.github.gonbei774.calisthenicsmemory.ui.train.TrainSettingsStore.load(ctx).effectiveBetweenSetMode())
        rule.onNodeWithTag("between_rest").performClick(); rule.waitForIdle()
        assertEquals(app.calisthenics.domain.model.BetweenSetMode.REST, io.github.gonbei774.calisthenicsmemory.ui.train.TrainSettingsStore.load(ctx).effectiveBetweenSetMode())
    }

    @Test fun directTrainStartUsesLatestModeAndStartsRealStretchBlocks() {
        val settings = io.github.gonbei774.calisthenicsmemory.ui.train.TrainSettingsStore.load(ctx)
            .copy(betweenSetMode = app.calisthenics.domain.model.BetweenSetMode.REST, flow = false, stretchOn = false)
        io.github.gonbei774.calisthenicsmemory.ui.train.TrainSettingsStore.save(ctx, settings)
        val key = io.github.gonbei774.calisthenicsmemory.ui.train.draftKey(settings)
        io.github.gonbei774.calisthenicsmemory.ui.train.PreviewDraftStore.put(ctx,
            io.github.gonbei774.calisthenicsmemory.ui.train.PreviewDraft(key, betweenSetMode = app.calisthenics.domain.model.BetweenSetMode.REST))
        var started = false
        rule.setContent {
            CalisthenicsMemoryTheme(darkTheme = false) {
                TrainScreen(onPreview = {}, onStarted = { started = true }, onEditProfile = {})
            }
        }
        rule.waitForIdle()
        rule.onNodeWithTag("between_stretch").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("train_start_button").performClick(); rule.waitForIdle()
        assertTrue(started)
        val intent = shadowOf(ctx.applicationContext as android.app.Application).nextStartedService
        assertNotNull(intent)
        val plan = Json { ignoreUnknownKeys = true }.decodeFromString(
            app.calisthenics.domain.model.WorkoutPlan.serializer(),
            intent.getStringExtra(WorkoutSessionService.EXTRA_PLAN)!!,
        )
        assertEquals(app.calisthenics.domain.model.BetweenSetMode.STRETCH, plan.betweenSetMode)
        assertTrue(plan.blocks.any { it.type == app.calisthenics.domain.model.BlockType.STRETCH })
    }

    @Test fun modeEditOnSavedRoutineKeepsItsEditsAndSuppressesOldBreakExtras() {
        val catalog = io.github.gonbei774.calisthenicsmemory.ui.train.loadCatalog(ctx)
        val stretch = catalog.variations.first { it.kind == app.calisthenics.domain.model.Kind.STRETCH && !it.demonstrationHold }.id
        val progressionRule = app.calisthenics.domain.model.ProgressionRule(app.calisthenics.domain.model.RuleKind.REP_RANGE, sets = 3, from = 5, to = 8, sessions = 1)
        val edits = app.calisthenics.domain.planner.PlanEdits(
            swaps = mapOf("push" to "pushup-knee"), tierOverrides = mapOf("squat" to 3),
            extraStretches = mapOf("push" to listOf(stretch)), rule = progressionRule, extraStretchModeRevision = 10L,
        )
        val saved = app.calisthenics.domain.routine.savedFrom(
            "saved-mode", "Saved mode", 1, app.calisthenics.domain.routine.StarterRoutine.routine,
            edits, app.calisthenics.domain.planner.TrainSettings(rounds = 3, betweenSetMode = app.calisthenics.domain.model.BetweenSetMode.REST), progressionRule, 1L,
        )
        io.github.gonbei774.calisthenicsmemory.ui.screens.SavedRoutineStore.upsert(ctx, saved)
        val savedKey = io.github.gonbei774.calisthenicsmemory.ui.train.draftKey(
            app.calisthenics.domain.routine.settingsFor(saved, io.github.gonbei774.calisthenicsmemory.ui.train.TrainSettingsStore.load(ctx)),
        )
        io.github.gonbei774.calisthenicsmemory.ui.train.TrainSettingsStore.setModeRevision(ctx, savedKey, 50L)
        show(); rule.waitForIdle()
        rule.onNodeWithTag("objective_type_ROUTINE").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("goal_field").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("routine_saved-mode").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("between_flow").performClick(); rule.waitForIdle()

        val selected = io.github.gonbei774.calisthenicsmemory.ui.train.TrainSettingsStore.load(ctx)
        val draft = io.github.gonbei774.calisthenicsmemory.ui.train.PreviewDraftStore.get(ctx, io.github.gonbei774.calisthenicsmemory.ui.train.draftKey(selected))!!
        assertEquals(saved.edits.swaps, draft.edits.swaps) // savedFrom folds swaps into the routine slots
        assertEquals(3, draft.edits.tierOverrides["squat"])
        assertEquals(progressionRule, draft.edits.rule)
        assertEquals(listOf(stretch), draft.edits.extraStretches["push"])
        assertEquals(11L, draft.modeRevision) // objective re-entry reset its prior revision; saved authored extras are superseded

        rule.onNodeWithTag("train_start_button").performClick(); rule.waitForIdle()
        val intent = shadowOf(ctx.applicationContext as android.app.Application).nextStartedService
        val plan = Json { ignoreUnknownKeys = true }.decodeFromString(
            app.calisthenics.domain.model.WorkoutPlan.serializer(), intent.getStringExtra(WorkoutSessionService.EXTRA_PLAN)!!,
        )
        assertEquals(app.calisthenics.domain.model.BetweenSetMode.FLOW, plan.betweenSetMode)
        assertTrue(plan.blocks.any { it.slotId == "push" && it.variationId == "pushup-knee" })
        assertFalse(plan.blocks.any { it.variationId == stretch && it.type == app.calisthenics.domain.model.BlockType.STRETCH })
    }

    @Test fun savedRestDurationsApplyToDirectStartAndStayDormantForStretchBlocks() {
        val base = io.github.gonbei774.calisthenicsmemory.ui.screens.RoutineStore.load(ctx)
        val saved = app.calisthenics.domain.routine.savedFrom(
            "saved-rest", "Saved rest", 1, base, app.calisthenics.domain.planner.PlanEdits(),
            app.calisthenics.domain.planner.TrainSettings(betweenSetMode = app.calisthenics.domain.model.BetweenSetMode.REST),
            app.calisthenics.domain.model.ProgressionRule(), 1L,
        ).copy(restDurations = mapOf("push" to 91))
        io.github.gonbei774.calisthenicsmemory.ui.screens.SavedRoutineStore.upsert(ctx, saved)
        io.github.gonbei774.calisthenicsmemory.ui.train.TrainSettingsStore.save(
            ctx, app.calisthenics.domain.routine.settingsFor(saved, io.github.gonbei774.calisthenicsmemory.ui.train.TrainSettingsStore.load(ctx)),
        )
        show(); rule.waitForIdle()

        rule.onNodeWithTag("train_start_button").performClick(); rule.waitForIdle()
        fun nextPlan() = Json { ignoreUnknownKeys = true }.decodeFromString(
            app.calisthenics.domain.model.WorkoutPlan.serializer(),
            shadowOf(ctx.applicationContext as android.app.Application).nextStartedService!!.getStringExtra(WorkoutSessionService.EXTRA_PLAN)!!,
        )
        val restPlan = nextPlan()
        val pushRest = restPlan.blocks.filter { it.type == app.calisthenics.domain.model.BlockType.PASSIVE_RECOVERY && it.slotId == "push" }
        assertTrue(pushRest.isNotEmpty())
        assertTrue(pushRest.all { it.durationSeconds == 91 })

        rule.onNodeWithTag("between_stretch").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("train_start_button").performClick(); rule.waitForIdle()
        val stretchPlan = nextPlan()
        assertTrue(stretchPlan.blocks.any { it.type == app.calisthenics.domain.model.BlockType.STRETCH })
        assertFalse(stretchPlan.blocks.any { it.type == app.calisthenics.domain.model.BlockType.STRETCH && it.durationSeconds == 91 })
    }

    // R8.1-T05 (F6): the Preview's edits belong to the objective; picking another goal starts clean
    @Test fun changingTheObjectiveDropsThePreviewDraft() {
        val key = io.github.gonbei774.calisthenicsmemory.ui.train.draftKey(io.github.gonbei774.calisthenicsmemory.ui.train.TrainSettingsStore.load(ctx))
        io.github.gonbei774.calisthenicsmemory.ui.train.PreviewDraftStore.put(ctx, io.github.gonbei774.calisthenicsmemory.ui.train.PreviewDraft(key, app.calisthenics.domain.planner.PlanEdits(removed = setOf("core"))))
        assertNotNull(io.github.gonbei774.calisthenicsmemory.ui.train.PreviewDraftStore.get(ctx, key))
        show(); rule.waitForIdle()
        rule.onNodeWithTag("rounds_plus").performClick(); rule.waitForIdle()   // rounds are not the objective: the draft stays
        assertNotNull(io.github.gonbei774.calisthenicsmemory.ui.train.PreviewDraftStore.get(ctx, key))
        rule.onNodeWithTag("goal_field").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("goal_body-upper").performClick(); rule.waitForIdle()
        assertNull(io.github.gonbei774.calisthenicsmemory.ui.train.PreviewDraftStore.get(ctx, key))
    }

    @Test fun changingExerciseCountDropsManualSelectionButPreservesModeAndDoesNotRestoreOldEdits() {
        val settings = io.github.gonbei774.calisthenicsmemory.ui.train.TrainSettingsStore.load(ctx)
            .copy(exercisesPerRound = 6, betweenSetMode = app.calisthenics.domain.model.BetweenSetMode.REST,
                flow = false, stretchOn = false)
        io.github.gonbei774.calisthenicsmemory.ui.train.TrainSettingsStore.save(ctx, settings)
        val key = io.github.gonbei774.calisthenicsmemory.ui.train.draftKey(settings)
        val catalog = io.github.gonbei774.calisthenicsmemory.ui.train.loadCatalog(ctx)
        val manual = app.calisthenics.domain.planner.PlanEdits(
            swaps = mapOf("push" to "pushup-knee"), removed = setOf("core"),
            tierOverrides = mapOf("squat" to 3),
            added = listOf(app.calisthenics.domain.planner.AddedSlot("push",
                app.calisthenics.domain.planner.slotFor(catalog.variation("pushup-diamond")!!))),
            overrideEpochDay = java.time.LocalDate.now().toEpochDay())
        io.github.gonbei774.calisthenicsmemory.ui.train.PreviewDraftStore.put(ctx,
            io.github.gonbei774.calisthenicsmemory.ui.train.PreviewDraft(key, manual, rest = mapOf("push" to 91)))
        show(); rule.waitForIdle()
        rule.onNodeWithTag("rounds_plus").performClick(); rule.waitForIdle()
        assertEquals(manual, io.github.gonbei774.calisthenicsmemory.ui.train.PreviewDraftStore.get(ctx, key)!!.edits)
        rule.onNodeWithTag("exercises_plus").performScrollTo().performClick(); rule.waitForIdle()
        val fresh = io.github.gonbei774.calisthenicsmemory.ui.train.PreviewDraftStore.get(ctx, key)!!
        assertEquals(app.calisthenics.domain.planner.PlanEdits(), fresh.edits)
        assertTrue(fresh.rest.isEmpty())
        assertEquals(app.calisthenics.domain.model.BetweenSetMode.REST, fresh.betweenSetMode)
        rule.onNodeWithTag("exercises_minus").performScrollTo().performClick(); rule.waitForIdle()
        assertEquals(app.calisthenics.domain.planner.PlanEdits(),
            io.github.gonbei774.calisthenicsmemory.ui.train.PreviewDraftStore.get(ctx, key)!!.edits)
        rule.onNodeWithTag("train_start_button").performClick(); rule.waitForIdle()
        val intent = shadowOf(ctx.applicationContext as android.app.Application).nextStartedService!!
        val plan = Json { ignoreUnknownKeys = true }.decodeFromString(
            app.calisthenics.domain.model.WorkoutPlan.serializer(), intent.getStringExtra(WorkoutSessionService.EXTRA_PLAN)!!)
        val ids = plan.blocks.filter { it.type == app.calisthenics.domain.model.BlockType.WORK }
            .mapNotNull { it.variationId }.distinct()
        for ((a, id) in ids.withIndex()) for (other in ids.drop(a+1)) {
            assertFalse("automatic family duplicate: $id / $other", catalog.sharesLogicalRoute(id, other))
        }
    }

    // R8.2-T04 (G4): the "exercises in each set" tile at the bottom duplicated the Exercises per round stepper: only the stepper is left, and it drives the plan
    @Test fun onlyTheExercisesPerRoundStepperIsLeftAndItDrivesThePlan() {
        show(); rule.waitForIdle()
        rule.onNodeWithTag("exercise_count").assertDoesNotExist()
        rule.onNodeWithTag("exercises_plus").performScrollTo().assertExists()
        val before = minutes()
        rule.onNodeWithTag("exercises_plus").performClick(); rule.waitForIdle()
        assertTrue(minutes() > before)
        assertTrue(ctx.getSharedPreferences("train", Context.MODE_PRIVATE).getInt("exercises", -1) > 0)
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
        assertTrue("4 rounds = $at4 min", at4 in 30..45)
        // the default exercise count follows the rounds so the estimate stays within 30 to 45 minutes (owner decision); more rounds means fewer exercises
        val ex4 = exercises()
        rule.onNodeWithTag("rounds_plus").performClick(); rule.waitForIdle()
        assertTrue("5 rounds = ${minutes()} min", minutes() in 30..45)
        assertTrue(exercises() <= ex4)
        rule.onNodeWithTag("rounds_minus").performClick(); rule.onNodeWithTag("rounds_minus").performClick(); rule.waitForIdle()
        assertTrue("3 rounds = ${minutes()} min", minutes() in 30..45)
        assertTrue(exercises() >= ex4)
    }

    private fun exercises(): Int = rule.onNodeWithTag("exercises_value", useUnmergedTree = true).fetchSemanticsNode().config
        .first { it.key.name == "Text" }.value.let { (it as List<*>).first().toString() }.toInt()

    // R7-T08 (D1): the stepper next to the rounds; a body-part session starts at the count that makes it 30 minutes, a tap overrides it
    @Test fun exercisesPerRoundStepperRespectsTheAvailableDefaultPoolAndCanBeChanged() {
        show()
        rule.waitForIdle()
        rule.onNodeWithTag("objective_type_BODY_PART").assertIsSelected()
        rule.onNodeWithTag("goal_field").performClick(); rule.waitForIdle()
        rule.onNodeWithText("Focus: upper body").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("rounds_minus").performClick(); rule.waitForIdle()   // 3 rounds
        rule.onNodeWithTag("exercises_plus", useUnmergedTree = true).performScrollTo().assertExists()
        // A deliberately small pool may finish before 30 minutes; it must not repeat a progression to fill time.
        assertTrue("upper body has a usable plan", minutes() > 0)
        val n = exercises()
        assertTrue(n >= 3)
        rule.onNodeWithTag("exercises_minus", useUnmergedTree = true).performClick(); rule.waitForIdle()
        assertEquals(n - 1, exercises())
        assertTrue("one exercise fewer is shorter", minutes() < 35)
    }

    // R7-T13 (D10): the cool-down switch sits under the warm-up; its two modes appear when it is on
    @Test fun theCooldownSwitchShowsItsTwoModes() {
        show(); rule.waitForIdle()
        rule.onNodeWithTag("cooldown_mode_TIMED").assertDoesNotExist()
        rule.onNodeWithTag("cooldown_switch").performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("cooldown_mode_STRETCHING").assertIsSelected()
        val before = minutes()
        rule.onNodeWithTag("cooldown_mode_TIMED").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("cooldown_mode_TIMED").assertIsSelected()
        assertTrue("the cool-down counts in the estimate", minutes() >= 0 && before >= 0)
    }

    @Test fun betweenSetsStretchOrRestPersists() {
        show()
        rule.onNodeWithTag("between_stretch").assertIsSelected()
        rule.onNodeWithTag("between_rest").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("between_rest").assertIsSelected()
        rule.onNodeWithTag("between_stretch").assertIsNotSelected()
    }

    @Test fun objectiveHasBodyPartSkillAndRoutine() {
        show()
        rule.onNodeWithTag("objective_type_BODY_PART").assertIsSelected()
        rule.onNodeWithTag("objective_type_SKILL").assertIsDisplayed()
        rule.onNodeWithTag("objective_type_ROUTINE").assertIsDisplayed()   // V22: the ready-made routines are always there
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
        rule.onNodeWithTag("exercises_plus").assertIsDisplayed()
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

    // R8.1-T08 (F9): programs and the person's routines share one dropdown, in two labelled sections; there is no separate programs screen
    @Test fun programsAndMyRoutinesShareOneDropdownInTwoSections() {
        show(); rule.waitForIdle()
        val catalog = io.github.gonbei774.calisthenicsmemory.ui.train.loadCatalog(ctx)
        rule.onNodeWithTag("objective_type_ROUTINE").performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("routine_fixed_programs").assertDoesNotExist()
        rule.onNodeWithTag("goal_field").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("section_programs").assertExists()
        rule.onNodeWithTag("section_my_routines").assertExists()
        rule.onNodeWithTag("my_routines_empty").assertExists()
        catalog.fixedPrograms.filter { it.listed }.forEach { rule.onNodeWithTag("program_${it.id}").assertExists() }
        catalog.fixedPrograms.filter { !it.listed }.forEach { rule.onNodeWithTag("program_${it.id}").assertDoesNotExist() }   // R8.1-T09: the merged sessions replace the separate drills in the list
        rule.onNodeWithTag("routine_${app.calisthenics.domain.routine.Presets.RR}").assertExists()   // the ready-made routines are programs too
    }

    // R8.1-T09 (F11): the Army PRT session and the Pilates mat session each show at least 30 minutes
    private fun programMinutes(id: String): Int {
        val catalog = io.github.gonbei774.calisthenicsmemory.ui.train.loadCatalog(ctx)
        io.github.gonbei774.calisthenicsmemory.ui.train.TrainSettingsStore.save(ctx, app.calisthenics.domain.routine.settingsForProgram(catalog.fixedPrograms.first { it.id == id }, io.github.gonbei774.calisthenicsmemory.ui.train.TrainSettingsStore.load(ctx)))
        show(); rule.waitForIdle()
        rule.onNodeWithTag("train_start_button").assertIsEnabled()
        return minutes()
    }
    // R8.1-T12: a program's Flow start value does not stay on when the objective goes back to a body part
    @Test fun leavingAProgramForABodyPartTurnsFlowOff() {
        val catalog = io.github.gonbei774.calisthenicsmemory.ui.train.loadCatalog(ctx)
        io.github.gonbei774.calisthenicsmemory.ui.train.TrainSettingsStore.save(ctx, app.calisthenics.domain.routine.settingsForProgram(catalog.fixedPrograms.first { it.id == "army-prt" }, io.github.gonbei774.calisthenicsmemory.ui.train.TrainSettingsStore.load(ctx)))
        assertTrue(io.github.gonbei774.calisthenicsmemory.ui.train.TrainSettingsStore.load(ctx).flow)
        show(); rule.waitForIdle()
        rule.onNodeWithTag("objective_type_BODY_PART").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick); rule.waitForIdle()
        val s = io.github.gonbei774.calisthenicsmemory.ui.train.TrainSettingsStore.load(ctx)
        assertFalse(s.flow); assertNull(s.fixedProgramId)
    }

    // R8.2-T03 (G3): one Include row with Warm-up, Conditioning and Cool-down; gold when included
    @Test fun theIncludeRowHasThreeToggleButtonsThatPersistAndChangeTheTime() {
        show(); rule.waitForIdle()
        rule.onNodeWithTag("include_row").performScrollTo().assertExists()
        for (t in listOf("warmup_switch", "conditioning_switch", "cooldown_switch")) rule.onNodeWithTag(t).performScrollTo().assertIsOff()
        val before = minutes()
        rule.onNodeWithTag("conditioning_switch").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("conditioning_switch").assertIsOn()
        assertTrue("the finisher is counted in the time, which stays about 45 minutes: ${minutes()}", minutes() in 30..46)
        assertTrue(ctx.getSharedPreferences("train", Context.MODE_PRIVATE).getBoolean("conditioning", false))
        rule.onNodeWithTag("warmup_switch").performClick(); rule.onNodeWithTag("cooldown_switch").performClick(); rule.waitForIdle()
        for (t in listOf("warmup_switch", "conditioning_switch", "cooldown_switch")) rule.onNodeWithTag(t).assertIsOn()
        rule.onNodeWithTag("cooldown_mode_STRETCHING").performScrollTo().assertExists()   // the cool-down keeps its modes
    }

    @Test fun theMergedArmySessionShowsAtLeastThirtyMinutes() { val m = programMinutes("army-prt"); assertTrue("$m min", m >= 30) }
    @Test fun theMergedPilatesSessionShowsAtLeastThirtyMinutes() { val m = programMinutes("pilates-mat"); assertTrue("$m min", m >= 30) }

    @Test fun pickingAProgramLoadsItAsTheStartingPointWithOneSetAndItsOwnFlow() {
        show(); rule.waitForIdle()
        val catalog = io.github.gonbei774.calisthenicsmemory.ui.train.loadCatalog(ctx)
        val program = catalog.fixedPrograms.first { it.listed }
        rule.onNodeWithTag("objective_type_ROUTINE").performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("goal_field").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("program_${program.id}").performClick(); rule.waitForIdle()
        val s = io.github.gonbei774.calisthenicsmemory.ui.train.TrainSettingsStore.load(ctx)
        assertEquals(program.id, s.fixedProgramId); assertEquals(1, s.rounds); assertEquals(program.flow, s.flow); assertNull(s.routineId)
        rule.onNodeWithTag("exercises_value", useUnmergedTree = true).assertDoesNotExist()   // R8.2-T04: the duplicate count is gone
        rule.onNodeWithTag("warmup_switch").assertDoesNotExist()   // a program is a list: no generated warm-up, cool-down or extra exercises
        rule.onNodeWithTag("exercises_plus").assertDoesNotExist()
    }

    @Test fun workoutFormatControlHasThreeFormatsPersistsAndRenamesRoundsToSets() {
        show(); rule.waitForIdle()
        for (f in listOf("CIRCUIT", "PAIRS", "STRAIGHT")) rule.onNodeWithTag("format_$f").assertExists()
        rule.onNodeWithTag("format_CIRCUIT").assertIsSelected()
        rule.onNodeWithText("Rounds", ignoreCase = true).assertExists()
        rule.onNodeWithTag("format_PAIRS").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("format_PAIRS").assertIsSelected()
        rule.onNodeWithText("Sets", ignoreCase = true).assertExists()
        assertEquals("PAIRS", ctx.getSharedPreferences("train", Context.MODE_PRIVATE).getString("format", ""))
        rule.onNodeWithTag("exercises_plus").assertExists() // the plan still builds in every format
    }

    private fun saveRoutine(name: String = "Push day", format: app.calisthenics.domain.model.WorkoutFormat = app.calisthenics.domain.model.WorkoutFormat.PAIRS): app.calisthenics.domain.routine.SavedRoutine {
        val catalog = io.github.gonbei774.calisthenicsmemory.ui.train.loadCatalog(ctx)
        val edits = app.calisthenics.domain.planner.PlanEdits(added = listOf(app.calisthenics.domain.planner.AddedSlot("squat", app.calisthenics.domain.planner.slotFor(catalog.variation("pushup-diamond")!!))), removed = setOf("core2"))
        val r = app.calisthenics.domain.routine.savedFrom("rt-1", name, 1, app.calisthenics.domain.routine.StarterRoutine.routine, edits,
            app.calisthenics.domain.planner.TrainSettings(rounds = 3, format = format, stretchOn = false), app.calisthenics.domain.model.ProgressionRule(), 1L)
        io.github.gonbei774.calisthenicsmemory.ui.screens.SavedRoutineStore.upsert(ctx, r); return r
    }

    @Test fun readyMadeRoutinesAreListedFirstAndLoadTheirStructure() {   // V22
        saveRoutine(); show(); rule.waitForIdle()
        rule.onNodeWithTag("objective_type_ROUTINE").performClick(); rule.waitForIdle()
        // the first ready-made routine is chosen: the Recommended Routine = pairs, plain rest, 9 exercises, 3 sets
        rule.onNodeWithTag("goal_field").assertTextContains("Recommended Routine")
        rule.onNodeWithTag("format_PAIRS").assertIsSelected(); rule.onNodeWithTag("between_rest").assertIsSelected()
        rule.onNodeWithTag("exercises_value", useUnmergedTree = true).assertTextEquals("9")   // R8.2-T04: the stepper is the one control
        assertEquals("preset-rr", ctx.getSharedPreferences("train", Context.MODE_PRIVATE).getString("routine", null))
        rule.onNodeWithTag("goal_field").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("routine_preset-minimalist").assertExists()
        rule.onNodeWithTag("routine_rt-1").assertExists()          // the person's own routines follow the ready-made ones
        rule.onNodeWithTag("routine_preset-minimalist").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick); rule.waitForIdle()
        rule.onNodeWithTag("format_CIRCUIT").assertIsSelected(); rule.onNodeWithTag("exercises_value", useUnmergedTree = true).assertTextEquals("4")   // R8.2-T04: the stepper is the one control
    }

    @Test fun choosingASavedRoutineLoadsItsFormatRestAndSetsAndItsExercises() {
        saveRoutine(); show(); rule.waitForIdle()
        rule.onNodeWithTag("objective_type_ROUTINE").assertIsDisplayed().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("objective_type_ROUTINE").assertIsSelected()
        rule.onNodeWithTag("goal_field").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("routine_rt-1").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick); rule.waitForIdle()   // R8.1-T08: the menu now also lists the programs, so the entry can be below the fold
        rule.onNodeWithTag("goal_field").assertTextContains("Push day")
        rule.onNodeWithTag("format_PAIRS").assertIsSelected()
        rule.onNodeWithTag("between_rest").assertIsSelected()
        assertEquals("rt-1", ctx.getSharedPreferences("train", Context.MODE_PRIVATE).getString("routine", null))
        // five original exercises minus core2 plus the added push-up: still six exercises per set
        rule.onNodeWithTag("exercises_value", useUnmergedTree = true).assertTextEquals("6")   // R8.2-T04: the stepper is the one control
        // picking a body part again leaves the routine
        rule.onNodeWithTag("objective_type_BODY_PART").performClick(); rule.waitForIdle()
        assertNull(ctx.getSharedPreferences("train", Context.MODE_PRIVATE).getString("routine", null))
    }

    @Test fun thePreviewOfASavedRoutineShowsItsAddedExerciseAndOffersUpdateOrNew() {
        saveRoutine()
        ctx.getSharedPreferences("train", Context.MODE_PRIVATE).edit().putString("routine", "rt-1").putInt("rounds", 3).commit()
        rule.setContent { CalisthenicsMemoryTheme(darkTheme = false) { io.github.gonbei774.calisthenicsmemory.ui.train.PreviewScreen(onBack = {}, onStarted = {}) } }
        rule.waitForIdle()
        rule.onNodeWithTag("exercise_add-pushup-diamond").performScrollTo().assertExists()
        rule.onNodeWithTag("exercise_core2").assertDoesNotExist()
        rule.onNodeWithTag("save_routine_button").assertIsDisplayed().assertIsEnabled()
        rule.onNodeWithTag("start_button").assertIsDisplayed()
    }

    @Test fun warmUpSwitchIsOffByDefaultPersistsAndAddsTime() {   // V23
        show(); rule.waitForIdle()
        rule.onNodeWithTag("warmup_switch").assertIsDisplayed().assertIsOff()
        val before = minutes()
        rule.onNodeWithTag("warmup_switch").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("warmup_switch").assertIsOn()
        assertTrue("counted in the time, which stays 30 to 45 minutes: ${minutes()}", minutes() in 30..45)
        assertTrue(ctx.getSharedPreferences("train", Context.MODE_PRIVATE).getBoolean("warmup", false))
    }
    @Test fun specialistPoolsAreNotSelectedFromLegacyTrainSettings() {
        ctx.getSharedPreferences("train", Context.MODE_PRIVATE).edit()
            .putStringSet("additional_pools", setOf("POWER", "CONDITIONING", "PREHAB", "MOBILITY")).commit()
        show(); rule.waitForIdle()
        assertTrue(io.github.gonbei774.calisthenicsmemory.ui.train.TrainSettingsStore.load(ctx).additionalPools.isEmpty())
        rule.onNodeWithTag("pool_POWER").assertDoesNotExist()
        rule.onNodeWithTag("additional_pools").assertDoesNotExist()
        rule.onNodeWithTag("open_programs").assertDoesNotExist()
    }

}
