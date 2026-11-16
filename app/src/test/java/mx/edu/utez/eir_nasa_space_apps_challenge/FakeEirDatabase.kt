package mx.edu.utez.eir_nasa_space_apps_challenge

import androidx.room.InvalidationTracker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import mx.edu.utez.eir_nasa_space_apps_challenge.data.local.dao.*
import mx.edu.utez.eir_nasa_space_apps_challenge.data.local.db.EirDatabase
import mx.edu.utez.eir_nasa_space_apps_challenge.data.local.entity.*

class FakeEirDatabase : EirDatabase() {

    val outboxEntries = mutableListOf<OutboxEntity>()
    val healthRecords = mutableListOf<HealthRecordEntity>()
    val wellbeingCheckins = mutableListOf<WellbeingCheckinEntity>()
    var profile: AstronautProfileEntity? = null

    private val _outboxCountFlow = MutableStateFlow(0)
    private val _healthFlow = MutableStateFlow<List<HealthRecordEntity>>(emptyList())
    private val _wellbeingFlow = MutableStateFlow<List<WellbeingCheckinEntity>>(emptyList())
    private val _profileFlow = MutableStateFlow<AstronautProfileEntity?>(null)

    private val fakeOutboxDao = object : OutboxDao {
        override suspend fun getPendingOutboxEntries(limit: Int): List<OutboxEntity> {
            return outboxEntries
                .filter { it.status == "PENDING" || it.status == "RETRYABLE" }
                .sortedBy { it.createdAt }
                .take(limit)
        }

        override suspend fun getPendingOutboxEntriesForUser(userId: String, limit: Int): List<OutboxEntity> {
            return outboxEntries
                .filter { (it.status == "PENDING" || it.status == "RETRYABLE") && (it.userId == userId || it.userId.isEmpty()) }
                .sortedBy { it.createdAt }
                .take(limit)
        }

        override suspend fun getAllPendingRecordIds(): List<String> {
            return outboxEntries
                .filter { it.status == "PENDING" || it.status == "IN_PROGRESS" || it.status == "RETRYABLE" }
                .map { it.recordId }
        }

        override suspend fun getAllOutboxEntries(): List<OutboxEntity> {
            return outboxEntries.toList()
        }

        override suspend fun insertOutboxEntry(entry: OutboxEntity) {
            outboxEntries.removeAll { it.recordId == entry.recordId }
            outboxEntries.add(entry)
            updateOutboxFlow()
        }

        override suspend fun updateStatus(recordId: String, status: String) {
            val idx = outboxEntries.indexOfFirst { it.recordId == recordId }
            if (idx >= 0) {
                outboxEntries[idx] = outboxEntries[idx].copy(status = status)
            }
            updateOutboxFlow()
        }

        override suspend fun markFailure(recordId: String, status: String, error: String?) {
            val idx = outboxEntries.indexOfFirst { it.recordId == recordId }
            if (idx >= 0) {
                val curr = outboxEntries[idx]
                outboxEntries[idx] = curr.copy(
                    status = status,
                    retryCount = curr.retryCount + 1,
                    lastError = error
                )
            }
            updateOutboxFlow()
        }

        override suspend fun deleteAcknowledged() {
            outboxEntries.removeAll { it.status == "ACKNOWLEDGED" }
            updateOutboxFlow()
        }

        override suspend fun deleteByIds(recordIds: List<String>) {
            outboxEntries.removeAll { it.recordId in recordIds }
            updateOutboxFlow()
        }

        override fun getPendingCountFlow(): Flow<Int> = _outboxCountFlow.asStateFlow()

        override suspend fun getPendingCount(): Int {
            return outboxEntries.count { it.status == "PENDING" || it.status == "RETRYABLE" }
        }

        override suspend fun clearAll() {
            outboxEntries.clear()
            updateOutboxFlow()
        }

        private fun updateOutboxFlow() {
            _outboxCountFlow.value = outboxEntries.count { it.status == "PENDING" || it.status == "RETRYABLE" || it.status == "IN_PROGRESS" }
        }
    }

