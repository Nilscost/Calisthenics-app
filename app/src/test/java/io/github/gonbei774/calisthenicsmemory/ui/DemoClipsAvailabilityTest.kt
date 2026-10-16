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
    @Test fun heldDemonstrationsCannotPlayThroughAnyCallerEvenWithBundledAssets() {
        val ctx = ApplicationProvider.getApplicationContext<Application>()
        val held = loadCatalog(ctx).variations.filter { it.demonstrationHold }
        assertTrue(held.isNotEmpty())
        held.forEach { v ->
            assertTrue("held asset still exists for integrity checks: ${v.id}", ctx.assets.open("demos/${v.id}.mp4").use { it.read() } >= 0)
            assertNull("held clip: ${v.id}", DemoClips.file(ctx, v.id))
        }
        assertNotNull(DemoClips.file(ctx, "pushup-standard"))
    }
}
