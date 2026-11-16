package mx.edu.utez.eir_nasa_space_apps_challenge.data.sync

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mx.edu.utez.eir_nasa_space_apps_challenge.data.auth.SessionManager
import mx.edu.utez.eir_nasa_space_apps_challenge.data.local.db.EirDatabase
import mx.edu.utez.eir_nasa_space_apps_challenge.data.local.entity.OutboxEntity
import mx.edu.utez.eir_nasa_space_apps_challenge.data.remote.api.EirApiService
import mx.edu.utez.eir_nasa_space_apps_challenge.data.remote.dtos.*

data class SyncSummary(
    val processedCount: Int,
    val acknowledgedCount: Int,
    val rejectedCount: Int,
    val conflictCount: Int,
    val retryableCount: Int
)

class OutboxSyncManager(
    private val db: EirDatabase,
    private val apiService: EirApiService,
    private val sessionManager: SessionManager,
    private val maxBatchSize: Int = 20
) {
    private val gson: Gson = GsonBuilder().create()

    suspend fun syncPendingRecords(): Result<SyncSummary> = withContext(Dispatchers.IO) {
        val currentUserId = sessionManager.getUserId() ?: ""
        val deviceId = sessionManager.getDeviceId()

        var totalProcessed = 0
        var totalAck = 0
        var totalRejected = 0
        var totalConflict = 0
        var totalRetryable = 0

        val attemptedRecordIds = mutableSetOf<String>()

        while (true) {
            // Fetch up to maxBatchSize (20) pending or retryable records not yet attempted in this sync run
            val pendingEntries = (if (currentUserId.isNotEmpty()) {
                db.outboxDao().getPendingOutboxEntriesForUser(currentUserId, maxBatchSize)
            } else {
                db.outboxDao().getPendingOutboxEntries(maxBatchSize)
            }).filter { it.recordId !in attemptedRecordIds }

            if (pendingEntries.isEmpty()) {
                break
            }

            attemptedRecordIds.addAll(pendingEntries.map { it.recordId })

            val recordsToSend = mutableListOf<SyncRecordDto>()
            val validEntries = mutableListOf<OutboxEntity>()

            for (entry in pendingEntries) {
                val record = try {
                    SyncRecordUtils.parseAndMigrateSyncRecord(entry.payloadJson)
                } catch (e: Exception) {
                    // Malformed entry: mark as REJECTED so it doesn't block the queue indefinitely
                    db.outboxDao().markFailure(entry.recordId, "REJECTED", "Malformed payload: ${e.message}")
                    totalRejected++
                    continue
                }

                val validation = SyncRecordUtils.validateSyncRecord(record)
                if (validation is ValidationResult.Invalid) {
                    db.outboxDao().markFailure(entry.recordId, "REJECTED", validation.reason)
                    totalRejected++
                    continue
                }

                recordsToSend.add(record)
                validEntries.add(entry)
            }

            if (recordsToSend.isEmpty()) {
                // If all items in this batch were invalid/rejected, check if more remain
                val remaining = db.outboxDao().getPendingCount()
                if (remaining == 0) break else continue
            }

            // Mark entries as IN_PROGRESS
            recordsToSend.forEach {
                db.outboxDao().updateStatus(it.recordId, "IN_PROGRESS")
            }

            val request = SyncBatchRequestDto(
                deviceId = deviceId,
                records = recordsToSend
            )

            val response = try {
                apiService.pushRecords(request)
            } catch (e: Exception) {
                // Network / connectivity failure: reset IN_PROGRESS back to RETRYABLE without deleting!
                recordsToSend.forEach {
                    db.outboxDao().markFailure(it.recordId, "RETRYABLE", "Network failure: ${e.localizedMessage ?: e.message}")
                }
                return@withContext Result.failure(e)
            }

            if (!response.isSuccessful) {
                val errCode = response.code()
                val errMsg = "HTTP $errCode: ${response.message()}"
                val status = if (errCode in listOf(408, 429, 500, 502, 503, 504)) "RETRYABLE" else "REJECTED"
                recordsToSend.forEach {
                    db.outboxDao().markFailure(it.recordId, status, errMsg)
                }
                return@withContext Result.failure(retrofit2.HttpException(response))
            }

            val batchResponse = response.body()
            if (batchResponse == null) {
                recordsToSend.forEach {
                    db.outboxDao().markFailure(it.recordId, "RETRYABLE", "Null response body from server")
                }
                return@withContext Result.failure(IllegalStateException("Empty sync response body"))
            }

            val sentIds = recordsToSend.map { it.recordId }.toSet()
            val acknowledgedIds = mutableListOf<String>()
            val processedIdsInResponse = mutableSetOf<String>()

            for (result in batchResponse.results) {
                // Security / integrity check: Do NOT accept unknown IDs returned by server
                if (!sentIds.contains(result.recordId)) {
                    continue
                }

                processedIdsInResponse.add(result.recordId)

                when (result.status) {
                    "accepted", "duplicate" -> {
                        acknowledgedIds.add(result.recordId)
                        totalAck++
                    }
                    "rejected" -> {
                        db.outboxDao().markFailure(
                            result.recordId,
                            "REJECTED",
                            result.error?.message ?: "Rejected by server"
                        )
                        totalRejected++
                    }
                    "conflict" -> {
                        db.outboxDao().markFailure(
                            result.recordId,
                            "CONFLICT",
                            result.error?.message ?: "Conflict detected by server"
                        )
                        totalConflict++
                    }
                    "retryable" -> {
                        db.outboxDao().markFailure(
                            result.recordId,
                            "RETRYABLE",
                            result.error?.message ?: "Server requested retry"
                        )
                        totalRetryable++
                    }
                    else -> {
                        db.outboxDao().markFailure(
                            result.recordId,
                            "RETRYABLE",
                            "Unrecognized status: ${result.status}"
                        )
                        totalRetryable++
                    }
                }
            }

            // Incomplete server response handling: if server omitted any sent record from results,
            // reset it to RETRYABLE so it is never dropped or deleted
            for (sentId in sentIds) {
                if (!processedIdsInResponse.contains(sentId)) {
                    db.outboxDao().markFailure(sentId, "RETRYABLE", "Record omitted from server batch results")
                    totalRetryable++
                }
            }

            // CRITICAL (P05): Delete ONLY acknowledged (accepted or duplicate) records
            if (acknowledgedIds.isNotEmpty()) {
                db.outboxDao().deleteByIds(acknowledgedIds)
            }

            totalProcessed += recordsToSend.size
        }

        Result.success(
            SyncSummary(
                processedCount = totalProcessed,
                acknowledgedCount = totalAck,
                rejectedCount = totalRejected,
                conflictCount = totalConflict,
                retryableCount = totalRetryable
            )
        )
    }
}
