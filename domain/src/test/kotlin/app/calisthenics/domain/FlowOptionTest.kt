package app.calisthenics.domain

import app.calisthenics.domain.backup.*
import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.equipment.SeedProfiles
import app.calisthenics.domain.model.*
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.progression.ProgressSnapshot
import app.calisthenics.domain.routine.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** R8.3: Flow is a single preparation interval between every pair of work blocks. */
class FlowOptionTest {
    private val c = parseCatalog(File(System.getProperty("repo.root"), "content/starter/catalog.json").readText())
    private val none = ProgressSnapshot(emptyMap(), emptyMap(), emptyMap(), emptyList())
    private fun plan(s: TrainSettings) = (buildTrainPlan(c, StarterRoutine.routine, none, SeedProfiles.home, s.copy(exercisesPerRound = 4)) as PlanResult.Ready).plan

    /** The blocks after a work block, up to the next work block: "was a break there?" per work block index. */
    private fun breaksAfterWork(p: WorkoutPlan): List<Pair<String, Boolean>> {
        val work = p.blocks.indices.filter { p.blocks[it].type == BlockType.WORK }
        return work.mapIndexed { n, i ->
            val end = work.getOrNull(n + 1) ?: p.blocks.size
            "${p.blocks[i].slotId}#${p.blocks[i].roundIndex}" to p.blocks.subList(i + 1, end).any { it.type == BlockType.STRETCH || it.type == BlockType.PASSIVE_RECOVERY }
        }
    }

    @Test fun flowUsesOnePreparationBeforeEveryNextWorkBlockInCircuit() {
        val p = plan(TrainSettings(rounds = 3, stretchOn = false, flow = true))
        assertTrue(p.flow)
        assertFalse(p.blocks.any { it.type == BlockType.PASSIVE_RECOVERY || it.type == BlockType.STRETCH })
        val work = p.blocks.indices.filter { p.blocks[it].type == BlockType.WORK }
        work.drop(1).forEach { i -> assertEquals(BlockType.TRANSITION, p.blocks[i - 1].type) }
        assertTrue(p.blocks.filter { it.type == BlockType.TRANSITION }.all { it.durationSeconds == 5 && it.variationId != null })
        assertFalse(plan(TrainSettings(rounds = 3, stretchOn = false)).flow)
    }

    @Test fun straightSetsFlowKeepsTheRestBetweenSetsOfOneExerciseOnly() {
        val p = plan(TrainSettings(rounds = 3, stretchOn = false, flow = true, format = WorkoutFormat.STRAIGHT))
        assertFalse(p.blocks.any { it.type == BlockType.PASSIVE_RECOVERY || it.type == BlockType.STRETCH })
        assertEquals(p.blocks.count { it.type == BlockType.WORK } - 1, p.blocks.count { it.type == BlockType.TRANSITION })
    }

    @Test fun pairsFlowKeepsTheRestBetweenTheSetsOfAPairAndDropsItInsideThePair() {
        val p = plan(TrainSettings(rounds = 2, stretchOn = false, flow = true, format = WorkoutFormat.PAIRS))
        assertFalse(p.blocks.any { it.type == BlockType.PASSIVE_RECOVERY || it.type == BlockType.STRETCH })
        assertEquals(p.blocks.count { it.type == BlockType.WORK } - 1, p.blocks.count { it.type == BlockType.TRANSITION })
    }

    @Test fun flowRemovesRecoveryAndRestModeKeepsConfiguredRecovery() {
        val off = plan(TrainSettings(rounds = 3)); val on = plan(TrainSettings(rounds = 3, flow = true))
        assertTrue(on.plannedDurationSeconds < off.plannedDurationSeconds)
        assertFalse(on.blocks.any { it.type == BlockType.STRETCH || it.type == BlockType.PASSIVE_RECOVERY })
        assertTrue(off.blocks.any { it.type == BlockType.STRETCH || it.type == BlockType.PASSIVE_RECOVERY })
    }

