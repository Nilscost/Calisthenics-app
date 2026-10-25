package app.calisthenics.domain

import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.equipment.SeedProfiles
import app.calisthenics.domain.equipment.isAvailable
import app.calisthenics.domain.feedback.*
import app.calisthenics.domain.goals.Goals
import app.calisthenics.domain.intake.*
import app.calisthenics.domain.model.*
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.progression.*
import app.calisthenics.domain.routine.StarterRoutine
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/**
 * Owner decisions (planner round 2): the first workouts prescribe exactly what the person answered at the start (also a route alternate), per family;
 * skill goals keep their own pull/hang; a default workout is 30 to 45 minutes; no pull equipment gives floor back work; notes are plain.
 */
class FirstWorkoutAnswersTest {
    private val catalog = parseCatalog(File(System.getProperty("repo.root"), "content/starter/catalog.json").readText())
    private val home = SeedProfiles.home
    private val bar = EquipmentProfile("bar", "Bar", listOf(EquipmentItem("pullup-bar"), EquipmentItem("mat")), setOf("floor-space", "wall"))
    private val band = EquipmentProfile("band", "Band", listOf(EquipmentItem("resistance-band", quantity = 1, suitability = setOf("stable-anchor")), EquipmentItem("mat")), setOf("floor-space", "wall"))
    private val bare = EquipmentProfile("bare", "Bare", emptyList(), setOf("floor-space", "wall"))
    private fun fam(id: String) = catalog.onboardingFamilies.first { it.id == id }

    /** What the app does after the questionnaire: levels -> the usual plan -> progress -> the Train tab's default plan. */
    private class First(val levels: List<StartLevel>, val routine: Routine, val progress: ProgressSnapshot, val settings: TrainSettings, val plan: WorkoutPlan)

    private fun first(answers: Map<String, FamilyAnswer>, profile: EquipmentProfile = home, goal: String = "general", rounds: Int = DEFAULT_TRAIN_ROUNDS): First {
        val levels = answers.mapNotNull { (f, a) -> levelFor(catalog, fam(f), a) { isAvailable(it, profile) } }
        val routine = routineFromLevels(catalog, StarterRoutine.routine, levels)
        val progress = ProgressionEngine(catalog).replay(emptyList(), levels.map { selfAssessmentFor(it.variationId, it.storedTier) })
        val settings = withDefaultLength(catalog, routine, progress, profile, TrainSettings(goalId = goal, profileId = profile.id, rounds = rounds))
        val plan = (buildTrainPlan(catalog, routine, progress, profile, settings) as PlanResult.Ready).plan
        return First(levels, routine, progress, settings, plan)
    }

    private fun WorkoutPlan.firstRound() = blocks.filter { it.type == BlockType.WORK && it.roundIndex == 1 }
    private val jargon = listOf("tier", "pull horizontal", "pull vertical", "qualifying", "streak", "passive", "NOT the same", "slot", "progression", "assessed", "route")
    private fun assertPlain(p: WorkoutPlan, what: String) {
        for (n in p.notes) for (j in jargon) assertFalse("$what: \"${n.text}\" contains \"$j\"", n.text.contains(j, ignoreCase = true))
        assertEquals(p.notes.map { it.text }.toSet(), (p.warnings + p.changesExplained).toSet())
    }

    /** [mayBeDropped]: answered extras (hinge, shoulders) that a default of 30 to 45 minutes may leave out when everything is answered at once. */
    private fun assertExact(label: String, f: First, mayBeDropped: Set<String> = emptySet()) {
        val round = f.plan.firstRound()
        for (l in f.levels) {
            if (l.completed) continue   // a finished ladder moves on to the next exercise by design
            val b = round.firstOrNull { it.variationId == l.variationId }
            if (b == null && familyOfAnswer(catalog, l.variationId)?.id in mayBeDropped) continue
            assertNotNull("$label: ${l.variationId} is in the first workout (got ${round.map { it.variationId }})", b)
            assertEquals("$label: ${l.variationId} level", l.tier, b!!.prescriptionTier)
            assertEquals("$label: ${l.variationId} target", catalog.policyForVariation(l.variationId)!!.tier(l.tier).target, b.target)
        }
        assertPlain(f.plan, label)
    }

