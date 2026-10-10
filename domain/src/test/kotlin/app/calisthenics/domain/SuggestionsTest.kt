package app.calisthenics.domain

import app.calisthenics.domain.equipment.SeedProfiles
import app.calisthenics.domain.goals.Goals
import app.calisthenics.domain.model.*
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.progression.*
import app.calisthenics.domain.routine.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** V27 (R29, D9): one dismissible suggestion at a time, never required; bodyweight only stays complete. */
class SuggestionsTest {
    private val catalog = app.calisthenics.domain.content.parseCatalog(File(System.getProperty("repo.root"), "content/starter/catalog.json").readText())
    private val bare = EquipmentProfile("bare", "Bare", emptyList(), setOf("floor-space"))
    private fun vp(id: String, tier: Int, achieved: Set<Int> = emptySet(), load: Int? = null, raise: Boolean = false, baseline: Int? = 1) =
        VariationProgress(id, catalog.variation(id)!!.familyId, tier, 0, baseline, achieved, null, emptyList(), 0, 0, false, false, null, null, load, emptyMap(), 0, raise)
    private fun snap(vararg v: VariationProgress) = ProgressSnapshot(v.associateBy { it.variationId }, emptyMap(), emptyMap(), emptyList())
    private val none = snap()

    @Test fun nothingIsSuggestedWithoutProgress() {
        assertNull(suggest(catalog, bare, none, emptySet()))
        assertNull(suggest(catalog, SeedProfiles.home, none, emptySet()))
    }

    @Test fun aTooEasySuggestionComesFirstAndCanBeDismissed() {
        val p = snap(vp("pushup-standard", 3, raise = true), vp("row-band", 5, setOf(1, 2, 3, 4, 5)))
        val s = suggest(catalog, bare, p, emptySet())
        assertEquals(Suggestion.RaiseLevel("pushup-standard", 4), s)
        // dismissed: the next kind of suggestion (here the low bar the mastered band row opens) is next; never two at once
        val after = suggest(catalog, bare, p, setOf(s!!.id))
        assertTrue(after is Suggestion.GetEquipment)
        assertFalse(suggest(catalog, bare, snap(vp("pushup-standard", 5, raise = true)), emptySet()) is Suggestion.RaiseLevel)   // top level: nothing to raise
    }

    @Test fun masteredBandRowSuggestsALowBarForInvertedRows() {
        val p = snap(vp("row-band", 5, setOf(1, 2, 3, 4, 5)))
        val s = suggest(catalog, bare.copy(items = listOf(EquipmentItem("resistance-band", suitability = setOf("stable-anchor")), EquipmentItem("pullup-bar"))), p, emptySet()) as Suggestion.GetEquipment
        assertEquals("low-bar", s.equipmentId); assertEquals("inverted-row-bent-knees", s.unlocksVariationId)
        // owning a low bar removes it
        // owning a low bar removes it; the next thing the mastered band row opens (a dumbbell for the one-arm row) is then offered instead, one at a time
        val own = suggest(catalog, bare.copy(items = listOf(EquipmentItem("low-bar"), EquipmentItem("pullup-bar"))), p, emptySet()) as Suggestion.GetEquipment
        assertEquals("weight", own.equipmentId); assertEquals("dumbbell-row", own.unlocksVariationId)
        assertNull(suggest(catalog, bare.copy(items = listOf(EquipmentItem("low-bar"), EquipmentItem("pullup-bar"), EquipmentItem("weight", quantity = 2, massGrams = 2_500), EquipmentItem("kettlebell", massGrams = 12_000))), p, emptySet()))
    }

    @Test fun dipSupportIsHintedAfterASolidPushUp() {
        val withChair = bare.copy(items = listOf(EquipmentItem("chair", suitability = setOf("stable"))))
        val first = suggest(catalog, bare, snap(vp("pushup-standard", 4, setOf(1, 2, 3, 4))), emptySet()) as Suggestion.GetEquipment
        assertEquals("chair", first.equipmentId)   // the nearer step: feet-elevated push-ups need a chair
        val s = suggest(catalog, withChair, snap(vp("pushup-standard", 4, setOf(1, 2, 3, 4))), emptySet()) as Suggestion.GetEquipment
        assertEquals("dip-support", s.equipmentId); assertEquals("dip-support-hold", s.unlocksVariationId)
        assertNull(suggest(catalog, bare, snap(vp("pushup-standard", 2)), emptySet()))   // not before
    }

    @Test fun aHeavierWeightIsSuggestedOnceEveryLevelIsEarnedAtTheBellInUse() {
        val p = snap(vp("kettlebell-deadlift", 5, setOf(1, 2, 3, 4, 5), load = 12_000))
        assertEquals(Suggestion.HeavierWeight("kettlebell-deadlift", "kettlebell", 16_000), suggest(catalog, SeedProfiles.home, p, emptySet()))
        val heavier = SeedProfiles.home.copy(items = SeedProfiles.home.items.map { if (it.equipmentId == "kettlebell") it.copy(massGrams = 16_000) else it })
        assertNull(suggest(catalog, heavier, p, emptySet()))   // already owns the next bell
        assertNull(suggest(catalog, SeedProfiles.home, snap(vp("kettlebell-deadlift", 4, setOf(1, 2, 3, 4), load = 12_000)), emptySet()))
    }

    @Test fun theBodyweightOnlyProfileAlwaysGetsACompletePlan() {   // D9
        val routines = listOf(app.calisthenics.domain.routine.StarterRoutine.routine)
        for (g in Goals.all.filter { it.entryVariationId == null }) {
            val r = (buildTrainPlan(catalog, routines.first(), none, bare, TrainSettings(goalId = g.id, rounds = 3)) as? PlanResult.Ready)
            assertNotNull("goal ${g.id} has a plan with no equipment", r)
            assertTrue(r!!.plan.blocks.any { it.type == BlockType.WORK })
        }
        for (preset in Presets.all(catalog, bare)) {
            val r = resolveSaved(preset, settingsFor(preset, TrainSettings()))
            assertTrue(preset.id, buildTrainPlan(catalog, r.routine, none, bare, r.settings, edits = r.edits) is PlanResult.Ready)
        }
    }
}
