package app.calisthenics.domain

import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.equipment.SeedProfiles
import app.calisthenics.domain.equipment.isAvailable
import app.calisthenics.domain.model.*
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.routine.SessionDraft
import app.calisthenics.domain.routine.StarterRoutine
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class StarterPlanTest {
    private val catalog = parseCatalog(File(System.getProperty("repo.root"), "content/starter/catalog.json").readText())
    private fun plan(minutes: Int, profile: EquipmentProfile) = generate(PlanInput(
        "p", 0L, 0, catalog, StarterRoutine.routine,
        SessionDraft.from(Preferences(defaultDurationSeconds = minutes * 60), StarterRoutine.routine), profile))

    @Test fun everyRoutineSlotPointsAtARealStrengthVariation() {
        for (s in StarterRoutine.routine.slots) {
            val v = catalog.variations.firstOrNull { it.id == s.preferredVariationId }
            assertNotNull("slot ${s.id} -> ${s.preferredVariationId}", v)
            assertTrue(s.intent in v!!.patterns)
            assertTrue(s.area in v.areas)
        }
    }

    @Test fun homeProfileGivesPlansOfEveryRequestedLength() {
        for (m in listOf(10, 20, 30, 45, 60, 90)) {
            val r = plan(m, SeedProfiles.home)
            assertTrue("$m min -> $r", r is PlanResult.Ready)
            val p = (r as PlanResult.Ready).plan
            assertTrue("$m min planned=${p.plannedDurationSeconds}", Math.abs(p.plannedDurationSeconds - m * 60) <= 60 || p.needsAcceptance)
            assertEquals(p.blocks.sumOf { it.durationSeconds }, p.plannedDurationSeconds)
            assertTrue(p.usesDraftContent) // honest: catalog is DRAFT
        }
    }

    @Test fun travelProfileEitherPlansWithoutHomeGearOrExplainsWhy() {
        val r = plan(30, SeedProfiles.travel)
        if (r is PlanResult.Ready) {
            val used = r.plan.blocks.mapNotNull { it.variationId }.toSet()
            val home = setOf("pullup-band-assisted", "row-band", "kettlebell-deadlift")
            assertTrue("travel plan used home gear: ${used intersect home}", (used intersect home).isEmpty())
        } else {
            r as PlanResult.Infeasible
            assertTrue(r.reasons.isNotEmpty())
        }
    }
}

class ExplicitRoundsTest {
    private val catalog = parseCatalog(File(System.getProperty("repo.root"), "content/starter/catalog.json").readText())
    private fun plan(rounds: Int?) = generate(PlanInput("p", 0L, 0, catalog, StarterRoutine.routine,
        SessionDraft.from(Preferences(), StarterRoutine.routine).copy(rounds = rounds), SeedProfiles.home)) as PlanResult.Ready

