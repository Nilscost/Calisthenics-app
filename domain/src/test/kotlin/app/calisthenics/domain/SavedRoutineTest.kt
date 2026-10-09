package app.calisthenics.domain

import app.calisthenics.domain.backup.*
import app.calisthenics.domain.model.*
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.routine.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** V18 (R13, D5): saved routines. */
class SavedRoutineTest {
    private val catalog = app.calisthenics.domain.content.parseCatalog(File(System.getProperty("repo.root"), "content/starter/catalog.json").readText())
    private val none = app.calisthenics.domain.progression.ProgressSnapshot(emptyMap(), emptyMap(), emptyMap(), emptyList())
    private val home = app.calisthenics.domain.equipment.SeedProfiles.home
    private val base = StarterRoutine.routine
    private val settings = TrainSettings(rounds = 3, stretchOn = false, format = WorkoutFormat.PAIRS)
    private val edits = PlanEdits(swaps = mapOf("push" to "pushup-diamond"), removed = setOf("core2"), added = listOf(AddedSlot("squat", slotFor(catalog.variation("pushup-archer")!!))),
        tierOverrides = mapOf("pull" to 4), stretchPicks = mapOf("core" to "stretch-frog"))
    private val rule = ProgressionRule(RuleKind.REP_RANGE)

    private fun planOf(r: ResolvedRoutine) = (buildTrainPlan(catalog, r.routine, none, home, r.settings, edits = r.edits) as PlanResult.Ready).plan

    @Test fun aSavedRoutineKeepsTheChosenExercisesOverridesFormatRuleAndRest() {
        val live = (buildTrainPlan(catalog, base, none, home, settings, edits = edits.copy(rule = rule)) as PlanResult.Ready).plan
        val saved = savedFrom("rt-1", "  Push day ", 1, base, edits, settings, rule, now = 10L)
        assertEquals("Push day", saved.name)
        val back = planOf(resolveSaved(saved, settingsFor(saved, TrainSettings())))
        // the rebuilt workout is the one that was saved: same order of exercises, targets, format, rule, rest type and length
        assertEquals(live.blocks.map { it.variationId to it.target }, back.blocks.map { it.variationId to it.target })
        assertEquals(WorkoutFormat.PAIRS, back.format); assertEquals(rule, back.rule); assertFalse(back.stretchOn); assertEquals(3, back.rounds)
        assertEquals(live.plannedDurationSeconds, back.plannedDurationSeconds)
        assertEquals(listOf("pushup-diamond"), back.blocks.filter { it.slotId == "push" }.mapNotNull { it.variationId }.distinct())
        assertTrue(back.blocks.none { it.slotId == "core2" })
    }

    @Test fun savedStructureLivesInTheSlotsNotInTheOverlay() {
        val saved = savedFrom("rt-1", "x", 1, base, edits, settings, rule, 10L)
        assertTrue(saved.edits.swaps.isEmpty() && saved.edits.removed.isEmpty() && saved.edits.added.isEmpty())
        assertEquals(4, saved.edits.tierOverrides["pull"]); assertEquals("stretch-frog", saved.edits.stretchPicks["core"])
        assertEquals("pushup-diamond", saved.routine.slots.first { it.id == "push" }.preferredVariationId)
        assertTrue(saved.routine.slots.any { it.id == "add-pushup-archer" } && saved.routine.slots.none { it.id == "core2" })
    }

    @Test fun updateKeepsTheIdAndBumpsTheRevisionSaveAsNewGetsANewId() {
        val a = savedFrom("rt-1", "A", 1, base, edits, settings, rule, 10L)
        val b = updateSaved(a, savedFrom("tmp", "A2", 1, base, PlanEdits(), settings, rule, 20L))
        assertEquals("rt-1", b.id); assertEquals(2, b.revision); assertEquals(2, b.routine.revision); assertEquals("rt-1", b.routine.id); assertEquals(10L, b.createdAtEpochMs)
        val list = upsertSaved(upsertSaved(emptyList(), a), b)
        assertEquals(1, list.size); assertEquals("A2", list.single().name)
        val asNew = upsertSaved(list, savedFrom("rt-2", "B", 1, base, edits, settings, rule, 30L))
        assertEquals(setOf("rt-1", "rt-2"), asNew.map { it.id }.toSet())
        assertEquals(listOf("rt-2"), deleteSaved(asNew, "rt-1").map { it.id })
    }

    @Test fun earlierPlansKeepTheirOwnSnapshotWhenARoutineIsUpdated() {
        val a = savedFrom("rt-1", "A", 1, base, edits, settings, rule, 10L)
        val before = planOf(resolveSaved(a, settingsFor(a, TrainSettings())))
        val updated = updateSaved(a, savedFrom("tmp", "A", 1, base, PlanEdits(), settings, rule, 20L))
        assertEquals("rt-1", before.routineId); assertEquals(1, before.routineRevision)       // a plan stores which revision it was made from
        assertEquals(2, planOf(resolveSaved(updated, settingsFor(updated, TrainSettings()))).routineRevision)
    }

    @Test fun backupsRoundTripAndMergeByRevision() {
        val a = savedFrom("rt-1", "A", 1, base, edits, settings, rule, 10L)
        val payload = BackupPayload(Preferences(), listOf(base), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), savedRoutines = listOf(a))
        val back = (importBackup(exportBackup(payload, "t", 0L)) as ImportResult.Ok).payload
        assertEquals(listOf(a), back.savedRoutines)
        val newer = updateSaved(a, savedFrom("x", "A newer", 1, base, edits, settings, rule, 20L))
        assertEquals("A newer", mergeSaved(listOf(a), listOf(newer)).single().name)
        assertEquals("A newer", mergeSaved(listOf(newer), listOf(a)).single().name)   // an older copy never overwrites
        assertEquals(2, mergeSaved(listOf(a), listOf(savedFrom("rt-9", "Z", 1, base, edits, settings, rule, 5L))).size)
        // a backup from before V18 has none
        val old = exportBackup(payload.copy(savedRoutines = emptyList()), "t", 0L)
        assertTrue((importBackup(old) as ImportResult.Ok).payload.savedRoutines.isEmpty())
    }
}
