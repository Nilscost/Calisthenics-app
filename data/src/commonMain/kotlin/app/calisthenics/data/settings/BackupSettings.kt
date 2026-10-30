package app.calisthenics.data.settings

import app.calisthenics.domain.model.EquipmentProfile
import app.calisthenics.domain.model.PersonalLevelTargets
import app.calisthenics.domain.model.Preferences
import app.calisthenics.domain.model.Routine
import app.calisthenics.domain.planner.PreviewDraft
import app.calisthenics.domain.planner.TrainSettings
import app.calisthenics.domain.routine.SavedRoutine

interface BackupSettings {
    fun loadPreferences(): Preferences
    fun savePreferences(value: Preferences)
    fun loadRoutine(): Routine
    fun saveRoutine(value: Routine)
    fun loadProfiles(): List<EquipmentProfile>
    fun saveProfiles(value: List<EquipmentProfile>)
    fun loadSavedRoutines(): List<SavedRoutine>
    fun saveSavedRoutines(value: List<SavedRoutine>)
    fun loadPersonalTargets(): Map<String, PersonalLevelTargets>
    fun savePersonalTargets(value: Map<String, PersonalLevelTargets>)
    fun loadTrainSettings(): TrainSettings
    fun saveTrainSettings(value: TrainSettings)
    fun clearTrainDefaultMarkers()
    fun getDraft(key: String, todayEpochDay: Long): PreviewDraft?
    fun putDraft(draft: PreviewDraft, todayEpochDay: Long)
    fun clearDraft()
    fun setModeRevision(key: String, revision: Long)
}
class StoreBackupSettings(private val provider: SettingsProvider, private val todayEpochDay: Long) : BackupSettings {
    override fun loadPreferences() = PrefsStore.load(provider)
    override fun savePreferences(value: Preferences) = PrefsStore.save(provider, value)
    override fun loadRoutine() = RoutineStore.load(provider)
    override fun saveRoutine(value: Routine) = RoutineStore.save(provider, value)
    override fun loadProfiles() = ProfileStore.load(provider)
    override fun saveProfiles(value: List<EquipmentProfile>) = ProfileStore.save(provider, value)
    override fun loadSavedRoutines() = SavedRoutineStore.load(provider)
    override fun saveSavedRoutines(value: List<SavedRoutine>) = SavedRoutineStore.saveAll(provider, value)
    override fun loadPersonalTargets() = PersonalTargetStore.load(provider)
    override fun savePersonalTargets(value: Map<String, PersonalLevelTargets>) = PersonalTargetStore.saveAll(provider, value)
    override fun loadTrainSettings() = TrainSettingsStore.load(provider, todayEpochDay)
    override fun saveTrainSettings(value: TrainSettings) = TrainSettingsStore.save(provider, value)
    override fun clearTrainDefaultMarkers() = provider.open("train").edit { remove("ordinary_rule_default_version"); remove("ordinary_draft_rule_default_version") }
    override fun getDraft(key: String, todayEpochDay: Long) = PreviewDraftStore.get(provider, key, todayEpochDay)
    override fun putDraft(draft: PreviewDraft, todayEpochDay: Long) = PreviewDraftStore.put(provider, draft, todayEpochDay)
    override fun clearDraft() = PreviewDraftStore.clear(provider)
    override fun setModeRevision(key: String, revision: Long) = TrainSettingsStore.setModeRevision(provider, key, revision)
}
