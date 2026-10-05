package io.github.gonbei774.calisthenicsmemory.ui.theme

import android.os.Build
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

private val LightFallback = lightColorScheme(
    primary = Teal40, onPrimary = Color.White, primaryContainer = Teal90, onPrimaryContainer = Teal10,
    secondary = Teal30, onSecondary = Color.White, secondaryContainer = Teal95, onSecondaryContainer = Teal10,
    tertiary = Accent, onTertiary = Color.White,
    background = Slate99, onBackground = Slate10, surface = Slate99, onSurface = Slate10,
    surfaceVariant = Slate95, onSurfaceVariant = Slate20, error = ErrorLight,
)

private val DarkFallback = darkColorScheme(
    primary = Teal80, onPrimary = Teal20, primaryContainer = Teal30, onPrimaryContainer = Teal90,
    secondary = Teal80, onSecondary = Teal20, secondaryContainer = Teal30, onSecondaryContainer = Teal95,
    tertiary = AccentOnDark, onTertiary = Color.Black,
    background = Slate10, onBackground = Slate90, surface = Slate10, onSurface = Slate90,
    surfaceVariant = Slate20, onSurfaceVariant = Slate90, error = ErrorDark,
)

/** Colours that are not part of the Material scheme and must stay the same whatever the dynamic palette is. */
@Immutable
data class AppAccent(val accent: Color, val accentLight: Color, val onAccent: Color)

val LocalAppAccent = staticCompositionLocalOf { AppAccent(Accent, AccentLight, Color.White) }

/** Accent for muscles and stars: `AppTheme.accent`. */
object AppAccentTheme {
    val colors: AppAccent @Composable @ReadOnlyComposable get() = LocalAppAccent.current
}

fun appColorScheme(dark: Boolean, dynamic: ColorScheme?): ColorScheme = dynamic ?: if (dark) DarkFallback else LightFallback

@Composable
fun CalisthenicsMemoryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val ctx = LocalContext.current
    val dynamic = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
    } else null
    val accent = if (darkTheme) AppAccent(AccentOnDark, AccentLight, Color.Black) else AppAccent(Accent, AccentLight, Color.White)
    CompositionLocalProvider(LocalAppAccent provides accent) {
        MaterialTheme(colorScheme = appColorScheme(darkTheme, dynamic), typography = AppTypography, shapes = AppShapes, content = content)
    }
}
