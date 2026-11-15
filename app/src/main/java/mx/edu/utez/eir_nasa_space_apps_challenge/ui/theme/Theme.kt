package mx.edu.utez.eir_nasa_space_apps_challenge.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = EirSky,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF0369A1),
    onPrimaryContainer = Color.White,
    secondary = EirIndigo,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF3730A3),
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
    primary = EirSky,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = EirIndigo,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEEF2FF),
    onSecondaryContainer = Color(0xFF3730A3),
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
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
