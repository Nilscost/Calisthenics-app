// Backup payload building and restore-merge, shared by the Backup screen and the debug-only seed receiver (V00b).
package io.github.gonbei774.calisthenicsmemory.ui.screens

import android.content.Context
import app.calisthenics.domain.backup.*
import app.calisthenics.domain.history.BlockRecord
import app.calisthenics.domain.history.SessionRecord
import io.github.gonbei774.calisthenicsmemory.data.*

object BackupApply {
    suspend fun buildPayload(ctx: Context): BackupPayload {
        val dao = AppDatabase.getDatabase(ctx).historyDao()
        val sessions = dao.sessions()
        return BackupPayload(
            preferences = PrefsStore.load(ctx), routines = listOf(RoutineStore.load(ctx)),
            plans = sessions.mapNotNull { dao.plan(it.planId) }.distinctBy { it.planId }.map { PlanRecord(it.planId, it.planJson) },
            sessions = sessions.map { s -> SessionRecord(s.sessionId, s.planId, s.startedAtEpochMs, s.endedAtEpochMs, s.status,
                dao.blockResults(s.sessionId).map { BlockRecord(it.blockId, null, "WORK", it.outcome, it.actualSeconds, it.achievedValue) }) },
            feedback = dao.allFeedback().map { FeedbackRecord(it.sessionId, it.variationId, it.revision, it.rating, it.discomfort, it.assumedMet, it.createdAtEpochMs, it.actualReps) },
            events = dao.allEvents().map { ProgressionEventRecord(it.eventId, it.variationId, it.kind, it.fromTier, it.toTier, it.reason, it.atEpochMs) }, profiles = ProfileStore.load(ctx))
    }

    /** Merges history (never deletes or overwrites), replaces settings and the usual plan. Returns the number of new sessions. */
    suspend fun restore(ctx: Context, p: BackupPayload): Int {
        val dao = AppDatabase.getDatabase(ctx).historyDao()
        val pj = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        val plans = p.plans.map { pr ->
            val wp = pj.decodeFromString(app.calisthenics.domain.model.WorkoutPlan.serializer(), pr.planJson)
            PlanSnapshotEntity(pr.planId, wp.createdAtEpochMs, wp.routineId, wp.routineRevision, wp.catalogVersion, wp.profileId, pr.planJson)
        }
        val sessions = p.sessions.map { WorkoutSessionEntity(it.sessionId, it.planId, it.startedAtEpochMs, it.endedAtEpochMs, it.status) }
        val blocks = p.sessions.flatMap { s -> s.blocks.map { BlockResultEntity(sessionId = s.sessionId, blockId = it.blockId, outcome = it.outcome, actualSeconds = it.actualSeconds, achievedValue = it.achievedValue) } }
        val fb = p.feedback.map { FeedbackRevisionEntity(sessionId = it.sessionId, variationId = it.variationId, revision = it.revision, rating = it.rating, discomfort = it.discomfort, assumedMet = it.assumedMet, createdAtEpochMs = it.createdAtEpochMs, actualReps = it.actualReps) }
        val ev = p.events.map { ProgressionEventEntity(it.eventId, it.variationId, it.kind, it.fromTier, it.toTier, it.reason, it.atEpochMs) }
        val added = dao.restoreMerge(plans, sessions, blocks, fb, ev)
        PrefsStore.save(ctx, p.preferences); p.routines.firstOrNull()?.let { RoutineStore.save(ctx, it) }
        if (p.profiles.isNotEmpty()) ProfileStore.save(ctx, p.profiles)
        return added
    }
}
