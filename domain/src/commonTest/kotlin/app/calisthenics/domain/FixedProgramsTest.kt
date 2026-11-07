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
import kotlin.test.Test

class FixedProgramsTest {
    private val catalog = parseCatalog(fixtureText("content/starter/catalog.json"))
    private val byProgram = catalog.fixedPrograms.associateBy { it.id }
    /** Owner 2026-10-26: an item is two blocks (Left, Right) only when its exercise is SPLIT; an ALTERNATING one is a single block lasting twice the per-side estimate. */
    private fun blocksOf(exerciseId: String, perSide: Boolean) = if (perSide && catalog.variation(exerciseId)!!.sideMode == SideMode.SPLIT) 2 else 1
    private fun durationFactor(exerciseId: String, perSide: Boolean) = if (perSide && catalog.variation(exerciseId)!!.sideMode == SideMode.ALTERNATING) 2 else 1

    // R8.1-T09 (F11): was "exactlyEight...": the eight drills stay as unlisted building blocks and two merged sessions (Army PRT, Pilates mat) are listed
    @Test fun theEightDrillsAndTheTwoMergedSessionsKeepContractOrderAndPrescriptions() {
        val contract = Json.parseToJsonElement(fixtureText("docs/23-new-exercises.json"))
            .jsonObject.getValue("routines").jsonArray.associate { it.jsonObject.getValue("id").jsonPrimitive.content to it.jsonObject }
        assertEquals(10, catalog.fixedPrograms.size)
        assertEquals(listOf("army-prt", "pilates-mat"), catalog.fixedPrograms.filter { it.listed }.map { it.id })
        for ((id, source) in contract) {
            val program = byProgram.getValue(id)
            val expectedItems = source.getValue("items").jsonArray.map { it.jsonObject }
            val expectedIds = expectedItems.flatMap { item ->
                List(blocksOf(item.getValue("exerciseId").jsonPrimitive.content, item["perSide"]?.jsonPrimitive?.content?.toBoolean() == true)) { item.getValue("exerciseId").jsonPrimitive.content }
            }
            assertEquals(expectedIds, program.items.flatMap { item -> List(blocksOf(item.exerciseId, item.perSide)) { item.exerciseId } })
            val plan = (buildFixedProgramPlan(catalog, program, SeedProfiles.home, flow = program.flow) as FixedProgramPlanResult.Ready).plan
            assertEquals(expectedIds, plan.blocks.filter { it.type == BlockType.WORK || it.type == BlockType.STRETCH }.map { it.variationId })
            val exerciseBlocks = plan.blocks.filter { it.type == BlockType.WORK || it.type == BlockType.STRETCH }
            val expectedSetups = if (program.flow) (exerciseBlocks.count { it.type == BlockType.WORK } - 1).coerceAtLeast(0) + program.items.count { blocksOf(it.exerciseId, it.perSide) == 2 && catalog.variation(it.exerciseId)?.kind in setOf(Kind.STRETCH, Kind.MOBILITY) } else program.items.count { blocksOf(it.exerciseId, it.perSide) == 2 && it.seconds != null }
            assertEquals(expectedSetups, plan.blocks.count { it.type == BlockType.TRANSITION })
            assertTrue(plan.blocks.filter { it.type == BlockType.TRANSITION }.all { it.durationSeconds == 5 })
            var blockIndex = 0
            expectedItems.forEach { expected ->
                val reps = expected["reps"]?.jsonPrimitive?.intOrNull
                val seconds = expected["seconds"]?.jsonPrimitive?.intOrNull
                val estimated = expected["estSeconds"]?.jsonPrimitive?.intOrNull
                val perSide = expected["perSide"]?.jsonPrimitive?.content?.toBoolean() ?: false
                val exerciseId = expected.getValue("exerciseId").jsonPrimitive.content
                val count = blocksOf(exerciseId, perSide)
                val blocks = exerciseBlocks.subList(blockIndex, blockIndex + count)
                assertEquals(List(count) { (estimated ?: seconds ?: ((reps ?: 1) * 3)) * durationFactor(exerciseId, perSide) }, blocks.map { it.durationSeconds })
                assertEquals(List(count) { reps ?: seconds }, blocks.map { it.target!!.value })
                assertEquals(if (reps != null) List(count) { TargetType.REPS } else List(count) { TargetType.HOLD_SECONDS }, blocks.map { it.target!!.type })
                assertEquals(if (count == 2) listOf(Side.LEFT, Side.RIGHT) else listOf(Side.NONE), blocks.map { it.side })
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
        val expectedIds = program.items.flatMap { item -> List(blocksOf(item.exerciseId, item.perSide)) { item.exerciseId } }
        assertEquals(expectedIds, plan.blocks.filter { it.type == BlockType.WORK || it.type == BlockType.STRETCH }.map { it.variationId })
        assertEquals(program.items.sumOf { (it.estSeconds ?: it.seconds ?: ((it.reps ?: 1) * 3)) * (if (it.perSide) 2 else 1) } + plan.blocks.count { it.type == BlockType.TRANSITION } * 5, plan.plannedDurationSeconds)
        val core = (buildFixedProgramPlan(catalog, byProgram.getValue("pilates-core-10"), SeedProfiles.home, flow = true) as FixedProgramPlanResult.Ready).plan
        assertEquals(byProgram.getValue("pilates-core-10").items.sumOf { itemSeconds(it) * if (it.perSide) 2 else 1 } + core.blocks.count { it.type == BlockType.TRANSITION } * 5, core.plannedDurationSeconds)
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
        assertEquals(program.items.flatMap { item -> List(blocksOf(item.exerciseId, item.perSide)) { item.exerciseId } }, plan.blocks.filter { it.type == BlockType.WORK || it.type == BlockType.STRETCH }.map { it.variationId })
        assertEquals(listOf(60, 60, 60, 15, 15, 60, 60), plan.blocks.filter { it.type == BlockType.WORK || it.type == BlockType.STRETCH }.map { it.target!!.value })
        assertEquals(listOf(Side.NONE, Side.LEFT, Side.RIGHT, Side.LEFT, Side.RIGHT, Side.LEFT, Side.RIGHT), plan.blocks.filter { it.type == BlockType.WORK || it.type == BlockType.STRETCH }.map { it.side })
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
        val heldId = "pilates-roll-up"
        val heldCatalog = catalog.copy(variations = catalog.variations.map {
            if (it.id == heldId) it.copy(demonstrationHold = true, mediaId = "synthetic-held-demo") else it
        })
        val plan = (buildFixedProgramPlan(heldCatalog, program, SeedProfiles.home, flow = false) as FixedProgramPlanResult.Ready).plan
        val heldBlocks = plan.blocks.filter { it.variationId == heldId }
        assertTrue(heldBlocks.isNotEmpty())
        assertTrue(heldBlocks.all { it.mediaId == null })
        assertTrue(heldBlocks.all { it.note?.contains("written instructions") == true })
        val heldItem = program.items.single { it.exerciseId == heldId }
        assertTrue(heldBlocks.all { it.target?.value == (heldItem.reps ?: heldItem.seconds) })
        assertEquals(program.items.flatMap { item -> List(blocksOf(item.exerciseId, item.perSide)) { item.reps ?: item.seconds } },
            plan.blocks.filter { it.type == BlockType.WORK || it.type == BlockType.STRETCH }.map { it.target!!.value })
    }

    // R8.1-T09 (F11): one 30+ minute session each, as the Preview estimates it
    @Test fun theMergedArmyAndPilatesSessionsAreAtLeastThirtyMinutesInOneSession() {
        for (id in listOf("army-prt", "pilates-mat")) {
            val plan = (buildFixedProgramPlan(catalog, byProgram.getValue(id), SeedProfiles.home) as FixedProgramPlanResult.Ready).plan
            assertTrue("$id ${plan.plannedDurationSeconds / 60} min", plan.plannedDurationSeconds >= 30 * 60)
            assertTrue(plan.blocks.sumOf { it.durationSeconds } == plan.plannedDurationSeconds)
        }
        val army = byProgram.getValue("army-prt").items.map { it.exerciseId }
        val parts = listOf("army-pd", "army-4c", "army-hsd", "army-ssd").flatMap { byProgram.getValue(it).items.map { i -> i.exerciseId } }
        assertEquals(parts, army.take(parts.size))                                   // Preparation Drill, 4 for the Core, Hip, Shoulder in the manual order
        val cd1 = army.drop(parts.size).take(5)
        assertEquals(listOf("jump-vertical", "v-up", "mountain-climber", "leg-tuck-and-twist", "pushup-single-leg"), cd1)   // Conditioning Drill 1
        assertEquals(byProgram.getValue("army-rd").items.map { it.exerciseId }, army.takeLast(byProgram.getValue("army-rd").items.size))   // Recovery Drill last
        assertTrue(byProgram.getValue("pilates-mat").items.size >= 20)
    }
}
