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
import mx.edu.utez.eir_nasa_space_apps_challenge.data.model.Exercise
import mx.edu.utez.eir_nasa_space_apps_challenge.data.model.Meal

data class HabitosUiState(
    val habitFilter: String = "all", // "all", "food", "exercise"
    val energyIntake: Int = 1750,
    val energyGoal: Int = 2400,
    val waterGlasses: Int = 4,
    val waterGoal: Int = 6,
    val dailyMeals: List<Meal> = emptyList(),
    val treadmillTimerSec: Int = 215,
    val isTreadmillRunning: Boolean = false,
    val exercises: List<Exercise> = emptyList()
)

class HabitosViewModel(application: Application) : AndroidViewModel(application) {

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

    private val _uiState = MutableStateFlow(
        HabitosUiState(
            dailyMeals = initialMeals,
            exercises = initialExercises
        )
    )
    val uiState: StateFlow<HabitosUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    fun setHabitFilter(filter: String) {
        _uiState.update { it.copy(habitFilter = filter) }
    }

    fun addWaterGlass() {
        _uiState.update { current ->
            val next = (current.waterGlasses + 1).coerceAtMost(current.waterGoal)
            current.copy(waterGlasses = next)
        }
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
}
