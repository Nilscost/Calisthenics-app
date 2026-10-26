package io.github.gonbei774.calisthenicsmemory.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.calisthenics.domain.model.Target
import app.calisthenics.domain.model.TargetType
import app.calisthenics.domain.model.alternatingTotal
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

/**
 * An exercise that works both sides in one set stores its target per side; the person sees the total first, then the per-side number:
 * "12 reps (6 per side)", "40 s (20 s per side)". [alternating] false = [targetLabel] unchanged.
 */
@Composable
fun targetLabel(t: Target, alternating: Boolean): String =
    if (!alternating) targetLabel(t)
    else {
        // the per-side number is bare for reps ("6 per side"), with its unit for holds ("20 s per side")
        val perSide = if (t.type == TargetType.REPS) (if (t.max != null && t.max != t.value) "${t.value}–${t.max}" else "${t.value}") else targetLabel(t)
        stringResource(R.string.target_total_per_side, targetLabel(t.alternatingTotal()), perSide)
    }
