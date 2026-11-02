// V09 (R4, R9, R25): the still thumbnail of an exercise (assets/thumbs/<id>.webp, made by tools/blender/install_clips.py from the clip's start frame).
package io.github.gonbei774.calisthenicsmemory.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.gonbei774.calisthenicsmemory.platform.PlatformContext
import io.github.gonbei774.calisthenicsmemory.platform.currentPlatformContext

/** Decoded thumbnails are small (about 128 px); the platform caches them for the process. Null = no picture. */
expect fun loadThumb(ctx: PlatformContext, id: String): ImageBitmap?

/**
 * Loads a thumbnail that [loadThumb] could not return synchronously (web: fetched and decoded on first use, then served
 * by [loadThumb]). Android decodes synchronously, so its actual simply returns null.
 */
expect suspend fun awaitThumb(ctx: PlatformContext, id: String): ImageBitmap?

/** The picture of exercise [id] on a rounded surface tile; the first letter of [name] if there is no picture. Decorative: the name sits next to it. */
@Composable
fun ExerciseThumb(id: String, name: String, size: Dp = 52.dp, modifier: Modifier = Modifier) {
    val ctx = currentPlatformContext()
    var bmp by remember(id) { mutableStateOf(loadThumb(ctx, id)) }
    if (bmp == null) LaunchedEffect(id) { bmp = awaitThumb(ctx, id) }
    Box(modifier.size(size).clip(RoundedCornerShape(12.dp)).testTag("thumb_$id"), contentAlignment = Alignment.Center) {
        val shown = bmp
        if (shown != null) Image(shown, contentDescription = null, modifier = Modifier.size(size))
        else Text(name.take(1), style = MaterialTheme.typography.titleMedium)
    }
}
