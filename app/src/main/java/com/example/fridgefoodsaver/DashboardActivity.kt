package com.example.fridgefoodsaver

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.Button
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class DashboardActivity : AppCompatActivity() {

    private lateinit var tvTotalFood: TextView
    private lateinit var tvExpiringFood: TextView
    private lateinit var alertContainer: LinearLayout
    private lateinit var recentContainer: LinearLayout
    private lateinit var tvEmptyAlert: TextView
    private lateinit var tvEmptyRecent: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        tvTotalFood = findViewById(R.id.tvTotalFood)
        tvExpiringFood = findViewById(R.id.tvExpiringFood)
        alertContainer = findViewById(R.id.alertContainer)
        recentContainer = findViewById(R.id.recentContainer)
        tvEmptyAlert = findViewById(R.id.tvEmptyAlert)
        tvEmptyRecent = findViewById(R.id.tvEmptyRecent)

        findViewById<Button>(R.id.btnAddFood).setOnClickListener {
            startActivity(Intent(this, AddFoodActivity::class.java))
        }

        findViewById<Button>(R.id.btnOpenInventory).setOnClickListener {
            startActivity(Intent(this, InventoryActivity::class.java))
        }

        findViewById<Button>(R.id.btnOpenProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        findViewById<Button>(R.id.btnOpenNotifications).setOnClickListener {
            startActivity(Intent(this, NotificationActivity::class.java))
        }

        observeFoodDatabase()
    }

    private fun observeFoodDatabase() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                val database = withContext(Dispatchers.IO) {
                    FoodDatabase.getDatabase(applicationContext)
                }

                database.foodDao().getAllFood().collect { foodList ->
                    updateDashboard(foodList)
                }
            }
        }
    }

    private fun updateDashboard(foodList: List<FoodItem>) {

        // Menghitung seluruh bahan makanan
        tvTotalFood.text = foodList.size.toString()

        // Mengambil bahan yang sudah kedaluwarsa atau
        // akan kedaluwarsa dalam 3 hari
        val attentionList = foodList
            .map { food -> Pair(food, getDaysUntilExpiry(food.expiryDate)) }
            .filter { it.second != null && it.second!! <= 3 }
            .sortedBy { it.second }

        val expiringCount = attentionList.count {
            it.second != null && it.second!! >= 0
        }

        tvExpiringFood.text = expiringCount.toString()

        // Menampilkan daftar peringatan
        alertContainer.removeAllViews()

        if (attentionList.isEmpty()) {
            tvEmptyAlert.visibility = View.VISIBLE
        } else {
            tvEmptyAlert.visibility = View.GONE

            attentionList.forEach { item ->
                addAlertItem(item.first, item.second!!)
            }
        }

        // Menampilkan 3 bahan terbaru
        recentContainer.removeAllViews()

        val recentItems = foodList.take(3)

        if (recentItems.isEmpty()) {
            tvEmptyRecent.visibility = View.VISIBLE
        } else {
            tvEmptyRecent.visibility = View.GONE

            recentItems.forEach { food ->
                addRecentItem(food)
            }
        }
    }

    private fun getDaysUntilExpiry(dateString: String): Int? {
        return try {
            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            formatter.isLenient = false
            formatter.timeZone = TimeZone.getTimeZone("UTC")

            val expiryDate = formatter.parse(dateString) ?: return null

            val expiryCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            expiryCalendar.time = expiryDate
            expiryCalendar.set(Calendar.HOUR_OF_DAY, 0)
            expiryCalendar.set(Calendar.MINUTE, 0)
            expiryCalendar.set(Calendar.SECOND, 0)
            expiryCalendar.set(Calendar.MILLISECOND, 0)

            val todayCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            todayCalendar.set(Calendar.HOUR_OF_DAY, 0)
            todayCalendar.set(Calendar.MINUTE, 0)
            todayCalendar.set(Calendar.SECOND, 0)
            todayCalendar.set(Calendar.MILLISECOND, 0)

            val difference = expiryCalendar.timeInMillis -
                    todayCalendar.timeInMillis

            (difference / (24 * 60 * 60 * 1000)).toInt()

        } catch (e: Exception) {
            null
        }
    }

    private fun addAlertItem(food: FoodItem, days: Int) {

        val itemLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 12, 0, 12)
        }

        val nameText = TextView(this).apply {
            text = "${getFoodEmoji(food.category)}  ${food.name}"
            textSize = 16f
            setTextColor(getColor(R.color.text_dark))
            setTypeface(null, Typeface.BOLD)
        }

        val statusText = TextView(this).apply {
            text = when {
                days < 0 -> "เลยวันหมดอายุแล้ว ${-days} hari"
                days == 0 -> "Kedaluwarsa hari ini"
                days == 1 -> "Kedaluwarsa dalam 1 hari"
                else -> "Kedaluwarsa dalam $days hari"
            }

            textSize = 13f
            setTextColor(
                if (days <= 1) getColor(R.color.expiry_red)
                else getColor(R.color.warm_orange)
            )
        }

        itemLayout.addView(nameText)
        itemLayout.addView(statusText)

        alertContainer.addView(itemLayout)
    }

    private fun addRecentItem(food: FoodItem) {

        val itemText = TextView(this).apply {
            text = "${getFoodEmoji(food.category)}  ${food.name}     ${food.quantity}"
            textSize = 15f
            setTextColor(getColor(R.color.text_dark))
            setPadding(16, 18, 16, 18)
            setBackgroundColor(getColor(R.color.pure_white))
        }

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        params.bottomMargin = 8

        recentContainer.addView(itemText, params)
    }

    private fun getFoodEmoji(category: String): String {
        return when (category) {
            "Sayuran" -> "🥬"
            "Buah-buahan" -> "🍎"
            "Daging" -> "🥩"
            "Seafood" -> "🐟"
            "Susu dan Olahan" -> "🥛"
            "Telur" -> "🥚"
            "Minuman" -> "🥤"
            else -> "🍱"
        }
    }
}
