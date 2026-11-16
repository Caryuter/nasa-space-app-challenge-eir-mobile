package mx.edu.utez.eir_nasa_space_apps_challenge.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.edu.utez.eir_nasa_space_apps_challenge.R
import mx.edu.utez.eir_nasa_space_apps_challenge.data.model.BodyZoneInfo
import mx.edu.utez.eir_nasa_space_apps_challenge.data.model.HealthMetric
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.components.BodyZoneStatus
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.components.MascotAvatar
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.components.MascotEmotion
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
    val isDark = MaterialTheme.colorScheme.isDark
    val cardBg = MaterialTheme.colorScheme.surface
    val currentZone = bodyZonesInfo[state.selectedZoneId] ?: bodyZonesInfo["head"] ?: BodyZoneInfo(
        id = "head",
        title = stringResource(R.string.zone_head_title),
        summary = stringResource(R.string.zone_head_summary, "7.5"),
        vsWeek = stringResource(R.string.zone_head_vs_week),
        trend = listOf(7f, 7.5f),
        color = EirIndigo,
        whySpaceFeelsWeird = stringResource(R.string.zone_head_why_space),
        quickChips = listOf(
            stringResource(R.string.zone_head_chip_1),
            stringResource(R.string.zone_head_chip_2),
            stringResource(R.string.zone_head_chip_3)
        )
    )

    val bodyBatteryVal = state.healthMetrics.find { it.id == "bodyBattery" }?.value?.toIntOrNull() ?: 84
    val mascotReadinessEmotion = when {
        bodyBatteryVal >= 80 -> MascotEmotion.HAPPY
        bodyBatteryVal >= 60 -> MascotEmotion.IDLE
        else -> MascotEmotion.CONCERNED
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // MASCOT READINESS CARD
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF0F9FF)
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MascotAvatar(
                    emotion = mascotReadinessEmotion,
                    size = 48.dp,
                    showGlow = true
                )

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.readiness_state, bodyBatteryVal),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp,
                            color = EirSky
                        )
                    }
                    Text(
                        text = when (mascotReadinessEmotion) {
                            MascotEmotion.HAPPY -> stringResource(R.string.readiness_optimal)
                            MascotEmotion.IDLE -> stringResource(R.string.readiness_stable)
                            else -> stringResource(R.string.readiness_low)
                        },
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

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
                            text = stringResource(R.string.body_feeling_question),
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                        Text(
                            text = stringResource(R.string.body_feeling_sub),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            label = stringResource(R.string.zone_head),
                            sub = stringResource(R.string.hours_sleep_sub, state.healthMetrics.find { it.id == "sleep" }?.value ?: "7.5"),
                            isSelected = state.selectedZoneId == "head",
                            hasNote = state.discomforts.any { it.zoneId == "head" },
                            isDark = isDark,
                            onClick = { onSelectZone("head") }
                        )
                        ZoneQuickButton(
                            label = stringResource(R.string.zone_abdomen),
                            sub = stringResource(R.string.water_sub, "4/6"),
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
                            label = stringResource(R.string.zone_chest),
                            sub = stringResource(R.string.bpm_sub, state.healthMetrics.find { it.id == "heartRate" }?.value ?: "68"),
                            isSelected = state.selectedZoneId == "chest",
                            hasNote = state.discomforts.any { it.zoneId == "chest" },
                            isDark = isDark,
                            onClick = { onSelectZone("chest") }
                        )
                        ZoneQuickButton(
                            label = stringResource(R.string.zone_legs),
                            sub = stringResource(R.string.battery_sub, state.healthMetrics.find { it.id == "bodyBattery" }?.value ?: "84"),
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
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                    text = stringResource(R.string.last_7_days),
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }

                        // Space Explanation
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                text = currentZone.whySpaceFeelsWeird,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface,
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
                                        text = stringResource(R.string.past_discomfort),
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
                                    text = stringResource(R.string.report_discomfort_prompt),
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
                                        placeholder = { Text(text = stringResource(R.string.other_discomfort_placeholder), fontSize = 11.sp) },
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
                                        Text(text = stringResource(R.string.record_discomfort), fontSize = 11.sp, color = Color.Black)
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
                                    contentDescription = stringResource(R.string.record_discomfort),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = stringResource(R.string.report_discomfort_here), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // 8 HEALTH METRICS LIST
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val metrics = if (state.healthMetrics.isNotEmpty()) state.healthMetrics else sampleHealthMetrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.body_status_header, metrics.size),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.swipe_for_more),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Horizontal Carousel for Health Metrics (1 card per row/view)
            val lazyListState = rememberLazyListState()
            val snapFlingBehavior = rememberSnapFlingBehavior(
                lazyListState = lazyListState,
                snapPosition = SnapPosition.Center
            )
            val cardFraction = 0.88f

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val horizontalPadding = maxWidth * ((1f - cardFraction) / 2f)
                LazyRow(
                    state = lazyListState,
                    flingBehavior = snapFlingBehavior,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = horizontalPadding),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(metrics, key = { it.id }) { metric ->
                        MetricCard(
                            metric = metric,
                            isExpanded = state.expandedMetricId == metric.id,
                            isDark = isDark,
                            cardBg = cardBg,
                            onToggleExpand = { onToggleMetricExpand(metric.id) },
                            modifier = Modifier.fillParentMaxWidth(cardFraction)
                        )
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
            } else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) EirSky else if (hasNote) EirAmber else MaterialTheme.colorScheme.outline
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
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isExpanded) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { onToggleExpand() }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isExpanded) stringResource(R.string.close) else "?",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isExpanded) Color.White else MaterialTheme.colorScheme.primary
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(0.dp)
                        )
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.metric_what_means),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = EirSky
                    )
                    Text(
                        text = metric.meaning,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = stringResource(R.string.metric_what_do),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = EirEmerald,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = metric.action,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = stringResource(R.string.metric_updated, metric.updatedAt),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

val sampleHealthMetrics: List<HealthMetric>
    @Composable
    get() = listOf(
        HealthMetric("heartRate", stringResource(R.string.metric_heart_rate_title), "68", stringResource(R.string.metric_heart_rate_unit), stringResource(R.string.metric_heart_rate_label), stringResource(R.string.time_15m_ago), listOf(71f, 69f, 72f, 70f, 68f, 69f, 68f), stringResource(R.string.metric_heart_rate_meaning), stringResource(R.string.metric_heart_rate_action), EirSky),
        HealthMetric("bodyBattery", stringResource(R.string.metric_body_battery_title), "84", "%", stringResource(R.string.metric_body_battery_label), stringResource(R.string.time_today_0730), listOf(76f, 79f, 78f, 81f, 85f, 82f, 84f), stringResource(R.string.metric_body_battery_meaning), stringResource(R.string.metric_body_battery_action), EirIndigo),
        HealthMetric("oxygen", stringResource(R.string.metric_oxygen_title), "98", "%", stringResource(R.string.metric_oxygen_label), stringResource(R.string.time_15m_ago), listOf(98f, 99f, 98f, 97f, 98f, 98f, 98f), stringResource(R.string.metric_oxygen_meaning), stringResource(R.string.metric_oxygen_action), EirCyan),
        HealthMetric("bloodPressure", stringResource(R.string.metric_bp_title), "118/76", stringResource(R.string.metric_bp_unit), stringResource(R.string.metric_bp_label), stringResource(R.string.time_2h_ago), listOf(120f, 119f, 122f, 118f, 117f, 119f, 118f), stringResource(R.string.metric_bp_meaning), stringResource(R.string.metric_bp_action), EirBlue),
        HealthMetric("sleep", stringResource(R.string.metric_sleep_title), "7.5", stringResource(R.string.metric_sleep_unit), stringResource(R.string.metric_sleep_label), stringResource(R.string.time_today_0730), listOf(6.8f, 7.0f, 7.4f, 6.5f, 7.8f, 7.2f, 7.5f), stringResource(R.string.metric_sleep_meaning), stringResource(R.string.metric_sleep_action), EirPurple),
        HealthMetric("temp", stringResource(R.string.metric_temp_title), "36.7", "°C", stringResource(R.string.metric_temp_label), stringResource(R.string.time_2h_ago), listOf(36.6f, 36.7f, 36.6f, 36.8f, 36.6f, 36.7f, 36.7f), stringResource(R.string.metric_temp_meaning), stringResource(R.string.metric_temp_action), EirAmber),
        HealthMetric("energySugar", stringResource(R.string.metric_sugar_title), "92", stringResource(R.string.metric_sugar_unit), stringResource(R.string.metric_sugar_label), stringResource(R.string.time_1h_ago), listOf(95f, 92f, 96f, 91f, 93f, 94f, 92f), stringResource(R.string.metric_sugar_meaning), stringResource(R.string.metric_sugar_action), EirEmerald),
        HealthMetric("hydration", stringResource(R.string.metric_hydration_title), "4/6", stringResource(R.string.metric_hydration_unit), stringResource(R.string.metric_hydration_label), stringResource(R.string.time_25m_ago), listOf(5f, 6f, 4f, 6f, 5f, 6f, 4f), stringResource(R.string.metric_hydration_meaning), stringResource(R.string.metric_hydration_action), EirTeal)
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
