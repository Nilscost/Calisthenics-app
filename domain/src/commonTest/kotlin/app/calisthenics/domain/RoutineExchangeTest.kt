package app.calisthenics.domain

import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.equipment.SeedProfiles
import app.calisthenics.domain.model.Kind
import app.calisthenics.domain.model.SideMode
import app.calisthenics.domain.model.WorkoutFormat
import app.calisthenics.domain.planner.PlanResult
import app.calisthenics.domain.planner.TrainSettings
import app.calisthenics.domain.planner.buildTrainPlan
import app.calisthenics.domain.progression.ProgressSnapshot
import app.calisthenics.domain.routine.ExchangeExercise
import app.calisthenics.domain.routine.ExchangeRoutine
import app.calisthenics.domain.routine.closestExercises
import app.calisthenics.domain.routine.exampleRoutine
import app.calisthenics.domain.routine.exchangeOf
import app.calisthenics.domain.routine.exportGuide
import app.calisthenics.domain.routine.parseRoutines
import app.calisthenics.domain.routine.resolveSaved
import app.calisthenics.domain.routine.routinesToJson
import app.calisthenics.domain.routine.savedFromExchange
import app.calisthenics.domain.routine.settingsFor
import kotlin.test.Test

/** Routines to and from a chatbot: the guide is built from the catalog, the format parses back, bad input is explained, nothing is guessed. */
class RoutineExchangeTest {
    private val catalog = parseCatalog(fixtureText("content/starter/catalog.json"))
    private val none = ProgressSnapshot(emptyMap(), emptyMap(), emptyMap(), emptyList())
    private val home = SeedProfiles.home
    private val pullDay = """{"calisthenicsRoutine": 1, "name": "Pull day", "sets": 3, "format": "CIRCUIT",
        "exercises": [{"id": "pullup-full"}, {"id": "plank", "seconds": 30}, {"id": "walking-lunge", "reps": 8}]}"""

    private fun guideSections() = exportGuide(catalog).split("\n## ")

    @Test fun guideListsEveryNonRoutineOnlyStrengthExerciseOnceInTheMainTable() {
        val main = guideSections().first { it.startsWith("Exercise library") }
        val expected = catalog.variations.filter { (it.kind == Kind.REPS || it.kind == Kind.HOLD) && !it.routineOnly }
        assertTrue(expected.size > 300)
        for (v in expected) assertEquals(v.id, 1, main.lines().count { it.startsWith("| `${v.id}` |") })
        assertEquals(expected.size, main.lines().count { it.startsWith("| `") })
        // nothing routine-only or stretch in the main table
        for (v in catalog.variations.filter { it.routineOnly || it.kind == Kind.STRETCH || it.kind == Kind.MOBILITY }) assertEquals(v.id, 0, main.lines().count { it.startsWith("| `${v.id}` |") })
    }

    @Test fun routineOnlyAndStretchesHaveTheirOwnTables() {
        val sections = guideSections()
        val routineOnly = sections.first { it.startsWith("Routine-only") }
        val stretches = sections.first { it.startsWith("Stretches") }
        for (v in catalog.variations.filter { it.routineOnly }) assertEquals(v.id, 1, routineOnly.lines().count { it.startsWith("| `${v.id}` |") })
        for (v in catalog.variations.filter { it.kind == Kind.STRETCH || it.kind == Kind.MOBILITY }) assertEquals(v.id, 1, stretches.lines().count { it.startsWith("| `${v.id}` |") })
    }

    @Test fun guideRowCarriesSidesUnitAndFiveLevels() {
        val guide = exportGuide(catalog)
        fun row(id: String) = guide.lines().first { it.startsWith("| `$id` |") }.split("|").map { it.trim() }.filter { it.isNotEmpty() }
        val hold = catalog.variations.first { it.kind == Kind.HOLD && !it.routineOnly }
        assertEquals("seconds (hold)", row(hold.id)[2])
        val sided = catalog.variations.first { it.sideMode == SideMode.SPLIT && it.kind == Kind.REPS && !it.routineOnly }
        assertTrue(row(sided.id)[3].startsWith("per side"))
        val alt = catalog.variation("pushup-archer")!!
        assertEquals("per side (alternating)", row(alt.id)[3])
        val tiers = catalog.policyFor(alt)!!.tiers.sortedBy { it.index }
        assertEquals(tiers.map { if (it.target.max != null && it.target.max != it.target.value) "${it.target.value}-${it.target.max}" else "${it.target.value}" }, row(alt.id).takeLast(5))
        assertEquals(11, row(alt.id).size)
        // the per-side rule is spelled out
        assertTrue("per side" in guide.lines().first { it.startsWith("- A fixed number") })
    }

