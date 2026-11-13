// T19 (domain part): history records and weekly summary. Pure Kotlin, no clock: callers pass a zone.
package app.calisthenics.domain.history

import kotlinx.serialization.Serializable
import kotlinx.datetime.*
import kotlin.time.Instant

@Serializable
data class BlockRecord(
    val blockId: String,
    val variationId: String?,
    /** WORK, STRETCH, ... as in BlockType */
    val type: String,
    /** MET, PARTIAL, SKIPPED, REPLACED */
    val outcome: String,
    val actualSeconds: Int,
    /** Reps or seconds achieved; added after format 1 shipped, so it defaults to null for old files. */
    val achievedValue: Int? = null,
)

@Serializable
data class SessionRecord(
    val sessionId: String,
    val planId: String,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long?,
    /** COMPLETED, PARTIAL_FINISHED, RUNNING, RECOVERY_REQUIRED */
    val status: String,
    val blocks: List<BlockRecord>,
)

data class WeekSummary(
    val weekStart: LocalDate,
    val sessions: Int,
    val completed: Int,
    val partial: Int,
    val workSeconds: Int,
    val stretchSeconds: Int,
    val skippedBlocks: Int,
    val variationIds: Set<String>,
)

/** Weeks start on Monday in the given zone. Unfinished sessions (RUNNING/RECOVERY_REQUIRED) are not counted. */
fun weeklySummaries(sessions: List<SessionRecord>, zone: TimeZone): List<WeekSummary> =
    sessions.filter { it.status == "COMPLETED" || it.status == "PARTIAL_FINISHED" }
        .groupBy { Instant.fromEpochMilliseconds(it.startedAtEpochMs).toLocalDateTime(zone).date.mondayOfWeek() }
        .map { (week, list) ->
            val blocks = list.flatMap { it.blocks }
            WeekSummary(
                weekStart = week,
                sessions = list.size,
                completed = list.count { it.status == "COMPLETED" },
                partial = list.count { it.status == "PARTIAL_FINISHED" },
                // Only time actually spent counts; skipped blocks contribute 0 seconds by construction.
                workSeconds = blocks.filter { it.type == "WORK" && it.outcome != "SKIPPED" }.sumOf { it.actualSeconds },
                stretchSeconds = blocks.filter { it.type == "STRETCH" && it.outcome != "SKIPPED" }.sumOf { it.actualSeconds },
                skippedBlocks = blocks.count { it.outcome == "SKIPPED" },
                variationIds = blocks.filter { it.type == "WORK" && it.outcome != "SKIPPED" }.mapNotNull { it.variationId }.toSet(),
            )
        }.sortedByDescending { it.weekStart }
