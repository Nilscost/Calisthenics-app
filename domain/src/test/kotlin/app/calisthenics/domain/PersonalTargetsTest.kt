package app.calisthenics.domain

import app.calisthenics.domain.model.*
import app.calisthenics.domain.model.Target
import app.calisthenics.domain.feedback.*
import app.calisthenics.domain.progression.*
import app.calisthenics.domain.backup.*
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.routine.*
import org.junit.Assert.*
import org.junit.Test

class PersonalTargetsTest {
    private val c = Fx.catalog()
    private val id = "pushup-knee"
    private fun plan(eligible: Boolean = true, revision: Int = 0, hold: Boolean = false): WorkoutPlan {
        val blocks = (1..3).map { n -> TimelineBlock("w$n", BlockType.WORK, 60, n, "push", id,
            target = Target(if (hold) TargetType.HOLD_SECONDS else TargetType.REPS, 40), prescriptionTier = 5,
            freeTarget = true, levelFiveEligible = eligible, targetRevision = revision) }
        return WorkoutPlan("p", "usual", 1, 1, 0, "home", 180, 180, setOf(StrengthFocus.FULL_BODY), false,
            null, 3, emptyList(), emptyList(), false, false, blocks)
    }
    private fun evidence(p: WorkoutPlan, day: Int = 1, actual: Int = 40, skip: String? = null): SessionEvidence {
        val logs = p.blocks.associate { it.id to Feedback(actualReps = actual, actualHoldSeconds = actual) }
        return deriveEvidence("s$day", day, p, resolveFeedback(p,
            p.blocks.associate { it.id to if (it.id == skip) Execution.SKIPPED else Execution.COMPLETED }, logs),
            { "pushup" }, { _, _ -> 2 }).single()
    }
    @Test fun savedFortyRepAndHoldTargetsSurviveBackupAndRaisedPersonalBoundary() {
        for (hold in listOf(false, true)) {
            val catalog = if (!hold) c else c.copy(
                variations = c.variations.map { if (it.id == id) it.copy(kind = Kind.HOLD) else it },
                policies = c.policies.map { if (it.variationId == id) it.copy(tiers = it.tiers.map { t -> t.copy(target = Target(TargetType.HOLD_SECONDS, t.target.value)) }) else it })
            val routine = Routine("single", 1, "Single", listOf(RoutineSlot("push", Pattern.PUSH_HORIZONTAL, Area.UPPER_BODY, id)))
            val settings = TrainSettings(rounds = 3, flow = true, preparationSeconds = 8, exercisesPerRound = 1)
            val edits = PlanEdits(tierOverrides = mapOf("push" to 5), levelFiveTargets = mapOf("push" to 40))
            val saved = savedFrom("forty", "Forty", 1, routine, edits, settings, ProgressionRule(), 1)
            val payload = BackupPayload(Preferences(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), savedRoutines = listOf(saved))
            val restored = (importBackup(exportBackup(payload, "test", 1)) as ImportResult.Ok).payload.savedRoutines.single()
            assertEquals(40, restored.edits.levelFiveTargets["push"])
            assertEquals(8, restored.preparationSeconds)
            val progress = ProgressionEngine(catalog).replay(emptyList(), listOf(UserAction.SelfAssessment(0, id, 5)))
            fun rebuilt(cat: Catalog): WorkoutPlan {
                val resolved = resolveSaved(restored, settingsFor(restored, TrainSettings()))
                return (buildTrainPlan(cat, resolved.routine, progress, EquipmentProfile("home", "Home", emptyList(), emptySet()), resolved.settings, edits = resolved.edits) as PlanResult.Ready).plan
            }
            val plan = rebuilt(catalog)
            val work = plan.blocks.filter { it.type == BlockType.WORK }
            assertEquals(3, work.size)
            assertTrue(work.all { it.target!!.value == 40 && it.prescriptionTier == 5 && it.levelFiveEligible && !it.freeTarget })
            if (hold) assertTrue(work.all { it.durationSeconds >= 40 })
            val outcomes = resolveFeedback(plan, work.associate { it.id to Execution.COMPLETED },
                work.associate { it.id to if (hold) Feedback(actualHoldSeconds = 40) else Feedback(actualReps = 40) })
            assertTrue(deriveEvidence("saved", 1, plan, outcomes, { "pushup" }, { _, _ -> 2 }).single().qualifying)
            val raised = rebuilt(catalog.copy(personalTargets = mapOf(id to PersonalLevelTargets(1, listOf(10, 20, 30, 40, 50)))))
            assertTrue(raised.blocks.filter { it.type == BlockType.WORK }.all { it.target!!.value == 50 && it.targetRevision == 1 })
        }
    }

