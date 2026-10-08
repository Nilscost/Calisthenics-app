package app.calisthenics.domain

import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.equipment.SeedProfiles
import app.calisthenics.domain.feedback.*
import app.calisthenics.domain.load.formatKg
import app.calisthenics.domain.load.isLoaded
import app.calisthenics.domain.load.loadGrams
import app.calisthenics.domain.model.*
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.progression.*
import app.calisthenics.domain.routine.StarterRoutine
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** Owner 2026-10-08: kettlebell progression with the same weight (harder exercise) or more weight (same exercise). */
class KettlebellLoadTest {
    private val catalog = parseCatalog(File(System.getProperty("repo.root"), "content/starter/catalog.json").readText())
    private val engine = ProgressionEngine(catalog)
    private val DL = "kettlebell-deadlift"

    private fun ev(day: Int, tier: Int, grams: Int?, vid: String = DL) =
        SessionEvidence("s$day$vid", vid, "deadlift", day, tier, 2, false, false, false, null, false, 2, 0, 2, grams)

    private fun profile(grams: Int) = SeedProfiles.home.copy(
        items = SeedProfiles.home.items.map { if (it.equipmentId == "kettlebell") it.copy(massGrams = grams) else it })

    private fun progressAt(tier: Int, grams: Int?) = ProgressSnapshot(
        mapOf(DL to VariationProgress(DL, "deadlift", tier, 0, tier, emptySet(), null, emptyList(), 0, 0, false, false, null, null, grams)),
        mapOf("deadlift" to DL), emptyMap(), emptyList())

    private fun dlBlocks(progress: ProgressSnapshot, grams: Int): List<TimelineBlock> {
        val r = buildTrainPlan(catalog, StarterRoutine.routine, progress, profile(grams), TrainSettings(), mapOf("hinge" to DL))
        return (r as PlanResult.Ready).plan.blocks.filter { it.type == BlockType.WORK && it.variationId == DL }
    }

    @Test fun formatsKilograms() {
        assertEquals("12", formatKg(12_000)); assertEquals("12.5", formatKg(12_500)); assertEquals("8", formatKg(8_000))
    }

    @Test fun onlyKettlebellExercisesAreLoaded() {
        assertTrue(catalog.variation(DL)!!.isLoaded())
        assertTrue(catalog.variation("kettlebell-swing-one-arm")!!.isLoaded())
        assertFalse(catalog.variation("pushup-standard")!!.isLoaded())
        assertEquals(12_000, SeedProfiles.home.loadGrams()); assertNull(SeedProfiles.travel.loadGrams())
    }

    @Test fun planCarriesTheBellWeightOnKettlebellWorkOnly() {
        val p = (buildTrainPlan(catalog, StarterRoutine.routine, progressAt(3, 12_000), profile(12_000), TrainSettings(), mapOf("hinge" to DL)) as PlanResult.Ready).plan
        val work = p.blocks.filter { it.type == BlockType.WORK }
        assertTrue(work.filter { it.variationId == DL }.all { it.loadGrams == 12_000 && it.prescriptionTier == 3 })
        assertTrue(work.filter { it.variationId != DL }.all { it.loadGrams == null })
    }

    @Test fun aHeavierBellStartsTheSameExerciseAgainAtTierOne() {
        val blocks = dlBlocks(progressAt(4, 12_000), 16_000)
        assertTrue(blocks.all { it.prescriptionTier == 1 && it.loadGrams == 16_000 })
        val p = (buildTrainPlan(catalog, StarterRoutine.routine, progressAt(4, 12_000), profile(16_000), TrainSettings(), mapOf("hinge" to DL)) as PlanResult.Ready).plan
        assertTrue(p.changesExplained.any { "heavier kettlebell (16 kg instead of 12 kg)" in it })
    }

    @Test fun aLighterBellKeepsTheTierButWarnsItWillNotCount() {
        assertTrue(dlBlocks(progressAt(4, 16_000), 12_000).all { it.prescriptionTier == 4 && it.loadGrams == 12_000 })
        val p = (buildTrainPlan(catalog, StarterRoutine.routine, progressAt(4, 16_000), profile(12_000), TrainSettings(), mapOf("hinge" to DL)) as PlanResult.Ready).plan
        assertTrue(p.warnings.any { "lighter than the 16 kg" in it })
    }

