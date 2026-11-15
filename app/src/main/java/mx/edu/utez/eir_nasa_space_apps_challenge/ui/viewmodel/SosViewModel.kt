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
                    instruction = "Sujétalo al asiento con el cinturón para que no flote ni se golpee.",
                    why = "En gravedad cero un cuerpo inconsciente puede chocar contra los paneles.",
                    kitAction = "Asegura primero el arnés de pecho y cintura."
                ),
                SosStep(
                    instruction = "Saca del estuche azul la pinza blanca y ponla en su dedo índice.",
                    why = "Nos dirá en 5 segundos si le está llegando suficiente aire y sangre.",
                    kitAction = "Usa la pinza del bolsillo superior del Estuche Azul."
                ),
                SosStep(
                    instruction = "Si ya despertó, dale pequeños sorbos de agua con sales.",
                    why = "En el espacio los líquidos suben a la cabeza y al moverse rápido baja la presión.",
                    kitAction = "Usa la bolsa con boquilla naranja del botiquín."
                ),
                SosStep(
                    instruction = "Déjalo descansar quieto 10 minutos antes de volver a moverse.",
                    why = "Su cuerpo necesita tiempo para reacomodar la circulación.",
                    kitAction = "Mantén luz suave y acompáñalo."
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
                    instruction = "Detén cualquier esfuerzo de inmediato y abróchalo en un asiento.",
                    why = "El corazón necesita gastar la menor energía posible ahora mismo.",
                    kitAction = "No lo dejes caminar ni hacer fuerza."
                ),
                SosStep(
                    instruction = "Pídele que respire lento: 4 segundos adentro, 6 segundos afuera.",
                    why = "Respirar despacio calma los latidos acelerados y baja la tensión.",
                    kitAction = "Cuenta en voz alta junto con él para darle calma."
                ),
                SosStep(
                    instruction = "Abre el Maletín Rojo y ten a la mano el equipo con parches.",
                    why = "Si el dolor no baja en 5 minutos, el equipo guía paso a paso por voz.",
                    kitAction = "Maletín Rojo • Compartimento central con símbolo de corazón."
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
                    instruction = "Presiona directo sobre la herida con una gasa gruesa sin soltar.",
                    why = "Sin gravedad, la sangre no cae al suelo: se acumula como una burbuja.",
                    kitAction = "Saca los paquetes blancos de la Bolsa Verde."
                ),
                SosStep(
                    instruction = "Mantén presión firme durante 3 minutos completos sin levantar la gasa.",
                    why = "Levantarla antes de tiempo rompe el tapón natural que frena el sangrado.",
                    kitAction = "Usa el cronómetro en pantalla y mantén firme la mano."
                ),
                SosStep(
                    instruction = "Envuelve con la venda autoadhesiva sin apretar demasiado.",
                    why = "Debe detener el sangrado pero dejar que los dedos sigan calientes.",
                    kitAction = "Rollo azul elástico de la Bolsa Verde."
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
                    instruction = "Colócale la mascarilla transparente sobre nariz y boca y gira la válvula verde.",
                    why = "Le dará aire puro de inmediato mientras revisamos la cabina.",
                    kitAction = "Mascarilla en el compartimento derecho junto a tu asiento."
                ),
                SosStep(
                    instruction = "Enciende el ventilador portátil frente a su rostro.",
                    why = "En el espacio el aire exhalado se queda quieto alrededor de la cara si no circula.",
                    kitAction = "Interruptor azul de ventilación de cabina."
                ),
                SosStep(
                    instruction = "Afloja el cuello de su traje y quédate a su lado hasta que respire normal.",
                    why = "Sentirse acompañado reduce el susto y ayuda a recuperar el ritmo.",
                    kitAction = "Verifica que el indicador de aire de cabina esté en verde."
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
