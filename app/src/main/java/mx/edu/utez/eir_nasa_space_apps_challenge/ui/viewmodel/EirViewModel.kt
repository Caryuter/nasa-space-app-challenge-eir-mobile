package mx.edu.utez.eir_nasa_space_apps_challenge.ui.viewmodel

import android.app.Application
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.edu.utez.eir_nasa_space_apps_challenge.R
import mx.edu.utez.eir_nasa_space_apps_challenge.data.model.*
import mx.edu.utez.eir_nasa_space_apps_challenge.data.repository.EirHealthRepository
import mx.edu.utez.eir_nasa_space_apps_challenge.domain.engine.EirAiEngine
import mx.edu.utez.eir_nasa_space_apps_challenge.domain.engine.LocalDeterministicEirEngine
import mx.edu.utez.eir_nasa_space_apps_challenge.ui.theme.*
import java.util.Locale

data class EirUiState(
    val activeTab: String = "salud", // "salud", "habitos", "bitacora", "agente"
    val isDarkMode: Boolean = false,
    val toastMessage: String? = null,
    val bannerVisible: Boolean = true,

    // SOS Emergency
    val sosOpen: Boolean = false,
    val activeScenario: SosScenario = SosScenario(
        id = "faint",
        title = "Alguien se mareó o desmayó",
        tag = "MAREO / DESMAYO",
        kitItem = "Estuche Azul",
        steps = emptyList()
    ),
    val sosStepIndex: Int = 0,
    val isVoiceGuidePlaying: Boolean = false,

    // Health
    val healthMetrics: List<HealthMetric> = emptyList(),
    val expandedMetricId: String? = null,
    val selectedZoneId: String = "head",
    val discomforts: List<Discomfort> = listOf(
        Discomfort(1L, "legs", "Cansancio muscular leve en piernas", "Hoy 09:10")
    ),
    val reportingZoneId: String? = null,
    val customDiscomfortInput: String = "",

    // Habits
    val habitFilter: String = "all", // "all", "food", "exercise"
    val energyIntake: Int = 1750,
    val energyGoal: Int = 2400,
    val waterGlasses: Int = 4,
    val waterGoal: Int = 6,
    val dailyMeals: List<Meal> = emptyList(),
    val treadmillTimerSec: Int = 215,
    val isTreadmillRunning: Boolean = false,
    val exercises: List<Exercise> = emptyList(),

    // Journal & Breathing
    val moodLabel: String = "Tranquilo",
    val moodPoint: Pair<Float, Float> = Pair(74f, 64f),
    val journalInput: String = "",
    val journalEntries: List<JournalEntry> = emptyList(),
    val breathingActive: Boolean = false,
    val breathingPhase: Int = 0, // 0..3

    // Agent Chat
    val chatMessages: List<ChatMessage> = emptyList(),
    val chatInput: String = "",
    val isAgentLoading: Boolean = false,

    // Mandatory Checkup Modal (4 Steps)
    val checkupOpen: Boolean = false,
    val checkupStep: Int = 1, // 1..4
    val faceScanStatus: String = "idle", // "idle", "scanning", "done"
    val faceFindings: FaceScanFindings? = null,
    val phraseIdx: Int = 0,
    val voiceScanStatus: String = "idle", // "idle", "recording", "done"
    val voiceFindings: VoiceScanFindings? = null,
    val vitalsDraft: VitalsDraft = VitalsDraft(),
    val vitalsConfirmed: Boolean = false,
    val moodDraftLabel: String = "Tranquilo",
    val moodDraftPoint: Pair<Float, Float> = Pair(74f, 64f),
    val moodTouched: Boolean = false,

    // Outbox & Backend Sync
    val pendingOutboxCount: Int = 0
)

typealias SolisUiState = EirUiState

class EirViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = EirHealthRepository.getInstance(application)
    private val aiEngine: EirAiEngine = LocalDeterministicEirEngine()
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false

    private val calibrationPhrases = listOf(
        "Bitácora de Eir Sol 142: tripulación en calma, respiración estable y lista para iniciar turno.",
        "Reporte matutino de cabina: energía constante, mente despejada y sin molestias físicas."
    )

    private val sosScenarios = listOf(
        SosScenario(
            id = "faint",
            title = "Alguien se mareó o desmayó",
            tag = "MAREO / DESMAYO",
            kitItem = "Estuche Azul (Cruz Blanca) • Pinza de dedo + Botella electrolitos",
            steps = listOf(
                SosStep(
                    instruction = "Sujétalo al asiento con el cinturón",
                    why = "Evita que flote y golpee los paneles en gravedad cero.",
                    kitAction = "Arnés de pecho y cintura",
                    imageRes = R.drawable.sos_seatbelt
                ),
                SosStep(
                    instruction = "Coloca la pinza blanca en su dedo",
                    why = "Nos dirá en 5 segundos si le está llegando suficiente oxígeno.",
                    kitAction = "Pinza de oximetría (Estuche Azul)",
                    imageRes = R.drawable.sos_oximeter
                ),
                SosStep(
                    instruction = "Dale pequeños sorbos de agua con sales",
                    why = "En microgravedad los líquidos restablecen la presión arterial.",
                    kitAction = "Bolsa de sales con boquilla naranja",
                    imageRes = R.drawable.sos_drink_pouch
                ),
                SosStep(
                    instruction = "Déjalo descansar quieto 10 minutos",
                    why = "El sistema circulatorio necesita tiempo para reestabilizarse.",
                    kitAction = "Luz suave y acompañamiento",
                    imageRes = R.drawable.sos_seatbelt
                )
            )
        ),
        SosScenario(
            id = "chest",
            title = "Dolor fuerte en el pecho",
            tag = "CORAZÓN / PECHO",
            kitItem = "Maletín Rojo Principal • Parches cardíacos y pastilla sublingual",
            steps = listOf(
                SosStep(
                    instruction = "Detén cualquier esfuerzo y abróchalo al asiento",
                    why = "El corazón necesita consumir la menor energía posible ahora mismo.",
                    kitAction = "Asiento de cabina con arnés holgado",
                    imageRes = R.drawable.sos_seatbelt
                ),
                SosStep(
                    instruction = "Pídele que respire lento y profundo",
                    why = "Inhala 4 segundos, exhala 6 segundos para calmar los latidos.",
                    kitAction = "Conteo guiado y respiración pausada",
                    imageRes = null
                ),
                SosStep(
                    instruction = "Abre el Maletín Rojo y prepara los parches",
                    why = "Monitorea el ritmo cardíaco si el dolor no cede en 5 minutos.",
                    kitAction = "Parches cardíacos (Maletín Rojo)",
                    imageRes = R.drawable.sos_aed
                )
            )
        ),
        SosScenario(
            id = "cut",
            title = "Corte o golpe con sangrado",
            tag = "HERIDA / GOLPE",
            kitItem = "Bolsa Verde • Gasas absorbentes selladas y venda elástica",
            steps = listOf(
                SosStep(
                    instruction = "Presiona la herida con gasa gruesa sin soltar",
                    why = "En gravedad cero la sangre no cae: forma burbujas y no coagula bien.",
                    kitAction = "Gasas absorbentes (Bolsa Verde)",
                    imageRes = R.drawable.sos_gauze
                ),
                SosStep(
                    instruction = "Mantén presión firme por 3 minutos seguidos",
                    why = "Levantar la gasa antes de tiempo rompe el coágulo en formación.",
                    kitAction = "Compresión continua sin levantar el apósito",
                    imageRes = R.drawable.sos_gauze
                ),
                SosStep(
                    instruction = "Envuelve con la venda elástica autoadhesiva",
                    why = "Fija el vendaje asegurando que los dedos sigan tibios y con flujo.",
                    kitAction = "Rollo elástico azul (Bolsa Verde)",
                    imageRes = R.drawable.sos_gauze
                )
            )
        ),
        SosScenario(
            id = "breath",
            title = "Falta de aire o ahogo",
            tag = "RESPIRACIÓN",
            kitItem = "Panel Lateral • Mascarilla transparente de oxígeno",
            steps = listOf(
                SosStep(
                    instruction = "Coloca la mascarilla y gira la válvula verde",
                    why = "Suministra flujo de oxígeno puro directo a nariz y boca.",
                    kitAction = "Mascarilla de oxígeno (Panel Lateral)",
                    imageRes = R.drawable.sos_mask
                ),
                SosStep(
                    instruction = "Enciende el ventilador portátil hacia su rostro",
                    why = "En microgravedad el CO₂ exhalado se queda flotando frente a la cara.",
                    kitAction = "Interruptor azul de flujo de cabina",
                    imageRes = R.drawable.sos_mask
                ),
                SosStep(
                    instruction = "Afloja el cuello de su traje espacial",
                    why = "Libera la vía respiratoria y alivia la sensación de ahogo.",
                    kitAction = "Cierre y sellos del cuello",
                    imageRes = R.drawable.sos_seatbelt
                )
            )
        )
    )

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

    private val initialMeals = listOf(
        Meal(1L, "Desayuno energético + Vitamina D (para huesos)", 550, true),
        Meal(2L, "Comida principal rica en proteína", 700, true),
        Meal(3L, "Colación de frutos secos y calcio", 500, true),
        Meal(4L, "Cena ligera + Sobres de sales minerales", 650, false)
    )

    private val initialExercises = listOf(
        Exercise(
            id = 1L,
            name = "Sentadillas con ligas de resistencia",
            why = "Mantiene fuertes los huesos de cadera y piernas sin gravedad",
            duration = "20 min",
            done = true
        ),
        Exercise(
            id = 2L,
            name = "Caminadora espacial con arnés",
            why = "Cuida tu corazón y evita que pierdas condición física",
            duration = "15 min",
            done = false
        ),
        Exercise(
            id = 3L,
            name = "Remo de espalda y abdomen",
            why = "Evita dolor de espalda baja por flotar todo el día",
            duration = "10 min",
            done = false
        )
    )

    private val initialJournal = listOf(
        JournalEntry(
            id = 1L,
            sol = "Sol 142 • Hoy 08:15",
            mood = "Tranquilo",
            x = 74f,
            y = 64f,
            text = "Dormí mucho mejor después de bajar un poco la temperatura de mi cabina. Me siento listo para las pruebas de hoy."
        ),
        JournalEntry(
            id = 2L,
            sol = "Sol 141 • Ayer 21:30",
            mood = "Nostálgico",
            x = 50f,
            y = 44f,
            text = "Vi una foto de mi familia antes de dormir. Se siente la distancia de casa, pero hacer los ejercicios de respiración me relajó."
        ),
        JournalEntry(
            id = 3L,
            sol = "Sol 140 • Hace 2 días",
            mood = "Motivado",
            x = 84f,
            y = 80f,
            text = "Terminamos de calibrar los paneles solares antes de tiempo. Gran equipo a bordo."
        )
    )

    private val initialChat = listOf(
        ChatMessage(
            id = 1L,
            sender = "eir",
            text = "Hola. Vi tus datos de hoy: dormiste 7.5 horas, tus latidos están tranquilos (68) y tu batería física está al 84%. ¿Cómo sientes tu cuerpo ahorita?",
            canSimplify = true
        )
    )

    private val _uiState = MutableStateFlow(
        EirUiState(
            activeScenario = sosScenarios[0],
            healthMetrics = initialMetrics,
            dailyMeals = initialMeals,
            exercises = initialExercises,
            journalEntries = initialJournal,
            chatMessages = initialChat
        )
    )
    val uiState: StateFlow<EirUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var breathingJob: Job? = null

    init {
        tts = TextToSpeech(application) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                isTtsInitialized = true
            }
        }

        // Initialize Repository & Perform First Load / Bootstrap from backend or local DB
        viewModelScope.launch {
            repository.ensureInitializedAndBootstrapped()

            // Observe Outbox pending items count
            launch {
                repository.pendingOutboxCountFlow.collect { count ->
                    _uiState.update { it.copy(pendingOutboxCount = count) }
                }
            }

            // Observe Latest Health Record from Room DB
            launch {
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
                            current.copy(healthMetrics = updatedMetrics)
                        }
                    }
                }
            }

            // Observe Wellbeing Checkins from Room DB
            launch {
                repository.wellbeingCheckinsFlow.collect { checkins ->
                    if (checkins.isNotEmpty()) {
                        val newEntries = checkins.map { ch ->
                            JournalEntry(
                                id = ch.recordId.hashCode().toLong(),
                                sol = "Sol 142 • " + ch.recordedAt.take(10),
                                mood = when (ch.mood) {
                                    5 -> "Motivado"
                                    4 -> "Tranquilo"
                                    3 -> "Enfocado"
                                    2 -> "Cansado"
                                    else -> "Nostálgico"
                                },
                                x = (ch.mood * 18f).coerceIn(10f, 90f),
                                y = (ch.energy * 18f).coerceIn(10f, 90f),
                                text = ch.note ?: "Registro guardado en Eir."
                            )
                        }
                        _uiState.update { current ->
                            val merged = (newEntries + current.journalEntries).distinctBy { it.id }
                            current.copy(journalEntries = merged)
                        }
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
    }

    fun setActiveTab(tab: String) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun toggleDarkMode() {
        _uiState.update { it.copy(isDarkMode = !it.isDarkMode) }
    }

    fun dismissBanner() {
        _uiState.update { it.copy(bannerVisible = false) }
    }

    fun showToast(msg: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(toastMessage = msg) }
            delay(3200)
            _uiState.update { it.copy(toastMessage = null) }
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
        showToast("Molestia anotada en tu mapa corporal")
    }

    fun removeDiscomfort(id: Long) {
        _uiState.update {
            it.copy(discomforts = it.discomforts.filter { item -> item.id != id })
        }
    }

    fun setHabitFilter(filter: String) {
        _uiState.update { it.copy(habitFilter = filter) }
    }

    fun addWaterGlass() {
        _uiState.update { current ->
            val next = (current.waterGlasses + 1).coerceAtMost(current.waterGoal)
            val remaining = current.waterGoal - next
            val updatedMetrics = current.healthMetrics.map { m ->
                if (m.id == "hydration") {
                    m.copy(
                        value = "$next/${current.waterGoal}",
                        humanLabel = if (remaining == 0) "Meta de agua cumplida" else "Faltan $remaining vasos",
                        updatedAt = "Hace un momento",
                        history = m.history.drop(1) + next.toFloat()
                    )
                } else m
            }
            current.copy(
                waterGlasses = next,
                healthMetrics = updatedMetrics
            )
        }
        showToast("+1 vaso de agua registrado")
    }

    fun toggleMeal(mealId: Long) {
        _uiState.update { current ->
            var deltaKcal = 0
            val updatedMeals = current.dailyMeals.map { m ->
                if (m.id == mealId) {
                    val nextDone = !m.done
                    deltaKcal = if (nextDone) m.kcal else -m.kcal
                    m.copy(done = nextDone)
                } else m
            }
            current.copy(
                dailyMeals = updatedMeals,
                energyIntake = current.energyIntake + deltaKcal
            )
        }
    }

    fun toggleTreadmill() {
        val running = !_uiState.value.isTreadmillRunning
        _uiState.update { it.copy(isTreadmillRunning = running) }
        if (running) {
            timerJob?.cancel()
            timerJob = viewModelScope.launch {
                while (true) {
                    delay(1000)
                    _uiState.update { it.copy(treadmillTimerSec = it.treadmillTimerSec + 1) }
                }
            }
        } else {
            timerJob?.cancel()
        }
    }

    fun resetTreadmill() {
        timerJob?.cancel()
        _uiState.update { it.copy(isTreadmillRunning = false, treadmillTimerSec = 0) }
    }

    fun toggleExercise(exId: Long) {
        _uiState.update { current ->
            current.copy(
                exercises = current.exercises.map { if (it.id == exId) it.copy(done = !it.done) else it }
            )
        }
    }

    fun toggleBreathing() {
        val active = !_uiState.value.breathingActive
        _uiState.update { it.copy(breathingActive = active, breathingPhase = 0) }
        if (active) {
            breathingJob?.cancel()
            breathingJob = viewModelScope.launch {
                while (true) {
                    delay(4000)
                    _uiState.update { it.copy(breathingPhase = (it.breathingPhase + 1) % 4) }
                }
            }
        } else {
            breathingJob?.cancel()
        }
    }

    fun setJournalInput(text: String) {
        _uiState.update { it.copy(journalInput = text) }
    }

    fun saveJournalEntry() {
        val text = _uiState.value.journalInput.trim()
        if (text.isBlank()) return

        val moodRating = when (_uiState.value.moodLabel) {
            "Motivado" -> 5
            "Tranquilo" -> 4
            "Enfocado" -> 3
            "Cansado" -> 2
            else -> 1
        }

        viewModelScope.launch {
            repository.saveWellbeingCheckin(
                mood = moodRating,
                connection = 4,
                energy = (_uiState.value.moodPoint.second / 20f).toInt().coerceIn(1, 5),
                purpose = 5,
                sleep = 4,
                note = text
            )

            setJournalInput("")
            showToast("Nota sincronizada en tu Bitácora")
        }
    }

    fun setChatInput(text: String) {
        _uiState.update { it.copy(chatInput = text) }
    }

    fun sendChatMessage(
        textOverride: String? = null,
        forceSimpler: Boolean = false,
        giveSteps: Boolean = false
    ) {
        val query = (textOverride ?: _uiState.value.chatInput).trim()
        if (query.isBlank()) return

        if (!forceSimpler && !giveSteps) {
            val userMsg = ChatMessage(System.currentTimeMillis(), "user", query)
            _uiState.update { it.copy(chatMessages = it.chatMessages + userMsg, chatInput = "", isAgentLoading = true) }
        } else {
            _uiState.update { it.copy(isAgentLoading = true) }
        }

        viewModelScope.launch {
            val context = "Latidos: ${_uiState.value.vitalsDraft.heartRate}/min, Batería: 84%, Sueño: ${_uiState.value.vitalsDraft.sleepHours}h, Agua: ${_uiState.value.waterGlasses}/${_uiState.value.waterGoal}, Ánimo: ${_uiState.value.moodLabel}"
            delay(500)
            val reply = aiEngine.query(query, context, forceSimpler, giveSteps)
            val botMsg = ChatMessage(System.currentTimeMillis() + 1, "eir", reply, canSimplify = !forceSimpler)
            _uiState.update {
                it.copy(chatMessages = it.chatMessages + botMsg, isAgentLoading = false)
            }
        }
    }

    fun openCheckup() {
        _uiState.update {
            it.copy(
                checkupOpen = true,
                checkupStep = 1,
                faceScanStatus = "idle",
                faceFindings = null,
                voiceScanStatus = "idle",
                voiceFindings = null,
                vitalsDraft = VitalsDraft(),
                vitalsConfirmed = false,
                moodDraftLabel = it.moodLabel,
                moodDraftPoint = it.moodPoint,
                moodTouched = false
            )
        }
    }

    fun closeCheckup() {
        _uiState.update { it.copy(checkupOpen = false) }
    }

    fun setCheckupStep(step: Int) {
        _uiState.update { it.copy(checkupStep = step) }
    }

    fun runFaceScan() {
        _uiState.update { it.copy(faceScanStatus = "scanning") }
        viewModelScope.launch {
            delay(1500)
            _uiState.update {
                it.copy(
                    faceScanStatus = "done",
                    faceFindings = FaceScanFindings(
                        fluidShift = "Hinchazón facial normal por falta de gravedad (leve)",
                        eyeFatigue = "Mirada despejada, sin cansancio visual fuerte",
                        skinTone = "Buena circulación y color saludable"
                    )
                )
            }
        }
    }

    fun cyclePhrase() {
        _uiState.update {
            it.copy(
                phraseIdx = (it.phraseIdx + 1) % calibrationPhrases.size,
                voiceScanStatus = "idle",
                voiceFindings = null
            )
        }
    }

    fun getCurrentPhrase(): String = calibrationPhrases[_uiState.value.phraseIdx]

    fun runVoiceScan() {
        _uiState.update { it.copy(voiceScanStatus = "recording") }
        viewModelScope.launch {
            delay(1700)
            _uiState.update {
                it.copy(
                    voiceScanStatus = "done",
                    voiceFindings = VoiceScanFindings(
                        breathingPace = "Respiración pausada, sin falta de aire al hablar",
                        vocalFatigue = "Voz firme y sin cansancio en la garganta",
                        nasalCongestion = "Ligera congestión nasal típica del espacio (normal)"
                    )
                )
            }
        }
    }

    fun updateVitalsDraft(modifier: (VitalsDraft) -> VitalsDraft) {
        _uiState.update {
            it.copy(vitalsDraft = modifier(it.vitalsDraft), vitalsConfirmed = true)
        }
    }

    fun toggleVitalsConfirmed() {
        _uiState.update { it.copy(vitalsConfirmed = !it.vitalsConfirmed) }
    }

    fun setMoodDraftPoint(x: Float, y: Float) {
        _uiState.update {
            it.copy(
                moodDraftPoint = Pair(x.coerceIn(6f, 94f), y.coerceIn(6f, 94f)),
                moodTouched = true
            )
        }
    }

    fun setMoodDraftLabel(label: String) {
        _uiState.update {
            it.copy(moodDraftLabel = label, moodTouched = true)
        }
    }

    fun completeCheckup() {
        val draft = _uiState.value.vitalsDraft
        val moodLabel = _uiState.value.moodDraftLabel

        viewModelScope.launch {
            // 1. Save Health Checkup via Outbox Pattern
            repository.saveHealthCheckup(
                heartRate = draft.heartRate,
                systolicBp = draft.sys,
                temp = draft.temp,
                sleepHours = draft.sleepHours
            )

            // 2. Save Wellbeing Checkin via Outbox Pattern
            val moodRating = when (moodLabel) {
                "Motivado" -> 5
                "Tranquilo" -> 4
                "Enfocado" -> 3
                "Cansado" -> 2
                else -> 1
            }

            repository.saveWellbeingCheckin(
                mood = moodRating,
                connection = 4,
                energy = (_uiState.value.moodDraftPoint.second / 20f).toInt().coerceIn(1, 5),
                purpose = 5,
                sleep = (draft.sleepHours / 2f).toInt().coerceIn(1, 5),
                note = "Chequeo completado: pulso en ${draft.heartRate}, ${draft.sleepHours}h de sueño."
            )

            _uiState.update {
                it.copy(
                    moodLabel = it.moodDraftLabel,
                    moodPoint = it.moodDraftPoint,
                    checkupOpen = false
                )
            }
            showToast(getApplication<Application>().getString(R.string.checkup_completed_toast))
        }
    }

    fun openSos() {
        _uiState.update {
            it.copy(
                sosOpen = true,
                activeScenario = sosScenarios[0],
                sosStepIndex = 0,
                isVoiceGuidePlaying = false
            )
        }
    }

    fun closeSos() {
        tts?.stop()
        _uiState.update { it.copy(sosOpen = false, isVoiceGuidePlaying = false) }
    }

    fun selectSosScenario(scenarioId: String) {
        val sc = sosScenarios.find { it.id == scenarioId } ?: return
        tts?.stop()
        _uiState.update {
            it.copy(activeScenario = sc, sosStepIndex = 0, isVoiceGuidePlaying = false)
        }
    }

    fun nextSosStep() {
        val currentIdx = _uiState.value.sosStepIndex
        val maxIdx = _uiState.value.activeScenario.steps.size - 1
        if (currentIdx < maxIdx) {
            tts?.stop()
            _uiState.update { it.copy(sosStepIndex = currentIdx + 1, isVoiceGuidePlaying = false) }
        }
    }

    fun prevSosStep() {
        val currentIdx = _uiState.value.sosStepIndex
        if (currentIdx > 0) {
            tts?.stop()
            _uiState.update { it.copy(sosStepIndex = currentIdx - 1, isVoiceGuidePlaying = false) }
        }
    }

    fun playVoiceGuide() {
        val step = _uiState.value.activeScenario.steps[_uiState.value.sosStepIndex]
        val textToSpeak = "${step.instruction}. ${step.why}. ${step.kitAction}"
        _uiState.update { it.copy(isVoiceGuidePlaying = true) }

        if (isTtsInitialized) {
            tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "sos_guide")
            viewModelScope.launch {
                delay(3000)
                _uiState.update { it.copy(isVoiceGuidePlaying = false) }
            }
        } else {
            viewModelScope.launch {
                delay(1800)
                _uiState.update { it.copy(isVoiceGuidePlaying = false) }
            }
        }
    }

    fun getSosScenarios(): List<SosScenario> = sosScenarios

    fun getZoneStatus(zoneId: String): mx.edu.utez.eir_nasa_space_apps_challenge.ui.components.BodyZoneStatus {
        val state = _uiState.value
        val zoneDiscomforts = state.discomforts.filter { it.zoneId == zoneId }
        val heartRate = state.vitalsDraft.heartRate
        val temp = state.vitalsDraft.temp
        val water = state.waterGlasses

        val hasCriticalDiscomfort = zoneDiscomforts.any {
            it.label.contains("fuerte", ignoreCase = true) ||
            it.label.contains("apretado", ignoreCase = true) ||
            it.label.contains("falta de aire", ignoreCase = true) ||
            it.label.contains("mareo", ignoreCase = true)
        }

        if (hasCriticalDiscomfort || (zoneId == "chest" && heartRate > 100) || (zoneId == "head" && temp >= 38.0f)) {
            return mx.edu.utez.eir_nasa_space_apps_challenge.ui.components.BodyZoneStatus.CRITICAL
        }

        if (zoneDiscomforts.isNotEmpty() || (zoneId == "abdomen" && water < 4) || (zoneId == "head" && state.vitalsDraft.sleepHours < 6.0f)) {
            return mx.edu.utez.eir_nasa_space_apps_challenge.ui.components.BodyZoneStatus.MODERATE
        }

        return mx.edu.utez.eir_nasa_space_apps_challenge.ui.components.BodyZoneStatus.OPTIMAL
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
                summary = "${state.waterGlasses} de ${state.waterGoal} vasos de agua • Energía estable",
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

typealias SolisViewModel = EirViewModel
