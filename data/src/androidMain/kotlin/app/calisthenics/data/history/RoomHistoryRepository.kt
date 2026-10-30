package app.calisthenics.data.history

import io.github.gonbei774.calisthenicsmemory.data.*

private fun PlanSnapshotEntity.record() = PlanRecord(planId, createdAtEpochMs, routineId, routineRevision, catalogVersion, profileId, planJson)
private fun WorkoutSessionEntity.record() = SessionRow(sessionId, planId, startedAtEpochMs, endedAtEpochMs, status)
private fun BlockResultEntity.record() = BlockRow(sessionId, blockId, outcome, actualSeconds, achievedValue)
private fun FeedbackRevisionEntity.record() = FeedbackRow(sessionId, variationId, revision, rating, discomfort, assumedMet, createdAtEpochMs, actualReps, blockId, actualHoldSeconds)
private fun ProgressionEventEntity.record() = EventRow(eventId, variationId, kind, fromTier, toTier, reason, atEpochMs)
private fun PlanRecord.entity() = PlanSnapshotEntity(planId, createdAtEpochMs, routineId, routineRevision, catalogVersion, profileId, planJson)
private fun SessionRow.entity() = WorkoutSessionEntity(sessionId, planId, startedAtEpochMs, endedAtEpochMs, status)
private fun BlockRow.entity() = BlockResultEntity(sessionId = sessionId, blockId = blockId, outcome = outcome, actualSeconds = actualSeconds, achievedValue = achievedValue)
private fun FeedbackRow.entity() = FeedbackRevisionEntity(sessionId = sessionId, variationId = variationId, revision = revision, rating = rating, discomfort = discomfort, assumedMet = assumedMet, createdAtEpochMs = createdAtEpochMs, actualReps = actualReps, blockId = blockId, actualHoldSeconds = actualHoldSeconds)
private fun EventRow.entity() = ProgressionEventEntity(eventId, variationId, kind, fromTier, toTier, reason, atEpochMs)

class RoomHistoryRepository(private val dao: HistoryDao) : HistoryRepository {
    override suspend fun sessions() = dao.sessions().map { it.record() }
    override suspend fun plan(id: String) = dao.plan(id)?.record()
    override suspend fun blockResults(id: String) = dao.blockResults(id).map { it.record() }
    override suspend fun feedbackHistory(id: String, variationId: String) = dao.feedbackHistory(id, variationId).map { it.record() }
    override suspend fun feedbackForSession(id: String) = dao.feedbackForSession(id).map { it.record() }
    override suspend fun latestRevision(id: String, variationId: String) = dao.latestRevision(id, variationId)
    override suspend fun events(variationId: String) = dao.events(variationId).map { it.record() }
    override suspend fun allFeedback() = dao.allFeedback().map { it.record() }
    override suspend fun allEvents() = dao.allEvents().map { it.record() }
    override suspend fun saveFinishedSession(plan: PlanRecord, session: SessionRow, blocks: List<BlockRow>, feedback: List<FeedbackRow>) = dao.saveFinishedSession(plan.entity(), session.entity(), blocks.map { it.entity() }, feedback.map { it.entity() })
    override suspend fun reviseFeedback(sessionId: String, variationId: String, rating: String, discomfort: Boolean, assumedMet: Boolean, now: Long, actualReps: Int?, blockId: String?, actualHoldSeconds: Int?) = dao.reviseFeedback(sessionId, variationId, rating, discomfort, assumedMet, now, actualReps, blockId, actualHoldSeconds)
    override suspend fun reviseRounds(sessionId: String, variationId: String, rounds: List<RoundRevision>, now: Long) = dao.reviseRounds(sessionId, variationId, rounds.map { io.github.gonbei774.calisthenicsmemory.data.RoundRevision(it.blockId, it.reps, it.rating, it.discomfort, it.isHold) }, now)
    override suspend fun restoreMerge(plans: List<PlanRecord>, sessions: List<SessionRow>, blocks: List<BlockRow>, feedback: List<FeedbackRow>, events: List<EventRow>) = dao.restoreMerge(plans.map { it.entity() }, sessions.map { it.entity() }, blocks.map { it.entity() }, feedback.map { it.entity() }, events.map { it.entity() })
}
