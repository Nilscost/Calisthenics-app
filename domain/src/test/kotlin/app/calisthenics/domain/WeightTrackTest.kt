package app.calisthenics.domain

import app.calisthenics.domain.equipment.SeedProfiles
import app.calisthenics.domain.load.*
import app.calisthenics.domain.model.*
import app.calisthenics.domain.planner.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** V26 (R27, O3, O5): weight tracks for the kettlebell and dumbbells. */
class WeightTrackTest {
    private val kg = 1000

    @Test fun kettlebellTrackRunsFromTheCurrentBellUpwardAndLocksHeavierStepsWithTheProfile() {
        val t = weightTrack("kettlebell", profileGrams = 12 * kg, recorded = 12 * kg, currentStars = 5, starsByLoad = emptyMap())
        assertEquals(listOf(12 * kg, 16 * kg, 20 * kg, 24 * kg), t.map { it.grams })
        assertEquals(listOf(WeightState.NOW, WeightState.LOCKED, WeightState.LOCKED, WeightState.LOCKED), t.map { it.state })
        assertEquals(5, t.first().stars)
    }

    @Test fun lighterWeightsTheOwnerTrainedAtStayWithTheirStars() {
        // trained 12 kg to 3 stars, now owns a 16 kg bell and has not trained at it: 12 DONE (kept), 16 READY (starts again at level 1)
        val t = weightTrack("kettlebell", profileGrams = 16 * kg, recorded = 12 * kg, currentStars = 3, starsByLoad = emptyMap())
        assertEquals(listOf(12 * kg to WeightState.NOW, 16 * kg to WeightState.READY, 20 * kg to WeightState.LOCKED, 24 * kg to WeightState.LOCKED), t.map { it.grams to it.state })
        // once the level is recorded at 16 kg: the 12 kg stars are kept and shown
        val t2 = weightTrack("kettlebell", 16 * kg, 16 * kg, 1, mapOf(12 * kg to 3))
        assertEquals(listOf(12 * kg to WeightState.DONE, 16 * kg to WeightState.NOW, 20 * kg to WeightState.LOCKED, 24 * kg to WeightState.LOCKED), t2.map { it.grams to it.state })
        assertEquals(3, t2.first().stars); assertEquals(1, t2[1].stars)
    }

    @Test fun anOddWeightIsShownAndNothingBeforeTheBellInUse() {
        val t = weightTrack("kettlebell", 14 * kg, null, 0, emptyMap())
        assertEquals(listOf(14 * kg, 16 * kg, 20 * kg, 24 * kg), t.map { it.grams }); assertEquals(WeightState.NOW, t.first().state)   // not trained yet: the profile's bell is where it starts
        assertTrue(weightTrack("kettlebell", null, null, 0, emptyMap()).isEmpty())
    }

    @Test fun dumbbellsUseTheirOwnLadderAndTheirOwnWeightInTheProfile() {
        val t = weightTrack(DUMBBELL_EQUIPMENT_ID, 2_500, 2_500, 2, emptyMap())
        assertEquals(listOf(2_500, 5_000, 7_500, 10_000, 15_000, 20_000), t.map { it.grams })
        val db = ExerciseVariation(id = "db-test", familyId = "db", name = "Dumbbell test", patterns = setOf(Pattern.HINGE), areas = setOf(Area.LOWER_BODY), kind = Kind.REPS,
            equipmentAlternatives = listOf(RequirementSet(needs = listOf(EquipmentNeed("weight", quantity = 2)))))
        assertEquals(DUMBBELL_EQUIPMENT_ID, db.loadEquipmentId()); assertTrue(db.isLoaded())
        assertEquals(2_500, SeedProfiles.home.loadGramsFor(db))        // the owner's pair: 2.5 kg each
        assertEquals(12_000, SeedProfiles.home.loadGramsFor(kbVariation()))   // kettlebell exercises still read the bell
        assertNull(SeedProfiles.home.loadGramsFor(db.copy(equipmentAlternatives = emptyList())))      // bodyweight: no load
    }

    private fun kbVariation() = ExerciseVariation(id = "kb-test", familyId = "kb", name = "KB", patterns = setOf(Pattern.HINGE), areas = setOf(Area.LOWER_BODY), kind = Kind.REPS,
        equipmentAlternatives = listOf(RequirementSet(needs = listOf(EquipmentNeed("kettlebell", minMassGrams = 8000)))))

    @Test fun theRealKettlebellExercisesAreLoadedAndBodyweightOnesAreNot() {
        val c = app.calisthenics.domain.content.parseCatalog(File(System.getProperty("repo.root"), "content/starter/catalog.json").readText())
        assertTrue(c.variation("kettlebell-swing")!!.isLoaded()); assertFalse(c.variation("pushup-standard")!!.isLoaded())
        assertEquals(KETTLEBELL_ID_FOR_TEST, c.variation("kettlebell-deadlift")!!.loadEquipmentId())
    }
    private companion object { const val KETTLEBELL_ID_FOR_TEST = "kettlebell" }
}
