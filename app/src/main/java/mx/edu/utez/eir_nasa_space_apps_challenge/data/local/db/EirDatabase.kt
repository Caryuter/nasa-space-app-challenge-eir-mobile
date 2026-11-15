package mx.edu.utez.eir_nasa_space_apps_challenge.data.local.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.room.Database
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import mx.edu.utez.eir_nasa_space_apps_challenge.data.local.dao.*
import mx.edu.utez.eir_nasa_space_apps_challenge.data.local.entity.*

@Database(
    entities = [
        AstronautProfileEntity::class,
        HealthRecordEntity::class,
        WellbeingCheckinEntity::class,
        OutboxEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class EirDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun healthRecordDao(): HealthRecordDao
    abstract fun wellbeingDao(): WellbeingDao
    abstract fun outboxDao(): OutboxDao

    companion object {
        @Volatile
        private var INSTANCE: EirDatabase? = null

        fun getInstance(context: Context): EirDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = EirDatabase_Impl(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}

class EirDatabase_Impl(private val context: Context) : EirDatabase() {

    private val dbHelper = object : SQLiteOpenHelper(context, "eir_health_v2.db", null, 1) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS astronaut_profile (
                    userId TEXT PRIMARY KEY,
                    astronautId TEXT NOT NULL,
                    username TEXT NOT NULL,
                    displayName TEXT NOT NULL,
                    missionId TEXT NOT NULL,
                    profileVersion INTEGER NOT NULL
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS health_records (
                    recordId TEXT PRIMARY KEY,
                    heartRateBpm INTEGER,
                    glucoseMgDl REAL,
                    temperatureC REAL,
                    sleepHours REAL,
                    systolicBpMmhg INTEGER,
                    diastolicBpMmhg INTEGER,
                    recordedAt TEXT NOT NULL
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS wellbeing_checkins (
                    recordId TEXT PRIMARY KEY,
                    mood INTEGER NOT NULL,
                    connection INTEGER NOT NULL,
                    energy INTEGER NOT NULL,
                    purpose INTEGER NOT NULL,
                    sleep INTEGER NOT NULL,
                    note TEXT,
                    recordedAt TEXT NOT NULL
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS outbox (
                    recordId TEXT PRIMARY KEY,
                    recordType TEXT NOT NULL,
                    payloadJson TEXT NOT NULL,
                    status TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    retryCount INTEGER NOT NULL,
                    lastError TEXT
                )
                """.trimIndent()
            )
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS astronaut_profile")
            db.execSQL("DROP TABLE IF EXISTS health_records")
            db.execSQL("DROP TABLE IF EXISTS wellbeing_checkins")
            db.execSQL("DROP TABLE IF EXISTS outbox")
            onCreate(db)
        }
    }

    private val _profileFlow = MutableStateFlow<AstronautProfileEntity?>(null)
    private val _healthFlow = MutableStateFlow<List<HealthRecordEntity>>(emptyList())
    private val _latestHealthFlow = MutableStateFlow<HealthRecordEntity?>(null)
    private val _wellbeingFlow = MutableStateFlow<List<WellbeingCheckinEntity>>(emptyList())
    private val _pendingOutboxFlow = MutableStateFlow(0)

    private val profileDaoImpl = object : ProfileDao {
        override fun getProfile(): Flow<AstronautProfileEntity?> = _profileFlow.asStateFlow()

        override suspend fun getProfileSync(): AstronautProfileEntity? {
            val db = dbHelper.readableDatabase
            db.rawQuery("SELECT * FROM astronaut_profile LIMIT 1", null).use { cursor ->
                if (cursor.moveToFirst()) {
                    return AstronautProfileEntity(
                        userId = cursor.getString(0),
                        astronautId = cursor.getString(1),
                        username = cursor.getString(2),
                        displayName = cursor.getString(3),
                        missionId = cursor.getString(4),
                        profileVersion = cursor.getInt(5)
                    )
                }
            }
            return null
        }

        override suspend fun insertProfile(profile: AstronautProfileEntity) {
            val db = dbHelper.writableDatabase
            val cv = ContentValues().apply {
                put("userId", profile.userId)
                put("astronautId", profile.astronautId)
                put("username", profile.username)
                put("displayName", profile.displayName)
                put("missionId", profile.missionId)
                put("profileVersion", profile.profileVersion)
            }
            db.insertWithOnConflict("astronaut_profile", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            _profileFlow.value = profile
        }
    }

    private val healthRecordDaoImpl = object : HealthRecordDao {
        override fun getAllHealthRecords(): Flow<List<HealthRecordEntity>> = _healthFlow.asStateFlow()

        override fun getLatestHealthRecord(): Flow<HealthRecordEntity?> = _latestHealthFlow.asStateFlow()

        override suspend fun insertHealthRecord(record: HealthRecordEntity) {
            val db = dbHelper.writableDatabase
            val cv = ContentValues().apply {
                put("recordId", record.recordId)
                put("heartRateBpm", record.heartRateBpm)
                put("glucoseMgDl", record.glucoseMgDl)
                put("temperatureC", record.temperatureC)
                put("sleepHours", record.sleepHours)
                put("systolicBpMmhg", record.systolicBpMmhg)
                put("diastolicBpMmhg", record.diastolicBpMmhg)
                put("recordedAt", record.recordedAt)
            }
            db.insertWithOnConflict("health_records", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            refreshHealthCache()
        }

        override suspend fun insertAllHealthRecords(records: List<HealthRecordEntity>) {
            val db = dbHelper.writableDatabase
            db.beginTransaction()
            try {
                records.forEach { record ->
                    val cv = ContentValues().apply {
                        put("recordId", record.recordId)
                        put("heartRateBpm", record.heartRateBpm)
                        put("glucoseMgDl", record.glucoseMgDl)
                        put("temperatureC", record.temperatureC)
                        put("sleepHours", record.sleepHours)
                        put("systolicBpMmhg", record.systolicBpMmhg)
                        put("diastolicBpMmhg", record.diastolicBpMmhg)
                        put("recordedAt", record.recordedAt)
                    }
                    db.insertWithOnConflict("health_records", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            refreshHealthCache()
        }
    }

    private fun refreshHealthCache() {
        val list = mutableListOf<HealthRecordEntity>()
        val db = dbHelper.readableDatabase
        db.rawQuery("SELECT * FROM health_records ORDER BY recordedAt DESC", null).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(
                    HealthRecordEntity(
                        recordId = cursor.getString(0),
                        heartRateBpm = if (cursor.isNull(1)) null else cursor.getInt(1),
                        glucoseMgDl = if (cursor.isNull(2)) null else cursor.getDouble(2),
                        temperatureC = if (cursor.isNull(3)) null else cursor.getDouble(3),
                        sleepHours = if (cursor.isNull(4)) null else cursor.getDouble(4),
                        systolicBpMmhg = if (cursor.isNull(5)) null else cursor.getInt(5),
                        diastolicBpMmhg = if (cursor.isNull(6)) null else cursor.getInt(6),
                        recordedAt = cursor.getString(7)
                    )
                )
            }
        }
        _healthFlow.value = list
        _latestHealthFlow.value = list.firstOrNull()
    }

    private val wellbeingDaoImpl = object : WellbeingDao {
        override fun getAllWellbeingCheckins(): Flow<List<WellbeingCheckinEntity>> = _wellbeingFlow.asStateFlow()

        override suspend fun insertWellbeingCheckin(checkin: WellbeingCheckinEntity) {
            val db = dbHelper.writableDatabase
            val cv = ContentValues().apply {
                put("recordId", checkin.recordId)
                put("mood", checkin.mood)
                put("connection", checkin.connection)
                put("energy", checkin.energy)
                put("purpose", checkin.purpose)
                put("sleep", checkin.sleep)
                put("note", checkin.note)
                put("recordedAt", checkin.recordedAt)
            }
            db.insertWithOnConflict("wellbeing_checkins", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            refreshWellbeingCache()
        }

        override suspend fun insertAllWellbeingCheckins(checkins: List<WellbeingCheckinEntity>) {
            val db = dbHelper.writableDatabase
            db.beginTransaction()
            try {
                checkins.forEach { checkin ->
                    val cv = ContentValues().apply {
                        put("recordId", checkin.recordId)
                        put("mood", checkin.mood)
                        put("connection", checkin.connection)
                        put("energy", checkin.energy)
                        put("purpose", checkin.purpose)
                        put("sleep", checkin.sleep)
                        put("note", checkin.note)
                        put("recordedAt", checkin.recordedAt)
                    }
                    db.insertWithOnConflict("wellbeing_checkins", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            refreshWellbeingCache()
        }
    }

    private fun refreshWellbeingCache() {
        val list = mutableListOf<WellbeingCheckinEntity>()
        val db = dbHelper.readableDatabase
        db.rawQuery("SELECT * FROM wellbeing_checkins ORDER BY recordedAt DESC", null).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(
                    WellbeingCheckinEntity(
                        recordId = cursor.getString(0),
                        mood = cursor.getInt(1),
                        connection = cursor.getInt(2),
                        energy = cursor.getInt(3),
                        purpose = cursor.getInt(4),
                        sleep = cursor.getInt(5),
                        note = if (cursor.isNull(6)) null else cursor.getString(6),
                        recordedAt = cursor.getString(7)
                    )
                )
            }
        }
        _wellbeingFlow.value = list
    }

    private val outboxDaoImpl = object : OutboxDao {
        override suspend fun getPendingOutboxEntries(): List<OutboxEntity> {
            val list = mutableListOf<OutboxEntity>()
            val db = dbHelper.readableDatabase
            db.rawQuery(
                "SELECT * FROM outbox WHERE status IN ('PENDING', 'RETRYABLE') ORDER BY createdAt ASC LIMIT 20",
                null
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    list.add(
                        OutboxEntity(
                            recordId = cursor.getString(0),
                            recordType = cursor.getString(1),
                            payloadJson = cursor.getString(2),
                            status = cursor.getString(3),
                            createdAt = cursor.getLong(4),
                            retryCount = cursor.getInt(5),
                            lastError = if (cursor.isNull(6)) null else cursor.getString(6)
                        )
                    )
                }
            }
            return list
        }

        override suspend fun insertOutboxEntry(entry: OutboxEntity) {
            val db = dbHelper.writableDatabase
            val cv = ContentValues().apply {
                put("recordId", entry.recordId)
                put("recordType", entry.recordType)
                put("payloadJson", entry.payloadJson)
                put("status", entry.status)
                put("createdAt", entry.createdAt)
                put("retryCount", entry.retryCount)
                put("lastError", entry.lastError)
            }
            db.insertWithOnConflict("outbox", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            refreshOutboxCount()
        }

        override suspend fun updateStatus(recordId: String, status: String) {
            val db = dbHelper.writableDatabase
            val cv = ContentValues().apply {
                put("status", status)
            }
            db.update("outbox", cv, "recordId = ?", arrayOf(recordId))
            refreshOutboxCount()
        }

        override suspend fun markFailure(recordId: String, status: String, error: String?) {
            val db = dbHelper.writableDatabase
            db.execSQL(
                "UPDATE outbox SET status = ?, retryCount = retryCount + 1, lastError = ? WHERE recordId = ?",
                arrayOf(status, error, recordId)
            )
            refreshOutboxCount()
        }

        override suspend fun deleteAcknowledged() {
            val db = dbHelper.writableDatabase
            db.delete("outbox", "status = ?", arrayOf("ACKNOWLEDGED"))
            refreshOutboxCount()
        }

        override fun getPendingCountFlow(): Flow<Int> = _pendingOutboxFlow.asStateFlow()
    }

    private fun refreshOutboxCount() {
        val db = dbHelper.readableDatabase
        db.rawQuery("SELECT COUNT(*) FROM outbox WHERE status = 'PENDING'", null).use { cursor ->
            if (cursor.moveToFirst()) {
                _pendingOutboxFlow.value = cursor.getInt(0)
            }
        }
    }

    override fun profileDao(): ProfileDao = profileDaoImpl
    override fun healthRecordDao(): HealthRecordDao = healthRecordDaoImpl
    override fun wellbeingDao(): WellbeingDao = wellbeingDaoImpl
    override fun outboxDao(): OutboxDao = outboxDaoImpl

    override fun createOpenHelper(config: androidx.room.DatabaseConfiguration): androidx.sqlite.db.SupportSQLiteOpenHelper {
        throw UnsupportedOperationException("Using direct SQLite helper implementation")
    }

    override fun createInvalidationTracker(): androidx.room.InvalidationTracker {
        return androidx.room.InvalidationTracker(
            this,
            HashMap<String, String>(),
            HashMap<String, Set<String>>(),
            "astronaut_profile", "health_records", "wellbeing_checkins", "outbox"
        )
    }

    override fun clearAllTables() {
        val db = dbHelper.writableDatabase
        db.execSQL("DELETE FROM astronaut_profile")
        db.execSQL("DELETE FROM health_records")
        db.execSQL("DELETE FROM wellbeing_checkins")
        db.execSQL("DELETE FROM outbox")
    }
}
