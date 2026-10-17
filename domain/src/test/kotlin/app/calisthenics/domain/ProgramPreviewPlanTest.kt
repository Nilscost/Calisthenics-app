package app.calisthenics.domain

import app.calisthenics.domain.backup.*
import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.equipment.SeedProfiles
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
        // Flow keeps the rest between sets of a circuit and drops it between exercises
        assertEquals(1, circuit.blocks.count { it.type == BlockType.PASSIVE_RECOVERY })
        assertEquals(exercises * 2 - 1, plan(sets = 2, flow = false).blocks.count { it.type == BlockType.PASSIVE_RECOVERY })   // flow off: rest after every entry but the last
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
