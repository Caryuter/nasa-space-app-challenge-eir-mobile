package mx.edu.utez.eir_nasa_space_apps_challenge.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.edu.utez.eir_nasa_space_apps_challenge.R
import mx.edu.utez.eir_nasa_space_apps_challenge.data.model.BodyZoneInfo
import mx.edu.utez.eir_nasa_space_apps_challenge.data.model.Discomfort
import mx.edu.utez.eir_nasa_space_apps_challenge.data.model.HealthMetric
import mx.edu.utez.eir_nasa_space_apps_challenge.data.model.VitalsDraft
import mx.edu.utez.eir_nasa_space_apps_challenge.data.repository.EirHealthRepository
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.components.BodyZoneStatus
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.theme.*
import java.util.Locale

data class SaludUiState(
    val healthMetrics: List<HealthMetric> = emptyList(),
    val expandedMetricId: String? = null,
    val selectedZoneId: String = "head",
    val discomforts: List<Discomfort> = emptyList(),
    val reportingZoneId: String? = null,
    val customDiscomfortInput: String = "",
    val bannerVisible: Boolean = true,
    val vitalsDraft: VitalsDraft = VitalsDraft()
)

class SaludViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = EirHealthRepository.getInstance(application)

    private val initialMetrics by lazy {
        listOf(
            HealthMetric(
                id = "heartRate",
                title = application.getString(R.string.metric_heart_rate_title),
                value = "68",
                unit = application.getString(R.string.metric_heart_rate_unit),
                humanLabel = application.getString(R.string.metric_heart_rate_label),
                updatedAt = application.getString(R.string.time_15m_ago),
                history = listOf(71f, 69f, 72f, 70f, 68f, 69f, 68f),
                meaning = application.getString(R.string.metric_heart_rate_meaning),
                action = application.getString(R.string.metric_heart_rate_action),
                color = EirSky
            ),
            HealthMetric(
                id = "bodyBattery",
                title = application.getString(R.string.metric_body_battery_title),
                value = "84",
                unit = "%",
                humanLabel = application.getString(R.string.metric_body_battery_label),
                updatedAt = application.getString(R.string.time_today_0730),
                history = listOf(76f, 79f, 78f, 81f, 85f, 82f, 84f),
                meaning = application.getString(R.string.metric_body_battery_meaning),
                action = application.getString(R.string.metric_body_battery_action),
                color = EirIndigo
            ),
            HealthMetric(
                id = "oxygen",
                title = application.getString(R.string.metric_oxygen_title),
                value = "98",
                unit = "%",
                humanLabel = application.getString(R.string.metric_oxygen_label),
                updatedAt = application.getString(R.string.time_15m_ago),
                history = listOf(98f, 99f, 98f, 97f, 98f, 98f, 98f),
                meaning = application.getString(R.string.metric_oxygen_meaning),
                action = application.getString(R.string.metric_oxygen_action),
                color = EirCyan
            ),
            HealthMetric(
                id = "bloodPressure",
                title = application.getString(R.string.metric_bp_title),
                value = "118/76",
                unit = application.getString(R.string.metric_bp_unit),
                humanLabel = application.getString(R.string.metric_bp_label),
                updatedAt = application.getString(R.string.time_2h_ago),
                history = listOf(120f, 119f, 122f, 118f, 117f, 119f, 118f),
                meaning = application.getString(R.string.metric_bp_meaning),
                action = application.getString(R.string.metric_bp_action),
                color = EirBlue
            ),
            HealthMetric(
                id = "sleep",
                title = application.getString(R.string.metric_sleep_title),
                value = "7.5",
                unit = application.getString(R.string.metric_sleep_unit),
                humanLabel = application.getString(R.string.metric_sleep_label),
                updatedAt = application.getString(R.string.time_today_0730),
                history = listOf(6.8f, 7.0f, 7.4f, 6.5f, 7.8f, 7.2f, 7.5f),
                meaning = application.getString(R.string.metric_sleep_meaning),
                action = application.getString(R.string.metric_sleep_action),
                color = EirPurple
            ),
            HealthMetric(
                id = "temp",
                title = application.getString(R.string.metric_temp_title),
                value = "36.7",
                unit = "°C",
                humanLabel = application.getString(R.string.metric_temp_label),
                updatedAt = application.getString(R.string.time_2h_ago),
                history = listOf(36.6f, 36.7f, 36.6f, 36.8f, 36.6f, 36.7f, 36.7f),
                meaning = application.getString(R.string.metric_temp_meaning),
                action = application.getString(R.string.metric_temp_action),
                color = EirAmber
            ),
            HealthMetric(
                id = "energySugar",
                title = application.getString(R.string.metric_sugar_title),
                value = "92",
                unit = application.getString(R.string.metric_sugar_unit),
                humanLabel = application.getString(R.string.metric_sugar_label),
                updatedAt = application.getString(R.string.time_1h_ago),
                history = listOf(95f, 92f, 96f, 91f, 93f, 94f, 92f),
                meaning = application.getString(R.string.metric_sugar_meaning),
                action = application.getString(R.string.metric_sugar_action),
                color = EirEmerald
            ),
            HealthMetric(
                id = "hydration",
                title = application.getString(R.string.metric_hydration_title),
                value = "4/6",
                unit = application.getString(R.string.metric_hydration_unit),
                humanLabel = application.getString(R.string.metric_hydration_label),
                updatedAt = application.getString(R.string.time_25m_ago),
                history = listOf(5f, 6f, 4f, 6f, 5f, 6f, 4f),
                meaning = application.getString(R.string.metric_hydration_meaning),
                action = application.getString(R.string.metric_hydration_action),
                color = EirTeal
            )
        )
    }

    private val _uiState by lazy {
        MutableStateFlow(
            SaludUiState(
                healthMetrics = initialMetrics,
                discomforts = listOf(
                    Discomfort(
                        1L,
                        "legs",
                        application.getString(R.string.discomfort_mild_legs),
                        application.getString(R.string.time_today_0910)
                    )
                )
            )
        )
    }
    val uiState: StateFlow<SaludUiState> by lazy { _uiState.asStateFlow() }

    init {
        viewModelScope.launch {
            repository.ensureInitializedAndBootstrapped()
            repository.latestHealthRecordFlow.collect { record ->
                if (record != null) {
                    _uiState.update { current ->
                        val updatedMetrics = current.healthMetrics.map { m ->
                            when (m.id) {
                                "heartRate" -> record.heartRateBpm?.let {
                                    m.copy(
                                        value = "$it",
                                        humanLabel = if (it > 95) application.getString(R.string.metric_heart_rate_label_fast) else application.getString(R.string.metric_heart_rate_label),
                                        updatedAt = application.getString(R.string.time_just_now)
                                    )
                                } ?: m
                                "bloodPressure" -> record.systolicBpMmhg?.let {
                                    m.copy(
                                        value = "$it/76",
                                        updatedAt = application.getString(R.string.time_just_now)
                                    )
                                } ?: m
                                "temp" -> record.temperatureC?.let {
                                    m.copy(
                                        value = String.format(Locale.US, "%.1f", it),
                                        humanLabel = if (it >= 37.5) application.getString(R.string.metric_temp_label_warm) else application.getString(R.string.metric_temp_label),
                                        updatedAt = application.getString(R.string.time_just_now)
                                    )
                                } ?: m
                                "sleep" -> record.sleepHours?.let {
                                    m.copy(
                                        value = String.format(Locale.US, "%.1f", it),
                                        humanLabel = if (it >= 7.0) application.getString(R.string.metric_sleep_label) else application.getString(R.string.metric_sleep_label_short),
                                        updatedAt = application.getString(R.string.time_just_now)
                                    )
                                } ?: m
                                else -> m
                            }
                        }
                        current.copy(
                            healthMetrics = updatedMetrics,
                            vitalsDraft = VitalsDraft(
                                heartRate = record.heartRateBpm ?: 68,
                                sys = record.systolicBpMmhg ?: 118,
                                temp = record.temperatureC?.toFloat() ?: 36.7f,
                                sleepHours = record.sleepHours?.toFloat() ?: 7.5f
                            )
                        )
                    }
                }
            }
        }
    }

    fun toggleMetricExpanded(metricId: String) {
        _uiState.update {
            it.copy(expandedMetricId = if (it.expandedMetricId == metricId) null else metricId)
        }
    }

    fun selectZone(zoneId: String) {
        _uiState.update { it.copy(selectedZoneId = zoneId, reportingZoneId = null) }
    }

    fun setReportingZone(zoneId: String?) {
        _uiState.update { it.copy(reportingZoneId = zoneId, customDiscomfortInput = "") }
    }

    fun setCustomDiscomfortInput(text: String) {
        _uiState.update { it.copy(customDiscomfortInput = text) }
    }

    fun addDiscomfort(zoneId: String, text: String) {
        if (text.isBlank()) return
        val newDiscomfort = Discomfort(
            id = System.currentTimeMillis(),
            zoneId = zoneId,
            label = text.trim(),
            time = getApplication<Application>().getString(R.string.time_just_now)
        )
        _uiState.update {
            it.copy(
                discomforts = listOf(newDiscomfort) + it.discomforts,
                reportingZoneId = null,
                customDiscomfortInput = "",
                bannerVisible = true
            )
        }
    }

    fun removeDiscomfort(id: Long) {
        _uiState.update {
            it.copy(discomforts = it.discomforts.filter { item -> item.id != id })
        }
    }

    fun dismissBanner() {
        _uiState.update { it.copy(bannerVisible = false) }
    }

    fun getZoneStatus(zoneId: String): BodyZoneStatus {
        val state = _uiState.value
        val zoneDiscomforts = state.discomforts.filter { it.zoneId == zoneId }
        val heartRate = state.vitalsDraft.heartRate
        val temp = state.vitalsDraft.temp

        val hasCriticalDiscomfort = zoneDiscomforts.any {
            it.label.contains("fuerte", ignoreCase = true) ||
            it.label.contains("apretado", ignoreCase = true) ||
            it.label.contains("falta de aire", ignoreCase = true) ||
            it.label.contains("mareo", ignoreCase = true) ||
            it.label.contains("severe", ignoreCase = true) ||
            it.label.contains("tight", ignoreCase = true) ||
            it.label.contains("breath", ignoreCase = true) ||
            it.label.contains("dizzy", ignoreCase = true)
        }

        if (hasCriticalDiscomfort || (zoneId == "chest" && heartRate > 100) || (zoneId == "head" && temp >= 38.0f)) {
            return BodyZoneStatus.CRITICAL
        }

        if (zoneDiscomforts.isNotEmpty() || (zoneId == "head" && state.vitalsDraft.sleepHours < 6.0f)) {
            return BodyZoneStatus.MODERATE
        }

        return BodyZoneStatus.OPTIMAL
    }

    fun getBodyZonesInfo(): Map<String, BodyZoneInfo> {
        val app = getApplication<Application>()
        val state = _uiState.value
        val sleepMetric = state.healthMetrics.find { it.id == "sleep" }
        val heartMetric = state.healthMetrics.find { it.id == "heartRate" }
        val oxygenMetric = state.healthMetrics.find { it.id == "oxygen" }
        val batteryMetric = state.healthMetrics.find { it.id == "bodyBattery" }
        val sugarMetric = state.healthMetrics.find { it.id == "energySugar" }

        val sleepVal = sleepMetric?.value ?: "7.5"
        val heartVal = heartMetric?.value ?: "68"
        val oxygenVal = oxygenMetric?.value ?: "98"
        val batteryVal = batteryMetric?.value ?: "84"

        return mapOf(
            "head" to BodyZoneInfo(
                id = "head",
                title = app.getString(R.string.zone_head_title),
                summary = app.getString(R.string.zone_head_summary, sleepVal),
                vsWeek = app.getString(R.string.zone_head_vs_week),
                trend = sleepMetric?.history ?: listOf(7f, 7.5f),
                color = EirIndigo,
                whySpaceFeelsWeird = app.getString(R.string.zone_head_why_space),
                quickChips = listOf(
                    app.getString(R.string.zone_head_chip_1),
                    app.getString(R.string.zone_head_chip_2),
                    app.getString(R.string.zone_head_chip_3)
                )
            ),
            "chest" to BodyZoneInfo(
                id = "chest",
                title = app.getString(R.string.zone_chest_title),
                summary = app.getString(R.string.zone_chest_summary, heartVal, oxygenVal),
                vsWeek = app.getString(R.string.zone_chest_vs_week),
                trend = heartMetric?.history ?: listOf(68f, 70f),
                color = EirSky,
                whySpaceFeelsWeird = app.getString(R.string.zone_chest_why_space),
                quickChips = listOf(
                    app.getString(R.string.zone_chest_chip_1),
                    app.getString(R.string.zone_chest_chip_2),
                    app.getString(R.string.zone_chest_chip_3)
                )
            ),
            "abdomen" to BodyZoneInfo(
                id = "abdomen",
                title = app.getString(R.string.zone_abdomen_title),
                summary = app.getString(R.string.zone_abdomen_summary),
                vsWeek = app.getString(R.string.zone_abdomen_vs_week),
                trend = sugarMetric?.history ?: listOf(92f, 94f),
                color = EirTeal,
                whySpaceFeelsWeird = app.getString(R.string.zone_abdomen_why_space),
                quickChips = listOf(
                    app.getString(R.string.zone_abdomen_chip_1),
                    app.getString(R.string.zone_abdomen_chip_2),
                    app.getString(R.string.zone_abdomen_chip_3)
                )
            ),
            "arms" to BodyZoneInfo(
                id = "arms",
                title = app.getString(R.string.zone_arms_title),
                summary = app.getString(R.string.zone_arms_summary, batteryVal),
                vsWeek = app.getString(R.string.zone_arms_vs_week),
                trend = batteryMetric?.history ?: listOf(80f, 84f),
                color = EirCyan,
                whySpaceFeelsWeird = app.getString(R.string.zone_arms_why_space),
                quickChips = listOf(
                    app.getString(R.string.zone_arms_chip_1),
                    app.getString(R.string.zone_arms_chip_2),
                    app.getString(R.string.zone_arms_chip_3)
                )
            ),
            "legs" to BodyZoneInfo(
                id = "legs",
                title = app.getString(R.string.zone_legs_title),
                summary = app.getString(R.string.zone_legs_summary, batteryVal),
                vsWeek = app.getString(R.string.zone_legs_vs_week),
                trend = batteryMetric?.history ?: listOf(80f, 84f),
                color = EirAmber,
                whySpaceFeelsWeird = app.getString(R.string.zone_legs_why_space),
                quickChips = listOf(
                    app.getString(R.string.zone_legs_chip_1),
                    app.getString(R.string.zone_legs_chip_2),
                    app.getString(R.string.zone_legs_chip_3)
                )
            )
        )
    }
}
