package io.github.gonbei774.calisthenicsmemory.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import app.calisthenics.domain.model.Muscle
import io.github.gonbei774.calisthenicsmemory.ui.components.*
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalisthenicsMemoryTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** V08 (R5): the body figure. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BodyMapTest {
    @get:Rule val rule = createComposeRule()

    @Test fun everyMuscleHasARegionInsideTheBox() {
        for (m in Muscle.entries) {
            val regions = BodyRegions.byMuscle[m]
            assertTrue("$m has no region", !regions.isNullOrEmpty())
            for (r in regions!!) assertTrue("$m region outside the box: $r", r.l >= 0 && r.t >= 0 && r.r <= BodyRegions.W && r.b <= BodyRegions.H && r.l < r.r && r.t < r.b)
        }
        assertEquals(Muscle.entries.toSet(), BodyRegions.byMuscle.keys)
    }

    @Test fun frontAndBackBothShowSomething() {
        val all = BodyRegions.byMuscle.values.flatten()
        assertTrue(all.any { it.view == BodyView.FRONT }); assertTrue(all.any { it.view == BodyView.BACK })
        // pushing and pulling muscles are on the side the owner would look for them
        assertTrue(BodyRegions.byMuscle.getValue(Muscle.CHEST).all { it.view == BodyView.FRONT })
        assertTrue(BodyRegions.byMuscle.getValue(Muscle.LATS).all { it.view == BodyView.BACK })
        assertTrue(BodyRegions.byMuscle.getValue(Muscle.GLUTES).all { it.view == BodyView.BACK })
    }

    @Test fun regionsMirrorLeftAndRight() {
        for ((m, regions) in BodyRegions.byMuscle) {
            val pairs = regions.groupBy { it.view }.values.filter { it.size > 1 }
            for (g in pairs) assertTrue("$m left/right sizes", g.size % 2 == 0)
        }
    }

    @Test fun descriptionListsThePrimaryAndSecondaryMuscles() {
        assertEquals("Chest, Triceps; + Abs", musclesDescription(listOf(Muscle.CHEST, Muscle.TRICEPS), listOf(Muscle.ABS)) { it.name.lowercase().replaceFirstChar(Char::uppercase) })
        assertEquals("", musclesDescription(emptyList(), emptyList()) { it.name })
    }

    @Test fun theFigureSaysItsMusclesToTheScreenReaderAndShowsNamesOnTap() {
        rule.setContent { CalisthenicsMemoryTheme(darkTheme = false) { BodyMap(listOf(Muscle.CHEST, Muscle.TRICEPS), listOf(Muscle.ABS)) } }
        val d = rule.onNodeWithTag("body_map").fetchSemanticsNode().config[SemanticsProperties.ContentDescription].joinToString()
        assertTrue(d, d.contains("Chest") && d.contains("Triceps") && d.contains("Abs"))
        rule.onNodeWithTag("body_map_names").assertDoesNotExist()
        rule.onNodeWithTag("body_map").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("body_map_names", useUnmergedTree = true).assertExists()
    }

    @Test fun emptyMusclesGiveAPlainDescription() {
        rule.setContent { CalisthenicsMemoryTheme(darkTheme = true) { BodyMap(emptyList()) } }
        val d = rule.onNodeWithTag("body_map").fetchSemanticsNode().config[SemanticsProperties.ContentDescription].joinToString()
        assertEquals("No muscles listed", d)
    }
}
