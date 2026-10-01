// On-device Room coverage for the CURRENT schema (T03 / G5).
//
// NOTE: this replaces the upstream-inherited MigrationTest, which targeted
// Room version 10 and used MigrationTestHelper against schema JSON files that
// do not exist (AppDatabase has exportSchema = false), so it could never pass
// on-device. The app ships a full manual migration chain (9..21) plus
// fallbackToDestructiveMigration; validating that chain needs exportSchema =
// true + exported schemas, which is tracked separately. This test instead
// proves the live v21 schema and DAO round-trip on real hardware, which is
// what G1/G5 need for the S21 session.
package io.github.gonbei774.calisthenicsmemory.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DataLayerInstrumentedTest {

    private lateinit var database: AppDatabase

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun exerciseRoundTripPreservesAllV21Columns() = runBlocking {
        val dao = database.exerciseDao()
        val insertedId = dao.insertExercise(
            Exercise(
                name = "Wall Push-up",
                type = "Dynamic",
                group = "Step 1 Pushups",
                sortOrder = 1,
                displayOrder = 0,
                laterality = "Bilateral",
                targetSets = 3,
                targetValue = 50,
                isFavorite = true,
                restInterval = 15,
                repDuration = 3,
                distanceTrackingEnabled = true,
                weightTrackingEnabled = false,
                assistanceTrackingEnabled = true,
                description = "Keep a straight line head to heels"
            )
        )
        assertNotNull(insertedId)

        val readBack = dao.getExerciseByNameAndType("Wall Push-up", "Dynamic")
        assertNotNull(readBack)
        val e = readBack!!
        assertEquals("Step 1 Pushups", e.group)
        assertEquals(1, e.sortOrder)
        assertEquals("Bilateral", e.laterality)
        assertEquals(3, e.targetSets)
        assertEquals(50, e.targetValue)
        assertTrue(e.isFavorite)
        assertEquals(15, e.restInterval)
        assertEquals(3, e.repDuration)
        assertTrue(e.distanceTrackingEnabled)
        assertEquals(false, e.weightTrackingEnabled)
        assertTrue(e.assistanceTrackingEnabled)
        assertEquals("Keep a straight line head to heels", e.description)
    }

    @Test
    fun groupRoundTripAndQueryByGroup() = runBlocking {
        val groupDao = database.exerciseGroupDao()
        val exerciseDao = database.exerciseDao()

        groupDao.insertGroup(ExerciseGroup(name = "Step 2 Squats", displayOrder = 1))
        exerciseDao.insertExercise(
            Exercise(name = "Bodyweight Squat", type = "Dynamic", group = "Step 2 Squats")
        )

        val groups = groupDao.getAllGroups().first()
        assertTrue(groups.any { it.name == "Step 2 Squats" })

        val inGroup = exerciseDao.getExercisesByGroup("Step 2 Squats")
        assertEquals(1, inGroup.size)
        assertEquals("Bodyweight Squat", inGroup[0].name)
    }
}
