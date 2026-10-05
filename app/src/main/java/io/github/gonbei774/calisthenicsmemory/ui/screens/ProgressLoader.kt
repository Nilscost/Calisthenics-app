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
            val vids = plan.blocks.mapNotNull { it.variationId }.distinct()
            val rows = vids.mapNotNull { v ->
                dao.feedbackHistory(s.sessionId, v).lastOrNull()?.let { f ->
                    v to Feedback(rating = runCatching { Rating.valueOf(f.rating) }.getOrNull(), discomfort = f.discomfort, revision = f.revision)
                }
            }.toMap()
            val day = Instant.ofEpochMilli(s.startedAtEpochMs).atZone(zone).toLocalDate().toEpochDay().toInt()
            evidence += deriveEvidence(s.sessionId, day, plan, resolveFeedback(plan, exec, emptyMap(), rows),
                { catalog.variation(it)?.familyId ?: it },
                { v, tier -> catalog.policyForVariation(v)?.tiers?.firstOrNull { it.index == tier }?.minQualifyingBlocks ?: 2 })
        }
        val actions = LevelStore.load(ctx).map { (v, t) -> UserAction.SelfAssessment(0, v, t) }
        return ProgressionEngine(catalog).replay(evidence, actions)
    }
}