    @Test fun preparationDurationIsClampedAndChangesEveryFlowTransition() {
        val p = plan(TrainSettings(rounds = 2, flow = true, preparationSeconds = 60))
        assertEquals(30, p.preparationSeconds)
        val transitions = p.blocks.withIndex().filter { it.value.type == BlockType.TRANSITION }
        assertTrue(transitions.isNotEmpty())
        assertTrue(transitions.all { (i, tr) ->
            val next = p.blocks[i + 1]
            tr.durationSeconds == 30 && next.type == BlockType.WORK && tr.variationId == next.variationId && tr.target == next.target && tr.side == next.side
        })
    }

    @Test fun flowIsSavedWithARoutineCarriedToTodaysSettingsAndKeptByTheBackup() {
        val s = TrainSettings(rounds = 3, flow = true, preparationSeconds = 8)
        val saved = savedFrom("f1", "Flowing", 1, StarterRoutine.routine, PlanEdits(), s, ProgressionRule(), 5L)
        assertTrue(saved.flow)
        assertEquals(BetweenSetMode.FLOW, saved.betweenSetMode)
        assertEquals(8, saved.preparationSeconds)
        assertEquals(BetweenSetMode.FLOW, settingsFor(saved, TrainSettings()).effectiveBetweenSetMode())
        assertEquals(8, settingsFor(saved, TrainSettings()).preparationSeconds)
        val payload = BackupPayload(Preferences(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), savedRoutines = listOf(saved))
        val back = (importBackup(exportBackup(payload, "0.11.1", 1L)) as ImportResult.Ok).payload.savedRoutines.single()
        assertTrue(back.flow); assertEquals(saved, back)
    }

    @Test fun legacyModeMappingGivesFlowPriorityAndStretchOtherwise() {
        assertEquals(BetweenSetMode.FLOW, TrainSettings(flow = true, stretchOn = true).effectiveBetweenSetMode())
        assertEquals(BetweenSetMode.STRETCH, TrainSettings(flow = false, stretchOn = true).effectiveBetweenSetMode())
        assertEquals(BetweenSetMode.REST, TrainSettings(flow = false, stretchOn = false).effectiveBetweenSetMode())
    }

    @Test fun allModesWorkAcrossEverySetFormatAndExplicitModeOverridesLegacyFlags() {
        for (format in WorkoutFormat.entries) {
            for (mode in BetweenSetMode.entries) {
                val p = plan(TrainSettings(rounds = 2, format = format, betweenSetMode = mode,
                    flow = mode == BetweenSetMode.FLOW, stretchOn = mode != BetweenSetMode.FLOW))
                assertEquals(mode, p.betweenSetMode)
                when (mode) {
                    BetweenSetMode.FLOW -> {
                        assertFalse(p.blocks.any { it.type == BlockType.PASSIVE_RECOVERY || it.type == BlockType.STRETCH })
                        assertEquals(p.blocks.count { it.type == BlockType.WORK } - 1, p.blocks.count { it.type == BlockType.TRANSITION })
                    }
                    BetweenSetMode.REST -> assertTrue(p.blocks.any { it.type == BlockType.PASSIVE_RECOVERY })
                    BetweenSetMode.STRETCH -> assertTrue(p.blocks.any { it.type == BlockType.STRETCH })
                }
            }
        }
        val restWins = plan(TrainSettings(rounds = 2, betweenSetMode = BetweenSetMode.REST, flow = true, stretchOn = true))
        assertFalse(restWins.flow)
        assertFalse(restWins.blocks.any { it.type == BlockType.STRETCH })
        assertTrue(restWins.blocks.any { it.type == BlockType.PASSIVE_RECOVERY })
        val stretchWins = plan(TrainSettings(rounds = 2, betweenSetMode = BetweenSetMode.STRETCH, flow = true, stretchOn = false))
        assertFalse(stretchWins.flow)
        assertTrue(stretchWins.blocks.any { it.type == BlockType.STRETCH })
    }
}