    // ---- 1. the first workouts prescribe exactly the exercise and level that was answered ----

    @Test fun everyFamilyEveryGuessIsPrescribedExactly() {
        for (family in listOf("pushup", "squat", "pull", "core", "hinge", "shoulders")) for (guess in Guess.entries)
            assertExact("$family $guess", first(mapOf(family to FamilyAnswer.NotSure(guess))))
    }

    @Test fun everyFamilyIKnowAnswerIsPrescribedExactly() {
        val answers = listOf(
            "pushup" to FamilyAnswer.Does("pushup-knee", 9, 3), "pushup" to FamilyAnswer.Does("pushup-incline", 12, 3), "pushup" to FamilyAnswer.Does("pushup-standard", 10, 3),
            "pushup" to FamilyAnswer.Does("pushup-diamond", 8, 3),
            "squat" to FamilyAnswer.Does("squat-air", 15, 3), "squat" to FamilyAnswer.Does("split-squat", 8, 3), "squat" to FamilyAnswer.Does("lunge-reverse", 8, 3),
            "pull" to FamilyAnswer.Does("flexed-arm-hang", 10, 3), "pull" to FamilyAnswer.Does("pullup-band-assisted", 6, 3), "pull" to FamilyAnswer.Does("row-band", 10, 3),
            "pull" to FamilyAnswer.Does("pullup-full", 4, 3), "pull" to FamilyAnswer.Does("towel-door-row", 8, 3),
            "core" to FamilyAnswer.Does("hollow-hold", 40, 3), "core" to FamilyAnswer.Does("plank", 30, 3), "core" to FamilyAnswer.Does("hollow-hold", 12, 3),
            "hinge" to FamilyAnswer.Does("bridge-single-leg", 8, 3), "hinge" to FamilyAnswer.Does("glute-bridge", 15, 3),
            "shoulders" to FamilyAnswer.Does("pike-pushup", 8, 3), "shoulders" to FamilyAnswer.Does("pike-pushup-elevated", 5, 3),
        )
        for ((family, a) in answers) assertExact("$family ${(a as FamilyAnswer.Does).variationId} ${a.value}", first(mapOf(family to a), profile = EquipmentProfile("all", "All", home.items + EquipmentItem("chair", suitability = setOf("stable")), home.capabilities)))
    }

    @Test fun theSpecificReportedCasesAreFixed() {
        // Easy push = the knee push-up at step 3 with 9 reps, not the incline push-up at level 1 with 6
        val knee = first(mapOf("pushup" to FamilyAnswer.NotSure(Guess.EASY))).plan.firstRound().first { it.slotId == "push" }
        assertEquals("pushup-knee", knee.variationId); assertEquals(3, knee.prescriptionTier); assertEquals(9, knee.target!!.value)
        // flexed-arm hang and band-assisted answers keep their own exercise, with no false "finished" note
        for (v in listOf("flexed-arm-hang", "pullup-band-assisted")) {
            val f = first(mapOf("pull" to FamilyAnswer.Does(v, 4.coerceAtLeast(1), 3)))
            assertEquals(v, f.plan.firstRound().first { it.slotId == "pull" }.variationId)
            assertTrue(f.plan.notes.none { it.text.contains("finished every level") || it.text.contains("completed") })
            assertTrue(f.plan.firstRound().none { it.variationId == "pullup-negative" })
        }
        // an advanced hollow hold is prescribed, not a front plank
        val core = first(mapOf("core" to FamilyAnswer.Does("hollow-hold", 40, 3))).plan.firstRound()
        assertTrue(core.any { it.variationId == "hollow-hold" }); assertTrue(core.none { it.variationId == "plank" })
        // the shoulders answer has an effect
        val sh = first(mapOf("shoulders" to FamilyAnswer.Does("pike-pushup", 8, 3))).plan.firstRound()
        assertTrue(sh.any { it.variationId == "pike-pushup" })
    }

