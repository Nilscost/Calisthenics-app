package app.calisthenics.domain

import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.equipment.SeedProfiles
import app.calisthenics.domain.feedback.Execution
import app.calisthenics.domain.feedback.Rating
import app.calisthenics.domain.feedback.RatingOrigin
import app.calisthenics.domain.feedback.ResolvedOutcome
import app.calisthenics.domain.feedback.deriveEvidence
import app.calisthenics.domain.model.*
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.progression.ProgressionEngine
import app.calisthenics.domain.session.BlockLog
import app.calisthenics.domain.session.rowLogsFrom
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class FixedProgramsTest {
    private val catalog = parseCatalog(File(System.getProperty("repo.root") ?: ".", "content/starter/catalog.json").readText())
    private val byProgram = catalog.fixedPrograms.associateBy { it.id }

    @Test fun exactlyEightProgramsKeepContractOrderAndPrescriptions() {
        val contract = Json.parseToJsonElement(File(System.getProperty("repo.root") ?: ".", "docs/23-new-exercises.json").readText())
            .jsonObject.getValue("routines").jsonArray.associate { it.jsonObject.getValue("id").jsonPrimitive.content to it.jsonObject }
        assertEquals(8, catalog.fixedPrograms.size)
        for ((id, source) in contract) {
            val program = byProgram.getValue(id)
            val expectedItems = source.getValue("items").jsonArray.map { it.jsonObject }
            val expectedIds = expectedItems.flatMap { item ->
                List(if (item["perSide"]?.jsonPrimitive?.content?.toBoolean() == true) 2 else 1) { item.getValue("exerciseId").jsonPrimitive.content }
            }
            assertEquals(expectedIds, program.items.flatMap { item -> List(if (item.perSide) 2 else 1) { item.exerciseId } })
            val plan = (buildFixedProgramPlan(catalog, program, SeedProfiles.home, flow = program.flow) as FixedProgramPlanResult.Ready).plan
            assertEquals(expectedIds, plan.blocks.filter { it.variationId != null }.map { it.variationId })
            val exerciseBlocks = plan.blocks.filter { it.variationId != null }
            val expectedSetups = program.items.count { it.perSide && it.seconds != null }
            assertEquals(expectedSetups, plan.blocks.count { it.type == BlockType.TRANSITION })
            assertTrue(plan.blocks.filter { it.type == BlockType.TRANSITION }.all { it.durationSeconds == 5 })
            var blockIndex = 0
            expectedItems.forEach { expected ->
                val reps = expected["reps"]?.jsonPrimitive?.intOrNull
                val seconds = expected["seconds"]?.jsonPrimitive?.intOrNull
                val estimated = expected["estSeconds"]?.jsonPrimitive?.intOrNull
                val perSide = expected["perSide"]?.jsonPrimitive?.content?.toBoolean() ?: false
                val count = if (perSide) 2 else 1
                val blocks = exerciseBlocks.subList(blockIndex, blockIndex + count)
                assertEquals(List(count) { estimated ?: seconds ?: ((reps ?: 1) * 3) }, blocks.map { it.durationSeconds })
                assertEquals(List(count) { reps ?: seconds }, blocks.map { it.target!!.value })
                assertEquals(if (reps != null) List(count) { TargetType.REPS } else List(count) { TargetType.HOLD_SECONDS }, blocks.map { it.target!!.type })
                assertEquals(if (perSide) listOf(Side.LEFT, Side.RIGHT) else listOf(Side.NONE), blocks.map { it.side })
                blockIndex += count
            }
        }
    }

    @Test fun pilatesFlowPlanUsesOrderedEstimatedTimingAndDoesNotCreateAssumedProgress() {
        val program = byProgram.getValue("pilates-mat-beginner")
        val result = buildFixedProgramPlan(catalog, program, SeedProfiles.home, flow = true) as FixedProgramPlanResult.Ready
        val plan = result.plan
        assertTrue(plan.flow)
        assertEquals(program.id, plan.fixedProgramId)
        val expectedIds = program.items.flatMap { item -> List(if (item.perSide) 2 else 1) { item.exerciseId } }
        assertEquals(expectedIds, plan.blocks.filter { it.variationId != null }.map { it.variationId })
        assertEquals(program.items.sumOf { (it.estSeconds ?: it.seconds ?: ((it.reps ?: 1) * 3)) * (if (it.perSide) 2 else 1) + (if (it.perSide && it.seconds != null) 5 else 0) }, plan.plannedDurationSeconds)
        assertEquals(739, (buildFixedProgramPlan(catalog, byProgram.getValue("pilates-core-10"), SeedProfiles.home, flow = true) as FixedProgramPlanResult.Ready).plan.plannedDurationSeconds)
        assertTrue(plan.blocks.filter { it.side != Side.NONE }.map { it.side }.all { it == Side.LEFT || it == Side.RIGHT })
        assertTrue(plan.blocks.none { it.type == BlockType.PASSIVE_RECOVERY || it.prescriptionTier != null })
        assertTrue(plan.blocks.filter { it.type == BlockType.WORK }.all { it.freeTarget })
        assertTrue(plan.blocks.any { it.side == Side.LEFT })
        assertTrue(plan.blocks.any { it.side == Side.RIGHT })
        assertTrue(rowLogsFrom(plan, emptyMap()).isEmpty())

        val outcomes = plan.blocks.filter { it.type == BlockType.WORK }.map {
            ResolvedOutcome(it.id, it.variationId!!, Execution.COMPLETED, Rating.MET, RatingOrigin.ASSUMED, null, null, false, null)
        }
        val evidence = deriveEvidence("flow", 1, plan, outcomes, { catalog.variation(it)!!.familyId }, { _, _ -> 2 })
        assertTrue(evidence.isEmpty())
        assertTrue(ProgressionEngine(catalog).replay(evidence, emptyList(), autoProgression = true).events.isEmpty())
        val encoded = Json.encodeToString(WorkoutPlan.serializer(), plan)
        val restored = Json.decodeFromString(WorkoutPlan.serializer(), encoded)
        assertEquals(plan.fixedProgramId, restored.fixedProgramId)
        assertTrue(restored.flow)
    }

    @Test fun nonFlowFourCoreKeepsItsHoldLoggerTargets() {
        val program = byProgram.getValue("army-4c")
        assertFalse(program.flow)
        val plan = (buildFixedProgramPlan(catalog, program, SeedProfiles.home, flow = false) as FixedProgramPlanResult.Ready).plan
        assertFalse(plan.flow)
        assertEquals(program.items.flatMap { item -> List(if (item.perSide) 2 else 1) { item.exerciseId } }, plan.blocks.filter { it.variationId != null }.map { it.variationId })
        assertEquals(listOf(60, 60, 60, 30, 60, 60), plan.blocks.filter { it.variationId != null }.map { it.target!!.value })
        assertEquals(listOf(Side.NONE, Side.LEFT, Side.RIGHT, Side.NONE, Side.LEFT, Side.RIGHT), plan.blocks.filter { it.variationId != null }.map { it.side })
        assertTrue(plan.blocks.filter { it.type == BlockType.WORK }.all { !it.freeTarget && it.prescriptionTier == null })
    }

    @Test fun profileWithoutMatBlocksTheWholePilatesProgramWithoutSubstitutions() {
        val program = byProgram.getValue("pilates-mat-beginner")
        val profile = SeedProfiles.travel.copy(items = SeedProfiles.travel.items.filterNot { it.equipmentId == "mat" })
        val result = buildFixedProgramPlan(catalog, program, profile)
        assertTrue(result is FixedProgramPlanResult.Unavailable)
        val missing = (result as FixedProgramPlanResult.Unavailable).items
        assertTrue(missing.any { it.exerciseId == "pilates-roll-up" && "mat" in it.missing })
        assertTrue(missing.any { it.exerciseId == "pilates-seal" && "mat" in it.missing })
    }

    @Test fun flowOverrideDoesNotMutateTheCatalogDefault() {
        val program = byProgram.getValue("army-hsd")
        assertTrue(program.flow)
        val plan = (buildFixedProgramPlan(catalog, program, SeedProfiles.home, flow = false) as FixedProgramPlanResult.Ready).plan
        assertFalse(plan.flow)
        assertTrue(program.flow)
    }

    @Test fun heldPilatesDemonstrationsKeepProgramTargetsButDoNotClaimAClip() {
        val program = byProgram.getValue("pilates-mat-beginner")
        val plan = (buildFixedProgramPlan(catalog, program, SeedProfiles.home, flow = false) as FixedProgramPlanResult.Ready).plan
        val heldIds = catalog.variations.filter { it.demonstrationHold }.map { it.id }.toSet()
        val heldBlocks = plan.blocks.filter { it.variationId in heldIds }
        assertTrue(heldBlocks.isNotEmpty())
        assertTrue(heldBlocks.all { it.mediaId == null })
        assertTrue(heldBlocks.all { it.note?.contains("written instructions") == true })
        assertEquals(program.items.flatMap { item -> List(if (item.perSide) 2 else 1) { item.reps ?: item.seconds } },
            plan.blocks.filter { it.variationId != null }.map { it.target!!.value })
    }
}
