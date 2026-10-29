package app.calisthenics.domain

import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.model.ProgressionRole
import app.calisthenics.domain.tree.routeTreeLayout
import kotlin.test.Test

/** R8.1-T10 (F13): every route draws its main spine and every same-rung alternate beside its rung; support stays out of the tree. */
class RouteTreeAlternatesTest {
    private val c = parseCatalog(fixtureText("content/starter/catalog.json"))

    @Test fun everyAlternateOfEveryRouteIsInItsRoutesTreeBesideItsRung() {
        for (route in c.progressionRoutes.filter { it.mainPath.isNotEmpty() }) {
            val layout = routeTreeLayout(route) { c.variation(it) != null }
            val ids = layout.nodes.map { it.variationId }
            assertEquals(route.id, ids.distinct().size, ids.size)                                   // nobody twice
            assertEquals(route.mainPath, layout.nodes.filter { it.col == 0 }.map { it.variationId })
            for (m in route.members.filter { it.role == ProgressionRole.ALTERNATE && it.variationId !in route.mainPath }) {
                val at = layout.node(m.atVariationId!!)!!; val alt = layout.node(m.variationId)
                assertNotNull("${route.id}: alternate ${m.variationId} is missing from the tree", alt)
                assertTrue("${route.id}/${m.variationId} beside ${m.atVariationId}", alt!!.col in 1..2 && alt.row >= at.row)
            }
            assertTrue(route.members.filter { it.role == ProgressionRole.SUPPORT }.none { s -> s.variationId in ids && s.variationId !in route.mainPath })
            val cells = layout.nodes.map { it.col to it.row }
            assertEquals("${route.id}: two nodes in one cell", cells.size, cells.toSet().size)
        }
    }

    @Test fun diamondSitsBesideFeetElevatedInThePushUpRoute() {
        val layout = routeTreeLayout(c.routeById("pushup")!!)
        assertEquals(layout.node("pushup-feet-elevated")!!.row, layout.node("pushup-diamond")!!.row)
        assertEquals(1, layout.node("pushup-diamond")!!.col)
    }
}
