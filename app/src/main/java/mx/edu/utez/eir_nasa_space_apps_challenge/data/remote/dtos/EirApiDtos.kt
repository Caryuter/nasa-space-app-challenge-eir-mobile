package mx.edu.utez.eir_nasa_space_apps_challenge.data.remote.dtos

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import java.util.UUID

data class ApiErrorDto(
    @SerializedName("code") val code: String,
    @SerializedName("message") val message: String,
    @SerializedName("retryable") val retryable: Boolean = false
)

data class HealthResponseDto(
    @SerializedName("status") val status: String = "ok"
)

data class LoginRequestDto(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String,
    @SerializedName("device_id") val deviceId: String
)

data class RefreshRequestDto(
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("device_id") val deviceId: String
)

data class SessionTokensDto(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("token_type") val tokenType: String = "Bearer",
    @SerializedName("expires_in") val expiresIn: Int
)

data class AstronautProfileDto(
    @SerializedName("user_id") val userId: String,
    @SerializedName("astronaut_id") val astronautId: String,
    @SerializedName("username") val username: String,
    @SerializedName("display_name") val displayName: String,
    @SerializedName("mission_id") val missionId: String,
    @SerializedName("profile_version") val profileVersion: Int = 1
)

data class LoginResponseDto(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("token_type") val tokenType: String = "Bearer",
    @SerializedName("expires_in") val expiresIn: Int,
    @SerializedName("user") val user: AstronautProfileDto,
    @SerializedName("online_authenticated_at") val onlineAuthenticatedAt: String
)

data class HealthPayloadDto(
    @SerializedName("heart_rate_bpm") val heartRateBpm: Int? = null,
    @SerializedName("glucose_mg_dl") val glucoseMgDl: Double? = null,
    @SerializedName("temperature_c") val temperatureC: Double? = null,
    @SerializedName("sleep_hours") val sleepHours: Double? = null,
    @SerializedName("systolic_bp_mmhg") val systolicBpMmhg: Int? = null,
    @SerializedName("diastolic_bp_mmhg") val diastolicBpMmhg: Int? = null
)

data class WellbeingPayloadDto(
    @SerializedName("mood") val mood: Int, // 1..5
    @SerializedName("connection") val connection: Int, // 1..5
    @SerializedName("energy") val energy: Int, // 1..5
    @SerializedName("purpose") val purpose: Int, // 1..5
    @SerializedName("sleep") val sleep: Int, // 1..5
    @SerializedName("note") val note: String? = null
)

data class SyncRecordDto(
    @SerializedName("record_id") val recordId: String,
    @SerializedName("recorded_at") val recordedAt: String,
    @SerializedName("schema_version") val schemaVersion: Int = 1,
    @SerializedName("type") val type: String, // "health_record" or "wellbeing_checkin"
    @SerializedName("payload") val payload: JsonElement
) {
    companion object {
        private val gson: Gson = GsonBuilder().create()

        fun fromHealthPayload(
            recordId: String,
            recordedAt: String,
            payload: HealthPayloadDto
        ): SyncRecordDto {
            return SyncRecordDto(
                recordId = recordId,
                recordedAt = recordedAt,
                schemaVersion = 1,
                type = "health_record",
                payload = gson.toJsonTree(payload)
            )
        }

        fun fromWellbeingPayload(
            recordId: String,
            recordedAt: String,
            payload: WellbeingPayloadDto
        ): SyncRecordDto {
            return SyncRecordDto(
                recordId = recordId,
                recordedAt = recordedAt,
                schemaVersion = 1,
                type = "wellbeing_checkin",
                payload = gson.toJsonTree(payload)
            )
        }
    }
}

data class SyncBatchRequestDto(
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("records") val records: List<SyncRecordDto>
)

data class SyncItemResultDto(
    @SerializedName("record_id") val recordId: String,
    @SerializedName("status") val status: String, // "accepted", "duplicate", "rejected", "conflict", "retryable"
    @SerializedName("error") val error: ApiErrorDto? = null
)

data class SyncBatchResponseDto(
    @SerializedName("results") val results: List<SyncItemResultDto>,
    @SerializedName("server_time") val serverTime: String
)

data class StoredHealthRecordDto(
    @SerializedName("record_id") val recordId: String,
    @SerializedName("recorded_at") val recordedAt: String,
    @SerializedName("schema_version") val schemaVersion: Int = 1,
    @SerializedName("type") val type: String = "health_record",
    @SerializedName("payload") val payload: HealthPayloadDto,
    @SerializedName("received_at") val receivedAt: String
)

data class StoredWellbeingCheckinDto(
    @SerializedName("record_id") val recordId: String,
    @SerializedName("recorded_at") val recordedAt: String,
    @SerializedName("schema_version") val schemaVersion: Int = 1,
    @SerializedName("type") val type: String = "wellbeing_checkin",
    @SerializedName("payload") val payload: WellbeingPayloadDto,
    @SerializedName("received_at") val receivedAt: String
)

data class HealthPageDto(
    @SerializedName("items") val items: List<StoredHealthRecordDto>,
    @SerializedName("next_cursor") val nextCursor: String? = null
)

data class WellbeingPageDto(
    @SerializedName("items") val items: List<StoredWellbeingCheckinDto>,
    @SerializedName("next_cursor") val nextCursor: String? = null
)

