package io.github.gonbei774.calisthenicsmemory.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class HistoryDao {
    /** Plans are immutable: a second insert of the same id is ignored, never overwritten. */
    @Insert(onConflict = OnConflictStrategy.IGNORE) abstract suspend fun insertPlan(p: PlanSnapshotEntity): Long
    @Insert(onConflict = OnConflictStrategy.ABORT) abstract suspend fun insertSession(s: WorkoutSessionEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT) abstract suspend fun insertBlockResults(r: List<BlockResultEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) abstract suspend fun insertFeedback(f: FeedbackRevisionEntity)
    @Insert(onConflict = OnConflictStrategy.IGNORE) abstract suspend fun insertEvent(e: ProgressionEventEntity): Long

    @Query("UPDATE workout_sessions SET status = :status, endedAtEpochMs = :endedAt WHERE sessionId = :id")
    abstract suspend fun finishSession(id: String, status: String, endedAt: Long)

    @Query("SELECT * FROM workout_sessions ORDER BY startedAtEpochMs DESC")
    abstract suspend fun sessions(): List<WorkoutSessionEntity>

    @Query("SELECT * FROM plan_snapshots WHERE planId = :id")
    abstract suspend fun plan(id: String): PlanSnapshotEntity?

    @Query("SELECT * FROM block_results WHERE sessionId = :id")
    abstract suspend fun blockResults(id: String): List<BlockResultEntity>

    @Query("SELECT * FROM feedback_revisions WHERE sessionId = :id AND variationId = :vid ORDER BY revision")
    abstract suspend fun feedbackHistory(id: String, vid: String): List<FeedbackRevisionEntity>

    @Query("SELECT COALESCE(MAX(revision), 0) FROM feedback_revisions WHERE sessionId = :id AND variationId = :vid")
    abstract suspend fun latestRevision(id: String, vid: String): Int

    @Query("SELECT * FROM progression_events WHERE variationId = :vid ORDER BY atEpochMs")
    abstract suspend fun events(vid: String): List<ProgressionEventEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE) abstract suspend fun insertSessionIfNew(s: WorkoutSessionEntity): Long
    @Insert(onConflict = OnConflictStrategy.IGNORE) abstract suspend fun insertBlockResultsIfNew(r: List<BlockResultEntity>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) abstract suspend fun insertFeedbackIfNew(f: FeedbackRevisionEntity): Long
    @Query("SELECT * FROM feedback_revisions ORDER BY sessionId, variationId, revision") abstract suspend fun allFeedback(): List<FeedbackRevisionEntity>
    @Query("SELECT * FROM progression_events ORDER BY atEpochMs") abstract suspend fun allEvents(): List<ProgressionEventEntity>

    /** Merge restore: nothing existing is overwritten or deleted. Returns the number of sessions that were new. One transaction. */
    @Transaction
    open suspend fun restoreMerge(plans: List<PlanSnapshotEntity>, sessions: List<WorkoutSessionEntity>, blocks: List<BlockResultEntity>,
                                  feedback: List<FeedbackRevisionEntity>, events: List<ProgressionEventEntity>): Int {
        plans.forEach { insertPlan(it) }
        var added = 0
        val fresh = HashSet<String>()
        sessions.forEach { if (insertSessionIfNew(it) != -1L) { added++; fresh += it.sessionId } }
        insertBlockResultsIfNew(blocks.filter { it.sessionId in fresh })
        feedback.filter { it.sessionId in fresh }.forEach { insertFeedbackIfNew(it) }
        events.forEach { insertEvent(it) }
        return added
    }

    /** All-or-nothing: plan + session + block results land together or not at all. */
    @Transaction
    open suspend fun saveFinishedSession(
        plan: PlanSnapshotEntity, session: WorkoutSessionEntity, blocks: List<BlockResultEntity>,
    ) {
        insertPlan(plan)
        insertSession(session)
        insertBlockResults(blocks)
    }

    /** Editing old feedback adds a NEW revision; the earlier one is kept. */
    @Transaction
    open suspend fun reviseFeedback(sessionId: String, variationId: String, rating: String, discomfort: Boolean, assumedMet: Boolean, now: Long, actualReps: Int? = null): Int {
        val next = latestRevision(sessionId, variationId) + 1
        insertFeedback(FeedbackRevisionEntity(sessionId = sessionId, variationId = variationId, revision = next,
            rating = rating, discomfort = discomfort, assumedMet = assumedMet, createdAtEpochMs = now, actualReps = actualReps))
        return next
    }
}
