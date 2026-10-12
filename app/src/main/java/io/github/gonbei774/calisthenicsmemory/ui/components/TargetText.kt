package io.github.gonbei774.calisthenicsmemory.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.calisthenics.domain.model.Target
import app.calisthenics.domain.model.TargetType
import io.github.gonbei774.calisthenicsmemory.R

/** "8 reps", "30 s". Reps under a Rep range / Custom rule show the range ("5–8 reps"); holds show the top ("30 s", R7-T01). */
@Composable
fun targetLabel(t: Target): String {
    val range = t.max != null && t.max != t.value
    return if (t.type == TargetType.REPS) {
        if (range) stringResource(R.string.target_reps_range, t.value, t.max!!) else stringResource(R.string.target_reps, t.value)
    } else {
        stringResource(R.string.target_seconds, t.shownValue())
    }
}
