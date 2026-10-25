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

    // Preview: swapping a hand-added stretch replaces it in place; the swapped-away one does not come back after another removal
    @Test fun swappingAHandAddedCooldownStretchReplacesItInPlaceAndItStaysGone() {
        val settings = TrainSettings(rounds = 2, cooldownOn = true)
        val auto = cool(plan(settings)).mapNotNull { it.variationId }.distinct()
        val unused = catalog.variations.filter { it.kind == Kind.STRETCH && !it.optIn && it.id !in auto && !it.unilateral }.map { it.id }
        val added = unused[0]; val pick = unused[1]
        var edits = PlanEdits(cooldownAdded = listOf(added))
        val shown = cool(plan(settings, edits)).mapNotNull { it.variationId }.distinct()
        assertEquals(auto + added, shown)
        edits = edits.swapCooldownStretch(shown, shown.indexOf(added), pick)
        assertEquals(auto + pick, cool(plan(settings, edits)).mapNotNull { it.variationId }.distinct())
        // another removal afterwards: the swapped-away stretch stays gone, the swapped-in one is not doubled
        val afterRemoval = edits.copy(cooldownRemoved = edits.cooldownRemoved + auto[0])
        assertEquals(auto.drop(1) + pick, cool(plan(settings, afterRemoval)).mapNotNull { it.variationId }.distinct())
        assertEquals(1, cool(plan(settings, afterRemoval)).count { it.variationId == pick })
    }

    @Test fun swappingAnAutomaticCooldownStretchWithAnotherRemovalKeepsTheOriginalGone() {
        val settings = TrainSettings(rounds = 2, cooldownOn = true)
        val auto = cool(plan(settings)).mapNotNull { it.variationId }.distinct()
        val pick = catalog.variations.first { it.kind == Kind.STRETCH && !it.optIn && it.id !in auto }.id
        val swapped = PlanEdits().swapCooldownStretch(auto, 0, pick)
        assertEquals(listOf(pick) + auto.drop(1), cool(plan(settings, swapped)).mapNotNull { it.variationId }.distinct())
        val removed = swapped.copy(cooldownRemoved = swapped.cooldownRemoved + auto[1])
        assertEquals(listOf(pick) + auto.drop(2), cool(plan(settings, removed)).mapNotNull { it.variationId }.distinct())
    }

    @Test fun manuallyChosenUnilateralCooldownGetsFiveSecondsBeforeTheRightSide() {
        val id = "stretch-calf-wall"
        val v = catalog.variation(id)!!
        assertTrue(v.unilateral)
        val p = plan(
            TrainSettings(rounds = 2, cooldownOn = true, cooldownMode = CooldownMode.STRETCHING),
            PlanEdits(cooldownStretches = listOf(id)),
        )
        val pair = p.blocks.withIndex().filter { it.value.type == BlockType.COOLDOWN && it.value.variationId == id }
        assertEquals(listOf(Side.LEFT, Side.RIGHT), pair.map { it.value.side })
        assertEquals(pair.first().index + 2, pair.last().index)
        val setup = p.blocks[pair.first().index + 1]
        assertEquals(BlockType.TRANSITION, setup.type)
        assertEquals(SIDE_CHANGE_PREPARATION_SECONDS, setup.durationSeconds)
        assertEquals(listOf(v.defaultSeconds ?: 30, v.defaultSeconds ?: 30), pair.map { it.value.durationSeconds })
        assertEquals(p.blocks.sumOf { it.durationSeconds }, p.plannedDurationSeconds)
    }

    // R7.2-T04 (F5): remove, then add, then plan
    @Test fun aRemovedStretchLeavesTheCooldownAndAnAddedOneJoinsAtTheEnd() {
        val settings = TrainSettings(rounds = 2, cooldownOn = true)
        val auto = cool(plan(settings)).mapNotNull { it.variationId }.distinct()
        val gone = auto[1]
        val removed = cool(plan(settings, PlanEdits(cooldownRemoved = setOf(gone)))).mapNotNull { it.variationId }.distinct()
        assertEquals(auto - gone, removed)
        val extra = catalog.variations.first { it.kind == Kind.STRETCH && it.id !in auto }.id
        val both = cool(plan(settings, PlanEdits(cooldownRemoved = setOf(gone), cooldownAdded = listOf(extra)))).mapNotNull { it.variationId }.distinct()
        assertEquals(auto - gone + extra, both)
        // a one-sided stretch keeps both sides when added
        val sided = catalog.variations.first { it.kind == Kind.STRETCH && it.unilateral && it.id !in auto }
        assertEquals(2, cool(plan(settings, PlanEdits(cooldownAdded = listOf(sided.id)))).count { it.variationId == sided.id })
        // everything removed leaves an empty cool-down; adding a removed one again (removal wins only on the base list) brings it back
        assertTrue(cool(plan(settings, PlanEdits(cooldownRemoved = auto.toSet()))).isEmpty())
        assertEquals(listOf(gone), cool(plan(settings, PlanEdits(cooldownRemoved = auto.toSet() - gone))).mapNotNull { it.variationId }.distinct())
        // the total time follows
        assertTrue(plan(settings, PlanEdits(cooldownRemoved = setOf(gone))).plannedDurationSeconds < plan(settings).plannedDurationSeconds)
    }

    @Test fun removalsApplyToAHandPickedListToo() {
        val settings = TrainSettings(rounds = 2, cooldownOn = true)
        val auto = cool(plan(settings)).mapNotNull { it.variationId }.distinct()
        val c = cool(plan(settings, PlanEdits(cooldownStretches = auto, cooldownRemoved = setOf(auto.first())))).mapNotNull { it.variationId }.distinct()
        assertEquals(auto.drop(1), c)
    }

    @Test fun theHeaderCountsTheCooldownBlocks() {
        val p = plan(TrainSettings(rounds = 2, cooldownOn = true))
        val i = p.blocks.indexOfFirst { it.type == BlockType.COOLDOWN }
        val h = app.calisthenics.domain.session.headerFor(p, p.blocks[i], i)
        assertEquals(app.calisthenics.domain.session.HeaderKind.COOLDOWN, h.kind); assertEquals(1, h.set); assertEquals(cool(p).size, h.sets)
    }
}
