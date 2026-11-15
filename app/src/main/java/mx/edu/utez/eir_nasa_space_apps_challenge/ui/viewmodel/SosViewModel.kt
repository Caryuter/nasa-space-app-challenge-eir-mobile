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

    private val scenarioList by lazy {
        listOf(
            SosScenario(
                id = "faint",
                title = application.getString(R.string.sos_faint_title),
                tag = application.getString(R.string.sos_faint_tag),
                kitItem = application.getString(R.string.sos_faint_kit),
                steps = listOf(
                    SosStep(
                        instruction = application.getString(R.string.sos_faint_step1_instruction),
                        why = application.getString(R.string.sos_faint_step1_why),
                        kitAction = application.getString(R.string.sos_faint_step1_kit),
                        imageRes = R.drawable.sos_seatbelt
                    ),
                    SosStep(
                        instruction = application.getString(R.string.sos_faint_step2_instruction),
                        why = application.getString(R.string.sos_faint_step2_why),
                        kitAction = application.getString(R.string.sos_faint_step2_kit),
                        imageRes = R.drawable.sos_oximeter
                    ),
                    SosStep(
                        instruction = application.getString(R.string.sos_faint_step3_instruction),
                        why = application.getString(R.string.sos_faint_step3_why),
                        kitAction = application.getString(R.string.sos_faint_step3_kit),
                        imageRes = R.drawable.sos_drink_pouch
                    ),
                    SosStep(
                        instruction = application.getString(R.string.sos_faint_step4_instruction),
                        why = application.getString(R.string.sos_faint_step4_why),
                        kitAction = application.getString(R.string.sos_faint_step4_kit),
                        imageRes = R.drawable.sos_seatbelt
                    )
                )
            ),
            SosScenario(
                id = "chest",
                title = application.getString(R.string.sos_chest_title),
                tag = application.getString(R.string.sos_chest_tag),
                kitItem = application.getString(R.string.sos_chest_kit),
                steps = listOf(
                    SosStep(
                        instruction = application.getString(R.string.sos_chest_step1_instruction),
                        why = application.getString(R.string.sos_chest_step1_why),
                        kitAction = application.getString(R.string.sos_chest_step1_kit),
                        imageRes = R.drawable.sos_seatbelt
                    ),
                    SosStep(
                        instruction = application.getString(R.string.sos_chest_step2_instruction),
                        why = application.getString(R.string.sos_chest_step2_why),
                        kitAction = application.getString(R.string.sos_chest_step2_kit),
                        imageRes = null
                    ),
                    SosStep(
                        instruction = application.getString(R.string.sos_chest_step3_instruction),
                        why = application.getString(R.string.sos_chest_step3_why),
                        kitAction = application.getString(R.string.sos_chest_step3_kit),
                        imageRes = R.drawable.sos_aed
                    )
                )
            ),
            SosScenario(
                id = "cut",
                title = application.getString(R.string.sos_cut_title),
                tag = application.getString(R.string.sos_cut_tag),
                kitItem = application.getString(R.string.sos_cut_kit),
                steps = listOf(
                    SosStep(
                        instruction = application.getString(R.string.sos_cut_step1_instruction),
                        why = application.getString(R.string.sos_cut_step1_why),
                        kitAction = application.getString(R.string.sos_cut_step1_kit),
                        imageRes = R.drawable.sos_gauze
                    ),
                    SosStep(
                        instruction = application.getString(R.string.sos_cut_step2_instruction),
                        why = application.getString(R.string.sos_cut_step2_why),
                        kitAction = application.getString(R.string.sos_cut_step2_kit),
                        imageRes = R.drawable.sos_gauze
                    ),
                    SosStep(
                        instruction = application.getString(R.string.sos_cut_step3_instruction),
                        why = application.getString(R.string.sos_cut_step3_why),
                        kitAction = application.getString(R.string.sos_cut_step3_kit),
                        imageRes = R.drawable.sos_gauze
                    )
                )
            ),
            SosScenario(
                id = "breath",
                title = application.getString(R.string.sos_breath_title),
                tag = application.getString(R.string.sos_breath_tag),
                kitItem = application.getString(R.string.sos_breath_kit),
                steps = listOf(
                    SosStep(
                        instruction = application.getString(R.string.sos_breath_step1_instruction),
                        why = application.getString(R.string.sos_breath_step1_why),
                        kitAction = application.getString(R.string.sos_breath_step1_kit),
                        imageRes = R.drawable.sos_mask
                    ),
                    SosStep(
                        instruction = application.getString(R.string.sos_breath_step2_instruction),
                        why = application.getString(R.string.sos_breath_step2_why),
                        kitAction = application.getString(R.string.sos_breath_step2_kit),
                        imageRes = R.drawable.sos_mask
                    ),
                    SosStep(
                        instruction = application.getString(R.string.sos_breath_step3_instruction),
                        why = application.getString(R.string.sos_breath_step3_why),
                        kitAction = application.getString(R.string.sos_breath_step3_kit),
                        imageRes = R.drawable.sos_seatbelt
                    )
                )
            )
        )
    }

    private val _uiState by lazy {
        MutableStateFlow(
            SosUiState(activeScenario = scenarioList[0])
        )
    }
    val uiState: StateFlow<SosUiState> by lazy { _uiState.asStateFlow() }

    init {
        tts = TextToSpeech(application) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
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
                activeScenario = scenarioList[0],
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
        val sc = scenarioList.find { it.id == scenarioId } ?: return
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

    fun getSosScenarios(): List<SosScenario> = scenarioList
}
