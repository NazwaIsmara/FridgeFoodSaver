package com.example.fridgefoodsaver

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.fridgefoodsaver.data.FoodDatabase
import com.example.fridgefoodsaver.data.FoodItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

class NotificationActivity : AppCompatActivity() {

    private lateinit var tvEmptyNotification: TextView
    private lateinit var expiredContainer: LinearLayout
    private lateinit var attentionContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        findViewById<TextView>(R.id.tvBack).setOnClickListener {
            finish()
        }

        tvEmptyNotification =
            findViewById(R.id.tvEmptyNotification)

        expiredContainer =
            findViewById(R.id.expiredContainer)

        attentionContainer =
            findViewById(R.id.attentionContainer)

        observeNotifications()
    }

    private fun observeNotifications() {

        lifecycleScope.launch {

            repeatOnLifecycle(Lifecycle.State.STARTED) {

                val database = withContext(Dispatchers.IO) {
                    FoodDatabase.getDatabase(applicationContext)
                }

                database.foodDao()
                    .getAllFood()
                    .collect { foodList ->
                        displayNotifications(foodList)
                    }
            }
        }
    }

    private fun displayNotifications(foodList: List<FoodItem>) {

        expiredContainer.removeAllViews()
        attentionContainer.removeAllViews()

        val expiredList = mutableListOf<Pair<FoodItem, Int>>()
        val attentionList = mutableListOf<Pair<FoodItem, Int>>()

        foodList.forEach { food ->

            val days = getDaysUntilExpiry(food.expiryDate)
                ?: return@forEach

            when {
                days < 0 -> {
                    expiredList.add(Pair(food, days))
                }

                days in 0..3 -> {
                    attentionList.add(Pair(food, days))
                }
            }
        }


        expiredList.sortBy { it.second }
        attentionList.sortBy { it.second }


        expiredList.forEach { (food, days) ->
            addNotificationItem(
                expiredContainer,
                food,
                days
            )
        }


        attentionList.forEach { (food, days) ->
            addNotificationItem(
                attentionContainer,
                food,
                days
            )
        }


        tvEmptyNotification.visibility =
            if (expiredList.isEmpty() && attentionList.isEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }
    }


    private fun addNotificationItem(
        container: LinearLayout,
        food: FoodItem,
        days: Int
    ) {

        val item = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            val padding = dpToPx(12)

            setPadding(
                0,
                padding,
                0,
                padding
            )
        }


        val name = TextView(this).apply {
            text = "${getFoodEmoji(food.category)} ${food.name}"
            textSize = 16f

            setTextColor(
                getColor(R.color.text_dark)
            )

            setTypeface(
                null,
                android.graphics.Typeface.BOLD
            )
        }


        val status = TextView(this).apply {

            text = when {
                days < 0 ->
                    "Sudah kedaluwarsa ${-days} hari lalu"

                days == 0 ->
                    "Kedaluwarsa hari ini"

                days == 1 ->
                    "Kedaluwarsa dalam 1 hari"

                else ->
                    "Kedaluwarsa dalam $days hari"
            }

            textSize = 13f

            setTextColor(
                if (days <= 1) {
                    getColor(R.color.expiry_red)
                } else {
                    getColor(R.color.warm_orange)
                }
            )
        }


        val date = TextView(this).apply {

            text = "Tanggal kedaluwarsa: ${food.expiryDate}"
            textSize = 13f

            setTextColor(
                getColor(R.color.text_gray)
            )
        }

        item.addView(name)
        item.addView(status)
        item.addView(date)

        container.addView(item)
    }


    private fun getDaysUntilExpiry(dateString: String): Int? {

        return try {

            val formatter =
                DateTimeFormatter.ofPattern("dd/MM/yyyy")

            val expiryDate =
                LocalDate.parse(dateString, formatter)

            val today =
                LocalDate.now()

            ChronoUnit.DAYS.between(
                today,
                expiryDate
            ).toInt()

        } catch (e: Exception) {
            null
        }
    }


    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }


    private fun getFoodEmoji(category: String): String {

        return when (category.lowercase(Locale.ROOT)) {

            "sayuran" -> "🥬"

            "buah-buahan",
            "buah" -> "🍎"

            "daging" -> "🥩"

            "seafood",
            "ikan" -> "🐟"

            "susu dan olahan",
            "produk susu" -> "🥛"

            "telur" -> "🥚"

            "minuman" -> "🥤"

            "bumbu" -> "🧂"

            else -> "🍱"
        }
    }
}