    @Test fun overrideIsPerExerciseAndPreservesCompoundProperties() {
        val v = c.variation(id)!!
        val sibling = v.copy(id = "sibling")
        val authored = c.copy(variations = c.variations + sibling)
        val policy = authored.policyFor(v)!!
        val values = policy.tiers.map { it.target.value + 20 }
        val effective = authored.copy(personalTargets = mapOf(id to PersonalLevelTargets(1, values)))
        assertEquals(values, effective.policyFor(v)!!.tiers.map { it.target.value })
        assertEquals(policy.tiers, effective.policyFor(sibling)!!.tiers)
        assertEquals(policy.tiers.map { it.minQualifyingBlocks }, effective.policyFor(v)!!.tiers.map { it.minQualifyingBlocks })
        assertEquals(policy.tierProfile, effective.policyFor(v)!!.tierProfile)
    }
    @Test fun positiveNondecreasingFiveValuesAllowEqualCompoundTargets() {
        PersonalLevelTargets(1, listOf(3, 4, 5, 5, 5))
        for (values in listOf(listOf(0, 1, 2, 3, 4), listOf(5, 4, 3, 2, 1), listOf(1))) {
            assertTrue(runCatching { PersonalLevelTargets(1, values) }.isFailure)
        }
    }
    @Test fun onlyExplicitNewExtensionCountsAndActualPerformanceIsRetained() {
        val p = plan()
        assertEquals(5, evidence(p).prescribedTier)
        assertTrue(evidence(p).levelFiveEligible)
        assertTrue(evidence(p).qualifying)
        assertEquals(40, resolveFeedback(p, p.blocks.associate { it.id to Execution.COMPLETED },
            p.blocks.associate { it.id to Feedback(actualReps = 40) }).first().actualReps)
        assertFalse(evidence(plan(false)).qualifying)
        assertTrue(evidence(plan(hold = true)).qualifying)
    }
    @Test fun skippedIncompleteMixedAndCorrectedResultsCannotReceiveCredit() {
        assertFalse(evidence(plan(), skip = "w2").qualifying)
        assertFalse(evidence(plan(), actual = 12).qualifying)
        val mixed = plan().let { it.copy(blocks = it.blocks.mapIndexed { i, b -> if (i == 0) b.copy(levelFiveEligible = false) else b }) }
        assertFalse(evidence(mixed).qualifying)
        val p = plan()
        val outcomes = resolveFeedback(p, mapOf("w1" to Execution.COMPLETED, "w2" to Execution.COMPLETED),
            p.blocks.associate { it.id to Feedback(actualReps = 40) })
        assertFalse(deriveEvidence("s", 1, p, outcomes, { "pushup" }, { _, _ -> 2 }).single().qualifying)
    }
    @Test fun flowProgramsRequireActualLogsRatherThanTimerOnlyCompletion() {
        val p = plan().copy(fixedProgramId = "program", flow = true)
        assertTrue(evidence(p).qualifying)
        val outcomes = resolveFeedback(p, p.blocks.associate { it.id to Execution.COMPLETED })
        assertTrue(deriveEvidence("s", 1, p, outcomes, { "pushup" }, { _, _ -> 2 }).isEmpty())
    }
    @Test fun changedDefinitionPreservesEarnedHistoryAndClearsPendingStreak() {
        val old = evidence(plan(), day = 1).copy(prescribedTier = 2, levelFiveEligible = false)
        val edited = c.copy(personalTargets = mapOf(id to PersonalLevelTargets(1, listOf(20, 25, 30, 35, 40))))
        val start = listOf(UserAction.SelfAssessment(0, id, 2))
        val pending = ProgressionEngine(edited).replay(listOf(old), start).variations.getValue(id)
        assertTrue(pending.streakDays.isEmpty())
        val oldEarned = listOf(1, 2, 3).map { old.copy(sessionId = "s$it", day = it) }
        assertTrue(2 in ProgressionEngine(edited).replay(oldEarned, start).variations.getValue(id).achievedTiers)
        val fresh = old.copy(sessionId = "fresh", day = 4, targetRevision = 1)
        val p = ProgressionEngine(edited).replay(listOf(old, old.copy(sessionId = "s2", day = 2), fresh), start).variations.getValue(id)
        assertEquals(2, p.tier)
        assertEquals(listOf(4), p.streakDays)
    }
    @Test fun loggedAboveLevelFiveFromLowerTrackedTierUsesNormalPacingAndNeverLevelSix() {
        val start = listOf(UserAction.SelfAssessment(0, id, 2))
        val first = ProgressionEngine(c).replay(listOf(evidence(plan(), 1)), start).variations.getValue(id)
        assertEquals(5, first.tier)
        assertTrue(first.achievedTiers.isEmpty())
        val done = ProgressionEngine(c).replay(listOf(1, 2, 3).map { evidence(plan(), it) }, start).variations.getValue(id)
        assertEquals(5, done.tier)
        assertTrue(5 in done.achievedTiers)
        assertEquals(5, done.earnedStars())
    }
    @Test fun rowMinimumCannotInventMissingFlowSetLogs() {
        val p = plan().copy(fixedProgramId = "program", flow = true)
        val outcomes = resolveFeedback(p, p.blocks.associate { it.id to Execution.COMPLETED },
            rowFeedback = mapOf(id to Feedback(actualReps = 40)))
        assertTrue(deriveEvidence("partial-logs", 1, p, outcomes, { "pushup" }, { _, _ -> 2 }, loggedBlockIds = setOf("w1")).isEmpty())
    }
    @Test fun aboveThresholdEvidenceRespectsTheAutomaticProgressionToggle() {
        val p = ProgressionEngine(c).replay((1..3).map { evidence(plan(), it) }, listOf(UserAction.SelfAssessment(0, id, 2)), autoProgression = false).variations.getValue(id)
        assertEquals(2, p.tier)
        assertTrue(p.achievedTiers.isEmpty())
    }
    @Test fun aboveThresholdAlternativeEarnsItsOwnFiveStarsWithoutPromotingTheMain() {
        val withAlternative = c.copy(exerciseGroups = listOf(ExerciseGroup("push", "pushup-std", listOf(id))))
        val result = ProgressionEngine(withAlternative).replay((1..3).map { evidence(plan(), it) }, listOf(UserAction.SelfAssessment(0, id, 2)))
        assertEquals(5, result.variations.getValue(id).earnedStars())
        assertTrue(result.variations["pushup-std"]?.achievedTiers.orEmpty().isEmpty())
        assertFalse(result.events.any { it.message.contains("Moving on to") })
    }
    @Test fun wrongUnitAndMixedPrescribedLevelsCannotInventCredit() {
        val p = plan().copy(fixedProgramId = "program", flow = true)
        val wrong = resolveFeedback(p, p.blocks.associate { it.id to Execution.COMPLETED }, p.blocks.associate { it.id to Feedback(actualHoldSeconds = 40) })
        assertTrue(deriveEvidence("wrong", 1, p, wrong, { "pushup" }, { _, _ -> 2 }).isEmpty())
        val mixed = p.copy(blocks = p.blocks.mapIndexed { n, b -> if (n == 0) b.copy(prescriptionTier = 4) else b })
        assertFalse(evidence(mixed).qualifying)
    }
    @Test fun postEditStaleRoutineDoesNotAdvanceButOlderHistoryDoes() {
        val def = PersonalLevelTargets(1, listOf(20, 25, 30, 35, 40), 1000)
        val engine = ProgressionEngine(c.copy(personalTargets = mapOf(id to def)))
        val old = (1..3).map { evidence(plan(), it).copy(recordedAtEpochMs = 500) }
        val stale = (4..6).map { evidence(plan(), it).copy(recordedAtEpochMs = 1500) }
        val start = listOf(UserAction.SelfAssessment(0, id, 2))
        assertFalse(5 in engine.replay(stale, start).variations.getValue(id).achievedTiers)
        assertTrue(5 in engine.replay(old, start).variations.getValue(id).achievedTiers)
    }
    @Test fun backupsCarryDefinitionsAndLegacyPayloadHasNone() {
        val payload = BackupPayload(Preferences(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(),
            personalTargets = mapOf(id to PersonalLevelTargets(4, listOf(10, 20, 30, 40, 50))))
        assertEquals(payload, (importBackup(exportBackup(payload, "test", 0)) as ImportResult.Ok).payload)
    }
}
