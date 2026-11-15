package mx.edu.utez.eir_nasa_space_apps_challenge.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.edu.utez.eir_nasa_space_apps_challenge.data.model.JournalEntry
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.theme.SolisIndigo
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.theme.SolisSky

import androidx.compose.material3.MaterialTheme
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.theme.isDark

@Composable
fun Emotional2DQuadrant(
    selectedPoint: Pair<Float, Float>, // X: 0..100 (Mood), Y: 0..100 (Energy)
    isInteractive: Boolean = true,
    entries: List<JournalEntry> = emptyList(),
    isDarkMode: Boolean = false,
    height: Dp = 150.dp,
    onPointChanged: ((Float, Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDark = isDarkMode || MaterialTheme.colorScheme.isDark
    val bgBrush = if (isDark) {
        Brush.linearGradient(
            listOf(
                Color(0xFF0F172A),
                Color(0xFF1E1B4B).copy(alpha = 0.5f),
                Color(0xFF0C4A6E).copy(alpha = 0.4f)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                Color(0xFFF1F5F9),
                Color(0xFFEEF2FF),
                Color(0xFFE0F2FE)
            )
        )
    }

    val borderColor = MaterialTheme.colorScheme.outline
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(20.dp))
            .background(bgBrush)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
    ) {
        // Labels
        Text(
            text = "Mucha Energía",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = labelColor,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 6.dp)
        )
        Text(
            text = "Poca Energía",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = labelColor,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp)
        )
        Text(
            text = "Ánimo Bajo",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = labelColor,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp)
        )
        Text(
            text = "Ánimo Alto",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = labelColor,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp)
        )

        // Canvas for grid axes & points
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isInteractive && onPointChanged != null) {
                        Modifier
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    val nx = ((offset.x / size.width) * 100f).coerceIn(6f, 94f)
                                    val ny = ((1f - (offset.y / size.height)) * 100f).coerceIn(6f, 94f)
                                    onPointChanged(nx, ny)
                                }
                            }
                            .pointerInput(Unit) {
                                detectDragGestures { change, _ ->
                                    change.consume()
                                    val nx = ((change.position.x / size.width) * 100f).coerceIn(6f, 94f)
                                    val ny = ((1f - (change.position.y / size.height)) * 100f).coerceIn(6f, 94f)
                                    onPointChanged(nx, ny)
                                }
                            }
                    } else Modifier
                )
        ) {
            val w = size.width
            val h = size.height

            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            val axisColor = if (isDark) Color(0xFF475569) else Color(0xFF94A3B8)

            // Horizontal Midline
            drawLine(
                color = axisColor,
                start = Offset(0f, h / 2f),
                end = Offset(w, h / 2f),
                strokeWidth = 1.dp.toPx(),
                pathEffect = dashEffect
            )

            // Vertical Midline
            drawLine(
                color = axisColor,
                start = Offset(w / 2f, 0f),
                end = Offset(w / 2f, h),
                strokeWidth = 1.dp.toPx(),
                pathEffect = dashEffect
            )

            // Historical Entries dots
            entries.forEachIndexed { index, entry ->
                val px = (entry.x / 100f) * w
                val py = (1f - (entry.y / 100f)) * h
                if (index == 0) {
                    drawCircle(
                        color = SolisIndigo.copy(alpha = 0.3f),
                        radius = 12.dp.toPx(),
                        center = Offset(px, py)
                    )
                    drawCircle(
                        color = SolisIndigo,
                        radius = 7.dp.toPx(),
                        center = Offset(px, py)
                    )
                } else {
                    drawCircle(
                        color = SolisSky.copy(alpha = 0.7f),
                        radius = 4.dp.toPx(),
                        center = Offset(px, py)
                    )
                }
            }

            // Current Interactive Marker (if interactive mode)
            if (isInteractive) {
                val cx = (selectedPoint.first / 100f) * w
                val cy = (1f - (selectedPoint.second / 100f)) * h

                drawCircle(
                    color = SolisIndigo.copy(alpha = 0.35f),
                    radius = 14.dp.toPx(),
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = SolisIndigo,
                    radius = 8.dp.toPx(),
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = Color.White,
                    radius = 8.dp.toPx(),
                    center = Offset(cx, cy),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Emotional2DQuadrant Light")
@Composable
fun Emotional2DQuadrantLightPreview() {
    mx.edu.utez.eir_nasa_space_apps_challenge.ui.theme.EirNASASpaceAppsChallengeTheme(darkTheme = false) {
        Emotional2DQuadrant(
            selectedPoint = Pair(74f, 64f),
            isInteractive = true,
            isDarkMode = false
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Emotional2DQuadrant Dark")
@Composable
fun Emotional2DQuadrantDarkPreview() {
    mx.edu.utez.eir_nasa_space_apps_challenge.ui.theme.EirNASASpaceAppsChallengeTheme(darkTheme = true) {
        Emotional2DQuadrant(
            selectedPoint = Pair(74f, 64f),
            isInteractive = true,
            isDarkMode = true
        )
    }
}