    @Test fun allFamiliesAtOnceAndTheSummaryAgreesWithThePlan() {
        val answers = mapOf("pushup" to FamilyAnswer.NotSure(Guess.EASY), "squat" to FamilyAnswer.NotSure(Guess.NORMAL), "pull" to FamilyAnswer.NotSure(Guess.HARD),
            "core" to FamilyAnswer.Does("hollow-hold", 25, 3), "hinge" to FamilyAnswer.NotSure(Guess.HARD), "shoulders" to FamilyAnswer.NotSure(Guess.NORMAL))
        val f = first(answers)
        assertEquals(6, f.levels.size)
        assertExact("all six", f, mayBeDropped = setOf("hinge", "shoulders"))
        assertTrue("${f.plan.plannedDurationSeconds / 60} min", f.plan.plannedDurationSeconds in MIN_SESSION_SECONDS..MAX_SESSION_SECONDS)
        // the mandatory four are always there, the optional extras only when the session stays within 45 minutes
        assertTrue(f.plan.firstRound().map { it.variationId }.containsAll(listOf("pushup-knee", "split-squat", "flexed-arm-hang", "hollow-hold")))
        assertFalse(f.plan.needsAcceptance)
    }

    @Test fun aRedoneQuestionnaireDropsAnOldShouldersAnswer() {
        val withShoulders = routineFromLevels(catalog, StarterRoutine.routine, listOf(StartLevel("pike-pushup", 3)))
        assertTrue(withShoulders.slots.any { it.id == "shoulders" })
        val redone = routineFromLevels(catalog, withShoulders, listOf(StartLevel("pushup-standard", 3)))
        assertTrue(redone.slots.none { it.id == "shoulders" })
        assertSame(StarterRoutine.routine, routineFromLevels(catalog, StarterRoutine.routine, listOf(StartLevel("squat-air", 3))))
    }

    @Test fun skillGoalSessionsKeepTheAnsweredExercisesOfTheirFamilies() {
        val f = first(mapOf("pushup" to FamilyAnswer.NotSure(Guess.EASY), "core" to FamilyAnswer.Does("hollow-hold", 25, 3)), goal = "front-lever")
        val round = f.plan.firstRound()
        assertTrue("the answered knee push-up is the push of a skill session: ${round.map { it.variationId }}", round.any { it.variationId == "pushup-knee" && it.prescriptionTier == 3 })
        assertTrue(round.none { it.variationId == "pushup-wall" || it.variationId == "pushup-incline" })
    }

    // ---- 2. the default length is 30 to 45 minutes ----

    @Test fun firstWorkoutsAreBetweenThirtyAndFortyFiveMinutes() {
        val cases = listOf(
            emptyMap(), mapOf("pushup" to FamilyAnswer.NotSure(Guess.EASY)), mapOf("pushup" to FamilyAnswer.NotSure(Guess.HARD), "core" to FamilyAnswer.NotSure(Guess.HARD)),
            mapOf("pull" to FamilyAnswer.NotSure(Guess.NORMAL), "hinge" to FamilyAnswer.NotSure(Guess.HARD), "squat" to FamilyAnswer.NotSure(Guess.NORMAL)),
        )
        for (profile in listOf(home, bar, band, bare)) for (rounds in 2..5) for ((i, a) in cases.withIndex()) {
            val p = first(a, profile, rounds = rounds).plan
            assertTrue("${profile.id} rounds=$rounds case=$i: ${p.plannedDurationSeconds / 60} min", p.plannedDurationSeconds in MIN_SESSION_SECONDS..MAX_SESSION_SECONDS)
        }
    }

    @Test fun skillGoalsAreInTheWindowWheneverThePoolAllows() {
        for (goal in listOf("front-lever", "muscle-up", "planche", "hspu", "handstand", "l-sit", "one-arm-pushup")) for (profile in listOf(home, bar)) {
            val p = first(emptyMap(), profile, goal).plan
            assertTrue("$goal ${profile.id}: ${p.plannedDurationSeconds / 60} min", p.plannedDurationSeconds in MIN_SESSION_SECONDS..MAX_SESSION_SECONDS)
        }
    }

