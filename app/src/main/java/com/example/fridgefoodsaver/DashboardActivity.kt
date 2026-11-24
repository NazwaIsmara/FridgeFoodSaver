package com.example.fridgefoodsaver

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
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
import com.example.fridgefoodsaver.notification.ExpiryNotificationHelper
import com.example.fridgefoodsaver.notification.NotificationScheduler
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DashboardActivity : AppCompatActivity() {

    private lateinit var tvTotalFood: TextView
    private lateinit var tvExpiringFood: TextView
    private lateinit var tvExpiredFood: TextView
    private lateinit var etDashboardSearch: EditText
    private lateinit var alertContainer: LinearLayout
    private lateinit var recentContainer: LinearLayout
    private lateinit var tvEmptyAlert: TextView
    private lateinit var tvEmptyRecent: TextView
    private lateinit var tvSeeAllRecent: TextView
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var btnDashboardNotification: MaterialCardView

    private var allFoodCache: List<FoodItem> = emptyList()

    private val foodDao by lazy {
        FoodDatabase.getDatabase(applicationContext).foodDao()
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                setupExpiryReminder()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        bindViews()
        setupTopActions()
        setupSearchInput()
        setupRecentActions()
        setupBottomNavigation()
        requestNotificationPermission()
        observeFoodDatabase()
    }

    override fun onResume() {
        super.onResume()
        bottomNavigation.selectedItemId = R.id.nav_home
    }

    private fun bindViews() {
        tvTotalFood = findViewById(R.id.tvTotalFood)
        tvExpiringFood = findViewById(R.id.tvExpiringFood)
        tvExpiredFood = findViewById(R.id.tvExpiredFood)
        etDashboardSearch = findViewById(R.id.etDashboardSearch)
        alertContainer = findViewById(R.id.alertContainer)
        recentContainer = findViewById(R.id.recentContainer)
        tvEmptyAlert = findViewById(R.id.tvEmptyAlert)
        tvEmptyRecent = findViewById(R.id.tvEmptyRecent)
        tvSeeAllRecent = findViewById(R.id.tvSeeAllRecent)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        btnDashboardNotification = findViewById(R.id.btnDashboardNotification)
    }

    private fun setupTopActions() {
        btnDashboardNotification.setOnClickListener {
            openPage(NotificationActivity::class.java)
        }
    }

    private fun setupSearchInput() {
        etDashboardSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateDashboard(allFoodCache)
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })

        etDashboardSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = etDashboardSearch.text?.toString()?.trim().orEmpty()
                val intent = Intent(this, InventoryActivity::class.java)
                if (query.isNotEmpty()) {
                    intent.putExtra("SEARCH_QUERY", query)
                }
                startActivity(intent)
                finish()
                true
            } else {
                false
            }
        }
    }

    private fun setupRecentActions() {
        tvSeeAllRecent.setOnClickListener {
            openPage(InventoryActivity::class.java)
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigation.selectedItemId = R.id.nav_home

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
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
        if (destination != AddFoodActivity::class.java) {
            finish()
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            val permissionStatus = ContextCompat.checkSelfPermission(
                this,
                permission
            )

            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(permission)
                return
            }
        }

        setupExpiryReminder()
    }

    private fun setupExpiryReminder() {
        val preferences = getSharedPreferences(
            "fridge_settings",
            MODE_PRIVATE
        )

        val reminderEnabled = preferences.getBoolean(
            "reminder_enabled",
            true
        )

        ExpiryNotificationHelper.createNotificationChannel(this)

        if (reminderEnabled) {
            NotificationScheduler.scheduleDailyNotification(
                applicationContext
            )
        } else {
            NotificationScheduler.cancelDailyNotification(
                applicationContext
            )
        }
    }

    private fun observeFoodDatabase() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                foodDao.getAllFood().collect { foodList ->
                    allFoodCache = foodList
                    updateDashboard(foodList)
                }
            }
        }
    }

    private fun updateDashboard(foodList: List<FoodItem>) {
        tvTotalFood.text = foodList.size.toString()

        val allAttention = foodList.mapNotNull { food ->
            val days = getDaysUntilExpiry(food.expiryDate)
            if (days != null && days <= 3) {
                Pair(food, days)
            } else {
                null
            }
        }.sortedBy { it.second }

        val expiringCount = allAttention.count { it.second in 0..3 }
        val expiredCount = allAttention.count { it.second < 0 }

        tvExpiringFood.text = expiringCount.toString()
        tvExpiredFood.text = expiredCount.toString()

        val query = etDashboardSearch.text?.toString()?.trim()?.lowercase(Locale.ROOT).orEmpty()

        val filteredAttention = if (query.isEmpty()) {
            allAttention
        } else {
            allAttention.filter { (food, _) ->
                food.name.lowercase(Locale.ROOT).contains(query) ||
                        food.category.lowercase(Locale.ROOT).contains(query)
            }
        }

        alertContainer.removeAllViews()
        if (filteredAttention.isEmpty()) {
            tvEmptyAlert.visibility = View.VISIBLE
            alertContainer.visibility = View.GONE
        } else {
            tvEmptyAlert.visibility = View.GONE
            alertContainer.visibility = View.VISIBLE
            filteredAttention.forEachIndexed { index, (food, days) ->
                addAlertItem(food, days, index == filteredAttention.lastIndex)
            }
        }

        val filteredRecent = if (query.isEmpty()) {
            foodList
        } else {
            foodList.filter { food ->
                food.name.lowercase(Locale.ROOT).contains(query) ||
                        food.category.lowercase(Locale.ROOT).contains(query)
            }
        }.take(3)

        recentContainer.removeAllViews()
        if (filteredRecent.isEmpty()) {
            tvEmptyRecent.visibility = View.VISIBLE
            recentContainer.visibility = View.GONE
        } else {
            tvEmptyRecent.visibility = View.GONE
            recentContainer.visibility = View.VISIBLE
            filteredRecent.forEach { food ->
                addRecentItem(food)
            }
        }
    }

    private fun addAlertItem(food: FoodItem, days: Int, isLast: Boolean) {
        val density = resources.displayMetrics.density
        fun dp(value: Int): Int = (value * density).toInt()

        val isUrgent = days <= 0
        val softBgColor = if (isUrgent) {
            Color.parseColor("#FFF2F1")
        } else {
            Color.parseColor("#FFF8EE")
        }
        val softStrokeColor = if (isUrgent) {
            Color.parseColor("#F8D5D2")
        } else {
            Color.parseColor("#F8E2C2")
        }

        val alertCard = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                if (!isLast) {
                    bottomMargin = dp(8)
                }
            }
            radius = dp(12).toFloat()
            cardElevation = 0f
            strokeWidth = dp(1)
            strokeColor = softStrokeColor
            setCardBackgroundColor(softBgColor)
            setOnClickListener { openFoodDetail(food) }
        }

        val textColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }

        val nameText = TextView(this).apply {
            text = "${getFoodEmoji(food.name, food.category)} ${food.name}"
            textSize = 15f
            setTextColor(getColor(R.color.text_dark))
            setTypeface(null, Typeface.BOLD)
        }

        val statusText = TextView(this).apply {
            text = when {
                days < 0 -> getString(R.string.status_expired_days, -days)
                days == 0 -> getString(R.string.status_today)
                days == 1 -> getString(R.string.status_in_one_day)
                else -> getString(R.string.status_in_days, days)
            }
            textSize = 12f
            setPadding(0, dp(2), 0, 0)
            setTextColor(
                if (isUrgent) {
                    getColor(R.color.expiry_red)
                } else {
                    getColor(R.color.warm_orange)
                }
            )
        }

        textColumn.addView(nameText)
        textColumn.addView(statusText)
        alertCard.addView(textColumn)
        alertContainer.addView(alertCard)
    }

    private fun addRecentItem(food: FoodItem) {
        val density = resources.displayMetrics.density
        fun dp(value: Int): Int = (value * density).toInt()

        val card = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(10)
            }
            radius = dp(16).toFloat()
            cardElevation = 0f
            strokeWidth = dp(1)
            strokeColor = Color.parseColor("#E5EBE6")
            setCardBackgroundColor(Color.parseColor("#FFFFFF"))
            setOnClickListener { openFoodDetail(food) }
        }

        val rowLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }

        val iconBox = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
            radius = dp(12).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(Color.parseColor("#F2F6F1"))
        }

        val emojiView = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            gravity = Gravity.CENTER
            text = getFoodEmoji(food.name, food.category)
            textSize = 22f
        }
        iconBox.addView(emojiView)

        val infoColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginStart = dp(12)
                marginEnd = dp(8)
            }
        }

        val titleView = TextView(this).apply {
            text = food.name
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(getColor(R.color.text_dark))
            maxLines = 1
        }

        val subtitleView = TextView(this).apply {
            text = getString(
                R.string.dashboard_recent_item_format,
                "",
                food.category,
                food.quantity
            ).trim()
            textSize = 12f
            setTextColor(getColor(R.color.text_muted))
            setPadding(0, dp(2), 0, 0)
            maxLines = 1
        }

        infoColumn.addView(titleView)
        infoColumn.addView(subtitleView)

        val days = getDaysUntilExpiry(food.expiryDate)
        val badgeLabel = when {
            days == null -> getString(R.string.status_no_expiry)
            days < 0 -> getString(R.string.dashboard_badge_expired, -days)
            days == 0 -> getString(R.string.dashboard_badge_today)
            days == 1 -> getString(R.string.dashboard_badge_one_day)
            else -> getString(R.string.dashboard_badge_days, days)
        }

        val badgeColor = when {
            days == null -> Color.parseColor("#87948B")
            days <= 0 -> Color.parseColor("#E65A50")
            days <= 3 -> Color.parseColor("#F5A623")
            else -> Color.parseColor("#57B863")
        }

        val badgeView = TextView(this).apply {
            text = badgeLabel
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setPadding(dp(10), dp(5), dp(10), dp(5))
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(badgeColor)
            }
        }

        val arrowView = TextView(this).apply {
            text = "›"
            textSize = 20f
            setTextColor(Color.parseColor("#9AA69E"))
            setPadding(dp(8), 0, dp(2), 0)
        }

        rowLayout.addView(iconBox)
        rowLayout.addView(infoColumn)
        rowLayout.addView(badgeView)
        rowLayout.addView(arrowView)
        card.addView(rowLayout)
        recentContainer.addView(card)
    }

    private fun openFoodDetail(food: FoodItem) {
        val intent = Intent(this, DetailFoodActivity::class.java)
        intent.putExtra("FOOD_ID", food.id)
        intent.putExtra("food_id", food.id)
        startActivity(intent)
    }

    private fun getFoodEmoji(name: String, category: String): String {
        val foodName = name.trim().lowercase(Locale.ROOT)

        val keywordEmoji = when {
            listOf("keju", "cheese", "cheddar", "mozzarella").any { foodName.contains(it) } -> "🧀"
            listOf("susu", "milk", "yogurt", "yoghurt", "mentega", "butter").any { foodName.contains(it) } -> "🥛"
            listOf("telur", "egg").any { foodName.contains(it) } -> "🥚"
            listOf("stroberi", "strawberry", "stoberi").any { foodName.contains(it) } -> "🍓"
            listOf("apel", "apple").any { foodName.contains(it) } -> "🍎"
            listOf("pisang", "banana").any { foodName.contains(it) } -> "🍌"
            listOf("jeruk", "orange").any { foodName.contains(it) } -> "🍊"
            listOf("semangka", "watermelon").any { foodName.contains(it) } -> "🍉"
            listOf("anggur", "grape").any { foodName.contains(it) } -> "🍇"
            listOf("alpukat", "avocado").any { foodName.contains(it) } -> "🥑"
            listOf("tomat", "tomato").any { foodName.contains(it) } -> "🍅"
            listOf("wortel", "carrot").any { foodName.contains(it) } -> "🥕"
            listOf("brokoli", "broccoli").any { foodName.contains(it) } -> "🥦"
            listOf("jagung", "corn").any { foodName.contains(it) } -> "🌽"
            listOf("kentang", "potato").any { foodName.contains(it) } -> "🥔"
            listOf("bawang", "onion", "garlic").any { foodName.contains(it) } -> "🧅"
            listOf("cabai", "cabe", "chili").any { foodName.contains(it) } -> "🌶️"
            listOf("bayam", "kangkung", "sawi", "selada", "kubis", "sayur").any { foodName.contains(it) } -> "🥬"
            listOf("ayam", "chicken").any { foodName.contains(it) } -> "🍗"
            listOf("daging", "sapi", "beef", "steak").any { foodName.contains(it) } -> "🥩"
            listOf("ikan", "fish", "salmon", "tuna", "lele", "nila").any { foodName.contains(it) } -> "🐟"
            listOf("udang", "shrimp").any { foodName.contains(it) } -> "🍤"
            listOf("roti", "bread").any { foodName.contains(it) } -> "🍞"
            listOf("nasi", "rice").any { foodName.contains(it) } -> "🍚"
            listOf("tahu", "tempe").any { foodName.contains(it) } -> "🧈"
            listOf("kopi", "coffee").any { foodName.contains(it) } -> "☕"
            listOf("teh", "tea").any { foodName.contains(it) } -> "🍵"
            listOf("jus", "juice").any { foodName.contains(it) } -> "🧃"
            else -> null
        }

        if (keywordEmoji != null) {
            return keywordEmoji
        }

        return when (category.trim().lowercase(Locale.ROOT)) {
            "sayuran", "sayur" -> "🥬"
            "buah-buahan", "buah" -> "🍎"
            "daging" -> "🥩"
            "seafood", "ikan" -> "🐟"
            "susu dan olahan", "produk susu", "olahan susu" -> "🥛"
            "telur" -> "🥚"
            "minuman" -> "🥤"
            "bumbu" -> "🧂"
            else -> "🍱"
        }
    }

    private fun getDaysUntilExpiry(expiryDate: String): Int? {
        return try {
            val formatter = SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
            ).apply {
                isLenient = false
            }

            val parsedDate = formatter.parse(expiryDate) ?: return null

            val expiryCalendar = Calendar.getInstance().apply {
                time = parsedDate
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

            val difference = expiryCalendar.timeInMillis - todayCalendar.timeInMillis
            (difference / (24 * 60 * 60 * 1000L)).toInt()
        } catch (e: Exception) {
            null
        }
    }
}