package io.github.gonbei774.calisthenicsmemory.ui.theme

import android.graphics.Typeface
import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Doc 17 typography: Barlow Condensed (screen titles, big numbers, timers, primary button labels) and IBM Plex Sans (everything else).
 * The font files are not in the repository yet (they need a download that was not approved), so the two families fall back to the
 * system condensed sans and the system sans. To switch, put the TTFs in `res/font/` and change only the two lines below.
 */
object AppFonts {
    val display: FontFamily = FontFamily(Typeface.create("sans-serif-condensed", Typeface.BOLD))
    val body: FontFamily = FontFamily.Default
}

private val base = Typography()

val AppTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = AppFonts.display, fontWeight = FontWeight.Bold),     // timers
    displayMedium = base.displayMedium.copy(fontFamily = AppFonts.display, fontWeight = FontWeight.Bold),   // the logger number
    displaySmall = base.displaySmall.copy(fontFamily = AppFonts.display, fontWeight = FontWeight.Bold),
    headlineLarge = base.headlineLarge.copy(fontFamily = AppFonts.display, fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.copy(fontFamily = AppFonts.display, fontWeight = FontWeight.Bold), // screen titles
    headlineSmall = base.headlineSmall.copy(fontFamily = AppFonts.display, fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontFamily = AppFonts.display, fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontFamily = AppFonts.body, fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontFamily = AppFonts.body, fontWeight = FontWeight.Medium),
    labelSmall = base.labelSmall.copy(fontFamily = AppFonts.body, fontWeight = FontWeight.Medium, letterSpacing = 1.2.sp),   // the small UPPER-CASE captions
)
