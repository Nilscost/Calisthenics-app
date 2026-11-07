package app.calisthenics.domain

import app.calisthenics.domain.backup.BackupFile
import app.calisthenics.domain.backup.ImportResult
import app.calisthenics.domain.backup.importBackup
import kotlinx.serialization.json.Json
import kotlin.test.Test

class BackupChecksumCompatibilityTest {
    @Test fun standardAndUnicodePayloadHashesAreAcceptedBeforePayloadParsing() {
        val vectors = listOf(
            "" to "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            "abc" to "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            "café 🏋️" to "33a6a307035d587c37655cbe5a8bdfb6af1e025378cd90e84097b1649c4d3b08",
            "a".repeat(1000) to "41edece42d63e8d9bf515a9ba6932e1c20cbc9f5a5d134645adb5db1b9737ea3"
        )
        vectors.forEach { (body, hash) ->
            val text = Json.encodeToString(BackupFile.serializer(), BackupFile(1, "test", 0, hash, body))
            assertEquals(ImportResult.Rejected("The backup content is unreadable."), importBackup(text))
        }
    }

    @Test fun committedSeedPayloadRetainsItsOriginalChecksum() {
        val text = fixtureText("app/src/debug/assets/seed/backup.json")
        val seed = Json.decodeFromString(BackupFile.serializer(), text)
        assertEquals("2fb97385aeaed021a588174b686a9d5e7d2fb431568888118059e444ef262c48", seed.payloadSha256)
        assertTrue(importBackup(text) is ImportResult.Ok)
    }
}
