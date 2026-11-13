package io.github.gonbei774.calisthenicsmemory.ui.components

import org.jetbrains.compose.resources.StringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import app.calisthenics.domain.model.Muscle
import io.github.gonbei774.calisthenicsmemory.shared.resources.*
import io.github.gonbei774.calisthenicsmemory.ui.theme.AppAccentTheme

fun muscleLabel(m: Muscle): StringResource = when (m) {
    Muscle.CHEST -> Res.string.muscle_chest
    Muscle.FRONT_DELTS -> Res.string.muscle_front_delts
    Muscle.SIDE_DELTS -> Res.string.muscle_side_delts
    Muscle.REAR_DELTS -> Res.string.muscle_rear_delts
    Muscle.TRICEPS -> Res.string.muscle_triceps
    Muscle.BICEPS -> Res.string.muscle_biceps
    Muscle.FOREARMS -> Res.string.muscle_forearms
    Muscle.LATS -> Res.string.muscle_lats
    Muscle.UPPER_BACK -> Res.string.muscle_upper_back
    Muscle.LOWER_BACK -> Res.string.muscle_lower_back
    Muscle.ABS -> Res.string.muscle_abs
    Muscle.OBLIQUES -> Res.string.muscle_obliques
    Muscle.GLUTES -> Res.string.muscle_glutes
    Muscle.QUADS -> Res.string.muscle_quads
    Muscle.HAMSTRINGS -> Res.string.muscle_hamstrings
    Muscle.CALVES -> Res.string.muscle_calves
    Muscle.HIP_FLEXORS -> Res.string.muscle_hip_flexors
    Muscle.ADDUCTORS -> Res.string.muscle_adductors
    Muscle.NECK -> Res.string.muscle_neck
    Muscle.TIBIALIS -> Res.string.muscle_tibialis
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
