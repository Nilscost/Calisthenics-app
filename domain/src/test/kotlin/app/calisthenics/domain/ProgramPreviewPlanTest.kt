package app.calisthenics.domain

import app.calisthenics.domain.backup.*
import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.equipment.SeedProfiles
import app.calisthenics.domain.equipment.isAvailable
import app.calisthenics.domain.model.*
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.routine.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** R8.1-T08 (F9, F5, F7): a program opens as a starting point: formats, sets, Flow and rest work on it; saving it leaves the program as it was. */
class ProgramPreviewPlanTest {
    private val c = parseCatalog(File(System.getProperty("repo.root"), "content/starter/catalog.json").readText())
    private val program = c.fixedPrograms.first { it.id == "army-hsd" }
    private val content = programContentOf(program)
    private fun plan(content: ProgramContent = this.content, flow: Boolean = true, sets: Int = 1, f: WorkoutFormat = WorkoutFormat.CIRCUIT) =
        (buildProgramPlan(c, content, SeedProfiles.home, flow, sets, f) as FixedProgramPlanResult.Ready).plan

    @Test fun oneSetInFlowIsExactlyTheCatalogProgram() {
        val p = plan()
        val fixed = (buildFixedProgramPlan(c, program, SeedProfiles.home, flow = true) as FixedProgramPlanResult.Ready).plan
        assertEquals(fixed.blocks, p.blocks); assertEquals(program.id, p.fixedProgramId); assertTrue(p.flow)
        assertTrue(p.blocks.none { it.type == BlockType.PASSIVE_RECOVERY })
    }

    @Test fun flowOffAddsTheRestBetweenExercisesAndSetsRepeatTheProgramInTheChosenFormat() {
        val off = plan(flow = false)
        val exercises = program.items.size
        assertEquals(exercises - 1, off.blocks.count { it.type == BlockType.PASSIVE_RECOVERY })
        assertTrue(off.blocks.filter { it.type == BlockType.PASSIVE_RECOVERY }.all { it.durationSeconds == DEFAULT_PROGRAM_REST_SECONDS })
        val circuit = plan(sets = 2); val straight = plan(sets = 2, f = WorkoutFormat.STRAIGHT)
        val sideless = { p: WorkoutPlan -> p.blocks.filter { it.type == BlockType.WORK || it.type == BlockType.STRETCH }.map { "${it.variationId}:${it.roundIndex}" }.distinct() }
        assertEquals(exercises * 2, sideless(circuit).size)
        assertEquals("${program.items[0].exerciseId}:1", sideless(circuit).first())
        assertEquals(listOf("${program.items[0].exerciseId}:1", "${program.items[0].exerciseId}:2", "${program.items[1].exerciseId}:1"), sideless(straight).take(3))
        assertEquals(2, circuit.rounds); assertEquals(WorkoutFormat.STRAIGHT, straight.format)
        // Flow replaces all recovery with one preparation interval between each pair of work blocks.
        assertEquals(0, circuit.blocks.count { it.type == BlockType.PASSIVE_RECOVERY })
        assertEquals(circuit.blocks.count { it.type == BlockType.WORK } - 1, circuit.blocks.withIndex().count { (i, b) -> b.type == BlockType.TRANSITION && circuit.blocks.getOrNull(i + 1)?.type == BlockType.WORK })
        assertEquals(exercises * 2 - 1, plan(sets = 2, flow = false).blocks.count { it.type == BlockType.PASSIVE_RECOVERY })   // Rest: interval after every entry but the last
    }

    @Test fun fixedProgramStretchModeAvoidsCoolDownOnlyStretchesAndExplainsSafeRestFallback() {
        val item = program.items.first { source ->
            c.variation(source.exerciseId)?.compatibleStretchIds.orEmpty().any { id ->
                c.variation(id)?.let { it.kind == Kind.STRETCH && !it.optIn && isAvailable(it, SeedProfiles.home) } == true
            }
        }
        val compatible = c.variation(item.exerciseId)!!.compatibleStretchIds.mapNotNull(c::variation)
            .filter { it.kind == Kind.STRETCH && !it.optIn && isAvailable(it, SeedProfiles.home) }
        val cooldownIds = compatible.map { it.id }.toSet()
        val restricted = c.copy(variations = c.variations.map { if (it.id in cooldownIds) it.copy(stretchUse = StretchUse.COOL_DOWN) else it })
        val content = ProgramContent(program.id, program.name, listOf(item, item), restSeconds = 20)
        val result = buildProgramPlan(restricted, content, SeedProfiles.home, false, sets = 2, betweenSetMode = BetweenSetMode.STRETCH)
        val plan = (result as FixedProgramPlanResult.Ready).plan

        assertTrue(plan.blocks.filter { it.type == BlockType.PASSIVE_RECOVERY }.isNotEmpty())
        assertFalse(plan.blocks.any { it.variationId in cooldownIds && it.recoveryForBlockIds.isNotEmpty() })
        assertTrue(plan.changesExplained.any { it.contains("Some program breaks use passive rest") })
    }

