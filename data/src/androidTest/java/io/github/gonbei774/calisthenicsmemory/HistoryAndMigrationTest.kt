// T07 on-device tests (run on the phone / emulator; the sandbox can only compile them).
package io.github.gonbei774.calisthenicsmemory.data

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryAndMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    private val ctx get() = ApplicationProvider.getApplicationContext<android.content.Context>()
    private var db: AppDatabase? = null

    @After fun close() { db?.close() }

    private fun mem() = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).allowMainThreadQueries().build().also { db = it }

    private fun plan(id: String) = PlanSnapshotEntity(id, 1L, "r", 1, 1, "home", """{"frozen":true}""")

    @Test fun migration21to22KeepsExistingRowsAndAddsHistoryTables() {
        helper.createDatabase("mig", 21).apply {
            execSQL("INSERT INTO exercises (name, type, `group`, sortOrder, displayOrder, laterality, targetSets, targetValue, isFavorite, restInterval, repDuration, distanceTrackingEnabled, weightTrackingEnabled, assistanceTrackingEnabled, description) VALUES ('Push-up','Dynamic',NULL,0,0,'Bilateral',3,10,0,60,NULL,0,0,0,'')")
            close()
        }
        val migrated = helper.runMigrationsAndValidate("mig", 22, true, AppDatabase.MIGRATION_21_22)
        migrated.query("SELECT COUNT(*) FROM exercises").use { it.moveToFirst(); assertEquals(1, it.getInt(0)) }
        migrated.query("SELECT COUNT(*) FROM plan_snapshots").use { it.moveToFirst(); assertEquals(0, it.getInt(0)) }
    }

    @Test fun planSnapshotIsNeverOverwritten() = runBlocking {
        val dao = mem().historyDao()
        dao.insertPlan(plan("p1"))
        dao.insertPlan(plan("p1").copy(planJson = """{"frozen":false}"""))
        assertEquals("""{"frozen":true}""", dao.plan("p1")!!.planJson)
    }

    @Test fun duplicateSessionIdIsRejected() = runBlocking {
        val dao = mem().historyDao()
        dao.insertPlan(plan("p1"))
        dao.insertSession(WorkoutSessionEntity("s1", "p1", 1L, null, "RUNNING"))
        try { dao.insertSession(WorkoutSessionEntity("s1", "p1", 2L, null, "RUNNING")); fail("duplicate accepted") }
        catch (_: SQLiteConstraintException) {}
    }

    @Test fun feedbackLoggedDuringTheWorkoutIsSavedWithTheSession() = runBlocking {
        val dao = mem().historyDao()
        dao.saveFinishedSession(plan("p1"), WorkoutSessionEntity("s1", "p1", 1L, 2L, "COMPLETED"),
            listOf(BlockResultEntity(sessionId = "s1", blockId = "b1", outcome = "MET", actualSeconds = 30, achievedValue = 6)),
            listOf(FeedbackRevisionEntity(sessionId = "s1", variationId = "v1", revision = 1, rating = "BELOW", discomfort = false, assumedMet = false, createdAtEpochMs = 3L, actualReps = 6)))
        assertEquals(6, dao.feedbackHistory("s1", "v1").single().actualReps)
        assertEquals(6, dao.blockResults("s1").single().achievedValue)
    }

    @Test fun failedSaveLeavesPriorRecordsIntact() = runBlocking {
        val dao = mem().historyDao()
        dao.saveFinishedSession(plan("p1"), WorkoutSessionEntity("s1", "p1", 1L, 2L, "COMPLETED"),
            listOf(BlockResultEntity(sessionId = "s1", blockId = "b1", outcome = "MET", actualSeconds = 30, achievedValue = 10)))
        // Second save: valid plan, but duplicate block id inside the batch -> whole transaction must roll back.
        try {
            dao.saveFinishedSession(plan("p2"), WorkoutSessionEntity("s2", "p2", 3L, 4L, "COMPLETED"), listOf(
                BlockResultEntity(sessionId = "s2", blockId = "b1", outcome = "MET", actualSeconds = 30, achievedValue = 10),
                BlockResultEntity(sessionId = "s2", blockId = "b1", outcome = "MET", actualSeconds = 30, achievedValue = 10)))
            fail("expected constraint failure")
        } catch (_: SQLiteConstraintException) {}
        assertEquals(listOf("s1"), dao.sessions().map { it.sessionId })
        assertEquals(null, dao.plan("p2"))
        assertEquals(1, dao.blockResults("s1").size)
    }

    @Test fun editingFeedbackAddsRevisionAndKeepsOriginal() = runBlocking {
        val dao = mem().historyDao()
        dao.saveFinishedSession(plan("p1"), WorkoutSessionEntity("s1", "p1", 1L, 2L, "COMPLETED"), emptyList())
        assertEquals(1, dao.reviseFeedback("s1", "pushup-standard", "MET", false, false, 10L))
        assertEquals(2, dao.reviseFeedback("s1", "pushup-standard", "BELOW", false, false, 20L))
        val h = dao.feedbackHistory("s1", "pushup-standard")
        assertEquals(listOf("MET", "BELOW"), h.map { it.rating })
    }

    @Test fun plansReferencedBySessionsCannotBeDeleted() = runBlocking {
        val d = mem()
        d.historyDao().saveFinishedSession(plan("p1"), WorkoutSessionEntity("s1", "p1", 1L, 2L, "COMPLETED"), emptyList())
        try { d.openHelper.writableDatabase.execSQL("DELETE FROM plan_snapshots WHERE planId='p1'"); fail("history plan deleted") }
        catch (_: android.database.sqlite.SQLiteException) {}
        assertNotNull(d.historyDao().plan("p1"))
    }
}
