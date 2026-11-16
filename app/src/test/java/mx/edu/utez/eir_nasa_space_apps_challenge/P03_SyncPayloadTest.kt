package mx.edu.utez.eir_nasa_space_apps_challenge

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import mx.edu.utez.eir_nasa_space_apps_challenge.data.remote.dtos.*
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class P03_SyncPayloadTest {

    private val gson: Gson = GsonBuilder().create()

    @Test
    fun syncRecord_healthRecordUsesUnifiedPayloadKey() {
        val recordId = UUID.randomUUID().toString()
        val recordedAt = "2026-11-15T14:30:00Z"
        val healthPayload = HealthPayloadDto(
            heartRateBpm = 68,
            glucoseMgDl = 92.0,
            temperatureC = 36.7,
            sleepHours = 7.5,
            systolicBpMmhg = 118,
            diastolicBpMmhg = 76
        )

        val record = SyncRecordDto.fromHealthPayload(recordId, recordedAt, healthPayload)
        val json = gson.toJson(record)

        assertTrue("JSON must contain 'payload' property", json.contains("\"payload\":"))
        assertFalse("JSON must NOT contain 'health_payload' property", json.contains("\"health_payload\""))
        assertFalse("JSON must NOT contain 'wellbeing_payload' property", json.contains("\"wellbeing_payload\""))
    }

    @Test
    fun syncRecord_wellbeingCheckinUsesUnifiedPayloadKey() {
        val recordId = UUID.randomUUID().toString()
        val recordedAt = "2026-11-15T14:30:00Z"
        val wellbeingPayload = WellbeingPayloadDto(
            mood = 4,
            connection = 4,
            energy = 4,
            purpose = 5,
            sleep = 4,
            note = "Sol 142 checkin"
        )

        val record = SyncRecordDto.fromWellbeingPayload(recordId, recordedAt, wellbeingPayload)
        val json = gson.toJson(record)

        assertTrue("JSON must contain 'payload' property", json.contains("\"payload\":"))
        assertFalse("JSON must NOT contain 'wellbeing_payload' property", json.contains("\"wellbeing_payload\""))
        assertFalse("JSON must NOT contain 'health_payload' property", json.contains("\"health_payload\""))
    }

    @Test
    fun syncRecord_omitsNullPropertiesFromPayload() {
        val recordId = UUID.randomUUID().toString()
        val recordedAt = "2026-11-15T14:30:00Z"
        // Only heartRateBpm provided; other fields null
        val healthPayload = HealthPayloadDto(
            heartRateBpm = 72,
            glucoseMgDl = null,
            temperatureC = null,
            sleepHours = null,
            systolicBpMmhg = null,
            diastolicBpMmhg = null
        )

        val record = SyncRecordDto.fromHealthPayload(recordId, recordedAt, healthPayload)
        val json = gson.toJson(record)

        assertTrue(json.contains("\"heart_rate_bpm\":72"))
        assertFalse("Null properties must not be serialized", json.contains("\"glucose_mg_dl\""))
        assertFalse("Null properties must not be serialized", json.contains("\"temperature_c\""))
    }

    @Test
    fun migration_safelyTransformsLegacyHealthPayload() {
        val uuid = UUID.randomUUID().toString()
        val recordedAt = "2026-11-14T10:00:00Z"
        val legacyJson = """
            {
                "record_id": "$uuid",
                "recorded_at": "$recordedAt",
                "schema_version": 1,
                "type": "health_record",
                "health_payload": {
                    "heart_rate_bpm": 80,
                    "systolic_bp_mmhg": 120
                }
            }
        """.trimIndent()

        val migrated = SyncRecordUtils.parseAndMigrateSyncRecord(legacyJson)

        assertEquals(uuid, migrated.recordId)
        assertEquals(recordedAt, migrated.recordedAt)
        assertEquals(1, migrated.schemaVersion)
        assertEquals("health_record", migrated.type)

        val migratedJson = gson.toJson(migrated)
        assertTrue("Migrated record must have 'payload'", migratedJson.contains("\"payload\":"))
        assertFalse("Migrated record must not have 'health_payload'", migratedJson.contains("\"health_payload\""))

        val validation = SyncRecordUtils.validateSyncRecord(migrated)
        assertTrue("Migrated record must pass validation", validation is ValidationResult.Valid)
    }

    @Test
    fun migration_safelyTransformsLegacyWellbeingPayload() {
        val uuid = UUID.randomUUID().toString()
        val recordedAt = "2026-11-14T11:00:00Z"
        val legacyJson = """
            {
                "record_id": "$uuid",
                "recorded_at": "$recordedAt",
                "schema_version": 1,
                "type": "wellbeing_checkin",
                "wellbeing_payload": {
                    "mood": 5,
                    "connection": 4,
                    "energy": 5,
                    "purpose": 5,
                    "sleep": 4,
                    "note": "Legacy format note"
                }
            }
        """.trimIndent()

        val migrated = SyncRecordUtils.parseAndMigrateSyncRecord(legacyJson)

        assertEquals(uuid, migrated.recordId)
        assertEquals(recordedAt, migrated.recordedAt)
        assertEquals("wellbeing_checkin", migrated.type)

        val migratedJson = gson.toJson(migrated)
        assertTrue(migratedJson.contains("\"payload\":"))
        assertFalse(migratedJson.contains("\"wellbeing_payload\""))

        val validation = SyncRecordUtils.validateSyncRecord(migrated)
        assertTrue(validation is ValidationResult.Valid)
    }

    @Test
    fun validation_catchesInvalidRecordId() {
        val invalidRecord = SyncRecordDto(
            recordId = "not-a-valid-uuid",
            recordedAt = "2026-11-15T12:00:00Z",
            schemaVersion = 1,
            type = "health_record",
            payload = gson.toJsonTree(HealthPayloadDto(heartRateBpm = 70))
        )

        val validation = SyncRecordUtils.validateSyncRecord(invalidRecord)
        assertTrue(validation is ValidationResult.Invalid)
        assertTrue((validation as ValidationResult.Invalid).reason.contains("not a valid UUID"))
    }

    @Test
    fun validation_catchesMissingRequiredWellbeingField() {
        val obj = com.google.gson.JsonObject().apply {
            addProperty("mood", 4)
            // missing connection, energy, purpose, sleep
        }

        val invalidRecord = SyncRecordDto(
            recordId = UUID.randomUUID().toString(),
            recordedAt = "2026-11-15T12:00:00Z",
            schemaVersion = 1,
            type = "wellbeing_checkin",
            payload = obj
        )

        val validation = SyncRecordUtils.validateSyncRecord(invalidRecord)
        assertTrue(validation is ValidationResult.Invalid)
    }
}
