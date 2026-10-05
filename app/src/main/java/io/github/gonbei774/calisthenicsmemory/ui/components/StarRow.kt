package io.github.gonbei774.calisthenicsmemory.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.ui.theme.AppAccentTheme

/** Up to [total] stars; filled ones use the accent colour. Described as one unit for screen readers. */
@Composable
fun StarRow(filled: Int, modifier: Modifier = Modifier, total: Int = 5, size: Dp = 16.dp) {
    val description = pluralStringResource(R.plurals.stars_of, filled, filled, total)
    Row(modifier.semantics { contentDescription = description }) {
        repeat(total) { i ->
            Icon(
                Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(size),
                tint = if (i < filled) AppAccentTheme.colors.accent else MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}
