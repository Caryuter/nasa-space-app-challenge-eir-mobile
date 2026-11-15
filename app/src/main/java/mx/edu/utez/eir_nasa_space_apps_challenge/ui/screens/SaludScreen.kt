package mx.edu.utez.eir_nasa_space_apps_challenge.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.edu.utez.eir_nasa_space_apps_challenge.data.model.BodyZoneInfo
import mx.edu.utez.eir_nasa_space_apps_challenge.data.model.HealthMetric
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.components.BodyZoneStatus
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.components.RealisticBodyScan
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.components.Sparkline
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.theme.*
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.viewmodel.SaludUiState
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.viewmodel.SaludViewModel

@Composable
fun SaludScreen(
    state: SaludUiState,
    viewModel: SaludViewModel,
    onOpenCheckup: () -> Unit,
    modifier: Modifier = Modifier
) {
    SaludScreenContent(
        state = state,
        onSelectZone = { viewModel.selectZone(it) },
        onOpenCheckup = onOpenCheckup,
        onDismissBanner = { viewModel.dismissBanner() },
        onToggleMetricExpand = { viewModel.toggleMetricExpanded(it) },
        onAddDiscomfort = { zoneId, text -> viewModel.addDiscomfort(zoneId, text) },
        onRemoveDiscomfort = { viewModel.removeDiscomfort(it) },
        onSetReportingZone = { viewModel.setReportingZone(it) },
        onSetCustomDiscomfortInput = { viewModel.setCustomDiscomfortInput(it) },
        getZoneStatus = { zoneId -> viewModel.getZoneStatus(zoneId) },
        bodyZonesInfo = viewModel.getBodyZonesInfo(),
        modifier = modifier
    )
}

