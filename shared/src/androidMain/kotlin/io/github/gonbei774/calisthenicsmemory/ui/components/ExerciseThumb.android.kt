package io.github.gonbei774.calisthenicsmemory.ui.components

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.util.concurrent.ConcurrentHashMap

private val cache = ConcurrentHashMap<String, ImageBitmap>()

/** Decoded thumbnails are small (about 128 px); they are cached for the process. */
actual fun loadThumb(ctx: android.content.Context, id: String): ImageBitmap? =
    cache[id] ?: run {
        runCatching { ctx.assets.open("thumbs/$id.webp").use { BitmapFactory.decodeStream(it) }?.asImageBitmap() }.getOrNull()?.also { cache[id] = it }
    }
