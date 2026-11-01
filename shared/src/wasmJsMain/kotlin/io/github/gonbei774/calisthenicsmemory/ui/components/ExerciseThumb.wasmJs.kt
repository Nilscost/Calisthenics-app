package io.github.gonbei774.calisthenicsmemory.ui.components

import androidx.compose.ui.graphics.ImageBitmap
import io.github.gonbei774.calisthenicsmemory.platform.PlatformContext

// Compile-only for Phase 3b: the letter fallback is shown until web thumbnails are wired in Phase 4.
actual fun loadThumb(ctx: PlatformContext, id: String): ImageBitmap? = null
