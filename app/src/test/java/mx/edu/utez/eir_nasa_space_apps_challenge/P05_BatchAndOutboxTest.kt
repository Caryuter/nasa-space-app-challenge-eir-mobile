package mx.edu.utez.eir_nasa_space_apps_challenge

import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import mx.edu.utez.eir_nasa_space_apps_challenge.data.auth.DeviceIdManager
import mx.edu.utez.eir_nasa_space_apps_challenge.data.auth.InMemorySessionStorage
import mx.edu.utez.eir_nasa_space_apps_challenge.data.auth.SessionManager
import mx.edu.utez.eir_nasa_space_apps_challenge.data.local.entity.OutboxEntity
import mx.edu.utez.eir_nasa_space_apps_challenge.data.remote.api.EirApiService
import mx.edu.utez.eir_nasa_space_apps_challenge.data.remote.dtos.*
import mx.edu.utez.eir_nasa_space_apps_challenge.data.sync.OutboxSyncManager
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID

class P05_BatchAndOutboxTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var fakeDb: FakeEirDatabase
    private lateinit var sessionManager: SessionManager
    private lateinit var apiService: EirApiService
    private val gson = Gson()

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        fakeDb = FakeEirDatabase()
        val sessionStorage = InMemorySessionStorage()
        val deviceIdManager = DeviceIdManager.createForTesting("TEST-DEVICE-001")
        sessionManager = SessionManager.createForTesting(sessionStorage, deviceIdManager)

        sessionStorage.saveSession(
            accessToken = "test_token",
            refreshToken = "test_refresh",
            tokenType = "Bearer",
            expiresIn = 900,
            onlineAuthenticatedAt = "2026-11-15T12:00:00Z",
            userId = "usr_ares_142",
            astronautId = "ASTRO-ARES-IV-07",
            username = "commander_eir",
            displayName = "Comandante Eir",
            missionId = "ARES-IV-MARS",
            profileVersion = 1
        )

        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/").toString())
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(EirApiService::class.java)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun batching_queueOf25RecordsIsProcessedInBatchesOfAtMost20() {
        val totalRecords = 25
        for (i in 1..totalRecords) {
            val recordId = UUID.randomUUID().toString()
            val syncRecord = SyncRecordDto.fromHealthPayload(
                recordId = recordId,
                recordedAt = "2026-11-15T12:00:00Z",
                payload = HealthPayloadDto(heartRateBpm = 60 + i)
            )
            fakeDb.outboxEntries.add(
                OutboxEntity(
                    recordId = recordId,
                    recordType = "health_record",
                    payloadJson = gson.toJson(syncRecord),
                    status = "PENDING",
                    userId = "usr_ares_142",
                    createdAt = System.currentTimeMillis() + i
                )
            )
        }

        assertEquals(25, fakeDb.outboxEntries.size)

        // Mock response for Batch 1 (20 items) and Batch 2 (5 items)
        val capturedRequests = mutableListOf<SyncBatchRequestDto>()
        mockWebServer.dispatcher = object : okhttp3.mockwebserver.Dispatcher() {
            override fun dispatch(request: okhttp3.mockwebserver.RecordedRequest): MockResponse {
                val body = request.body.clone().readUtf8()
                val syncRequest = gson.fromJson(body, SyncBatchRequestDto::class.java)
                capturedRequests.add(syncRequest)
                val results = syncRequest.records.map {
                    SyncItemResultDto(recordId = it.recordId, status = "accepted")
                }
                return MockResponse().setResponseCode(200).setBody(
                    gson.toJson(SyncBatchResponseDto(results = results, serverTime = "2026-11-15T12:05:00Z"))
                )
            }
        }

        val syncManager = OutboxSyncManager(
            db = fakeDb,
            apiService = apiService,
            sessionManager = sessionManager,
            maxBatchSize = 20
        )

        val result = runBlocking {
            syncManager.syncPendingRecords()
        }

        assertTrue(result.isSuccess)
        val summary = result.getOrNull()!!
        assertEquals(25, summary.processedCount)
        assertEquals(25, summary.acknowledgedCount)

        // All 25 acknowledged records must be removed from outbox
        assertEquals("All accepted records must be deleted from outbox", 0, fakeDb.outboxEntries.size)
        assertEquals(2, capturedRequests.size)

        assertEquals("First batch must contain exactly 20 items", 20, capturedRequests[0].records.size)
        assertEquals("Second batch must contain remaining 5 items", 5, capturedRequests[1].records.size)
    }

    @Test
    fun individualAcks_onlyAcceptedAndDuplicateAreDeleted_rejectedAndConflictPreserved() {
        val idAccepted = UUID.randomUUID().toString()
        val idDuplicate = UUID.randomUUID().toString()
        val idRejected = UUID.randomUUID().toString()
        val idConflict = UUID.randomUUID().toString()
        val idRetryable = UUID.randomUUID().toString()

        val ids = listOf(idAccepted, idDuplicate, idRejected, idConflict, idRetryable)
        ids.forEachIndexed { idx, id ->
            val syncRecord = SyncRecordDto.fromHealthPayload(
                recordId = id,
                recordedAt = "2026-11-15T12:00:00Z",
                payload = HealthPayloadDto(heartRateBpm = 70 + idx)
            )
            fakeDb.outboxEntries.add(
                OutboxEntity(
                    recordId = id,
                    recordType = "health_record",
                    payloadJson = gson.toJson(syncRecord),
                    status = "PENDING",
                    userId = "usr_ares_142",
                    createdAt = System.currentTimeMillis() + idx
                )
            )
        }

        val results = listOf(
            SyncItemResultDto(idAccepted, "accepted"),
            SyncItemResultDto(idDuplicate, "duplicate"),
            SyncItemResultDto(idRejected, "rejected", ApiErrorDto("invalid_range", "Out of plausible range")),
            SyncItemResultDto(idConflict, "conflict", ApiErrorDto("conflict", "Different payload exists for record_id")),
            SyncItemResultDto(idRetryable, "retryable", ApiErrorDto("transient", "Please retry"))
        )

        mockWebServer.enqueue(
            MockResponse().setResponseCode(200).setBody(
                gson.toJson(SyncBatchResponseDto(results = results, serverTime = "2026-11-15T12:05:00Z"))
            )
        )

        val syncManager = OutboxSyncManager(
            db = fakeDb,
            apiService = apiService,
            sessionManager = sessionManager
        )

        val result = runBlocking {
            syncManager.syncPendingRecords()
        }

        assertTrue(result.isSuccess)
        val summary = result.getOrNull()!!
        assertEquals(2, summary.acknowledgedCount)
        assertEquals(1, summary.rejectedCount)
        assertEquals(1, summary.conflictCount)
        assertEquals(1, summary.retryableCount)

        // Only accepted and duplicate are deleted
        assertNull(fakeDb.outboxEntries.find { it.recordId == idAccepted })
        assertNull(fakeDb.outboxEntries.find { it.recordId == idDuplicate })

        // Rejected and conflict are preserved for review
        val rejectedEntry = fakeDb.outboxEntries.find { it.recordId == idRejected }
        assertNotNull(rejectedEntry)
        assertEquals("REJECTED", rejectedEntry?.status)
        assertEquals("Out of plausible range", rejectedEntry?.lastError)

        val conflictEntry = fakeDb.outboxEntries.find { it.recordId == idConflict }
        assertNotNull(conflictEntry)
        assertEquals("CONFLICT", conflictEntry?.status)
        assertEquals("Different payload exists for record_id", conflictEntry?.lastError)

        // Retryable entry is maintained
        val retryableEntry = fakeDb.outboxEntries.find { it.recordId == idRetryable }
        assertNotNull(retryableEntry)
        assertEquals("RETRYABLE", retryableEntry?.status)
        assertEquals("Please retry", retryableEntry?.lastError)
    }

    @Test
    fun unknownIds_areIgnoredAndNeverAcknowledgeUnsentItems() {
        val knownId = UUID.randomUUID().toString()
        val unknownId = UUID.randomUUID().toString()

        val syncRecord = SyncRecordDto.fromHealthPayload(
            recordId = knownId,
            recordedAt = "2026-11-15T12:00:00Z",
            payload = HealthPayloadDto(heartRateBpm = 75)
        )
        fakeDb.outboxEntries.add(
            OutboxEntity(
                recordId = knownId,
                recordType = "health_record",
                payloadJson = gson.toJson(syncRecord),
                status = "PENDING",
                userId = "usr_ares_142"
            )
        )

        // Server acknowledges knownId AND unknownId
        val results = listOf(
            SyncItemResultDto(knownId, "accepted"),
            SyncItemResultDto(unknownId, "accepted")
        )

        mockWebServer.enqueue(
            MockResponse().setResponseCode(200).setBody(
                gson.toJson(SyncBatchResponseDto(results = results, serverTime = "2026-11-15T12:05:00Z"))
            )
        )

        val syncManager = OutboxSyncManager(
            db = fakeDb,
            apiService = apiService,
            sessionManager = sessionManager
        )

        val result = runBlocking {
            syncManager.syncPendingRecords()
        }

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()!!.acknowledgedCount)
        assertNull(fakeDb.outboxEntries.find { it.recordId == knownId })
    }

    @Test
    fun incompleteResponse_omittedItemsRemainRetryableAndAreNotDeleted() {
        val id1 = UUID.randomUUID().toString()
        val id2Omitted = UUID.randomUUID().toString()

        listOf(id1, id2Omitted).forEach { id ->
            val syncRecord = SyncRecordDto.fromHealthPayload(
                recordId = id,
                recordedAt = "2026-11-15T12:00:00Z",
                payload = HealthPayloadDto(heartRateBpm = 70)
            )
            fakeDb.outboxEntries.add(
                OutboxEntity(
                    recordId = id,
                    recordType = "health_record",
                    payloadJson = gson.toJson(syncRecord),
                    status = "PENDING",
                    userId = "usr_ares_142"
                )
            )
        }

        // Server returns result ONLY for id1, omitting id2
        val results = listOf(SyncItemResultDto(id1, "accepted"))

        mockWebServer.enqueue(
            MockResponse().setResponseCode(200).setBody(
                gson.toJson(SyncBatchResponseDto(results = results, serverTime = "2026-11-15T12:05:00Z"))
            )
        )

        val syncManager = OutboxSyncManager(
            db = fakeDb,
            apiService = apiService,
            sessionManager = sessionManager
        )

        val result = runBlocking {
            syncManager.syncPendingRecords()
        }

        assertTrue(result.isSuccess)
        assertNull(fakeDb.outboxEntries.find { it.recordId == id1 })

        val omittedEntry = fakeDb.outboxEntries.find { it.recordId == id2Omitted }
        assertNotNull("Omitted record must NOT be deleted", omittedEntry)
        assertEquals("RETRYABLE", omittedEntry?.status)
    }

    @Test
    fun networkFailure_preservesAllOutboxEntriesAndMarksRetryable() {
        mockWebServer.shutdown() // Server down

        val id = UUID.randomUUID().toString()
        val syncRecord = SyncRecordDto.fromHealthPayload(
            recordId = id,
            recordedAt = "2026-11-15T12:00:00Z",
            payload = HealthPayloadDto(heartRateBpm = 75)
        )
        fakeDb.outboxEntries.add(
            OutboxEntity(
                recordId = id,
                recordType = "health_record",
                payloadJson = gson.toJson(syncRecord),
                status = "PENDING",
                userId = "usr_ares_142"
            )
        )

        val syncManager = OutboxSyncManager(
            db = fakeDb,
            apiService = apiService,
            sessionManager = sessionManager
        )

        val result = runBlocking {
            syncManager.syncPendingRecords()
        }

        assertTrue("Network error must return failure Result", result.isFailure)
        val entry = fakeDb.outboxEntries.find { it.recordId == id }
        assertNotNull("Record must not be lost on network failure", entry)
        assertEquals("RETRYABLE", entry?.status)
    }
}
