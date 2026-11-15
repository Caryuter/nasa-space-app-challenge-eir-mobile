package mx.edu.utez.eir_nasa_space_apps_challenge.data.repository

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import mx.edu.utez.eir_nasa_space_apps_challenge.data.local.db.EirDatabase
import mx.edu.utez.eir_nasa_space_apps_challenge.data.local.entity.*
import mx.edu.utez.eir_nasa_space_apps_challenge.data.remote.api.EirApiService
import mx.edu.utez.eir_nasa_space_apps_challenge.data.remote.api.MockEirApiInterceptor
import mx.edu.utez.eir_nasa_space_apps_challenge.data.remote.dtos.*
import mx.edu.utez.eir_nasa_space_apps_challenge.data.sync.OutboxSyncManager
import mx.edu.utez.eir_nasa_space_apps_challenge.data.sync.OutboxSyncWorker
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

class EirHealthRepository private constructor(
    context: Context
) {
    private val appContext = context.applicationContext
    private val db = EirDatabase.getInstance(appContext)
    private val gson = Gson()

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(MockEirApiInterceptor())
        .connectTimeout(10, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.example.org")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiService: EirApiService = retrofit.create(EirApiService::class.java)
    private val outboxSyncManager = OutboxSyncManager(db, apiService)

    // Flow Accessors (Single Source of Truth from Room)
    val profileFlow: Flow<AstronautProfileEntity?> = db.profileDao().getProfile()
    val healthRecordsFlow: Flow<List<HealthRecordEntity>> = db.healthRecordDao().getAllHealthRecords()
    val latestHealthRecordFlow: Flow<HealthRecordEntity?> = db.healthRecordDao().getLatestHealthRecord()
    val wellbeingCheckinsFlow: Flow<List<WellbeingCheckinEntity>> = db.wellbeingDao().getAllWellbeingCheckins()
    val pendingOutboxCountFlow: Flow<Int> = db.outboxDao().getPendingCountFlow()

    suspend fun ensureInitializedAndBootstrapped() = withContext(Dispatchers.IO) {
        val existingProfile = db.profileDao().getProfileSync()
        if (existingProfile == null) {
            try {
                val response = apiService.getBootstrapPage()
                if (response.isSuccessful && response.body() != null) {
                    val bootstrapPage = response.body()!!
                    val p = bootstrapPage.profile
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

                    val healthEntities = bootstrapPage.healthRecords.map { hr ->
                        HealthRecordEntity(
                            recordId = hr.recordId,
                            heartRateBpm = hr.payload.heartRateBpm,
                            glucoseMgDl = hr.payload.glucoseMgDl,
                            temperatureC = hr.payload.temperatureC,
                            sleepHours = hr.payload.sleepHours,
                            systolicBpMmhg = hr.payload.systolicBpMmhg,
                            diastolicBpMmhg = hr.payload.diastolicBpMmhg,
                            recordedAt = hr.recordedAt
                        )
                    }
                    db.healthRecordDao().insertAllHealthRecords(healthEntities)

                    val wellbeingEntities = bootstrapPage.wellbeingCheckins.map { wb ->
                        WellbeingCheckinEntity(
                            recordId = wb.recordId,
                            mood = wb.payload.mood,
                            connection = wb.payload.connection,
                            energy = wb.payload.energy,
                            purpose = wb.payload.purpose,
                            sleep = wb.payload.sleep,
                            note = wb.payload.note,
                            recordedAt = wb.recordedAt
                        )
                    }
                    db.wellbeingDao().insertAllWellbeingCheckins(wellbeingEntities)
                } else {
                    seedLocalDefaults()
                }
            } catch (_: Exception) {
                seedLocalDefaults()
            }
        }
    }

    private suspend fun seedLocalDefaults() {
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
                recordedAt = Instant.now().toString()
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
                recordedAt = Instant.now().toString()
            )
        )
    }

    // OUTBOX PATTERN: Atomic Save to Local DB + Outbox Table
    suspend fun saveHealthCheckup(
        heartRate: Int,
        systolicBp: Int,
        temp: Float,
        sleepHours: Float
    ): String = withContext(Dispatchers.IO) {
        val recordId = UUID.randomUUID().toString()
        val nowIso = Instant.now().toString()

        val entity = HealthRecordEntity(
            recordId = recordId,
            heartRateBpm = heartRate,
            glucoseMgDl = 92.0,
            temperatureC = temp.toDouble(),
            sleepHours = sleepHours.toDouble(),
            systolicBpMmhg = systolicBp,
            diastolicBpMmhg = 76,
            recordedAt = nowIso
        )

        val syncRecord = SyncRecordDto(
            recordId = recordId,
            recordedAt = nowIso,
            schemaVersion = 1,
            type = "health_record",
            healthPayload = HealthPayloadDto(
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
            status = "PENDING"
        )

        db.healthRecordDao().insertHealthRecord(entity)
        db.outboxDao().insertOutboxEntry(outboxEntry)

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

        val entity = WellbeingCheckinEntity(
            recordId = recordId,
            mood = mood,
            connection = connection,
            energy = energy,
            purpose = purpose,
            sleep = sleep,
            note = note,
            recordedAt = nowIso
        )

        val syncRecord = SyncRecordDto(
            recordId = recordId,
            recordedAt = nowIso,
            schemaVersion = 1,
            type = "wellbeing_checkin",
            wellbeingPayload = WellbeingPayloadDto(
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
            status = "PENDING"
        )

        db.wellbeingDao().insertWellbeingCheckin(entity)
        db.outboxDao().insertOutboxEntry(outboxEntry)

        triggerOutboxSync()

        recordId
    }

    suspend fun triggerOutboxSync() = withContext(Dispatchers.IO) {
        outboxSyncManager.syncPendingRecords()
        OutboxSyncWorker.scheduleOneTime(appContext)
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
