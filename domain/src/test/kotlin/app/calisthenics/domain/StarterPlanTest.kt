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
