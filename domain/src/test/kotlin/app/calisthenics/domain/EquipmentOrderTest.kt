package app.calisthenics.domain

import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.equipment.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** R8.1-T11 (F14): the equipment list is ordered by movement area, with one shared order for onboarding, Settings and everything else. */
class EquipmentOrderTest {
    private val c = parseCatalog(File(System.getProperty("repo.root"), "content/starter/catalog.json").readText())

    @Test fun groupsAreContiguousAndInTheTabOrder() {
        val groups = EquipmentOptions.all.map { it.group }.distinct()
        assertEquals(EquipmentGroup.entries.toList(), groups)                                    // Pull, Push, Legs, Core, Weights, Other, each in one block
        assertEquals(listOf(EquipmentGroup.PULL), EquipmentOptions.all.take(1).map { it.group })
        fun ids(g: EquipmentGroup) = EquipmentOptions.all.filter { it.group == g }.map { it.id }
        assertTrue(ids(EquipmentGroup.PULL).containsAll(listOf("pullup-bar", "rings")))
        assertTrue(ids(EquipmentGroup.PUSH).containsAll(listOf("dip-support", "parallettes")))
        assertTrue(ids(EquipmentGroup.LEGS).containsAll(listOf("box", "step", "bench")))
        assertTrue(ids(EquipmentGroup.CORE).containsAll(listOf("ab-wheel", "sliders")))
        assertEquals(listOf("weight", "kettlebell", "barbell", "weighted-vest"), ids(EquipmentGroup.WEIGHTS))
        assertTrue(ids(EquipmentGroup.OTHER).containsAll(listOf("resistance-band", "wall", "mat")))
    }

    @Test fun everyEquipmentTheCatalogNeedsIsInTheList() {
        val needed = c.variations.flatMap { it.equipmentAlternatives }.flatMap { it.needs }.map { it.equipmentId }.toSet()
        needed.forEach { assertNotNull("no checklist row for $it", EquipmentOptions.byId(it)) }
        assertEquals(EquipmentOptions.all.size, EquipmentOptions.all.map { it.id }.toSet().size)
    }

    @Test fun orderOfFollowsTheList() {
        assertTrue(EquipmentOptions.orderOf("rings") < EquipmentOptions.orderOf("box"))
        assertTrue(EquipmentOptions.orderOf("sliders") < EquipmentOptions.orderOf("kettlebell"))
        assertEquals(EquipmentOptions.all.size, EquipmentOptions.orderOf("not-gear"))
    }
}
