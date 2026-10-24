package app.calisthenics.domain

import app.calisthenics.domain.backup.*
import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.equipment.SeedProfiles
import app.calisthenics.domain.model.*
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.progression.ProgressSnapshot
import app.calisthenics.domain.routine.StarterRoutine
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** Serialized policy boundaries survive backup and recovery without rewriting old plans. */
class PhoneFeedbackCompatibilityTest {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
    private val catalog = parseCatalog(File(System.getProperty("repo.root"), "content/starter/catalog.json").readText())
    private val empty = ProgressSnapshot(emptyMap(), emptyMap(), emptyMap(), emptyList())
    private fun plan() = (buildTrainPlan(catalog, StarterRoutine.routine, empty, SeedProfiles.home,
        TrainSettings(rounds = 2, exercisesPerRound = 3)) as PlanResult.Ready).plan

    @Test fun newPlansOptIntoDemonstratedEvidenceWhileLegacyJsonRemainsLegacy() {
        val fresh = plan()
        assertEquals(1, fresh.progressionEvidenceVersion)
        assertTrue(fresh.progressionPolicies.isNotEmpty())
        val encoded = json.encodeToString(WorkoutPlan.serializer(), fresh)
        val legacyJson = encoded.replace(",\"progressionEvidenceVersion\":1", "")
        assertEquals(0, json.decodeFromString(WorkoutPlan.serializer(), legacyJson).progressionEvidenceVersion)
        assertEquals(fresh, json.decodeFromString(WorkoutPlan.serializer(), encoded))
    }

    @Test fun backupRetainsMixedHistoricalAndNewEvidenceSemanticsAndActualPlanBytes() {
        val fresh = plan()
        val old = fresh.copy(id = "historical", progressionEvidenceVersion = 0, progressionPolicies = emptyMap())
        val records = listOf(old, fresh).map { PlanRecord(it.id, json.encodeToString(WorkoutPlan.serializer(), it)) }
        val payload = BackupPayload(Preferences(), emptyList(), records, emptyList(), emptyList(), emptyList())
        val restored = (importBackup(exportBackup(payload, "0.11.4-r8", 123L)) as ImportResult.Ok).payload
        assertEquals(records, restored.plans)
        assertEquals(listOf(0, 1), restored.plans.map { json.decodeFromString(WorkoutPlan.serializer(), it.planJson).progressionEvidenceVersion })
    }

    @Test fun freshEditableProgramsUseNewEvidenceWithoutInventingAuthoredLevels() {
        val program = catalog.fixedPrograms.first { it.listed }
        val p = (buildFixedProgramPlan(catalog, program, SeedProfiles.home) as FixedProgramPlanResult.Ready).plan
        assertEquals(1, p.progressionEvidenceVersion)
        assertTrue(p.blocks.filter { it.type == BlockType.WORK }.any { it.prescriptionTier == null })
    }
    @Test fun savedRoutineKeepsDormantRestMapAndAuthoredRuleThroughBackup() {
        val rule = ProgressionRule(kind = RuleKind.REP_RANGE)
        val saved = app.calisthenics.domain.routine.savedFrom("saved", "Saved", 1, StarterRoutine.routine,
            PlanEdits(), TrainSettings(betweenSetMode = BetweenSetMode.FLOW), rule, 1L)
            .copy(restDurations = mapOf("push" to 45))
        val payload = BackupPayload(Preferences(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), savedRoutines = listOf(saved))
        val restored = (importBackup(exportBackup(payload, "0.11.4-r8", 1L)) as ImportResult.Ok).payload
        assertEquals(saved, restored.savedRoutines.single())
        assertEquals(RuleKind.REP_RANGE, restored.savedRoutines.single().rule.kind)
        assertEquals(mapOf("push" to 45), restored.savedRoutines.single().restDurations)
    }

    @Test fun savedAndEditableProgramModesMatchActualBlocksInEverySetFormat() {
        val range = ProgressionRule(kind = RuleKind.REP_RANGE)
        for (format in WorkoutFormat.entries) for (mode in BetweenSetMode.entries) {
            val settings = TrainSettings(rounds = 3, exercisesPerRound = 3, format = format,
                betweenSetMode = mode, preparationSeconds = 8)
            val saved = app.calisthenics.domain.routine.savedFrom("matrix", "Matrix", 1,
                StarterRoutine.routine, PlanEdits(), settings, range, 1L)
            val resolved = app.calisthenics.domain.routine.resolveSaved(saved,
                app.calisthenics.domain.routine.settingsFor(saved, settings))
            val ordinary = (buildTrainPlan(catalog, resolved.routine, empty, SeedProfiles.home,
                resolved.settings, edits = resolved.edits) as PlanResult.Ready).plan
            assertEquals(range, ordinary.rule)
            val program = (buildProgramPlan(catalog, ProgramContent("matrix", "Matrix", listOf(
                FixedProgramItem("pushup-knee", reps = 5), FixedProgramItem("squat-air", reps = 5))),
                SeedProfiles.home, mode == BetweenSetMode.FLOW, sets = 2, format = format,
                betweenSetMode = mode, preparationSeconds = 8) as FixedProgramPlanResult.Ready).plan
            for (p in listOf(ordinary, program)) {
                assertEquals(mode, p.betweenSetMode)
                assertEquals(format, p.format)
                assertEquals(p.blocks.sumOf { it.durationSeconds }, p.plannedDurationSeconds)
                when (mode) {
                    BetweenSetMode.FLOW -> assertFalse(p.blocks.any { it.type in setOf(BlockType.PASSIVE_RECOVERY, BlockType.STRETCH) })
                    BetweenSetMode.REST -> {
                        assertTrue(p.blocks.any { it.type == BlockType.PASSIVE_RECOVERY })
                        assertFalse(p.blocks.any { it.type == BlockType.STRETCH })
                    }
                    BetweenSetMode.STRETCH -> assertTrue(p.blocks.any { it.type == BlockType.STRETCH })
                }
            }
        }
    }

}
