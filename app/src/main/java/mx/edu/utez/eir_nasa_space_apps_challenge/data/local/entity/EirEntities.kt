package mx.edu.utez.eir_nasa_space_apps_challenge.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "astronaut_profile")
data class AstronautProfileEntity(
    @PrimaryKey val userId: String,
    val astronautId: String,
    val username: String,
    val displayName: String,
    val missionId: String,
    val profileVersion: Int
)

@Entity(tableName = "health_records")
data class HealthRecordEntity(
    @PrimaryKey val recordId: String,
    val heartRateBpm: Int?,
    val glucoseMgDl: Double?,
    val temperatureC: Double?,
    val sleepHours: Double?,
    val systolicBpMmhg: Int?,
    val diastolicBpMmhg: Int?,
    val recordedAt: String,
    val userId: String = ""
)

@Entity(tableName = "wellbeing_checkins")
data class WellbeingCheckinEntity(
    @PrimaryKey val recordId: String,
    val mood: Int,
    val connection: Int,
    val energy: Int,
    val purpose: Int,
    val sleep: Int,
    val note: String?,
    val recordedAt: String,
    val userId: String = ""
)

@Entity(tableName = "outbox")
data class OutboxEntity(
    @PrimaryKey val recordId: String,
    val recordType: String, // "health_record" or "wellbeing_checkin"
    val payloadJson: String,
    val status: String, // "PENDING", "IN_PROGRESS", "ACKNOWLEDGED", "FAILED", "RETRYABLE", "REJECTED", "CONFLICT"
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val lastError: String? = null,
    val userId: String = ""
)
