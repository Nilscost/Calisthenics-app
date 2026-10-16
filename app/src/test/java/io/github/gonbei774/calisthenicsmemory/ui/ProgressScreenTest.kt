package io.github.gonbei774.calisthenicsmemory.ui

import io.github.gonbei774.calisthenicsmemory.ui.screens.ProfileStore
import android.content.Context
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import io.github.gonbei774.calisthenicsmemory.ui.progress.ProgressScreen
import io.github.gonbei774.calisthenicsmemory.ui.screens.GoalStore
import io.github.gonbei774.calisthenicsmemory.ui.screens.LevelStore
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalisthenicsMemoryTheme
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
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
    private fun revealTreeNode(id: String) { rule.onNodeWithTag("tree_rows").performScrollToNode(hasTestTag("tree_node_$id")) }
    private fun chooseRouteContaining(id: String) {
        val catalog = io.github.gonbei774.calisthenicsmemory.ui.train.loadCatalog(ctx)
        val route = catalog.progressionRoutes.firstOrNull { id in it.mainPath } ?: return
        if (rule.onAllNodesWithTag("route_chip_${route.id}").fetchSemanticsNodes().isEmpty()) {
            rule.onNodeWithTag("all_progressions").performClick(); rule.waitForIdle()
        }
        rule.onNodeWithTag("route_chip_${route.id}").performScrollTo().performClick(); rule.waitForIdle()
    }
    private fun openFromSearch(id: String) {
        val variation = io.github.gonbei774.calisthenicsmemory.ui.train.loadCatalog(ctx).variation(id) ?: error("Missing variation $id")
        rule.onNodeWithTag("progress_search").performTextInput(variation.name); rule.waitForIdle()
        rule.onNodeWithTag("search_result_$id").performClick(); rule.waitForIdle()
    }

    @Test fun pushTreeShowsNodesWithStarsAndStates() {
        LevelStore.save(ctx, mapOf("pushup-standard" to 3))
        show()
        chooseRouteContaining("pushup-standard")
        assertTrue(desc("tree_node_pushup-standard").contains("2 of 5 stars")) // D8: the two levels below the starting level count
        assertTrue(desc("tree_node_pushup-standard").contains("Training now"))
        assertTrue(desc("tree_node_pushup-incline").contains("Ready to start") || desc("tree_node_pushup-incline").contains("Needs equipment"))
        revealTreeNode("pushup-one-arm")
        assertTrue(desc("tree_node_pushup-one-arm").contains("Locked"))
        chooseRouteContaining("planche-lean")
        revealTreeNode("planche-lean")
        rule.onNodeWithTag("tree_node_planche-lean").assertExists() // the planche chain sits under the push-ups
    }

    // R7.2-T06 (F7): the "Too easy: n of 2" line is gone from the exercise detail (it was R7-T05)
    @Test fun theDetailNoLongerShowsATooEasyCounter() {
        LevelStore.save(ctx, mapOf("pushup-standard" to 3))
        show()
        chooseRouteContaining("pushup-standard")
        revealTreeNode("pushup-standard")
        rule.onNodeWithTag("tree_node_pushup-standard").performSemanticsAction(SemanticsActions.OnClick); rule.waitForIdle()
        rule.onNodeWithTag("panel_open_detail").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("detail_too_easy").assertDoesNotExist()
        rule.onNodeWithText("Too easy:", substring = true).assertDoesNotExist()
    }

    // R7.2-T06 (F7): the cards read "Move on to <exercise> level <n>?" and "Move on to <next exercise>?"
    private fun card(s: app.calisthenics.domain.progression.Suggestion) {
        val catalog = io.github.gonbei774.calisthenicsmemory.ui.train.loadCatalog(ctx)
        rule.setContent { CalisthenicsMemoryTheme(darkTheme = false) { io.github.gonbei774.calisthenicsmemory.ui.progress.SuggestionCard(catalog, s, onDismiss = {}, onAccept = {}) } }
    }
    @Test fun theRaiseCardReadsMoveOnToTheExerciseAndLevel() {
        card(app.calisthenics.domain.progression.Suggestion.RaiseLevel("pushup-standard", 5))
        rule.onNodeWithTag("suggestion_text").assertTextContains("Move on to Standard Push-Up level 5?", substring = true)
    }
    @Test fun theNextExerciseCardReadsMoveOnToTheNextExercise() {
        card(app.calisthenics.domain.progression.Suggestion.NextExercise("pushup-standard", "pushup-diamond", 1))
        rule.onNodeWithTag("suggestion_text").assertTextContains("Move on to Diamond Push-Up?", substring = true)
        rule.onNodeWithText("twice", substring = true).assertDoesNotExist()
    }

    private fun openDetailOf(id: String) {
        revealTreeNode(id)
        rule.onNodeWithTag("tree_node_$id").performSemanticsAction(SemanticsActions.OnClick); rule.waitForIdle()
        rule.onNodeWithTag("panel_open_detail").performClick(); rule.waitForIdle()
    }

    // R7-T11 (D5): Edit mode sets the level by hand; V-up at level 3 shows 3 stars
    @Test fun editModeSetsTheLevelByHandAndTheStarsFollow() {
        show()
        rule.onNodeWithTag("tab_chip_core").performScrollTo().performClick(); rule.waitForIdle()
        chooseRouteContaining("v-up")
        openDetailOf("v-up")
        rule.onNodeWithTag("detail_edit").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("detail_set_level_3").performClick(); rule.waitForIdle()
        assertEquals(3, io.github.gonbei774.calisthenicsmemory.ui.screens.LevelStore.load(ctx).let { app.calisthenics.domain.progression.selfAssessmentFor("v-up", it["v-up"]!!).achievedUpTo })
        rule.waitUntil(5_000) { runCatching { rule.onNodeWithTag("detail_level_4").assertTextContains("NOW", substring = true, ignoreCase = true) }.isSuccess }   // the progress is re-read in the background
        rule.onNodeWithTag("detail_level_4").assertTextContains("NOW", substring = true, ignoreCase = true)
        rule.onNodeWithTag("detail_level_3").assertTextContains("DONE", substring = true, ignoreCase = true)
    }

    // R7-T11 (D6): Needs and Unlocks open that exercise; Back returns to the previous detail (a stack)
    @Test fun needsAndUnlocksAreLinksAndBackWalksTheStack() {
        show()
        chooseRouteContaining("pushup-standard")
        openDetailOf("pushup-standard")
        rule.onNodeWithTag("detail_title").assertTextContains("Standard Push-Up", substring = true, ignoreCase = true)
        val next = rule.onAllNodes(hasTestTag("detail_unlocks")).fetchSemanticsNodes().size
        assertEquals(1, next)
        rule.onNodeWithTag("detail_unlock_pushup-feet-elevated", useUnmergedTree = true).performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("detail_title").assertTextContains("Feet-Elevated", substring = true, ignoreCase = true)
        rule.onNodeWithTag("detail_back").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("detail_title").assertTextContains("Standard Push-Up", substring = true, ignoreCase = true)
        rule.onNodeWithTag("detail_back").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("detail_title").assertDoesNotExist()
    }

    // R7-T11 (O2): the planche lean tile says it needs the plank, and tapping it opens the plank
    @Test fun plancheLeanNeedsPlankIsATappableNote() {
        show()
        chooseRouteContaining("planche-lean")
        revealTreeNode("planche-lean")
        rule.onNodeWithTag("tree_need_planche-lean", useUnmergedTree = true).assertHasClickAction()
            .performScrollTo().assertTextContains("Plank", substring = true)
            .performSemanticsAction(SemanticsActions.OnClick); rule.waitForIdle()
        rule.onNodeWithTag("detail_title").assertTextContains("Plank", substring = true, ignoreCase = true)
    }

    // R7-T12 (D9): the Jefferson curl sits in Hips and back and its detail carries the pay-attention points
    @Test fun jeffersonCurlDetailShowsItsPayAttentionPoints() {
        show()
        openFromSearch("jefferson-curl")
        rule.onNodeWithTag("detail_caution").performScrollTo().assertExists()
        rule.onNodeWithText("Pay attention to: stop at any back pain.").assertExists()
    }

    @Test fun theGoalPicksTheFamilyTabAndOutlinesItsChain() {
        GoalStore.save(ctx, "hspu")
        show()
        revealTreeNode("hspu-wall")
        rule.onNodeWithTag("tree_node_hspu-wall").assertExists()
        revealTreeNode("pike-pushup")
        rule.onNodeWithTag("tree_node_pike-pushup").assertExists()
        rule.onNodeWithText("Goal: Handstand push-up").assertIsDisplayed()
    }

    @Test fun familyPickerSwitchesTrees() {
        show()
        rule.onNodeWithTag("tab_chip_squat").performClick(); rule.waitForIdle()
        chooseRouteContaining("squat-air")
        rule.onNodeWithTag("tree_node_squat-air").assertExists()
        rule.onNodeWithTag("tree_node_pushup-standard").assertDoesNotExist()
        rule.onNodeWithTag("tab_chip_row").performClick(); rule.waitForIdle()
        chooseRouteContaining("archer-row")
        revealTreeNode("archer-row")
        rule.onNodeWithTag("tree_node_archer-row").assertExists() // F15: the band row now leads somewhere
        rule.onNodeWithTag("tab_chip_core").performScrollTo().performClick(); rule.waitForIdle()
        chooseRouteContaining("dead-bug")
        revealTreeNode("dead-bug")
        rule.onNodeWithTag("tree_node_dead-bug").assertExists()
    }

    @Test fun aliasesAreSearchableAndShownOnExerciseDetail() {
        show()
        rule.onNodeWithTag("progress_search").performTextInput("Teaser (Pilates)"); rule.waitForIdle()
        rule.onNodeWithTag("search_result_v-up").assertExists().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("detail_title").assertTextContains("V-UP", substring = true)
        rule.onNodeWithTag("detail_aliases").assertTextContains("Also known as: V-Up (Army), Teaser (Pilates)", substring = true)
    }

    @Test fun newProgressTabsAndRoutineOnlyDrillsAreReachableAsExpected() {
        val profiles = ProfileStore.load(ctx)
        val home = profiles.first { it.id == "home" }
        if (home.items.none { it.equipmentId == "rings" }) ProfileStore.save(ctx, profiles.map { if (it.id == "home") it.copy(items = it.items + app.calisthenics.domain.model.EquipmentItem("rings")) else it })
        show()
        for (tab in listOf("rings", "power", "conditioning", "prehab", "pilates")) {
            rule.onNodeWithTag("tab_chip_$tab").performScrollTo().performClick(); rule.waitForIdle()
            val family = when (tab) {
                "rings" -> "ring-support-hold"
                "power" -> "jump-squat"
                "conditioning" -> "bear-crawl"
                "prehab" -> "external-rotation-band"
                else -> "pilates-roll-up"
            }
            val catalog = io.github.gonbei774.calisthenicsmemory.ui.train.loadCatalog(ctx)
            if (catalog.progressionRoutes.any { family in it.mainPath }) {
                chooseRouteContaining(family)
                revealTreeNode(family)
                rule.onNodeWithTag("tree_node_$family").assertExists()
            } else {
                val variation = catalog.variation(family) ?: continue
                rule.onNodeWithTag("progress_search").performTextClearance()
                rule.onNodeWithTag("progress_search").performTextInput(variation.name); rule.waitForIdle()
                rule.onNodeWithTag("search_result_$family").assertExists()
                rule.onNodeWithTag("tree_node_$family").assertDoesNotExist()
            }
        }
        val drillName = io.github.gonbei774.calisthenicsmemory.ui.train.loadCatalog(ctx).variations.first { it.routineOnly }.name
        rule.onNodeWithTag("progress_search").performTextClearance()
        rule.onNodeWithTag("progress_search").performTextInput(drillName); rule.waitForIdle()
        rule.onNodeWithTag("search_result_${io.github.gonbei774.calisthenicsmemory.ui.train.loadCatalog(ctx).variations.first { it.name == drillName }.id}").assertExists()
    }

    @Test fun defaultProgressShowsPoolAndGoalRoutesAndAllProgressionsIsDeliberate() {
        val catalog = io.github.gonbei774.calisthenicsmemory.ui.train.loadCatalog(ctx)
        val tabFamilies = app.calisthenics.domain.tree.TreeTabs.byId("push")!!.familyIds
        val profile = ProfileStore.selected(ctx)
        val eligible: (app.calisthenics.domain.model.ProgressionRoute) -> Boolean = { route ->
            route.mainPath.firstOrNull()?.let { catalog.variation(it)?.familyId in tabFamilies } == true &&
                (route.mainPath.any { id -> catalog.variation(id)?.let { app.calisthenics.domain.equipment.isAvailable(it, profile) } == true } ||
                    route.members.any { it.role == app.calisthenics.domain.model.ProgressionRole.ALTERNATE && catalog.variation(it.variationId)?.let { v -> app.calisthenics.domain.equipment.isAvailable(v, profile) } == true })
        }
        val defaultRoute = catalog.progressionRoutes.firstOrNull { it.defaultPool && eligible(it) }
            ?: return // Legacy fixture catalogs have not yet loaded route metadata.
        val extraRoute = catalog.progressionRoutes.firstOrNull { !it.defaultPool && eligible(it) }
        show(); rule.waitForIdle()
        rule.onNodeWithTag("route_chip_${defaultRoute.id}").assertExists()
        if (extraRoute != null) {
            rule.onNodeWithTag("route_chip_${extraRoute.id}").assertDoesNotExist()
            rule.onNodeWithTag("all_progressions").performClick(); rule.waitForIdle()
            rule.onNodeWithTag("route_chip_${extraRoute.id}").assertExists()
        }
    }

    @Test fun anActiveGoalAddsItsRouteWithoutOpeningAllProgressions() {
        val catalog = io.github.gonbei774.calisthenicsmemory.ui.train.loadCatalog(ctx)
        val profile = ProfileStore.selected(ctx)
        val route = catalog.progressionRoutes.firstOrNull { candidate ->
            !candidate.defaultPool && candidate.mainPath.any { id -> catalog.variation(id)?.let { app.calisthenics.domain.equipment.isAvailable(it, profile) } == true } &&
                candidate.goalIds.any { app.calisthenics.domain.goals.Goals.byId(it) != null }
        } ?: return // Legacy fixture catalogs have not yet loaded route metadata.
        val goalId = route.goalIds.first { app.calisthenics.domain.goals.Goals.byId(it) != null }
        GoalStore.save(ctx, goalId)
        show(); rule.waitForIdle()
        rule.onNodeWithTag("route_chip_${route.id}").assertExists()
        rule.onNodeWithTag("all_progressions").assertTextContains("All progressions")
    }

    @Test fun routeRemainsVisibleWhenOnlyItsBodyweightEntryIsAvailable() {
        val catalog = io.github.gonbei774.calisthenicsmemory.ui.train.loadCatalog(ctx)
        val profile = ProfileStore.selected(ctx)
        val route = catalog.progressionRoutes.firstOrNull { candidate ->
            candidate.mainPath.any { id -> catalog.variation(id)?.let { app.calisthenics.domain.equipment.isAvailable(it, profile) } == true } &&
                candidate.mainPath.any { id -> catalog.variation(id)?.let { !app.calisthenics.domain.equipment.isAvailable(it, profile) } == true }
        } ?: return
        show(); rule.waitForIdle()
        rule.onNodeWithTag("all_progressions").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("route_chip_${route.id}").performScrollTo().performClick(); rule.waitForIdle()
        val unavailableId = route.mainPath.first { id -> catalog.variation(id)?.let { !app.calisthenics.domain.equipment.isAvailable(it, profile) } == true }
        revealTreeNode(unavailableId)
        rule.onNodeWithTag("tree_node_$unavailableId").assertExists()
    }

    @Test fun skillProgressUsesItsNamedRouteInsteadOfTheLegacyFamilyGraph() {
        val catalog = io.github.gonbei774.calisthenicsmemory.ui.train.loadCatalog(ctx)
        val routeAndGoal = catalog.progressionRoutes.firstNotNullOfOrNull { route ->
            val goal = route.goalIds.mapNotNull { app.calisthenics.domain.goals.Goals.byId(it) }.firstOrNull {
                app.calisthenics.domain.goals.Goals.typeOf(it) == app.calisthenics.domain.goals.ObjectiveType.SKILL
            }
            val firstFamily = route.mainPath.firstOrNull()?.let { catalog.variation(it)?.familyId }
            val extra = catalog.variations.firstOrNull { it.familyId == firstFamily && it.id !in route.mainPath && !it.routineOnly && route.mainPath.isNotEmpty() }
            if (goal != null && extra != null) Triple(route, goal, extra) else null
        } ?: return
        show(); rule.waitForIdle()
        rule.onNodeWithTag("progress_mode_skill").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("skill_chip_${routeAndGoal.second.id}").performScrollTo().performClick(); rule.waitForIdle()
        val goalRoutes = catalog.progressionRoutes.filter { routeAndGoal.second.id in it.goalIds }
        if (goalRoutes.size > 1) rule.onNodeWithTag("skill_route_chip_${routeAndGoal.first.id}").performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("tree_node_${routeAndGoal.third.id}").assertDoesNotExist()
        goalRoutes.forEach { route ->
            if (goalRoutes.size > 1) rule.onNodeWithTag("skill_route_chip_${route.id}").performScrollTo().performClick(); rule.waitForIdle()
            route.mainPath.forEach { id ->
                revealTreeNode(id)
                rule.onNodeWithTag("tree_node_$id").assertExists()
            }
        }
    }

    @Test fun heldDemonstrationShowsWrittenCuesWithoutPlayingItsClip() {
        val variation = io.github.gonbei774.calisthenicsmemory.ui.train.loadCatalog(ctx).variations.firstOrNull { it.demonstrationHold } ?: return
        show()
        rule.onNodeWithTag("progress_search").performTextInput(variation.name); rule.waitForIdle()
        rule.onNodeWithTag("search_result_${variation.id}").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("detail_demo_held").assertExists()
        rule.onNodeWithText("Text cue only", substring = true).assertExists()
        rule.onNodeWithTag("detail_demo").assertDoesNotExist()
    }

    @Test fun firstTapSelectsTheNodeAndOpensThePanelAboveTheTabBar() {
        LevelStore.save(ctx, mapOf("pushup-standard" to 3))
        show()
        chooseRouteContaining("pushup-standard")
        rule.onNodeWithTag("node_panel").assertDoesNotExist()
        revealTreeNode("pushup-standard")
        rule.onNodeWithTag("tree_node_pushup-standard").performSemanticsAction(SemanticsActions.OnClick); rule.waitForIdle()
        rule.onNodeWithTag("node_panel").assertIsDisplayed()
        rule.onNodeWithTag("panel_title").assertTextEquals("Standard Push-Up")
        rule.onNodeWithTag("panel_level").assertTextContains("Level 3 of 5", substring = true)
        rule.onNodeWithTag("panel_unlocks").assertExists()
        rule.onNodeWithTag("panel_open_detail").assertIsDisplayed()
        rule.onNodeWithTag("panel_close").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("node_panel").assertDoesNotExist()
    }

    @Test fun aSuggestionCardShowsOneSuggestionAndCanBeClosed() {   // V27
        LevelStore.save(ctx, mapOf("pushup-standard" to 5))     // a solid push-up: dip support is hinted (the home profile has none)
        show(); rule.waitForIdle()
        chooseRouteContaining("pushup-standard")
        rule.onNodeWithTag("suggestion_card").assertIsDisplayed()
        rule.onNodeWithTag("suggestion_text").assertTextContains("opens", substring = true)
        rule.onAllNodesWithTag("suggestion_card").assertCountEquals(1)
        fun text() = rule.onNodeWithTag("suggestion_text").fetchSemanticsNode().config.first { it.key.name == "Text" }.value.let { (it as List<*>).first().toString() }
        val first = text()
        rule.onNodeWithTag("suggestion_dismiss").performClick(); rule.waitForIdle()
        // the closed one does not come back; at most one other may follow, still only one card
        rule.onAllNodesWithTag("suggestion_card").assertCountEquals(rule.onAllNodesWithTag("suggestion_card").fetchSemanticsNodes().size.coerceAtMost(1))
        if (rule.onAllNodesWithTag("suggestion_card").fetchSemanticsNodes().isNotEmpty()) assertNotEquals(first, text())
        rule.onNodeWithTag("tree_area").assertIsDisplayed()      // never blocks the tree
    }

    @Test fun aKettlebellDetailShowsTheWeightTrackFromTheBellInUseUpward() {   // V26
        LevelStore.save(ctx, mapOf("kettlebell-deadlift" to 2))
        show()
        openFromSearch("kettlebell-deadlift")
        rule.onNodeWithTag("weight_step_12000").performScrollTo().assertTextContains("NOW", substring = true)   // the home profile's 12 kg bell
        rule.onNodeWithTag("weight_step_16000").performScrollTo().assertTextContains("Needs a 16 kg kettlebell", substring = true, ignoreCase = true)
        rule.onNodeWithTag("weight_step_24000").performScrollTo().assertExists()
        rule.onNodeWithTag("weight_step_8000").assertDoesNotExist()   // lighter than the bell in use: not shown
    }

    @Test fun theExerciseDetailShowsTheFiveLevelsUnlocksAndCautionsWithoutActionButtons() {
        LevelStore.save(ctx, mapOf("pushup-standard" to 3))
        show()
        chooseRouteContaining("pushup-standard")
        revealTreeNode("pushup-standard")
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
        revealTreeNode("pushup-standard")
        rule.onNodeWithTag("tree_node_pushup-standard").assertExists()
    }

    @Test fun byTypeAndBySkillAreSwitchedUnderTheTitleAndSkillsListTheirChain() {
        show()
        rule.onNodeWithTag("progress_title").assertTextEquals("PROGRESS")
        rule.onNodeWithTag("progress_mode_type").assertIsSelected()
        rule.onNodeWithTag("progress_mode_skill").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("progress_mode_skill").assertIsSelected()
        rule.onNodeWithTag("skill_chip_muscle-up").performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("tree_node_pullup-band-assisted").assertDoesNotExist()
        revealTreeNode("muscle-up-bar")
        rule.onNodeWithTag("tree_node_muscle-up-bar").assertExists()
        rule.onNodeWithTag("tree_node_pushup-standard").assertDoesNotExist()
        rule.onNodeWithTag("tree_node_muscle-up-bar").performSemanticsAction(SemanticsActions.OnClick); rule.waitForIdle()
        rule.onNodeWithTag("panel_title").assertTextEquals("Bar Muscle-Up")
    }

    @Test fun exercisesBelowTheCurrentOneShowFiveStars() {
        LevelStore.save(ctx, mapOf("pushup-standard" to 2, "pushup-feet-elevated" to 1))
        show()
        chooseRouteContaining("pushup-standard")
        revealTreeNode("pushup-standard")
        assertTrue(desc("tree_node_pushup-standard"), desc("tree_node_pushup-standard").contains("5 of 5 stars"))
        revealTreeNode("pushup-incline")
        assertTrue(desc("tree_node_pushup-incline").contains("5 of 5 stars"))
    }

    @Test fun treeNodesAreVerticalSoLowerExercisesAreBelowEasierOnes() {
        show()
        chooseRouteContaining("pushup-standard")
        val y = { t: String -> rule.onNodeWithTag(t).fetchSemanticsNode().boundsInRoot.top }
        assertTrue("${y("tree_node_pushup-incline")} < ${y("tree_node_pushup-standard")}", y("tree_node_pushup-incline") < y("tree_node_pushup-standard"))
        assertTrue("${y("tree_node_pushup-standard")} < ${y("tree_node_pushup-feet-elevated")}", y("tree_node_pushup-standard") < y("tree_node_pushup-feet-elevated"))
    }

    @Test fun theWidePushTreeScrollsToR8NodesAtItsRightEdge() {
        val catalog = io.github.gonbei774.calisthenicsmemory.ui.train.loadCatalog(ctx)
        val profile = ProfileStore.selected(ctx)
        val candidate = catalog.progressionRoutes.firstNotNullOfOrNull { route ->
            route.members.firstOrNull { member -> member.role == app.calisthenics.domain.model.ProgressionRole.ALTERNATE && member.atVariationId != null && member.atVariationId in route.mainPath &&
                catalog.variation(member.variationId)?.let { app.calisthenics.domain.equipment.isAvailable(it, profile) } == true }?.let { route to it }
        } ?: return
        show(); chooseRouteContaining(candidate.second.atVariationId!!)
        revealTreeNode(candidate.second.atVariationId!!)
        rule.onNodeWithTag("tree_node_${candidate.second.atVariationId}").performSemanticsAction(SemanticsActions.OnClick); rule.waitForIdle()
        rule.onNodeWithTag("route_extras_toggle").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("route_member_${candidate.second.variationId}").assertExists()
    }

    @Test fun deepTreeRowsAreComposedOnDemandAndRemainReachable() {
        show()
        chooseRouteContaining("pushup-standard")
        rule.onNodeWithTag("tree_node_pushup-one-arm").assertDoesNotExist()
        revealTreeNode("pushup-one-arm")
        val deepNode = rule.onNodeWithTag("tree_node_pushup-one-arm")
        deepNode.performScrollTo().assertIsDisplayed()
        deepNode.performSemanticsAction(SemanticsActions.OnClick); rule.waitForIdle()
        rule.onNodeWithTag("panel_title").assertTextContains("One-Arm Push-Up", substring = true)
    }

    @Test fun stretchesHaveTheirOwnTab() {
        show()
        rule.onNodeWithTag("tab_chip_stretches").performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("stretch_stretch-hamstring").assertExists()
    }
}
