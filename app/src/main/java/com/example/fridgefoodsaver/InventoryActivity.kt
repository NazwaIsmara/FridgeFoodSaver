package com.example.fridgefoodsaver

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.fridgefoodsaver.data.FoodDatabase
import com.example.fridgefoodsaver.data.FoodItem
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class InventoryActivity : AppCompatActivity() {

    private lateinit var tvInventoryCount: TextView
    private lateinit var tvEmptyInventory: TextView
    private lateinit var inventoryContainer: LinearLayout
    private lateinit var categoryChipsContainer: LinearLayout
    private lateinit var etSearchFood: EditText
    private lateinit var btnInventoryNotification: MaterialCardView
    private lateinit var bottomNavigation: BottomNavigationView

    private var allFoods: List<FoodItem> = emptyList()
    private var selectedCategory: String = "Semua Kategori"
    private val chipViews = mutableListOf<Pair<String, TextView>>()

    private data class CategoryChipItem(
        val chipLabel: String,
        val filterValue: String
    )

    private val chipItems by lazy {
        listOf(
            CategoryChipItem(getString(R.string.inventory_chip_all), "Semua Kategori"),
            CategoryChipItem(getString(R.string.inventory_chip_vegetable), "Sayuran"),
            CategoryChipItem(getString(R.string.inventory_chip_fruit), "Buah-buahan"),
            CategoryChipItem(getString(R.string.inventory_chip_meat), "Daging"),
            CategoryChipItem(getString(R.string.inventory_chip_seafood), "Seafood"),
            CategoryChipItem(getString(R.string.inventory_chip_dairy), "Susu dan Olahan"),
            CategoryChipItem(getString(R.string.inventory_chip_egg), "Telur"),
            CategoryChipItem(getString(R.string.inventory_chip_drink), "Minuman")
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inventory)

        bindViews()
        setupTopActions()
        setupBottomNavigation()
        setupCategoryChips()
        setupSearch()
        applyInitialSearchQuery()
        observeInventory()
    }

    override fun onResume() {
        super.onResume()
        bottomNavigation.selectedItemId = R.id.nav_inventory
    }

    private fun bindViews() {
        tvInventoryCount = findViewById(R.id.tvInventoryCount)
        tvEmptyInventory = findViewById(R.id.tvEmptyInventory)
        inventoryContainer = findViewById(R.id.inventoryContainer)
        categoryChipsContainer = findViewById(R.id.categoryChipsContainer)
        etSearchFood = findViewById(R.id.etSearchFood)
        btnInventoryNotification = findViewById(R.id.btnInventoryNotification)
        bottomNavigation = findViewById(R.id.bottomNavigation)
    }

    private fun setupTopActions() {
        btnInventoryNotification.setOnClickListener {
            openPage(NotificationActivity::class.java)
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigation.selectedItemId = R.id.nav_inventory

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inventory -> true
                R.id.nav_home -> {
                    openPage(DashboardActivity::class.java)
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

    private fun setupCategoryChips() {
        val density = resources.displayMetrics.density
        fun dp(value: Int): Int = (value * density).toInt()

        categoryChipsContainer.removeAllViews()
        chipViews.clear()

        chipItems.forEachIndexed { index, item ->
            val chip = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    dp(34)
                ).apply {
                    if (index < chipItems.lastIndex) {
                        marginEnd = dp(8)
                    }
                }
                gravity = Gravity.CENTER
                text = item.chipLabel
                textSize = 13f
                setPadding(dp(16), 0, dp(16), 0)
                setOnClickListener {
                    selectCategory(item.filterValue)
                }
            }

            categoryChipsContainer.addView(chip)
            chipViews.add(item.filterValue to chip)
        }

        updateChipStyles()
    }

    private fun selectCategory(categoryFilter: String) {
        selectedCategory = categoryFilter
        updateChipStyles()
        applyFilters()
    }

    private fun updateChipStyles() {
        val density = resources.displayMetrics.density
        val radiusPx = (17 * density)

        chipViews.forEach { (filterValue, chip) ->
            val isSelected = filterValue.equals(selectedCategory, ignoreCase = true)
            chip.setTypeface(null, if (isSelected) Typeface.BOLD else Typeface.NORMAL)
            chip.setTextColor(
                if (isSelected) {
                    getColor(R.color.pure_white)
                } else {
                    getColor(R.color.text_dark)
                }
            )
            chip.background = GradientDrawable().apply {
                cornerRadius = radiusPx
                setColor(
                    if (isSelected) {
                        Color.parseColor("#3F7D44")
                    } else {
                        getColor(R.color.light_sage)
                    }
                )
            }
        }
    }

    private fun applyInitialSearchQuery() {
        val initialQuery = intent.getStringExtra("SEARCH_QUERY").orEmpty()
        if (initialQuery.isNotBlank()) {
            etSearchFood.setText(initialQuery)
            etSearchFood.setSelection(initialQuery.length)
        }
    }

    private fun setupSearch() {
        etSearchFood.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) = Unit

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                applyFilters()
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun observeInventory() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                val database = withContext(Dispatchers.IO) {
                    FoodDatabase.getDatabase(applicationContext)
                }

                database.foodDao().getAllFood().collect { foodList ->
                    allFoods = foodList
                    applyFilters()
                }
            }
        }
    }

    private fun applyFilters() {
        val keyword = etSearchFood.text.toString()
            .trim()
            .lowercase(Locale.ROOT)

        val filteredFoods = allFoods.filter { food ->
            val matchesKeyword = keyword.isEmpty() ||
                    food.name.lowercase(Locale.ROOT).contains(keyword) ||
                    food.category.lowercase(Locale.ROOT).contains(keyword)

            val matchesCategory = selectedCategory == "Semua Kategori" ||
                    food.category.equals(selectedCategory, ignoreCase = true)

            matchesKeyword && matchesCategory
        }

        displayInventory(filteredFoods)
    }

    private fun displayInventory(foodList: List<FoodItem>) {
        tvInventoryCount.text = getString(
            R.string.inventory_count_format,
            foodList.size,
            allFoods.size
        )

        inventoryContainer.removeAllViews()

        if (foodList.isEmpty()) {
            tvEmptyInventory.visibility = View.VISIBLE
            tvEmptyInventory.text = if (allFoods.isEmpty()) {
                getString(R.string.inventory_empty_all)
            } else {
                getString(R.string.inventory_empty_filtered)
            }
            return
        }

        tvEmptyInventory.visibility = View.GONE

        foodList.forEach { food ->
            addFoodCard(food)
        }
    }

    private fun addFoodCard(food: FoodItem) {
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
            strokeColor = Color.parseColor("#DCE7DC")
            setCardBackgroundColor(getColor(R.color.pure_white))
            setOnClickListener { openFoodDetail(food) }
        }

        val rowLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }

        val iconBox = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(50), dp(50))
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
            textSize = 24f
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

        val nameView = TextView(this).apply {
            text = food.name
            textSize = 15f
            setTextColor(getColor(R.color.text_dark))
            setTypeface(null, Typeface.BOLD)
            maxLines = 1
        }

        val metaView = TextView(this).apply {
            text = getString(
                R.string.inventory_card_meta_format,
                food.category,
                food.quantity
            )
            textSize = 12f
            setTextColor(getColor(R.color.text_gray))
            setPadding(0, dp(2), 0, 0)
            maxLines = 1
        }

        val expiryView = TextView(this).apply {
            text = getString(
                R.string.inventory_expiry_label_inline,
                food.expiryDate
            )
            textSize = 11f
            setTextColor(getColor(R.color.text_muted))
            setPadding(0, dp(2), 0, 0)
            maxLines = 1
        }

        infoColumn.addView(nameView)
        infoColumn.addView(metaView)
        infoColumn.addView(expiryView)

        val daysLeft = getDaysLeft(food.expiryDate)
        val badgeLabel = when {
            daysLeft == null -> getString(R.string.expiry_status_invalid)
            daysLeft < 0 -> getString(R.string.expiry_badge_expired_days, -daysLeft)
            daysLeft == 0 -> getString(R.string.expiry_badge_today)
            daysLeft == 1 -> getString(R.string.expiry_badge_one_day)
            else -> getString(R.string.expiry_badge_days, daysLeft)
        }

        val badgeBgColor = when {
            daysLeft == null -> Color.parseColor("#8A968D")
            daysLeft < 0 -> getColor(R.color.expiry_red)
            daysLeft == 0 -> getColor(R.color.expiry_red)
            daysLeft <= 3 -> getColor(R.color.warm_orange)
            else -> Color.parseColor("#57B863")
        }

        val rightColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
        }

        val badgeView = TextView(this).apply {
            text = badgeLabel
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setTextColor(getColor(R.color.pure_white))
            setPadding(dp(10), dp(4), dp(10), dp(4))
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(badgeBgColor)
            }
        }

        val actionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, 0)
        }

        val btnEditIcon = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(26), dp(26)).apply {
                marginEnd = dp(8)
            }
            contentDescription = getString(R.string.inventory_btn_edit)
            setImageResource(android.R.drawable.ic_menu_edit)
            setColorFilter(Color.parseColor("#3F7D44"))
            setPadding(dp(4), dp(4), dp(4), dp(4))
            setOnClickListener {
                val intent = Intent(
                    this@InventoryActivity,
                    EditFoodActivity::class.java
                )
                intent.putExtra("food_id", food.id)
                intent.putExtra("food_name", food.name)
                intent.putExtra("food_category", food.category)
                intent.putExtra("food_quantity", food.quantity)
                intent.putExtra("food_expiry", food.expiryDate)
                intent.putExtra("food_notes", food.notes)
                startActivity(intent)
            }
        }

        val btnDeleteIcon = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(26), dp(26))
            contentDescription = getString(R.string.inventory_btn_delete)
            setImageResource(android.R.drawable.ic_menu_delete)
            setColorFilter(getColor(R.color.expiry_red))
            setPadding(dp(4), dp(4), dp(4), dp(4))
            setOnClickListener { deleteFood(food) }
        }

        actionRow.addView(btnEditIcon)
        actionRow.addView(btnDeleteIcon)

        rightColumn.addView(badgeView)
        rightColumn.addView(actionRow)

        rowLayout.addView(iconBox)
        rowLayout.addView(infoColumn)
        rowLayout.addView(rightColumn)
        card.addView(rowLayout)
        inventoryContainer.addView(card)
    }

    private fun openFoodDetail(food: FoodItem) {
        val intent = Intent(
            this@InventoryActivity,
            DetailFoodActivity::class.java
        )
        intent.putExtra("food_id", food.id)
        intent.putExtra("FOOD_ID", food.id)
        startActivity(intent)
    }

    private fun deleteFood(food: FoodItem) {
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.inventory_delete_title))
            .setMessage(
                getString(R.string.inventory_delete_message, food.name)
            )
            .setNegativeButton(getString(R.string.inventory_btn_cancel)) { d, _ ->
                d.dismiss()
            }
            .setPositiveButton(getString(R.string.inventory_btn_delete)) { _, _ ->
                lifecycleScope.launch {
                    try {
                        val database = withContext(Dispatchers.IO) {
                            FoodDatabase.getDatabase(applicationContext)
                        }

                        withContext(Dispatchers.IO) {
                            database.foodDao().deleteFood(food)
                        }

                        Toast.makeText(
                            this@InventoryActivity,
                            getString(R.string.inventory_delete_success),
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (e: Exception) {
                        Toast.makeText(
                            this@InventoryActivity,
                            getString(
                                R.string.inventory_delete_error,
                                e.message.orEmpty()
                            ),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .create()

        dialog.show()

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
            ?.setTextColor(Color.parseColor("#C62828"))
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)
            ?.setTextColor(Color.parseColor("#3F7D44"))
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

    private fun getDaysLeft(expiryDate: String): Int? {
        return try {
            val formatter = SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
            ).apply {
                isLenient = false
            }

            val expiry = formatter.parse(expiryDate) ?: return null

            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val expiryCalendar = Calendar.getInstance().apply {
                time = expiry
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val difference = expiryCalendar.timeInMillis - today.timeInMillis
            (difference / (24 * 60 * 60 * 1000L)).toInt()
        } catch (e: Exception) {
            null
        }
    }
}