package com.example.fridgefoodsaver.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.OneTimeWorkRequestBuilder
import java.util.concurrent.TimeUnit

object NotificationScheduler {

    private const val WORK_NAME = "daily_expiry_notification"

    fun scheduleDailyNotification(context: Context) {

        val workRequest =
            PeriodicWorkRequestBuilder<ExpiryNotificationWorker>(
                24,
                TimeUnit.HOURS
            ).build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
    }

    fun cancelDailyNotification(context: Context) {

        WorkManager.getInstance(context)
            .cancelUniqueWork(WORK_NAME)
    }

    fun runTestNotification(context: Context) {

        val testRequest =
            OneTimeWorkRequestBuilder<ExpiryNotificationWorker>()
                .build()

        WorkManager.getInstance(context)
            .enqueue(testRequest)
    }
}
