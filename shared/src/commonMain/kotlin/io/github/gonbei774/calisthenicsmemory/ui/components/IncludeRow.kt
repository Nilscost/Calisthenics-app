// R8.2-T03 (G3): the "Include" row of the Train setup: three toggle buttons (Warm-up, Conditioning, Cool-down). A button is gold and filled when
// that part is in the workout and outlined when it is not.
package io.github.gonbei774.calisthenicsmemory.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.gonbei774.calisthenicsmemory.ui.theme.AppAccentTheme
import io.github.gonbei774.calisthenicsmemory.ui.theme.Radius
import io.github.gonbei774.calisthenicsmemory.ui.theme.Spacing

class IncludeOption(val label: String, val on: Boolean, val tag: String, val onToggle: (Boolean) -> Unit)

@Composable
fun IncludeRow(options: List<IncludeOption>, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().testTag("include_row"), horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
        options.forEach { o ->
            val gold = AppAccentTheme.colors.accent
            Surface(
                modifier = Modifier.weight(1f).heightIn(min = 56.dp).toggleable(value = o.on, role = Role.Checkbox, onValueChange = o.onToggle).testTag(o.tag),
                shape = RoundedCornerShape(Radius.button),
                color = if (o.on) gold else MaterialTheme.colorScheme.surface,
                contentColor = if (o.on) AppAccentTheme.colors.onAccent else MaterialTheme.colorScheme.onSurface,
                border = BorderStroke(if (o.on) 0.dp else 1.5.dp, if (o.on) gold else MaterialTheme.colorScheme.outline),
            ) {
                Box(Modifier.padding(horizontal = Spacing.s, vertical = Spacing.s), contentAlignment = Alignment.Center) {
                    Text(o.label, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
