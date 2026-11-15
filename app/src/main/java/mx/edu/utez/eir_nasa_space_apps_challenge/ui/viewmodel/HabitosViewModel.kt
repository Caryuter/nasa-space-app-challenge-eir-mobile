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
import mx.edu.utez.eir_nasa_space_apps_challenge.R
import mx.edu.utez.eir_nasa_space_apps_challenge.data.model.Exercise
import mx.edu.utez.eir_nasa_space_apps_challenge.data.model.Meal
import mx.edu.utez.eir_nasa_space_apps_challenge.data.model.MealFoodItem

data class HabitosUiState(
    val habitFilter: String = "all", // "all", "food", "exercise"
    val energyIntake: Int = 1750,
    val energyGoal: Int = 2400,
    val waterGlasses: Int = 4,
    val waterGoal: Int = 8,
    val dailyMeals: List<Meal> = emptyList(),
    // Meal detail
    val selectedMealId: Long? = null,
    // Workout / Routine timer
    val workoutTimerSec: Int = 2,
    val isWorkoutRunning: Boolean = true,
    val routineName: String = "Contramedidas Espaciales",
    val exercises: List<Exercise> = emptyList(),
    // Exercise detail
    val selectedExerciseId: Long? = null,
    // Rest countdown between sets
    val restTimerSec: Int = 45,
    val activeRestExerciseId: Long? = null,
    val isRestTimerRunning: Boolean = false,
    val workoutCompleted: Boolean = false
) {
    // Legacy support property if anything refers to treadmillTimerSec
    val treadmillTimerSec: Int get() = workoutTimerSec
    val isTreadmillRunning: Boolean get() = isWorkoutRunning
}

class HabitosViewModel(application: Application) : AndroidViewModel(application) {

    private val initialMeals = listOf(
        Meal(
            id = 1L,
            name = "Desayuno energético + Vitamina D",
            kcal = 550,
            done = true,
            mealType = "Desayuno",
            time = "07:30",
            proteinGrams = 28,
            carbsGrams = 68,
            fatGrams = 16,
            fiberGrams = 9,
            imageRes = R.drawable.meal_breakfast,
            whySpace = "Protege tus huesos de la pérdida acelerada de calcio y te da energía constante para tu primer turno de operaciones.",
            prepInfo = "Inyecta 150 ml de agua caliente a 70°C en la boquilla A. Deja reposar 4 minutos antes de abrir el sello hermético.",
            items = listOf(
                MealFoodItem("Avena liofilizada con semillas de chía", "1 paquete (60g)", 240, "🥣"),
                MealFoodItem("Frutas del bosque y plátano deshidratado", "1 porción (45g)", 140, "🫐"),
                MealFoodItem("Bebida de almendras fortificada con calcio", "1 sobre (200ml)", 90, "🥛"),
                MealFoodItem("Cápsula de Vitamina D3 (1000 UI) + Magnesio", "1 sachet sellado", 0, "💊"),
                MealFoodItem("Nueces y miel de abeja", "1 blíster (20g)", 80, "🍯")
            )
        ),
        Meal(
            id = 2L,
            name = "Comida principal rica en proteína",
            kcal = 700,
            done = true,
            mealType = "Almuerzo",
            time = "13:00",
            proteinGrams = 46,
            carbsGrams = 72,
            fatGrams = 22,
            fiberGrams = 11,
            imageRes = R.drawable.meal_lunch,
            whySpace = "Repara los tejidos musculares sometidos al arnés de ejercicio ARED y previene el catabolismo proteico en ingravidez.",
            prepInfo = "Coloca el contenedor en el calentador térmico del módulo durante 12 minutos a 80°C. Mantener sellado hasta consumir.",
            items = listOf(
                MealFoodItem("Filete de salmón salvaje en pouch térmico", "1 pouch (150g)", 290, "🐟"),
                MealFoodItem("Quinoa orgánica con vegetales al vapor", "1 ración (120g)", 210, "🥗"),
                MealFoodItem("Brócoli liofilizado alto en calcio", "1 bolsa (50g)", 50, "🥦"),
                MealFoodItem("Aguacate en crema estabilizado", "1 tubito (40g)", 95, "🥑"),
                MealFoodItem("Infusión isotónica de electrolitos", "250 ml", 55, "🧃")
            )
        ),
        Meal(
            id = 3L,
            name = "Colación de frutos secos y calcio",
            kcal = 500,
            done = true,
            mealType = "Colación",
            time = "17:00",
            proteinGrams = 18,
            carbsGrams = 44,
            fatGrams = 26,
            fiberGrams = 7,
            imageRes = R.drawable.meal_snack,
            whySpace = "Grasas saludables Omega-3 que reducen la inflamación ocular y el estrés oxidativo por radiación cósmica.",
            prepInfo = "Listo para consumo inmediato. Abre el cierre hermético Zip-Lock al vacío en la estación de trabajo.",
            items = listOf(
                MealFoodItem("Mix de almendras tostadas y nueces de nogal", "1 paquete (50g)", 270, "🥜"),
                MealFoodItem("Arándanos rojos y fresas deshidratadas", "1 porción (30g)", 95, "🍓"),
                MealFoodItem("Barra de proteína con cacao 70% amargo", "1 barra (35g)", 135, "🍫")
            )
        ),
        Meal(
            id = 4L,
            name = "Cena ligera + Sobres de sales minerales",
            kcal = 650,
            done = false,
            mealType = "Cena",
            time = "20:30",
            proteinGrams = 30,
            carbsGrams = 62,
            fatGrams = 20,
            fiberGrams = 8,
            imageRes = R.drawable.meal_dinner,
            whySpace = "Control estricto de sodio para prevenir acumulación de fluidos en la cabeza y facilitar el descanso nocturno profundo.",
            prepInfo = "Rehidratar el pouch de caldo con 200 ml de agua caliente a 65°C y agitar suavemente durante 3 minutos.",
            items = listOf(
                MealFoodItem("Caldo reconfortante de verduras y cúrcuma", "1 tazón térmico (250ml)", 120, "🍲"),
                MealFoodItem("Pechuga de pavo deshebrada con hierbas", "1 ración (120g)", 220, "🍗"),
                MealFoodItem("Puré de camote dulce y arroz integral", "1 porción (100g)", 190, "🍠"),
                MealFoodItem("Sobre de electrolitos y citrato de potasio", "1 sachet en 200ml", 20, "💧"),
                MealFoodItem("Té de manzanilla y lavanda relajante", "1 infusión", 0, "🍵")
            )
        )
    )

