package io.github.gonbei774.calisthenicsmemory.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import io.github.gonbei774.calisthenicsmemory.platform.PlatformContext

actual class DemoClip(val url: String)

// Compile-only for Phase 3b: no clip is offered on the web until the <video> player lands in Phase 4.
actual object DemoClips {
    actual fun file(ctx: PlatformContext, id: String): DemoClip? = null
    actual fun file(ctx: PlatformContext, id: String, heldIds: Set<String>): DemoClip? = null
}

@Composable
actual fun DemoPlayer(file: DemoClip, controls: Boolean, onLoad: (String) -> Unit, minHeight: Dp, fit: Boolean) = Unit
