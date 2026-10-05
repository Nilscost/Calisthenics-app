package app.calisthenics.domain

import app.calisthenics.domain.content.validateCatalog
import app.calisthenics.domain.equipment.*
import app.calisthenics.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class ContentEquipmentTest {
    private val c = Fx.catalog()

    @Test fun `valid test catalog has no errors`() {
        val r = validateCatalog(c)
        assertTrue(r.errors.toString(), r.ok)
    }

    @Test fun `duplicate ids and non kebab ids are rejected`() {
        val bad = c.copy(variations = c.variations + c.variations.first())
        assertTrue(validateCatalog(bad).errors.any { it.contains("duplicate variation") })
        val bad2 = c.copy(variations = c.variations.map { if (it.id == "squat-air") it.copy(id = "Squat_Air") else it })
        assertTrue(validateCatalog(bad2).errors.any { it.contains("kebab") })
    }

    @Test fun `strength variation without policy is rejected`() {
        val bad = c.copy(variations = c.variations.map { if (it.id == "squat-air") it.copy(progressionPolicyId = null) else it })
        assertTrue(validateCatalog(bad).errors.any { it.contains("without progression policy") })
    }

    @Test fun `progression cycle is rejected`() {
        val bad = c.copy(policies = c.policies.map { if (it.id == "pol-pushup-std") it.copy(nextVariationIds = listOf("pushup-knee")) else it })
        assertTrue(validateCatalog(bad).errors.any { it.contains("cycle") })
    }

    @Test fun `skill edge dangling and prerequisite cycle are rejected`() {
        val n = listOf(SkillNode("a", "A", variationIds = listOf("pushup-knee")), SkillNode("b", "B", variationIds = listOf("pushup-std")))
        val dangling = c.copy(skillNodes = n, skillEdges = listOf(SkillEdge("e1", "a", "zzz", EdgeRelation.PREREQUISITE)))
        assertTrue(validateCatalog(dangling).errors.any { it.contains("dangling") })
        val cyc = c.copy(skillNodes = n, skillEdges = listOf(
            SkillEdge("e1", "a", "b", EdgeRelation.PREREQUISITE), SkillEdge("e2", "b", "a", EdgeRelation.PREREQUISITE)))
        assertTrue(validateCatalog(cyc).errors.any { it.contains("skill prerequisite cycle") })
        // the same loop through RECOMMENDED_PREPARATION is allowed
        val ok = cyc.copy(skillEdges = cyc.skillEdges.map { it.copy(relation = EdgeRelation.RECOMMENDED_PREPARATION) })
        assertTrue(validateCatalog(ok).ok)
    }

    @Test fun `production mode rejects draft content`() {
        assertTrue(validateCatalog(c, production = true).errors.any { it.contains("DRAFT") })
    }

    @Test fun `reviewed content needs reviewer and sources`() {
        val bad = c.copy(variations = c.variations.map { if (it.id == "squat-air") it.copy(reviewState = ReviewState.REVIEWED) else it })
        assertTrue(validateCatalog(bad).errors.any { it.contains("REVIEWED without") })
    }

    @Test fun `non increasing tier targets are rejected`() {
        val p = c.policies.first()
        val badTiers = p.tiers.mapIndexed { i, t -> if (i == 2) t.copy(target = Target(TargetType.REPS, 1)) else t }
        val bad = c.copy(policies = listOf(p.copy(tiers = badTiers)) + c.policies.drop(1))
        assertTrue(validateCatalog(bad).errors.any { it.contains("easier than") })
    }

    // ---- equipment ----
    private val bandNeed = RequirementSet(needs = listOf(EquipmentNeed("resistance-band")))
    private val kbNeed = RequirementSet(needs = listOf(EquipmentNeed("kettlebell", minMassGrams = 10_000)))

    @Test fun `bodyweight is available everywhere`() {
        assertTrue(isAvailable(c.variation("squat-air")!!, SeedProfiles.travel))
    }

    @Test fun `AND within set and OR across alternatives`() {
        val needsBoth = Fx.strength("x", alts = listOf(RequirementSet(needs = listOf(EquipmentNeed("mat"), EquipmentNeed("pullup-bar")))))
        assertTrue(isAvailable(needsBoth, SeedProfiles.home))
        assertFalse(isAvailable(needsBoth, SeedProfiles.travel))
        val orVar = Fx.strength("y", alts = listOf(bandNeed, kbNeed))
        assertTrue(isAvailable(orVar, SeedProfiles.home))
        assertFalse(isAvailable(orVar, SeedProfiles.travel))
    }

    @Test fun `mass range is enforced`() {
        val heavy = Fx.strength("h", alts = listOf(RequirementSet(needs = listOf(EquipmentNeed("kettlebell", minMassGrams = 16_000)))))
        assertFalse(isAvailable(heavy, SeedProfiles.home)) // home kettlebell is 12 kg
        assertTrue(isAvailable(Fx.strength("l", alts = listOf(kbNeed)), SeedProfiles.home))
    }

    @Test fun `quantity is enforced`() {
        val two = Fx.strength("q", alts = listOf(RequirementSet(needs = listOf(EquipmentNeed("weight", quantity = 2)))))
        val three = Fx.strength("q3", alts = listOf(RequirementSet(needs = listOf(EquipmentNeed("weight", quantity = 3)))))
        assertTrue(isAvailable(two, SeedProfiles.home))
        assertFalse(isAvailable(three, SeedProfiles.home))
    }

    @Test fun `chair exercises need a stable chair and the travel chair is flagged stable by owner decision`() {
        val dips = Fx.strength("dip", alts = listOf(RequirementSet(needs = listOf(EquipmentNeed("chair", suitability = setOf("stable"))))))
        assertTrue(isAvailable(dips, SeedProfiles.travel))
        val unconfirmed = SeedProfiles.travel.copy(items = listOf(EquipmentItem("chair")))
        assertFalse(isAvailable(dips, unconfirmed))
    }

    @Test fun `session overlay never edits the saved profile`() {
        val today = AvailabilityOverlay(unavailableEquipmentIds = setOf("pullup-bar")).applyTo(SeedProfiles.home)
        assertTrue(today.items.none { it.equipmentId == "pullup-bar" })
        assertTrue(SeedProfiles.home.items.any { it.equipmentId == "pullup-bar" })
    }

    @Test fun `missing explanation names the gap`() {
        val v = Fx.strength("y", alts = listOf(kbNeed))
        assertTrue(missingFor(v, SeedProfiles.travel).contains("kettlebell"))
    }

    @Test fun `suggestions only unlock and respect dismissal`() {
        val v = Fx.strength("y", alts = listOf(bandNeed))
        val s = suggestEquipment(listOf(v), SeedProfiles.travel, emptySet())
        assertEquals(1, s.size); assertEquals("resistance-band", s[0].equipmentId)
        assertTrue(suggestEquipment(listOf(v), SeedProfiles.travel, setOf(s[0].id)).isEmpty())
        assertTrue(suggestEquipment(emptyList(), SeedProfiles.travel, emptySet()).isEmpty())
    }
}
