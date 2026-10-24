package app.calisthenics.domain

import app.calisthenics.domain.feedback.*
import app.calisthenics.domain.model.*
import app.calisthenics.domain.progression.*
import org.junit.Assert.*
import org.junit.Test

/** Reproductions for the versioned, actual-result demonstrated-level contract. */
class DemonstratedProgressionTest {
    private val catalog = Fx.catalog()
    private val variationId = "pushup-knee"
    private val policy get() = catalog.policyForVariation(variationId)!!

    private fun plan(tier: Int, version: Int = 1, source: Catalog = catalog, vid: String = variationId, sides: List<Side>? = null): WorkoutPlan {
        val effectivePolicy = source.policyForVariation(vid)!!
        val p = WorkoutPlan("p$tier", "usual", 1, 1, 0, "home", 180, 180,
            setOf(StrengthFocus.FULL_BODY), false, null, 3, emptyList(), emptyList(), false,
            false, emptyList(), progressionEvidenceVersion = version, progressionPolicies = mapOf(vid to effectivePolicy))
        val tierPolicy = effectivePolicy.tier(tier)
        val blockSides = sides ?: List(3) { Side.NONE }
        return p.copy(blocks = blockSides.mapIndexed { i, side -> TimelineBlock(
            "w${i + 1}", BlockType.WORK, maxOf(60, tierPolicy.workWindowSeconds), i + 1, "push", vid,
            side = side, target = tierPolicy.target, prescriptionTier = tier, freeTarget = false,
            targetRevision = source.personalTargets[vid]?.revision ?: 0,
        ) })
    }

    private fun evidence(p: WorkoutPlan, day: Int, values: List<Int>, skipped: Set<String> = emptySet(), source: Catalog = catalog): SessionEvidence {
        val work = p.blocks.filter { it.type == BlockType.WORK }
        val vid = work.first().variationId!!
        val effectivePolicy = p.progressionPolicies[vid] ?: source.policyForVariation(vid)!!
        val logs = work.mapIndexed { i, b -> b.id to Feedback(
            actualReps = if (b.target?.type == TargetType.REPS) values[i] else null,
            actualHoldSeconds = if (b.target?.type == TargetType.HOLD_SECONDS) values[i] else null,
        ) }.toMap()
        val outcomes = resolveFeedback(p, work.associate { it.id to if (it.id in skipped) Execution.SKIPPED else Execution.COMPLETED }, logs)
        return deriveEvidence("s$day", day, p, outcomes, { source.variation(it)?.familyId ?: it }, { v, t ->
            source.policyForVariation(v)?.tiers?.firstOrNull { it.index == t }?.minQualifyingBlocks ?: 2
        }, loggedBlockIds = work.map { it.id }.toSet(), effectivePolicyOf = { source.policyForVariation(it) }).single()
    }

    private fun replay(vararg ev: SessionEvidence) = replayFor(variationId, catalog, *ev)

    private fun replayFor(vid: String, source: Catalog, vararg ev: SessionEvidence) = ProgressionEngine(source).replay(
        ev.toList(), listOf(UserAction.SelfAssessment(0, vid, 1)),
    ).variations.getValue(vid)

    @Test fun higherSelectedLevelWithExplicitCompleteResultsAdoptsAndThenConsolidates() {
        val level3 = plan(3)
        val atLevel3 = List(3) { policy.tier(3).target.value }
        val lowerExposure = evidence(plan(1), 1, List(3) { policy.tier(1).target.value })
        val adoption = evidence(level3, 2, atLevel3)
        val first = replay(lowerExposure, adoption)
        assertEquals(3, first.tier)
        assertEquals(listOf(2), first.streakDays) // adoption is success 1/3; old level-1 credit is discarded
        assertTrue(first.achievedTiers.isEmpty())

        val second = replay(lowerExposure, adoption, evidence(level3, 3, atLevel3))
        assertEquals(3, second.tier)
        assertEquals(listOf(2, 3), second.streakDays)
        val third = replay(lowerExposure, adoption, evidence(level3, 3, atLevel3), evidence(level3, 4, atLevel3))
        assertEquals(4, third.tier)
    }

    @Test fun selectedLevelWithoutConfirmedFullResultsDoesNotAdopt() {
        val p = plan(3)
        val work = p.blocks.filter { it.type == BlockType.WORK }
        val unconfirmed = resolveFeedback(p, work.associate { it.id to Execution.COMPLETED })
        val noLogs = deriveEvidence("no-logs", 1, p, unconfirmed, { "pushup" }, { _, _ -> 2 },
            effectivePolicyOf = { policy }).single()
        assertEquals(1, replay(noLogs).tier)

        val partial = evidence(p, 2, List(3) { policy.tier(3).target.value }, setOf("w3"))
        assertEquals(1, replay(partial).tier)
    }

