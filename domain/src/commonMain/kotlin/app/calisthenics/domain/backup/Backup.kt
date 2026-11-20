// T20 (domain part): versioned, checksummed backup file. Adapters read/write the bytes; this is the logic.
package app.calisthenics.domain.backup

import app.calisthenics.domain.history.SessionRecord
import app.calisthenics.domain.model.EquipmentProfile
import app.calisthenics.domain.model.Preferences
import app.calisthenics.domain.model.Routine
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.kotlincrypto.hash.sha2.SHA256

const val BACKUP_FORMAT_VERSION = 1

@Serializable
data class FeedbackRecord(val sessionId: String, val variationId: String, val revision: Int, val rating: String,
                          val discomfort: Boolean, val assumedMet: Boolean, val createdAtEpochMs: Long,
                          val actualReps: Int? = null,
                          /** V04a: per-round correction (null = whole exercise) and hold seconds. Appended last with defaults. */
                          val blockId: String? = null, val actualHoldSeconds: Int? = null)

@Serializable
data class ProgressionEventRecord(val eventId: String, val variationId: String, val kind: String,
                                  val fromTier: Int, val toTier: Int, val reason: String, val atEpochMs: Long)

@Serializable
data class PlanRecord(val planId: String, val planJson: String)

/** 9.4 F9: a hand-set level (LevelStore): the variation's level and the epoch day it was set (0 = undated). */
@Serializable
data class LevelEditRecord(val variationId: String, val tier: Int, val day: Int)

/**
 * 9.4 F9: an accepted suggestion (SuggestionStore). [kind] is "harder" ("Too easy" raise, [value] = tier), "easier" ("Go down to level N-1",
 * [value] = tier) or "next" (accepted next exercise, [value] = the new variation id). [day] is the epoch day it was accepted.
 */
@Serializable
data class AcceptedSuggestionRecord(val kind: String, val variationId: String, val value: String, val day: Int)

/** Restore merge for levels: per variation the later day wins; on a tie the backup's value wins (same as personal targets). */
fun mergeLevelEdits(local: List<LevelEditRecord>, incoming: List<LevelEditRecord>): List<LevelEditRecord> {
    val out = LinkedHashMap<String, LevelEditRecord>()
    local.forEach { out[it.variationId] = it }
    incoming.forEach { e -> val l = out[e.variationId]; if (l == null || e.day >= l.day) out[e.variationId] = e }
    return out.values.toList()
}

/** Restore merge for accepted suggestions: union, so importing the same file twice adds nothing. */
fun mergeAcceptedSuggestions(local: List<AcceptedSuggestionRecord>, incoming: List<AcceptedSuggestionRecord>): List<AcceptedSuggestionRecord> =
    (local + incoming).distinct()

@Serializable
data class BackupPayload(
    val preferences: Preferences,
    val routines: List<Routine>,
    val plans: List<PlanRecord>,
    val sessions: List<SessionRecord>,
    val feedback: List<FeedbackRecord>,
    val events: List<ProgressionEventRecord>,
    /** U04: equipment profiles. Appended last with a default so older backup files still import. */
    val profiles: List<EquipmentProfile> = emptyList(),
    /** V18: the routines saved from the Preview. Appended last with a default so older backup files still import. */
    val savedRoutines: List<app.calisthenics.domain.routine.SavedRoutine> = emptyList(),
    /** R8 T07: selected program and per-program flow choices; absent on older backups. */
    val trainSettings: app.calisthenics.domain.planner.TrainSettings? = null,
    val personalTargets: Map<String, app.calisthenics.domain.model.PersonalLevelTargets> = emptyMap(),
    /** Current objective draft, encoded by the app adapter; absent in older backups. */
    val previewDraftJson: String? = null,
    /** 9.4 F9: hand-set levels. Appended with a default and not written when empty, so older files import and their checksums stay valid. */
    @OptIn(ExperimentalSerializationApi::class) @EncodeDefault(EncodeDefault.Mode.NEVER) val levelEdits: List<LevelEditRecord> = emptyList(),
    /** 9.4 F9: accepted suggestions (moves up and down, next exercise). Same rules as [levelEdits]. */
    @OptIn(ExperimentalSerializationApi::class) @EncodeDefault(EncodeDefault.Mode.NEVER) val acceptedSuggestions: List<AcceptedSuggestionRecord> = emptyList(),
    /** 9.4 F9: moment (epoch ms) of the last questionnaire reset (LevelStore.resetAtMs); 0 = never. Same rules as [levelEdits]. */
    @OptIn(ExperimentalSerializationApi::class) @EncodeDefault(EncodeDefault.Mode.NEVER) val levelResetAtMs: Long = 0L,
)

