package io.github.gonbei774.calisthenicsmemory.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.ui.theme.AppIcons
import io.github.gonbei774.calisthenicsmemory.ui.theme.Spacing

/** − value + with a haptic tick. Touch targets are 48 dp; [valueText] lets callers append a unit ("4 rounds"). */
@Composable
fun Stepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    modifier: Modifier = Modifier,
    step: Int = 1,
    valueText: (Int) -> String = { it.toString() },
    tag: String = "stepper",
) {
    val haptic = LocalHapticFeedback.current
    fun set(v: Int) {
        val c = v.coerceIn(range)
        if (c != value) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onValueChange(c) }
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        FilledTonalIconButton(onClick = { set(value - step) }, enabled = value > range.first, modifier = Modifier.size(Spacing.touch).testTag("${tag}_minus")) {
            Icon(AppIcons.Remove, contentDescription = stringResource(R.string.stepper_decrease))
        }
        Text(
            valueText(value), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center,
            modifier = Modifier.defaultMinSize(minWidth = 40.dp).semantics { contentDescription = valueText(value) }.testTag("${tag}_value"),
        )
        FilledTonalIconButton(onClick = { set(value + step) }, enabled = value < range.last, modifier = Modifier.size(Spacing.touch).testTag("${tag}_plus")) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.stepper_increase))
        }
    }
}
