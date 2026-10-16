package io.github.gonbei774.calisthenicsmemory.ui

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import io.github.gonbei774.calisthenicsmemory.ui.screens.DemoClips
import io.github.gonbei774.calisthenicsmemory.ui.train.loadCatalog
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DemoClipsAvailabilityTest {
    @Test fun syntheticHeldDemonstrationCannotPlayEvenWhenItsAssetExists() {
        val ctx = ApplicationProvider.getApplicationContext<Application>()
        val held = loadCatalog(ctx).variations.filter { it.demonstrationHold }
        held.forEach { v ->
            assertTrue("held asset still exists for integrity checks: ${v.id}", ctx.assets.open("demos/${v.id}.mp4").use { it.read() } >= 0)
            assertNull("held clip: ${v.id}", DemoClips.file(ctx, v.id))
        }

        val bundledAssets = ctx.assets.list("demos").orEmpty().toSet()
        val approved = loadCatalog(ctx).variations.first { !it.demonstrationHold && "${it.id}.mp4" in bundledAssets }
        ctx.assets.open("demos/${approved.id}.mp4").use { assertTrue("synthetic fixture needs a real bundled asset", it.read() >= 0) }
        assertNull("synthetic hold must override an existing asset", DemoClips.file(ctx, approved.id, setOf(approved.id)))
        assertNotNull("the same asset remains playable when no hold applies", DemoClips.file(ctx, approved.id, emptySet()))
    }
}
