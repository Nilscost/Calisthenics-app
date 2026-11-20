// Web demo clips (Phase 4b): the same MP4s as Android (single source app/src/main/assets/demos, copied into the web build
// as data/demos by the `copyWebAssets` Gradle task), played by an HTML <video>: muted, playsinline, loop, autoplay, white
// background.
//
// Stacking: an element laid OVER the canvas (WebElementView) would also cover Compose dialogs, menus and scrims, which are
// drawn on the same canvas. So the <video> sits BEHIND the canvas (fixed, z-index -1) and the composable punches a
// transparent hole (BlendMode.Clear) of its own size into the canvas: the clip shows through exactly where the composable
// is, scrolls and clips with it, is covered by whatever Compose draws on top, and never receives taps (the canvas gets them).
package io.github.gonbei774.calisthenicsmemory.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.calisthenics.domain.content.parseCatalog
import io.github.gonbei774.calisthenicsmemory.platform.PlatformContext
import io.github.gonbei774.calisthenicsmemory.platform.readCatalogText
import io.github.gonbei774.calisthenicsmemory.shared.resources.Res
import io.github.gonbei774.calisthenicsmemory.shared.resources.demo_clip_description
import org.jetbrains.compose.resources.stringResource
import org.w3c.dom.HTMLElement

/** The clip's URL, relative to the page. */
actual class DemoClip(val url: String)

actual object DemoClips {
    private var held: Set<String>? = null

    actual fun file(ctx: PlatformContext, id: String): DemoClip? {
        val h = held ?: parseCatalog(ctx.readCatalogText()).variations.filter { it.demonstrationHold }.map { it.id }.toSet().also { held = it }
        return file(ctx, id, h)
    }

    /** Same policy as Android: a held-back variation never plays, whatever asset exists. */
    actual fun file(ctx: PlatformContext, id: String, heldIds: Set<String>): DemoClip? =
        if (id in heldIds) null else DemoClip("data/demos/$id.mp4")
}

@JsFun("""(url) => {
  const v = document.createElement('video');
  v.muted = true; v.defaultMuted = true; v.loop = true; v.autoplay = true; v.playsInline = true; v.preload = 'auto';
  v.setAttribute('muted', ''); v.setAttribute('playsinline', ''); v.setAttribute('webkit-playsinline', '');
  v.disablePictureInPicture = true; v.disableRemotePlayback = true; v.controls = false;
  v.dataset.calClip = url;
  const st = v.style;
  st.position = 'fixed'; st.left = '0px'; st.top = '0px'; st.width = '0px'; st.height = '0px'; st.zIndex = '-1';
  st.background = '#fff'; st.objectFit = 'contain'; st.pointerEvents = 'none'; st.visibility = 'hidden';
  v.src = url;
  const play = () => { const p = v.play(); if (p && p.catch) p.catch(() => {}); };
  v.addEventListener('canplay', play);
  document.body.appendChild(v);
  play();
  return v;
}""")
private external fun createClipVideo(url: String): HTMLElement

@JsFun("(v, x, y, w, h) => { const s = v.style; s.left = x + 'px'; s.top = y + 'px'; s.width = w + 'px'; s.height = h + 'px'; s.visibility = 'visible'; }")
private external fun placeClipVideo(v: HTMLElement, x: Double, y: Double, w: Double, h: Double)

@JsFun("(v) => { try { v.pause(); v.removeAttribute('src'); v.load(); } catch (e) {} v.remove(); }")
private external fun releaseClipVideo(v: HTMLElement)

@JsFun("(v) => { if (v.paused) { const p = v.play(); if (p && p.catch) p.catch(() => {}); } else v.pause(); }")
private external fun toggleClipVideo(v: HTMLElement)

/**
 * Same layout rules as the Android VideoView; the <video> is keyed on the URL so a new clip is a new element. With
 * [controls] a tap on the clip toggles play/pause (Android shows a MediaController on tap).
 */
@Composable
actual fun DemoPlayer(file: DemoClip, controls: Boolean, onLoad: (String) -> Unit, minHeight: Dp, fit: Boolean) {
    val description = stringResource(Res.string.demo_clip_description)
    val url = file.url
    val density = LocalDensity.current.density.toDouble()
    val video = remember(url) { createClipVideo(url) }
    DisposableEffect(video) {
        onLoad(url)
        onDispose { releaseClipVideo(video) }
    }
    val size = if (fit) Modifier.heightIn(max = 260.dp).aspectRatio(16f / 10f)
    else Modifier.fillMaxWidth().heightIn(min = minHeight, max = 260.dp).aspectRatio(16f / 10f)
    Box(
        size
            .semantics { contentDescription = description }
            .onGloballyPositioned { c ->
                val r = c.boundsInWindow()
                placeClipVideo(video, r.left / density, r.top / density, r.width / density, r.height / density)
            }
            .drawBehind { drawRect(Color.Transparent, blendMode = BlendMode.Clear) }
            .then(if (controls) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { toggleClipVideo(video) } else Modifier),
    )
}
