package io.github.gonbei774.calisthenicsmemory.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.gonbei774.calisthenicsmemory.ui.screens.PersonalTargetStore
import io.github.gonbei774.calisthenicsmemory.ui.train.loadCatalog
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PersonalTargetStoreTest {
    private val ctx: Context get() = ApplicationProvider.getApplicationContext()
    @Before fun clean() { ctx.getSharedPreferences("personal_targets", Context.MODE_PRIVATE).edit().clear().commit() }
    @Test fun saveReloadAndResetAreProspectiveVersionedAndPerExercise() {
        val authored = loadCatalog(ctx)
        val id = "pushup-standard"
        val original = authored.policyForVariation(id)!!
        val values = original.tiers.map { it.target.value + 20 }
        val sibling = authored.variation(id)!!.copy(id = "shared-policy-sibling")
        val siblingTargets = authored.policyFor(sibling)!!.tiers
        val saved = PersonalTargetStore.save(ctx, id, values)
        assertEquals(1, saved.revision)
        assertTrue(saved.modifiedAtEpochMs > 0)
        val reloaded = loadCatalog(ctx)
        assertEquals(values, reloaded.policyForVariation(id)!!.tiers.map { it.target.value })
        assertEquals(siblingTargets, reloaded.policyFor(sibling)!!.tiers)
        PersonalTargetStore.reset(ctx, reloaded, id)
        assertEquals(2, PersonalTargetStore.load(ctx).getValue(id).revision)
        assertEquals(original.tiers, loadCatalog(ctx).policyForVariation(id)!!.tiers)
    }
    @Test fun invalidDefinitionDoesNotReplaceTheStoredValues() {
        val saved = PersonalTargetStore.save(ctx, "pushup-standard", listOf(10, 12, 14, 16, 18))
        assertTrue(runCatching { PersonalTargetStore.save(ctx, "pushup-standard", listOf(10, 9, 8, 7, 6)) }.isFailure)
        assertEquals(saved, PersonalTargetStore.load(ctx).getValue("pushup-standard"))
    }
}
