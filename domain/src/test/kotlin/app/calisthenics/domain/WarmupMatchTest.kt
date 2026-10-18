package app.calisthenics.domain

import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.equipment.SeedProfiles
import app.calisthenics.domain.model.*
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.progression.ProgressSnapshot
import app.calisthenics.domain.routine.StarterRoutine
import app.calisthenics.domain.tree.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** R8.2-T02 (G2): prehab and warm-up are one Warm-up category; the automatic warm-up picks 2 to 4 items matched to the session within its time. */
class WarmupMatchTest {
    private val c = parseCatalog(File(System.getProperty("repo.root"), "content/starter/catalog.json").readText())
    private val none = ProgressSnapshot(emptyMap(), emptyMap(), emptyMap(), emptyList())
    private fun warmIds(p: WorkoutPlan) = p.blocks.filter { it.type == BlockType.WARMUP }.mapNotNull { it.variationId }.distinct()
    private fun plan(goal: String, warm: Boolean = true) = (buildTrainPlan(c, StarterRoutine.routine, none, SeedProfiles.home, TrainSettings(goalId = goal, rounds = 2, warmupOn = warm)) as PlanResult.Ready).plan
    private fun baseSeconds(p: WorkoutPlan) = (buildTrainPlan(c, StarterRoutine.routine, none, SeedProfiles.home, TrainSettings(goalId = "general", rounds = 2, warmupOn = true)) as PlanResult.Ready).plan.blocks.filter { it.type == BlockType.WARMUP }.sumOf { it.durationSeconds }

    @Test fun prehabAndWarmupItemsAreOneWarmupCategoryByBodyArea() {
        assertNull(TreeTabs.byId("prehab")); val tab = TreeTabs.byId("warmup")!!
        assertEquals("Warm-up", tab.title)
        for (id in listOf("external-rotation-band", "wrist-palm-pulses", "hip-lateral-leg-raise", "spanish-squat", "chin-nod-supine", "warmup-shoulder-band")) assertEquals(id, "warmup", TreeTabs.tabForVariation(c, id)?.id)
        assertEquals(WarmupArea.SHOULDER, warmupAreaOf(c.variation("prone-y-raise")!!.familyId)); assertEquals(WarmupArea.GENERAL, warmupAreaOf("warmup-dead-bug"))
        assertEquals(WarmupArea.entries.flatMap { it.familyIds }.toSet().size, WarmupArea.entries.flatMap { it.familyIds }.size)
    }

    private fun matched(vararg ids: String, already: List<String> = emptyList()) = Ramp.matched(c, SeedProfiles.home, none, ids.map { c.variation(it)!! }, already)

    @Test fun pushWorkGetsWristPrepAndDipsGetShoulderItems() {
        assertEquals(listOf("wrist-palm-pulses"), matched("pushup-standard"))
        assertEquals(emptyList<String>(), matched("pushup-standard", already = listOf("warmup-wrist-prep")))   // the template already prepares the wrists
        val dips = matched("dip-parallel")
        assertTrue(dips.toString(), "wrist-palm-pulses" in dips && dips.any { it in listOf("external-rotation-band", "prone-y-raise", "prone-t-raise") })
        assertEquals(1, dips.count { it in listOf("external-rotation-band", "prone-y-raise", "prone-t-raise") })    // one shoulder item, not all three
        assertTrue(matched("pike-pushup").any { it == "external-rotation-band" })
        assertEquals("wrist-pushup-knees", matched("planche-lean").first())   // handstand and planche work get the loaded wrist item instead of the pulses
    }

    @Test fun pistolWorkGetsAKneeItemAndSingleLegWorkAHipItem() {
        assertEquals(listOf("spanish-squat", "clamshell"), matched("squat-pistol"))   // knee, then hip (single-leg)
        assertEquals(listOf("clamshell"), matched("split-squat"))
        assertEquals(listOf("spanish-squat"), matched("sissy-squat"))
        assertEquals(emptyList<String>(), matched("squat-air", "plank"))
        // an upper-body, a leg and a single-leg session together: at most four items, each need once
        val all = matched("planche-lean", "dip-parallel", "squat-pistol")
        assertTrue(all.toString(), all.size in 2..4 && all.distinct().size == all.size)
    }

    @Test fun aSessionOfThePersonsLevelGetsMatchedItemsInThePlanWithinTheTemplateTime() {
        val progress = app.calisthenics.domain.progression.ProgressionEngine(c).replay(emptyList(), listOf(
            app.calisthenics.domain.progression.UserAction.SelfAssessment(0, "pushup-standard", 6), app.calisthenics.domain.progression.UserAction.SelfAssessment(0, "dip-support-hold", 5),
            app.calisthenics.domain.progression.UserAction.SelfAssessment(0, "dip-negative", 5), app.calisthenics.domain.progression.UserAction.SelfAssessment(0, "dip-parallel", 3)))
        val p = (buildTrainPlan(c, StarterRoutine.routine, progress, SeedProfiles.home, TrainSettings(goalId = "hspu", rounds = 2, warmupOn = true)) as PlanResult.Ready).plan
        val warm = p.blocks.filter { it.type == BlockType.WARMUP }
        assertTrue(warm.sumOf { it.durationSeconds } <= baseSeconds(p))
        assertTrue(warm.isNotEmpty())
    }

    @Test fun theMatchedWarmupHasAtMostFourExtraItemsStaysInTheTemplatesTimeAndIsStable() {
        for (goal in listOf("general", "hspu", "pistol", "planche", "muscle-up", "body-lower")) {
            val p = plan(goal)
            val base = baseSeconds(p)
            val matched = p.blocks.filter { it.type == BlockType.WARMUP && it.id.startsWith("warmup-m") }.mapNotNull { it.variationId }.distinct()
            assertTrue("$goal matched $matched", matched.size <= 4)
            assertTrue("$goal ${p.blocks.filter { it.type == BlockType.WARMUP }.sumOf { it.durationSeconds }} > $base", p.blocks.filter { it.type == BlockType.WARMUP }.sumOf { it.durationSeconds } <= maxOf(base, 1))
            assertEquals(p.blocks, plan(goal).blocks)
        }
    }

    @Test fun warmupOffAddsNothingAndAnAuthoredRoutineWarmupIsLeftAsWritten() {
        assertTrue(plan("hspu", warm = false).blocks.none { it.type == BlockType.WARMUP })
        val authored = StarterRoutine.routine.copy(warmup = listOf("warmup-dead-bug"))
        val p = (buildTrainPlan(c, authored, none, SeedProfiles.home, TrainSettings(goalId = "hspu", rounds = 2, warmupOn = true)) as PlanResult.Ready).plan
        assertEquals(listOf("warmup-dead-bug"), warmIds(p))
    }
}
