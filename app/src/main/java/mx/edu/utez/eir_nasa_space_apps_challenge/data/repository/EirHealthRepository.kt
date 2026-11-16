package mx.edu.utez.eir_nasa_space_apps_challenge.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import mx.edu.utez.eir_nasa_space_apps_challenge.data.auth.SessionManager
import mx.edu.utez.eir_nasa_space_apps_challenge.data.local.db.EirDatabase
import mx.edu.utez.eir_nasa_space_apps_challenge.data.local.entity.*
import mx.edu.utez.eir_nasa_space_apps_challenge.data.remote.api.EirApiService
import mx.edu.utez.eir_nasa_space_apps_challenge.data.remote.config.EirNetworkModule
import mx.edu.utez.eir_nasa_space_apps_challenge.data.remote.config.NetworkEnvironment
import mx.edu.utez.eir_nasa_space_apps_challenge.data.remote.dtos.*
import mx.edu.utez.eir_nasa_space_apps_challenge.data.sync.OutboxSyncManager
import mx.edu.utez.eir_nasa_space_apps_challenge.data.sync.OutboxSyncWorker
import mx.edu.utez.eir_nasa_space_apps_challenge.data.sync.SyncSummary
import java.time.Instant
import java.util.UUID

class EirHealthRepository private constructor(
    context: Context
) {
    private val appContext = context.applicationContext
    private val db = EirDatabase.getInstance(appContext)
    private val gson: Gson = GsonBuilder().create()

    private val networkModule = EirNetworkModule.getInstance(appContext)
    private val apiService: EirApiService
        get() = networkModule.getApiService()
    val sessionManager: SessionManager = networkModule.sessionManager

    private val outboxSyncManager = OutboxSyncManager(
        db = db,
        apiService = apiService,
        sessionManager = sessionManager
    )

    // Flow Accessors (Single Source of Truth from Room)
    val profileFlow: Flow<AstronautProfileEntity?> = db.profileDao().getProfile()
    val healthRecordsFlow: Flow<List<HealthRecordEntity>> = db.healthRecordDao().getAllHealthRecords()
    val latestHealthRecordFlow: Flow<HealthRecordEntity?> = db.healthRecordDao().getLatestHealthRecord()
    val wellbeingCheckinsFlow: Flow<List<WellbeingCheckinEntity>> = db.wellbeingDao().getAllWellbeingCheckins()
    val pendingOutboxCountFlow: Flow<Int> = db.outboxDao().getPendingCountFlow()

    init {
        // Schedule periodic sync via WorkManager
        OutboxSyncWorker.schedulePeriodic(appContext)
    }

    // --- AUTHENTICATION (P02) ---

    suspend fun login(username: String, password: String): Result<LoginResponseDto> = withContext(Dispatchers.IO) {
        try {
            val deviceId = sessionManager.getDeviceId()
            val request = LoginRequestDto(
                username = username,
                password = password,
                deviceId = deviceId
            )

            val response = apiService.login(request)
            if (response.isSuccessful && response.body() != null) {
                val loginResponse = response.body()!!
                val newUserId = loginResponse.user.userId
                val previousUserId = sessionManager.getUserId()

                // User Isolation: If a different astronaut logs in, clear previous user's local tables
                if (previousUserId != null && previousUserId != newUserId) {
                    db.clearAllTables()
                }

                // Save session credentials
                sessionManager.saveLoginSession(loginResponse)

                // Persist profile
                db.profileDao().insertProfile(
                    AstronautProfileEntity(
                        userId = loginResponse.user.userId,
                        astronautId = loginResponse.user.astronautId,
                        username = loginResponse.user.username,
                        displayName = loginResponse.user.displayName,
                        missionId = loginResponse.user.missionId,
                        profileVersion = loginResponse.user.profileVersion
                    )
                )

                // Bootstrap initial history
                bootstrapUserData()

                // Trigger sync of any pending outbox items
                triggerOutboxSync()

                Result.success(loginResponse)
            } else {
                Result.failure(retrofit2.HttpException(response))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (sessionManager.isLoggedIn()) {
                try {
                    apiService.logout()
                } catch (_: Exception) {
                    // Best-effort remote revocation
                }
            }
            sessionManager.clearSession()
            Result.success(Unit)
        } catch (e: Exception) {
            sessionManager.clearSession()
            Result.failure(e)
        }
    }

    // --- BOOTSTRAP & HISTORY (P06) ---

    suspend fun ensureInitializedAndBootstrapped() = withContext(Dispatchers.IO) {
        val existingProfile = db.profileDao().getProfileSync()
        if (existingProfile == null) {
            val result = bootstrapUserData()
            if (result.isFailure && networkModule.currentConfig.environment == NetworkEnvironment.MOCK) {
                // Seed mock defaults ONLY in explicit MOCK environment
                seedMockDefaults()
            }
        }
    }

    suspend fun bootstrapUserData(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            var cursor: String? = null
            var firstPage = true

            // Retrieve pending outbox record IDs to prevent overwriting un-synced local changes
            val pendingRecordIds = db.outboxDao().getAllPendingRecordIds().toSet()

            do {
                val response = apiService.getBootstrapPage(cursor = cursor, limit = 30)
                if (!response.isSuccessful || response.body() == null) {
                    return@withContext Result.failure(retrofit2.HttpException(response))
                }

                val page = response.body()!!

                if (firstPage) {
                    firstPage = false
                    val p = page.profile
                    db.profileDao().insertProfile(
                        AstronautProfileEntity(
                            userId = p.userId,
                            astronautId = p.astronautId,
                            username = p.username,
                            displayName = p.displayName,
                            missionId = p.missionId,
                            profileVersion = p.profileVersion
                        )
                    )
                }

                // Reconcile health records: do NOT overwrite records pending in outbox
                val healthEntities = page.healthRecords
                    .filter { !pendingRecordIds.contains(it.recordId) }
                    .map { hr ->
                        HealthRecordEntity(
                            recordId = hr.recordId,
                            heartRateBpm = hr.payload.heartRateBpm,
                            glucoseMgDl = hr.payload.glucoseMgDl,
                            temperatureC = hr.payload.temperatureC,
                            sleepHours = hr.payload.sleepHours,
                            systolicBpMmhg = hr.payload.systolicBpMmhg,
                            diastolicBpMmhg = hr.payload.diastolicBpMmhg,
                            recordedAt = hr.recordedAt,
                            userId = page.profile.userId
                        )
                    }
                db.healthRecordDao().insertAllHealthRecords(healthEntities)

                // Reconcile wellbeing checkins: do NOT overwrite records pending in outbox
                val wellbeingEntities = page.wellbeingCheckins
                    .filter { !pendingRecordIds.contains(it.recordId) }
                    .map { wb ->
                        WellbeingCheckinEntity(
                            recordId = wb.recordId,
                            mood = wb.payload.mood,
                            connection = wb.payload.connection,
                            energy = wb.payload.energy,
                            purpose = wb.payload.purpose,
                            sleep = wb.payload.sleep,
                            note = wb.payload.note,
                            recordedAt = wb.recordedAt,
                            userId = page.profile.userId
                        )
                    }
                db.wellbeingDao().insertAllWellbeingCheckins(wellbeingEntities)

                cursor = page.nextCursor
            } while (!cursor.isNullOrEmpty())

            Result.success(Unit)
        } catch (e: Exception) {
            // CRITICAL (P01 & P06): In REAL mode, errors must be propagated directly without fake data
            Result.failure(e)
        }
    }

    suspend fun fetchHealthHistory(limit: Int = 30): Result<List<StoredHealthRecordDto>> = withContext(Dispatchers.IO) {
        try {
            var cursor: String? = null
            val allRecords = mutableListOf<StoredHealthRecordDto>()
            do {
                val resp = apiService.getMyHealthRecords(cursor = cursor, limit = limit)
                if (!resp.isSuccessful || resp.body() == null) {
                    return@withContext Result.failure(retrofit2.HttpException(resp))
                }
                val page = resp.body()!!
                allRecords.addAll(page.items)
                cursor = page.nextCursor
            } while (!cursor.isNullOrEmpty())
            Result.success(allRecords)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchWellbeingHistory(limit: Int = 30): Result<List<StoredWellbeingCheckinDto>> = withContext(Dispatchers.IO) {
        try {
            var cursor: String? = null
            val allCheckins = mutableListOf<StoredWellbeingCheckinDto>()
            do {
                val resp = apiService.getMyWellbeingCheckins(cursor = cursor, limit = limit)
                if (!resp.isSuccessful || resp.body() == null) {
                    return@withContext Result.failure(retrofit2.HttpException(resp))
                }
                val page = resp.body()!!
                allCheckins.addAll(page.items)
                cursor = page.nextCursor
            } while (!cursor.isNullOrEmpty())
            Result.success(allCheckins)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchAlerts(): Result<List<RiskAlertDto>> = withContext(Dispatchers.IO) {
        try {
            val resp = apiService.getMyAlerts()
            if (!resp.isSuccessful || resp.body() == null) {
                return@withContext Result.failure(retrofit2.HttpException(resp))
            }
            Result.success(resp.body()!!.items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun seedMockDefaults() {
        db.profileDao().insertProfile(
            AstronautProfileEntity(
                userId = "usr_ares_142",
                astronautId = "ASTRO-ARES-IV-07",
                username = "commander_eir",
                displayName = "Comandante Eir",
                missionId = "ARES-IV-MARS",
                profileVersion = 1
            )
        )

        db.healthRecordDao().insertHealthRecord(
            HealthRecordEntity(
                recordId = UUID.randomUUID().toString(),
                heartRateBpm = 68,
                glucoseMgDl = 92.0,
                temperatureC = 36.7,
                sleepHours = 7.5,
                systolicBpMmhg = 118,
                diastolicBpMmhg = 76,
                recordedAt = Instant.now().toString(),
                userId = "usr_ares_142"
            )
        )

        db.wellbeingDao().insertWellbeingCheckin(
            WellbeingCheckinEntity(
                recordId = UUID.randomUUID().toString(),
                mood = 4,
                connection = 4,
                energy = 4,
                purpose = 5,
                sleep = 4,
                note = "Chequeo matutino completo en Sol 142.",
                recordedAt = Instant.now().toString(),
                userId = "usr_ares_142"
            )
        )
    }

    // --- ATOMIC PERSISTENCE (P03 & P04) ---

    suspend fun saveHealthCheckup(
        heartRate: Int,
        systolicBp: Int,
        temp: Float,
        sleepHours: Float
    ): String = withContext(Dispatchers.IO) {
        val recordId = UUID.randomUUID().toString()
        val nowIso = Instant.now().toString()
        val currentUserId = sessionManager.getUserId() ?: ""

        val entity = HealthRecordEntity(
            recordId = recordId,
            heartRateBpm = heartRate,
            glucoseMgDl = 92.0,
            temperatureC = temp.toDouble(),
            sleepHours = sleepHours.toDouble(),
            systolicBpMmhg = systolicBp,
            diastolicBpMmhg = 76,
            recordedAt = nowIso,
            userId = currentUserId
        )

        // OpenAPI aligned payload: unified "payload" field (P03)
        val syncRecord = SyncRecordDto.fromHealthPayload(
            recordId = recordId,
            recordedAt = nowIso,
            payload = HealthPayloadDto(
                heartRateBpm = heartRate,
                glucoseMgDl = 92.0,
                temperatureC = temp.toDouble(),
                sleepHours = sleepHours.toDouble(),
                systolicBpMmhg = systolicBp,
                diastolicBpMmhg = 76
            )
        )

        val outboxEntry = OutboxEntity(
            recordId = recordId,
            recordType = "health_record",
            payloadJson = gson.toJson(syncRecord),
            status = "PENDING",
            userId = currentUserId
        )

        // Atomic transaction (P04)
        db.insertHealthRecordWithOutbox(entity, outboxEntry)

        triggerOutboxSync()

        recordId
    }

    suspend fun saveWellbeingCheckin(
        mood: Int,
        connection: Int,
        energy: Int,
        purpose: Int,
        sleep: Int,
        note: String?
    ): String = withContext(Dispatchers.IO) {
        val recordId = UUID.randomUUID().toString()
        val nowIso = Instant.now().toString()
        val currentUserId = sessionManager.getUserId() ?: ""

        val entity = WellbeingCheckinEntity(
            recordId = recordId,
            mood = mood,
            connection = connection,
            energy = energy,
            purpose = purpose,
            sleep = sleep,
            note = note,
            recordedAt = nowIso,
            userId = currentUserId
        )

        // OpenAPI aligned payload: unified "payload" field (P03)
        val syncRecord = SyncRecordDto.fromWellbeingPayload(
            recordId = recordId,
            recordedAt = nowIso,
            payload = WellbeingPayloadDto(
                mood = mood,
                connection = connection,
                energy = energy,
                purpose = purpose,
                sleep = sleep,
                note = note
            )
        )

        val outboxEntry = OutboxEntity(
            recordId = recordId,
            recordType = "wellbeing_checkin",
            payloadJson = gson.toJson(syncRecord),
            status = "PENDING",
            userId = currentUserId
        )

        // Atomic transaction (P04)
        db.insertWellbeingCheckinWithOutbox(entity, outboxEntry)

        triggerOutboxSync()

        recordId
    }

    suspend fun triggerOutboxSync(): Result<SyncSummary> = withContext(Dispatchers.IO) {
        val result = outboxSyncManager.syncPendingRecords()
        OutboxSyncWorker.scheduleOneTime(appContext)
        result
    }

    companion object {
        @Volatile
        private var INSTANCE: EirHealthRepository? = null

        fun getInstance(context: Context): EirHealthRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = EirHealthRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
