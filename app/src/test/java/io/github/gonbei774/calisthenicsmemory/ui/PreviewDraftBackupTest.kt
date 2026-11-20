package io.github.gonbei774.calisthenicsmemory.ui

import app.calisthenics.domain.backup.*
import app.calisthenics.domain.model.*
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.planner.PreviewDraft
import app.calisthenics.domain.planner.draftKey
import io.github.gonbei774.calisthenicsmemory.ui.screens.PreviewDraftBackupCodec
import io.github.gonbei774.calisthenicsmemory.ui.train.*
import org.junit.Assert.*
import org.junit.Test

class PreviewDraftBackupTest {
    @Test fun backupRetainsCurrentConfigurationAndUnrelatedDraftEdits() {
        val settings = TrainSettings(goalId = "general", betweenSetMode = BetweenSetMode.STRETCH, preparationSeconds = 8)
        val draft = PreviewDraft(draftKey(settings), edits = PlanEdits(swaps = mapOf("push" to "pushup-knee"),
            tierOverrides = mapOf("push" to 3), cooldownRemoved = setOf("calf"),
            rule = ProgressionRule(kind = RuleKind.CUSTOM, sessions = 2)),
            rest = mapOf("push" to 45), betweenSetMode = BetweenSetMode.STRETCH, preparationSeconds = 8)
        val payload = BackupPayload(Preferences(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(),
            trainSettings = settings, previewDraftJson = PreviewDraftBackupCodec.encode(draft))
        val restored = (importBackup(exportBackup(payload, "0.11.4-r8", 123)) as ImportResult.Ok).payload
        assertEquals(draft, PreviewDraftBackupCodec.decode(restored.previewDraftJson, restored.trainSettings))
    }

    @Test fun oldBackupHasNoDraftAndWrongObjectiveIsRejectedBeforeRestore() {
        assertNull(PreviewDraftBackupCodec.decode(null, TrainSettings()))
        val draft = PreviewDraft(draftKey(TrainSettings(goalId = "general")))
        assertThrows(IllegalArgumentException::class.java) {
            PreviewDraftBackupCodec.decode(PreviewDraftBackupCodec.encode(draft), TrainSettings(goalId = "other"))
        }
    }
}
