package app.calisthenics.domain

import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.equipment.*
import app.calisthenics.domain.model.EquipmentItem
import app.calisthenics.domain.model.EquipmentProfile
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class PlanEquipmentTest {
    private val c = parseCatalog(File(System.getProperty("repo.root"), "content/starter/catalog.json").readText())
    private val home = SeedProfiles.home

    @Test fun bodyweightOnlyPlanNeedsNothing() {
        val bw = c.variations.filter { v -> v.equipmentAlternatives.isEmpty() || v.equipmentAlternatives.all { a -> a.needs.isEmpty() } }.take(3).map { it.id }
        assertTrue(bw.isNotEmpty())
        assertEquals(emptyList<String>(), planEquipment(c, bw, home))
    }

    @Test fun collectsUniqueEquipmentInChecklistOrder() {
        val withGear = c.variations.filter { v -> v.equipmentAlternatives.firstOrNull()?.needs?.any { it.equipmentId == "pullup-bar" } == true }.first()
        val withMat = c.variations.first { v -> v.equipmentAlternatives.firstOrNull()?.needs?.any { it.equipmentId == "mat" } == true && equipmentNeededFor(v, home) == listOf("mat") }
        val r = planEquipment(c, listOf(withMat.id, withGear.id, withGear.id, "unknown-id"), home)
        assertEquals(r.distinct(), r)
        assertTrue("pullup-bar" in r && "mat" in r)
        assertTrue(r.indexOf("pullup-bar") < r.indexOf("mat"))
    }

    @Test fun usesTheAlternativeTheProfileSatisfies() {
        val v = c.variations.first { it.equipmentAlternatives.size > 1 && it.equipmentAlternatives.map { a -> a.needs.map { n -> n.equipmentId } }.distinct().size > 1 }
        val second = v.equipmentAlternatives[1]
        val profile = EquipmentProfile("p", "P", second.needs.map { EquipmentItem(it.equipmentId, it.quantity, it.minMassGrams ?: it.maxMassGrams, suitability = it.suitability) }, second.capabilities + "floor-space")
        if (v.equipmentAlternatives.take(1).none { setSatisfied(it, profile) }) assertEquals(second.needs.map { it.equipmentId }, equipmentNeededFor(v, profile))
    }
}
