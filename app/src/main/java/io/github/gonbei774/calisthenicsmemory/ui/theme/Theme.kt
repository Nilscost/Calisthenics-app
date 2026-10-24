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

// Fixed palette, no dynamic colour. Ultramarine is the action accent; gold remains reserved for stars.
private val DarkFallback = darkColorScheme(
    primary = AccentOnDark, onPrimary = ACCENT_TEXT_DARK, primaryContainer = SurfaceHighDark, onPrimaryContainer = TextDark,
    secondary = AccentOnDark, onSecondary = ACCENT_TEXT_DARK, secondaryContainer = SurfaceHighDark, onSecondaryContainer = TextDark,
    tertiary = AccentOnDark, onTertiary = ACCENT_TEXT_DARK, tertiaryContainer = SurfaceHighDark, onTertiaryContainer = TextDark,
    background = BgDark, onBackground = TextDark, surface = SurfaceDark, onSurface = TextDark,
    surfaceVariant = SurfaceHighDark, onSurfaceVariant = TextSecondaryDark, outline = TextMutedDark, outlineVariant = DividerDark,
    surfaceContainerLowest = BgDark, surfaceContainerLow = SurfaceDark, surfaceContainer = SurfaceDark, surfaceContainerHigh = SurfaceDark, surfaceContainerHighest = SurfaceDark,
    error = ErrorDark,
)

private val LightFallback = lightColorScheme(
    primary = Accent, onPrimary = ACCENT_TEXT_LIGHT, primaryContainer = SurfaceHighLight, onPrimaryContainer = TextLight,
    secondary = Accent, onSecondary = ACCENT_TEXT_LIGHT, secondaryContainer = SurfaceHighLight, onSecondaryContainer = TextLight,
    tertiary = Accent, onTertiary = ACCENT_TEXT_LIGHT, tertiaryContainer = SurfaceHighLight, onTertiaryContainer = TextLight,
    background = BgLight, onBackground = TextLight, surface = SurfaceLight, onSurface = TextLight,
    surfaceVariant = SurfaceHighLight, onSurfaceVariant = TextSecondaryLight, outline = TextMutedLight, outlineVariant = DividerLight,
    surfaceContainerLowest = BgLight, surfaceContainerLow = SurfaceLight, surfaceContainer = SurfaceLight, surfaceContainerHigh = SurfaceLight, surfaceContainerHighest = SurfaceLight,
    error = ErrorLight,
)

/** Supporting ultramarine colours used by custom muscle and selection indicators. */
@Immutable
data class AppAccent(
    /** Ultramarine action accent. */
    val accent: Color,
    /** accentDim: secondary muscles. */
    val accentLight: Color,
    val onAccent: Color,
    /** Ultramarine text with sufficient contrast on the current background. */
    val text: Color = accent,
    /** Ultramarine for thin marks and graphics on the current background. */
    val mark: Color = accent,
)

val DarkAccent = AppAccent(AccentOnDark, AccentLight, ACCENT_TEXT_DARK, text = AccentOnDark, mark = AccentOnDark)
val LightAccent = AppAccent(Accent, AccentLight, ACCENT_TEXT_LIGHT, text = Accent, mark = Accent)

val LocalAppAccent = staticCompositionLocalOf { DarkAccent }

/** Accent for custom action and muscle indicators; achievement stars use the separate Gold token. */
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
