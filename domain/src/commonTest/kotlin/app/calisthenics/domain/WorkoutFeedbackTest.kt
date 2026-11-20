package app.calisthenics.domain

import app.calisthenics.domain.equipment.SeedProfiles
import app.calisthenics.domain.model.*
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.progression.ProgressSnapshot
import app.calisthenics.domain.routine.SessionDraft
import kotlin.test.Test

class WorkoutFeedbackTest {
    private val push = Fx.strength("push", "pushup")
    private val otherPush = Fx.strength("push-alt", "pushup")
    private val dip = Fx.strength("dip", "dip")
    private val catalog = Catalog(1, listOf(push, otherPush, dip), listOf(Fx.policy(push.id), Fx.policy(otherPush.id), Fx.policy(dip.id)))
    private val empty = ProgressSnapshot(emptyMap(), emptyMap(), emptyMap(), emptyList())
    private fun slot(v: ExerciseVariation) = RoutineSlot(v.id, Pattern.PUSH_HORIZONTAL, Area.UPPER_BODY, v.id)

    @Test fun fillersNeverAddASecondStepOfAnExistingProgression() {
        val routine = Routine("r", 1, "General strength", listOf(slot(push)))
        val extra = extensionSlots(catalog, routine, setOf(StrengthFocus.UPPER_BODY), SeedProfiles.travel, empty, stretchOn = false)
        assertEquals(listOf("dip"), extra.map { it.preferredVariationId })
    }

    @Test fun generatedWorkoutKeepsDifferentPushProgressionsButDropsADuplicatePushup() {
        val routine = Routine("r", 1, "General strength", listOf(slot(push), slot(otherPush).copy(id = "ext-pushup"), slot(dip)))
        val draft = SessionDraft(600, false, emptySet(), false, false, setOf(StrengthFocus.UPPER_BODY), SeedProfiles.travel.id, rounds = 1)
        val plan = (generate(PlanInput("p", 0, 0, catalog, routine, draft, SeedProfiles.travel)) as PlanResult.Ready).plan
        assertEquals(listOf("push", "dip"), plan.blocks.filter { it.type == BlockType.WORK }.map { it.variationId })
    }

    @Test fun sidePreparationAddsFiveSecondsWithoutShorteningEitherHold() {
        val left = TimelineBlock("left", BlockType.WORK, 33, variationId = "side-plank", side = Side.LEFT,
            target = app.calisthenics.domain.model.Target(TargetType.HOLD_SECONDS, 30))
        val right = left.copy(id = "right", side = Side.RIGHT)
        val blocks = withSideChangePreparation(listOf(left, right))
        assertEquals(listOf(left, right), blocks.filter { it.type == BlockType.WORK })
        assertEquals(BlockType.TRANSITION, blocks[1].type)
        assertEquals(5, blocks[1].durationSeconds)
        assertEquals(71, blocks.sumOf { it.durationSeconds })
        assertEquals(listOf(left, right.copy(side = Side.LEFT)), withSideChangePreparation(listOf(left, right.copy(side = Side.LEFT))))
    }

    @Test fun generatedSidePlanksHaveAGetReadyBlockBetweenFullHolds() {
        val hold = push.copy(id = "side-plank", familyId = "side-plank", progressionPolicyId = "pol-side-plank", kind = Kind.HOLD, unilateral = true)
        val policy = Fx.policy(hold.id).copy(tiers = Fx.policy(hold.id).tiers.map {
            it.copy(target = app.calisthenics.domain.model.Target(TargetType.HOLD_SECONDS, 30), workWindowSeconds = 33)
        })
        val c = catalog.copy(variations = listOf(hold), policies = listOf(policy))
        val r = Routine("r", 1, "Holds", listOf(slot(hold)))
        val d = SessionDraft(600, false, emptySet(), false, false, setOf(StrengthFocus.UPPER_BODY), SeedProfiles.travel.id, rounds = 1)
        val plan = (generate(PlanInput("p", 0, 0, c, r, d, SeedProfiles.travel)) as PlanResult.Ready).plan
        val left = plan.blocks.indexOfFirst { it.type == BlockType.WORK && it.side == Side.LEFT }
        assertEquals(BlockType.TRANSITION, plan.blocks[left + 1].type)
        assertEquals(5, plan.blocks[left + 1].durationSeconds)
        assertEquals(Side.RIGHT, plan.blocks[left + 2].side)
        assertEquals(listOf(33, 33), plan.blocks.filter { it.type == BlockType.WORK }.map { it.durationSeconds })
    }
}
