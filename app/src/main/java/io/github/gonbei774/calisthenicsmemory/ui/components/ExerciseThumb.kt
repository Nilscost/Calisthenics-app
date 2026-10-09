// V09 (R4, R9, R25): the still thumbnail of an exercise (assets/thumbs/<id>.webp, made by tools/gen_demo_clips_v3.py).
package io.github.gonbei774.calisthenicsmemory.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.util.concurrent.ConcurrentHashMap

private val cache = ConcurrentHashMap<String, ImageBitmap>()

/** Decoded thumbnails are small (about 128 px); they are cached for the process. */
fun loadThumb(ctx: android.content.Context, id: String): ImageBitmap? =
    cache[id] ?: runCatching { ctx.assets.open("thumbs/$id.webp").use { BitmapFactory.decodeStream(it) }?.asImageBitmap() }.getOrNull()?.also { cache[id] = it }

/** The picture of exercise [id] on a rounded surface tile; the first letter of [name] if there is no picture. Decorative: the name sits next to it. */
@Composable
fun ExerciseThumb(id: String, name: String, size: Dp = 52.dp, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val bmp = remember(id) { loadThumb(ctx, id) }
    Box(modifier.size(size).clip(RoundedCornerShape(12.dp)).testTag("thumb_$id"), contentAlignment = Alignment.Center) {
        if (bmp != null) Image(bmp, contentDescription = null, modifier = Modifier.size(size))
        else Text(name.take(1), style = MaterialTheme.typography.titleMedium)
    }
}
