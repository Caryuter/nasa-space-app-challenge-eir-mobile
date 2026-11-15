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
import mx.edu.utez.eir_nasa_space_apps_challenge.R
import mx.edu.utez.eir_nasa_space_apps_challenge.data.reminder.CheckupReminderWorker
import mx.edu.utez.eir_nasa_space_apps_challenge.data.repository.EirHealthRepository

data class EirMainUiState(
    val activeTab: String = "salud", // "salud", "habitos", "bitacora", "agente"
    val isDarkMode: Boolean = false,
    val toastMessage: String? = null,
    val pendingOutboxCount: Int = 0,
    val showCheckupReminderPopup: Boolean = false
)

class EirMainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = EirHealthRepository.getInstance(application)

    private val _uiState = MutableStateFlow(EirMainUiState())
    val uiState: StateFlow<EirMainUiState> = _uiState.asStateFlow()

    init {
        // Schedule default daily WorkManager background reminder at 08:00 AM
        CheckupReminderWorker.scheduleDailyReminder(application)

        viewModelScope.launch {
            repository.pendingOutboxCountFlow.collect { count ->
                _uiState.update { it.copy(pendingOutboxCount = count) }
            }
        }

        // Auto-emerge floating checkup reminder popup after 1.5s if not yet completed today
        viewModelScope.launch {
            delay(1500)
            _uiState.update { it.copy(showCheckupReminderPopup = true) }
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

    fun snoozeReminder15Min() {
        CheckupReminderWorker.scheduleTestReminderIn15Min(getApplication())
        val msg = getApplication<Application>().getString(R.string.reminder_snoozed_toast)
        _uiState.update {
            it.copy(
                showCheckupReminderPopup = false,
                toastMessage = msg
            )
        }
    }

    fun dismissReminderPopup() {
        _uiState.update { it.copy(showCheckupReminderPopup = false) }
    }
}
