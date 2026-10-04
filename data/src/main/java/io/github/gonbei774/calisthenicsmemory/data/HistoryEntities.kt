// T07 (DATA-01/02): append-only history tables. Plans are frozen snapshots; feedback is revisioned;
// progression events are never edited. Existing v21 tables are untouched.
package io.github.gonbei774.calisthenicsmemory.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "plan_snapshots")
data class PlanSnapshotEntity(
    @PrimaryKey val planId: String,
    val createdAtEpochMs: Long,
    val routineId: String,
    val routineRevision: Int,
    val catalogVersion: Int,
    val profileId: String,
    /** Full serialized WorkoutPlan; never rewritten after insert. */
    val planJson: String,
)

@Entity(
    tableName = "workout_sessions",
    foreignKeys = [ForeignKey(PlanSnapshotEntity::class, ["planId"], ["planId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("planId")],
)
data class WorkoutSessionEntity(
    @PrimaryKey val sessionId: String,
    val planId: String,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long?,
    /** RUNNING, COMPLETED, PARTIAL_FINISHED, RECOVERY_REQUIRED */
    val status: String,
)

@Entity(
    tableName = "block_results",
    foreignKeys = [ForeignKey(WorkoutSessionEntity::class, ["sessionId"], ["sessionId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["sessionId", "blockId"], unique = true)],
)
data class BlockResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val blockId: String,
    /** MET, PARTIAL, SKIPPED, REPLACED */
    val outcome: String,
    val actualSeconds: Int,
    val achievedValue: Int?,
)

@Entity(
    tableName = "feedback_revisions",
    foreignKeys = [ForeignKey(WorkoutSessionEntity::class, ["sessionId"], ["sessionId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["sessionId", "variationId", "revision"], unique = true)],
)
data class FeedbackRevisionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val variationId: String,
    val revision: Int,
    /** BELOW, MET, ABOVE, or NONE (then assumedMet applies). */
    val rating: String,
    val discomfort: Boolean,
    val assumedMet: Boolean,
    val createdAtEpochMs: Long,
)

@Entity(tableName = "progression_events", indices = [Index("variationId")])
data class ProgressionEventEntity(
    @PrimaryKey val eventId: String,
    val variationId: String,
    /** ADVANCE, SUCCESSOR, HOLD, REENTRY, SUGGEST_LOWER */
    val kind: String,
    @ColumnInfo(name = "fromTier") val fromTier: Int,
    @ColumnInfo(name = "toTier") val toTier: Int,
    val reason: String,
    val atEpochMs: Long,
)
