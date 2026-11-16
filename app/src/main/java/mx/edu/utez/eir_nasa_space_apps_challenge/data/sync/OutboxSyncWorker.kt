package mx.edu.utez.eir_nasa_space_apps_challenge.data.sync

import android.content.Context
import androidx.work.*
import mx.edu.utez.eir_nasa_space_apps_challenge.data.local.db.EirDatabase
import mx.edu.utez.eir_nasa_space_apps_challenge.data.remote.config.EirNetworkModule
import java.util.concurrent.TimeUnit

class OutboxSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val db = EirDatabase.getInstance(applicationContext)
        val apiService = EirNetworkModule.getApiService(applicationContext)
        val sessionManager = EirNetworkModule.getSessionManager(applicationContext)

        val syncManager = OutboxSyncManager(
            db = db,
            apiService = apiService,
            sessionManager = sessionManager
        )

        val syncResult = syncManager.syncPendingRecords()
        return if (syncResult.isSuccess) {
            Result.success()
        } else {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "EirOutboxSyncWorker"

        fun schedulePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<OutboxSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun scheduleOneTime(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<OutboxSyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "${WORK_NAME}_OneTime",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
