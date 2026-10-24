package io.github.gonbei774.calisthenicsmemory.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.gonbei774.calisthenicsmemory.R

/** Up to [total] stars; filled ones stay yellow independently of the action accent. */
@Composable
fun StarRow(filled: Int, modifier: Modifier = Modifier, total: Int = 5, size: Dp = 16.dp) {
    val description = pluralStringResource(R.plurals.stars_of, filled, filled, total)
    Row(modifier.semantics { contentDescription = description }) {
        repeat(total) { i ->
            if (i < filled) {
                Box(Modifier.size(size)) {
                    Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(size), tint = if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) io.github.gonbei774.calisthenicsmemory.ui.theme.GoldTextOnLight else io.github.gonbei774.calisthenicsmemory.ui.theme.Gold)
                    Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(size * 0.86f).align(Alignment.Center), tint = io.github.gonbei774.calisthenicsmemory.ui.theme.Gold)
                }
            } else {
                Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(size), tint = MaterialTheme.colorScheme.outline)
            }
        }
    }
}