data class BootstrapPageDto(
    @SerializedName("profile") val profile: AstronautProfileDto,
    @SerializedName("health_records") val healthRecords: List<StoredHealthRecordDto>,
    @SerializedName("wellbeing_checkins") val wellbeingCheckins: List<StoredWellbeingCheckinDto>,
    @SerializedName("next_cursor") val nextCursor: String? = null
)

data class RiskAlertDto(
    @SerializedName("alert_id") val alertId: String,
    @SerializedName("severity") val severity: String, // "info", "warning", "critical"
    @SerializedName("rule_id") val ruleId: String,
    @SerializedName("explanation") val explanation: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("evidence_record_ids") val evidenceRecordIds: List<String> = emptyList()
)

data class RiskAlertsResponseDto(
    @SerializedName("items") val items: List<RiskAlertDto>
)

sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val reason: String) : ValidationResult()
}

object SyncRecordUtils {
    private val gson = GsonBuilder().create()

    /**
     * Parses a stored JSON payload and safely migrates legacy schemas
     * (which used "health_payload" or "wellbeing_payload") into the unified "payload" format.
     */
    fun parseAndMigrateSyncRecord(jsonString: String): SyncRecordDto {
        val jsonObj = JsonParser.parseString(jsonString).asJsonObject
        val recordId = jsonObj.get("record_id").asString
        val recordedAt = jsonObj.get("recorded_at").asString
        val schemaVersion = jsonObj.get("schema_version")?.asInt ?: 1
        val type = jsonObj.get("type").asString

        val payloadElement = when {
            jsonObj.has("payload") && !jsonObj.get("payload").isJsonNull -> {
                jsonObj.get("payload")
            }
            jsonObj.has("health_payload") && !jsonObj.get("health_payload").isJsonNull -> {
                jsonObj.get("health_payload")
            }
            jsonObj.has("wellbeing_payload") && !jsonObj.get("wellbeing_payload").isJsonNull -> {
                jsonObj.get("wellbeing_payload")
            }
            else -> throw IllegalArgumentException("Missing payload in SyncRecord: $jsonString")
        }

        // Clean null properties from payloadElement if it's a JsonObject
        val cleanPayload = cleanNulls(payloadElement)

        return SyncRecordDto(
            recordId = recordId,
            recordedAt = recordedAt,
            schemaVersion = schemaVersion,
            type = type,
            payload = cleanPayload
        )
    }

    private fun cleanNulls(element: JsonElement): JsonElement {
        if (!element.isJsonObject) return element
        val obj = element.asJsonObject
        val cleaned = JsonObject()
        for (entry in obj.entrySet()) {
            if (!entry.value.isJsonNull) {
                cleaned.add(entry.key, entry.value)
            }
        }
        return cleaned
    }

    /**
     * Validates that the record conforms strictly to the OpenAPI specification.
     */
    fun validateSyncRecord(record: SyncRecordDto): ValidationResult {
        // Validate record_id is a valid UUID
        try {
            UUID.fromString(record.recordId)
        } catch (_: Exception) {
            return ValidationResult.Invalid("record_id is not a valid UUID: ${record.recordId}")
        }

        if (record.schemaVersion != 1) {
            return ValidationResult.Invalid("schema_version must be 1, found: ${record.schemaVersion}")
        }

        if (record.recordedAt.isBlank()) {
            return ValidationResult.Invalid("recorded_at cannot be blank")
        }

        when (record.type) {
            "health_record" -> {
                if (!record.payload.isJsonObject) {
                    return ValidationResult.Invalid("health_record payload must be an object")
                }
                val obj = record.payload.asJsonObject
                if (obj.size() == 0) {
                    return ValidationResult.Invalid("health_record payload requires at least one property (minProperties: 1)")
                }
                // Check no unexpected fields (additionalProperties: false)
                val allowedFields = setOf(
                    "heart_rate_bpm", "glucose_mg_dl", "temperature_c",
                    "sleep_hours", "systolic_bp_mmhg", "diastolic_bp_mmhg"
                )
                for (key in obj.keySet()) {
                    if (key !in allowedFields) {
                        return ValidationResult.Invalid("Unknown property in health_record payload: $key")
                    }
                }
            }
            "wellbeing_checkin" -> {
                if (!record.payload.isJsonObject) {
                    return ValidationResult.Invalid("wellbeing_checkin payload must be an object")
                }
                val obj = record.payload.asJsonObject
                val requiredFields = listOf("mood", "connection", "energy", "purpose", "sleep")
                for (req in requiredFields) {
                    if (!obj.has(req) || obj.get(req).isJsonNull) {
                        return ValidationResult.Invalid("Missing required property in wellbeing_checkin: $req")
                    }
                    val value = obj.get(req).asInt
                    if (value !in 1..5) {
                        return ValidationResult.Invalid("Field $req out of bounds 1..5: $value")
                    }
                }
                val allowedFields = setOf("mood", "connection", "energy", "purpose", "sleep", "note")
                for (key in obj.keySet()) {
                    if (key !in allowedFields) {
                        return ValidationResult.Invalid("Unknown property in wellbeing_checkin payload: $key")
                    }
                }
            }
            else -> {
                return ValidationResult.Invalid("Unsupported record type: ${record.type}")
            }
        }

        return ValidationResult.Valid
    }
}
