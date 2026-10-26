package app.calisthenics.domain

import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.equipment.SeedProfiles
import app.calisthenics.domain.history.exerciseDetails
import app.calisthenics.domain.model.*
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.routine.SessionDraft
import app.calisthenics.domain.routine.StarterRoutine
import app.calisthenics.domain.session.CueText
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** Owner 2026-10-26: a number is never ambiguous between "total" and "per side". SPLIT = two blocks, ALTERNATING = one block shown as the total with the per-side number. */
class SideModeTest {
    private val catalog = parseCatalog(File(System.getProperty("repo.root"), "content/starter/catalog.json").readText())
    private val names = catalog.variations.associate { it.id to it.name }

    private val alternatingIds = setOf(
        "pushup-archer", "ring-pushup-rto-archer", "pushup-single-leg", "pilates-single-leg-stretch", "pilates-scissors",
        "bicycle-crunch", "pilates-corkscrew", "pilates-saw", "pilates-double-leg-kick", "pilates-spine-twist",
    )

    @Test fun sideModeDerivesFromUnilateralUnlessExplicit() {
        val v = Fx.strength("x")
        assertEquals(SideMode.NONE, v.sideMode)
        assertEquals(SideMode.SPLIT, v.copy(unilateral = true).sideMode)
        assertEquals(SideMode.ALTERNATING, v.copy(sides = SideMode.ALTERNATING).sideMode)
        assertTrue(v.copy(sides = SideMode.ALTERNATING).alternating)
        assertFalse(v.copy(unilateral = true).alternating)
    }

    @Test fun catalogClassification() {
        assertEquals(alternatingIds, catalog.variations.filter { it.alternating }.map { it.id }.toSet())
        // ALTERNATING is never also split, and nothing alternating still says "(per side)" in its name: the target says it
        assertTrue(catalog.variations.filter { it.alternating }.none { it.unilateral || "per side" in it.name.lowercase() })
        // the owner's examples: the archer push-up is one block with a total, the reverse lunge is two blocks
        assertEquals("Archer Push-Up", catalog.variation("pushup-archer")!!.name)
        assertEquals(SideMode.ALTERNATING, catalog.variation("pushup-archer")!!.sideMode)
        assertEquals(SideMode.SPLIT, catalog.variation("lunge-reverse")!!.sideMode)
        // a walking lunge steps alternately and counts steps: its number is already the total, so nothing says "per side"
        assertEquals(SideMode.NONE, catalog.variation("walking-lunge")!!.sideMode)
        // the old catalog JSON without the field still decodes
        assertNull(catalog.variation("squat-air")!!.sides)
    }

    @Test fun everyProgramItemIsPerSideExactlyWhenItsExerciseIsSided() {
        for (p in catalog.fixedPrograms) for (item in p.items) {
            val v = catalog.variation(item.exerciseId)!!
            assertEquals("${p.id}/${item.exerciseId}", v.sideMode != SideMode.NONE, item.perSide)
        }
    }

    private fun program(vararg items: FixedProgramItem) =
        (buildProgramPlan(catalog, ProgramContent("t", "t", items.toList()), SeedProfiles.home, flow = false) as FixedProgramPlanResult.Ready).plan

    @Test fun splitProgramItemIsTwoBlocksAndAlternatingIsOneTotalBlock() {
        val plan = program(
            FixedProgramItem("lunge-reverse", reps = 6, perSide = true, estSeconds = 24),
            FixedProgramItem("pushup-archer", reps = 6, perSide = true, estSeconds = 24),
            FixedProgramItem("walking-lunge", reps = 10, estSeconds = 48),
        )
        val work = plan.blocks.filter { it.type == BlockType.WORK }
        assertEquals(listOf("lunge-reverse", "lunge-reverse", "pushup-archer", "walking-lunge"), work.map { it.variationId })
        assertEquals(listOf(Side.LEFT, Side.RIGHT, Side.NONE, Side.NONE), work.map { it.side })
        assertEquals(listOf(false, false, true, false), work.map { it.alternating })
        // the target stays the per-side number; the alternating block is twice as long as the per-side estimate
        assertEquals(listOf(6, 6, 6, 10), work.map { it.target!!.value })
        assertEquals(listOf(24, 24, 48, 48), work.map { it.durationSeconds })
    }

