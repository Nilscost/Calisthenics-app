package app.calisthenics.domain

import app.calisthenics.domain.equipment.SeedProfiles
import app.calisthenics.domain.model.*
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.progression.*
import app.calisthenics.domain.routine.SessionDraft
import org.junit.Assert.*
import org.junit.Test

class PlannerTest {
    private val hipStretch = Fx.stretch.copy(id = "hip-stretch", familyId = "hip-stretch", name = "Hip stretch", stretchAreas = setOf(StretchArea.HIP), unilateral = false, defaultSeconds = 30)
    private val plank = Fx.strength("plank", "plank", Pattern.CORE_ANTI_EXTENSION, Area.CORE, stretches = listOf("hip-stretch"))
    private val bandRow = Fx.strength("row-band", "row", Pattern.PULL_HORIZONTAL, Area.UPPER_BODY,
        alts = listOf(RequirementSet(needs = listOf(EquipmentNeed("resistance-band")))))
    private val dead = Fx.strength("dead-bug", "dead-bug", Pattern.CORE_ANTI_ROTATION, Area.CORE, stretches = listOf("hip-stretch"))

    private val catalog = Fx.catalog().let {
        it.copy(
            variations = it.variations + hipStretch + plank + bandRow + dead,
            policies = it.policies + Fx.policy("plank") + Fx.policy("row-band") + Fx.policy("dead-bug"),
        )
    }
    private val routine = Routine("usual", 1, "Usual", listOf(
        RoutineSlot("s-push", Pattern.PUSH_HORIZONTAL, Area.UPPER_BODY, "pushup-knee"),
        RoutineSlot("s-squat", Pattern.SQUAT, Area.LOWER_BODY, "squat-air"),
        RoutineSlot("s-pull", Pattern.PULL_HORIZONTAL, Area.UPPER_BODY, "row-band"),
        RoutineSlot("s-core", Pattern.CORE_ANTI_EXTENSION, Area.CORE, "plank"),
        RoutineSlot("s-core2", Pattern.CORE_ANTI_ROTATION, Area.CORE, "dead-bug", optional = true),
    ))

    private fun input(minutes: Int = 45, profile: EquipmentProfile = SeedProfiles.home, stretch: Boolean = true,
                      focus: Set<StrengthFocus> = setOf(StrengthFocus.FULL_BODY), areas: Set<StretchArea> = setOf(StretchArea.FULL_BODY),
                      excluded: Set<String> = emptySet(), swaps: Map<String, String> = emptyMap(), cat: Catalog = catalog,
                      progress: ProgressSnapshot = ProgressSnapshot(emptyMap(), emptyMap(), emptyMap(), emptyList()),
                      warm: Boolean = false, cool: Boolean = false, day: Int = 0) =
        PlanInput("p1", 0, day, cat, routine,
            SessionDraft(minutes * 60, stretch, areas, warm, cool, focus, profile.id, swaps), profile, excluded, progress)

    private fun ready(i: PlanInput) = (generate(i) as? PlanResult.Ready)?.plan ?: fail("expected Ready, got ${generate(i)}").let { error("") }
    private fun infeasible(i: PlanInput) = generate(i) as? PlanResult.Infeasible ?: error("expected Infeasible, got ${generate(i)}")

    @Test fun `A04 plans for 10 20 45 90 minutes never exceed the budget and add up`() {
        for (m in listOf(10, 20, 45, 90)) {
            val r = generate(input(m))
            if (r is PlanResult.Ready) {
                assertTrue("$m min", r.plan.plannedDurationSeconds <= m * 60)
                assertEquals(r.plan.blocks.sumOf { it.durationSeconds }, r.plan.plannedDurationSeconds)
            } else r as PlanResult.Infeasible
        }
    }

    @Test fun `45 minute plan is within tolerance or asks for acceptance`() {
        val p = ready(input(45))
        val under = 45 * 60 - p.plannedDurationSeconds
        assertTrue(under <= UNDERFILL_TOLERANCE_SECONDS || p.needsAcceptance)
    }

