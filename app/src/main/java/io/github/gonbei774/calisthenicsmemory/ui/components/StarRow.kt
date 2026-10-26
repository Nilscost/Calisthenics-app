package io.github.gonbei774.calisthenicsmemory.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.scale
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.gonbei774.calisthenicsmemory.R

/**
 * Up to [total] stars; filled ones stay yellow independently of the action accent.
 * [popFrom]: the filled stars from this index on (the newly earned ones) pop in once, one after the other, with a soft halo (end of workout).
 */
@Composable
fun StarRow(filled: Int, modifier: Modifier = Modifier, total: Int = 5, size: Dp = 16.dp, popFrom: Int? = null) {
    val description = pluralStringResource(R.plurals.stars_of, filled, filled, total)
    Row(modifier.semantics { contentDescription = description }) {
        repeat(total) { i ->
            if (i < filled) {
                val pop = popFrom != null && i >= popFrom
                val scale = remember { Animatable(if (pop) 0.5f else 1f) }
                if (pop) LaunchedEffect(Unit) {
                    delay(250L + 220L * (i - popFrom!!))
                    scale.animateTo(1.45f, tween(260))
                    scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                }
                Box(Modifier.size(size).scale(scale.value).then(if (pop) Modifier.background(io.github.gonbei774.calisthenicsmemory.ui.theme.Gold.copy(alpha = 0.22f), CircleShape) else Modifier)) {
                    Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(size), tint = if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) io.github.gonbei774.calisthenicsmemory.ui.theme.GoldTextOnLight else io.github.gonbei774.calisthenicsmemory.ui.theme.Gold)
                    Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(size * 0.86f).align(Alignment.Center), tint = io.github.gonbei774.calisthenicsmemory.ui.theme.Gold)
                }
            } else {
                Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(size), tint = MaterialTheme.colorScheme.outline)
            }
        }
    }
}
