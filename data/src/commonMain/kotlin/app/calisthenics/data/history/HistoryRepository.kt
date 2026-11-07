package app.calisthenics.data.history

import kotlinx.serialization.Serializable

@Serializable data class PlanRecord(val planId: String, val createdAtEpochMs: Long, val routineId: String, val routineRevision: Int, val catalogVersion: Int, val profileId: String, val planJson: String)
@Serializable data class SessionRow(val sessionId: String, val planId: String, val startedAtEpochMs: Long, val endedAtEpochMs: Long?, val status: String)
@Serializable data class BlockRow(val sessionId: String, val blockId: String, val outcome: String, val actualSeconds: Int, val achievedValue: Int?)
@Serializable data class FeedbackRow(val sessionId: String, val variationId: String, val revision: Int, val rating: String, val discomfort: Boolean, val assumedMet: Boolean, val createdAtEpochMs: Long, val actualReps: Int? = null, val blockId: String? = null, val actualHoldSeconds: Int? = null)
@Serializable data class EventRow(val eventId: String, val variationId: String, val kind: String, val fromTier: Int, val toTier: Int, val reason: String, val atEpochMs: Long)
@Serializable data class RoundRevision(val blockId: String, val reps: Int?, val rating: String, val discomfort: Boolean, val isHold: Boolean = false)
@Serializable data class HistoryState(val plans: List<PlanRecord> = emptyList(), val sessions: List<SessionRow> = emptyList(), val blocks: List<BlockRow> = emptyList(), val feedback: List<FeedbackRow> = emptyList(), val events: List<EventRow> = emptyList())

interface HistoryRepository {
    suspend fun sessions(): List<SessionRow>
    suspend fun plan(id: String): PlanRecord?
    suspend fun blockResults(id: String): List<BlockRow>
    suspend fun feedbackHistory(id: String, variationId: String): List<FeedbackRow>
    suspend fun feedbackForSession(id: String): List<FeedbackRow>
    suspend fun latestRevision(id: String, variationId: String): Int
    suspend fun events(variationId: String): List<EventRow>
    suspend fun allFeedback(): List<FeedbackRow>
    suspend fun allEvents(): List<EventRow>
    suspend fun saveFinishedSession(plan: PlanRecord, session: SessionRow, blocks: List<BlockRow>, feedback: List<FeedbackRow> = emptyList())
    suspend fun reviseFeedback(sessionId: String, variationId: String, rating: String, discomfort: Boolean, assumedMet: Boolean, now: Long, actualReps: Int? = null, blockId: String? = null, actualHoldSeconds: Int? = null): Int
    suspend fun reviseRounds(sessionId: String, variationId: String, rounds: List<RoundRevision>, now: Long): Int
    suspend fun restoreMerge(plans: List<PlanRecord>, sessions: List<SessionRow>, blocks: List<BlockRow>, feedback: List<FeedbackRow>, events: List<EventRow>): Int
}
