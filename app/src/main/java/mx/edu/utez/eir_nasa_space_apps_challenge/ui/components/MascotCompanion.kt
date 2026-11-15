package mx.edu.utez.eir_nasa_space_apps_challenge.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
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
 * Overlay flotante interactivo y arrastrable para la mascota EIR.
 * Se ubica inicialmente en una posición despejada que no obstruye la navegación
 * y permite al astronauta arrastrarla libremente por la pantalla.
 */
@Composable
fun MascotCompanionOverlay(
    isDarkMode: Boolean,
    modifier: Modifier = Modifier,
    currentEmotion: MascotEmotion = MascotEmotion.IDLE
) {
    var isExpanded by remember { mutableStateOf(false) }
    var tipIndex by remember { mutableIntStateOf(0) }
    var isDragging by remember { mutableStateOf(false) }

    val spaceTips = remember {
        listOf(
            "¡Hola Astronauta! Tus biometrías del Sol 142 están bajo monitoreo continuo 🚀",
            "Recuerda hidratarte regularmente. El entorno de cabina seca requiere 2.5L de agua al día.",
            "Una siesta de 20 minutos puede optimizar tus niveles de enfoque cognitivo.",
            "Tus niveles de Varianza de Frecuencia Cardíaca (HRV) sugieren un descanso balanceado.",
            "¡Excelente trabajo completando tu registro de hábitos diarios!"
        )
    }

    val mascotSize = 56.dp

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val density = LocalDensity.current
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val screenHeightPx = with(density) { maxHeight.toPx() }
        val mascotSizePx = with(density) { mascotSize.toPx() }

        // Márgenes seguros: evitar tapar la barra superior (TopBar) y la barra inferior de navegación (BottomBar)
        val minX = with(density) { 12.dp.toPx() }
        val maxX = (screenWidthPx - mascotSizePx - with(density) { 12.dp.toPx() }).coerceAtLeast(minX)

        val minY = with(density) { 76.dp.toPx() } // Debajo del TopAppBar / barra de estado
        val maxY = (screenHeightPx - mascotSizePx - with(density) { 96.dp.toPx() }).coerceAtLeast(minY) // Por encima del BottomNav

        // Posición inicial: a la derecha, en zona media despejada (despejando completamente la navegación inferior)
        val initialX = remember(maxX) {
            (screenWidthPx - mascotSizePx - with(density) { 16.dp.toPx() }).coerceIn(minX, maxX)
        }
        val initialY = remember(screenHeightPx, minY, maxY) {
            (screenHeightPx * 0.52f).coerceIn(minY, maxY)
        }

        var offsetX by remember { mutableFloatStateOf(-1f) }
        var offsetY by remember { mutableFloatStateOf(-1f) }

        val currentX = if (offsetX < 0f) initialX else offsetX.coerceIn(minX, maxX)
        val currentY = if (offsetY < 0f) initialY else offsetY.coerceIn(minY, maxY)

        val isOnRight = (currentX + mascotSizePx / 2f) >= (screenWidthPx / 2f)
        val isAbove = (currentY + mascotSizePx / 2f) >= (screenHeightPx / 2f)

        var bubbleSize by remember { mutableStateOf(IntSize.Zero) }
        val bubbleWidthPx = if (bubbleSize.width > 0) bubbleSize.width.toFloat() else with(density) { 260.dp.toPx() }
        val bubbleHeightPx = if (bubbleSize.height > 0) bubbleSize.height.toFloat() else with(density) { 140.dp.toPx() }

        val dragScale by animateFloatAsState(
            targetValue = if (isDragging) 1.15f else 1.0f,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "dragScale"
        )

        val elevation by animateDpAsState(
            targetValue = if (isDragging) 14.dp else 4.dp,
            label = "elevation"
        )

        val bubbleShape = remember(isAbove, isOnRight) {
            when {
                isAbove && isOnRight -> RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp)
                isAbove && !isOnRight -> RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 20.dp)
                !isAbove && isOnRight -> RoundedCornerShape(topStart = 20.dp, topEnd = 4.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
                else -> RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
            }
        }

        val transformOrigin = remember(isAbove, isOnRight) {
            when {
                isAbove && isOnRight -> TransformOrigin(1f, 1f)
                isAbove && !isOnRight -> TransformOrigin(0f, 1f)
                !isAbove && isOnRight -> TransformOrigin(1f, 0f)
                else -> TransformOrigin(0f, 0f)
            }
        }

        // Globo de diálogo interactivo (Speech Bubble)
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + scaleIn(transformOrigin = transformOrigin),
            exit = fadeOut() + scaleOut(transformOrigin = transformOrigin),
            modifier = Modifier.offset {
                val targetBubbleX = if (isOnRight) {
                    currentX + mascotSizePx - bubbleWidthPx
                } else {
                    currentX
                }
                val bubbleX = targetBubbleX.coerceIn(minX, (screenWidthPx - bubbleWidthPx - minX).coerceAtLeast(minX))

                val targetBubbleY = if (isAbove) {
                    currentY - bubbleHeightPx - 8.dp.toPx()
                } else {
                    currentY + mascotSizePx + 8.dp.toPx()
                }
                val bubbleY = targetBubbleY.coerceIn(minY, (screenHeightPx - bubbleHeightPx - 90.dp.toPx()).coerceAtLeast(minY))

                IntOffset(bubbleX.roundToInt(), bubbleY.roundToInt())
            }
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 270.dp)
                    .onSizeChanged { bubbleSize = it }
                    .clip(bubbleShape)
                    .border(
                        width = 1.5.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(EirSky, EirIndigo)
                        ),
                        shape = bubbleShape
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

        // Avatar Flotante Arrastrable (Draggable Mascot Avatar)
        Surface(
            modifier = Modifier
                .offset { IntOffset(currentX.roundToInt(), currentY.roundToInt()) }
                .scale(dragScale)
                .shadow(elevation, CircleShape)
                .clip(CircleShape)
                .pointerInput(minX, maxX, minY, maxY) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var totalDragDistance = 0f
                        val touchSlop = viewConfiguration.touchSlop
                        var dragging = false

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break

                            if (change.changedToUp()) {
                                if (!dragging) {
                                    isExpanded = !isExpanded
                                }
                                isDragging = false
                                change.consume()
                                break
                            }

                            val dragDelta = change.position - change.previousPosition
                            totalDragDistance += dragDelta.getDistance()

                            if (!dragging && totalDragDistance > touchSlop) {
                                dragging = true
                                isDragging = true
                                isExpanded = false
                            }

                            if (dragging) {
                                change.consume()
                                offsetX = (currentX + dragDelta.x).coerceIn(minX, maxX)
                                offsetY = (currentY + dragDelta.y).coerceIn(minY, maxY)
                            }
                        }
                    }
                },
            color = Color.Transparent,
            shape = CircleShape
        ) {
            MascotAvatar(
                emotion = currentEmotion,
                size = mascotSize,
                showGlow = true
            )
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
            Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                MascotCompanionOverlay(isDarkMode = true)
            }
        }
    }
}
