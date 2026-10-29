package app.calisthenics.domain

import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.equipment.isAvailable
import app.calisthenics.domain.equipment.SeedProfiles
import app.calisthenics.domain.model.*
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.progression.ProgressSnapshot
import app.calisthenics.domain.routine.StarterRoutine
import kotlin.test.Test

class ManualMobilityTest {
    private val base = parseCatalog(fixtureText("content/starter/catalog.json"))
    private val none = ProgressSnapshot(emptyMap(), emptyMap(), emptyMap(), emptyList())
    private val mobility = ExerciseVariation(
        id = "manual-mobility-test", familyId = "manual-mobility-test", name = "Wall mobility drill",
        patterns = setOf(Pattern.MOBILITY), kind = Kind.MOBILITY, defaultSeconds = 30,
        equipmentAlternatives = listOf(RequirementSet(needs = listOf(EquipmentNeed("pullup-bar")))),
        instructions = listOf("Move gently through a comfortable range."),
    )
    private val heldMobility = mobility.copy(id = "held-manual-mobility-test", familyId = "held-manual-mobility-test", demonstrationHold = true)
    private val catalog = base.copy(variations = base.variations + listOf(mobility, heldMobility))

    private fun plan(profile: app.calisthenics.domain.model.EquipmentProfile, edits: PlanEdits = PlanEdits(), cooldown: Boolean = false) =
        (buildTrainPlan(catalog, StarterRoutine.routine, none, profile,
            TrainSettings(rounds = 2, stretchOn = true, cooldownOn = cooldown, cooldownMode = CooldownMode.STRETCHING),
            edits = edits) as PlanResult.Ready).plan

    @Test fun newSplitStretchesStayOutOfAutomaticRecoveryButCanBePickedManually() {
        val optedSplits = base.variations.map {
            if (it.id == "stretch-split-front" || it.id == "stretch-split-middle") it.copy(optIn = true) else it
        }
        val splitId = "stretch-split-front"
        val authored = base.copy(variations = optedSplits.map {
            if (it.kind in setOf(Kind.REPS, Kind.HOLD)) it.copy(compatibleStretchIds = listOf(splitId, "stretch-back")) else it
        })
        val automatic = (buildTrainPlan(authored, StarterRoutine.routine, none, SeedProfiles.home,
            TrainSettings(rounds = 2, stretchOn = true)) as PlanResult.Ready).plan
        assertTrue(automatic.blocks.filter { it.type == BlockType.STRETCH }.none { it.variationId == splitId })

        val manual = (buildTrainPlan(authored, StarterRoutine.routine, none, SeedProfiles.home,
            TrainSettings(rounds = 2, stretchOn = true), edits = PlanEdits(stretchPicks = mapOf("push" to splitId))) as PlanResult.Ready).plan
        assertTrue(manual.blocks.any { it.type == BlockType.STRETCH && it.slotId == "push" && it.variationId == splitId })
    }

    @Test fun mobilityIsGearFilteredInTheExistingStretchPickerAndManualAddsCreateBlocks() {
        assertTrue(stretchChoices(catalog, SeedProfiles.home).any { it.id == mobility.id })
        assertFalse(stretchChoices(catalog, SeedProfiles.travel).any { it.id == mobility.id })
        assertTrue("the held fixture is available, so the exclusion check is non-vacuous", isAvailable(heldMobility, SeedProfiles.home))
        assertFalse(stretchChoices(catalog, SeedProfiles.home).any { it.id == heldMobility.id })
        assertFalse(stretchChoices(catalog, SeedProfiles.home).any { it.demonstrationHold })

        val extra = plan(SeedProfiles.home, PlanEdits(extraStretches = mapOf("push" to listOf(mobility.id))))
        assertTrue(extra.blocks.any { it.type == BlockType.STRETCH && it.variationId == mobility.id })
        val cooldown = plan(SeedProfiles.home, PlanEdits(cooldownAdded = listOf(mobility.id)), cooldown = true)
        assertTrue(cooldown.blocks.any { it.type == BlockType.COOLDOWN && it.variationId == mobility.id })
    }
}
