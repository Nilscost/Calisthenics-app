// T20 (domain part): versioned, checksummed backup file. Adapters read/write the bytes; this is the logic.
package app.calisthenics.domain.backup

import app.calisthenics.domain.history.SessionRecord
import app.calisthenics.domain.model.EquipmentProfile
import app.calisthenics.domain.model.Preferences
import app.calisthenics.domain.model.Routine
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.MessageDigest

const val BACKUP_FORMAT_VERSION = 1

@Serializable
data class FeedbackRecord(val sessionId: String, val variationId: String, val revision: Int, val rating: String,
                          val discomfort: Boolean, val assumedMet: Boolean, val createdAtEpochMs: Long,
                          val actualReps: Int? = null)

@Serializable
data class ProgressionEventRecord(val eventId: String, val variationId: String, val kind: String,
                                  val fromTier: Int, val toTier: Int, val reason: String, val atEpochMs: Long)

@Serializable
data class PlanRecord(val planId: String, val planJson: String)

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

private fun sha256(s: String) = MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }

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
