package mx.edu.utez.eir_nasa_space_apps_challenge.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.edu.utez.eir_nasa_space_apps_challenge.R
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.theme.*

/**
 * Emociones disponibles para la mascota espacial EIR
 */
enum class MascotEmotion(val iconRes: Int, val displayName: String) {
    IDLE(R.drawable.mascot_idle, "Tranquilo"),
    HAPPY(R.drawable.mascot_happy, "Alegre"),
    THINKING(R.drawable.mascot_thinking, "Analizando"),
    CONCERNED(R.drawable.mascot_concerned, "Preocupado"),
    SLEEP(R.drawable.mascot_sleep, "Descansando")
}

/**
 * Composable reutilizable para mostrar el avatar de la mascota
 */
@Composable
fun MascotAvatar(
    emotion: MascotEmotion,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    @Suppress("UNUSED_PARAMETER") showGlow: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "MascotGlow")
    val scaleValue by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(if (emotion == MascotEmotion.THINKING) scaleValue else 1f)
            .clip(CircleShape)
            .then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = emotion.iconRes),
            contentDescription = "Mascota EIR (${emotion.displayName})",
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Overlay flotante interactivo para la mascota en pantalla principal
 */
@Composable
fun MascotCompanionOverlay(
    isDarkMode: Boolean,
    modifier: Modifier = Modifier,
    currentEmotion: MascotEmotion = MascotEmotion.IDLE
) {
    var isExpanded by remember { mutableStateOf(false) }
    var tipIndex by remember { mutableIntStateOf(0) }

    val spaceTips = remember {
        listOf(
            "¡Hola Astronauta! Tus biometrías del Sol 142 están bajo monitoreo continuo 🚀",
            "Recuerda hidratarte regularmente. El entorno de cabina seca requiere 2.5L de agua al día.",
            "Una siesta de 20 minutos puede optimizar tus niveles de enfoque cognitivo.",
            "Tus niveles de Varianza de Frecuencia Cardíaca (HRV) sugieren un descanso balanceado.",
            "¡Excelente trabajo completando tu registro de hábitos diarios!"
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(end = 16.dp, bottom = 80.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Globo de diálogo interactivo (Speech Bubble)
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + scaleIn(transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 1f)),
                exit = fadeOut() + scaleOut(transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 1f))
            ) {
                Surface(
                    modifier = Modifier
                        .widthIn(max = 270.dp)
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp))
                        .border(
                            width = 1.5.dp,
                            brush = Brush.horizontalGradient(
                                colors = listOf(EirSky, EirIndigo)
                            ),
                            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp)
                        ),
                    color = if (isDarkMode) Color(0xFF0F172A).copy(alpha = 0.95f) else Color(0xFFFFFFFF).copy(alpha = 0.95f),
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = "Consejo",
                                    tint = EirAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "EIR Compañero",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EirSky
                                )
                            }

                            IconButton(
                                onClick = { isExpanded = false },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cerrar",
                                    tint = if (isDarkMode) EirDarkTextSecondary else EirLightTextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Text(
                            text = spaceTips[tipIndex % spaceTips.size],
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDarkMode) EirDarkTextPrimary else EirLightTextPrimary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { tipIndex = (tipIndex + 1) % spaceTips.size },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Otro consejo",
                                    tint = EirSky,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Otro consejo",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EirSky
                                )
                            }
                        }
                    }
                }
            }

            // Avatar Botón Flotante
            Surface(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { isExpanded = !isExpanded },
                color = Color.Transparent
            ) {
                MascotAvatar(
                    emotion = currentEmotion,
                    size = 56.dp,
                    showGlow = true
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MascotCompanionPreview() {
    EirNASASpaceAppsChallengeTheme(darkTheme = true) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MascotEmotion.entries.forEach { emotion ->
                    MascotAvatar(emotion = emotion, size = 48.dp)
                }
            }
            MascotCompanionOverlay(isDarkMode = true)
        }
    }
}