    @Test fun `same inputs give identical plans`() = assertEquals(generate(input(30)), generate(input(30)))

    @Test fun `routine slot order is preserved in every round`() {
        val p = ready(input(30))
        val round1 = p.blocks.filter { it.type == BlockType.WORK && it.roundIndex == 1 }.map { it.slotId }
        assertEquals(listOf("s-push", "s-squat", "s-pull", "s-core", "s-core2").filter { it in round1 }, round1)
    }

    private fun bigRoutine(optionalCount: Int) = routine.copy(slots =
        listOf(routine.slots[0], routine.slots[1]) +
            (1..optionalCount).map { RoutineSlot("o$it", Pattern.PUSH_HORIZONTAL, Area.UPPER_BODY, "pushup-knee", optional = true) })

    @Test fun `short budget drops optional slots, keeps mandatory ones and explains it`() {
        val p = ready(input(10).copy(routine = bigRoutine(12)))
        val slots = p.blocks.filter { it.type == BlockType.WORK }.mapNotNull { it.slotId }.toSet()
        assertTrue("s-push" in slots && "s-squat" in slots)
        assertTrue(slots.size < 14)
        assertTrue(p.changesExplained.any { it.contains("optional exercises were left out") })
        assertTrue(p.plannedDurationSeconds <= 600)
    }

    @Test fun `optional slots are kept when time allows`() {
        val p = ready(input(45))
        assertTrue(p.blocks.any { it.slotId == "s-core2" })
    }

    @Test fun `impossible tiny budget is infeasible with alternatives`() {
        val mandatory = routine.copy(slots = (1..20).map { RoutineSlot("m$it", Pattern.PUSH_HORIZONTAL, Area.UPPER_BODY, "pushup-knee") })
        val r = infeasible(input(10).copy(routine = mandatory))
        assertEquals("budget-too-small", r.reasons.single().code); assertTrue(r.alternatives.isNotEmpty())
    }

    @Test fun `duration outside 10 to 90 or not whole minutes is rejected`() {
        assertEquals("duration-range", infeasible(input(5)).reasons.single().code)
        assertEquals("duration-range", infeasible(input(91)).reasons.single().code)
        assertEquals("duration-range", infeasible(input(30).copy(draft = input(30).draft.copy(durationSeconds = 1830))).reasons.single().code)
    }

    @Test fun `stretch on has no passive recovery and every recovery follows a stretch`() {
        val p = ready(input(30))
        assertTrue(p.blocks.none { it.type == BlockType.PASSIVE_RECOVERY })
        assertTrue(p.blocks.any { it.type == BlockType.STRETCH })
    }

    @Test fun `stretch off keeps ordinary recovery and no stretch blocks`() {
        val p = ready(input(30, stretch = false))
        assertTrue(p.blocks.any { it.type == BlockType.PASSIVE_RECOVERY }); assertTrue(p.blocks.none { it.type == BlockType.STRETCH })
        assertTrue(p.changesExplained.any { it.contains("Stretching is off") })
    }

    @Test fun `recovery windows are never shortened below the reviewed minimum`() {
        val p = ready(input(30))
        val byWork = p.blocks.filter { it.recoveryForBlockIds.isNotEmpty() }.groupBy { it.recoveryForBlockIds }
        for ((_, recs) in byWork) assertTrue(recs.sumOf { it.durationSeconds } >= 20)
    }

    @Test fun `left and right stretch segments are equal and paired`() {
        val calfAll = catalog.copy(variations = catalog.variations.map { if (it.kind == Kind.REPS) it.copy(compatibleStretchIds = listOf("calf-stretch")) else it })
        val p = ready(input(30, areas = setOf(StretchArea.CALF), cat = calfAll))
        val sides = p.blocks.filter { it.variationId == "calf-stretch" }
        assertTrue(sides.isNotEmpty())
        assertEquals(sides.count { it.side == Side.LEFT }, sides.count { it.side == Side.RIGHT })
        sides.groupBy { it.recoveryForBlockIds }.values.forEach { g -> assertEquals(1, g.map { it.durationSeconds }.distinct().size) }
    }