    @Test fun editingTheItemsNeverChangesTheCatalogProgram() {
        val before = program.items
        val edited = content.copy(items = content.items.drop(1).map { adjustProgramItem(it, 2) } + defaultProgramItem(c, c.variation("plank")!!))
        val p = plan(edited)
        assertEquals(before.size, program.items.size); assertEquals(before, c.fixedPrograms.first { it.id == "army-hsd" }.items)
        assertEquals(edited.items.size, p.blocks.count { it.type == BlockType.WORK || it.type == BlockType.STRETCH } - edited.items.count { it.perSide })
        assertEquals(12, (edited.items.first().reps))
        val saved = savedFromProgram("mine", "My hips", 1, edited, TrainSettings(rounds = 2, format = WorkoutFormat.PAIRS, flow = false), 5L)
        assertEquals(edited.copy(name = "My hips"), saved.program); assertEquals(2, saved.sets); assertFalse(saved.flow); assertEquals(WorkoutFormat.PAIRS, saved.format)
        val back = (importBackup(exportBackup(BackupPayload(Preferences(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), savedRoutines = listOf(saved)), "0.11.1", 1L)) as ImportResult.Ok).payload.savedRoutines.single()
        assertEquals(saved, back)
    }

    @Test fun newProgramItemsUseAppLevelsAndExplicitLevelFiveExtensionsCountAsLevelFive() {
        val v = c.variation("pushup-standard")!!
        val base = defaultProgramItem(c, v)
        assertEquals(2, base.appLevel)
        val levelTwo = (buildProgramPlan(c, content.copy(items = listOf(base)), SeedProfiles.home, true) as FixedProgramPlanResult.Ready).plan.blocks.single()
        assertEquals(2, levelTwo.prescriptionTier)
        assertEquals(c.policyFor(v)!!.tier(2).target, levelTwo.target)
        assertFalse(levelTwo.freeTarget)

        val personal = c.copy(personalTargets = mapOf(v.id to PersonalLevelTargets(3, listOf(5, 8, 10, 12, 14))))
        val levelFive = base.copy(appLevel = 5, levelFiveTarget = 15, reps = 15)
        val extended = (buildProgramPlan(personal, content.copy(items = listOf(levelFive)), SeedProfiles.home, true) as FixedProgramPlanResult.Ready).plan.blocks.single()
        assertEquals(Target(TargetType.REPS, 15), extended.target)
        assertEquals(5, extended.prescriptionTier)
        assertTrue(extended.levelFiveEligible)
        assertEquals(3, extended.targetRevision)
        assertFalse(extended.freeTarget)
    }

    @Test fun anItemTheEquipmentCannotDoBlocksTheProgramAsAWholeUntilItIsRemoved() {
        val pilates = programContentOf(c.fixedPrograms.first { it.id == "pilates-mat-beginner" })
        val r = buildProgramPlan(c, pilates, SeedProfiles.travel, true)
        assertTrue(r is FixedProgramPlanResult.Unavailable)
        val missing = (r as FixedProgramPlanResult.Unavailable).items.map { it.exerciseId }.toSet()
        assertTrue(buildProgramPlan(c, pilates.copy(items = pilates.items.filter { it.exerciseId !in missing }), SeedProfiles.travel, true) is FixedProgramPlanResult.Ready)
    }

    @Test fun startingAProgramOnTrainSetsOneSetCircuitAndItsOwnFlowAsTheStartValue() {
        val s = settingsForProgram(c.fixedPrograms.first { it.flow }, TrainSettings(rounds = 4, format = WorkoutFormat.PAIRS, routineId = "x", warmupOn = true, cooldownOn = true))
        assertEquals(1, s.rounds); assertEquals(WorkoutFormat.CIRCUIT, s.format); assertTrue(s.flow); assertNull(s.routineId); assertFalse(s.warmupOn || s.cooldownOn)
        assertFalse(settingsForProgram(c.fixedPrograms.first { !it.flow }, TrainSettings(flow = true)).flow)
    }
}
