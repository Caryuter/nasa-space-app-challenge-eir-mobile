package mx.edu.utez.eir_nasa_space_apps_challenge.data.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import mx.edu.utez.eir_nasa_space_apps_challenge.MainActivity
import mx.edu.utez.eir_nasa_space_apps_challenge.R
import java.util.concurrent.TimeUnit

class CheckupReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        showNotification(context)
        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "eir_checkup_reminders"
        const val NOTIFICATION_ID = 2026

        fun showNotification(context: Context) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notif_channel_desc)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("OPEN_CHECKUP", true)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(context.getString(R.string.notif_title))
                .setContentText(context.getString(R.string.notif_content))
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(context.getString(R.string.notif_big_text))
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(NOTIFICATION_ID, notification)
            }
        }

        fun scheduleDailyReminder(context: Context) {
            val workManager = WorkManager.getInstance(context)

            val periodicRequest = PeriodicWorkRequestBuilder<CheckupReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(8, TimeUnit.HOURS)
                .build()

            workManager.enqueueUniquePeriodicWork(
                "EirDailyCheckupReminder",
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
        }

        fun scheduleTestReminderIn15Min(context: Context) {
            val workManager = WorkManager.getInstance(context)

            val testRequest = OneTimeWorkRequestBuilder<CheckupReminderWorker>()
                .setInitialDelay(15, TimeUnit.MINUTES)
                .build()

            workManager.enqueue(testRequest)
        }
    }
}
