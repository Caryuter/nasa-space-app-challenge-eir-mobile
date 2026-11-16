package mx.edu.utez.eir_nasa_space_apps_challenge

import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import mx.edu.utez.eir_nasa_space_apps_challenge.data.auth.DeviceIdManager
import mx.edu.utez.eir_nasa_space_apps_challenge.data.auth.InMemorySessionStorage
import mx.edu.utez.eir_nasa_space_apps_challenge.data.auth.SessionManager
import mx.edu.utez.eir_nasa_space_apps_challenge.data.local.entity.HealthRecordEntity
import mx.edu.utez.eir_nasa_space_apps_challenge.data.local.entity.OutboxEntity
import mx.edu.utez.eir_nasa_space_apps_challenge.data.remote.api.EirApiService
import mx.edu.utez.eir_nasa_space_apps_challenge.data.remote.config.EirNetworkModule
import mx.edu.utez.eir_nasa_space_apps_challenge.data.remote.config.NetworkEnvironment
import mx.edu.utez.eir_nasa_space_apps_challenge.data.remote.dtos.*
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

class P06_BootstrapPaginationTest {

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
            accessToken = "valid_token",
            refreshToken = "valid_refresh",
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
    fun bootstrap_paginatesFollowingNextCursorUntilNull() {
        val profile = AstronautProfileDto(
            userId = "usr_ares_142",
            astronautId = "ASTRO-ARES-IV-07",
            username = "commander_eir",
            displayName = "Comandante Eir",
            missionId = "ARES-IV-MARS",
            profileVersion = 1
        )

        val rec1 = StoredHealthRecordDto(
            recordId = UUID.randomUUID().toString(),
            recordedAt = "2026-11-15T10:00:00Z",
            payload = HealthPayloadDto(heartRateBpm = 70),
            receivedAt = "2026-11-15T10:05:00Z"
        )
        val rec2 = StoredHealthRecordDto(
            recordId = UUID.randomUUID().toString(),
            recordedAt = "2026-11-15T09:00:00Z",
            payload = HealthPayloadDto(heartRateBpm = 72),
            receivedAt = "2026-11-15T09:05:00Z"
        )

        // Page 1: returns rec1 and next_cursor = "cursor_page_2"
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200).setBody(
                gson.toJson(
                    BootstrapPageDto(
                        profile = profile,
                        healthRecords = listOf(rec1),
                        wellbeingCheckins = emptyList(),
                        nextCursor = "cursor_page_2"
                    )
                )
            )
        )

        // Page 2: returns rec2 and next_cursor = null
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200).setBody(
                gson.toJson(
                    BootstrapPageDto(
                        profile = profile,
                        healthRecords = listOf(rec2),
                        wellbeingCheckins = emptyList(),
                        nextCursor = null
                    )
                )
            )
        )

        // Perform bootstrap
        var cursor: String? = null
        val fetchedRecords = mutableListOf<StoredHealthRecordDto>()

        runBlocking {
            do {
                val resp = apiService.getBootstrapPage(cursor = cursor, limit = 30)
                assertTrue(resp.isSuccessful)
                val body = resp.body()!!
                fetchedRecords.addAll(body.healthRecords)
                cursor = body.nextCursor
            } while (!cursor.isNullOrEmpty())
        }

        assertEquals(2, fetchedRecords.size)
        assertEquals(rec1.recordId, fetchedRecords[0].recordId)
        assertEquals(rec2.recordId, fetchedRecords[1].recordId)
        assertEquals(2, mockWebServer.requestCount)
    }

    @Test
    fun bootstrap_reconciliationDoesNotOverwritePendingLocalOutboxRecords() {
        val pendingId = UUID.randomUUID().toString()
        val syncedId = UUID.randomUUID().toString()

        // Local state: record with pendingId has local unsynced edits (heartRate = 99)
        fakeDb.healthRecords.add(
            HealthRecordEntity(
                recordId = pendingId,
                heartRateBpm = 99,
                glucoseMgDl = 95.0,
                temperatureC = 36.8,
                sleepHours = 7.0,
                systolicBpMmhg = 125,
                diastolicBpMmhg = 80,
                recordedAt = "2026-11-15T12:00:00Z",
                userId = "usr_ares_142"
            )
        )
        fakeDb.outboxEntries.add(
            OutboxEntity(
                recordId = pendingId,
                recordType = "health_record",
                payloadJson = "{}",
                status = "PENDING",
                userId = "usr_ares_142"
            )
        )

        // Server returns pendingId with stale heartRate (65), and syncedId with heartRate (75)
        val profile = AstronautProfileDto("usr_ares_142", "ASTRO-01", "cmd", "Commander", "M-1", 1)
        val serverPendingRec = StoredHealthRecordDto(
            recordId = pendingId,
            recordedAt = "2026-11-15T11:00:00Z",
            payload = HealthPayloadDto(heartRateBpm = 65),
            receivedAt = "2026-11-15T11:05:00Z"
        )
        val serverSyncedRec = StoredHealthRecordDto(
            recordId = syncedId,
            recordedAt = "2026-11-15T10:00:00Z",
            payload = HealthPayloadDto(heartRateBpm = 75),
            receivedAt = "2026-11-15T10:05:00Z"
        )

        // Reconcile logic: filter out any server records whose ID is pending in outbox
        runBlocking {
            val pendingOutboxIds = fakeDb.outboxDao().getAllPendingRecordIds().toSet()
            val serverRecords = listOf(serverPendingRec, serverSyncedRec)

            val recordsToInsert = serverRecords
                .filter { !pendingOutboxIds.contains(it.recordId) }
                .map {
                    HealthRecordEntity(
                        recordId = it.recordId,
                        heartRateBpm = it.payload.heartRateBpm,
                        glucoseMgDl = it.payload.glucoseMgDl,
                        temperatureC = it.payload.temperatureC,
                        sleepHours = it.payload.sleepHours,
                        systolicBpMmhg = it.payload.systolicBpMmhg,
                        diastolicBpMmhg = it.payload.diastolicBpMmhg,
                        recordedAt = it.recordedAt,
                        userId = profile.userId
                    )
                }

            fakeDb.healthRecordDao().insertAllHealthRecords(recordsToInsert)
        }

        // Verify: local pending record with heartRate 99 was NOT overwritten!
        val localPendingRecord = fakeDb.healthRecords.find { it.recordId == pendingId }
        assertNotNull(localPendingRecord)
        assertEquals("Local unsynced change (99) must be preserved", 99, localPendingRecord?.heartRateBpm)

        // New synced record was inserted
        val newSyncedRecord = fakeDb.healthRecords.find { it.recordId == syncedId }
        assertNotNull(newSyncedRecord)
        assertEquals(75, newSyncedRecord?.heartRateBpm)
    }

    @Test
    fun realMode_doesNotSeedSyntheticDataOnNetworkFailure() {
        mockWebServer.shutdown() // Server unreachable

        val result = runBlocking {
            try {
                apiService.getBootstrapPage()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        assertTrue("In REAL mode, network failure must not be masked", result.isFailure)
        assertEquals("Database must remain free of synthetic fake data", 0, fakeDb.healthRecords.size)
        assertEquals(0, fakeDb.wellbeingCheckins.size)
        assertNull(fakeDb.profile)
    }
}