    @Test fun afterTwelveWeeksOfProgressTheDefaultIsStillNotLongerThanFortyFiveMinutes() {
        // far up every ladder: long holds and big rep targets used to push the default past an hour
        val levels = catalog.onboardingFamilies.map { StartLevel(it.anchorVariationId, 5) }
        val progress = ProgressionEngine(catalog).replay(emptyList(), levels.map { selfAssessmentFor(it.variationId, it.storedTier) })
        val routine = routineFromLevels(catalog, StarterRoutine.routine, levels)
        val settings = withDefaultLength(catalog, routine, progress, home, TrainSettings())
        val p = (buildTrainPlan(catalog, routine, progress, home, settings) as PlanResult.Ready).plan
        assertTrue("${p.plannedDurationSeconds / 60} min", p.plannedDurationSeconds <= MAX_SESSION_SECONDS)
    }

    // ---- 3. no pull equipment: floor back work, not a push ----

    @Test fun noPullEquipmentGivesTheTowelRowNotAPushAndNeedsNoAcceptance() {
        val p = first(emptyMap(), bare).plan
        val pull = p.firstRound().first { it.slotId == "pull" }
        assertEquals("towel-door-row", pull.variationId)
        assertTrue(Pattern.PULL_HORIZONTAL in catalog.variation(pull.variationId!!)!!.patterns)
        assertFalse(p.needsAcceptance)
        assertEquals(NoteKind.EQUIPMENT, p.topNotes(1).single().kind)
        assertTrue(p.topNotes(1).single().text.startsWith("You don't have a resistance band"))
        assertEquals(1, p.firstRound().count { Pattern.PUSH_HORIZONTAL in catalog.variation(it.variationId!!)!!.patterns })   // the push slot only; the pull slot is not another push
        assertPlain(p, "bare")
    }

    @Test fun aPullUpAnswerWithoutABarFallsBackToBackWork() {
        // the usual plan points at the band-assisted pull-up; today there is no bar or band
        val routine = routineFromLevels(catalog, StarterRoutine.routine, listOf(StartLevel("pullup-band-assisted", 3)))
        val progress = ProgressionEngine(catalog).replay(emptyList(), listOf(selfAssessmentFor("pullup-band-assisted", 3)))
        val p = (buildTrainPlan(catalog, routine, progress, bare, withDefaultLength(catalog, routine, progress, bare, TrainSettings())) as PlanResult.Ready).plan
        val pull = p.firstRound().first { it.slotId == "pull" }
        assertEquals("towel-door-row", pull.variationId)
        assertFalse(p.needsAcceptance)
    }

    @Test fun thePronesAndThenTheBackExtensionsFollowWhenTheRowIsExcluded() {
        fun pullFor(excluded: Set<String>): String {
            val draft = app.calisthenics.domain.routine.SessionDraft.from(Preferences(), StarterRoutine.routine).copy(rounds = 2)
            val progress = ProgressSnapshot(emptyMap(), emptyMap(), emptyMap(), emptyList())
            val plan = (generate(PlanInput("t", 0, 0, catalog, StarterRoutine.routine, draft, bare, excludedVariationIds = excluded, progress = progress)) as PlanResult.Ready).plan
            assertFalse(plan.needsAcceptance)
            return plan.blocks.first { it.type == BlockType.WORK && it.slotId == "pull" }.variationId!!
        }
        assertEquals("towel-door-row", pullFor(emptySet()))
        val prone = pullFor(setOf("towel-door-row"))
        assertTrue(prone, prone.startsWith("prone-"))
        val extension = pullFor(setOf("towel-door-row") + catalog.variations.filter { it.id.startsWith("prone-") }.map { it.id })
        assertEquals("extension", catalog.progressionGroupOf(extension))
    }

