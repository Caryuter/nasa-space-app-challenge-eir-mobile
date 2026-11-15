package mx.edu.utez.eir_nasa_space_apps_challenge.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.edu.utez.eir_nasa_space_apps_challenge.data.model.ChatMessage
import mx.edu.utez.eir_nasa_space_apps_challenge.domain.engine.EirAiEngine
import mx.edu.utez.eir_nasa_space_apps_challenge.domain.engine.LocalDeterministicEirEngine

data class AgenteIaUiState(
    val chatMessages: List<ChatMessage> = emptyList(),
    val chatInput: String = "",
    val isAgentLoading: Boolean = false
)

class AgenteIaViewModel(application: Application) : AndroidViewModel(application) {

    private val aiEngine: EirAiEngine = LocalDeterministicEirEngine()

    private val initialChat = listOf(
        ChatMessage(
            id = 1L,
            sender = "eir",
            text = "Hola. Vi tus datos de hoy: dormiste 7.5 horas, tus latidos están tranquilos (68) y tu batería física está al 84%. ¿Cómo sientes tu cuerpo ahorita?",
            canSimplify = true
        )
    )

    private val _uiState = MutableStateFlow(AgenteIaUiState(chatMessages = initialChat))
    val uiState: StateFlow<AgenteIaUiState> = _uiState.asStateFlow()

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
            val context = "Latidos: 68/min, Batería: 84%, Sueño: 7.5h, Agua: 4/6, Ánimo: Tranquilo"
            delay(500)
            val reply = aiEngine.query(query, context, forceSimpler, giveSteps)
            val botMsg = ChatMessage(System.currentTimeMillis() + 1, "eir", reply, canSimplify = !forceSimpler)
            _uiState.update {
                it.copy(chatMessages = it.chatMessages + botMsg, isAgentLoading = false)
            }
        }
    }
}
