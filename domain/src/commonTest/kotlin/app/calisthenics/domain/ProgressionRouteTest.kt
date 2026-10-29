package app.calisthenics.domain

import app.calisthenics.domain.content.validateCatalog
import app.calisthenics.domain.model.*
import app.calisthenics.domain.equipment.SeedProfiles
import app.calisthenics.domain.planner.mainPathOptions
import app.calisthenics.domain.progression.ProgressionEngine
import app.calisthenics.domain.progression.UserAction
import kotlin.test.Test

class ProgressionRouteTest {
    private fun routedCatalog(): Catalog {
        val base = Fx.catalog()
        val route = ProgressionRoute(
            id = "route-push", name = "Push route", progressionGroup = "push",
            goalVariationId = "pushup-knee", mainPath = listOf("pushup-std", "pushup-knee"),
            members = listOf(
                ProgressionMember("pushup-std", ProgressionRole.MAIN),
                ProgressionMember("pushup-knee", ProgressionRole.MAIN),
                ProgressionMember("squat-air", ProgressionRole.ALTERNATE, "pushup-std"),
            ), goalIds = listOf("push-goal"),
        )
        return base.copy(
            variations = base.variations.map { if (it.id == "squat-air") it.copy(progressionGroup = "push") else it },
            progressionRoutes = listOf(route),
        )
    }

    @Test fun alternateResolvesToItsExplicitMainAnchor() {
        val c = routedCatalog()
        assertEquals("route-push", c.mainRouteFor("squat-air")?.id)
        assertEquals("pushup-std", c.routeAnchorFor("squat-air"))
        assertEquals("pushup-knee", c.nextMain("squat-air"))
        assertNull(c.previousMain("pushup-std"))
        assertEquals("pushup-std", c.previousMain("pushup-knee"))
    }

    @Test fun engineUsesRouteMainSuccessorAndLegacyCatalogStillUsesPolicy() {
        val actions = listOf(UserAction.SelfAssessment(1, "pushup-std", 6))
        val routed = ProgressionEngine(routedCatalog()).replay(emptyList(), actions)
        assertEquals("pushup-knee", routed.activeInFamily.getValue("pushup"))
        val legacy = ProgressionEngine(Fx.catalog()).replay(emptyList(), actions)
        assertEquals("pushup-std", legacy.activeInFamily.getValue("pushup"))
    }

    @Test fun validatorRejectsAnUnanchoredAlternateAndIncompleteMainMembers() {
        val c = routedCatalog()
        val route = c.progressionRoutes.single().copy(members = listOf(
            ProgressionMember("pushup-std", ProgressionRole.MAIN),
            ProgressionMember("squat-air", ProgressionRole.ALTERNATE),
        ))
        val report = validateCatalog(c.copy(progressionRoutes = listOf(route)))
        assertTrue(report.errors.any { it.contains("MAIN members must exactly match mainPath") })
        assertTrue(report.errors.any { it.contains("needs a main-path anchor") })
    }

    @Test fun realCatalogUsesExplicitPushupRouteAndKeepsRouteRolesCoherent() {
        val c = app.calisthenics.domain.content.parseCatalog(
            fixtureText("content/starter/catalog.json"),
        )
        val route = c.mainRouteFor("pushup-standard", "one-arm-pushup")!!
        assertEquals("pushup", route.id)
        assertEquals("pushup-feet-elevated", c.nextMain("pushup-standard", "one-arm-pushup"))
        assertEquals("pushup-one-arm-elevated", c.nextMain("pushup-uneven", "one-arm-pushup"))
        assertEquals("pushup-feet-elevated", c.previousMain("pushup-uneven", "one-arm-pushup"))
        assertTrue(c.progressionRoutes.any { it.defaultPool && it.mainPath.contains("pushup-standard") })
        assertTrue(validateCatalog(c).errors.toString(), validateCatalog(c).ok)
    }

    @Test fun mainPathOptionsUseReviewedOrderAndReplaceTheAlternateAnchor() {
        val c = app.calisthenics.domain.content.parseCatalog(
            fixtureText("content/starter/catalog.json"),
        )
        val options = mainPathOptions(c, "pushup-diamond", SeedProfiles.travel, "one-arm-pushup")
        val i = options.indexOfFirst { it.id == "pushup-diamond" }
        assertTrue(i > 0)
        assertEquals("pushup-standard", options[i - 1].id)
        assertEquals("pushup-archer", options[i + 1].id)
        assertFalse(options.any { it.id == "pushup-feet-elevated" })
        val uneven = mainPathOptions(c, "pushup-uneven", SeedProfiles.travel, "one-arm-pushup")
        val u = uneven.indexOfFirst { it.id == "pushup-uneven" }
        assertEquals("pushup-feet-elevated", uneven[u - 1].id)
        assertEquals("pushup-one-arm-elevated", uneven[u + 1].id)
    }
}
