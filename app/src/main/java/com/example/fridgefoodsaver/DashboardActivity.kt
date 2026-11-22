package com.example.fridgefoodsaver

import android.content.Intent
import android.graphics.Typeface
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
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
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
    private lateinit var bottomNavigation: BottomNavigationView

    private val foodDao by lazy {
        FoodDatabase.getDatabase(applicationContext).foodDao()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        bindViews()
        setupBottomNavigation()
        observeFoodDatabase()
    }

    private fun bindViews() {
        tvTotalFood = findViewById(R.id.tvTotalFood)
        tvExpiringFood = findViewById(R.id.tvExpiringFood)
        alertContainer = findViewById(R.id.alertContainer)
        recentContainer = findViewById(R.id.recentContainer)
        tvEmptyAlert = findViewById(R.id.tvEmptyAlert)
        tvEmptyRecent = findViewById(R.id.tvEmptyRecent)
        bottomNavigation = findViewById(R.id.bottomNavigation)
    }

    private fun setupBottomNavigation() {
        bottomNavigation.selectedItemId = R.id.nav_home

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {

                R.id.nav_home -> {
                    true
                }

                R.id.nav_inventory -> {
                    openPage(InventoryActivity::class.java)
                    true
                }

                R.id.nav_add -> {
                    openPage(AddFoodActivity::class.java)
                    true
                }

                R.id.nav_notification -> {
                    openPage(NotificationActivity::class.java)
                    true
                }

                R.id.nav_profile -> {
                    openPage(ProfileActivity::class.java)
                    true
                }

                else -> false
            }
        }
    }

    private fun openPage(destination: Class<*>) {
        startActivity(Intent(this, destination))
    }

    private fun observeFoodDatabase() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                foodDao.getAllFood().collect { foodList ->
                    updateDashboard(foodList)
                }
            }
        }
    }

    private fun updateDashboard(foodList: List<FoodItem>) {
        tvTotalFood.text = foodList.size.toString()

        val attentionList = foodList.mapNotNull { food ->
            val days = getDaysUntilExpiry(food.expiryDate)
            if (days != null && days <= 3) {
                Pair(food, days)
            } else {
                null
            }
        }.sortedBy { it.second }

        val expiringCount = attentionList.count { it.second >= 0 }
        tvExpiringFood.text = expiringCount.toString()

        alertContainer.removeAllViews()

        if (attentionList.isEmpty()) {
            tvEmptyAlert.visibility = View.VISIBLE
            alertContainer.visibility = View.GONE
        } else {
            tvEmptyAlert.visibility = View.GONE
            alertContainer.visibility = View.VISIBLE

            attentionList.forEach { (food, days) ->
                addAlertItem(food, days)
            }
        }

        recentContainer.removeAllViews()

        val recentList = foodList.take(3)

        if (recentList.isEmpty()) {
            tvEmptyRecent.visibility = View.VISIBLE
            recentContainer.visibility = View.GONE
        } else {
            tvEmptyRecent.visibility = View.GONE
            recentContainer.visibility = View.VISIBLE

            recentList.forEach { food ->
                addRecentItem(food)
            }
        }
    }

    private fun addAlertItem(food: FoodItem, days: Int) {
        val itemLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 10, 0, 10)
        }

        val nameText = TextView(this).apply {
            text = food.name
            textSize = 15f
            setTextColor(android.graphics.Color.rgb(38, 61, 50))
            setTypeface(null, Typeface.BOLD)
        }

        val statusText = TextView(this).apply {
            text = when {
                days < 0 -> "Sudah kedaluwarsa ${-days} hari"
                days == 0 -> "Kedaluwarsa hari ini"
                else -> "Kedaluwarsa dalam $days hari"
            }
            textSize = 13f
            setTextColor(android.graphics.Color.rgb(212, 107, 53))
        }

        itemLayout.addView(nameText)
        itemLayout.addView(statusText)
        alertContainer.addView(itemLayout)
    }

    private fun addRecentItem(food: FoodItem) {
        val itemText = TextView(this).apply {
            text = "${getFoodEmoji(food.category)}  ${food.name}  •  ${food.quantity}"
            textSize = 14f
            setTextColor(android.graphics.Color.rgb(38, 61, 50))
            setPadding(0, 12, 0, 12)
        }

        recentContainer.addView(itemText)
    }

    private fun getFoodEmoji(category: String): String {
        return when (category.lowercase(Locale.getDefault())) {
            "sayuran" -> "🥬"
            "buah" -> "🍎"
            "daging" -> "🥩"
            "ikan" -> "🐟"
            "minuman" -> "🥛"
            "produk susu" -> "🧀"
            "bumbu" -> "🧂"
            else -> "🍱"
        }
    }

    private fun getDaysUntilExpiry(expiryDate: String): Int? {
        return try {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            dateFormat.isLenient = false
            dateFormat.timeZone = TimeZone.getTimeZone("UTC")

            val expiry = dateFormat.parse(expiryDate) ?: return null

            val todayCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            todayCalendar.set(Calendar.HOUR_OF_DAY, 0)
            todayCalendar.set(Calendar.MINUTE, 0)
            todayCalendar.set(Calendar.SECOND, 0)
            todayCalendar.set(Calendar.MILLISECOND, 0)

            val expiryCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            expiryCalendar.time = expiry
            expiryCalendar.set(Calendar.HOUR_OF_DAY, 0)
            expiryCalendar.set(Calendar.MINUTE, 0)
            expiryCalendar.set(Calendar.SECOND, 0)
            expiryCalendar.set(Calendar.MILLISECOND, 0)

            val difference = expiryCalendar.timeInMillis -
                    todayCalendar.timeInMillis

            (difference / (24 * 60 * 60 * 1000)).toInt()

        } catch (e: Exception) {
            null
        }
    }
}