    private val fakeProfileDao = object : ProfileDao {
        override fun getProfile(): Flow<AstronautProfileEntity?> = _profileFlow.asStateFlow()
        override suspend fun getProfileSync(): AstronautProfileEntity? = profile
        override suspend fun insertProfile(p: AstronautProfileEntity) {
            profile = p
            _profileFlow.value = p
        }
        override suspend fun clearProfile() {
            profile = null
            _profileFlow.value = null
        }
    }

    private val fakeHealthRecordDao = object : HealthRecordDao {
        override fun getAllHealthRecords(): Flow<List<HealthRecordEntity>> = _healthFlow.asStateFlow()
        override fun getLatestHealthRecord(): Flow<HealthRecordEntity?> = MutableStateFlow(healthRecords.firstOrNull()).asStateFlow()
        override suspend fun insertHealthRecord(record: HealthRecordEntity) {
            healthRecords.removeAll { it.recordId == record.recordId }
            healthRecords.add(0, record)
            _healthFlow.value = healthRecords.toList()
        }
        override suspend fun insertAllHealthRecords(records: List<HealthRecordEntity>) {
            records.forEach { r ->
                healthRecords.removeAll { it.recordId == r.recordId }
                healthRecords.add(0, r)
            }
            _healthFlow.value = healthRecords.toList()
        }
        override suspend fun clearAll() {
            healthRecords.clear()
            _healthFlow.value = emptyList()
        }
    }

    private val fakeWellbeingDao = object : WellbeingDao {
        override fun getAllWellbeingCheckins(): Flow<List<WellbeingCheckinEntity>> = _wellbeingFlow.asStateFlow()
        override suspend fun insertWellbeingCheckin(checkin: WellbeingCheckinEntity) {
            wellbeingCheckins.removeAll { it.recordId == checkin.recordId }
            wellbeingCheckins.add(0, checkin)
            _wellbeingFlow.value = wellbeingCheckins.toList()
        }
        override suspend fun insertAllWellbeingCheckins(checkins: List<WellbeingCheckinEntity>) {
            checkins.forEach { ch ->
                wellbeingCheckins.removeAll { it.recordId == ch.recordId }
                wellbeingCheckins.add(0, ch)
            }
            _wellbeingFlow.value = wellbeingCheckins.toList()
        }
        override suspend fun clearAll() {
            wellbeingCheckins.clear()
            _wellbeingFlow.value = emptyList()
        }
    }

    override fun profileDao(): ProfileDao = fakeProfileDao
    override fun healthRecordDao(): HealthRecordDao = fakeHealthRecordDao
    override fun wellbeingDao(): WellbeingDao = fakeWellbeingDao
    override fun outboxDao(): OutboxDao = fakeOutboxDao

    override fun <T> runInTransaction(block: () -> T): T = block()

    override suspend fun insertHealthRecordWithOutbox(record: HealthRecordEntity, outbox: OutboxEntity) {
        fakeHealthRecordDao.insertHealthRecord(record)
        fakeOutboxDao.insertOutboxEntry(outbox)
    }

    override suspend fun insertWellbeingCheckinWithOutbox(record: WellbeingCheckinEntity, outbox: OutboxEntity) {
        fakeWellbeingDao.insertWellbeingCheckin(record)
        fakeOutboxDao.insertOutboxEntry(outbox)
    }

    override fun clearAllTables() {
        outboxEntries.clear()
        healthRecords.clear()
        wellbeingCheckins.clear()
        profile = null
    }

    override fun createInvalidationTracker(): InvalidationTracker {
        return InvalidationTracker(this, emptyMap(), emptyMap(), "outbox")
    }

    override fun createOpenHelper(config: androidx.room.DatabaseConfiguration): androidx.sqlite.db.SupportSQLiteOpenHelper {
        throw UnsupportedOperationException()
    }
}