    @Test fun unconfirmedHigherSelectionDoesNotSeedCurrentTierOrStarsWithoutSelfAssessment() {
        val p = plan(3)
        val work = p.blocks.filter { it.type == BlockType.WORK }
        val unconfirmed = resolveFeedback(p, work.associate { it.id to Execution.SKIPPED })
        val ev = deriveEvidence("unconfirmed", 1, p, unconfirmed, { "pushup" }, { _, _ -> 2 },
            effectivePolicyOf = { policy }).single()

        val progress = ProgressionEngine(catalog).replay(listOf(ev), emptyList()).variations.getValue(variationId)
        assertEquals(1, progress.tier)
        assertNull(progress.baselineTier)
        assertEquals(0, progress.earnedStars())
    }

    @Test fun oneExceptionalSetCannotDemonstrateTheSelectedLevel() {
        val p = plan(3)
        val values = listOf(policy.tier(3).target.value, policy.tier(1).target.value, policy.tier(1).target.value)
        assertEquals(1, replay(evidence(p, 1, values)).tier)
    }

    @Test fun historicalVersionZeroDoesNotGainDemonstratedLevelSemantics() {
        val p = plan(3, version = 0)
        assertEquals(1, replay(evidence(p, 1, List(3) { policy.tier(3).target.value })).tier)
    }

    @Test fun freshAppLevelFreeTargetsCanDemonstrateButRangeAndLegacyFreeTargetsCannot() {
        val actual = List(3) { policy.tier(3).target.value }
        val freeApp = plan(3).let { p -> p.copy(blocks = p.blocks.map { it.copy(freeTarget = true) }) }
        val demonstrated = evidence(freeApp, 1, actual)
        assertEquals(3, demonstrated.demonstratedTier)
        assertEquals(3, replay(demonstrated).tier)

        val legacyFree = evidence(freeApp.copy(id = "old", progressionEvidenceVersion = 0), 1, actual)
        assertNull(legacyFree.demonstratedTier)
        assertNull(legacyFree.prescribedTier)

        val range = evidence(plan(3).copy(rule = ProgressionRule(kind = RuleKind.REP_RANGE)), 1, actual)
        assertNull(range.demonstratedTier)
    }

    @Test fun effectivePersonalTargetsAreUsedForTheDemonstrationThreshold() {
        val effective = catalog.copy(personalTargets = mapOf(
            variationId to PersonalLevelTargets(1, listOf(10, 20, 30, 40, 50)),
        ))
        val raisedPlan = plan(3, source = effective)
        val result = evidence(raisedPlan, 1, List(3) { policy.tier(3).target.value }, source = effective)
        assertNull(result.demonstratedTier)
        assertEquals(1, replay(result).tier)
    }

    @Test fun timedHoldsAndCompoundSetRequirementsAreChecked() {
        val id = "hold-demo"
        val v = Fx.strength(id).copy(kind = Kind.HOLD)
        val base = Fx.policy(id, type = TargetType.HOLD_SECONDS)
        val profilePolicy = base.copy(tierProfile = TierProfile.H60,
            tiers = base.tiers.mapIndexed { i, t ->
                val seconds = listOf(10, 15, 20, 30, 60)[i]
                t.copy(target = Target(TargetType.HOLD_SECONDS, seconds), workWindowSeconds = seconds + 3)
            })
        val holdCatalog = Catalog(1, listOf(v), listOf(profilePolicy))

        val level3 = plan(3, source = holdCatalog, vid = id)
        val onTarget = evidence(level3, 1, List(3) { 20 }, source = holdCatalog)
        assertEquals(3, onTarget.demonstratedTier)

        // H60 tier 1 requires six 10-second holds; three logged holds are insufficient.
        val level1 = plan(1, source = holdCatalog, vid = id)
        assertNull(evidence(level1, 2, List(3) { 10 }, source = holdCatalog).demonstratedTier)
    }

    @Test fun unilateralTargetsRequireCompleteResultsForBothSides() {
        val id = "unilateral-demo"
        val v = Fx.strength(id).copy(unilateral = true)
        val base = Fx.policy(id)
        val sideCatalog = Catalog(1, listOf(v), listOf(base.copy(tierProfile = TierProfile.RU)))
        val both = plan(3, source = sideCatalog, vid = id,
            sides = List(3) { Side.LEFT } + List(3) { Side.RIGHT })
        assertEquals(3, evidence(both, 1, List(6) { 15 }, source = sideCatalog).demonstratedTier)

        val leftOnly = plan(3, source = sideCatalog, vid = id, sides = List(3) { Side.LEFT })
        assertNull(evidence(leftOnly, 2, List(3) { 15 }, source = sideCatalog).demonstratedTier)
    }

