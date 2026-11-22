package com.example.fridgefoodsaver.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.fridgefoodsaver.data.FoodDatabase
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ExpiryNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {

        return try {

            val database = FoodDatabase.getDatabase(
                applicationContext
            )

            val foodList = database.foodDao()
                .getAllFood()
                .first()

            val attentionList = foodList.mapNotNull { food ->

                val days = getDaysUntilExpiry(food.expiryDate)

                if (days != null && days <= 3) {
                    Pair(food, days)
                } else {
                    null
                }
            }.sortedBy { it.second }

            if (attentionList.isNotEmpty()) {

                val expiredCount = attentionList.count {
                    it.second < 0
                }

                val todayCount = attentionList.count {
                    it.second == 0
                }

                val upcomingCount = attentionList.count {
                    it.second in 1..3
                }

                val message = buildString {

                    attentionList.take(5).forEach { (food, days) ->

                        val status = when {
                            days < 0 ->
                                "Sudah kedaluwarsa"

                            days == 0 ->
                                "Kedaluwarsa hari ini"

                            else ->
                                "Kedaluwarsa dalam $days hari"
                        }

                        append("• ${food.name}: $status\n")
                    }

                    if (attentionList.size > 5) {
                        append(
                            "Dan ${attentionList.size - 5} bahan lainnya."
                        )
                    }
                }

                val title = buildString {
                    append("Pengingat bahan makanan")

                    if (expiredCount > 0) {
                        append(" • $expiredCount kedaluwarsa")
                    }

                    if (todayCount > 0) {
                        append(" • $todayCount hari ini")
                    }

                    if (upcomingCount > 0) {
                        append(" • $upcomingCount segera")
                    }
                }

                ExpiryNotificationHelper.showExpiryNotification(
                    applicationContext,
                    title,
                    message
                )
            }

            Result.success()

        } catch (e: Exception) {

            Result.retry()
        }
    }

    private fun getDaysUntilExpiry(
        dateString: String
    ): Int? {

        return try {

            val formatter = SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
            ).apply {
                isLenient = false
            }

            val expiryDate = formatter.parse(dateString)
                ?: return null

            val expiryCalendar = Calendar.getInstance().apply {
                time = expiryDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val todayCalendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val difference =
                expiryCalendar.timeInMillis -
                        todayCalendar.timeInMillis

            (
                    difference /
                            (24 * 60 * 60 * 1000L)
                    ).toInt()

        } catch (e: Exception) {
            null
        }
    }
}
