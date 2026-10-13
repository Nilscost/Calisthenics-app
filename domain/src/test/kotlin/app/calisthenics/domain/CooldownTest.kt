package app.calisthenics.domain

import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.equipment.SeedProfiles
import app.calisthenics.domain.model.*
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.progression.ProgressSnapshot
import app.calisthenics.domain.routine.StarterRoutine
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** R7-T13 (D10): the cool-down switch with its two modes. */
class CooldownTest {
    private val catalog = parseCatalog(File(System.getProperty("repo.root"), "content/starter/catalog.json").readText())
    private val none = ProgressSnapshot(emptyMap(), emptyMap(), emptyMap(), emptyList())
    private fun plan(s: TrainSettings, edits: PlanEdits = PlanEdits()) = (buildTrainPlan(catalog, StarterRoutine.routine, none, SeedProfiles.home, s, edits = edits) as PlanResult.Ready).plan
    private fun cool(p: WorkoutPlan) = p.blocks.filter { it.type == BlockType.COOLDOWN }

    @Test fun switchedOffThereIsNoCooldown() = assertTrue(cool(plan(TrainSettings(rounds = 2))).isEmpty())

    @Test fun stretchingModePlansStretchesForTheSessionInFiveToEightMinutes() {
        val p = plan(TrainSettings(rounds = 2, cooldownOn = true, cooldownMode = CooldownMode.STRETCHING))
        val c = cool(p)
        assertTrue(c.isNotEmpty())
        val secs = c.sumOf { it.durationSeconds }
        assertTrue("about 5-8 min, was ${secs / 60.0}", secs in 240..480)
        val compatible = p.blocks.filter { it.type == BlockType.WORK }.mapNotNull { catalog.variation(it.variationId!!) }.flatMap { it.compatibleStretchIds }.toSet()
        assertTrue("every cool-down stretch is listed by an exercise of the session", c.all { it.variationId in compatible })
        assertTrue(c.all { catalog.variation(it.variationId!!)!!.kind == Kind.STRETCH && !catalog.variation(it.variationId!!)!!.optIn })
        assertEquals(p.blocks.sumOf { it.durationSeconds }, p.plannedDurationSeconds)
        assertEquals("the cool-down is the last part", BlockType.COOLDOWN, p.blocks.last().type)
    }

    @Test fun timedModeIsOneRelaxBlockOfTheChosenMinutesAndCountsInTheEstimate() {
        val base = plan(TrainSettings(rounds = 2)).plannedDurationSeconds
        val p = plan(TrainSettings(rounds = 2, cooldownOn = true, cooldownMode = CooldownMode.TIMED))
        val c = cool(p)
        assertEquals(1, c.size); assertNull(c.single().variationId); assertEquals(5 * 60, c.single().durationSeconds)
        assertEquals(base + 5 * 60, p.plannedDurationSeconds)
        assertEquals(8 * 60, cool(plan(TrainSettings(rounds = 2, cooldownOn = true, cooldownMode = CooldownMode.TIMED, cooldownMinutes = 8))).single().durationSeconds)
        // the Preview's - / + wins over the setting
        assertEquals(7 * 60, cool(plan(TrainSettings(rounds = 2, cooldownOn = true, cooldownMode = CooldownMode.TIMED), PlanEdits(cooldownMinutes = 7))).single().durationSeconds)
    }

    @Test fun aStretchSwappedInThePreviewReplacesTheAutomaticList() {
        val auto = cool(plan(TrainSettings(rounds = 2, cooldownOn = true))).mapNotNull { it.variationId }.distinct()
        val other = catalog.variations.first { it.kind == Kind.STRETCH && !it.optIn && it.id !in auto }.id
        val swapped = listOf(other) + auto.drop(1)
        val c = cool(plan(TrainSettings(rounds = 2, cooldownOn = true), PlanEdits(cooldownStretches = swapped))).mapNotNull { it.variationId }.distinct()
        assertEquals(swapped, c)
    }

    @Test fun theHeaderCountsTheCooldownBlocks() {
        val p = plan(TrainSettings(rounds = 2, cooldownOn = true))
        val i = p.blocks.indexOfFirst { it.type == BlockType.COOLDOWN }
        val h = app.calisthenics.domain.session.headerFor(p, p.blocks[i], i)
        assertEquals(app.calisthenics.domain.session.HeaderKind.COOLDOWN, h.kind); assertEquals(1, h.set); assertEquals(cool(p).size, h.sets)
    }
}