    @Test fun guideChangesWithTheCatalogItIsBuiltFrom() {
        val fewer = catalog.copy(variations = catalog.variations.filter { it.id != "pullup-full" })
        assertTrue("`pullup-full`" in exportGuide(catalog))
        assertFalse("`pullup-full`" in exportGuide(fewer))
        // a side mode read from the catalog at run time, never hard-coded
        // (walking-lunge is ALTERNATING in the 9.3 catalog, so the flip goes the other way: to no side mode)
        val flipped = catalog.copy(variations = catalog.variations.map { if (it.id == "walking-lunge") it.copy(sides = null) else it })
        assertTrue(exportGuide(catalog).lines().first { it.startsWith("| `walking-lunge` |") }.contains("per side (alternating)"))
        assertTrue(exportGuide(flipped).lines().first { it.startsWith("| `walking-lunge` |") }.contains(" total "))
    }

    @Test fun theExampleInTheGuideParses() {
        val guide = exportGuide(catalog)
        val result = parseRoutines(guide, catalog, home)
        assertEquals(emptyList<String>(), result.errors)
        assertTrue(result.canSave)
        assertEquals(exampleRoutine(catalog), result.routines.single())
        assertEquals(3, result.routines.single().exercises.size)
    }

    @Test fun theDocumentedRoutineParses() {
        val r = parseRoutines(pullDay, catalog, home)
        assertEquals(emptyList<String>(), r.errors)
        assertEquals(ExchangeRoutine("Pull day", 3, WorkoutFormat.CIRCUIT,
            listOf(ExchangeExercise("pullup-full"), ExchangeExercise("plank", seconds = 30), ExchangeExercise("walking-lunge", reps = 8))), r.routines.single())
    }

    @Test fun aFencedBlockInsideChatProseParses() {
        val chat = "Sure! Here is a pull day for you.\n\n```json\n$pullDay\n```\n\nStart light and add reps when it feels easy {not json}."
        assertEquals("Pull day", parseRoutines(chat, catalog).routines.single().name)
        // bare JSON in prose, and JSON that a chat app gave curly quotes
        assertEquals("Pull day", parseRoutines("Here you go: $pullDay Enjoy!", catalog).routines.single().name)
        val curly = pullDay.replace('"', '“').let { c -> c.replace("“", "\"") }.replace("\"", "”")
        assertTrue(parseRoutines(curly, catalog).canSave)
        // an unrelated JSON block before the routine is skipped
        assertEquals("Pull day", parseRoutines("```json\n{\"note\": 1}\n```\n```\n$pullDay\n```", catalog).routines.single().name)
    }

    @Test fun anArrayHoldsSeveralRoutines() {
        val two = "[$pullDay, ${pullDay.replace("Pull day", "Second")}]"
        val r = parseRoutines(two, catalog)
        assertEquals(listOf("Pull day", "Second"), r.routines.map { it.name })
    }

    @Test fun textWithoutARoutineIsReported() {
        assertFalse(parseRoutines("no json here", catalog).canSave)
        assertFalse(parseRoutines("{\"x\": 1}", catalog).canSave)
        assertTrue(parseRoutines("{\"calisthenicsRoutine\": 2, \"exercises\": []}", catalog).errors.single().contains("version"))
    }

    @Test fun anUnknownIdIsReportedWithTheClosestNames() {
        val r = parseRoutines("""{"calisthenicsRoutine": 1, "name": "x", "exercises": [{"id": "pullup-ful"}]}""", catalog)
        assertFalse(r.canSave)
        val msg = r.errors.single()
        assertTrue(msg, "unknown id \"pullup-ful\"" in msg)
        assertTrue(msg, "pullup-full" in msg)
        assertEquals(3, closestExercises("pushup-archr", catalog).size)
        assertEquals("pushup-archer", closestExercises("pushup-archr", catalog).first().id)
        assertEquals("pullup-full", closestExercises("pull up full", catalog).first().id)
    }