    @Test fun equalRepTargetsStillRespectNegativeTempoRequirements() {
        val negativePolicy = Fx.policy("negative").copy(tierProfile = TierProfile.N,
            tiers = Fx.policy("negative").tiers.mapIndexed { i, t ->
                t.copy(target = Target(TargetType.REPS, listOf(3, 4, 5, 5, 5)[i]))
            })
        val negativeCatalog = Catalog(1, listOf(Fx.strength("negative")), listOf(negativePolicy))
        val p = plan(3, source = negativeCatalog, vid = "negative")
        val result = evidence(p, 1, List(3) { 5 }, source = negativeCatalog)
        assertEquals(3, result.demonstratedTier) // five reps do not infer the slower tier 4/5 tempo
    }

    @Test fun correctionsRecomputeAdoptionFromPerBlockActuals() {
        val p = plan(3)
        val work = p.blocks.filter { it.type == BlockType.WORK }
        val ids = work.map { it.id }.toSet()
        val recorded = mapOf(work[0].id to 15, work[1].id to 5, work[2].id to 15)
        fun corrected(rows: List<FeedbackRow>): SessionEvidence {
            val rounds = effectiveRounds(p, variationId, recorded, rows)
            val block = rounds.associate { r -> r.blockId to Feedback(
                rating = r.rating, actualReps = r.value, discomfort = r.discomfort,
            ) }
            return deriveEvidence("corrected", 1, p,
                resolveFeedback(p, work.associate { it.id to Execution.COMPLETED }, block),
                { "pushup" }, { _, _ -> 2 }, loggedBlockIds = ids,
                effectivePolicyOf = { policy }).single()
        }
        val rows = work.mapIndexed { index, b -> FeedbackRow(variationId, b.id, index + 1, Rating.MET, false,
            if (index == 1) 15 else recorded.getValue(b.id)) }
        val adopted = corrected(rows)
        assertEquals(3, adopted.demonstratedTier)
        assertEquals(3, replay(adopted).tier)

        val invalidate = rows + FeedbackRow(variationId, work[1].id, 4, Rating.BELOW, false, 5)
        val withdrawn = corrected(invalidate)
        assertNull(withdrawn.demonstratedTier)
        assertEquals(1, replay(withdrawn).tier)
    }

    @Test fun targetRevisionChangeClearsConsolidationAndKeepsOldAttainment() {
        val start = listOf(UserAction.SelfAssessment(0, variationId, 1))
        val level3 = plan(3).copy(createdAtEpochMs = 500)
        val actual = List(3) { policy.tier(3).target.value }
        val oldDays = listOf(evidence(level3, 1, actual), evidence(level3, 2, actual), evidence(level3, 3, actual))
        val prior = ProgressionEngine(catalog).replay(oldDays, start).variations.getValue(variationId)
        assertEquals(4, prior.tier)
        assertEquals(setOf(3), prior.achievedTiers)

        val changed = catalog.copy(personalTargets = mapOf(variationId to PersonalLevelTargets(1, listOf(10, 20, 30, 40, 50), modifiedAtEpochMs = 1_000)))
        val nextPlan = plan(4, source = changed).copy(createdAtEpochMs = 2_000)
        val next = evidence(nextPlan, 4, List(3) { 40 }, source = changed)
        val replayed = ProgressionEngine(changed).replay(oldDays + next, start).variations.getValue(variationId)
        assertEquals(4, replayed.tier)
        assertEquals(listOf(4), replayed.streakDays)
        assertEquals(setOf(3), replayed.achievedTiers)
    }

    @Test fun painManualEasierWeightAndAutomaticToggleStillProtectAdoption() {
        val higher = evidence(plan(3), 1, List(3) { policy.tier(3).target.value })
        assertEquals(1, replay(higher.copy(discomfort = true, discomfortReportedDay = 1)).tier)
        assertEquals(1, replay(higher.copy(easierOverride = true)).tier)
        assertEquals(1, ProgressionEngine(catalog).replay(listOf(higher),
            listOf(UserAction.SelfAssessment(0, variationId, 2), UserAction.ManualOverride(0, variationId, 1)))
            .variations.getValue(variationId).tier)
        assertEquals(1, ProgressionEngine(catalog).replay(listOf(higher),
            listOf(UserAction.SelfAssessment(0, variationId, 1)), autoProgression = false)
            .variations.getValue(variationId).tier)

        val weightedStart = evidence(plan(1), 0, List(3) { policy.tier(1).target.value }).copy(loadGrams = 12_000)
        val unknown = higher.copy(loadGrams = null, day = 1)
        assertEquals(1, replay(weightedStart, unknown).tier)
        val heavier = higher.copy(loadGrams = 16_000, day = 1)
        val reset = replay(weightedStart, heavier)
        assertEquals(1, reset.tier)
        assertEquals(16_000, reset.loadGrams)
    }