    private val initialExercises = listOf(
        Exercise(
            id = 1L,
            name = "Sentadillas con ligas de resistencia",
            setsReps = "4x 15 a 20",
            loadKg = "35",
            restSec = 45,
            why = "Mantiene fuertes los huesos de cadera y piernas sin gravedad",
            duration = "20 min",
            done = true,
            imageRes = R.drawable.ex_space_squats,
            thumbRes = R.drawable.ex_space_squats,
            category = "Piernas y Cadera",
            instructions = "Fija las bandas elásticas y arnés ARED a la plataforma. Desciende flexionando rodillas a 90° controlando la tracción elástica para comprimir los huesos largos del tren inferior."
        ),
        Exercise(
            id = 2L,
            name = "Caminadora espacial con arnés",
            setsReps = "Intervalos 15 min",
            loadKg = "65% masa",
            restSec = 60,
            why = "Cuida tu corazón y evita que pierdas condición física",
            duration = "15 min",
            done = false,
            imageRes = R.drawable.ex_space_treadmill,
            thumbRes = R.drawable.ex_space_treadmill,
            category = "Cardio y Resistencia",
            instructions = "Ajusta las correas del arnés T2 COLBERT para simular el 65% de tu peso corporal. Mantén una zancada fluida y amortigua cada pisada sobre el tapiz rodante aislado."
        ),
        Exercise(
            id = 3L,
            name = "Remo de espalda y abdomen",
            setsReps = "3x 12 a 15",
            loadKg = "25",
            restSec = 45,
            why = "Evita dolor de espalda baja por flotar todo el día",
            duration = "10 min",
            done = false,
            imageRes = R.drawable.ex_space_rower,
            thumbRes = R.drawable.ex_space_rower,
            category = "Espalda y Core",
            instructions = "Fija los talones en los reposapiés magnéticos. Tracciona el manillar hacia el esternón activando dorsales y transverso abdominal para revertir la elongación de la columna vertebral en microgravedad."
        )
    )

