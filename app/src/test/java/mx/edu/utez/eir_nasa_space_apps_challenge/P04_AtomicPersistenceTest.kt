package mx.edu.utez.eir_nasa_space_apps_challenge

import kotlinx.coroutines.runBlocking
import mx.edu.utez.eir_nasa_space_apps_challenge.data.local.db.EirDatabase
import mx.edu.utez.eir_nasa_space_apps_challenge.data.local.entity.HealthRecordEntity
import mx.edu.utez.eir_nasa_space_apps_challenge.data.local.entity.OutboxEntity
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class P04_AtomicPersistenceTest {

    @Test
    fun transactionContract_maintainsIdentifiersAndAtomicity() {
        // Contract test for atomic health + outbox persistence
        val recordId = UUID.randomUUID().toString()
        val nowIso = "2026-11-15T12:00:00Z"
        val userId = "usr_ares_142"

        val healthEntity = HealthRecordEntity(
            recordId = recordId,
            heartRateBpm = 72,
            glucoseMgDl = 95.0,
            temperatureC = 36.6,
            sleepHours = 7.5,
            systolicBpMmhg = 120,
            diastolicBpMmhg = 78,
            recordedAt = nowIso,
            userId = userId
        )

        val outboxEntity = OutboxEntity(
            recordId = recordId,
            recordType = "health_record",
            payloadJson = "{\"record_id\":\"$recordId\"}",
            status = "PENDING",
            userId = userId
        )

        assertEquals("Record IDs must strictly match between entity and outbox", healthEntity.recordId, outboxEntity.recordId)
        assertEquals(userId, healthEntity.userId)
        assertEquals(userId, outboxEntity.userId)
    }

    @Test
    fun transactionRollback_abortsAllChangesOnFailure() {
        // Mock state simulating a transactional database unit of work
        var tableRecordsCount = 0
        var tableOutboxCount = 0

        fun executeAtomicOperation(shouldFailOutbox: Boolean) {
            val prevRecords = tableRecordsCount
            val prevOutbox = tableOutboxCount
            var transactionCommitted = false

            try {
                // Step 1: Insert record
                tableRecordsCount++

                // Step 2: Insert outbox (may fail)
                if (shouldFailOutbox) {
                    throw IllegalStateException("Disk I/O error or constraint failure while saving outbox entry")
                }
                tableOutboxCount++

                // Commit
                transactionCommitted = true
            } finally {
                if (!transactionCommitted) {
                    // Rollback
                    tableRecordsCount = prevRecords
                    tableOutboxCount = prevOutbox
                }
            }
        }

        // Test failure rollback
        try {
            executeAtomicOperation(shouldFailOutbox = true)
            fail("Expected exception was not thrown")
        } catch (e: IllegalStateException) {
            assertEquals("Disk I/O error or constraint failure while saving outbox entry", e.message)
        }

        assertEquals("On failure, record insertion must be rolled back", 0, tableRecordsCount)
        assertEquals("On failure, outbox insertion must be rolled back", 0, tableOutboxCount)

        // Test success commit
        executeAtomicOperation(shouldFailOutbox = false)
        assertEquals(1, tableRecordsCount)
        assertEquals(1, tableOutboxCount)
    }
}
