package io.github.gonbei774.calisthenicsmemory.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Direction B (doc 17): fixed palette, no dynamic colour. Material roles are mapped so stock components land on the tokens:
// cards = surface, selected neutral things = surfaceHigh, divider = outlineVariant, captions = outline, gold = primary.
private val DarkFallback = darkColorScheme(
    primary = Gold, onPrimary = OnGold, primaryContainer = SurfaceHighDark, onPrimaryContainer = TextDark,
    secondary = Gold, onSecondary = OnGold, secondaryContainer = SurfaceHighDark, onSecondaryContainer = TextDark,
    tertiary = Gold, onTertiary = OnGold, tertiaryContainer = SurfaceHighDark, onTertiaryContainer = TextDark,
    background = BgDark, onBackground = TextDark, surface = SurfaceDark, onSurface = TextDark,
    surfaceVariant = SurfaceHighDark, onSurfaceVariant = TextSecondaryDark, outline = TextMutedDark, outlineVariant = DividerDark,
    surfaceContainerLowest = BgDark, surfaceContainerLow = SurfaceDark, surfaceContainer = SurfaceDark, surfaceContainerHigh = SurfaceDark, surfaceContainerHighest = SurfaceDark,
    error = ErrorDark,
)

private val LightFallback = lightColorScheme(
    primary = Gold, onPrimary = OnGold, primaryContainer = SurfaceHighLight, onPrimaryContainer = TextLight,
    secondary = Gold, onSecondary = OnGold, secondaryContainer = SurfaceHighLight, onSecondaryContainer = TextLight,
    tertiary = Gold, onTertiary = OnGold, tertiaryContainer = SurfaceHighLight, onTertiaryContainer = TextLight,
    background = BgLight, onBackground = TextLight, surface = SurfaceLight, onSurface = TextLight,
    surfaceVariant = SurfaceHighLight, onSurfaceVariant = TextSecondaryLight, outline = TextMutedLight, outlineVariant = DividerLight,
    surfaceContainerLowest = BgLight, surfaceContainerLow = SurfaceLight, surfaceContainer = SurfaceLight, surfaceContainerHigh = SurfaceLight, surfaceContainerHighest = SurfaceLight,
    error = ErrorLight,
)

/** Colours that are not part of the Material scheme and must stay the same whatever the dynamic palette is. */
@Immutable
data class AppAccent(
    /** Gold: fills, stars, primary muscles, timers. */
    val accent: Color,
    /** accentDim: secondary muscles. */
    val accentLight: Color,
    val onAccent: Color,
    /** Gold as TEXT on the current background (at least 4.5:1). */
    val text: Color = accent,
    /** Gold for thin marks and graphics on the current background. */
    val mark: Color = accent,
)

val DarkAccent = AppAccent(Gold, GoldDimDark, OnGold, text = Gold, mark = Gold)
val LightAccent = AppAccent(Gold, GoldDimLight, OnGold, text = GoldTextOnLight, mark = GoldMarkOnLight)

val LocalAppAccent = staticCompositionLocalOf { DarkAccent }

/** Accent for muscles and stars: `AppTheme.accent`. */
object AppAccentTheme {
    val colors: AppAccent @Composable @ReadOnlyComposable get() = LocalAppAccent.current
}

fun appColorScheme(dark: Boolean, @Suppress("UNUSED_PARAMETER") dynamic: ColorScheme? = null): ColorScheme = if (dark) DarkFallback else LightFallback

@Composable
fun CalisthenicsMemoryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // doc 17: dynamic colour is off; the parameter stays so old callers compile
    content: @Composable () -> Unit,
) {
    val ctx = LocalContext.current
    val dynamic: ColorScheme? = null // never used: the palette is fixed (doc 17)
    val accent = if (darkTheme) DarkAccent else LightAccent
    val scheme = appColorScheme(darkTheme, dynamic)
    // V01 (V00 defect): the status bar follows the app theme (it stayed grey in dark mode). Icons are dark on a light bar.
    val view = LocalView.current
    if (!view.isInEditMode) SideEffect {
        var c: Context = view.context
        while (c is ContextWrapper && c !is Activity) c = c.baseContext
        (c as? Activity)?.window?.let { w ->
            @Suppress("DEPRECATION") w.statusBarColor = scheme.background.toArgb()
            WindowCompat.getInsetsController(w, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    CompositionLocalProvider(LocalAppAccent provides accent) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography, shapes = AppShapes, content = content)
    }
}