    private val _uiState = MutableStateFlow(
        HabitosUiState(
            dailyMeals = initialMeals,
            exercises = initialExercises,
            isWorkoutRunning = true
        )
    )
    val uiState: StateFlow<HabitosUiState> = _uiState.asStateFlow()

    private var workoutTimerJob: Job? = null
    private var restTimerJob: Job? = null

    init {
        startWorkoutTimer()
    }

    fun setHabitFilter(filter: String) {
        _uiState.update { it.copy(habitFilter = filter) }
    }

    fun addWaterGlass() {
        _uiState.update { current ->
            val next = (current.waterGlasses + 1).coerceAtMost(current.waterGoal)
            current.copy(waterGlasses = next)
        }
    }

    fun setWaterGlasses(count: Int) {
        _uiState.update { current ->
            val next = count.coerceIn(0, current.waterGoal)
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

    // WORKOUT STOPWATCH TIMER (Smart Fit header)
    private fun startWorkoutTimer() {
        workoutTimerJob?.cancel()
        workoutTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _uiState.update { current ->
                    if (current.isWorkoutRunning) {
                        current.copy(workoutTimerSec = current.workoutTimerSec + 1)
                    } else current
                }
            }
        }
    }

    fun toggleWorkoutTimer() {
        _uiState.update { current ->
            val nextRunning = !current.isWorkoutRunning
            current.copy(isWorkoutRunning = nextRunning)
        }
    }

    fun resetWorkoutTimer() {
        _uiState.update { it.copy(workoutTimerSec = 0, isWorkoutRunning = false) }
    }

    fun finishWorkout() {
        _uiState.update { current ->
            current.copy(
                isWorkoutRunning = false,
                workoutCompleted = true,
                exercises = current.exercises.map { it.copy(done = true) }
            )
        }
    }

    // MEAL SELECTION (Nutrition / Meal Detail)
    fun selectMeal(mealId: Long?) {
        _uiState.update { it.copy(selectedMealId = mealId) }
    }

    // EXERCISE SELECTION (Smart Fit Exercise Detail)
    fun selectExercise(exerciseId: Long?) {
        _uiState.update { it.copy(selectedExerciseId = exerciseId) }
    }

    fun toggleExercise(exId: Long) {
        _uiState.update { current ->
            current.copy(
                exercises = current.exercises.map { if (it.id == exId) it.copy(done = !it.done) else it }
            )
        }
    }

    fun updateExerciseParameters(exId: Long, setsReps: String, loadKg: String) {
        _uiState.update { current ->
            current.copy(
                exercises = current.exercises.map {
                    if (it.id == exId) it.copy(setsReps = setsReps, loadKg = loadKg) else it
                }
            )
        }
    }

    // REST COUNTDOWN TIMER (Smart Fit "00:45 (Descanso entre series) ▶")
    fun toggleRestTimer(exerciseId: Long) {
        val current = _uiState.value
        if (current.isRestTimerRunning && current.activeRestExerciseId == exerciseId) {
            // Pause/cancel current rest timer
            restTimerJob?.cancel()
            _uiState.update { it.copy(isRestTimerRunning = false, activeRestExerciseId = null, restTimerSec = 45) }
        } else {
            // Start rest timer for this exercise
            restTimerJob?.cancel()
            val targetExercise = current.exercises.find { it.id == exerciseId }
            val initialRestSec = targetExercise?.restSec ?: 45
            _uiState.update {
                it.copy(
                    isRestTimerRunning = true,
                    activeRestExerciseId = exerciseId,
                    restTimerSec = initialRestSec
                )
            }
            restTimerJob = viewModelScope.launch {
                while (_uiState.value.restTimerSec > 0) {
                    delay(1000)
                    _uiState.update {
                        if (it.restTimerSec > 1) {
                            it.copy(restTimerSec = it.restTimerSec - 1)
                        } else {
                            it.copy(restTimerSec = 0, isRestTimerRunning = false, activeRestExerciseId = null)
                        }
                    }
                }
            }
        }
    }

    // Legacy methods if still called anywhere
    fun toggleTreadmill() = toggleWorkoutTimer()
    fun resetTreadmill() = resetWorkoutTimer()
}