    @Test fun anItemFlaggedPerSideOnAnUnsidedExerciseIsNeverSplit() {
        val work = program(FixedProgramItem("squat-air", reps = 8, perSide = true, estSeconds = 24)).blocks.filter { it.type == BlockType.WORK }
        assertEquals(listOf(Side.NONE), work.map { it.side })
        assertFalse(work.single().alternating)
    }

    @Test fun defaultProgramItemIsPerSideForSplitAndAlternating() {
        assertTrue(defaultProgramItem(catalog, catalog.variation("lunge-reverse")!!).perSide)
        assertTrue(defaultProgramItem(catalog, catalog.variation("pushup-archer")!!).perSide)
        assertFalse(defaultProgramItem(catalog, catalog.variation("walking-lunge")!!).perSide)
    }

    @Test fun plannerMakesOneAlternatingBlockWithTwiceTheWindow() {
        fun plan(c: Catalog) = (generate(PlanInput("p", 0L, 0, c, StarterRoutine.routine,
            SessionDraft.from(Preferences(stretchOn = false), StarterRoutine.routine).copy(rounds = 1), SeedProfiles.home)) as PlanResult.Ready).plan
        val base = plan(catalog)
        val v = base.blocks.first { it.type == BlockType.WORK && it.side == Side.NONE }
        val alt = plan(catalog.copy(variations = catalog.variations.map { if (it.id == v.variationId) it.copy(sides = SideMode.ALTERNATING) else it }))
        val before = base.blocks.single { it.id == v.id }
        val after = alt.blocks.single { it.id == v.id }
        assertFalse(before.alternating); assertTrue(after.alternating)
        assertEquals(before.target, after.target)
        assertEquals(before.durationSeconds * 2, after.durationSeconds)
        assertEquals(Side.NONE, after.side)
    }

    @Test fun targetTotalDoublesBothEndsOfARange() {
        assertEquals(Target(TargetType.REPS, 12), Target(TargetType.REPS, 6).alternatingTotal())
        assertEquals(Target(TargetType.REPS, 10, 16), Target(TargetType.REPS, 5, 8).alternatingTotal())
        assertEquals(Target(TargetType.HOLD_SECONDS, 40), Target(TargetType.HOLD_SECONDS, 20).alternatingTotal())
    }

    @Test fun voiceCueGivesTotalAndPerSideForAlternatingOnly() {
        val plan = program(
            FixedProgramItem("pushup-archer", reps = 6, perSide = true, estSeconds = 24),
            FixedProgramItem("lunge-reverse", reps = 6, perSide = true, estSeconds = 24),
            FixedProgramItem("plank", seconds = 20),
        ).let { p -> p.copy(blocks = p.blocks + TimelineBlock("alt-hold", BlockType.WORK, 50, variationId = "pushup-archer", target = Target(TargetType.HOLD_SECONDS, 20), alternating = true)) }
        val archer = plan.blocks.first { it.variationId == "pushup-archer" }
        assertEquals("12 reps, 6 per side", CueText.alternatingPhrase(archer))
        assertEquals("Archer Push-Up. 12 reps, 6 per side", CueText.spoken(plan, archer.id, names))
        assertEquals("40 seconds, 20 seconds per side", CueText.alternatingPhrase(plan.blocks.last()))
        val left = plan.blocks.first { it.side == Side.LEFT }
        assertNull(CueText.alternatingPhrase(left))
        assertEquals("Reverse Lunge — Left side", CueText.spoken(plan, left.id, names))
    }

    @Test fun historyDetailKnowsWhichExercisesAreAlternating() {
        val plan = program(FixedProgramItem("pushup-archer", reps = 6, perSide = true, estSeconds = 24), FixedProgramItem("lunge-reverse", reps = 6, perSide = true, estSeconds = 24))
        val records = plan.blocks.filter { it.type == BlockType.WORK }.map { app.calisthenics.domain.history.BlockRecord(it.id, it.variationId, "WORK", "MET", 20, 6) }
        val details = exerciseDetails(plan, records).associateBy { it.variationId }
        assertTrue(details.getValue("pushup-archer").alternating)
        assertFalse(details.getValue("lunge-reverse").alternating)
        assertEquals(2, details.getValue("lunge-reverse").rounds.size)
    }

    @Test fun oldSavedBlocksWithoutTheFlagDecodeAsNotAlternating() {
        val json = """{"id":"b","type":"WORK","durationSeconds":30}"""
        assertFalse(kotlinx.serialization.json.Json { ignoreUnknownKeys = true }.decodeFromString(TimelineBlock.serializer(), json).alternating)
    }
}
