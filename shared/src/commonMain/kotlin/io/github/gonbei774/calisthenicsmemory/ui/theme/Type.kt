package io.github.gonbei774.calisthenicsmemory.ui.theme

import io.github.gonbei774.calisthenicsmemory.shared.resources.Res
import io.github.gonbei774.calisthenicsmemory.shared.resources.barlow_condensed_bold
import io.github.gonbei774.calisthenicsmemory.shared.resources.barlow_condensed_semibold
import io.github.gonbei774.calisthenicsmemory.shared.resources.ibm_plex_sans
import org.jetbrains.compose.resources.Font
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Doc 17 typography: Barlow Condensed (screen titles, big numbers, timers, primary button labels) and IBM Plex Sans (everything else).
 * Both are SIL OFL 1.1, bundled in `composeResources/font/` from github.com/google/fonts (owner-approved 2026-10-09; licences in `licenses/fonts/`).
 * IBM Plex Sans ships there as one variable font; each weight is an instance of it (variable fonts need API 26 = our minSdk on Android).
 */
@OptIn(ExperimentalTextApi::class)
object AppFonts {
    val display: FontFamily
        @Composable get() {
            val semiBold = Font(Res.font.barlow_condensed_semibold, FontWeight.SemiBold)
            val bold = Font(Res.font.barlow_condensed_bold, FontWeight.Bold)
            return remember(semiBold, bold) { FontFamily(semiBold, bold) }
        }

    @Composable
    private fun plex(w: FontWeight) = Font(Res.font.ibm_plex_sans, w, variationSettings = FontVariation.Settings(FontVariation.weight(w.weight)))

    val body: FontFamily
        @Composable get() {
            val normal = plex(FontWeight.Normal)
            val medium = plex(FontWeight.Medium)
            val semiBold = plex(FontWeight.SemiBold)
            val bold = plex(FontWeight.Bold)
            return remember(normal, medium, semiBold, bold) { FontFamily(normal, medium, semiBold, bold) }
        }
}

private val base = Typography()

val AppTypography: Typography
    @Composable get() {
        val display = AppFonts.display
        val body = AppFonts.body
        return remember(display, body) { appTypography(display, body) }
    }

private fun appTypography(display: FontFamily, body: FontFamily) = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = display, fontWeight = FontWeight.Bold),     // timers
    displayMedium = base.displayMedium.copy(fontFamily = display, fontWeight = FontWeight.Bold),   // the logger number
    displaySmall = base.displaySmall.copy(fontFamily = display, fontWeight = FontWeight.Bold),
    headlineLarge = base.headlineLarge.copy(fontFamily = display, fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.copy(fontFamily = display, fontWeight = FontWeight.Bold), // screen titles
    headlineSmall = base.headlineSmall.copy(fontFamily = display, fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontFamily = display, fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontFamily = body, fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontFamily = body, fontWeight = FontWeight.Medium),
    labelSmall = base.labelSmall.copy(fontFamily = body, fontWeight = FontWeight.Medium, letterSpacing = 1.2.sp),   // the small UPPER-CASE captions
)
