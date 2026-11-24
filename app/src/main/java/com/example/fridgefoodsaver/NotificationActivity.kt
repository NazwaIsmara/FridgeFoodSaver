package com.example.fridgefoodsaver

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.fridgefoodsaver.data.FoodDatabase
import com.example.fridgefoodsaver.data.FoodItem
import com.example.fridgefoodsaver.notification.NotificationScheduler
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

class NotificationActivity : AppCompatActivity() {

    private lateinit var tvEmptyNotification: TextView
    private lateinit var tvEmptyLog: TextView
    private lateinit var logContainer: LinearLayout
    private lateinit var expiredContainer: LinearLayout
    private lateinit var attentionContainer: LinearLayout

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            NotificationScheduler.scheduleDailyNotification(applicationContext)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        tvEmptyNotification = findViewById(R.id.tvEmptyNotification)
        tvEmptyLog = findViewById(R.id.tvEmptyLog)
        logContainer = findViewById(R.id.logContainer)
        expiredContainer = findViewById(R.id.expiredContainer)
        attentionContainer = findViewById(R.id.attentionContainer)

        setupBottomNavigation()
        requestPermissionAndSchedule()
        displayNotificationLogs()
        observeNotifications()
    }

    override fun onResume() {
        super.onResume()
        displayNotificationLogs()
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_notification

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_notification -> true
                R.id.nav_home -> {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_inventory -> {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_add -> {
                    startActivity(Intent(this, AddFoodActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun requestPermissionAndSchedule() {
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        NotificationScheduler.scheduleDailyNotification(applicationContext)
    }

    private fun displayNotificationLogs() {
        logContainer.removeAllViews()
        val prefs = getSharedPreferences("fridge_notif_logs", Context.MODE_PRIVATE)
        val array = JSONArray(prefs.getString("logs", "[]") ?: "[]")

        if (array.length() == 0) {
            tvEmptyLog.visibility = View.VISIBLE
            logContainer.visibility = View.GONE
            return
        }

        tvEmptyLog.visibility = View.GONE
        logContainer.visibility = View.VISIBLE

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val title = obj.optString("title")
            val message = obj.optString("message")
            val time = obj.optString("time")

            val card = createCardView(
                bgColor = Color.parseColor("#E9F4EA"),
                borderColor = Color.parseColor("#B8D9BC")
            )

            val topRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val titleTv = TextView(this).apply {
                text = title
                textSize = 14f
                setTextColor(getColor(R.color.dark_green))
                setTypeface(null, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val timeTv = TextView(this).apply {
                text = time
                textSize = 11f
                setTextColor(getColor(R.color.text_gray))
            }

            topRow.addView(titleTv)
            topRow.addView(timeTv)

            val msgTv = TextView(this).apply {
                text = message
                textSize = 13f
                setTextColor(getColor(R.color.text_dark))
                setPadding(0, dpToPx(6), 0, 0)
            }

            card.addView(topRow)
            card.addView(msgTv)
            logContainer.addView(card)
        }
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
            val days = getDaysUntilExpiry(food.expiryDate) ?: return@forEach
            when {
                days < 0 -> expiredList.add(Pair(food, days))
                days in 0..3 -> attentionList.add(Pair(food, days))
            }
        }

        expiredList.sortBy { it.second }
        attentionList.sortBy { it.second }

        expiredList.forEach { (food, days) ->
            addNotificationItem(
                container = expiredContainer,
                food = food,
                days = days,
                bgColor = Color.parseColor("#FDECEC"),
                borderColor = Color.parseColor("#F5C2C2")
            )
        }

        attentionList.forEach { (food, days) ->
            addNotificationItem(
                container = attentionContainer,
                food = food,
                days = days,
                bgColor = Color.parseColor("#FFF6E5"),
                borderColor = Color.parseColor("#F6D69B")
            )
        }

        tvEmptyNotification.visibility =
            if (expiredList.isEmpty() && attentionList.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun addNotificationItem(
        container: LinearLayout,
        food: FoodItem,
        days: Int,
        bgColor: Int,
        borderColor: Int
    ) {
        val card = createCardView(
            bgColor = bgColor,
            borderColor = borderColor
        )

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val name = TextView(this).apply {
            text = "${getFoodEmoji(food.category)} ${food.name}"
            textSize = 15f
            setTextColor(getColor(R.color.text_dark))
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val dateBadge = TextView(this).apply {
            text = food.expiryDate
            textSize = 11f
            setTextColor(getColor(R.color.text_gray))
        }

        topRow.addView(name)
        topRow.addView(dateBadge)

        val status = TextView(this).apply {
            text = when {
                days < 0 -> "Sudah kedaluwarsa ${-days} hari lalu"
                days == 0 -> "Kedaluwarsa hari ini"
                days == 1 -> "Kedaluwarsa dalam 1 hari"
                else -> "Kedaluwarsa dalam $days hari"
            }
            textSize = 13f
            setPadding(0, dpToPx(4), 0, 0)
            setTextColor(
                if (days <= 1) getColor(R.color.expiry_red) else getColor(R.color.warm_orange)
            )
        }

        card.addView(topRow)
        card.addView(status)
        container.addView(card)
    }

    private fun createCardView(bgColor: Int, borderColor: Int): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = dpToPx(14)
            setPadding(pad, pad, pad, pad)
            background = GradientDrawable().apply {
                setColor(bgColor)
                cornerRadius = dpToPx(14).toFloat()
                setStroke(dpToPx(1), borderColor)
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(10)
            }
        }
    }

    private fun getDaysUntilExpiry(dateString: String): Int? {
        return try {
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val expiryDate = LocalDate.parse(dateString, formatter)
            val today = LocalDate.now()
            ChronoUnit.DAYS.between(today, expiryDate).toInt()
        } catch (_: Exception) {
            null
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun getFoodEmoji(category: String): String {
        return when (category.lowercase(Locale.ROOT)) {
            "sayuran" -> "🥬"
            "buah-buahan", "buah" -> "🍎"
            "daging" -> "🥩"
            "seafood", "ikan" -> "🐟"
            "susu dan olahan", "produk susu" -> "🥛"
            "telur" -> "🥚"
            "minuman" -> "🥤"
            "bumbu" -> "🧂"
            else -> "🍱"
        }
    }
}