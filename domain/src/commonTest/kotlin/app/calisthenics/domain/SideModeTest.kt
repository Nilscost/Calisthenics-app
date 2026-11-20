package app.calisthenics.domain

import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.equipment.SeedProfiles
import app.calisthenics.domain.history.exerciseDetails
import app.calisthenics.domain.model.*
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.routine.SessionDraft
import app.calisthenics.domain.routine.StarterRoutine
import app.calisthenics.domain.session.CueText
import kotlin.test.Test

/** Owner 2026-10-26: a number is never ambiguous between "total" and "per side". SPLIT = two blocks, ALTERNATING = one block shown as the total with the per-side number. */
class SideModeTest {
    private val catalog = parseCatalog(fixtureText("content/starter/catalog.json"))
    private val names = catalog.variations.associate { it.id to it.name }

    private val alternatingIds = setOf(
        "pushup-archer", "ring-pushup-rto-archer", "pushup-single-leg", "pilates-single-leg-stretch", "pilates-scissors",
        "bicycle-crunch", "pilates-corkscrew", "pilates-saw", "pilates-double-leg-kick", "pilates-spine-twist",
        // 9.3 (docs/evidence/9.3-l1-side-modes.md)
        "walking-lunge", "russian-twist", "dead-bug", "dead-bug-single-leg", "bird-dog", "plank-arm-leg-reach", "plank-shoulder-tap",
        "windshield-wiper-floor", "windshield-wiper-bent-knee", "bird-dog-arm-raise", "bird-dog-leg-raise",
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
        // the owner counts a walking lunge and a Russian twist per side: one block, the total with the per-side number
        assertEquals(SideMode.ALTERNATING, catalog.variation("walking-lunge")!!.sideMode)
        assertEquals(SideMode.ALTERNATING, catalog.variation("russian-twist")!!.sideMode)
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
            FixedProgramItem("squat-air", reps = 10, estSeconds = 48),
        )
        val work = plan.blocks.filter { it.type == BlockType.WORK }
        assertEquals(listOf("lunge-reverse", "lunge-reverse", "pushup-archer", "squat-air"), work.map { it.variationId })
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
        assertTrue(defaultProgramItem(catalog, catalog.variation("walking-lunge")!!).perSide)
        assertFalse(defaultProgramItem(catalog, catalog.variation("squat-air")!!).perSide)
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

    /** Why a side-sounding exercise stays side mode NONE (9.3: docs/evidence/9.3-l1-side-modes.md). Everything else that says so must be SPLIT or ALTERNATING. */
    private val noneOnPurpose = mapOf(
        "stretch-back" to "cat-cow alternates round and arch, not sides",
        "warmup-dead-bug" to "timed warm-up (seconds), no rep count",
        "swimmer" to "timed in the Pilates programs, alternation inside a timed block",
        "wrist-seated-mobility" to "circles and bends in both directions, not sides",
        "neck-isometric-4-way" to "four directions, each held once",
        "shoulder-roll" to "a forward roll is one discrete skill rep; the side is chosen per roll",
        "hexagon-drill" to "per side of the taped hexagon, not of the body",
        "pullup-commando" to "numbers are whole pull-ups (profile 5-10), not per side; the head alternates each rep",
        "hanging-knee-raise-twisting" to "hanging core rep, numbers are total raises",
        "hanging-windshield-wiper-half" to "one rep is a full swing there and back",
        "ankle-hop-side-to-side" to "hops over a line, counted as hops",
        "bounding-alternate" to "bounds over a distance, counted as bounds",
        "box-push-off-alternating" to "plyometric, counted as jumps",
        "mountain-climber-slow" to "cadence drill, counted as steps",
        "windmill-army" to "routine-only Army drill keeps its source count",
        "turn-and-lunge" to "routine-only Army drill keeps its source count",
        "front-kick-toe-touch" to "routine-only Army drill keeps its source count",
        "leg-tuck-and-twist" to "routine-only Army drill keeps its source count",
        "side-to-side-knee-lifts" to "routine-only Army drill keeps its source count",
        "quarter-turn-jump" to "routine-only Army drill keeps its source count",
        "alternate-twist-jump" to "routine-only Army drill keeps its source count",
    )

    @Test fun nothingThatSaysItAlternatesSidesIsLeftWithoutASideMode() {
        val says = Regex("alternat|each side|side[ -]to[ -]side|per side", RegexOption.IGNORE_CASE)
        val bare = catalog.variations.filter { it.sideMode == SideMode.NONE && says.containsMatchIn(it.name + " " + it.instructions.joinToString(" ")) }
        assertEquals("add to ALTERNATING_IDS in tools/gen_r8_catalog.py or justify in noneOnPurpose", emptyList<String>(), bare.map { it.id }.filter { it !in noneOnPurpose })
        // the allow-list never goes stale: every entry exists, still sounds sided, and is still NONE
        for (id in noneOnPurpose.keys) {
            val v = catalog.variation(id) ?: fail("$id is gone from the catalog")
            assertEquals(id, SideMode.NONE, v.sideMode)
            assertTrue("$id no longer says anything about sides: drop it from the allow-list", says.containsMatchIn(v.name + " " + v.instructions.joinToString(" ")))
        }
    }

    @Test fun walkingLungeAndRussianTwistCarryTheirNumbersPerSide() {
        for (id in listOf("walking-lunge", "russian-twist")) {
            assertTrue(id, catalog.variation(id)!!.alternating)
            assertFalse(id, catalog.variation(id)!!.unilateral)
        }
        assertEquals(listOf(6, 8, 10, 12, 16), catalog.policyForVariation("walking-lunge")!!.tiers.map { it.target.value })
        assertEquals(listOf(8, 9, 10, 11, 12), catalog.policyForVariation("russian-twist")!!.tiers.map { it.target.value })
        val plan = program(FixedProgramItem("walking-lunge", reps = 6, perSide = true, estSeconds = 24))
        val lunge = plan.blocks.single { it.type == BlockType.WORK }
        assertTrue(lunge.alternating)
        assertEquals("12 reps, 6 per side", CueText.alternatingPhrase(lunge))
        assertEquals(Target(TargetType.REPS, 12), lunge.target!!.alternatingTotal())
    }

    @Test fun armyProgramsKeepTheirTenLungeTotalWrittenPerSide() {
        for (id in listOf("army-prt", "army-pd")) {
            for (exercise in listOf("walking-lunge", "windshield-wiper-bent-knee")) {
                val item = catalog.fixedPrograms.single { it.id == id }.items.single { it.exerciseId == exercise }
                assertEquals("$id/$exercise", 5, item.reps)
                assertTrue("$id/$exercise", item.perSide)
                assertEquals("$id/$exercise", 24, item.estSeconds)
            }
        }
        // a mountain climber stays a drill count of 10
        assertEquals(10, catalog.fixedPrograms.single { it.id == "army-prt" }.items.single { it.exerciseId == "mountain-climber" }.reps)
    }

    @Test fun shoulderTapNumbersAreNowPerSideAndKeepTheirTotals() {
        val v = catalog.variation("plank-shoulder-tap")!!
        assertTrue(v.alternating)
        val perSide = catalog.policyForVariation("plank-shoulder-tap")!!.tiers.map { it.target.value }
        assertEquals(listOf(5, 6, 7, 8, 10), perSide)
        // the totals the screen shows are the 9.2 numbers
        assertEquals(listOf(10, 12, 14, 16, 20), perSide.map { Target(TargetType.REPS, it).alternatingTotal().value })
    }
}
