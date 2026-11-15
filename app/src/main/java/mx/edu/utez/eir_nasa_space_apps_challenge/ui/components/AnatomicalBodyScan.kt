package mx.edu.utez.eir_nasa_space_apps_challenge.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mx.edu.utez.eir_nasa_space_apps_challenge.R
import mx.edu.utez.eir_nasa_space_apps_challenge.data.model.Discomfort
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.theme.*
import kotlin.math.sqrt

data class AnatomicalZoneNode(
    val id: String,
    val name: String,
    val relX: Float, // 0..1
    val relY: Float, // 0..1
    val radiusFactor: Float = 0.18f, // relative to width
    val leaderTargetX: Float = -1f // -1 left, 1 right
)

enum class BodyZoneStatus {
    OPTIMAL,   // Verde 0xFF4ADE80
    MODERATE,  // Amarillo 0xFFFACC15
    CRITICAL   // Rojo 0xFFF43F5E
}

@Composable
fun RealisticBodyScan(
    selectedZoneId: String,
    discomforts: List<Discomfort>,
    isDarkMode: Boolean,
    getZoneStatus: (String) -> BodyZoneStatus = { zoneId ->
        val hasDiscomfort = discomforts.any { it.zoneId == zoneId }
        if (hasDiscomfort) BodyZoneStatus.MODERATE else BodyZoneStatus.OPTIMAL
    },
    onSelectZone: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "haloPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // 5 anatomical zones: Head, Chest, Abdomen, Arms, Legs
    val zoneNodes = listOf(
        AnatomicalZoneNode("head", "Cabeza", 0.50f, 0.12f, radiusFactor = 0.22f, leaderTargetX = -1f),
        AnatomicalZoneNode("chest", "Pecho", 0.50f, 0.28f, radiusFactor = 0.25f, leaderTargetX = 1f),
        AnatomicalZoneNode("abdomen", "Abdomen", 0.50f, 0.44f, radiusFactor = 0.22f, leaderTargetX = -1f),
        AnatomicalZoneNode("arms", "Brazos", 0.32f, 0.36f, radiusFactor = 0.18f, leaderTargetX = -1f),
        AnatomicalZoneNode("legs", "Piernas", 0.44f, 0.70f, radiusFactor = 0.28f, leaderTargetX = 1f)
    )

    // Zone animated colors
    val animatedHeadColor by animateColorAsState(
        targetValue = when (getZoneStatus("head")) {
            BodyZoneStatus.CRITICAL -> Color(0xFFF43F5E)
            BodyZoneStatus.MODERATE -> Color(0xFFFACC15)
            BodyZoneStatus.OPTIMAL -> Color(0xFF4ADE80)
        },
        animationSpec = tween(600),
        label = "headColor"
    )

    val animatedChestColor by animateColorAsState(
        targetValue = when (getZoneStatus("chest")) {
            BodyZoneStatus.CRITICAL -> Color(0xFFF43F5E)
            BodyZoneStatus.MODERATE -> Color(0xFFFACC15)
            BodyZoneStatus.OPTIMAL -> Color(0xFF4ADE80)
        },
        animationSpec = tween(600),
        label = "chestColor"
    )

    val animatedAbdomenColor by animateColorAsState(
        targetValue = when (getZoneStatus("abdomen")) {
            BodyZoneStatus.CRITICAL -> Color(0xFFF43F5E)
            BodyZoneStatus.MODERATE -> Color(0xFFFACC15)
            BodyZoneStatus.OPTIMAL -> Color(0xFF4ADE80)
        },
        animationSpec = tween(600),
        label = "abdomenColor"
    )

    val animatedArmsColor by animateColorAsState(
        targetValue = when (getZoneStatus("arms")) {
            BodyZoneStatus.CRITICAL -> Color(0xFFF43F5E)
            BodyZoneStatus.MODERATE -> Color(0xFFFACC15)
            BodyZoneStatus.OPTIMAL -> Color(0xFF4ADE80)
        },
        animationSpec = tween(600),
        label = "armsColor"
    )

    val animatedLegsColor by animateColorAsState(
        targetValue = when (getZoneStatus("legs")) {
            BodyZoneStatus.CRITICAL -> Color(0xFFF43F5E)
            BodyZoneStatus.MODERATE -> Color(0xFFFACC15)
            BodyZoneStatus.OPTIMAL -> Color(0xFF4ADE80)
        },
        animationSpec = tween(600),
        label = "legsColor"
    )

    fun getColorForZone(zoneId: String): Color {
        return when (zoneId) {
            "head" -> animatedHeadColor
            "chest" -> animatedChestColor
            "abdomen" -> animatedAbdomenColor
            "arms" -> animatedArmsColor
            "legs" -> animatedLegsColor
            else -> animatedChestColor
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp),
        contentAlignment = Alignment.Center
    ) {
        // Layer 1 & 2: Offscreen compositing container for exact image masking
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        ) {
            // Layer 1: Base Image (Silhouette Source & Alpha Mask)
            Image(
                painter = painterResource(id = R.drawable.human_body),
                contentDescription = "Cuerpo Humano Volumétrico",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            // Layer 2: Masked Dynamic Heatmap Canvas using BlendMode.SrcAtop
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Draw heatmaps for each section using BlendMode.SrcAtop
                zoneNodes.forEach { node ->
                    val zoneColor = getColorForZone(node.id)
                    val centerOffset = Offset(node.relX * w, node.relY * h)
                    val radius = w * node.radiusFactor

                    // Soft radial gradient for volumetric heatmap
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                zoneColor.copy(alpha = 0.65f),
                                zoneColor.copy(alpha = 0.35f),
                                Color.Transparent
                            ),
                            center = centerOffset,
                            radius = radius
                        ),
                        radius = radius,
                        center = centerOffset,
                        blendMode = BlendMode.SrcAtop
                    )

                    // Secondary symmetrical heatmap for arms / legs
                    if (node.id == "arms") {
                        val rightArmOffset = Offset((1f - node.relX) * w, node.relY * h)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    zoneColor.copy(alpha = 0.65f),
                                    zoneColor.copy(alpha = 0.35f),
                                    Color.Transparent
                                ),
                                center = rightArmOffset,
                                radius = radius
                            ),
                            radius = radius,
                            center = rightArmOffset,
                            blendMode = BlendMode.SrcAtop
                        )
                    } else if (node.id == "legs") {
                        val rightLegOffset = Offset((1f - node.relX) * w, node.relY * h)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    zoneColor.copy(alpha = 0.65f),
                                    zoneColor.copy(alpha = 0.35f),
                                    Color.Transparent
                                ),
                                center = rightLegOffset,
                                radius = radius
                            ),
                            radius = radius,
                            center = rightLegOffset,
                            blendMode = BlendMode.SrcAtop
                        )
                    }
                }
            }
        }

        // Layer 3: Interactive Leader Lines, Glowing Nodes & Touch Handler
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val w = size.width
                        val h = size.height
                        var closestZone: String? = null
                        var minDist = Float.MAX_VALUE

                        zoneNodes.forEach { node ->
                            val nx = node.relX * w
                            val ny = node.relY * h
                            val dist = sqrt((offset.x - nx) * (offset.x - nx) + (offset.y - ny) * (offset.y - ny))

                            var altDist = Float.MAX_VALUE
                            if (node.id == "arms" || node.id == "legs") {
                                val altX = (1f - node.relX) * w
                                altDist = sqrt((offset.x - altX) * (offset.x - altX) + (offset.y - ny) * (offset.y - ny))
                            }

                            val effectiveDist = minOf(dist, altDist)
                            if (effectiveDist < 45.dp.toPx() && effectiveDist < minDist) {
                                minDist = effectiveDist
                                closestZone = node.id
                            }
                        }

                        closestZone?.let { onSelectZone(it) }
                    }
                }
        ) {
            val w = size.width
            val h = size.height

            zoneNodes.forEach { node ->
                val isSelected = selectedZoneId == node.id
                val zoneColor = getColorForZone(node.id)
                val nodeCenter = Offset(node.relX * w, node.relY * h)

                // Leader callout lines pointing outward
                val targetX = if (node.leaderTargetX < 0) 8.dp.toPx() else w - 8.dp.toPx()
                val linePath = Path().apply {
                    moveTo(nodeCenter.x, nodeCenter.y)
                    lineTo(nodeCenter.x + node.leaderTargetX * 16.dp.toPx(), nodeCenter.y)
                    lineTo(targetX, nodeCenter.y)
                }

                val lineAlpha = if (isSelected) 0.95f else 0.40f
                drawPath(
                    path = linePath,
                    color = zoneColor.copy(alpha = lineAlpha),
                    style = Stroke(width = (if (isSelected) 2.0f else 1.2f).dp.toPx())
                )

                // Endpoint dot on leader line
                drawCircle(
                    color = zoneColor.copy(alpha = lineAlpha),
                    radius = 3.dp.toPx(),
                    center = Offset(targetX, nodeCenter.y)
                )

                // Glowing Halo Pulsing Effect
                val haloRadius = if (isSelected) 24.dp.toPx() * pulseScale else 16.dp.toPx() * pulseScale
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            zoneColor.copy(alpha = if (isSelected) 0.75f else 0.45f),
                            Color.Transparent
                        ),
                        center = nodeCenter,
                        radius = haloRadius
                    ),
                    radius = haloRadius,
                    center = nodeCenter
                )

                // Shiny White Node Center Dot
                drawCircle(
                    color = Color.White,
                    radius = if (isSelected) 7.dp.toPx() else 5.dp.toPx(),
                    center = nodeCenter
                )
                drawCircle(
                    color = zoneColor,
                    radius = if (isSelected) 7.dp.toPx() else 5.dp.toPx(),
                    center = nodeCenter,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}

// Backward compatibility alias for AnatomicalBodyScan
@Composable
fun AnatomicalBodyScan(
    selectedZoneId: String,
    discomforts: List<Discomfort>,
    isDarkMode: Boolean,
    onSelectZone: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    RealisticBodyScan(
        selectedZoneId = selectedZoneId,
        discomforts = discomforts,
        isDarkMode = isDarkMode,
        onSelectZone = onSelectZone,
        modifier = modifier
    )
}

@Preview(showBackground = true, name = "RealisticBodyScan Light Mode")
@Composable
fun RealisticBodyScanLightPreview() {
    EirNASASpaceAppsChallengeTheme(darkTheme = false) {
        RealisticBodyScan(
            selectedZoneId = "chest",
            discomforts = emptyList(),
            isDarkMode = false,
            onSelectZone = {}
        )
    }
}

@Preview(showBackground = true, name = "RealisticBodyScan Dark Mode", backgroundColor = 0xFF0B1120)
@Composable
fun RealisticBodyScanDarkPreview() {
    EirNASASpaceAppsChallengeTheme(darkTheme = true) {
        RealisticBodyScan(
            selectedZoneId = "chest",
            discomforts = emptyList(),
            isDarkMode = true,
            onSelectZone = {}
        )
    }
}
