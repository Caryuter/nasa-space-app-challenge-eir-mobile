package mx.edu.utez.eir_nasa_space_apps_challenge.data.sync

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mx.edu.utez.eir_nasa_space_apps_challenge.data.local.db.EirDatabase
import mx.edu.utez.eir_nasa_space_apps_challenge.data.remote.api.EirApiService
import mx.edu.utez.eir_nasa_space_apps_challenge.data.remote.dtos.SyncBatchRequestDto
import mx.edu.utez.eir_nasa_space_apps_challenge.data.remote.dtos.SyncRecordDto

class OutboxSyncManager(
    private val db: EirDatabase,
    private val apiService: EirApiService,
    private val deviceId: String = "DEVICE-ARES-IV-01"
) {
    private val gson = Gson()

    suspend fun syncPendingRecords(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val pendingEntries = db.outboxDao().getPendingOutboxEntries()
            if (pendingEntries.isEmpty()) {
                return@withContext Result.success(0)
            }

            val recordsToPush = pendingEntries.mapNotNull { entry ->
                try {
                    gson.fromJson(entry.payloadJson, SyncRecordDto::class.java)
                } catch (e: Exception) {
                    null
                }
            }

            if (recordsToPush.isEmpty()) {
                return@withContext Result.success(0)
            }

            val request = SyncBatchRequestDto(
                deviceId = deviceId,
                records = recordsToPush
            )

            val response = apiService.pushRecords(request)
            if (response.isSuccessful && response.body() != null) {
                val batchResponse = response.body()!!
                var ackCount = 0

                batchResponse.results.forEach { result ->
                    when (result.status) {
                        "accepted", "duplicate" -> {
                            db.outboxDao().updateStatus(result.recordId, "ACKNOWLEDGED")
                            ackCount++
                        }
                        "rejected", "conflict" -> {
                            db.outboxDao().markFailure(result.recordId, "FAILED", result.error?.message ?: "Rejected")
                        }
                        "retryable" -> {
                            db.outboxDao().markFailure(result.recordId, "RETRYABLE", result.error?.message ?: "Retryable")
                        }
                    }
                }

                // Clean up acknowledged items from outbox table
                db.outboxDao().deleteAcknowledged()
                Result.success(ackCount)
            } else {
                Result.failure(Exception("HTTP Error ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