    @Test fun duplicateSameDayResultsCannotCompleteTheThreeDayGate() {
        val p = plan(3)
        val actual = List(3) { policy.tier(3).target.value }
        val sameDay = listOf(evidence(p, 1, actual), evidence(p, 1, actual), evidence(p, 1, actual))
        val afterSameDay = replay(*sameDay.toTypedArray())
        assertEquals(3, afterSameDay.tier)
        assertEquals(listOf(1), afterSameDay.streakDays)
        val afterTwoDays = replay(*(sameDay + evidence(p, 2, actual)).toTypedArray())
        assertEquals(3, afterTwoDays.tier)
        val afterThreeDays = replay(*(sameDay + evidence(p, 2, actual) + evidence(p, 3, actual)).toTypedArray())
        assertEquals(4, afterThreeDays.tier)
    }

    private fun sameRungCatalog(alternateTargetMultiplier: Int = 1) = catalog.copy(
        exerciseGroups = listOf(ExerciseGroup("pushup-rung", "pushup-knee", listOf("pushup-std"))),
        policies = catalog.policies.map { p ->
            if (p.variationId == "pushup-std" && alternateTargetMultiplier != 1) p.copy(tiers = p.tiers.map { tier ->
                tier.copy(target = tier.target.copy(value = tier.target.value * alternateTargetMultiplier))
            }) else p
        },
    )

    @Test fun mixedPrescribedTiersCanDemonstrateOnlyWhenEveryActualMeetsTheWholeLevel() {
        val p = plan(3).let { plan ->
            plan.copy(blocks = plan.blocks.mapIndexed { index, block ->
                if (index == 0) block.copy(prescriptionTier = 1, target = policy.tier(1).target)
                else block
            })
        }
        val actual = List(3) { policy.tier(3).target.value }
        val mixed = evidence(p, 1, actual)
        assertEquals(3, mixed.demonstratedTier)
        assertEquals(3, replay(mixed).tier)
        val insufficient = evidence(p, 2, listOf(actual[0], policy.tier(1).target.value, actual[2]))
        assertNull(insufficient.demonstratedTier)
    }

    @Test fun sameRungV1DaysPoolOnlyAtCompatibleCurrentTier() {
        val source = sameRungCatalog()
        val main = "pushup-knee"
        val mate = "pushup-std"
        val high = source.policyForVariation(main)!!.tier(3).target.value
        val adoptedMain = evidence(plan(3, source = source, vid = main), 1, List(3) { high }, source = source)
        val adoptedMate = evidence(plan(3, source = source, vid = mate), 2,
            List(3) { source.policyForVariation(mate)!!.tier(3).target.value }, source = source)
        val current = evidence(plan(3, source = source, vid = main), 3, List(3) { high }, source = source)
        val pooled = replayFor(main, source, adoptedMain, adoptedMate, current)
        assertEquals(4, pooled.tier)

        val incompatible = sameRungCatalog(alternateTargetMultiplier = 2)
        val incompatibleMain = evidence(plan(3, source = incompatible, vid = main), 1,
            List(3) { incompatible.policyForVariation(main)!!.tier(3).target.value }, source = incompatible)
        val incompatibleMate = evidence(plan(3, source = incompatible, vid = mate), 2,
            List(3) { incompatible.policyForVariation(mate)!!.tier(3).target.value }, source = incompatible)
        val stillCurrent = evidence(plan(3, source = incompatible, vid = main), 3,
            List(3) { incompatible.policyForVariation(main)!!.tier(3).target.value }, source = incompatible)
        assertEquals(3, replayFor(main, incompatible, incompatibleMain, incompatibleMate, stillCurrent).tier)

        val lowerMate = evidence(plan(1, source = source, vid = mate), 2,
            List(3) { source.policyForVariation(mate)!!.tier(1).target.value }, source = source)
        assertEquals(3, replayFor(main, source, adoptedMain, lowerMate, current).tier)
    }

    @Test fun adoptionAndConsolidationUseAnInclusiveFourteenCalendarDayWindow() {
        val p = plan(3)
        val actual = List(3) { policy.tier(3).target.value }
        assertEquals(3, replay(evidence(p, 1, actual), evidence(p, 8, actual), evidence(p, 15, actual)).tier)
        assertEquals(4, replay(evidence(p, 1, actual), evidence(p, 8, actual), evidence(p, 14, actual)).tier)
    }
}
