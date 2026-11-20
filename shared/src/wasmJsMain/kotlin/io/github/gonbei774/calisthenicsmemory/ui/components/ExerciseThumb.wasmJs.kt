// Web thumbnails (Phase 4b): assets/thumbs/<id>.webp (single source, copied to data/thumbs by `copyWebAssets`) are fetched
// on first use and decoded with Skia; `loadThumb` serves the cache, `awaitThumb` fills it.
package io.github.gonbei774.calisthenicsmemory.ui.components

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import io.github.gonbei774.calisthenicsmemory.platform.PlatformContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.await
import org.jetbrains.skia.Image
import kotlin.js.Promise

private val cache = HashMap<String, ImageBitmap>()
private val missing = HashSet<String>()
private val loading = HashMap<String, CompletableDeferred<ImageBitmap?>>()

/** The file's bytes as a binary string (one char per byte), or "" when it cannot be fetched. */
@JsFun("""(url) => fetch(url).then(r => r.ok ? r.arrayBuffer() : null).then(b => {
  if (!b) return '';
  const a = new Uint8Array(b); let s = '';
  for (let i = 0; i < a.length; i += 8192) s += String.fromCharCode.apply(null, a.subarray(i, i + 8192));
  return s;
}).catch(() => '')""")
private external fun fetchBinaryString(url: String): Promise<JsString>

actual fun loadThumb(ctx: PlatformContext, id: String): ImageBitmap? = cache[id]

actual suspend fun awaitThumb(ctx: PlatformContext, id: String): ImageBitmap? {
    cache[id]?.let { return it }
    if (id in missing) return null
    loading[id]?.let { return it.await() }
    val d = CompletableDeferred<ImageBitmap?>()
    loading[id] = d
    val bmp = try {
        val s = fetchBinaryString("data/thumbs/$id.webp").await<JsString>().toString()
        if (s.isEmpty()) null else Image.makeFromEncoded(ByteArray(s.length) { s[it].code.toByte() }).toComposeImageBitmap()
    } catch (e: kotlinx.coroutines.CancellationException) {
        loading.remove(id); d.complete(null); throw e   // scrolled away: not a missing thumbnail
    } catch (_: Throwable) { null }
    if (bmp != null) cache[id] = bmp else missing += id
    loading.remove(id)
    d.complete(bmp)
    return bmp
}
