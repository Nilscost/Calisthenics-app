// Feeds real history (Room) + self-assessed levels into the progression engine. Progress is recomputed, never stored.
package io.github.gonbei774.calisthenicsmemory.ui.screens

import android.content.Context
import app.calisthenics.domain.feedback.*
import app.calisthenics.domain.model.Catalog
import app.calisthenics.domain.model.WorkoutPlan
import app.calisthenics.domain.progression.*
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.ZoneId

object ProgressLoader {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun load(ctx: Context, catalog: Catalog): ProgressSnapshot {
        val dao = AppDatabase.getDatabase(ctx).historyDao()
        val zone = ZoneId.systemDefault()
        val evidence = mutableListOf<SessionEvidence>()
        for (s in dao.sessions()) {
            if (s.status != "COMPLETED" && s.status != "PARTIAL_FINISHED") continue
            val plan = dao.plan(s.planId)?.let { try { json.decodeFromString(WorkoutPlan.serializer(), it.planJson) } catch (_: Exception) { null } } ?: continue
            val exec = dao.blockResults(s.sessionId).associate { r ->
                r.blockId to when (r.outcome) { "MET" -> Execution.COMPLETED; "PARTIAL" -> Execution.PARTIAL; "SKIPPED" -> Execution.SKIPPED; else -> Execution.NOT_STARTED }
            }
            // V04a: per-round corrections (feedback revisions with a blockId) are applied by the domain; the original per-round numbers are the block results.
            val achieved = dao.blockResults(s.sessionId).filter { it.outcome == "MET" || it.outcome == "PARTIAL" }.associate { it.blockId to it.achievedValue }
            val rows = effectiveFeedback(plan, achieved, dao.feedbackForSession(s.sessionId).map { f ->
                FeedbackRow(f.variationId, f.blockId, f.revision, runCatching { Rating.valueOf(f.rating) }.getOrNull(), f.discomfort, f.actualReps, f.actualHoldSeconds)
            })
            val day = Instant.ofEpochMilli(s.startedAtEpochMs).atZone(zone).toLocalDate().toEpochDay().toInt()
            evidence += deriveEvidence(s.sessionId, day, plan, resolveFeedback(plan, exec, emptyMap(), rows),
                { catalog.variation(it)?.familyId ?: it },
                { v, tier -> catalog.policyForVariation(v)?.tiers?.firstOrNull { it.index == tier }?.minQualifyingBlocks ?: 2 })
        }
        val actions = LevelStore.load(ctx).map { (v, t) -> UserAction.SelfAssessment(0, v, t) }
        return ProgressionEngine(catalog).replay(evidence, actions, PrefsStore.load(ctx).autoProgression)
    }
}
