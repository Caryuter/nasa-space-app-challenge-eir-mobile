package mx.edu.utez.eir_nasa_space_apps_challenge.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.edu.utez.eir_nasa_space_apps_challenge.data.model.ChatMessage
import mx.edu.utez.eir_nasa_space_apps_challenge.data.repository.EirHealthRepository
import mx.edu.utez.eir_nasa_space_apps_challenge.domain.engine.EirAiEngine
import mx.edu.utez.eir_nasa_space_apps_challenge.domain.engine.LiteRtLmEirEngine
import mx.edu.utez.eir_nasa_space_apps_challenge.domain.engine.LiteRtModelStatus

data class AgenteIaUiState(
    val chatMessages: List<ChatMessage> = emptyList(),
    val chatInput: String = "",
    val isAgentLoading: Boolean = false,
    val modelStatus: LiteRtModelStatus = LiteRtModelStatus.NotDownloaded(),
    val isChatUnlocked: Boolean = false,
    val personalizedContextPill: String = "Sincronizado: Latidos 68 • Sueño 7.5h",
    val isDownloadingOrConnecting: Boolean = false
)

class AgenteIaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = EirHealthRepository.getInstance(application)
    private val aiEngine: EirAiEngine = LiteRtLmEirEngine(application)

    private var currentBiometricContext: String = "Latidos: 68/min, Batería: 84%, Sueño: 7.5h, Agua: 4/6, Ánimo: Tranquilo"

    private val initialChat = listOf(
        ChatMessage(
            id = 1L,
            sender = "eir",
            text = "Hola, comandante. Soy EIR. Para chatear con el asistente local sin conexión, descárgalo arriba.",
            canSimplify = false
        )
    )

    private val _uiState = MutableStateFlow(AgenteIaUiState(chatMessages = initialChat))
    val uiState: StateFlow<AgenteIaUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureInitializedAndBootstrapped()
            repository.latestHealthRecordFlow.collect { record ->
                if (record != null) {
                    val heartRate = record.heartRateBpm ?: 68
                    val sleep = record.sleepHours ?: 7.5
                    val temp = record.temperatureC ?: 36.7
                    currentBiometricContext = "Latidos: $heartRate/min, Sueño: ${sleep}h, Temp: ${temp}°C, Batería: 84%, Ánimo: Tranquilo"

                    _uiState.update {
                        it.copy(
                            personalizedContextPill = "Sincronizado: $heartRate lat/min • ${sleep}h descanso"
                        )
                    }
                }
            }
        }

        (aiEngine as? LiteRtLmEirEngine)?.let { engine ->
            viewModelScope.launch {
                engine.modelStatus.collect { status ->
                    val isUnlocked = status is LiteRtModelStatus.Ready
                    val isBusy = status is LiteRtModelStatus.Downloading || status is LiteRtModelStatus.Initializing

                    _uiState.update {
                        it.copy(
                            modelStatus = status,
                            isChatUnlocked = isUnlocked,
                            isDownloadingOrConnecting = isBusy
                        )
                    }
                }
            }
        }
    }

    fun startDownloadModel() {
        if (_uiState.value.isDownloadingOrConnecting) return
        _uiState.update { it.copy(isDownloadingOrConnecting = true) }
        (aiEngine as? LiteRtLmEirEngine)?.startModelDownload()
    }

    fun cancelDownloadModel() {
        _uiState.update { it.copy(isDownloadingOrConnecting = false) }
        (aiEngine as? LiteRtLmEirEngine)?.cancelModelDownload()
    }

    fun setChatInput(text: String) {
        _uiState.update { it.copy(chatInput = text) }
    }

    fun sendChatMessage(
        textOverride: String? = null,
        forceSimpler: Boolean = false,
        giveSteps: Boolean = false
    ) {
        if (!_uiState.value.isChatUnlocked) return

        val query = (textOverride ?: _uiState.value.chatInput).trim()
        if (query.isBlank()) return

        if (!forceSimpler && !giveSteps) {
            val userMsg = ChatMessage(System.currentTimeMillis(), "user", query)
            _uiState.update { it.copy(chatMessages = it.chatMessages + userMsg, chatInput = "", isAgentLoading = true) }
        } else {
            _uiState.update { it.copy(isAgentLoading = true) }
        }

        viewModelScope.launch {
            val reply = aiEngine.query(query, currentBiometricContext, forceSimpler, giveSteps)
            val botMsg = ChatMessage(System.currentTimeMillis() + 1, "eir", reply, canSimplify = !forceSimpler)
            _uiState.update {
                it.copy(chatMessages = it.chatMessages + botMsg, isAgentLoading = false)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        (aiEngine as? LiteRtLmEirEngine)?.close()
    }
}
