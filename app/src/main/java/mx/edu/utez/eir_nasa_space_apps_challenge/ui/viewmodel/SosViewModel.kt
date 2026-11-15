package mx.edu.utez.eir_nasa_space_apps_challenge.ui.viewmodel

import android.app.Application
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.edu.utez.eir_nasa_space_apps_challenge.R
import mx.edu.utez.eir_nasa_space_apps_challenge.data.model.SosScenario
import mx.edu.utez.eir_nasa_space_apps_challenge.data.model.SosStep
import java.util.Locale

data class SosUiState(
    val sosOpen: Boolean = false,
    val activeScenario: SosScenario,
    val sosStepIndex: Int = 0,
    val isVoiceGuidePlaying: Boolean = false
)

class SosViewModel(application: Application) : AndroidViewModel(application) {

    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false

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

    private val _uiState = MutableStateFlow(
        SosUiState(activeScenario = sosScenarios[0])
    )
    val uiState: StateFlow<SosUiState> = _uiState.asStateFlow()

    init {
        tts = TextToSpeech(application) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.Builder().setLanguage("es").setRegion("MX").build()
                isTtsInitialized = true
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
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
}
