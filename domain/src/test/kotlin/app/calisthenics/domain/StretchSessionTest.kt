package app.calisthenics.domain

import app.calisthenics.domain.equipment.SeedProfiles
import app.calisthenics.domain.model.*
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.routine.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** L11b: the ready-made "Starting To Stretch" session (r/flexibility): stretches only, the bump-and-hold protocol. */
class StretchSessionTest {
    private val catalog = app.calisthenics.domain.content.parseCatalog(File(System.getProperty("repo.root"), "content/starter/catalog.json").readText())
    private val none = app.calisthenics.domain.progression.ProgressSnapshot(emptyMap(), emptyMap(), emptyMap(), emptyList())
    private val home = SeedProfiles.home.copy(items = SeedProfiles.home.items + EquipmentItem("chair", suitability = setOf("stable")))
    private fun plan(p: SavedRoutine, profile: EquipmentProfile = home): WorkoutPlan {
        val r = resolveSaved(p, settingsFor(p, TrainSettings()))
        return (buildTrainPlan(catalog, r.routine, none, profile, r.settings, edits = r.edits) as PlanResult.Ready).plan
    }

    @Test fun theSessionHasTenStretchesInTwoHalves() {
        val all = Presets.startingToStretch(catalog, home)
        assertEquals(listOf(Presets.STRETCH, Presets.STRETCH_UPPER, Presets.STRETCH_LOWER), all.map { it.id })
        assertEquals(10, all[0].routine.stretchSession.size); assertEquals(5, all[1].routine.stretchSession.size); assertEquals(5, all[2].routine.stretchSession.size)
        assertEquals(all[0].routine.stretchSession, all[1].routine.stretchSession + all[2].routine.stretchSession)
        assertTrue(all.all { it.credit!!.contains("r/flexibility") && Presets.isPreset(it.id) })
        assertTrue(Presets.all(catalog, home).map { it.id }.containsAll(all.map { it.id }))
    }

    @Test fun everyStretchRunsBumpsThenHoldsOf10And20And30Seconds() {
        val p = plan(Presets.startingToStretch(catalog, home)[1])
        val first = p.blocks.filter { it.slotId == "ss0" }          // the first stretch of the upper half: one side or two
        assertTrue(first.all { it.type == BlockType.STRETCH })
        val oneSide = first.filter { it.side == first.first().side }
        assertEquals(listOf(10, 10, 10, 20, 10, 30), oneSide.map { it.durationSeconds })   // bumps 10 s, holds 10, 20, 30 s
        assertEquals(listOf(10, 20, 30), oneSide.filter { it.id.endsWith("-hold") }.map { it.durationSeconds })
        assertEquals(3, oneSide.count { it.id.endsWith("-bump") && it.durationSeconds == 10 })
        assertTrue(oneSide.filter { it.id.endsWith("-bump") }.all { "10 times" in it.note!! && "never forced" in it.note!! })
        assertTrue(oneSide.filter { it.id.endsWith("-hold") }.all { "Hold for" in it.note!! })
    }

    @Test fun totalIsAboutHalfAnHourForBothHalvesAndHalvesAreShorter() {
        val full = plan(Presets.startingToStretch(catalog, home)[0]); val up = plan(Presets.startingToStretch(catalog, home)[1]); val lo = plan(Presets.startingToStretch(catalog, home)[2])
        assertTrue("${full.plannedDurationSeconds}", full.plannedDurationSeconds in 20 * 60..32 * 60)
        assertTrue(up.plannedDurationSeconds < full.plannedDurationSeconds && lo.plannedDurationSeconds < full.plannedDurationSeconds)
        assertEquals(full.blocks.sumOf { it.durationSeconds }, full.plannedDurationSeconds)
        assertTrue(full.blocks.none { it.type == BlockType.WORK } && full.rounds == 1)   // stretches only; never strength work
        assertTrue(full.blocks.none { it.type == BlockType.WARMUP })
    }

    @Test fun aStretchTheProfileCannotDoIsLeftOut() {
        val bare = SeedProfiles.home.copy(items = emptyList(), capabilities = setOf("floor-space"))      // no chair, no wall
        val lower = Presets.startingToStretch(catalog, bare)[2].routine.stretchSession
        assertFalse("stretch-pike-one-leg" in lower); assertFalse("stretch-calf-wall" in lower)
        assertTrue(plan(Presets.startingToStretch(catalog, bare)[0], bare).blocks.isNotEmpty())
    }
}
