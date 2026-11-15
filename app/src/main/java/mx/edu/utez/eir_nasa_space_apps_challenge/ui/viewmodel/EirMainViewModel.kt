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
import mx.edu.utez.eir_nasa_space_apps_challenge.data.repository.EirHealthRepository

data class EirMainUiState(
    val activeTab: String = "salud", // "salud", "habitos", "bitacora", "agente"
    val isDarkMode: Boolean = false,
    val toastMessage: String? = null,
    val pendingOutboxCount: Int = 0
)

class EirMainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = EirHealthRepository.getInstance(application)

    private val _uiState = MutableStateFlow(EirMainUiState())
    val uiState: StateFlow<EirMainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.pendingOutboxCountFlow.collect { count ->
                _uiState.update { it.copy(pendingOutboxCount = count) }
            }
        }
    }

    fun setActiveTab(tab: String) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun toggleDarkMode() {
        _uiState.update { it.copy(isDarkMode = !it.isDarkMode) }
    }

    fun showToast(msg: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(toastMessage = msg) }
            delay(3200)
            _uiState.update { it.copy(toastMessage = null) }
        }
    }
}
