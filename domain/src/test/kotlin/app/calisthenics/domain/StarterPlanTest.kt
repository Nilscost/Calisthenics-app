package app.calisthenics.domain

import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.equipment.SeedProfiles
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
    @Test fun threeRoundsTakeFortyToFortyFiveMinutes() {
        val m = plan(3).plan.plannedDurationSeconds / 60.0
        assertTrue("3 rounds = $m min", m in 40.0..45.0)
    }
    @Test fun printThreeRoundLength() { println("THREE_ROUNDS_SECONDS=" + plan(3).plan.plannedDurationSeconds) }
}