    @Test fun explicitRoundsAreHonouredAndLengthFollows() {
        val lens = (1..5).map { plan(it).plan }
        for ((i, p) in lens.withIndex()) assertEquals(i + 1, p.rounds)
        assertTrue(lens.zipWithNext().all { (a, b) -> b.plannedDurationSeconds > a.plannedDurationSeconds })
        // keeps every slot (no optional slot silently dropped)
        assertEquals(6, lens[2].blocks.filter { it.type == BlockType.WORK && it.roundIndex == 1 }.map { it.slotId }.distinct().size)
    }
    @Test fun nullRoundsKeepsDurationBehaviour() { assertTrue(plan(null).plan.rounds in 1..MAX_ROUNDS) }
    @Test fun fourRoundsOfTheStarterCircuitTakeAboutFortyFiveMinutes() {
        // Owner 2026-10-05: ~1 min work + 1 min rest per exercise; 4 rounds of 5-6 exercises = ~45 min.
        val m = plan(4).plan.plannedDurationSeconds / 60.0
        assertTrue("4 rounds = $m min", m in 40.0..50.0)
    }
    @Test fun timedRoundsAreSixtyWorkSixtyRest() {
        val r = generate(PlanInput("p", 0L, 0, catalog, StarterRoutine.routine,
            SessionDraft.from(Preferences(), StarterRoutine.routine).copy(rounds = 4, timed = true), SeedProfiles.home)) as PlanResult.Ready
        assertTrue(r.plan.timed)
        val work = r.plan.blocks.filter { it.type == BlockType.WORK }
        // U03: rep work fills 60 s; a hold lasts target + 3 s (never padded) and its unused time goes to the recovery after it.
        assertTrue(work.filter { it.target?.type == TargetType.REPS }.all { it.durationSeconds == TIMED_WORK_SECONDS || (it.side != Side.NONE && it.durationSeconds == TIMED_WORK_SECONDS / 2) })
        assertTrue(work.filter { it.target?.type == TargetType.HOLD_SECONDS }.all { it.durationSeconds == it.target!!.value + 3 })
        assertTrue(r.plan.blocks.filter { it.type == BlockType.PASSIVE_RECOVERY || it.type == BlockType.STRETCH }.all { it.durationSeconds >= (TIMED_REST_SECONDS + 1) / 2 })
        val m = r.plan.plannedDurationSeconds / 60.0
        val by = r.plan.blocks.groupBy { it.type }.mapValues { (_, b) -> b.size to b.sumOf { it.durationSeconds } }
        println("TIMED4 total=${r.plan.plannedDurationSeconds} by=$by slots=" + r.plan.blocks.filter { it.type == BlockType.WORK && it.roundIndex == 1 }.map { it.variationId + "/" + it.side })
        assertTrue("4 timed rounds = $m min", m in 40.0..50.0)
    }
    @Test fun muscleUpAndLeversNeedAHighBar() {
        val ids = listOf("muscle-up-bar", "front-lever-tuck", "front-lever-adv-tuck", "front-lever-straddle")
        for (id in ids) {
            val v = catalog.variation(id)!!
            assertTrue(id, !isAvailable(v, SeedProfiles.home))
            val withBar = SeedProfiles.home.copy(items = SeedProfiles.home.items + EquipmentItem("high-bar"))
            assertTrue(id, isAvailable(v, withBar))
        }
        assertTrue(isAvailable(catalog.variation("pullup-full")!!, SeedProfiles.home))
    }
    @Test fun printThreeRoundLength() { println("ROUNDS3=" + plan(3).plan.plannedDurationSeconds + " ROUNDS4=" + plan(4).plan.plannedDurationSeconds + " SLOTS=" + StarterRoutine.routine.slots.size) }
}

class RepsFeedbackTest {
    @Test fun repsBelowTargetCountAsBelowAndAtTargetAsMet() {
        val blk = TimelineBlock("b", BlockType.WORK, 60, 1, "s", "pushup-standard", Side.NONE, Target(TargetType.REPS, 8), 3)
        val plan = WorkoutPlan("p", "r", 1, 1, 0L, "home", 0, 60, emptySet(), false, null, 1, emptyList(), emptyList(), false, false, listOf(blk))
        fun rate(n: Int) = app.calisthenics.domain.feedback.resolveFeedback(plan, mapOf("b" to app.calisthenics.domain.feedback.Execution.COMPLETED), emptyMap(),
            mapOf("pushup-standard" to app.calisthenics.domain.feedback.Feedback(rating = app.calisthenics.domain.feedback.Rating.MET, actualReps = n))).single().rating
        assertEquals(app.calisthenics.domain.feedback.Rating.BELOW, rate(7))
        assertEquals(app.calisthenics.domain.feedback.Rating.MET, rate(8))
    }
}

class HoldFeedbackTest {
    @Test fun holdBelowTargetCountsAsBelow() {
        val blk = TimelineBlock("b", BlockType.WORK, 60, 1, "s", "plank", Side.NONE, Target(TargetType.HOLD_SECONDS, 30), 3)
        val plan = WorkoutPlan("p", "r", 1, 1, 0L, "home", 0, 60, emptySet(), false, null, 1, emptyList(), emptyList(), false, false, listOf(blk))
        fun rate(n: Int) = app.calisthenics.domain.feedback.resolveFeedback(plan, mapOf("b" to app.calisthenics.domain.feedback.Execution.COMPLETED), emptyMap(),
            mapOf("plank" to app.calisthenics.domain.feedback.Feedback(rating = app.calisthenics.domain.feedback.Rating.MET, actualHoldSeconds = n))).single().rating
        assertEquals(app.calisthenics.domain.feedback.Rating.BELOW, rate(25))
        assertEquals(app.calisthenics.domain.feedback.Rating.MET, rate(30))
    }
}
