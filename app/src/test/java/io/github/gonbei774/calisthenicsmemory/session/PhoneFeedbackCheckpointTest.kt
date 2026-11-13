package io.github.gonbei774.calisthenicsmemory.session

import app.calisthenics.domain.model.*
import app.calisthenics.domain.session.*
import org.junit.Assert.*
import org.junit.Test

class PhoneFeedbackCheckpointTest {
    @Test fun recoveryRetainsEvidenceBoundaryTargetsModeAndActualLogsWithoutUpgradingOldSessions() {
        for (version in listOf(0, 1)) {
            val plan = WorkoutPlan(id = "p", routineId = "r", routineRevision = 1, catalogVersion = 1,
                createdAtEpochMs = 0, profileId = "home", requestedDurationSeconds = 30,
                plannedDurationSeconds = 30, focus = emptySet(), stretchOn = false, rounds = 1,
                changesExplained = emptyList(), warnings = emptyList(), needsAcceptance = false,
                usesDraftContent = false, blocks = listOf(TimelineBlock("work", BlockType.WORK, 20,
                    variationId = "push", target = Target(TargetType.REPS, 40), prescriptionTier = 5,
                    levelFiveEligible = true, targetRevision = 3), TimelineBlock("rest", BlockType.PASSIVE_RECOVERY, 10)),
                betweenSetMode = BetweenSetMode.REST, preparationSeconds = 8, progressionEvidenceVersion = version)
            var state = reduce(newSession("s", plan), SessionEvent.Start(0)).state
            state = reduce(state, SessionEvent.Tick(5_000)).state
            state = reduce(state, SessionEvent.LogBlock("work", BlockLog(reps = 43))).state
            val checkpoint = CheckpointCodec.decode(CheckpointCodec.encode(state, 0, 5_000, 5_000))!!
            val restored = reduce(CheckpointCodec.toState(checkpoint), SessionEvent.ProcessRecovered).state
            assertEquals(plan, restored.plan)
            assertEquals(version, restored.plan.progressionEvidenceVersion)
            assertEquals(BlockLog(reps = 43), restored.logged["work"])
            assertEquals(Phase.RECOVERY_REQUIRED, restored.phase)
            assertEquals(15_000L, restored.remainingMs)
        }
    }
}
