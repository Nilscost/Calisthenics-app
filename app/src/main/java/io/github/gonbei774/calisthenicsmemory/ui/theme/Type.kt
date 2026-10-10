package io.github.gonbei774.calisthenicsmemory.ui.theme

import io.github.gonbei774.calisthenicsmemory.R

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Doc 17 typography: Barlow Condensed (screen titles, big numbers, timers, primary button labels) and IBM Plex Sans (everything else).
 * Both are SIL OFL 1.1, bundled in `res/font/` from github.com/google/fonts (owner-approved 2026-10-09; licences in `licenses/fonts/`).
 * IBM Plex Sans ships there as one variable font; each weight is an instance of it (variable fonts need API 26 = our minSdk).
 */
@OptIn(ExperimentalTextApi::class)
object AppFonts {
    val display: FontFamily = FontFamily(
        Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold),
        Font(R.font.barlow_condensed_bold, FontWeight.Bold),
    )
    private fun plex(w: FontWeight) = Font(R.font.ibm_plex_sans, w, variationSettings = FontVariation.Settings(FontVariation.weight(w.weight)))
    val body: FontFamily = FontFamily(plex(FontWeight.Normal), plex(FontWeight.Medium), plex(FontWeight.SemiBold), plex(FontWeight.Bold))
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
