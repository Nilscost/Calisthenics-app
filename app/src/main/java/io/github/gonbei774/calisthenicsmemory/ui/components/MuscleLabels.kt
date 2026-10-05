package io.github.gonbei774.calisthenicsmemory.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.calisthenics.domain.model.Muscle
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.ui.theme.AppAccentTheme

fun muscleLabel(m: Muscle): Int = when (m) {
    Muscle.CHEST -> R.string.muscle_chest
    Muscle.FRONT_DELTS -> R.string.muscle_front_delts
    Muscle.SIDE_DELTS -> R.string.muscle_side_delts
    Muscle.REAR_DELTS -> R.string.muscle_rear_delts
    Muscle.TRICEPS -> R.string.muscle_triceps
    Muscle.BICEPS -> R.string.muscle_biceps
    Muscle.FOREARMS -> R.string.muscle_forearms
    Muscle.LATS -> R.string.muscle_lats
    Muscle.UPPER_BACK -> R.string.muscle_upper_back
    Muscle.LOWER_BACK -> R.string.muscle_lower_back
    Muscle.ABS -> R.string.muscle_abs
    Muscle.OBLIQUES -> R.string.muscle_obliques
    Muscle.GLUTES -> R.string.muscle_glutes
    Muscle.QUADS -> R.string.muscle_quads
    Muscle.HAMSTRINGS -> R.string.muscle_hamstrings
    Muscle.CALVES -> R.string.muscle_calves
    Muscle.HIP_FLEXORS -> R.string.muscle_hip_flexors
    Muscle.ADDUCTORS -> R.string.muscle_adductors
}

/** Primary muscles in the accent colour, secondary ones muted: the same colour code the clips use. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun MuscleChips(primary: List<Muscle>, secondary: List<Muscle> = emptyList(), modifier: Modifier = Modifier) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        primary.forEach { m ->
            Surface(shape = RoundedCornerShape(8.dp), color = AppAccentTheme.colors.accent) {
                Text(stringResource(muscleLabel(m)), Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelMedium, color = AppAccentTheme.colors.onAccent)
            }
        }
        secondary.forEach { m ->
            Surface(shape = RoundedCornerShape(8.dp), color = AppAccentTheme.colors.accentLight.copy(alpha = 0.45f)) {
                Text(stringResource(muscleLabel(m)), Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
