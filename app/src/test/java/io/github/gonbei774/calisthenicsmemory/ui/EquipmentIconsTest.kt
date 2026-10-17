package io.github.gonbei774.calisthenicsmemory.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import app.calisthenics.domain.equipment.EquipmentGroup
import app.calisthenics.domain.equipment.EquipmentOptions
import io.github.gonbei774.calisthenicsmemory.ui.screens.EquipmentChecklist
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalisthenicsMemoryTheme
import io.github.gonbei774.calisthenicsmemory.ui.theme.EquipmentIcons
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** R8.1-T11 (F14): every equipment id has an icon, and the checklist lists them by movement area. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class EquipmentIconsTest {
    @get:Rule val rule = createComposeRule()

    @Test fun everyChecklistRowHasAnIcon() {
        EquipmentOptions.all.forEach { assertNotNull("no icon for ${it.id}", EquipmentIcons.forId(it.id)) }
    }

    @Test fun theChecklistShowsGroupCaptionsInTheSharedOrder() {
        rule.setContent { CalisthenicsMemoryTheme(darkTheme = false) { Column(Modifier.verticalScroll(rememberScrollState())) { EquipmentChecklist(listOf("mat"), {}, emptyMap(), {}) } } }
        rule.waitForIdle()
        val order = EquipmentOptions.all.map { "eq_${it.id}" }
        order.forEach { rule.onNodeWithTag(it).assertExists() }
        val y = order.map { rule.onNodeWithTag(it).fetchSemanticsNode().positionInRoot.y }
        assertEquals(y.sorted(), y)
        for (g in EquipmentGroup.entries) rule.onNodeWithTag("eq_group_${g.name}").assertExists()
    }
}
