package mx.edu.utez.eir_nasa_space_apps_challenge.data.remote.dtos

import com.google.gson.annotations.SerializedName

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
    @SerializedName("health_payload") val healthPayload: HealthPayloadDto? = null,
    @SerializedName("wellbeing_payload") val wellbeingPayload: WellbeingPayloadDto? = null
)

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
