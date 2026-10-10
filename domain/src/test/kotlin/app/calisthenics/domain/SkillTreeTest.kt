package app.calisthenics.domain

import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.equipment.SeedProfiles
import app.calisthenics.domain.goals.Goals
import app.calisthenics.domain.model.Kind
import app.calisthenics.domain.progression.*
import app.calisthenics.domain.tree.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class SkillTreeTest {
    private val catalog = parseCatalog(File(System.getProperty("repo.root"), "content/starter/catalog.json").readText())
    private val engine = ProgressionEngine(catalog)
    private fun snap(vararg levels: Pair<String, Int>) = engine.replay(emptyList(), levels.map { (v, t) -> UserAction.SelfAssessment(0, v, t) })
    private val empty = snap()

    @Test fun everyStrengthExerciseIsInExactlyOneTab() {
        val strength = catalog.variations.filter { it.kind == Kind.REPS || it.kind == Kind.HOLD }
        for (v in strength) assertEquals("${v.id} (${v.familyId})", 1, TreeTabs.all.count { v.familyId in it.familyIds })
        val shown = TreeTabs.all.flatMap { layoutTree(catalog, it.familyIds).nodes }.map { it.variationId }
        assertEquals(strength.map { it.id }.sorted(), shown.sorted())
    }

    @Test fun noTwoNodesShareACellAndEdgesRunLeftToRight() {
        for (tab in TreeTabs.all) {
            val l = layoutTree(catalog, tab.familyIds)
            assertEquals("${tab.id} overlap", l.nodes.size, l.nodes.map { it.col to it.row }.toSet().size)
            assertTrue(l.nodes.all { it.col < l.cols && it.row < l.rows })
            for (e in l.edges) assertTrue("${tab.id}: ${e.from} -> ${e.to}", l.node(e.from)!!.col < l.node(e.to)!!.col)
        }
    }

    @Test fun layoutIsDeterministic() {
        for (tab in TreeTabs.all) assertEquals(layoutTree(catalog, tab.familyIds), layoutTree(catalog, tab.familyIds.reversed()))
    }

    @Test fun pushChainReadsLeftToRightAndMergesIntoTheStandardPushup() {
        val l = layoutTree(catalog, listOf("pushup"))
        val standard = l.node("pushup-standard")!!
        assertEquals(setOf("pushup-incline", "pushup-knee"), l.edges.filter { it.to == "pushup-standard" }.map { it.from }.toSet())
        assertTrue(l.node("pushup-incline")!!.col < standard.col && standard.col < l.node("pushup-feet-elevated")!!.col)
        assertNotEquals(l.node("pushup-incline")!!.row, l.node("pushup-knee")!!.row)
    }

    @Test fun hspuNeedsTwoThingsAndBothDrawIntoIt() {
        val l = layoutTree(catalog, listOf("hspu"))
        assertTrue(l.edges.count { it.to == "hspu-wall" } >= 1)
        assertEquals(setOf("pike-pushup", "pike-pushup-elevated", "wall-handstand-hold", "hspu-wall-negative", "hspu-wall"), l.nodes.map { it.variationId }.toSet())
    }

    @Test fun independentChainsAreStackedNotOverlapped() {
        val l = layoutTree(catalog, TreeTabs.byId("core")!!.familyIds)
        assertTrue(l.rows >= 3)
        // dead bug now leads into the hollow-hold chain
        assertTrue(l.edges.any { it.from == "dead-bug" && it.to == "hollow-hold" })
    }

    @Test fun nodeStatesFollowProgressPrerequisitesAndEquipment() {
        val home = SeedProfiles.home
        val p = snap("pushup-standard" to 3)
        assertEquals(TreeNodeState.AVAILABLE, nodeState(catalog, "pushup-incline", p, home, emptySet()))
        assertEquals(TreeNodeState.CURRENT, nodeState(catalog, "pushup-standard", p, home, trainingNow(catalog, p, null)))
        assertEquals(TreeNodeState.LOCKED, nodeState(catalog, "hspu-wall", p, home, emptySet()))
        assertEquals(TreeNodeState.NEEDS_EQUIPMENT, nodeState(catalog, "muscle-up-bar", snapAll(), home, emptySet())) // prerequisites met, no high bar
        assertEquals(TreeNodeState.NEEDS_EQUIPMENT, nodeState(catalog, "pushup-feet-elevated", snap("pushup-feet-elevated" to 3), SeedProfiles.travel.copy(items = emptyList()), emptySet()))
    }

    @Test fun aStepInsertedBelowYourLevelNeverLocksYouOut() {   // L02: levels are kept by exercise id; the new wall / high incline steps count as passed
        val home = SeedProfiles.home
        val p = snap("pushup-standard" to 3, "pushup-incline" to 4)
        assertEquals(TreeNodeState.AVAILABLE, nodeState(catalog, "pushup-knee", p, home, emptySet()))      // a sibling step with a prerequisite below the person's level
        assertEquals(0, Goals.unmet(catalog, "pushup-knee", p).size)
        assertEquals(3, p.variations.getValue("pushup-standard").tier); assertEquals(4, p.variations.getValue("pushup-incline").tier)   // nothing moved, no star lost
        // a beginner who has not started anything still sees the first steps in order
        assertEquals(TreeNodeState.LOCKED, nodeState(catalog, "pushup-knee", empty, home, emptySet()))
        assertEquals(TreeNodeState.AVAILABLE, nodeState(catalog, "pushup-wall", empty, SeedProfiles.home, emptySet()))
    }

    private fun snapAll() = engine.replay(emptyList(), catalog.variations.filter { it.kind == Kind.REPS || it.kind == Kind.HOLD }.map { UserAction.SelfAssessment(0, it.id, 5) })

    @Test fun masteredNeedsAllFiveStepsAndStarsComeFromEarnedStars() {
        val none = empty
        assertEquals(0, stars(none, "pushup-standard"))
        val s = snap("pushup-standard" to 1)
        assertEquals(0, stars(s, "pushup-standard")) // a starting level is not a star
        assertEquals(TreeNodeState.AVAILABLE, nodeState(catalog, "pushup-standard", s, SeedProfiles.home, emptySet()))
        // earnedStars() is the single source: a snapshot with achieved tiers 1..3 from baseline 1 shows 3 stars
        val vp = s.variations.getValue("pushup-standard").copy(achievedTiers = setOf(1, 2, 3))
        assertEquals(3, vp.earnedStars())
        assertEquals(5, vp.copy(achievedTiers = setOf(1, 2, 3, 4, 5)).earnedStars())
        val master = s.copy(variations = s.variations + ("pushup-standard" to vp.copy(achievedTiers = setOf(1, 2, 3, 4, 5))))
        assertEquals(TreeNodeState.MASTERED, nodeState(catalog, "pushup-standard", master, SeedProfiles.home, emptySet()))
        assertEquals(5, stars(master, "pushup-standard"))
    }

    @Test fun tabsFindTheGoalFamily() {
        assertEquals("shoulders", TreeTabs.tabForVariation(catalog, "pike-pushup")?.id)
        assertEquals("core", TreeTabs.tabForVariation(catalog, "hollow-hold")?.id)
        assertEquals("pull", TreeTabs.tabForVariation(catalog, "front-lever-tuck")?.id)
    }

    // ---- V13: vertical layout, skills view, stars below the current level
    @Test fun verticalLayoutHasNoOverlapsAndEveryEdgeRunsDownwards() {
        for (tab in TreeTabs.all) {
            val l = layoutTreeVertical(catalog, tab.familyIds)
            assertEquals("${tab.id} overlap", l.nodes.size, l.nodes.map { it.col to it.row }.toSet().size)
            assertTrue(l.nodes.all { it.col < l.cols && it.row < l.rows })
            for (e in l.edges) assertTrue("${tab.id}: ${e.from} -> ${e.to}", l.node(e.from)!!.row < l.node(e.to)!!.row)
            assertTrue("${tab.id} is wider than a phone can show (${l.cols} columns)", l.cols <= 3)
        }
    }

    @Test fun verticalLayoutIsDeterministicAndShowsEveryExercise() {
        for (tab in TreeTabs.all) assertEquals(layoutTreeVertical(catalog, tab.familyIds), layoutTreeVertical(catalog, tab.familyIds.reversed()))
        val strength = catalog.variations.filter { it.kind == Kind.REPS || it.kind == Kind.HOLD }.map { it.id }
        assertEquals(strength.sorted(), TreeTabs.all.flatMap { layoutTreeVertical(catalog, it.familyIds).nodes }.map { it.variationId }.sorted())
    }

    @Test fun pushUpsBranchSideBySideAfterTheStandardPushup() {
        val l = layoutTreeVertical(catalog, listOf("pushup", "planche"))
        val std = l.node("pushup-standard")!!
        assertTrue(l.node("pushup-incline")!!.row < std.row && l.node("pushup-knee")!!.row < std.row)
        assertEquals(l.node("pushup-feet-elevated")!!.row, l.node("planche-lean")!!.row) // the two paths out of the standard push-up sit on the same row
        assertNotEquals(l.node("pushup-feet-elevated")!!.col, l.node("planche-lean")!!.col)
    }

    @Test fun aSkillIsOneVerticalChainFromTheEntryToTheTarget() {
        val g = Goals.byId("muscle-up")!!
        val l = skillLayout(catalog, g)
        assertEquals(Goals.chain(catalog, g), l.nodes.sortedBy { it.row }.map { it.variationId })
        assertTrue(l.nodes.all { it.col == 0 }); assertEquals("muscle-up-bar", l.nodes.last().variationId)
        assertEquals(l.nodes.size - 1, l.edges.size)
    }

    @Test fun exercisesYouHaveMovedPastShowFiveStars() {
        val s = snap("pushup-standard" to 2, "pushup-feet-elevated" to 1)
        assertEquals(5, displayStars(catalog, s, "pushup-standard"))     // a harder exercise after it has been started
        assertEquals(5, displayStars(catalog, s, "pushup-knee"))          // and everything below it in the chain
        assertEquals(0, displayStars(catalog, s, "pushup-feet-elevated")) // the current one shows its own stars
        assertEquals(0, displayStars(catalog, s, "pushup-one-arm"))
        assertEquals(1, displayStars(catalog, snap("pushup-standard" to 2), "pushup-standard"))
    }
}
