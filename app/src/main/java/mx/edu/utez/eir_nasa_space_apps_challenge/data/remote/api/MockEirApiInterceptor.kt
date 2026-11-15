package mx.edu.utez.eir_nasa_space_apps_challenge.data.remote.api

import com.google.gson.Gson
import mx.edu.utez.eir_nasa_space_apps_challenge.data.remote.dtos.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import java.time.Instant
import java.time.temporal.ChronoUnit

class MockEirApiInterceptor : Interceptor {

    private val gson = Gson()
    private val processedRecordIds = mutableSetOf<String>()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath

        try {
            val networkResponse = chain.proceed(request)
            if (networkResponse.isSuccessful) return networkResponse
        } catch (_: Exception) {
            // Fallback to local OpenAPI mock engine
        }

        val jsonContentType = "application/json; charset=utf-8".toMediaType()

        return when {
            path.contains("/api/v1/users/me/bootstrap") -> {
                val now = Instant.now()

                // Synthetic 7-day health history from backend contract
                val mockHealthRecords = listOf(
                    StoredHealthRecordDto(
                        recordId = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
                        recordedAt = now.toString(),
                        payload = HealthPayloadDto(
                            heartRateBpm = 68,
                            glucoseMgDl = 92.0,
                            temperatureC = 36.7,
                            sleepHours = 7.5,
                            systolicBpMmhg = 118,
                            diastolicBpMmhg = 76
                        ),
                        receivedAt = now.toString()
                    ),
                    StoredHealthRecordDto(
                        recordId = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12",
                        recordedAt = now.minus(1, ChronoUnit.DAYS).toString(),
                        payload = HealthPayloadDto(
                            heartRateBpm = 72,
                            glucoseMgDl = 94.0,
                            temperatureC = 36.6,
                            sleepHours = 7.2,
                            systolicBpMmhg = 119,
                            diastolicBpMmhg = 78
                        ),
                        receivedAt = now.minus(1, ChronoUnit.DAYS).toString()
                    ),
                    StoredHealthRecordDto(
                        recordId = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13",
                        recordedAt = now.minus(2, ChronoUnit.DAYS).toString(),
                        payload = HealthPayloadDto(
                            heartRateBpm = 70,
                            glucoseMgDl = 91.0,
                            temperatureC = 36.8,
                            sleepHours = 7.8,
                            systolicBpMmhg = 120,
                            diastolicBpMmhg = 77
                        ),
                        receivedAt = now.minus(2, ChronoUnit.DAYS).toString()
                    )
                )

                // Synthetic 7-day wellbeing checkins from backend contract
                val mockWellbeingCheckins = listOf(
                    StoredWellbeingCheckinDto(
                        recordId = "b1ffcd88-8b0a-3ef7-aa5c-5aa8ac270b22",
                        recordedAt = now.toString(),
                        payload = WellbeingPayloadDto(
                            mood = 4,
                            connection = 4,
                            energy = 4,
                            purpose = 5,
                            sleep = 4,
                            note = "Sol 142: Dormí mucho mejor después de bajar la temperatura de la cabina."
                        ),
                        receivedAt = now.toString()
                    ),
                    StoredWellbeingCheckinDto(
                        recordId = "b1ffcd88-8b0a-3ef7-aa5c-5aa8ac270b23",
                        recordedAt = now.minus(1, ChronoUnit.DAYS).toString(),
                        payload = WellbeingPayloadDto(
                            mood = 3,
                            connection = 3,
                            energy = 3,
                            purpose = 4,
                            sleep = 3,
                            note = "Sol 141: Vi una foto de mi familia antes de dormir. La respiración 4-4-4-4 me relajó."
                        ),
                        receivedAt = now.minus(1, ChronoUnit.DAYS).toString()
                    ),
                    StoredWellbeingCheckinDto(
                        recordId = "b1ffcd88-8b0a-3ef7-aa5c-5aa8ac270b24",
                        recordedAt = now.minus(2, ChronoUnit.DAYS).toString(),
                        payload = WellbeingPayloadDto(
                            mood = 5,
                            connection = 5,
                            energy = 5,
                            purpose = 5,
                            sleep = 5,
                            note = "Sol 140: Terminamos la calibración de paneles solares antes de tiempo."
                        ),
                        receivedAt = now.minus(2, ChronoUnit.DAYS).toString()
                    )
                )

                val mockBootstrap = BootstrapPageDto(
                    profile = AstronautProfileDto(
                        userId = "usr_ares_142",
                        astronautId = "ASTRO-ARES-IV-07",
                        username = "commander_eir",
                        displayName = "Comandante Eir",
                        missionId = "ARES-IV-MARS",
                        profileVersion = 1
                    ),
                    healthRecords = mockHealthRecords,
                    wellbeingCheckins = mockWellbeingCheckins
                )

                Response.Builder()
                    .code(200)
                    .message("OK")
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .body(gson.toJson(mockBootstrap).toResponseBody(jsonContentType))
                    .build()
            }

            path.contains("/api/v1/sync/records") -> {
                val buffer = okio.Buffer()
                request.body?.writeTo(buffer)
                val bodyString = buffer.readUtf8()

                val results = mutableListOf<SyncItemResultDto>()
                val syncRequest = try {
                    gson.fromJson(bodyString, SyncBatchRequestDto::class.java)
                } catch (_: Exception) { null }

                syncRequest?.records?.forEach { record ->
                    val isDuplicate = !processedRecordIds.add(record.recordId)
                    val status = if (isDuplicate) "duplicate" else "accepted"
                    results.add(SyncItemResultDto(recordId = record.recordId, status = status))
                }

                val responsePayload = SyncBatchResponseDto(
                    results = results,
                    serverTime = Instant.now().toString()
                )

                Response.Builder()
                    .code(200)
                    .message("OK")
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .body(gson.toJson(responsePayload).toResponseBody(jsonContentType))
                    .build()
            }

            path.contains("/api/v1/health") -> {
                Response.Builder()
                    .code(200)
                    .message("OK")
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .body(gson.toJson(HealthResponseDto("ok")).toResponseBody(jsonContentType))
                    .build()
            }

            else -> {
                Response.Builder()
                    .code(200)
                    .message("OK")
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .body("{}".toResponseBody(jsonContentType))
                    .build()
            }
        }
    }
}
