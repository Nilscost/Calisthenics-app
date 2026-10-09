// U12: where the History tab reads from. A small interface so the screens can be tested without Room
// (Robolectric's SQLite does not run on the sandbox), and so the late-edit rule lives in one place.
package io.github.gonbei774.calisthenicsmemory.ui.history

import android.content.Context
import app.calisthenics.domain.feedback.Rating
import app.calisthenics.domain.history.BlockRecord
import app.calisthenics.domain.history.SessionRecord
import app.calisthenics.domain.model.BlockType
import app.calisthenics.domain.model.WorkoutPlan
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import kotlinx.serialization.json.Json

data class StoredFeedback(val rating: String, val discomfort: Boolean, val actualReps: Int?)
data class StoredSession(val record: SessionRecord, val plan: WorkoutPlan?, val feedback: Map<String, StoredFeedback>)

interface HistorySource {
    /** Newest first. */
    suspend fun sessions(): List<StoredSession>
    /** A correction is a NEW feedback revision; the earlier one is kept. */
    suspend fun revise(sessionId: String, variationId: String, rating: Rating, discomfort: Boolean, reps: Int?)
    /** V04a: corrects the rounds of one exercise (each round a new revision with its block id); the history stays append-only. */
    suspend fun reviseRounds(sessionId: String, variationId: String, rounds: List<RoundCorrection>) {}
}

/** One corrected round: [reps] = reps, or seconds when [isHold]; [rating] already includes too hard / too easy / below target. */
data class RoundCorrection(val blockId: String, val reps: Int?, val rating: Rating, val discomfort: Boolean, val isHold: Boolean = false)

class RoomHistorySource(private val ctx: Context) : HistorySource {
    private val json = Json { ignoreUnknownKeys = true }
    private fun dao() = AppDatabase.getDatabase(ctx).historyDao()

    override suspend fun sessions(): List<StoredSession> {
        val dao = dao()
        return dao.sessions().map { s ->
            val plan = dao.plan(s.planId)?.let { runCatching { json.decodeFromString(WorkoutPlan.serializer(), it.planJson) }.getOrNull() }
            val byBlock = plan?.blocks?.associateBy { it.id }.orEmpty()
            val blocks = dao.blockResults(s.sessionId).map { r ->
                val b = byBlock[r.blockId]
                BlockRecord(r.blockId, b?.variationId, (b?.type ?: BlockType.WORK).name, r.outcome, r.actualSeconds, r.achievedValue)
            }
            val fb = plan?.blocks?.mapNotNull { it.variationId }?.distinct().orEmpty().mapNotNull { v ->
                dao.feedbackHistory(s.sessionId, v).lastOrNull { it.blockId == null }?.let { v to StoredFeedback(it.rating, it.discomfort, it.actualReps) }
            }.toMap()
            StoredSession(SessionRecord(s.sessionId, s.planId, s.startedAtEpochMs, s.endedAtEpochMs, s.status, blocks), plan, fb)
        }
    }

    override suspend fun revise(sessionId: String, variationId: String, rating: Rating, discomfort: Boolean, reps: Int?) {
        dao().reviseFeedback(sessionId, variationId, rating.name, discomfort, false, System.currentTimeMillis(), reps)
    }

    override suspend fun reviseRounds(sessionId: String, variationId: String, rounds: List<RoundCorrection>) {
        dao().reviseRounds(sessionId, variationId, rounds.map { io.github.gonbei774.calisthenicsmemory.data.RoundRevision(it.blockId, it.reps, it.rating.name, it.discomfort, it.isHold) }, System.currentTimeMillis())
    }
}
