package app.calisthenics.domain

import app.calisthenics.domain.backup.*
import app.calisthenics.domain.history.*
import app.calisthenics.domain.model.Preferences
import kotlin.test.Test
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

class BackupHistoryTest {
    private fun b(id: String, v: String?, type: String, outcome: String, s: Int) = BlockRecord(id, v, type, outcome, s)
    private fun ms(y: Int, m: Int, d: Int, h: Int = 12) = LocalDate(y, m, d).toEpochDays().toLong() * 86_400_000L + h * 3_600_000L

    private val s1 = SessionRecord("s1", "p1", ms(2026, 10, 5), ms(2026, 10, 5, 13), "COMPLETED", listOf(
        b("b1", "pushup-standard", "WORK", "MET", 40), b("b2", "stretch-chest-door", "STRETCH", "MET", 30), b("b3", "plank", "WORK", "SKIPPED", 0)))
    private val s2 = SessionRecord("s2", "p2", ms(2026, 10, 11, 20), ms(2026, 10, 11, 21), "PARTIAL_FINISHED", listOf(
        b("b1", "squat-air", "WORK", "PARTIAL", 25)))
    private val s3 = SessionRecord("s3", "p3", ms(2026, 10, 12), null, "RECOVERY_REQUIRED", listOf(b("b1", "plank", "WORK", "MET", 99)))

    private fun payload() = BackupPayload(Preferences(), emptyList(), listOf(PlanRecord("p1", "{}"), PlanRecord("p2", "{}"), PlanRecord("p3", "{}")),
        listOf(s1, s2, s3), listOf(FeedbackRecord("s1", "pushup-standard", 1, "MET", false, false, 5L)),
        listOf(ProgressionEventRecord("e1", "pushup-standard", "ADVANCE", 1, 2, "3 sessions", 9L)),
        trainSettings = app.calisthenics.domain.planner.TrainSettings(fixedProgramId = "pilates-core-10", fixedProgramFlowOverrides = mapOf("army-hsd" to false)))

    @Test fun weeksStartMondayAndGroupSessions() {
        val w = weeklySummaries(listOf(s1, s2), TimeZone.of("UTC"))
        assertEquals(1, w.size) // Mon 5 Oct .. Sun 11 Oct
        assertEquals(LocalDate(2026, 10, 5), w[0].weekStart)
        assertEquals(2, w[0].sessions); assertEquals(1, w[0].completed); assertEquals(1, w[0].partial)
    }

    @Test fun skippedWorkCountsAsZeroAndUnfinishedSessionsAreIgnored() {
        val w = weeklySummaries(listOf(s1, s2, s3), TimeZone.of("UTC"))
        assertEquals(1, w.size)
        assertEquals(65, w[0].workSeconds) // 40 + 25, skipped plank not counted, s3 ignored
        assertEquals(30, w[0].stretchSeconds)
        assertEquals(1, w[0].skippedBlocks)
        assertEquals(setOf("pushup-standard", "squat-air"), w[0].variationIds)
    }

    @Test fun sundayNightLocalTimeBelongsToThatWeek() {
        val zurich = TimeZone.of("Europe/Zurich")
        val late = SessionRecord("x", "p", ms(2026, 10, 11, 23), null, "COMPLETED", emptyList()) // 01:00 Mon in Zurich
        assertEquals(LocalDate(2026, 10, 12), weeklySummaries(listOf(late), zurich)[0].weekStart)
        assertEquals(LocalDate(2026, 10, 5), weeklySummaries(listOf(late), TimeZone.of("UTC"))[0].weekStart)
    }

    @Test fun zurichWeekBoundaryReflectsSeasonalOffset() {
        val zone = TimeZone.of("Europe/Zurich")
        fun week(y: Int, m: Int, d: Int, h: Int): LocalDate {
            val session = SessionRecord("dst", "p", ms(y, m, d, h), null, "COMPLETED", emptyList())
            return weeklySummaries(listOf(session), zone).single().weekStart
        }
        // The Sunday before spring's DST change is UTC+1; the next Sunday is UTC+2.
        assertEquals(LocalDate(2026, 3, 16), week(2026, 3, 22, 22))
        assertEquals(LocalDate(2026, 3, 30), week(2026, 3, 29, 22))
        // After the autumn transition, 22:00 UTC is still Sunday locally.
        assertEquals(LocalDate(2026, 10, 19), week(2026, 10, 25, 22))
        assertEquals(LocalDate(2026, 10, 26), week(2026, 10, 25, 23))
    }

    @Test fun backupRoundTripsExactly() {
        val text = exportBackup(payload(), "0.2.0", 1L)
        val r = importBackup(text)
        assertTrue(r is ImportResult.Ok)
        assertEquals(payload(), (r as ImportResult.Ok).payload)
    }

    @Test fun editedBackupIsRejected() {
        val text = exportBackup(payload(), "0.2.0", 1L).replace("squat-air", "squat-aix")
        val r = importBackup(text)
        assertTrue(r is ImportResult.Rejected)
        assertTrue((r as ImportResult.Rejected).reason.contains("checksum"))
    }

    @Test fun garbageAndNewerFormatAreRejected() {
        assertTrue(importBackup("hello") is ImportResult.Rejected)
        val newer = exportBackup(payload(), "9.9", 1L).replace("\"formatVersion\": 1", "\"formatVersion\": 2")
        val r = importBackup(newer) as ImportResult.Rejected
        assertTrue(r.reason.contains("newer"))
    }

    @Test fun inconsistentPayloadIsRejectedEvenWithValidChecksum() {
        val bad = payload().copy(sessions = listOf(s1.copy(planId = "missing")))
        val r = importBackup(exportBackup(bad, "0.2.0", 1L))
        assertTrue(r is ImportResult.Rejected && r.reason.contains("missing plan"))
    }

    @Test fun duplicateSessionsAreFlagged() {
        assertEquals(1, integrityProblems(payload().copy(sessions = listOf(s1, s1))).size)
    }
}
