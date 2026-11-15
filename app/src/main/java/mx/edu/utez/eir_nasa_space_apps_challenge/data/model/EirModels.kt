package mx.edu.utez.eir_nasa_space_apps_challenge.data.model

import androidx.compose.ui.graphics.Color

data class HealthMetric(
    val id: String,
    val title: String,
    val value: String,
    val unit: String,
    val humanLabel: String,
    val updatedAt: String,
    val history: List<Float>,
    val meaning: String,
    val action: String,
    val color: Color
)

data class BodyZoneInfo(
    val id: String,
    val title: String,
    val summary: String,
    val vsWeek: String,
    val trend: List<Float>,
    val color: Color,
    val whySpaceFeelsWeird: String,
    val quickChips: List<String>
)

data class Discomfort(
    val id: Long,
    val zoneId: String,
    val label: String,
    val time: String
)

data class MealFoodItem(
    val name: String,
    val portion: String,
    val kcal: Int,
    val icon: String = "🥣"
)

data class Meal(
    val id: Long,
    val name: String,
    val kcal: Int,
    val done: Boolean,
    val mealType: String = "Comida",
    val time: String = "12:00",
    val proteinGrams: Int = 30,
    val carbsGrams: Int = 50,
    val fatGrams: Int = 15,
    val fiberGrams: Int = 6,
    val imageRes: Int? = null,
    val whySpace: String = "Protege la densidad ósea y previene la pérdida de masa muscular en microgravedad.",
    val prepInfo: String = "Rehidratar con 180 ml de agua a 70°C durante 5 minutos en el dispensador de la estación.",
    val items: List<MealFoodItem> = emptyList()
)

data class Exercise(
    val id: Long,
    val name: String,
    val why: String = "",
    val duration: String = "15 min",
    val done: Boolean = false,
    val setsReps: String = "3x 15 a 20",
    val loadKg: String = "0",
    val restSec: Int = 45,
    val imageRes: Int? = null,
    val thumbRes: Int? = null,
    val category: String = "Piernas",
    val instructions: String = "Mantén la postura alineada, ejecuta el rango completo de movimiento y mantén la tensión en el arnés axial para contrarrestar la microgravedad."
)

data class JournalEntry(
    val id: Long,
    val sol: String,
    val mood: String,
    val x: Float, // 0..100
    val y: Float, // 0..100
    val text: String
)

data class ChatMessage(
    val id: Long,
    val sender: String, // "user" or "eir"
    val text: String,
    val canSimplify: Boolean = true
)

data class SosStep(
    val instruction: String,
    val why: String,
    val kitAction: String,
    val imageRes: Int? = null
)

data class SosScenario(
    val id: String,
    val title: String,
    val tag: String,
    val kitItem: String,
    val steps: List<SosStep>
)

data class FaceScanFindings(
    val fluidShift: String,
    val eyeFatigue: String,
    val skinTone: String
)

data class VoiceScanFindings(
    val breathingPace: String,
    val vocalFatigue: String,
    val nasalCongestion: String
)

data class VitalsDraft(
    val heartRateInput: String = "",
    val sysInput: String = "",
    val tempInput: String = "",
    val sleepHoursInput: String = ""
) {
    constructor(heartRate: Int, sys: Int, temp: Float, sleepHours: Float) : this(
        heartRateInput = heartRate.toString(),
        sysInput = sys.toString(),
        tempInput = temp.toString(),
        sleepHoursInput = sleepHours.toString()
    )

    val heartRate: Int get() = heartRateInput.toIntOrNull() ?: 68
    val sys: Int get() = sysInput.toIntOrNull() ?: 118
    val temp: Float get() = tempInput.toFloatOrNull() ?: 36.7f
    val sleepHours: Float get() = sleepHoursInput.toFloatOrNull() ?: 7.5f
}
