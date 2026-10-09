// Doc 17: gold is used as text only through accent.text (4.5:1 on the current background), and the selected segment is gold.
package io.github.gonbei774.calisthenicsmemory.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.gonbei774.calisthenicsmemory.ui.theme.AppAccentTheme

@Composable
fun AppTextButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) =
    TextButton(onClick, modifier, enabled, colors = ButtonDefaults.textButtonColors(contentColor = AppAccentTheme.colors.text), content = content)

@Composable
fun AppOutlinedButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) =
    OutlinedButton(onClick, modifier, enabled, colors = ButtonDefaults.outlinedButtonColors(contentColor = AppAccentTheme.colors.text),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant), content = content)

@Composable
fun appSegmentedColors() = SegmentedButtonDefaults.colors(
    activeContainerColor = AppAccentTheme.colors.accent, activeContentColor = AppAccentTheme.colors.onAccent, activeBorderColor = AppAccentTheme.colors.accent,
    inactiveContainerColor = MaterialTheme.colorScheme.surface, inactiveContentColor = MaterialTheme.colorScheme.onSurface, inactiveBorderColor = MaterialTheme.colorScheme.surfaceVariant,
)