    @Test fun theFloorBackExercisesHaveFullLevelsAndNoPrerequisites() {
        for (id in listOf("towel-door-row", "prone-i-raise", "prone-t-raise", "prone-y-raise", "prone-l-raise", "prone-w-raise", "superman-hold", "back-extension-floor", "swimmer")) {
            val v = catalog.variation(id)!!
            val policy = catalog.policyFor(v)
            assertNotNull(id, policy); assertEquals("$id has five levels", 5, policy!!.tiers.size)
            assertTrue("$id is open from the start", Goals.unmet(catalog, id, ProgressSnapshot(emptyMap(), emptyMap(), emptyMap(), emptyList())).isEmpty())
            assertTrue("$id needs no equipment", isAvailable(v, bare))
        }
    }

    // ---- 4. skill goals keep their own pull ----

    private fun hasPull(p: WorkoutPlan): Boolean = p.blocks.filter { it.type == BlockType.WORK }.mapNotNull { it.variationId }.any { id ->
        val v = catalog.variation(id)!!
        Pattern.PULL_VERTICAL in v.patterns || Pattern.PULL_HORIZONTAL in v.patterns
    }

    @Test fun skillWorkoutsContainTheirPullWheneverEquipmentAllows() {
        // (the muscle-up needs a bar: with a band alone there is no pull of the skill to train)
        for ((goal, profiles) in listOf("front-lever" to listOf(home, bar, band), "muscle-up" to listOf(home, bar))) for (profile in profiles) {
            val p = first(emptyMap(), profile, goal).plan
            assertTrue("$goal on ${profile.id}: ${p.firstRound().map { it.variationId }}", hasPull(p))
            assertEquals("goal", p.blocks.first { it.type == BlockType.WORK }.slotId.takeIf { it == "goal" } ?: p.blocks.first { it.type == BlockType.WORK }.slotId)
        }
        // with a bar, the hang or pull of the goal's own prerequisites; with a band, the band-assisted pull-up
        assertTrue(first(emptyMap(), bar, "front-lever").plan.firstRound().any { it.variationId == "dead-hang" })
        assertTrue(first(emptyMap(), home, "front-lever").plan.firstRound().any { it.variationId == "pullup-band-assisted" })
        assertTrue(first(emptyMap(), home, "muscle-up").plan.firstRound().any { it.variationId == "pullup-band-assisted" })
    }

    @Test fun skillExercisesFollowTheEquipmentThroughTheOpenPrerequisites() {
        val none = ProgressSnapshot(emptyMap(), emptyMap(), emptyMap(), emptyList())
        val fl = Goals.byId("front-lever")!!
        assertTrue("dead-hang" in Goals.skillExercises(catalog, fl, none, bar))
        assertFalse("pullup-band-assisted" in Goals.skillExercises(catalog, fl, none, bar))
        assertTrue("pullup-band-assisted" in Goals.skillExercises(catalog, fl, none, home))
    }

    @Test fun theSkillPullStaysAfterTheFirstSessionsToo() {
        // three completed sessions with a band: the band-assisted pull-up used to vanish once it had progress of its own
        for (goal in listOf("front-lever", "muscle-up")) {
            val engine = ProgressionEngine(catalog)
            val evidence = mutableListOf<SessionEvidence>()
            for (i in 0 until 3) {
                val progress = engine.replay(evidence, emptyList())
                val s = withDefaultLength(catalog, StarterRoutine.routine, progress, home, TrainSettings(goalId = goal))
                val plan = (buildTrainPlan(catalog, StarterRoutine.routine, progress, home, s, nowDay = i * 3) as PlanResult.Ready).plan
                assertTrue("$goal session ${i + 1}: ${plan.firstRound().map { it.variationId }}", hasPull(plan))
                val work = plan.blocks.filter { it.type == BlockType.WORK }
                val resolved = resolveFeedback(plan, work.associate { it.id to Execution.COMPLETED }, emptyMap(), work.associate { it.id to Feedback(Rating.MET, actualReps = it.target!!.value) })
                evidence += deriveEvidence("s$i", i * 3, plan, resolved, { catalog.variation(it)?.familyId ?: it },
                    { v, tier -> catalog.policyForVariation(v)?.tiers?.firstOrNull { it.index == tier }?.minQualifyingBlocks ?: 2 })
            }
        }
    }

