package io.github.gonbei774.calisthenicsmemory.ui

import android.app.Application
import app.calisthenics.domain.equipment.SeedProfiles as SeedProfilesForTest
import app.calisthenics.domain.planner.AddedSlot
import app.calisthenics.domain.planner.slotFor
import app.calisthenics.domain.planner.PlanEdits
import app.calisthenics.domain.model.Routine
import app.calisthenics.domain.model.RoutineSlot
import app.calisthenics.domain.model.BetweenSetMode
import app.calisthenics.domain.model.Kind
import app.calisthenics.domain.model.ProgressionRule
import app.calisthenics.domain.model.RuleKind
import app.calisthenics.domain.routine.savedFrom
import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import app.calisthenics.domain.model.WorkoutPlan
import app.calisthenics.domain.model.BlockType
import app.calisthenics.domain.planner.PlanResult
import app.calisthenics.domain.planner.buildTrainPlan
import io.github.gonbei774.calisthenicsmemory.session.WorkoutSessionService
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalisthenicsMemoryTheme
import io.github.gonbei774.calisthenicsmemory.ui.train.PreviewScreen
import io.github.gonbei774.calisthenicsmemory.ui.train.TrainScreen
import io.github.gonbei774.calisthenicsmemory.ui.train.TrainData
import io.github.gonbei774.calisthenicsmemory.ui.train.TrainSettingsStore
import io.github.gonbei774.calisthenicsmemory.ui.train.rememberTrainData
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
        for (n in listOf("profiles", "mode", "planner_prefs", "train", "goal", "levels", "routine", "personal_targets", "preview_draft", "saved_routines"))
            app.getSharedPreferences(n, Context.MODE_PRIVATE).edit().clear().commit()
        io.github.gonbei774.calisthenicsmemory.ui.screens.ProfileStore.save(app, SeedProfilesForTest.all) // full gear: these tests are not about the first-run defaults
    }

    /** R8.4 regression: an older Preview mode must not beat a newer explicit Train choice. */
    @Test fun latestTrainModeWinsOverAnExistingPreviewDraftAndProducesStretchBlocks() {
        val settings = TrainSettingsStore.load(app).copy(betweenSetMode = BetweenSetMode.REST, flow = false, stretchOn = false)
        TrainSettingsStore.save(app, settings)
        val key = io.github.gonbei774.calisthenicsmemory.ui.train.draftKey(settings)
        io.github.gonbei774.calisthenicsmemory.ui.train.PreviewDraftStore.put(app,
            io.github.gonbei774.calisthenicsmemory.ui.train.PreviewDraft(key, PlanEdits(), betweenSetMode = BetweenSetMode.REST))
        var preview by mutableStateOf(false)
        rule.setContent {
            CalisthenicsMemoryTheme(darkTheme = false) {
                if (preview) PreviewScreen(onBack = { preview = false }, onStarted = {})
                else TrainScreen(onPreview = { preview = true }, onStarted = {}, onEditProfile = {})
            }
        }
        rule.waitForIdle()
        rule.onNodeWithTag("between_stretch").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("preview_button").performClick(); rule.waitForIdle()

        rule.onNodeWithTag("between_stretch").assertIsSelected()
        assertEquals(BetweenSetMode.STRETCH,
            io.github.gonbei774.calisthenicsmemory.ui.train.PreviewDraftStore.get(app, key)?.betweenSetMode)
        rule.onNodeWithTag("timeline_toggle").performScrollTo().performClick(); rule.waitForIdle()
        val catalog = io.github.gonbei774.calisthenicsmemory.ui.train.loadCatalog(app)
        val compatibleStretchNames = catalog.variations.filter { it.kind == Kind.STRETCH }
            .map { it.name }
        assertTrue("Preview should show a real selected stretch block", compatibleStretchNames.any { name ->
            rule.onAllNodesWithText(name, substring = true).fetchSemanticsNodes().isNotEmpty()
        })
        rule.onNodeWithTag("preparation_seconds_plus").performScrollTo().performClick(); rule.waitForIdle()
        preview = false
        rule.waitForIdle()
        rule.onNodeWithTag("between_stretch").assertIsSelected()
        rule.onNodeWithTag("train_start_button").performClick(); rule.waitForIdle()
        val intent = shadowOf(app).nextStartedService
        assertNotNull("direct Train start should launch the current Preview configuration", intent)
        val plan = Json { ignoreUnknownKeys = true }.decodeFromString(WorkoutPlan.serializer(), intent!!.getStringExtra(WorkoutSessionService.EXTRA_PLAN)!!)
        assertEquals(BetweenSetMode.STRETCH, plan.betweenSetMode)
        assertEquals(6, plan.preparationSeconds)
        assertTrue("direct Train start should use real stretch blocks", plan.blocks.any { it.type == BlockType.STRETCH })
    }

    @Test fun legacyOrdinaryRuleIsScopedToAppLevelsWhileSavedRoutineRuleSurvives() {
        val legacyRange = ProgressionRule(RuleKind.REP_RANGE, sets = 3, from = 5, to = 8, sessions = 1)
        val ordinary = io.github.gonbei774.calisthenicsmemory.ui.screens.RoutineStore.load(app)
        io.github.gonbei774.calisthenicsmemory.ui.screens.RoutineStore.save(app, ordinary.copy(rule = legacyRange))
        val settings = TrainSettingsStore.load(app)
        assertEquals(RuleKind.APP_LEVELS, io.github.gonbei774.calisthenicsmemory.ui.screens.RoutineStore.load(app).rule.kind)
        assertEquals(ordinary.slots, io.github.gonbei774.calisthenicsmemory.ui.screens.RoutineStore.load(app).slots)

        val saved = savedFrom(
            "legacy-range", "Legacy range", 1, ordinary, PlanEdits(), settings, legacyRange, 1L,
        )
        io.github.gonbei774.calisthenicsmemory.ui.screens.SavedRoutineStore.upsert(app, saved)
        val catalog = io.github.gonbei774.calisthenicsmemory.ui.train.loadCatalog(app)
        val restored = io.github.gonbei774.calisthenicsmemory.ui.train.resolveTrain(
            app, io.github.gonbei774.calisthenicsmemory.ui.screens.RoutineStore.load(app), settings.copy(routineId = saved.id), catalog,
        )
        assertEquals(RuleKind.REP_RANGE, restored.routine.rule.kind)
        assertEquals(RuleKind.REP_RANGE, restored.edits.rule?.kind)
    }

    @Test fun newerGlobalModeSuppressesOldExtraStretchUntilExplicitlyReadded() {
        val catalog = io.github.gonbei774.calisthenicsmemory.ui.train.loadCatalog(app)
        val profile = io.github.gonbei774.calisthenicsmemory.ui.screens.ProfileStore.selected(app)
        val progress = io.github.gonbei774.calisthenicsmemory.ui.screens.LevelStore.snapshot(catalog, io.github.gonbei774.calisthenicsmemory.ui.screens.LevelStore.load(app))
        val settings = TrainSettingsStore.load(app).copy(betweenSetMode = BetweenSetMode.REST, flow = false, stretchOn = false)
        val extraId = catalog.variations.first { it.kind == Kind.STRETCH && !it.demonstrationHold }.id
        val legacyExtra = PlanEdits(extraStretches = mapOf("push" to listOf(extraId)), extraStretchModeRevision = 0L)
        fun plan(edits: PlanEdits) = buildTrainPlan(
            catalog, io.github.gonbei774.calisthenicsmemory.ui.screens.RoutineStore.load(app), progress, profile, settings, edits = edits,
        ) as PlanResult.Ready

        assertFalse(plan(io.github.gonbei774.calisthenicsmemory.ui.train.effectivePlanEdits(legacyExtra, 1L)).plan.blocks.any { it.variationId == extraId })
        val readded = legacyExtra.copy(extraStretchModeRevision = 1L)
        assertTrue(plan(io.github.gonbei774.calisthenicsmemory.ui.train.effectivePlanEdits(readded, 1L)).plan.blocks.any { it.variationId == extraId })
    }

    @Test fun savingAfterANewerModeChoiceUsesTheEffectiveTimeline() {
        val settings = TrainSettingsStore.load(app).copy(betweenSetMode = BetweenSetMode.REST, flow = false, stretchOn = false)
        TrainSettingsStore.save(app, settings)
        val key = io.github.gonbei774.calisthenicsmemory.ui.train.draftKey(settings)
        val extraId = io.github.gonbei774.calisthenicsmemory.ui.train.loadCatalog(app).variations.first { it.kind == Kind.STRETCH && !it.demonstrationHold }.id
        io.github.gonbei774.calisthenicsmemory.ui.train.PreviewDraftStore.put(app,
            io.github.gonbei774.calisthenicsmemory.ui.train.PreviewDraft(key, edits = PlanEdits(extraStretches = mapOf("push" to listOf(extraId))), rest = mapOf("push" to 75), betweenSetMode = BetweenSetMode.REST, ruleDefaultVersion = 1))
        show(); rule.waitForIdle()
        rule.onNodeWithTag("between_flow").performScrollTo().performClick(); rule.waitForIdle()
        val currentSettings = TrainSettingsStore.load(app)
        val currentDraft = io.github.gonbei774.calisthenicsmemory.ui.train.PreviewDraftStore.get(app, key)!!
        // Invoke the exact production Save resolver; dialog rendering is a separate device check.
        val saved = io.github.gonbei774.calisthenicsmemory.ui.train.savedCurrentWorkout(
            "flow-copy", "Flow copy", 1, io.github.gonbei774.calisthenicsmemory.ui.screens.RoutineStore.load(app), currentDraft.edits, currentSettings,
            currentDraft.modeRevision, currentDraft.rest, 1L)
        io.github.gonbei774.calisthenicsmemory.ui.screens.SavedRoutineStore.upsert(app, saved)
        assertEquals(saved, io.github.gonbei774.calisthenicsmemory.ui.screens.SavedRoutineStore.get(app, saved.id))
        assertTrue(saved.edits.extraStretches.isEmpty())
        assertTrue("saved routine should keep the dormant numeric rest override", saved.restDurations.containsKey("push"))
    }

    @Test fun ordinaryLegacyDraftRuleGetsOneScopedResetAndKeepsOtherEdits() {
        val settings = TrainSettingsStore.load(app)
        val key = io.github.gonbei774.calisthenicsmemory.ui.train.draftKey(settings)
        val legacy = ProgressionRule(RuleKind.REP_RANGE, sets = 3, from = 5, to = 8, sessions = 1)
        val edits = PlanEdits(rule = legacy, swaps = mapOf("push" to "pushup-standard"), removed = setOf("core"))
        io.github.gonbei774.calisthenicsmemory.ui.train.PreviewDraftStore.put(app,
            io.github.gonbei774.calisthenicsmemory.ui.train.PreviewDraft(key, edits = edits))
        app.getSharedPreferences("train", Context.MODE_PRIVATE).edit().putInt("ordinary_draft_rule_default_version", 0).commit()

        TrainSettingsStore.load(app)
        val migrated = io.github.gonbei774.calisthenicsmemory.ui.train.PreviewDraftStore.get(app, key)!!
        assertEquals(RuleKind.APP_LEVELS, migrated.edits.rule?.kind)
        assertEquals(edits.swaps, migrated.edits.swaps)
        assertEquals(edits.removed, migrated.edits.removed)
        assertEquals(1, migrated.ruleDefaultVersion)

        val explicit = migrated.copy(edits = migrated.edits.copy(rule = legacy))
        io.github.gonbei774.calisthenicsmemory.ui.train.PreviewDraftStore.put(app, explicit)
        app.getSharedPreferences("train", Context.MODE_PRIVATE).edit().putInt("ordinary_draft_rule_default_version", 0).commit()
        TrainSettingsStore.load(app)
        assertEquals(legacy, io.github.gonbei774.calisthenicsmemory.ui.train.PreviewDraftStore.get(app, key)?.edits?.rule)
    }

    private fun oneExerciseAtLevelFive(id: String, slotId: String, values: List<Int>) {
        val catalog = io.github.gonbei774.calisthenicsmemory.ui.train.loadCatalog(app)
        val v = catalog.variation(id)!!
        val slot = RoutineSlot(slotId, v.patterns.first(), v.areas.first(), id)
        io.github.gonbei774.calisthenicsmemory.ui.screens.RoutineStore.save(app,
            Routine("single", 1, "Single", listOf(slot)))
        io.github.gonbei774.calisthenicsmemory.ui.screens.LevelStore.save(app, mapOf(id to 5))
        io.github.gonbei774.calisthenicsmemory.ui.screens.PersonalTargetStore.save(app, id, values)
        TrainSettingsStore.save(app, TrainSettingsStore.load(app).copy(exercisesPerRound = 1))
    }

    @Test fun repStepperUsesPersonalTopAndReturnsToBoundaryBeforeLevelFour() {
        oneExerciseAtLevelFive("pushup-standard", "push", listOf(5, 7, 9, 10, 12))
        show(); rule.waitForIdle()
        fun target(n: Int) { rule.onNodeWithTag("exercise_target_push").performScrollTo().assertTextContains("$n reps", substring = true) }
        fun tap(direction: String) { rule.onNodeWithTag("level_${direction}_push").performScrollTo().performClick(); rule.waitForIdle() }
        target(12); tap("plus"); target(13); tap("plus"); target(14)
        tap("minus"); target(13); tap("minus"); target(12); tap("minus"); target(10)
    }

    @Test fun raisedPersonalTopRemapsAnOlderSavedExtensionAndStepsFromTheNewBoundary() {
        oneExerciseAtLevelFive("pushup-standard", "push", listOf(10, 20, 30, 40, 50))
        val settings = TrainSettingsStore.load(app)
        io.github.gonbei774.calisthenicsmemory.ui.train.PreviewDraftStore.put(app,
            io.github.gonbei774.calisthenicsmemory.ui.train.PreviewDraft(
                io.github.gonbei774.calisthenicsmemory.ui.train.draftKey(settings),
                PlanEdits(tierOverrides = mapOf("push" to 5), levelFiveTargets = mapOf("push" to 40))))
        show(); rule.waitForIdle()
        rule.onNodeWithTag("exercise_target_push").performScrollTo().assertTextContains("50 reps", substring = true)
        rule.onNodeWithTag("level_plus_push").performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("exercise_target_push").assertTextContains("51 reps", substring = true)
        rule.onNodeWithTag("level_minus_push").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("exercise_target_push").assertTextContains("50 reps", substring = true)
        rule.onNodeWithTag("level_minus_push").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("exercise_target_push").assertTextContains("40 reps", substring = true)
    }

    @Test fun holdStepperUsesFiveSecondsAndReturnsToBoundaryBeforeLevelFour() {
        oneExerciseAtLevelFive("plank", "core", listOf(10, 15, 20, 25, 30))
        show(); rule.waitForIdle()
        fun target(n: Int) { rule.onNodeWithTag("exercise_target_core").performScrollTo().assertTextContains("$n s", substring = true) }
        fun tap(direction: String) { rule.onNodeWithTag("level_${direction}_core").performScrollTo().performClick(); rule.waitForIdle() }
        target(30); tap("plus"); target(35); tap("plus"); target(40)
        tap("minus"); target(35); tap("minus"); target(30); tap("minus"); target(25)
    }

    // Touch injection does not reach the bottom-sheet window under Robolectric, so taps inside the sheet use the semantic click.
    private fun openSwap(slot: String) {
        rule.onNodeWithTag("swap_$slot").performScrollTo().performClick(); rule.waitForIdle()
        rule.mainClock.advanceTimeBy(1_000); rule.waitForIdle() // let the sheet finish sliding in before tapping inside it
    }

    @Test fun sixToEightRebuildUsesSameDuplicateFreeExercisesOnBothStartPaths() {
        val settings = TrainSettingsStore.load(app).copy(exercisesPerRound = 6,
            betweenSetMode = BetweenSetMode.REST, flow = false, stretchOn = false)
        TrainSettingsStore.save(app, settings)
        val key = io.github.gonbei774.calisthenicsmemory.ui.train.draftKey(settings)
        val catalog = io.github.gonbei774.calisthenicsmemory.ui.train.loadCatalog(app)
        val manual = PlanEdits(swaps = mapOf("pull" to "chinup-full"),
            added = listOf(AddedSlot("pull",
                slotFor(catalog.variation("pullup-full")!!))))
        io.github.gonbei774.calisthenicsmemory.ui.train.PreviewDraftStore.put(app,
            io.github.gonbei774.calisthenicsmemory.ui.train.PreviewDraft(key, manual))
        var preview by mutableStateOf(false)
        rule.setContent { CalisthenicsMemoryTheme(darkTheme = false) {
            if (preview) PreviewScreen(onBack = { preview = false }, onStarted = {})
            else TrainScreen(onPreview = { preview = true }, onStarted = {}, onEditProfile = {})
        } }
        rule.waitForIdle()
        repeat(2) { rule.onNodeWithTag("exercises_plus").performScrollTo().performClick(); rule.waitForIdle() }
        assertEquals(8, TrainSettingsStore.load(app).exercisesPerRound)
        rule.onNodeWithTag("train_start_button").performClick(); rule.waitForIdle()
        fun nextPlan(): WorkoutPlan = Json { ignoreUnknownKeys = true }.decodeFromString(
            WorkoutPlan.serializer(), shadowOf(app).nextStartedService!!.getStringExtra(WorkoutSessionService.EXTRA_PLAN)!!)
        val direct = nextPlan()
        rule.onNodeWithTag("preview_button").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("start_button").performClick(); rule.waitForIdle()
        val viaPreview = nextPlan()
        fun choices(plan: WorkoutPlan) = plan.blocks.filter { it.type == BlockType.WORK }
            .mapNotNull { it.variationId }.distinct()
        val ids = choices(direct)
        assertEquals(ids, choices(viaPreview))
        assertEquals(8, ids.size)
        for ((i, id) in ids.withIndex()) for (other in ids.drop(i + 1))
            assertFalse("automatic alternatives $id / $other", catalog.sharesLogicalRoute(id, other))
    }

    private fun show(onStarted: () -> Unit = {}) = rule.setContent {
        CalisthenicsMemoryTheme(darkTheme = false) { PreviewScreen(onBack = {}, onStarted = onStarted) }
    }

    // R7-T13 (D10): the cool-down is part of the Preview: timed minutes with - / +, or stretches that can be swapped
    @Test fun aTimedCooldownShowsItsMinutesAndTheyCanBeChanged() {
        app.getSharedPreferences("train", Context.MODE_PRIVATE).edit().putBoolean("cooldown", true).putString("cooldown_mode", "TIMED").commit()
        show(); rule.waitForIdle()
        rule.onNodeWithTag("cooldown_timed").performScrollTo().assertExists()
        rule.onNodeWithText("Relax and breathe · 5 min").assertExists()
        rule.onNodeWithTag("cooldown_minutes_plus").performClick(); rule.waitForIdle()
        rule.onNodeWithText("Relax and breathe · 6 min").assertExists()
    }

    @Test fun aCooldownStretchCanBeRemovedAndOneAddedWithThePlus() {   // R7.2-T04 (F5)
        app.getSharedPreferences("train", Context.MODE_PRIVATE).edit().putBoolean("cooldown", true).putString("cooldown_mode", "STRETCHING").commit()
        show(); rule.waitForIdle()
        fun count() = (0..20).count { rule.onAllNodesWithTag("cooldown_stretch_$it").fetchSemanticsNodes().isNotEmpty() }
        val before = count()
        assertTrue(before >= 2)
        rule.onNodeWithTag("cooldown_remove_0").performScrollTo().performClick(); rule.waitForIdle()
        assertEquals(before - 1, count())
        rule.onNodeWithTag("cooldown_stretch_0").performScrollTo().performTouchInput { longClick() }; rule.waitForIdle()   // long press removes too
        assertEquals(before - 2, count())
        rule.onNodeWithTag("cooldown_add").performScrollTo().performClick(); rule.waitForIdle(); rule.mainClock.advanceTimeBy(1_000); rule.waitForIdle()
        rule.onNodeWithTag("break_option_stretch-frog").performSemanticsAction(click); rule.waitForIdle()
        assertEquals(before - 1, count())
        rule.onNodeWithTag("cooldown_add").assertExists()
    }

    @Test fun aStretchingCooldownListsStretchesWithASwapButton() {
        app.getSharedPreferences("train", Context.MODE_PRIVATE).edit().putBoolean("cooldown", true).putString("cooldown_mode", "STRETCHING").commit()
        show(); rule.waitForIdle()
        rule.onNodeWithTag("cooldown_stretch_0").performScrollTo().assertExists()
        rule.onNodeWithTag("cooldown_swap_0").assertExists()
    }

    // R8.1-T05 (F5): Straight sets / Pairs / Circuit and the number of sets are chosen in the Preview too
    @Test fun theSetFormatAndTheNumberOfSetsAreChosenInThePreview() {
        show(); rule.waitForIdle()
        for (f in listOf("CIRCUIT", "PAIRS", "STRAIGHT")) rule.onNodeWithTag("preview_format_$f").performScrollTo().assertExists()
        rule.onNodeWithTag("preview_format_CIRCUIT").assertIsSelected()
        rule.onNodeWithTag("preview_format_STRAIGHT").performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("preview_format_STRAIGHT").assertIsSelected()
        assertEquals("STRAIGHT", app.getSharedPreferences("train", Context.MODE_PRIVATE).getString("format", null))
        rule.onNodeWithTag("preview_rounds_plus").performScrollTo().performClick(); rule.waitForIdle()
        assertEquals(5, app.getSharedPreferences("train", Context.MODE_PRIVATE).getInt("rounds", 0))
        rule.onNodeWithTag("preview_header").assertTextContains("5 rounds", substring = true)
    }

    // R8.3: Flow is chosen on Train; Preview exposes the shared preparation duration only.
    @Test fun flowHasNoPreviewSwitchAndPreparationDurationPersists() {
        val s = TrainSettingsStore.load(app)
        TrainSettingsStore.save(app, s.copy(betweenSetMode = BetweenSetMode.FLOW, flow = true, stretchOn = false))
        show(); rule.waitForIdle()
        rule.onNodeWithTag("preview_flow_switch").assertDoesNotExist()
        rule.onNodeWithTag("preparation_seconds_value").performScrollTo().assertTextEquals("5 s")
        rule.onNodeWithTag("preparation_seconds_plus").performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("preparation_seconds_value").assertTextEquals("6 s")
        assertTrue(app.getSharedPreferences("train", Context.MODE_PRIVATE).getBoolean("flow", false))
        assertEquals(6, TrainSettingsStore.load(app).preparationSeconds)
    }

    // R8.1-T05 (F6): every change survives going back to Train and returning; only another objective resets it
    @Test fun editsSurviveLeavingThePreviewAndComingBack() {
        io.github.gonbei774.calisthenicsmemory.ui.screens.PrefsStore.save(app, io.github.gonbei774.calisthenicsmemory.ui.screens.PrefsStore.load(app).copy(stretchOn = false))   // plain rest, so the rest steppers exist
        var visible by mutableStateOf(true)
        rule.setContent { CalisthenicsMemoryTheme(darkTheme = false) { if (visible) PreviewScreen(onBack = {}, onStarted = {}) else androidx.compose.foundation.layout.Box {} } }
        rule.waitForIdle()
        openSwap("push")
        rule.onNodeWithTag("swap_option_pushup-knee").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick); rule.waitForIdle()
        rule.onNodeWithTag("level_plus_squat").performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("rest_pull_plus").performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("preview_format_PAIRS").performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("exercise_core2").performScrollTo().performTouchInput { longClick() }; rule.waitForIdle()
        rule.onNodeWithTag("remove_confirm").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick); rule.waitForIdle()
        val level = rule.onNodeWithTag("level_squat").fetchSemanticsNode().config.first { it.key.name == "Text" }.value.toString()
        fun rest() = rule.onNodeWithTag("rest_pull_value").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.ContentDescription].joinToString()
        val rest = rest()
        visible = false; rule.waitForIdle()
        visible = true; rule.waitForIdle()
        rule.onNodeWithTag("exercise_name_push").assertTextEquals("Knee Push-Up")
        rule.onNodeWithTag("exercise_core2").assertDoesNotExist()
        assertEquals(level, rule.onNodeWithTag("level_squat").fetchSemanticsNode().config.first { it.key.name == "Text" }.value.toString())
        assertEquals(rest, rest())
        rule.onNodeWithTag("preview_format_PAIRS").assertIsSelected()
    }

    // R8.2-T03 (G3): the included parts are blocks in the Preview, editable, and the edits survive like all others
    @Test fun includedWarmupConditioningAndCooldownAreBlocksInThePreview() {
        app.getSharedPreferences("train", Context.MODE_PRIVATE).edit().putBoolean("warmup", true).putBoolean("conditioning", true).putBoolean("cooldown", true).putString("cooldown_mode", "STRETCHING").commit()
        show(); rule.waitForIdle()
        rule.onNodeWithTag("warmup_caption").performScrollTo().assertExists()
        rule.onNodeWithTag("conditioning_caption").performScrollTo().assertExists()
        rule.onNodeWithTag("cooldown_caption").performScrollTo().assertExists()
        rule.onNodeWithTag("conditioning_item_0").performScrollTo().assertExists()
        val first = rule.onAllNodes(hasTestTag("conditioning_item_0")).fetchSemanticsNodes().size
        rule.onNodeWithTag("conditioning_minutes_plus").performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("conditioning_minutes_value").assertTextEquals("7")   // the circuit then fits as much of the 7 minutes as whole rounds allow
        rule.onNodeWithTag("conditioning_remove_0").performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("warmup_remove_0").performScrollTo().performClick(); rule.waitForIdle()
        assertEquals(1, first)
        for (i in 0..5) rule.onNodeWithTag("exercise_cond-$i").assertDoesNotExist()   // the finisher is not one of the round tiles
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

    @Test fun cardsShowTheMusclesWorked() {
        show(); rule.waitForIdle()
        rule.onNodeWithTag("muscles_push").assertExists()
        // V09 (R9): the body figure replaces the muscle names on the card; the names stay in its screen-reader description
        fun desc(tag: String) = rule.onNodeWithTag(tag).fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.ContentDescription].joinToString()
        assertTrue(desc("muscles_push"), desc("muscles_push").contains("Chest"))
        assertTrue(desc("muscles_core"), desc("muscles_core").contains("Abs"))
        rule.onNodeWithTag("thumb_pushup-incline", useUnmergedTree = true).assertExists()
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

    @Test fun previewListsTheEquipmentTheExercisesNeed() {
        show(); rule.waitForIdle()
        rule.onNodeWithTag("equipment_needed").assertExists()
        rule.onNodeWithText("Equipment needed", ignoreCase = true).assertExists()
    }

    @Test fun todayOnlySwapLeavesTheUsualPlanAlone() {
        show(); rule.waitForIdle()
        openSwap("push")
        rule.onNodeWithTag("swap_option_pushup-knee").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick); rule.waitForIdle()
        assertEquals("pushup-incline", io.github.gonbei774.calisthenicsmemory.ui.screens.RoutineStore.load(app).slots.first { it.id == "push" }.preferredVariationId)
    }

    @Test fun kettlebellCardsShowTheBellWeightAndBodyweightCardsDoNot() {
        show(); rule.waitForIdle()
        openSwap("hinge")
        rule.onNodeWithTag("swap_other_types").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick); rule.waitForIdle()
        rule.onNodeWithTag("swap_option_kettlebell-deadlift").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick); rule.waitForIdle()
        rule.onNodeWithTag("exercise_name_hinge").assertTextEquals("Controlled Kettlebell Deadlift")
        rule.onNodeWithTag("exercise_target_hinge").assertTextContains("· 12 kg", substring = true) // Home profile seed bell
        rule.onNodeWithTag("exercise_target_push").assertTextEquals("6 reps")
    }

    @Test fun timelineIsCollapsedUntilOpened() {
        show(); rule.waitForIdle()
        rule.onNodeWithTag("timeline_list").assertDoesNotExist()
        rule.onNodeWithTag("timeline_toggle").performScrollTo().performClick()
        rule.onNodeWithTag("timeline_list").assertExists()
    }

    @Test fun notesShowAtMostTwoLinesAndTheRestBehindMore() {
        // a plan that fits has no notes and no row; without the band the pull is replaced and the plan says so
        val home = io.github.gonbei774.calisthenicsmemory.ui.screens.ProfileStore.selected(app)
        io.github.gonbei774.calisthenicsmemory.ui.screens.ProfileStore.upsert(app, home.copy(items = home.items.filter { it.equipmentId != "resistance-band" }))
        show(); rule.waitForIdle()
        // R8.1-T08 (F10): was asserting the "unreviewed draft" bubble; the row now only points at the notes, and no draft bubble exists anywhere in the Preview
        rule.onNodeWithTag("info_row").assertExists()
        rule.onNodeWithTag("plan_note_0").assertExists()
        rule.onNodeWithTag("plan_note_2").assertDoesNotExist()   // at most two lines until More is opened
        rule.onNodeWithText("Check the notes", substring = true).assertDoesNotExist()
        rule.onNodeWithText("unreviewed draft", substring = true).assertDoesNotExist()
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

    @Test fun restDurationCanBeChangedAndTheStartedPlanUsesTheUpdatedTime() {
        val prefs = io.github.gonbei774.calisthenicsmemory.ui.screens.PrefsStore.load(app)
        io.github.gonbei774.calisthenicsmemory.ui.screens.PrefsStore.save(app, prefs.copy(stretchOn = false))
        show(); rule.waitForIdle()
        val original = rule.onNodeWithTag("rest_push_value").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.ContentDescription].joinToString()
        val beforeHeader = rule.onNodeWithTag("preview_header").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.Text].joinToString()
        val originalSeconds = original.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
        val otherRestSeconds = listOf("squat", "pull", "core", "hinge", "core2").associateWith { slot ->
            val clock = rule.onNodeWithTag("rest_${slot}_value").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.ContentDescription].joinToString()
            clock.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
        }
        val updatedLabel = "%d:%02d".format((originalSeconds + 15) / 60, (originalSeconds + 15) % 60)
        rule.onNodeWithTag("rest_push_plus").performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("rest_push_value").assertContentDescriptionEquals(updatedLabel)
        val afterHeader = rule.onNodeWithTag("preview_header").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.Text].joinToString()
        assertNotEquals(beforeHeader, afterHeader)
        rule.onNodeWithTag("start_button").performClick()
        val intent = shadowOf(app).nextStartedService
        val plan = Json { ignoreUnknownKeys = true }.decodeFromString(WorkoutPlan.serializer(), intent.getStringExtra(WorkoutSessionService.EXTRA_PLAN)!!)
        val pushRests = plan.blocks.filter { it.type == BlockType.PASSIVE_RECOVERY && it.slotId == "push" }
        assertTrue(pushRests.isNotEmpty())
        assertTrue(pushRests.all { it.durationSeconds == originalSeconds + 15 })
        otherRestSeconds.forEach { (slot, seconds) ->
            assertTrue(plan.blocks.filter { it.type == BlockType.PASSIVE_RECOVERY && it.slotId == slot }.all { it.durationSeconds == seconds })
        }
        assertEquals(plan.blocks.sumOf { it.durationSeconds }, plan.plannedDurationSeconds)
    }

    @Test fun aProgressRefreshDoesNotDiscardPreviewRestEdits() {
        val prefs = io.github.gonbei774.calisthenicsmemory.ui.screens.PrefsStore.load(app)
        io.github.gonbei774.calisthenicsmemory.ui.screens.PrefsStore.save(app, prefs.copy(stretchOn = false))
        lateinit var firstData: TrainData
        lateinit var updateData: (TrainData) -> Unit
        rule.setContent {
            CalisthenicsMemoryTheme(darkTheme = false) {
                val loaded = rememberTrainData().value
                var data by remember { mutableStateOf(loaded) }
                SideEffect { firstData = data; updateData = { data = it } }
                PreviewScreen(onBack = {}, onStarted = {}, dataOverride = data)
            }
        }
        rule.waitForIdle()
        val original = rule.onNodeWithTag("rest_push_value").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.ContentDescription].joinToString()
        val originalSeconds = original.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
        val changed = "%d:%02d".format((originalSeconds + 15) / 60, (originalSeconds + 15) % 60)
        rule.onNodeWithTag("rest_push_plus").performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("rest_push_value").assertContentDescriptionEquals(changed)

        rule.runOnUiThread { updateData(firstData.copy(routine = firstData.routine.copy(name = "Refreshed routine"))) }
        rule.waitForIdle()
        rule.onNodeWithTag("rest_push_value").assertContentDescriptionEquals(changed)
        rule.onNodeWithTag("edits_reset").assertExists()
    }

    @Test fun resettingAPreviewRestChoiceRestoresItsOriginalDuration() {
        val prefs = io.github.gonbei774.calisthenicsmemory.ui.screens.PrefsStore.load(app)
        io.github.gonbei774.calisthenicsmemory.ui.screens.PrefsStore.save(app, prefs.copy(stretchOn = false))
        show(); rule.waitForIdle()
        val original = rule.onNodeWithTag("rest_push_value").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.ContentDescription].joinToString()
        rule.onNodeWithTag("rest_push_plus").performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("edits_reset").performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("rest_push_value").assertContentDescriptionEquals(original)
        rule.onNodeWithTag("edits_reset").assertDoesNotExist()
    }

    @Test fun aPlainRestStillOpensItsBreakPickerFromTheChangeButton() {
        val prefs = io.github.gonbei774.calisthenicsmemory.ui.screens.PrefsStore.load(app)
        io.github.gonbei774.calisthenicsmemory.ui.screens.PrefsStore.save(app, prefs.copy(stretchOn = false))
        show(); rule.waitForIdle()
        rule.onNodeWithTag("break_change_push").performScrollTo().performClick(); rule.waitForIdle(); rule.mainClock.advanceTimeBy(1_000); rule.waitForIdle()
        rule.onNodeWithTag("break_option_auto").assertExists()
        rule.onNodeWithTag("break_option_auto").performSemanticsAction(click); rule.waitForIdle()
        rule.onNodeWithTag("rest_push_plus").assertExists()
    }

    // ---- V16: the editor
    private val click = androidx.compose.ui.semantics.SemanticsActions.OnClick

    @Test fun theLevelStepperChangesTheTargetAndStaysWithinOneToFive() {
        show(); rule.waitForIdle()
        val before = rule.onNodeWithTag("exercise_target_push").fetchSemanticsNode().config.first { it.key.name == "Text" }.value.let { (it as List<*>).first().toString() }
        rule.onNodeWithTag("level_plus_push").performClick(); rule.waitForIdle()
        val after = rule.onNodeWithTag("exercise_target_push").fetchSemanticsNode().config.first { it.key.name == "Text" }.value.let { (it as List<*>).first().toString() }
        assertNotEquals(before, after)
        rule.onNodeWithTag("level_push").assertTextContains("Level 2 of 5")
        rule.onNodeWithTag("level_minus_push").performClick(); rule.onNodeWithTag("level_minus_push").assertIsNotEnabled()
        rule.onNodeWithTag("exercise_target_squat").assertExists() // other exercises untouched
    }

    @Test fun longPressRemovesAnExerciseAfterConfirmationAndUndoRestoresIt() {
        show(); rule.waitForIdle()
        rule.onNodeWithTag("exercise_core").performScrollTo().performTouchInput { longClick() }; rule.waitForIdle()
        rule.onNodeWithTag("remove_confirm").assertExists()
        rule.onNodeWithTag("remove_confirm").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("exercise_core").assertDoesNotExist()
        rule.onNodeWithTag("edits_reset").performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("exercise_core").assertExists()
    }

    @Test fun removingTheOnlyExerciseKeepsAnAddPathAndCanUndoJustThatRemoval() {
        TrainSettingsStore.save(app, TrainSettingsStore.load(app).copy(exercisesPerRound = 1,
            betweenSetMode = BetweenSetMode.REST, flow = false, stretchOn = false))
        show(); rule.waitForIdle()
        rule.onNodeWithTag("exercise_push").performScrollTo().performTouchInput { longClick() }; rule.waitForIdle()
        rule.onNodeWithTag("remove_confirm").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("preview_empty").assertExists()
        rule.onNodeWithTag("start_button").assertIsNotEnabled()
        rule.onNodeWithTag("add_empty").performClick(); rule.waitForIdle()
        rule.mainClock.advanceTimeBy(1_000); rule.waitForIdle()
        rule.onNodeWithTag("add_categories").assertExists()
        rule.onNodeWithTag("add_cat_stretches").assertDoesNotExist()
        rule.onNodeWithTag("add_cat_push").performSemanticsAction(click); rule.waitForIdle()
        rule.onNodeWithTag("add_option_pushup-knee").performScrollTo().performSemanticsAction(click); rule.waitForIdle()
        rule.onNodeWithTag("preview_empty").assertDoesNotExist()
        rule.onNodeWithTag("exercise_add-pushup-knee").assertExists()
        rule.onNodeWithTag("start_button").assertIsEnabled()
        rule.onNodeWithTag("exercise_add-pushup-knee").performScrollTo().performTouchInput { longClick() }; rule.waitForIdle()
        rule.onNodeWithTag("remove_confirm").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("remove_undo").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("exercise_add-pushup-knee").assertExists()
        rule.onNodeWithTag("exercise_push").assertDoesNotExist()
    }

    @Test fun removingTheFinalRowLeavesTheOtherExercisesAndSurvivesReopening() {
        TrainSettingsStore.save(app, TrainSettingsStore.load(app).copy(exercisesPerRound = 2,
            betweenSetMode = BetweenSetMode.REST, flow = false, stretchOn = false))
        var visible by mutableStateOf(true)
        rule.setContent { CalisthenicsMemoryTheme(darkTheme = false) {
            if (visible) PreviewScreen(onBack = {}, onStarted = {}) else androidx.compose.foundation.layout.Box {}
        } }
        rule.waitForIdle()
        val cards = rule.onAllNodes(SemanticsMatcher("exercise cards") { node ->
            val tag = androidx.compose.ui.semantics.SemanticsProperties.TestTag
            node.config.contains(tag) && node.config[tag].startsWith("exercise_") &&
                node.config.contains(androidx.compose.ui.semantics.SemanticsActions.OnLongClick)
        }).fetchSemanticsNodes().map { it.config[androidx.compose.ui.semantics.SemanticsProperties.TestTag] }
        assertEquals(2, cards.size)
        val first = cards.first(); val last = cards.last()
        rule.onNodeWithTag(last).performScrollTo().performTouchInput { longClick() }; rule.waitForIdle()
        rule.onNodeWithTag("remove_confirm").performClick(); rule.waitForIdle()
        rule.onNodeWithTag(last).assertDoesNotExist()
        rule.onNodeWithTag(first).assertExists()
        visible = false; rule.waitForIdle(); visible = true; rule.waitForIdle()
        rule.onNodeWithTag(last).assertDoesNotExist()
        rule.onNodeWithTag(first).assertExists()
        rule.onNodeWithTag("start_button").assertIsEnabled()
    }

    @Test fun theRoundViewHasNoSetTabsAndOneAllSetsHeader() {
        show(); rule.waitForIdle()
        for (i in 1..4) rule.onNodeWithTag("set_tab_$i").assertDoesNotExist()
        rule.onNodeWithTag("all_sets_header").assertExists().assertTextContains("All sets", substring = true)
        rule.onNodeWithTag("all_sets_header").assertTextContains("min each", substring = true)
        rule.onNodeWithText("Changes apply to all sets", ignoreCase = true).assertExists()
    }

    @Test fun breakRowsShowTheStretchAndCanBeChangedAndAFixedStretchIsUsed() {
        show(); rule.waitForIdle()
        rule.onNodeWithTag("break_push").assertExists()
        rule.onNodeWithTag("break_push").performScrollTo().performClick(); rule.waitForIdle(); rule.mainClock.advanceTimeBy(1_000); rule.waitForIdle()
        rule.onNodeWithTag("break_option_stretch-pigeon").performSemanticsAction(click); rule.waitForIdle()
        rule.onNodeWithTag("break_push").assertTextContains("Pigeon", substring = true)
    }

    @Test fun theBreakAndPlusShareARowAndTheBreakFillsTheRemainingWidth() {   // R7.2-T03 (F4)
        show(); rule.waitForIdle()
        rule.onNodeWithTag("break_push").performScrollTo().assertExists()
        rule.onNodeWithText("change").assertDoesNotExist()                               // the word is gone, the icon stays
        rule.onNodeWithTag("break_push").assertContentDescriptionEquals("change")
        val card = rule.onNodeWithTag("break_push").fetchSemanticsNode().boundsInRoot
        val plus = rule.onNodeWithTag("add_after_push").fetchSemanticsNode().boundsInRoot
        assertTrue("the + shares the card row", kotlin.math.abs(plus.center.y - card.center.y) < 16 * rule.density.density)
        assertTrue("the card fills the available row", card.width > plus.width * 4)
        assertTrue("the + is to the right of the card", plus.left >= card.right)
    }

    @Test fun thePlusBetweenBlocksAddsAnExerciseAfterThatSlot() {
        show(); rule.waitForIdle()
        rule.onNodeWithTag("add_after_squat").performScrollTo().performClick(); rule.waitForIdle(); rule.mainClock.advanceTimeBy(1_000); rule.waitForIdle()
        rule.onNodeWithTag("add_option_pushup-diamond").assertDoesNotExist()   // R7-T10 (D11): nothing is listed before a category is chosen
        rule.onNodeWithTag("add_cat_push").performSemanticsAction(click); rule.waitForIdle()
        rule.onNodeWithTag("add_option_squat-air").assertDoesNotExist()        // only that category's list
        rule.onNodeWithTag("add_option_pushup-diamond").performSemanticsAction(click); rule.waitForIdle()
        rule.onNodeWithTag("exercise_add-pushup-diamond").assertExists()
        val order = mutableListOf<String>()
        fun walk(n: androidx.compose.ui.semantics.SemanticsNode) { n.config.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.TestTag)?.let { order += it }; n.children.forEach { walk(it) } }
        walk(rule.onRoot().fetchSemanticsNode())
        assertTrue(order.indexOf("exercise_squat") < order.indexOf("exercise_add-pushup-diamond") && order.indexOf("exercise_add-pushup-diamond") < order.indexOf("exercise_pull"))
    }

    @Test fun specialistExerciseCategoriesRemainAvailableThroughManualAdd() {
        show(); rule.waitForIdle()
        rule.onNodeWithTag("add_start").performScrollTo().performClick(); rule.waitForIdle(); rule.mainClock.advanceTimeBy(1_000); rule.waitForIdle()
        for (category in listOf("squat", "conditioning", "warmup", "stretches")) {   // R8.2-T01: no Power category, the jumps are under Squat
            rule.onNodeWithTag("add_cat_$category").performScrollTo().assertExists()
        }
        rule.onNodeWithTag("add_cat_power").assertDoesNotExist()
        rule.onNodeWithTag("add_cat_squat").performSemanticsAction(click); rule.waitForIdle()
        rule.onNodeWithTag("add_option_jump-squat").performScrollTo().assertExists()
    }

    @Test fun theSwapSheetShowsTheWholeFamilyAndOtherTypesBehindAButton() {
        show(); rule.waitForIdle()
        openSwap("push")
        for (id in listOf("pushup-incline", "pushup-knee", "pushup-standard", "pushup-diamond")) rule.onNodeWithTag("swap_option_$id").assertExists()
        rule.onNodeWithTag("swap_option_squat-air").assertDoesNotExist()
        rule.onNodeWithTag("swap_other_types").performSemanticsAction(click); rule.waitForIdle()
        rule.onNodeWithTag("swap_option_squat-air").assertExists()
    }

    @Test fun tappingOtherTypesShowsTheSameFamilyGroupFirstInAnAccentBoxAndEveryOtherTabStays() {   // R7.2-T02 (F2, F3, O3)
        show(); rule.waitForIdle()
        openSwap("push")
        rule.onNodeWithTag("swap_same_family").assertDoesNotExist()
        rule.onNodeWithTag("swap_other_types").performSemanticsAction(click); rule.waitForIdle(); rule.mainClock.advanceTimeBy(1_000); rule.waitForIdle()
        rule.onNodeWithTag("swap_same_family").assertExists()
        rule.onNodeWithTag("swap_same_family_caption", useUnmergedTree = true).assertTextContains("SAME FAMILY: PUSH", ignoreCase = true)
        rule.onNodeWithTag("swap_same_family").assertIsDisplayed()       // the sheet scrolled to the first new item
        for (tab in listOf("pull", "row", "squat", "hips", "core", "calves")) rule.onNodeWithTag("swap_group_$tab", useUnmergedTree = true).assertExists()
        rule.onNodeWithTag("swap_option_squat-air").assertExists()
        rule.onNodeWithTag("swap_other_types").performSemanticsAction(click); rule.waitForIdle()    // closes again
        rule.onNodeWithTag("swap_same_family").assertDoesNotExist()
    }

    // ---- R7-T10 (D4): the full timeline is the editor (it replaces the V17 "Detailed edit" switch)
    @Test fun theFullTimelineListsEverySetWithItsNumberAndAStretchCanDifferPerSet() {
        show(); rule.waitForIdle()
        rule.onNodeWithTag("detailed_switch").assertDoesNotExist()
        rule.onNodeWithTag("timeline_toggle").performScrollTo().performClick(); rule.waitForIdle()
        for (st in 1..4) rule.onNodeWithTag("set_header_$st").performScrollTo().assertExists()
        rule.onNodeWithTag("round_badge_push_s3", useUnmergedTree = true).performScrollTo().assertTextEquals("3")
        rule.onNodeWithTag("break_push_s2").performScrollTo().performClick(); rule.waitForIdle(); rule.mainClock.advanceTimeBy(1_000); rule.waitForIdle()
        rule.onNodeWithTag("break_option_stretch-frog").performSemanticsAction(click); rule.waitForIdle()
        rule.onNodeWithTag("break_push_s2").performScrollTo().assertTextContains("Frog", substring = true)
        rule.onNodeWithTag("break_push_s3").performScrollTo().assert(!hasText("Frog", substring = true)) // the other sets keep their own (automatic) stretch
    }

    // The "type a number" dialog has a text field. Robolectric's Compose clock never goes idle while a dialog with a text field is open
    // (the test hangs for minutes), so the dialog itself is not driven here. What it produces is covered by the planner tests
    // (DetailedEditTest: the free number is marked, shown, and excluded from progression) and the target cell's tag is checked.
    @Test fun inTheFullTimelineEachSetsNumberIsATapTargetAndEveryTileHasItsControls() {
        show(); rule.waitForIdle()
        rule.onNodeWithTag("timeline_toggle").performScrollTo().performClick(); rule.waitForIdle()
        for (st in 1..4) rule.onNodeWithTag("exercise_target_push_s$st").performScrollTo().assertHasClickAction()
        rule.onNodeWithTag("level_push_s2").performScrollTo().assertTextContains("Level", substring = true)
        for (t in listOf("swap_push_s2", "level_plus_push_s2", "level_minus_push_s2")) rule.onNodeWithTag(t).performScrollTo().assertExists()
    }

    // R7-T10 (O1, D12): under a range rule the stepper steps through the exercises of the chain and the numbers stay the range
    @Test fun underARepRangeTheStepperMovesToTheNextExerciseOfTheChainAndKeepsTheRange() {
        show(); rule.waitForIdle()
        rule.onNodeWithTag("rule_REP_RANGE").performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("rule_range_note").assertExists()
        val nameBefore = rule.onNodeWithTag("exercise_name_push").fetchSemanticsNode().config.first { it.key.name == "Text" }.value.let { (it as List<*>).first().toString() }
        val targetBefore = rule.onNodeWithTag("exercise_target_push").fetchSemanticsNode().config.first { it.key.name == "Text" }.value.let { (it as List<*>).first().toString() }
        rule.onNodeWithTag("level_plus_push").performScrollTo().performClick(); rule.waitForIdle()
        val nameAfter = rule.onNodeWithTag("exercise_name_push").fetchSemanticsNode().config.first { it.key.name == "Text" }.value.let { (it as List<*>).first().toString() }
        assertEquals("Incline Push-Up", nameBefore)
        assertEquals("Standard Push-Up", nameAfter)
        assertNotEquals(nameBefore, nameAfter)
        val targetAfter = rule.onNodeWithTag("exercise_target_push").fetchSemanticsNode().config.first { it.key.name == "Text" }.value.let { (it as List<*>).first().toString() }
        assertTrue(targetBefore.contains("5–8 reps"))
        assertTrue(targetAfter.contains("5–8 reps"))
        assertFalse("standard push-ups use a bilateral target", targetAfter.endsWith("per side"))
        rule.onNodeWithTag("level_push").assertTextContains("Exercise", substring = true)
    }

    // ---- V21: the progression rule picker
    @Test fun theRuleDefaultsToAppLevelsAndRepRangeShowsRangesAndThreeSets() {
        show(); rule.waitForIdle()
        rule.onNodeWithTag("rule_APP_LEVELS").performScrollTo().assertIsSelected()
        rule.onNodeWithTag("rule_REP_RANGE").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("rule_REP_RANGE").assertIsSelected()
        rule.onNodeWithTag("rule_hint").assertTextContains("3 sets of 5–8 reps", substring = true)
        rule.onNodeWithTag("exercise_target_squat").performScrollTo().assertTextContains("5–8 reps", substring = true)
        rule.onNodeWithTag("preview_header").assertTextContains("3 rounds", substring = true)   // the rule's sets
        rule.onNodeWithTag("rule_sets_value").assertDoesNotExist()
    }

    @Test fun customHasEditableSetsRangeAndSessions() {
        show(); rule.waitForIdle()
        rule.onNodeWithTag("rule_CUSTOM").performScrollTo().performClick(); rule.waitForIdle()
        for (t in listOf("rule_sets", "rule_from", "rule_to", "rule_sessions")) rule.onNodeWithTag("${t}_value").assertExists()
        rule.onNodeWithTag("rule_to_plus").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("rule_hint").assertTextContains("5–9", substring = true)
        rule.onNodeWithTag("rule_sessions_plus").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("rule_hint").assertTextContains("2 session", substring = true)
        rule.onNodeWithTag("rule_APP_LEVELS").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("rule_sets_value").assertDoesNotExist()
    }
}