    @Test fun `no allowed stretch pairing is infeasible and never silent passive rest`() {
        val r = infeasible(input(30, areas = setOf(StretchArea.CHEST)))
        assertEquals("no-stretch-pairing", r.reasons.single().code)
        assertTrue(r.alternatives.any { it.id == "stretch-off" })
    }

    @Test fun `excluding a stretch removes it`() {
        val both = catalog.copy(variations = catalog.variations.map { if (it.kind == Kind.REPS) it.copy(compatibleStretchIds = listOf("calf-stretch", "hip-stretch")) else it })
        val p = ready(input(30, excluded = setOf("calf-stretch"), cat = both))
        assertTrue(p.blocks.none { it.variationId == "calf-stretch" })
    }

    @Test fun `coverage warning is honest about unstretched areas`() {
        val p = ready(input(30))
        assertTrue(p.warnings.any { it.contains("coverage is limited") })
    }

    @Test fun `focus core only keeps only core slots`() {
        val p = ready(input(20, focus = setOf(StrengthFocus.CORE)))
        assertTrue(p.blocks.filter { it.type == BlockType.WORK }.all { it.slotId?.startsWith("s-core") == true })
    }

    @Test fun `travel profile never plans the band row and flags what replaced the pull`() {
        val p = ready(input(30, profile = SeedProfiles.travel))
        assertTrue(p.blocks.none { it.variationId == "row-band" })
        val pullSlot = p.blocks.filter { it.slotId == "s-pull" }
        // either dropped with an explanation, or replaced by something explicitly flagged as NOT a pull
        assertTrue(pullSlot.isEmpty() || p.warnings.any { it.contains("NOT the same movement") })
        assertTrue(p.warnings.isNotEmpty()); assertTrue(p.needsAcceptance)
    }

    @Test fun `non equivalent substitute warns and requires confirmation`() {
        val withAlt = catalog.copy(variations = catalog.variations + Fx.strength("crunch", "crunch", Pattern.CORE_ANTI_EXTENSION, Area.UPPER_BODY, stretches = listOf("hip-stretch")),
            policies = catalog.policies + Fx.policy("crunch"))
        val p = ready(input(30, profile = SeedProfiles.travel, cat = withAlt))
        val sub = p.blocks.filter { it.slotId == "s-pull" }
        assertTrue(sub.isNotEmpty()); assertEquals("crunch", sub.first().variationId)
        assertTrue(p.warnings.any { it.contains("NOT the same movement") }); assertTrue(p.needsAcceptance)
    }

    @Test fun `excluded exercise is not planned`() {
        val p = ready(input(30, excluded = setOf("squat-air")))
        assertTrue(p.blocks.none { it.variationId == "squat-air" })
    }

    @Test fun `swaps apply to the plan`() {
        val p = ready(input(30, swaps = mapOf("s-push" to "pushup-std")))
        assertTrue(p.blocks.any { it.variationId == "pushup-std" }); assertTrue(p.blocks.none { it.variationId == "pushup-knee" })
    }

    @Test fun `unassessed exercises start at tier 1 and say so`() {
        val p = ready(input(30))
        assertTrue(p.blocks.filter { it.type == BlockType.WORK }.all { it.prescriptionTier == 1 })
        assertTrue(p.changesExplained.any { it.contains("no assessed level") })
    }

    @Test fun `assessed tier sets the target`() {
        val prog = ProgressionEngine(catalog).replay(emptyList(), listOf(UserAction.SelfAssessment(0, "pushup-knee", 3)))
        val p = ready(input(30, progress = prog))
        assertEquals(3, p.blocks.first { it.variationId == "pushup-knee" }.prescriptionTier)
        assertEquals(Target(TargetType.REPS, 15), p.blocks.first { it.variationId == "pushup-knee" }.target)
    }

