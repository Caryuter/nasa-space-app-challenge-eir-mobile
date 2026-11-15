package mx.edu.utez.eir_nasa_space_apps_challenge.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

val LocalDarkTheme = compositionLocalOf { false }

val ColorScheme.isDark: Boolean
    get() = surface.luminance() < 0.5f

val MaterialTheme.isDark: Boolean
    @Composable
    @ReadOnlyComposable
    get() = LocalDarkTheme.current || MaterialTheme.colorScheme.isDark

private val DarkColorScheme = darkColorScheme(
    primary = EirEmerald,
    onPrimary = Color.White,
    primaryContainer = EirEmerald.copy(alpha = 0.2f),
    onPrimaryContainer = EirEmeraldLight,
    secondary = EirCharcoalLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF374151),
    onSecondaryContainer = Color.White,
    tertiary = EirAmber,
    onTertiary = Color.Black,
    tertiaryContainer = EirAmberSoft.copy(alpha = 0.2f),
    onTertiaryContainer = EirAmberLight,
    background = EirDarkBg,
    onBackground = EirDarkTextPrimary,
    surface = EirDarkSurface,
    onSurface = EirDarkTextPrimary,
    surfaceVariant = EirDarkHeader,
    onSurfaceVariant = EirDarkTextSecondary,
    surfaceContainer = Color(0xFF1E293B),
    surfaceContainerLow = Color(0xFF162032),
    surfaceContainerLowest = EirDarkBg,
    surfaceContainerHigh = Color(0xFF243348),
    surfaceContainerHighest = Color(0xFF334155),
    outline = EirDarkBorder,
    outlineVariant = Color(0xFF334155).copy(alpha = 0.6f),
    error = EirRoseEmergency,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = EirEmerald,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF065F46),
    secondary = EirCharcoal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3F4F6),
    onSecondaryContainer = Color(0xFF1F2937),
    tertiary = EirAmber,
    onTertiary = Color.Black,
    tertiaryContainer = EirAmberSoft,
    onTertiaryContainer = Color(0xFF78350F),
    background = EirLightBg,
    onBackground = EirLightTextPrimary,
    surface = EirLightSurface,
    onSurface = EirLightTextPrimary,
    surfaceVariant = EirLightCardSubtle,
    onSurfaceVariant = EirLightTextSecondary,
    surfaceContainer = Color(0xFFF1F5F9),
    surfaceContainerLow = Color(0xFFF8FAFC),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFE2E8F0),
    surfaceContainerHighest = Color(0xFFCBD5E1),
    outline = EirLightBorder,
    outlineVariant = Color(0xFFE2E8F0).copy(alpha = 0.7f),
    error = EirRoseEmergency,
    onError = Color.White
)

@Composable
fun EirNASASpaceAppsChallengeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
