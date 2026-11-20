// Demo clips: one short silent clip per exercise variation, named <variationId>.mp4.
// The seam is platform-specific (Android: VideoView over a cached copy of the bundled asset; web: <video> in Phase 4).
package io.github.gonbei774.calisthenicsmemory.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.gonbei774.calisthenicsmemory.platform.PlatformContext

/** An opaque, playable clip. Android: the cached `java.io.File`. */
expect class DemoClip

expect object DemoClips {
    /** The clip of variation [id], or null when it has none or the catalog holds it back. */
    fun file(ctx: PlatformContext, id: String): DemoClip?

    /** Same, with an explicit set of held-back variations (policy seam for exercising a teaching hold). */
    fun file(ctx: PlatformContext, id: String, heldIds: Set<String>): DemoClip?
}

/**
 * Plays [file] muted in a loop. [onLoad] is called with every path handed to the player (used by tests).
 */
@Composable
expect fun DemoPlayer(file: DemoClip, controls: Boolean = true, onLoad: (String) -> Unit = {}, minHeight: Dp = 180.dp, fit: Boolean = false)