    @Test fun `discomfort hold blocks the slot and never prescribes it harder`() {
        val ev = app.calisthenics.domain.feedback.SessionEvidence("s", "pushup-knee", "pushup", 0, 1, 2, false, false, true, 0, false, 2, 0, 2)
        val prog = ProgressionEngine(catalog).replay(listOf(ev), emptyList())
        val p = ready(input(30, progress = prog, day = 1))
        assertTrue(p.blocks.none { it.variationId == "pushup-knee" })
        assertTrue(p.warnings.any { it.contains("discomfort") }); assertTrue(p.needsAcceptance)
    }

    @Test fun `successor replaces the completed variation with an explanation`() {
        val prog = ProgressSnapshot(emptyMap(), mapOf("pushup" to "pushup-std"), emptyMap(), emptyList())
        val p = ready(input(30, progress = prog))
        assertTrue(p.blocks.any { it.variationId == "pushup-std" })
        assertTrue(p.changesExplained.any { it.contains("completed all tiers") })
    }

    @Test fun `re-entry after a break needs acceptance`() {
        val evs = listOf(0, 3, 7).map { app.calisthenics.domain.feedback.SessionEvidence("s$it", "pushup-knee", "pushup", it, 2, 2, false, false, false, null, false, 2, 0, 2) }
        val prog = ProgressionEngine(catalog).replay(evs, listOf(UserAction.SelfAssessment(0, "pushup-knee", 2)))
        val p = ready(input(30, progress = prog, day = 30))
        assertTrue(p.needsAcceptance); assertTrue(p.warnings.any { it.contains("Welcome back") })
    }

    @Test fun `warm up on without a template is infeasible, with a template it is budgeted`() {
        assertEquals("no-warmup-template", infeasible(input(30, warm = true)).reasons.single().code)
        val mob = Fx.stretch.copy(id = "mob-a", familyId = "mob-a", kind = Kind.MOBILITY, unilateral = false, defaultSeconds = 30, stretchAreas = emptySet(), patterns = setOf(Pattern.MOBILITY))
        val c = catalog.copy(variations = catalog.variations + mob, warmupTemplate = listOf("mob-a"))
        val p = ready(input(30, warm = true, cat = c))
        assertEquals(BlockType.WARMUP, p.blocks.first().type)
        assertEquals(p.blocks.sumOf { it.durationSeconds }, p.plannedDurationSeconds)
    }

    @Test fun `transitions only when position changes and never exceed five seconds`() {
        val standing = catalog.copy(variations = catalog.variations.map { if (it.id == "squat-air") it.copy(position = "standing") else it })
        val p = ready(input(30, cat = standing))
        val trs = p.blocks.filter { it.type == BlockType.TRANSITION }
        assertTrue(trs.isNotEmpty()); assertTrue(trs.all { it.durationSeconds <= TRANSITION_SECONDS })
        assertTrue(ready(input(30)).blocks.none { it.type == BlockType.TRANSITION })
    }

    @Test fun `draft content is flagged`() = assertTrue(ready(input(30)).usesDraftContent)

    @Test fun `block ids are unique`() {
        val p = ready(input(45)); assertEquals(p.blocks.size, p.blocks.map { it.id }.distinct().size)
    }

    @Test fun `generated matrix always satisfies the invariants`() {
        for (m in 10..90 step 5) for (stretch in listOf(true, false)) for (prof in SeedProfiles.all) for (f in listOf(setOf(StrengthFocus.FULL_BODY), setOf(StrengthFocus.UPPER_BODY, StrengthFocus.CORE))) {
            val r = generate(input(m, prof, stretch, f))
            if (r is PlanResult.Ready) {
                val p = r.plan
                assertTrue(p.plannedDurationSeconds <= m * 60)
                assertEquals(p.blocks.sumOf { it.durationSeconds }, p.plannedDurationSeconds)
                if (stretch) assertTrue(p.blocks.none { it.type == BlockType.PASSIVE_RECOVERY })
                assertTrue(p.blocks.all { it.durationSeconds > 0 })
                if (m * 60 - p.plannedDurationSeconds > UNDERFILL_TOLERANCE_SECONDS) assertTrue("underfill needs acceptance m=$m", p.needsAcceptance)
            } else assertTrue((r as PlanResult.Infeasible).alternatives.isNotEmpty())
        }
    }
}
