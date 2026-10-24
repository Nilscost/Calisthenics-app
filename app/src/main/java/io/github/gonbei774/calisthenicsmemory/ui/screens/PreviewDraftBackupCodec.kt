package io.github.gonbei774.calisthenicsmemory.ui.screens

import app.calisthenics.domain.planner.TrainSettings
import io.github.gonbei774.calisthenicsmemory.ui.train.PreviewDraft
import io.github.gonbei774.calisthenicsmemory.ui.train.draftKey
import kotlinx.serialization.json.Json

/** Validate objective ownership before restore can mutate history or preferences. */
internal object PreviewDraftBackupCodec {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
    fun encode(draft: PreviewDraft): String = json.encodeToString(PreviewDraft.serializer(), draft)
    fun decode(text: String?, settings: TrainSettings?): PreviewDraft? {
        val draft = text?.let { json.decodeFromString(PreviewDraft.serializer(), it) } ?: return null
        require(settings?.let { draft.key == draftKey(it) } == true) { "Backup draft does not match its workout objective." }
        return draft
    }
}