@Serializable
data class BackupFile(
    val formatVersion: Int,
    val appVersion: String,
    val createdAtEpochMs: Long,
    /** SHA-256 hex of the canonical payload JSON. Detects corruption/edits, NOT an authenticity signature. */
    val payloadSha256: String,
    val payload: String,
)

sealed interface ImportResult {
    data class Ok(val payload: BackupPayload) : ImportResult
    data class Rejected(val reason: String) : ImportResult
}

private val json = Json { prettyPrint = false; encodeDefaults = true; ignoreUnknownKeys = false }
private val fileJson = Json { prettyPrint = true; encodeDefaults = true; ignoreUnknownKeys = false }

private fun sha256(s: String) = SHA256().digest(s.encodeToByteArray()).joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }

fun exportBackup(p: BackupPayload, appVersion: String, nowEpochMs: Long): String {
    val body = json.encodeToString(BackupPayload.serializer(), p)
    return fileJson.encodeToString(BackupFile.serializer(), BackupFile(BACKUP_FORMAT_VERSION, appVersion, nowEpochMs, sha256(body), body))
}

/** Validates everything BEFORE anything is applied; a rejected file changes nothing. */
fun importBackup(text: String): ImportResult {
    val file = try { fileJson.decodeFromString(BackupFile.serializer(), text) }
    catch (e: Exception) { return ImportResult.Rejected("Not a valid backup file.") }
    if (file.formatVersion > BACKUP_FORMAT_VERSION)
        return ImportResult.Rejected("This backup was made by a newer app version (format ${file.formatVersion}). Update the app first.")
    if (file.formatVersion < 1) return ImportResult.Rejected("Unknown backup format ${file.formatVersion}.")
    if (sha256(file.payload) != file.payloadSha256) return ImportResult.Rejected("The backup is damaged or was edited (checksum mismatch).")
    val p = try { json.decodeFromString(BackupPayload.serializer(), file.payload) }
    catch (e: Exception) { return ImportResult.Rejected("The backup content is unreadable.") }
    val problems = integrityProblems(p)
    if (problems.isNotEmpty()) return ImportResult.Rejected("Inconsistent backup: " + problems.first())
    return ImportResult.Ok(p)
}

fun integrityProblems(p: BackupPayload): List<String> {
    val out = mutableListOf<String>()
    val planIds = p.plans.map { it.planId }
    planIds.groupBy { it }.filter { it.value.size > 1 }.keys.forEach { out += "duplicate plan $it" }
    val sessionIds = p.sessions.map { it.sessionId }
    sessionIds.groupBy { it }.filter { it.value.size > 1 }.keys.forEach { out += "duplicate session $it" }
    p.sessions.filter { it.planId !in planIds }.forEach { out += "session ${it.sessionId} refers to missing plan ${it.planId}" }
    p.feedback.filter { it.sessionId !in sessionIds }.forEach { out += "feedback refers to missing session ${it.sessionId}" }
    p.feedback.groupBy { Triple(it.sessionId, it.variationId, it.revision) }.filter { it.value.size > 1 }.keys
        .forEach { out += "duplicate feedback revision $it" }
    p.events.map { it.eventId }.groupBy { it }.filter { it.value.size > 1 }.keys.forEach { out += "duplicate event $it" }
    return out
}
