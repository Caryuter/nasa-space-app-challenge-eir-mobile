package mx.edu.utez.eir_nasa_space_apps_challenge.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.edu.utez.eir_nasa_space_apps_challenge.data.model.JournalEntry
import mx.edu.utez.eir_nasa_space_apps_challenge.data.repository.EirHealthRepository

data class BitacoraUiState(
    val moodLabel: String = "Tranquilo",
    val moodPoint: Pair<Float, Float> = Pair(74f, 64f),
    val journalInput: String = "",
    val journalEntries: List<JournalEntry> = emptyList(),
    val breathingActive: Boolean = false,
    val breathingPhase: Int = 0 // 0..3
)

class BitacoraViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = EirHealthRepository.getInstance(application)

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

    private val _uiState = MutableStateFlow(BitacoraUiState(journalEntries = initialJournal))
    val uiState: StateFlow<BitacoraUiState> = _uiState.asStateFlow()

    private var breathingJob: Job? = null

    init {
        viewModelScope.launch {
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
        }
    }
}
