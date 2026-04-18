package com.breezy.assistant

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import java.util.*
import java.util.concurrent.TimeUnit

class MorningReportWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

   override suspend fun doWork(): Result {
       val report = MorningReport(applicationContext).generateReport()
       showNotification(report)
       return Result.success()
   }

   private fun showNotification(content: String) {
       val channelId = "morning_report"
       val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

       if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
           val channel = NotificationChannel(
               channelId, "Morning Report",
               NotificationManager.IMPORTANCE_DEFAULT
           ).apply {
               description = "Daily status report for your device"
           }
           notificationManager.createNotificationChannel(channel)
       }

       val intent = Intent(applicationContext, MainActivity::class.java).apply {
           flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
       }
       val pendingIntent = PendingIntent.getActivity(
           applicationContext, 0, intent,
           if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
       )

       val notification = NotificationCompat.Builder(applicationContext, channelId)
           .setSmallIcon(android.R.drawable.ic_dialog_info)
           .setContentTitle("Your Morning Report")
           .setContentText(content.split("\n")[0]) // First line only
           .setStyle(NotificationCompat.BigTextStyle().bigText(content))
           .setPriority(NotificationCompat.PRIORITY_DEFAULT)
           .setContentIntent(pendingIntent)
           .setAutoCancel(true)
           .build()

       notificationManager.notify(1001, notification)
   }

   companion object {
       fun schedule(context: Context) {
           val prefs = context.getSharedPreferences("BreezySettings", Context.MODE_PRIVATE)
           val isEnabled = prefs.getBoolean("report_enabled", true)
           if (!isEnabled) {
               WorkManager.getInstance(context).cancelUniqueWork("morning_report")
               return
           }
          
           val hour = prefs.getInt("report_hour", 8)
          
           val now = Calendar.getInstance()
           val target = Calendar.getInstance().apply {
               set(Calendar.HOUR_OF_DAY, hour)
               set(Calendar.MINUTE, 0)
               set(Calendar.SECOND, 0)
               if (before(now)) add(Calendar.DAY_OF_MONTH, 1)
           }
          
           val delay = target.timeInMillis - now.timeInMillis
          
           val request = PeriodicWorkRequestBuilder<MorningReportWorker>(24, TimeUnit.HOURS)
               .setInitialDelay(delay, TimeUnit.MILLISECONDS)
               .setConstraints(Constraints.Builder()
                   .setRequiresBatteryNotLow(true)
                   .build())
               .build()

           WorkManager.getInstance(context).enqueueUniquePeriodicWork(
               "morning_report",
               ExistingPeriodicWorkPolicy.UPDATE,
               request
           )
       }
   }
}
