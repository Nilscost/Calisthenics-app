package io.github.gonbei774.calisthenicsmemory.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight

// Only the Material 3 scale is used by screens (plan §6); a few weights are tuned for glanceability during a workout.
private val base = Typography()

val AppTypography = Typography(
    displayLarge = base.displayLarge.copy(fontWeight = FontWeight.Bold),   // session timer only
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Medium),
)
