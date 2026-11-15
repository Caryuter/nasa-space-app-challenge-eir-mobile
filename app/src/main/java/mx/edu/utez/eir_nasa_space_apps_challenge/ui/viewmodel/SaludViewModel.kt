package mx.edu.utez.eir_nasa_space_apps_challenge.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    val discomforts: List<Discomfort> = listOf(
        Discomfort(1L, "legs", "Cansancio muscular leve en piernas", "Hoy 09:10")
    ),
    val reportingZoneId: String? = null,
    val customDiscomfortInput: String = "",
    val bannerVisible: Boolean = true,
    val vitalsDraft: VitalsDraft = VitalsDraft()
)

class SaludViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = EirHealthRepository.getInstance(application)

    private val initialMetrics = listOf(
        HealthMetric(
            id = "heartRate",
            title = "Ritmo del Corazón",
            value = "68",
            unit = "lat/min",
            humanLabel = "Latidos tranquilos",
            updatedAt = "Hace 15 min",
            history = listOf(71f, 69f, 72f, 70f, 68f, 69f, 68f),
            meaning = "Tu corazón late a un ritmo relajado, igual que cuando estás descansando en la Tierra.",
            action = "No necesitas hacer nada especial; sigue con tu rutina normal.",
            color = EirSky
        ),
        HealthMetric(
            id = "bodyBattery",
            title = "Batería de tu Cuerpo",
            value = "84",
            unit = "%",
            humanLabel = "Batería física alta",
            updatedAt = "Hoy 07:30",
            history = listOf(76f, 79f, 78f, 81f, 85f, 82f, 84f),
            meaning = "Mide qué tan bien se recuperó tu cuerpo mientras dormías. Hoy tienes buena reserva de energía.",
            action = "Es un buen día para completar tu rutina de fuerza sin exigirte de más.",
            color = EirIndigo
        ),
        HealthMetric(
            id = "oxygen",
            title = "Aire en tu Sangre",
            value = "98",
            unit = "%",
            humanLabel = "Aire en sangre óptimo",
            updatedAt = "Hace 15 min",
            history = listOf(98f, 99f, 98f, 97f, 98f, 98f, 98f),
            meaning = "Tus pulmones están llevando oxígeno de maravilla a todo tu cuerpo.",
            action = "El aire de la cabina está limpio y bien ventilado.",
            color = EirCyan
        ),
        HealthMetric(
            id = "bloodPressure",
            title = "Fuerza de Circulación",
            value = "118/76",
            unit = "fuerza",
            humanLabel = "Circulación estable",
            updatedAt = "Hace 2 h",
            history = listOf(120f, 119f, 122f, 118f, 117f, 119f, 118f),
            meaning = "La fuerza con la que viaja tu sangre está equilibrada, a pesar de no tener gravedad.",
            action = "Evita levantarte o girar de golpe para no sentir mareo leve.",
            color = EirBlue
        ),
        HealthMetric(
            id = "sleep",
            title = "Sueño y Descanso",
            value = "7.5",
            unit = "hrs",
            humanLabel = "Descanso reparador",
            updatedAt = "Hoy 07:30",
            history = listOf(6.8f, 7.0f, 7.4f, 6.5f, 7.8f, 7.2f, 7.5f),
            meaning = "Dormiste el tiempo suficiente para que tu mente y músculos se reparen.",
            action = "Mantén la misma hora para acostarte hoy.",
            color = EirPurple
        ),
        HealthMetric(
            id = "temp",
            title = "Calor Corporal",
            value = "36.7",
            unit = "°C",
            humanLabel = "Temperatura normal",
            updatedAt = "Hace 2 h",
            history = listOf(36.6f, 36.7f, 36.6f, 36.8f, 36.6f, 36.7f, 36.7f),
            meaning = "No tienes fiebre ni señales de infección.",
            action = "Todo en orden.",
            color = EirAmber
        ),
        HealthMetric(
            id = "energySugar",
            title = "Combustible Diario",
            value = "92",
            unit = "nivel",
            humanLabel = "Combustible estable",
            updatedAt = "Hace 1 h",
            history = listOf(95f, 92f, 96f, 91f, 93f, 94f, 92f),
            meaning = "Tu nivel de energía en sangre está parejo; no tienes bajones de azúcar.",
            action = "Come tu siguiente ración a la hora programada.",
            color = EirEmerald
        ),
        HealthMetric(
            id = "hydration",
            title = "Agua en tu Cuerpo",
            value = "4/6",
            unit = "vasos",
            humanLabel = "Faltan 2 vasos",
            updatedAt = "Hace 25 min",
            history = listOf(5f, 6f, 4f, 6f, 5f, 6f, 4f),
            meaning = "En el espacio el cuerpo engaña al cerebro y quita la sed aunque necesites agua.",
            action = "Toma 1 vaso ahora y otro antes de tu ejercicio.",
            color = EirTeal
        )
    )

    private val _uiState = MutableStateFlow(SaludUiState(healthMetrics = initialMetrics))
    val uiState: StateFlow<SaludUiState> = _uiState.asStateFlow()

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
                                        humanLabel = if (it > 95) "Latidos algo rápidos" else "Latidos tranquilos",
                                        updatedAt = "Hace un momento"
                                    )
                                } ?: m
                                "bloodPressure" -> record.systolicBpMmhg?.let {
                                    m.copy(
                                        value = "$it/76",
                                        updatedAt = "Hace un momento"
                                    )
                                } ?: m
                                "temp" -> record.temperatureC?.let {
                                    m.copy(
                                        value = String.format(Locale.US, "%.1f", it),
                                        humanLabel = if (it >= 37.5) "Un poco caliente" else "Temperatura normal",
                                        updatedAt = "Hace un momento"
                                    )
                                } ?: m
                                "sleep" -> record.sleepHours?.let {
                                    m.copy(
                                        value = String.format(Locale.US, "%.1f", it),
                                        humanLabel = if (it >= 7.0) "Descanso reparador" else "Dormiste poco hoy",
                                        updatedAt = "Hace un momento"
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
            time = "Hace un momento"
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
            it.label.contains("mareo", ignoreCase = true)
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
        val state = _uiState.value
        val sleepMetric = state.healthMetrics.find { it.id == "sleep" }
        val heartMetric = state.healthMetrics.find { it.id == "heartRate" }
        val oxygenMetric = state.healthMetrics.find { it.id == "oxygen" }
        val batteryMetric = state.healthMetrics.find { it.id == "bodyBattery" }
        val sugarMetric = state.healthMetrics.find { it.id == "energySugar" }

        return mapOf(
            "head" to BodyZoneInfo(
                id = "head",
                title = "Cabeza, Ojos y Descanso",
                summary = "${sleepMetric?.value ?: "7.5"}h de sueño • Rostro revisado",
                vsWeek = "Dormiste 20 min más que tu promedio de la semana",
                trend = sleepMetric?.history ?: listOf(7f, 7.5f),
                color = EirIndigo,
                whySpaceFeelsWeird = "¿Por qué sientes presión o cara hinchada? En la Tierra la gravedad jala los líquidos hacia tus pies; aquí suben hacia tu cabeza. Es normal y se alivia tomando agua despacio.",
                quickChips = listOf("Siento presión en la frente", "Ojos cansados", "Me siento un poco mareado")
            ),
            "chest" to BodyZoneInfo(
                id = "chest",
                title = "Pecho, Corazón y Respiración",
                summary = "${heartMetric?.value ?: "68"} latidos/min • Aire ${oxygenMetric?.value ?: "98"}%",
                vsWeek = "Tu corazón late igual de tranquilo que toda la semana",
                trend = heartMetric?.history ?: listOf(68f, 70f),
                color = EirSky,
                whySpaceFeelsWeird = "Como no hay gravedad, tu corazón no tiene que bombear sangre cuesta arriba desde los pies. Por eso trabaja distinto y a veces sientes palpitaciones al moverte rápido.",
                quickChips = listOf("Me late rápido el corazón", "Siento el pecho apretado", "Me falta un poco el aire")
            ),
            "abdomen" to BodyZoneInfo(
                id = "abdomen",
                title = "Estómago, Agua y Energía",
                summary = "Agua e hidratación en nivel estable • Energía pareja",
                vsWeek = "Tu nivel de energía en sangre sigue parejo los últimos 7 días",
                trend = sugarMetric?.history ?: listOf(92f, 94f),
                color = EirTeal,
                whySpaceFeelsWeird = "En el espacio la comida flota en el estómago y suele dar menos hambre o sed de lo normal. Comer a tus horas evita mareos y bajones de ánimo.",
                quickChips = listOf("Tengo el estómago revuelto", "No tengo sed ni hambre", "Pesadez después de comer")
            ),
            "arms" to BodyZoneInfo(
                id = "arms",
                title = "Brazos, Hombros y Tono Muscular",
                summary = "Batería muscular ${batteryMetric?.value ?: "84"}% • Fuerza constante",
                vsWeek = "Tu fuerza en brazos y hombros se mantiene estable",
                trend = batteryMetric?.history ?: listOf(80f, 84f),
                color = EirCyan,
                whySpaceFeelsWeird = "En microgravedad usas los brazos constantemente para impulsarte y sujetarte de los rieles. Mantener los hombros relajados evita contracturas.",
                quickChips = listOf("Cansancio en hombros", "Fatiga en antebrazos", "Tensión al sujetar rieles")
            ),
            "legs" to BodyZoneInfo(
                id = "legs",
                title = "Piernas, Espalda y Huesos",
                summary = "Batería física ${batteryMetric?.value ?: "84"}% • 1 de 3 ejercicios hoy",
                vsWeek = "Tu recuperación muscular subió un 5% esta semana",
                trend = batteryMetric?.history ?: listOf(80f, 84f),
                color = EirAmber,
                whySpaceFeelsWeird = "Al flotar todo el día, tus piernas y espalda no cargan tu peso. Si no haces ejercicios con ligas, tus huesos se vuelven frágiles y la espalda baja duele al estirarse.",
                quickChips = listOf("Dolor en la espalda baja", "Piernas cansadas", "Punzada leve en rodilla")
            )
        )
    }
}
