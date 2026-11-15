package mx.edu.utez.eir_nasa_space_apps_challenge.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import mx.edu.utez.eir_nasa_space_apps_challenge.data.local.entity.*

@Dao
interface ProfileDao {
    @Query("SELECT * FROM astronaut_profile LIMIT 1")
    fun getProfile(): Flow<AstronautProfileEntity?>

    @Query("SELECT * FROM astronaut_profile LIMIT 1")
    suspend fun getProfileSync(): AstronautProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: AstronautProfileEntity)
}

@Dao
interface HealthRecordDao {
    @Query("SELECT * FROM health_records ORDER BY recordedAt DESC")
    fun getAllHealthRecords(): Flow<List<HealthRecordEntity>>

    @Query("SELECT * FROM health_records ORDER BY recordedAt DESC LIMIT 1")
    fun getLatestHealthRecord(): Flow<HealthRecordEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHealthRecord(record: HealthRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllHealthRecords(records: List<HealthRecordEntity>)
}

@Dao
interface WellbeingDao {
    @Query("SELECT * FROM wellbeing_checkins ORDER BY recordedAt DESC")
    fun getAllWellbeingCheckins(): Flow<List<WellbeingCheckinEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWellbeingCheckin(checkin: WellbeingCheckinEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllWellbeingCheckins(checkins: List<WellbeingCheckinEntity>)
}

@Dao
interface OutboxDao {
    @Query("SELECT * FROM outbox WHERE status IN ('PENDING', 'RETRYABLE') ORDER BY createdAt ASC LIMIT 20")
    suspend fun getPendingOutboxEntries(): List<OutboxEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutboxEntry(entry: OutboxEntity)

    @Query("UPDATE outbox SET status = :status WHERE recordId = :recordId")
    suspend fun updateStatus(recordId: String, status: String)

    @Query("UPDATE outbox SET status = :status, retryCount = retryCount + 1, lastError = :error WHERE recordId = :recordId")
    suspend fun markFailure(recordId: String, status: String, error: String?)

    @Query("DELETE FROM outbox WHERE status = 'ACKNOWLEDGED'")
    suspend fun deleteAcknowledged()

    @Query("SELECT COUNT(*) FROM outbox WHERE status = 'PENDING'")
    fun getPendingCountFlow(): Flow<Int>
}