@Composable
fun SaludScreenContent(
    state: SaludUiState,
    onSelectZone: (String) -> Unit,
    onOpenCheckup: () -> Unit,
    onDismissBanner: () -> Unit,
    onToggleMetricExpand: (String) -> Unit,
    onAddDiscomfort: (String, String) -> Unit,
    onRemoveDiscomfort: (Long) -> Unit,
    onSetReportingZone: (String?) -> Unit,
    onSetCustomDiscomfortInput: (String) -> Unit,
    getZoneStatus: (String) -> BodyZoneStatus,
    bodyZonesInfo: Map<String, BodyZoneInfo>,
    modifier: Modifier = Modifier
) {
    val isDark = false
    val cardBg = if (isDark) EirDarkSurface else EirLightSurface
    val currentZone = bodyZonesInfo[state.selectedZoneId] ?: bodyZonesInfo["head"] ?: BodyZoneInfo(
        id = "head",
        title = "Cabeza, Ojos y Descanso",
        summary = "7.5h de sueño • Rostro revisado",
        vsWeek = "Dormiste 20 min más que tu promedio de la semana",
        trend = listOf(7f, 7.5f),
        color = EirIndigo,
        whySpaceFeelsWeird = "¿Por qué sientes presión o cara hinchada? En la Tierra la gravedad jala los líquidos hacia tus pies; aquí suben hacia tu cabeza. Es normal y se alivia tomando agua despacio.",
        quickChips = listOf("Siento presión en la frente", "Ojos cansados", "Me siento un poco mareado")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // BODY SCAN SECTION (Elevated Card)
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = cardBg),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "¿Cómo se siente tu cuerpo?",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Toca una parte para entender qué pasa en el espacio",
                            fontSize = 11.sp,
                            color = if (isDark) EirDarkTextSecondary else EirLightTextSecondary
                        )
                    }
                }

                // Anatomical Map Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left Column (Head & Abdomen)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ZoneQuickButton(
                            label = "Cabeza",
                            sub = "${state.healthMetrics.find { it.id == "sleep" }?.value ?: "7.5"}h sueño",
                            isSelected = state.selectedZoneId == "head",
                            hasNote = state.discomforts.any { it.zoneId == "head" },
                            isDark = isDark,
                            onClick = { onSelectZone("head") }
                        )
                        ZoneQuickButton(
                            label = "Estómago",
                            sub = "Agua 4/6",
                            isSelected = state.selectedZoneId == "abdomen",
                            hasNote = state.discomforts.any { it.zoneId == "abdomen" },
                            isDark = isDark,
                            onClick = { onSelectZone("abdomen") }
                        )
                    }

                    // Center Silhouette Canvas
                    Box(
                        modifier = Modifier
                            .weight(1.3f)
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        RealisticBodyScan(
                            selectedZoneId = state.selectedZoneId,
                            discomforts = state.discomforts,
                            isDarkMode = isDark,
                            getZoneStatus = getZoneStatus,
                            onSelectZone = onSelectZone
                        )
                    }

                    // Right Column (Chest & Legs)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ZoneQuickButton(
                            label = "Pecho",
                            sub = "${state.healthMetrics.find { it.id == "heartRate" }?.value ?: "68"} lat/m",
                            isSelected = state.selectedZoneId == "chest",
                            hasNote = state.discomforts.any { it.zoneId == "chest" },
                            isDark = isDark,
                            onClick = { onSelectZone("chest") }
                        )
                        ZoneQuickButton(
                            label = "Piernas",
                            sub = "Bat. ${state.healthMetrics.find { it.id == "bodyBattery" }?.value ?: "84"}%",
                            isSelected = state.selectedZoneId == "legs",
                            hasNote = state.discomforts.any { it.zoneId == "legs" },
                            isDark = isDark,
                            onClick = { onSelectZone("legs") }
                        )
                    }
                }

                // Explanatory Panel in Human Language
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentZone.title,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = currentZone.summary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EirSky,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                                Text(
                                    text = currentZone.vsWeek,
                                    fontSize = 10.sp,
                                    color = if (isDark) EirDarkTextSecondary else EirLightTextSecondary,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Sparkline(
                                    data = currentZone.trend,
                                    color = currentZone.color,
                                    height = 20.dp,
                                    modifier = Modifier.width(70.dp)
                                )
                                Text(
                                    text = "Últimos 7 días",
                                    fontSize = 9.sp,
                                    color = if (isDark) EirDarkTextSecondary else EirLightTextSecondary,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }

                        // Space Explanation
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDark) Color(0xFF0B1120) else Color.White
                        ) {
                            Text(
                                text = currentZone.whySpaceFeelsWeird,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                color = if (isDark) EirDarkTextPrimary else EirLightTextPrimary,
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        // Reported Discomforts
                        state.discomforts
                            .filter { it.zoneId == state.selectedZoneId }
                            .forEach { disc ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(EirAmber.copy(alpha = 0.15f))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "• ${disc.label} (${disc.time})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = EirAmber
                                    )
                                    Text(
                                        text = "Ya pasó",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EirAmber,
                                        modifier = Modifier.clickable { onRemoveDiscomfort(disc.id) }
                                    )
                                }
                            }

                        // Report Discomfort Section
                        if (state.reportingZoneId == state.selectedZoneId) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Elige cómo se siente o escríbelo con tus palabras:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    currentZone.quickChips.take(2).forEach { chip ->
                                        FilterChip(
                                            selected = false,
                                            onClick = { onAddDiscomfort(state.selectedZoneId, chip) },
                                            label = { Text(text = "+ $chip", fontSize = 10.sp) }
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = state.customDiscomfortInput,
                                        onValueChange = { onSetCustomDiscomfortInput(it) },
                                        placeholder = { Text(text = "Otra molestia...", fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    Button(
                                        onClick = {
                                            onAddDiscomfort(
                                                state.selectedZoneId,
                                                state.customDiscomfortInput
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = EirAmber),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(text = "Anotar", fontSize = 11.sp, color = Color.Black)
                                    }
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = { onSetReportingZone(state.selectedZoneId) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Reportar",
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Reportar molestia aquí", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // 8 HEALTH METRICS LIST
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ESTADO DE TU CUERPO (8)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = if (isDark) EirDarkTextSecondary else EirLightTextSecondary
                )
                Text(
                    text = "Toca “?” para entender",
                    fontSize = 10.sp,
                    color = if (isDark) EirDarkTextSecondary else EirLightTextSecondary
                )
            }

            // Grid 2 Columns for Health Metrics
            val metrics = if (state.healthMetrics.isNotEmpty()) state.healthMetrics else sampleHealthMetrics
            for (i in metrics.indices step 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val m1 = metrics[i]
                    MetricCard(
                        metric = m1,
                        isExpanded = state.expandedMetricId == m1.id,
                        isDark = isDark,
                        cardBg = cardBg,
                        onToggleExpand = { onToggleMetricExpand(m1.id) },
                        modifier = Modifier.weight(1f)
                    )

                    if (i + 1 < metrics.size) {
                        val m2 = metrics[i + 1]
                        MetricCard(
                            metric = m2,
                            isExpanded = state.expandedMetricId == m2.id,
                            isDark = isDark,
                            cardBg = cardBg,
                            onToggleExpand = { onToggleMetricExpand(m2.id) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoneQuickButton(
    label: String,
    sub: String,
    isSelected: Boolean,
    hasNote: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isSelected) {
                EirSky.copy(alpha = 0.15f)
            } else if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) EirSky else if (hasNote) EirAmber else if (isDark) EirDarkBorder else EirLightBorder
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                if (hasNote) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(EirAmber)
                    )
                }
            }
            Text(
                text = sub,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = EirSky,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun MetricCard(
    metric: HealthMetric,
    isExpanded: Boolean,
    isDark: Boolean,
    cardBg: Color,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = cardBg),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = when (metric.id) {
                                "heartRate" -> Icons.Default.Favorite
                                "bodyBattery" -> Icons.Default.BatteryChargingFull
                                "oxygen" -> Icons.Default.Air
                                "bloodPressure" -> Icons.Default.MonitorHeart
                                "sleep" -> Icons.Default.Bedtime
                                "temp" -> Icons.Default.Thermostat
                                "energySugar" -> Icons.Default.LocalFireDepartment
                                else -> Icons.Default.WaterDrop
                            },
                            contentDescription = metric.title,
                            tint = metric.color,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = metric.title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) EirDarkTextPrimary else EirLightTextPrimary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isExpanded) EirSky
                                else if (isDark) Color(0xFF1E293B)
                                else Color(0xFFE0F2FE)
                            )
                            .clickable { onToggleExpand() }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isExpanded) "Cerrar" else "?",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isExpanded) Color.White else EirSky
                        )
                    }
                }

                // Numeric Value
                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = metric.value,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        fontFamily = FontFamily.Default
                    )
                    if (metric.unit.isNotBlank()) {
                        Text(
                            text = metric.unit,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) EirDarkTextSecondary else EirLightTextSecondary,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }

                // Human Label
                Text(
                    text = metric.humanLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = EirEmerald,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            if (!isExpanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Sparkline(data = metric.history, color = metric.color, height = 18.dp)
                    Text(
                        text = metric.updatedAt,
                        fontSize = 9.sp,
                        color = if (isDark) EirDarkTextSecondary else EirLightTextSecondary,
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 4.dp)
                    )
                }
            }

            // Expanded Accordion
            if (isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .border(
                            width = 0.5.dp,
                            color = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                            shape = RoundedCornerShape(0.dp)
                        )
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "¿Qué significa esto?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = EirSky
                    )
                    Text(
                        text = metric.meaning,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = if (isDark) EirDarkTextPrimary else EirLightTextPrimary
                    )

                    Text(
                        text = "¿Qué hago?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = EirEmerald,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = metric.action,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = if (isDark) EirDarkTextPrimary else EirLightTextPrimary
                    )

                    Text(
                        text = "Actualizado: ${metric.updatedAt}",
                        fontSize = 10.sp,
                        color = if (isDark) EirDarkTextSecondary else EirLightTextSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

val sampleHealthMetrics = listOf(
    HealthMetric("heartRate", "Ritmo del Corazón", "68", "lat/min", "Latidos tranquilos", "Hace 15 min", listOf(71f, 69f, 72f, 70f, 68f, 69f, 68f), "Tu corazón late a un ritmo relajado.", "Sigue con tu rutina normal.", EirSky),
    HealthMetric("bodyBattery", "Batería de tu Cuerpo", "84", "%", "Batería física alta", "Hoy 07:30", listOf(76f, 79f, 78f, 81f, 85f, 82f, 84f), "Mide la recuperación del cuerpo.", "Buen día para hacer ejercicio.", EirIndigo),
    HealthMetric("oxygen", "Aire en tu Sangre", "98", "%", "Aire en sangre óptimo", "Hace 15 min", listOf(98f, 99f, 98f, 97f, 98f, 98f, 98f), "Tus pulmones llevan oxígeno de maravilla.", "Cabina bien ventilada.", EirCyan),
    HealthMetric("bloodPressure", "Fuerza de Circulación", "118/76", "fuerza", "Circulación estable", "Hace 2 h", listOf(120f, 119f, 122f, 118f, 117f, 119f, 118f), "La presión de tu sangre está equilibrada.", "Evita levantarte de golpe.", EirBlue),
    HealthMetric("sleep", "Sueño y Descanso", "7.5", "hrs", "Descanso reparador", "Hoy 07:30", listOf(6.8f, 7.0f, 7.4f, 6.5f, 7.8f, 7.2f, 7.5f), "Dormiste lo suficiente para reparar tus músculos.", "Mantén la misma hora para dormir.", EirPurple),
    HealthMetric("temp", "Calor Corporal", "36.7", "°C", "Temperatura normal", "Hace 2 h", listOf(36.6f, 36.7f, 36.6f, 36.8f, 36.6f, 36.7f, 36.7f), "No tienes fiebre.", "Todo en orden.", EirAmber),
    HealthMetric("energySugar", "Combustible Diario", "92", "nivel", "Combustible estable", "Hace 1 h", listOf(95f, 92f, 96f, 91f, 93f, 94f, 92f), "Energía en sangre pareja.", "Come a tu hora programada.", EirEmerald),
    HealthMetric("hydration", "Agua en tu Cuerpo", "4/6", "vasos", "Faltan 2 vasos", "Hace 25 min", listOf(5f, 6f, 4f, 6f, 5f, 6f, 4f), "El cuerpo quita la sed en el espacio.", "Toma 1 vaso ahora.", EirTeal)
)

@Preview(showBackground = true, name = "SaludScreen Full Light Preview", heightDp = 1100)
@Composable
fun SaludScreenLightPreview() {
    EirNASASpaceAppsChallengeTheme(darkTheme = false) {
        Surface(color = EirLightBg) {
            SaludScreenContent(
                state = SaludUiState(healthMetrics = sampleHealthMetrics),
                onSelectZone = {},
                onOpenCheckup = {},
                onDismissBanner = {},
                onToggleMetricExpand = {},
                onAddDiscomfort = { _, _ -> },
                onRemoveDiscomfort = {},
                onSetReportingZone = {},
                onSetCustomDiscomfortInput = {},
                getZoneStatus = { BodyZoneStatus.OPTIMAL },
                bodyZonesInfo = emptyMap()
            )
        }
    }
}

@Preview(showBackground = true, name = "SaludScreen Full Dark Preview", backgroundColor = 0xFF0B1120, heightDp = 1100)
@Composable
fun SaludScreenDarkPreview() {
    EirNASASpaceAppsChallengeTheme(darkTheme = true) {
        Surface(color = EirDarkBg) {
            SaludScreenContent(
                state = SaludUiState(healthMetrics = sampleHealthMetrics),
                onSelectZone = {},
                onOpenCheckup = {},
                onDismissBanner = {},
                onToggleMetricExpand = {},
                onAddDiscomfort = { _, _ -> },
                onRemoveDiscomfort = {},
                onSetReportingZone = {},
                onSetCustomDiscomfortInput = {},
                getZoneStatus = { BodyZoneStatus.OPTIMAL },
                bodyZonesInfo = emptyMap()
            )
        }
    }
}
