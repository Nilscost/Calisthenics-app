package app.calisthenics.domain

import app.calisthenics.domain.equipment.SeedProfiles
import app.calisthenics.domain.model.*
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.progression.*
import app.calisthenics.domain.routine.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** L12: the warm-up in RAMP order with items that unlock from the ladder state. */
class RampTest {
    private val catalog = app.calisthenics.domain.content.parseCatalog(File(System.getProperty("repo.root"), "content/starter/catalog.json").readText())
    private val none = ProgressSnapshot(emptyMap(), emptyMap(), emptyMap(), emptyList())
    private val gym = SeedProfiles.home.copy(items = SeedProfiles.home.items + listOf(EquipmentItem("dip-support")))
    private val eight = listOf("warmup-shoulder-band", "warmup-squat-sky-reach", "warmup-wrist-prep", "warmup-dead-bug", "warmup-arch-hang", "warmup-support-hold", "warmup-squat-easier", "warmup-hinge-easier")
    private fun snap(vararg ids: String) = ProgressSnapshot(ids.associateWith { id ->
        VariationProgress(id, catalog.variation(id)!!.familyId, 1, 0, 1, emptySet(), null, emptyList(), 0, 0, false, false, null, null) }, emptyMap(), emptyMap(), emptyList())
    private val push = listOf(Pattern.PUSH_HORIZONTAL, Pattern.SQUAT)

    @Test fun beginnersGetTheFourBasicItemsInRampOrder() {
        val w = Ramp.order(catalog, gym, none, eight, push)
        // raise (squat sky reach) -> activate (dead bug) -> mobilise (shoulder band, wrist prep); nothing gated yet
        assertEquals(listOf("warmup-squat-sky-reach", "warmup-dead-bug", "warmup-shoulder-band", "warmup-wrist-prep"), w)
        assertEquals(RampPhase.RAISE, Ramp.phaseOf("warmup-squat-sky-reach")); assertEquals(RampPhase.POTENTIATE, Ramp.phaseOf("warmup-support-hold"))
    }

    @Test fun laterItemsUnlockFromTheLadderState() {
        val w = Ramp.order(catalog, gym, snap("pullup-negative", "dip-negative", "split-squat-bulgarian", "nordic-banded"), eight, push)
        assertEquals(listOf("warmup-squat-sky-reach", "warmup-dead-bug", "warmup-arch-hang", "warmup-shoulder-band", "warmup-wrist-prep", "warmup-support-hold", "warmup-squat-easier", "warmup-hinge-easier"), w)
        // "reached" counts something harder too: someone who started the pull-up has passed the negatives
        assertTrue("warmup-arch-hang" in Ramp.order(catalog, gym, snap("pullup-full"), eight, push))
        assertFalse("warmup-support-hold" in Ramp.order(catalog, gym, snap("pullup-negative"), eight, push))
    }

    @Test fun theWristPrepComesOnlyBeforePushWork() {
        assertFalse("warmup-wrist-prep" in Ramp.order(catalog, gym, none, eight, listOf(Pattern.SQUAT, Pattern.HINGE)))
        assertTrue("warmup-wrist-prep" in Ramp.order(catalog, gym, none, eight, listOf(Pattern.PUSH_VERTICAL)))
    }

    @Test fun anItemTheEquipmentCannotDoIsLeftOut() {
        val bare = SeedProfiles.home.copy(items = emptyList())
        assertFalse("warmup-arch-hang" in Ramp.order(catalog, bare, snap("pullup-negative"), eight, push))      // needs a bar
        assertFalse("warmup-support-hold" in Ramp.order(catalog, bare, snap("dip-negative"), eight, push))       // needs dip support
    }

    @Test fun thePlanUsesTheRampOrderAndLabelsEachBlock() {
        val rr = Presets.recommended(catalog, gym); assertEquals(eight, rr.routine.warmup)
        val r = resolveSaved(rr, settingsFor(rr, TrainSettings()))
        val p = (buildTrainPlan(catalog, r.routine, snap("pullup-negative"), gym, r.settings, edits = r.edits) as PlanResult.Ready).plan
        val warm = p.blocks.filter { it.type == BlockType.WARMUP }
        assertEquals(listOf("warmup-squat-sky-reach", "warmup-dead-bug", "warmup-arch-hang", "warmup-shoulder-band", "warmup-wrist-prep"), warm.mapNotNull { it.variationId })
        assertEquals("Warm-up · raise", warm.first().note); assertEquals("Warm-up · mobilise", warm.last().note)
        assertTrue(catalog.variation("warmup-squat-easier")!!.kind == Kind.MOBILITY && catalog.variation("warmup-hinge-easier")!!.defaultSeconds == 30)
    }
}