    @Test fun noRecordedWeightYetKeepsTheTier() {
        assertTrue(dlBlocks(progressAt(3, null), 16_000).all { it.prescriptionTier == 3 })
    }

    @Test fun evidenceRecordsTheWeightFromThePlan() {
        val p = (buildTrainPlan(catalog, StarterRoutine.routine, progressAt(2, 12_000), profile(12_000), TrainSettings(), mapOf("hinge" to DL)) as PlanResult.Ready).plan
        val outcomes = p.blocks.filter { it.type == BlockType.WORK }.map {
            ResolvedOutcome(it.id, it.variationId!!, Execution.COMPLETED, Rating.MET, RatingOrigin.ASSUMED, null, null, false, null)
        }
        val evs = deriveEvidence("s1", 5, p, outcomes, { catalog.variation(it)!!.familyId }, { _, _ -> 2 })
        assertEquals(12_000, evs.single { it.variationId == DL }.loadGrams)
        assertNull(evs.first { it.variationId != DL }.loadGrams)
    }

    @Test fun engineKeepsStarsPerWeightAndRestartsAtTierOneWithAHeavierBell() {
        val start = UserAction.SelfAssessment(0, DL, 2)
        val at12 = listOf(ev(0, 2, 12_000), ev(3, 2, 12_000), ev(7, 2, 12_000)) // tier 2 earned at 12 kg -> tier 3
        val s1 = engine.replay(at12, listOf(start)).variations.getValue(DL)
        assertEquals(3, s1.tier); assertEquals(12_000, s1.loadGrams)

        val at16 = at12 + listOf(ev(10, 1, 16_000), ev(13, 1, 16_000), ev(17, 1, 16_000))
        val snap = engine.replay(at16, listOf(start))
        val s2 = snap.variations.getValue(DL)
        assertEquals(16_000, s2.loadGrams)
        assertEquals(mapOf(12_000 to s1.earnedStars()), s2.starsByLoad)
        assertEquals("tier 1 earned at 16 kg -> tier 2", 2, s2.tier)
        assertTrue(snap.events.any { "Heavier kettlebell (16 kg)" in it.message && "at 12 kg are kept" in it.message })
    }

    @Test fun aLighterBellSessionIsLoggedButDoesNotCount() {
        val start = UserAction.SelfAssessment(0, DL, 2)
        val evs = listOf(ev(0, 2, 16_000), ev(3, 2, 8_000), ev(7, 2, 8_000))
        val snap = engine.replay(evs, listOf(start))
        val p = snap.variations.getValue(DL)
        assertEquals(2, p.tier); assertEquals(16_000, p.loadGrams); assertEquals(listOf(0), p.streakDays)
        assertEquals(7, p.lastTrainedDay)
        assertTrue(snap.events.any { "lighter kettlebell (8 kg instead of 16 kg)" in it.message })
    }

    @Test fun bodyweightEvidenceIsUnaffected() {
        val v = "pushup-knee"
        val evs = listOf(0, 3, 7).map { SessionEvidence("s$it", v, "pushup", it, 2, 2, false, false, false, null, false, 2, 0, 2) }
        val p = engine.replay(evs, listOf(UserAction.SelfAssessment(0, v, 2))).variations.getValue(v)
        assertEquals(3, p.tier); assertNull(p.loadGrams); assertTrue(p.starsByLoad.isEmpty())
    }

    @Test fun oneArmSwingIsTheSameBellProgressionAfterTheSwing() {
        val oas = catalog.variation("kettlebell-swing-one-arm")!!
        assertTrue(oas.unilateral)
        assertEquals(listOf("kettlebell-swing-one-arm"), catalog.policyForVariation("kettlebell-swing")!!.nextVariationIds)
        assertEquals(VariationTier("kettlebell-swing", 4), catalog.policyForVariation(oas.id)!!.prerequisiteRule.allOf.single().single().variationTierMet)
        // every kettlebell exercise explains how to pick the weight and that levels count per weight
        for (v in catalog.variations.filter { it.isLoaded() }) assertTrue(v.id, v.instructions.any { it.startsWith("Weight:") && "per weight" in it })
    }
}
