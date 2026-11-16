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

    @Query("DELETE FROM astronaut_profile")
    suspend fun clearProfile()
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

    @Query("DELETE FROM health_records")
    suspend fun clearAll()
}

@Dao
interface WellbeingDao {
    @Query("SELECT * FROM wellbeing_checkins ORDER BY recordedAt DESC")
    fun getAllWellbeingCheckins(): Flow<List<WellbeingCheckinEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWellbeingCheckin(checkin: WellbeingCheckinEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllWellbeingCheckins(checkins: List<WellbeingCheckinEntity>)

    @Query("DELETE FROM wellbeing_checkins")
    suspend fun clearAll()
}

@Dao
interface OutboxDao {
    @Query("SELECT * FROM outbox WHERE status IN ('PENDING', 'RETRYABLE') ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getPendingOutboxEntries(limit: Int = 20): List<OutboxEntity>

    @Query("SELECT * FROM outbox WHERE status IN ('PENDING', 'RETRYABLE') AND (userId = :userId OR userId = '') ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getPendingOutboxEntriesForUser(userId: String, limit: Int = 20): List<OutboxEntity>

    @Query("SELECT recordId FROM outbox WHERE status IN ('PENDING', 'IN_PROGRESS', 'RETRYABLE')")
    suspend fun getAllPendingRecordIds(): List<String>

    @Query("SELECT * FROM outbox ORDER BY createdAt ASC")
    suspend fun getAllOutboxEntries(): List<OutboxEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutboxEntry(entry: OutboxEntity)

    @Query("UPDATE outbox SET status = :status WHERE recordId = :recordId")
    suspend fun updateStatus(recordId: String, status: String)

    @Query("UPDATE outbox SET status = :status, retryCount = retryCount + 1, lastError = :error WHERE recordId = :recordId")
    suspend fun markFailure(recordId: String, status: String, error: String?)

    @Query("DELETE FROM outbox WHERE status = 'ACKNOWLEDGED'")
    suspend fun deleteAcknowledged()

    @Query("DELETE FROM outbox WHERE recordId IN (:recordIds)")
    suspend fun deleteByIds(recordIds: List<String>)

    @Query("SELECT COUNT(*) FROM outbox WHERE status IN ('PENDING', 'RETRYABLE', 'IN_PROGRESS')")
    fun getPendingCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM outbox WHERE status IN ('PENDING', 'RETRYABLE')")
    suspend fun getPendingCount(): Int

    @Query("DELETE FROM outbox")
    suspend fun clearAll()
}
