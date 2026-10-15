package app.calisthenics.domain

import app.calisthenics.domain.equipment.SeedProfiles
import app.calisthenics.domain.model.*
import app.calisthenics.domain.model.Target
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.progression.*
import app.calisthenics.domain.feedback.SessionEvidence
import app.calisthenics.domain.routine.SessionDraft
import org.junit.Assert.*
import org.junit.Test

class ExerciseRotationTest {
    private val main = Fx.strength("main", "pushup")
    private val alt = Fx.strength("alternate", "pushup")
    private val harder = Fx.strength("harder", "pushup")
    private val c = Catalog(1, listOf(main, alt, harder, Fx.stretch),
        listOf(Fx.policy("main", listOf("harder")), Fx.policy("alternate", listOf("harder")), Fx.policy("harder")),
        exerciseGroups = listOf(ExerciseGroup("push-stage", "main", listOf("alternate"))))
    private fun progress(count: Int) = ProgressSnapshot(emptyMap(), mapOf("pushup" to "main"), emptyMap(), emptyList(), mapOf("pushup" to count))

    @Test fun `history rotates alternatives while identical previews are stable`() {
        assertEquals("main", rotatedExercise(c, main, progress(0), SeedProfiles.home).id)
        assertEquals("alternate", rotatedExercise(c, main, progress(1), SeedProfiles.home).id)
        assertEquals("alternate", rotatedExercise(c, main, progress(1), SeedProfiles.home).id)
        assertEquals("main", rotatedExercise(c, main, progress(2), SeedProfiles.home).id)
    }

    @Test fun `unavailable excluded and locked alternatives are never rotated in`() {
        val equipment = alt.copy(equipmentAlternatives = listOf(RequirementSet(listOf(EquipmentNeed("rings")))))
        assertEquals("main", rotatedExercise(c.copy(variations = listOf(main, equipment, harder)), main, progress(1), SeedProfiles.travel).id)
        assertEquals("main", rotatedExercise(c, main, progress(1), SeedProfiles.home, setOf("alternate")).id)
        val locked = Fx.policy("alternate", rule = PrerequisiteRule(listOf(listOf(Predicate(variationTierMet = VariationTier("main", 5))))))
        assertEquals("main", rotatedExercise(c.copy(policies = c.policies.filter { it.variationId != "alternate" } + locked), main, progress(1), SeedProfiles.home).id)
    }

    @Test fun `main mastery advances without alternate mastery and keeps the alternate available`() {
        val p = ProgressionEngine(c).replay(emptyList(), listOf(UserAction.SelfAssessment(0, "main", 6)))
        assertEquals("harder", p.activeInFamily["pushup"])
        assertNull(p.variations["alternate"])
        assertEquals("alternate", rotatedExercise(c, harder, p.copy(familyWorkoutCounts = mapOf("pushup" to 1)), SeedProfiles.home).id)
    }

    @Test fun `alternate mastery does not advance the main branch`() {
        val p = ProgressionEngine(c).replay(emptyList(), listOf(UserAction.SelfAssessment(0, "main", 2), UserAction.SelfAssessment(0, "alternate", 6)))
        assertEquals("main", p.activeInFamily["pushup"])
        assertEquals(2, p.variations["main"]!!.tier)
        assertTrue(p.variations["alternate"]!!.achievedTiers.contains(5))
    }

    private fun evidence(id: String, variation: String, day: Int = 1, blocks: Int = 2, ruleMet: Boolean? = null) =
        SessionEvidence(id, variation, "pushup", day, 1, blocks, false, false, false, null, false, blocks, 0, 2, ruleMet = ruleMet)

    @Test fun `rotation counts workouts once across exercises and ignores skipped work`() {
        val e = listOf(evidence("one", "main"), evidence("one", "alternate"), evidence("skipped", "main", blocks = 0))
        val p = ProgressionEngine(c).replay(e, emptyList())
        assertEquals(1, p.familyWorkoutCounts["pushup"])
        assertEquals("main", ProgressionEngine(c).replay(listOf(evidence("one", "alternate")), emptyList()).activeInFamily["pushup"])
    }

    @Test fun `custom rule completion honors extra exercise prerequisites`() {
        val gate = PrerequisiteRule(listOf(listOf(Predicate(variationTierMet = VariationTier("alternate", 5)))))
        val gated = c.copy(policies = c.policies.map { if (it.variationId == "harder") it.copy(prerequisiteRule = gate) else it })
        val p = ProgressionEngine(gated).replay(listOf(evidence("one", "main", ruleMet = true)), emptyList())
        assertEquals("main", p.activeInFamily["pushup"])
        assertNull(p.variations["harder"])
        assertEquals(5, p.variations["main"]!!.earnedStars())
    }

    @Test fun `planner uses the alternate's own target and respects a manual swap`() {
        val p = ProgressionEngine(c).replay(emptyList(), listOf(UserAction.SelfAssessment(0, "main", 3), UserAction.SelfAssessment(0, "alternate", 1)))
            .copy(familyWorkoutCounts = mapOf("pushup" to 1))
        val r = Routine("test", 1, "Test", listOf(RoutineSlot("push", Pattern.PUSH_HORIZONTAL, Area.UPPER_BODY, "main")))
        val d = SessionDraft(600, false, emptySet(), false, false, setOf(StrengthFocus.FULL_BODY), SeedProfiles.home.id, rounds = 1)
        val i = PlanInput("plan", 0, 0, c, r, d, SeedProfiles.home, progress = p)
        val block = (generate(i) as PlanResult.Ready).plan.blocks.first { it.type == BlockType.WORK }
        assertEquals("alternate", block.variationId)
        assertEquals(5, block.target!!.value)
        assertEquals("main", (generate(i.copy(draft = d.copy(swaps = mapOf("push" to "main")))) as PlanResult.Ready).plan.blocks.first { it.type == BlockType.WORK }.variationId)
        val skillRoutine = r.copy(slots = r.slots.map { it.copy(id = "goal") })
        val skillBlock = (generate(i.copy(routine = skillRoutine)) as PlanResult.Ready).plan.blocks.first { it.type == BlockType.WORK }
        assertEquals("alternate", skillBlock.variationId)
        assertEquals("main", p.activeInFamily["pushup"])
    }

    @Test fun `explosive cap applies to custom ranges and free targets but not uncapped exercises`() {
        val capped = Fx.policy("main").copy(maxRepsPerSet = 3)
        assertEquals(Target(TargetType.REPS, 3, 3), cappedTarget(Target(TargetType.REPS, 5, 8), capped))
        assertEquals(Target(TargetType.REPS, 3), cappedTarget(Target(TargetType.REPS, 10), capped))
        assertEquals(Target(TargetType.REPS, 10), cappedTarget(Target(TargetType.REPS, 10), Fx.policy("main")))
        val r = Routine("test", 1, "Test", listOf(RoutineSlot("push", Pattern.PUSH_HORIZONTAL, Area.UPPER_BODY, "main")))
        val d = SessionDraft(600, false, emptySet(), false, false, setOf(StrengthFocus.FULL_BODY), SeedProfiles.home.id, rounds = 1,
            rule = ProgressionRule(RuleKind.CUSTOM, from = 5, to = 8), freeTargets = mapOf("push" to 12))
        val plan = (generate(PlanInput("plan", 0, 0, c.copy(policies = c.policies.filter { it.variationId != "main" } + capped), r, d, SeedProfiles.home)) as PlanResult.Ready).plan
        assertEquals(3, plan.blocks.first { it.type == BlockType.WORK }.target!!.shownValue())
    }
}
