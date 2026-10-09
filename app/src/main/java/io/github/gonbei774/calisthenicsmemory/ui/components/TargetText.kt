package io.github.gonbei774.calisthenicsmemory.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.calisthenics.domain.model.Target
import app.calisthenics.domain.model.TargetType
import io.github.gonbei774.calisthenicsmemory.R

/** "8 reps", "30 s", or a range under a Rep range / Custom rule: "5–8 reps", "10–30 s" (V21). */
@Composable
fun targetLabel(t: Target): String {
    val range = t.max != null && t.max != t.value
    return if (t.type == TargetType.REPS) {
        if (range) stringResource(R.string.target_reps_range, t.value, t.max!!) else stringResource(R.string.target_reps, t.value)
    } else {
        if (range) stringResource(R.string.target_seconds_range, t.value, t.max!!) else stringResource(R.string.target_seconds, t.value)
    }
}