    // ---- 5. plain notes with an importance ----

    @Test fun noteKindsAreOrderedByImportance() {
        assertEquals(listOf(NoteKind.EQUIPMENT, NoteKind.PAIN, NoteKind.MISSING_MOVEMENT), NoteKind.entries.take(3))
        assertEquals((1..NoteKind.entries.size).toList(), NoteKind.entries.map { it.priority })
        assertTrue(NoteKind.INFO.priority > NoteKind.COVERAGE.priority && NoteKind.COVERAGE.priority > NoteKind.LENGTH.priority)
        assertTrue(NoteKind.entries.filter { it.warning }.all { it.priority < NoteKind.COVERAGE.priority })
    }

    @Test fun topNotesPutsEquipmentAndMissingMovementBeforeBackground() {
        val base = first(emptyMap(), bare).plan
        val p = base.withNote(NoteKind.INFO, "Left out to fit the time: Plank.").withNote(NoteKind.MISSING_MOVEMENT, "Bear Crawl is a different kind of exercise.").withNote(NoteKind.COVERAGE, "No stretch for ankle in this workout.")
        assertEquals(listOf(NoteKind.EQUIPMENT, NoteKind.MISSING_MOVEMENT), p.topNotes(2).map { it.kind })
        assertEquals(NoteKind.INFO, p.notes.last().kind)
        assertTrue(p.warnings.contains("Bear Crawl is a different kind of exercise.")); assertTrue(p.changesExplained.contains("Left out to fit the time: Plank."))
        assertSame(p, p.withNote(NoteKind.INFO, "Left out to fit the time: Plank."))
    }

    @Test fun aWellFittingPlanHasNoNotesAtAll() {
        val p = first(mapOf("pushup" to FamilyAnswer.NotSure(Guess.NORMAL), "pull" to FamilyAnswer.NotSure(Guess.EASY))).plan
        assertTrue(p.notes.toString(), p.notes.isEmpty())
        assertTrue(p.warnings.isEmpty() && p.changesExplained.isEmpty())
    }

    @Test fun notesAreShortPlainSentencesAcrossManyPlans() {
        for (profile in listOf(home, bar, band, bare, SeedProfiles.travel)) for (goal in listOf("general", "front-lever", "muscle-up", "pistol", "body-upper")) {
            val r = buildTrainPlan(catalog, StarterRoutine.routine, ProgressSnapshot(emptyMap(), emptyMap(), emptyMap(), emptyList()), profile, TrainSettings(goalId = goal))
            if (r is PlanResult.Ready) { assertPlain(r.plan, "${profile.id}/$goal"); r.plan.notes.forEach { assertTrue(it.text, it.text.length < 200) } }
        }
    }

    @Test fun reentryAndUnavailableExercisesAreSaidInPlainWords() {
        val evs = listOf(0, 3, 7).map { SessionEvidence("s$it", "pushup-knee", "pushup", it, 2, 2, false, false, false, null, false, 2, 0, 2) }
        val progress = ProgressionEngine(catalog).replay(evs, listOf(UserAction.SelfAssessment(0, "pushup-knee", 2)))
        val routine = StarterRoutine.routine.copy(slots = StarterRoutine.routine.slots.map { if (it.id == "push") it.copy(preferredVariationId = "pushup-knee") else it })
        val p = (buildTrainPlan(catalog, routine, progress, home, TrainSettings(), nowDay = 30) as PlanResult.Ready).plan
        assertTrue(p.needsAcceptance)
        val back = p.notes.first { it.kind == NoteKind.RETURN }
        assertTrue(back.text, back.text.startsWith("Welcome back. You haven't done Knee Push-Up"))
        assertPlain(p, "reentry")
    }
}
