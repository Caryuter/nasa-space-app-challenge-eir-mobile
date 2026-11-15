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

data class Meal(
    val id: Long,
    val name: String,
    val kcal: Int,
    val done: Boolean
)

data class Exercise(
    val id: Long,
    val name: String,
    val why: String,
    val duration: String,
    val done: Boolean
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
    val kitAction: String
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
    val heartRate: Int = 68,
    val sys: Int = 118,
    val temp: Float = 36.7f,
    val sleepHours: Float = 7.5f
)
