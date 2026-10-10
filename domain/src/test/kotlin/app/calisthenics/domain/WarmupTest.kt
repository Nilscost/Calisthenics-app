package app.calisthenics.domain

import app.calisthenics.domain.equipment.SeedProfiles
import app.calisthenics.domain.model.*
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.routine.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** V23 (D12, C-B): a warm-up before any workout, counted in the time. */
class WarmupTest {
    private val catalog = app.calisthenics.domain.content.parseCatalog(File(System.getProperty("repo.root"), "content/starter/catalog.json").readText())
    private val none = app.calisthenics.domain.progression.ProgressSnapshot(emptyMap(), emptyMap(), emptyMap(), emptyList())
    private val home = SeedProfiles.home
    private fun plan(s: TrainSettings, r: Routine = StarterRoutine.routine) = (buildTrainPlan(catalog, r, none, home, s) as PlanResult.Ready).plan

    @Test fun warmUpComesFirstAndItsTimeIsCounted() {
        val off = plan(TrainSettings(rounds = 3, warmupOn = false)); val on = plan(TrainSettings(rounds = 3, warmupOn = true))
        assertTrue(off.blocks.none { it.type == BlockType.WARMUP })
        val warm = on.blocks.filter { it.type == BlockType.WARMUP }
        assertTrue(warm.isNotEmpty())
        assertEquals(catalog.warmupTemplate.toSet(), warm.mapNotNull { it.variationId }.toSet())   // the catalog's template when the routine has none (order: RAMP, see RampTest)
        assertEquals(BlockType.WARMUP, on.blocks.first().type)                              // first in the workout
        assertTrue(on.blocks.indexOfLast { it.type == BlockType.WARMUP } < on.blocks.indexOfFirst { it.type == BlockType.WORK })
        assertTrue(on.plannedDurationSeconds >= off.plannedDurationSeconds + warm.sumOf { it.durationSeconds })
        assertEquals(on.blocks.sumOf { it.durationSeconds }, on.plannedDurationSeconds)  // everything is counted
    }

    @Test fun aRoutinesOwnWarmUpReplacesTheCatalogTemplateInAnyFormat() {
        val own = listOf("warmup-shoulder-band", "warmup-wrist-prep")
        val r = StarterRoutine.routine.copy(warmup = own)
        for (f in WorkoutFormat.entries) {
            val p = plan(TrainSettings(rounds = 2, warmupOn = true, format = f), r)
            assertEquals(f.name, own, p.blocks.filter { it.type == BlockType.WARMUP }.mapNotNull { it.variationId })
            assertEquals(f.name, BlockType.WARMUP, p.blocks.first().type)
        }
    }

    @Test fun warmUpSurvivesSavingARoutineAndThePresetTurnsItOn() {
        val rr = Presets.recommended(catalog, home)
        assertTrue(settingsFor(rr, TrainSettings()).warmupOn && !settingsFor(Presets.minimalist(catalog, home), TrainSettings(warmupOn = true)).warmupOn)
        val saved = savedFrom("x", "mine", 1, rr.routine, PlanEdits(), TrainSettings(rounds = 3, warmupOn = true), rr.rule, 1L)
        assertTrue(saved.warmupOn); assertEquals(rr.routine.warmup, saved.routine.warmup)
        // choosing it, then switching the warm-up off on Train, is respected
        val today = settingsFor(saved, TrainSettings()).copy(warmupOn = false)
        assertFalse(resolveSaved(saved, today).settings.warmupOn)
    }
}
