package mx.edu.utez.eir_nasa_space_apps_challenge.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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
    background = EirDarkBg,
    onBackground = EirDarkTextPrimary,
    surface = EirDarkSurface,
    onSurface = EirDarkTextPrimary,
    surfaceVariant = EirDarkHeader,
    onSurfaceVariant = EirDarkTextSecondary,
    outline = EirDarkBorder,
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
    background = EirLightBg,
    onBackground = EirLightTextPrimary,
    surface = EirLightSurface,
    onSurface = EirLightTextPrimary,
    surfaceVariant = EirLightCardSubtle,
    onSurfaceVariant = EirLightTextSecondary,
    outline = EirLightBorder,
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
