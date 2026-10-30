package app.calisthenics.data.history

import app.calisthenics.data.settings.BackupSettings
import app.calisthenics.domain.backup.BackupPayload
import app.calisthenics.domain.backup.FeedbackRecord
import app.calisthenics.domain.backup.ProgressionEventRecord
import app.calisthenics.domain.backup.PlanRecord as BackupPlanRecord
import app.calisthenics.domain.history.BlockRecord
import app.calisthenics.domain.history.SessionRecord
import app.calisthenics.domain.model.WorkoutPlan
import app.calisthenics.domain.planner.PreviewDraft
import app.calisthenics.domain.planner.TrainSettings
import app.calisthenics.domain.planner.draftKey
import app.calisthenics.domain.routine.mergeSaved
import kotlinx.serialization.json.Json

object PreviewDraftBackupCodec {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
    fun encode(draft: PreviewDraft): String = json.encodeToString(PreviewDraft.serializer(), draft)
    fun decode(text: String?, settings: TrainSettings?): PreviewDraft? {
        val draft = text?.let { json.decodeFromString(PreviewDraft.serializer(), it) } ?: return null
        require(settings?.let { draft.key == draftKey(it) } == true) { "Backup draft does not match its workout objective." }
        return draft
    }
}

class BackupApply(private val history: HistoryRepository, private val settings: BackupSettings) {
    suspend fun buildPayload(todayEpochDay: Long): BackupPayload {
        val sessions = history.sessions()
        val train = settings.loadTrainSettings()
        val draft = settings.getDraft(draftKey(train), todayEpochDay)
        return BackupPayload(
            preferences = settings.loadPreferences(), routines = listOf(settings.loadRoutine()),
            plans = sessions.mapNotNull { history.plan(it.planId) }.distinctBy { it.planId }.map { BackupPlanRecord(it.planId, it.planJson) },
            sessions = sessions.map { s -> SessionRecord(s.sessionId, s.planId, s.startedAtEpochMs, s.endedAtEpochMs, s.status,
                history.blockResults(s.sessionId).map { BlockRecord(it.blockId, null, "WORK", it.outcome, it.actualSeconds, it.achievedValue) }) },
            feedback = history.allFeedback().map { FeedbackRecord(it.sessionId, it.variationId, it.revision, it.rating, it.discomfort, it.assumedMet, it.createdAtEpochMs, it.actualReps, it.blockId, it.actualHoldSeconds) },
            events = history.allEvents().map { ProgressionEventRecord(it.eventId, it.variationId, it.kind, it.fromTier, it.toTier, it.reason, it.atEpochMs) },
            profiles = settings.loadProfiles(), savedRoutines = settings.loadSavedRoutines(), personalTargets = settings.loadPersonalTargets(),
            trainSettings = train, previewDraftJson = draft?.let { PreviewDraftBackupCodec.encode(it) },
        )
    }
    suspend fun restore(p: BackupPayload, todayEpochDay: Long): Int {
        val restoredDraft = PreviewDraftBackupCodec.decode(p.previewDraftJson, p.trainSettings)
        val pj = Json { ignoreUnknownKeys = true }
        val plans = p.plans.map { pr -> val wp = pj.decodeFromString(WorkoutPlan.serializer(), pr.planJson); PlanRecord(pr.planId, wp.createdAtEpochMs, wp.routineId, wp.routineRevision, wp.catalogVersion, wp.profileId, pr.planJson) }
        val sessions = p.sessions.map { SessionRow(it.sessionId, it.planId, it.startedAtEpochMs, it.endedAtEpochMs, it.status) }
        val blocks = p.sessions.flatMap { s -> s.blocks.map { BlockRow(s.sessionId, it.blockId, it.outcome, it.actualSeconds, it.achievedValue) } }
        val feedback = p.feedback.map { FeedbackRow(it.sessionId, it.variationId, it.revision, it.rating, it.discomfort, it.assumedMet, it.createdAtEpochMs, it.actualReps, it.blockId, it.actualHoldSeconds) }
        val events = p.events.map { EventRow(it.eventId, it.variationId, it.kind, it.fromTier, it.toTier, it.reason, it.atEpochMs) }
        val added = history.restoreMerge(plans, sessions, blocks, feedback, events)
        settings.savePreferences(p.preferences)
        p.routines.firstOrNull()?.let { settings.saveRoutine(it) }
        if (p.profiles.isNotEmpty()) settings.saveProfiles(p.profiles)
        if (p.savedRoutines.isNotEmpty()) settings.saveSavedRoutines(mergeSaved(settings.loadSavedRoutines(), p.savedRoutines))
        if (p.personalTargets.isNotEmpty()) {
            val local = settings.loadPersonalTargets()
            settings.savePersonalTargets(local + p.personalTargets.filter { (id, value) -> value.revision >= (local[id]?.revision ?: 0) })
        }
        settings.clearTrainDefaultMarkers()
        p.trainSettings?.let { settings.saveTrainSettings(it) }
        p.trainSettings?.let { train ->
            settings.clearDraft()
            settings.setModeRevision(draftKey(train), restoredDraft?.modeRevision ?: 0L)
            restoredDraft?.let { settings.putDraft(it, todayEpochDay) }
        }
        return added
    }
}
