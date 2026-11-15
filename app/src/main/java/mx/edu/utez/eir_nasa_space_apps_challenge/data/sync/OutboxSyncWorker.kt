package mx.edu.utez.eir_nasa_space_apps_challenge.data.sync

import android.content.Context
import androidx.work.*
import com.google.gson.Gson
import mx.edu.utez.eir_nasa_space_apps_challenge.data.local.db.EirDatabase
import mx.edu.utez.eir_nasa_space_apps_challenge.data.remote.api.EirApiService
import mx.edu.utez.eir_nasa_space_apps_challenge.data.remote.api.MockEirApiInterceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class OutboxSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val db = EirDatabase.getInstance(applicationContext)

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(MockEirApiInterceptor())
            .connectTimeout(10, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.example.org")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(EirApiService::class.java)
        val syncManager = OutboxSyncManager(db, apiService)

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
            val request = OneTimeWorkRequestBuilder<OutboxSyncWorker>()
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "${WORK_NAME}_OneTime",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
