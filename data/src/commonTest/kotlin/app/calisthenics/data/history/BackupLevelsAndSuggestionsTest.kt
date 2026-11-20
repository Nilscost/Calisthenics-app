package app.calisthenics.data.history

import app.calisthenics.data.settings.LevelStore
import app.calisthenics.data.settings.MemoryProvider
import app.calisthenics.data.settings.StoreBackupSettings
import app.calisthenics.data.settings.SuggestionStore
import app.calisthenics.domain.backup.ImportResult
import app.calisthenics.domain.backup.exportBackup
import app.calisthenics.domain.backup.importBackup
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/** 9.4 F9: hand-set levels and accepted suggestions travel with the backup. */
class BackupLevelsAndSuggestionsTest {
    private val day = 20_000L
    private fun apply(p: MemoryProvider) = BackupApply(InMemoryHistoryRepository(), StoreBackupSettings(p, day))
    private fun source() = MemoryProvider().also { p ->
        LevelStore.save(p, mapOf("pushup-standard" to 3, "row-inverted" to 2), today = 19_990)
        SuggestionStore.accept(p, "pushup-standard", 4, 19_995)
        SuggestionStore.acceptNext(p, "pushup-standard", "pushup-diamond", 19_996)
        SuggestionStore.acceptEasier(p, "row-inverted", 1, 19_997)
    }
    private suspend fun export(from: MemoryProvider): String = exportBackup(apply(from).buildPayload(day), "t", 1L)
    private suspend fun restore(text: String, into: MemoryProvider) =
        apply(into).restore((importBackup(text) as ImportResult.Ok).payload, day)

    @Test fun exportThenImportIntoEmptyStoresGivesIdenticalActions() = runTest {
        val from = source(); val to = MemoryProvider()
        restore(export(from), to)
        assertEquals(LevelStore.actions(from).toSet(), LevelStore.actions(to).toSet())
        assertEquals(SuggestionStore.accepted(from), SuggestionStore.accepted(to))
        assertEquals(3, SuggestionStore.accepted(to).size)
        assertEquals(2, LevelStore.actions(to).size)
    }

    @Test fun importingTheSameFileTwiceAddsNothing() = runTest {
        val text = export(source()); val to = MemoryProvider()
        restore(text, to); val once = LevelStore.actions(to).toSet() to SuggestionStore.accepted(to)
        restore(text, to)
        assertEquals(once, LevelStore.actions(to).toSet() to SuggestionStore.accepted(to))
    }

    @Test fun oldBackupWithoutTheFieldsStillImportsAndLeavesLocalStoresAlone() = runTest {
        val text = export(MemoryProvider())
        assertFalse(text.contains("levelEdits")); assertFalse(text.contains("acceptedSuggestions")) // empty fields are not written
        val to = source(); val before = LevelStore.actions(to).toSet() to SuggestionStore.accepted(to)
        restore(text, to)
        assertEquals(before, LevelStore.actions(to).toSet() to SuggestionStore.accepted(to))
    }

    @Test fun restoreKeepsAMoreRecentLocalLevelAndAddsTheRest() = runTest {
        val text = export(source()) // pushup 3 and row 2, both dated 19990
        val to = MemoryProvider(); LevelStore.save(to, mapOf("pushup-standard" to 5, "squat-air" to 2), today = 19_999)
        restore(text, to)
        assertEquals(mapOf("pushup-standard" to 5, "row-inverted" to 2, "squat-air" to 2), LevelStore.load(to))
        assertEquals(19_999, LevelStore.days(to)["pushup-standard"])
    }
}
