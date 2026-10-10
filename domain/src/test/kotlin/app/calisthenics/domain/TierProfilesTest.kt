package app.calisthenics.domain

import app.calisthenics.domain.model.*
import app.calisthenics.domain.progression.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** L01: the shared tier vocabulary and the RR rule as one function. */
class TierProfilesTest {
    private fun values(p: TierProfile) = p.specs().map { it.value }
    private fun sets(p: TierProfile) = p.specs().map { it.sets }

    @Test fun profilesProduceTheReportsTierValues() {
        assertEquals(listOf(5, 6, 7, 8, 10), values(TierProfile.R)); assertEquals(values(TierProfile.R), values(TierProfile.RU))
        assertEquals(listOf(8, 9, 10, 11, 12), values(TierProfile.C))
        assertEquals(listOf(10, 15, 20, 25, 30), values(TierProfile.H30))
        assertEquals(listOf(6, 4, 3, 2, 1), sets(TierProfile.H60)); assertEquals(listOf(10, 15, 20, 30, 60), values(TierProfile.H60))
        assertTrue(TierProfile.H60.specs().all { it.sets * it.value == 60 })                      // one accumulated minute at every tier
        assertEquals(listOf(3, 4, 5, 5, 5), values(TierProfile.N)); assertEquals(listOf(3, 5, 5, 8, 10), TierProfile.N.specs().map { it.seconds })
        assertEquals(listOf(15, 18, 20, 22, 25), values(TierProfile.E))
        assertTrue(TierProfile.entries.all { it.specs().size == 5 })
        assertEquals(4, TierProfile.R.gateTier); assertEquals(4, TierProfile.RU.gateTier); assertEquals(5, TierProfile.C.gateTier); assertEquals(5, TierProfile.H30.gateTier); assertNull(TierProfile.M.gateTier)
        assertTrue(TierProfile.H30.isHold && TierProfile.H60.isHold && !TierProfile.R.isHold)
    }

    @Test fun theRrRuleIsOneFunction() {
        // R: three sets of 8 at tier 4 moves you on (and you start the next step at tier 1)
        assertEquals(TierVerdict.MoveOn, tierVerdict(TierProfile.R, 4, listOf(8, 8, 8)))
        assertEquals(TierVerdict.MoveOn, tierVerdict(TierProfile.R, 4, listOf(9, 8, 10)))
        assertEquals(TierVerdict.Stay, tierVerdict(TierProfile.R, 4, listOf(8, 8, 7)))
        assertEquals(TierVerdict.Stay, tierVerdict(TierProfile.R, 3, listOf(7, 7, 7)))        // below the gate tier: keep going
        assertEquals(TierVerdict.Stay, tierVerdict(TierProfile.R, 4, listOf(8, 8)))            // a missing set is not a pass
        // holds: 3 x 30 s at tier 5
        assertEquals(TierVerdict.MoveOn, tierVerdict(TierProfile.H30, 5, listOf(30, 30, 31)))
        assertEquals(TierVerdict.Stay, tierVerdict(TierProfile.H30, 5, listOf(30, 25, 30)))
        // cannot reach tier 1: one step back
        assertEquals(TierVerdict.StepBack, tierVerdict(TierProfile.R, 1, listOf(5, 5, 4)))
        assertEquals(TierVerdict.Stay, tierVerdict(TierProfile.R, 1, listOf(5, 5, 5)))
        // maintenance never moves on
        assertEquals(TierVerdict.Stay, tierVerdict(TierProfile.M, 5, listOf(10)))
        // one accumulated minute for the L-sit
        assertEquals(TierVerdict.MoveOn, tierVerdict(TierProfile.H60, 5, listOf(60)))
    }

    @Test fun everyTaggedPolicyOfTheRealCatalogFollowsItsProfile() {
        val c = app.calisthenics.domain.content.parseCatalog(File(System.getProperty("repo.root"), "content/starter/catalog.json").readText())
        val tagged = c.policies.filter { it.tierProfile != null }
        assertTrue(tagged.map { it.variationId }.containsAll(listOf("dip-support-hold", "pallof-press", "reverse-hyperextension")))
        for (p in tagged) {
            val prof = p.tierProfile!!
            assertEquals(p.variationId, prof.specs().map { it.value }, p.tiers.map { it.target.value })
            assertEquals(p.variationId, if (prof.isHold) TargetType.HOLD_SECONDS else TargetType.REPS, p.tiers.first().target.type)
            assertFalse(p.variationId, p.tierNote.isNullOrBlank())   // which numbers are sourced and which are DRAFT
        }
    }

    @Test fun negativesKeepTheirOwnNumbersUntilTheAppCanTrackTempo() {
        // profile N differs by the seconds of the descent (3 -> 10 s), which the app cannot record yet: its reps plateau, so no catalog policy uses it
        val c = app.calisthenics.domain.content.parseCatalog(File(System.getProperty("repo.root"), "content/starter/catalog.json").readText())
        assertTrue(c.policies.none { it.tierProfile == TierProfile.N })
    }

    @Test fun oldPoliciesWithoutAProfileStillDecode() {
        val j = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        val p = j.decodeFromString(app.calisthenics.domain.model.ProgressionPolicy.serializer(), """{"id":"p","variationId":"v","version":1,"tiers":[{"index":1,"target":{"type":"REPS","value":5},"workWindowSeconds":30,"minRecoverySeconds":60}]}""")
        assertNull(p.tierProfile); assertNull(p.tierNote)
    }
}