    @Test fun repsAndSecondsMustFitTheExerciseAndItsRange() {
        fun errors(ex: String) = parseRoutines("""{"calisthenicsRoutine": 1, "name": "x", "exercises": [$ex]}""", catalog).errors
        assertTrue(errors("""{"id": "pullup-full", "reps": 0}""").single().contains("out of range"))
        assertTrue(errors("""{"id": "pullup-full", "reps": 500}""").single().contains("out of range"))
        assertTrue(errors("""{"id": "plank", "seconds": 9999}""").single().contains("out of range"))
        // reps on a hold, seconds on a rep exercise, both, a stretch
        assertTrue(errors("""{"id": "plank", "reps": 10}""").single().contains("is a hold"))
        assertTrue(errors("""{"id": "pullup-full", "seconds": 10}""").single().contains("counts reps"))
        assertTrue(errors("""{"id": "pullup-full", "reps": 5, "seconds": 10}""").single().contains("not both"))
        assertTrue(errors("""{"id": "stretch-frog"}""").single().contains("stretch"))
        assertTrue(errors("""{"id": "pullup-full", "reps": "many"}""").single().contains("whole number"))
        assertEquals(emptyList<String>(), errors("""{"id": "pullup-full", "reps": 12}"""))
    }

    @Test fun badSetsFormatAndEmptyListAreReported() {
        fun errors(body: String) = parseRoutines("""{"calisthenicsRoutine": 1, "name": "x", $body}""", catalog).errors.joinToString()
        assertTrue(errors(""""sets": 40, "exercises": [{"id": "plank"}]""").contains("sets must be"))
        assertTrue(errors(""""format": "ZIGZAG", "exercises": [{"id": "plank"}]""").contains("CIRCUIT, PAIRS, STRAIGHT"))
        assertTrue(errors(""""exercises": []""").contains("at least one"))
        assertEquals(WorkoutFormat.STRAIGHT, parseRoutines("""{"calisthenicsRoutine": 1, "name": "x", "format": "straight", "exercises": [{"id": "plank"}]}""", catalog).routines.single().format)
    }

    @Test fun missingEquipmentWarnsButDoesNotBlock() {
        val needs = catalog.variations.first { it.kind == Kind.REPS && !it.routineOnly && it.equipmentAlternatives.none { s -> s.needs.isEmpty() && s.capabilities.isEmpty() } &&
            !app.calisthenics.domain.equipment.isAvailable(it, SeedProfiles.home) }
        val body = """{"calisthenicsRoutine": 1, "name": "x", "exercises": [{"id": "${needs.id}"}]}"""
        val withProfile = parseRoutines(body, catalog, SeedProfiles.home)
        assertTrue(withProfile.canSave)
        assertEquals(1, withProfile.warnings.size)
        assertTrue(withProfile.warnings.single().contains("does not have"))
        assertEquals(emptyList<String>(), parseRoutines(body, catalog).warnings)   // no profile known: nothing to check, nothing said
    }

    @Test fun aRoutineRoundTripsThroughASavedRoutineAndBack() {
        val parsed = parseRoutines(pullDay, catalog, home).routines.single()
        val saved = savedFromExchange(parsed, catalog, "rt-1", now = 5L)
        assertEquals("Pull day", saved.name)
        assertEquals(3, saved.sets)
        assertEquals(WorkoutFormat.CIRCUIT, saved.format)
        assertEquals(parsed, exchangeOf(saved, catalog))
        // and through JSON text
        assertEquals(parsed, parseRoutines(routinesToJson(listOf(parsed)), catalog).routines.single())
        assertEquals(listOf(parsed, parsed.copy(name = "B")), parseRoutines(routinesToJson(listOf(parsed, parsed.copy(name = "B"))), catalog).routines)
    }

    @Test fun theSavedRoutineBuildsAPlanWithFixedNumbersAndTheLevelsWhereNoneAreGiven() {
        val parsed = parseRoutines(pullDay, catalog, home).routines.single()
        val saved = savedFromExchange(parsed, catalog, "rt-1", now = 5L, base = TrainSettings(stretchOn = false))
        val r = resolveSaved(saved, settingsFor(saved, TrainSettings()))
        val plan = (buildTrainPlan(catalog, r.routine, none, home, r.settings, edits = r.edits) as PlanResult.Ready).plan
        val work = plan.blocks.filter { it.variationId != null && it.type == app.calisthenics.domain.model.BlockType.WORK }
        assertEquals(listOf("pullup-full", "plank", "walking-lunge"), work.filter { it.roundIndex == 1 }.map { it.variationId })
        assertEquals(3, work.map { it.roundIndex }.toSet().size)
        val byId = work.filter { it.roundIndex == 1 }.associateBy { it.variationId }
        assertEquals(30, byId["plank"]!!.target!!.value)
        assertEquals(8, byId["walking-lunge"]!!.target!!.value)
        assertTrue(byId["walking-lunge"]!!.freeTarget)
        // no number given: the person's current level (level 1 for a new person), progression untouched
        assertFalse(byId["pullup-full"]!!.freeTarget)
        assertEquals(catalog.policyForVariation("pullup-full")!!.tier(1).target.value, byId["pullup-full"]!!.target!!.value)
    }
}
