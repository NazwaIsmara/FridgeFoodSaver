package com.example.fridgefoodsaver

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.fridgefoodsaver.data.FoodDatabase
import com.example.fridgefoodsaver.data.FoodItem
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DetailFoodActivity : AppCompatActivity() {

    private var foodId: Int = -1
    private var currentFood: FoodItem? = null

    private lateinit var tvFoodEmoji: TextView
    private lateinit var tvFoodName: TextView
    private lateinit var tvFoodStatus: TextView
    private lateinit var tvFoodCategory: TextView
    private lateinit var tvFoodQuantity: TextView
    private lateinit var tvFoodExpiry: TextView
    private lateinit var tvFoodNotes: TextView
    private lateinit var expiryAlertCard: MaterialCardView
    private lateinit var tvExpiryAlertTitle: TextView
    private lateinit var tvExpiryAlertDesc: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_food)

        val btnBackCard = findViewById<MaterialCardView>(R.id.btnBackCard)
        val tvBack = findViewById<TextView>(R.id.tvBack)
        tvFoodEmoji = findViewById(R.id.tvFoodEmoji)
        tvFoodName = findViewById(R.id.tvFoodName)
        tvFoodStatus = findViewById(R.id.tvFoodStatus)
        tvFoodCategory = findViewById(R.id.tvFoodCategory)
        tvFoodQuantity = findViewById(R.id.tvFoodQuantity)
        tvFoodExpiry = findViewById(R.id.tvFoodExpiry)
        tvFoodNotes = findViewById(R.id.tvFoodNotes)
        expiryAlertCard = findViewById(R.id.expiryAlertCard)
        tvExpiryAlertTitle = findViewById(R.id.tvExpiryAlertTitle)
        tvExpiryAlertDesc = findViewById(R.id.tvExpiryAlertDesc)

        val btnEditFood = findViewById<MaterialButton>(R.id.btnEditFood)
        val btnDeleteFood = findViewById<MaterialButton>(R.id.btnDeleteFood)

        styleCategoryPill()

        foodId = intent.getIntExtra("food_id", -1).let { id ->
            if (id != -1) id else intent.getIntExtra("FOOD_ID", -1)
        }

        btnBackCard.setOnClickListener { finish() }
        tvBack.setOnClickListener { finish() }

        btnEditFood.setOnClickListener {
            val food = currentFood
            if (food == null) {
                Toast.makeText(
                    this,
                    getString(R.string.detail_toast_not_ready),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val editIntent = Intent(this, EditFoodActivity::class.java).apply {
                putExtra("food_id", food.id)
                putExtra("food_name", food.name)
                putExtra("food_category", food.category)
                putExtra("food_quantity", food.quantity)
                putExtra("food_expiry", food.expiryDate)
                putExtra("food_notes", food.notes)
            }
            startActivity(editIntent)
        }

        btnDeleteFood.setOnClickListener {
            val food = currentFood
            if (food == null) {
                Toast.makeText(
                    this,
                    getString(R.string.detail_toast_not_ready),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val dialog = MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.detail_dialog_delete_title))
                .setMessage(
                    getString(R.string.detail_dialog_delete_message, food.name)
                )
                .setNegativeButton(getString(R.string.detail_dialog_btn_cancel)) { d, _ ->
                    d.dismiss()
                }
                .setPositiveButton(getString(R.string.detail_dialog_btn_delete)) { _, _ ->
                    deleteFood(food.id)
                }
                .create()

            dialog.show()

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                ?.setTextColor(Color.parseColor("#C62828"))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                ?.setTextColor(Color.parseColor("#3F7D44"))
        }

        if (foodId == -1) {
            Toast.makeText(
                this,
                getString(R.string.detail_toast_missing_id),
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        if (foodId != -1) {
            loadFoodDetail()
        }
    }

    private fun styleCategoryPill() {
        val radiusPx = 14 * resources.displayMetrics.density
        tvFoodCategory.background = GradientDrawable().apply {
            cornerRadius = radiusPx
            setColor(Color.parseColor("#4CA657"))
        }
        tvFoodStatus.background = GradientDrawable().apply {
            cornerRadius = radiusPx
            setColor(Color.parseColor("#FFF8EE"))
        }
    }

    private fun loadFoodDetail() {
        lifecycleScope.launch {
            val food = withContext(Dispatchers.IO) {
                FoodDatabase.getDatabase(applicationContext)
                    .foodDao()
                    .getFoodById(foodId)
            }

            if (food == null) {
                Toast.makeText(
                    this@DetailFoodActivity,
                    getString(R.string.detail_toast_not_found),
                    Toast.LENGTH_SHORT
                ).show()
                finish()
                return@launch
            }

            currentFood = food
            displayFood(food)
        }
    }

    private fun deleteFood(id: Int) {
        lifecycleScope.launch {
            try {
                val database = withContext(Dispatchers.IO) {
                    FoodDatabase.getDatabase(applicationContext)
                }
                withContext(Dispatchers.IO) {
                    database.foodDao().deleteFoodById(id)
                }
                Toast.makeText(
                    this@DetailFoodActivity,
                    getString(R.string.inventory_delete_success),
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(
                    this@DetailFoodActivity,
                    getString(R.string.inventory_delete_error, e.message.orEmpty()),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun displayFood(food: FoodItem) {
        tvFoodEmoji.text = getFoodEmoji(food.name, food.category)
        tvFoodName.text = food.name
        tvFoodCategory.text = food.category
        tvFoodQuantity.text = food.quantity
        tvFoodExpiry.text = food.expiryDate

        tvFoodNotes.text = if (food.notes.isBlank()) {
            getString(R.string.detail_no_notes)
        } else {
            food.notes
        }

        val daysLeft = getDaysLeft(food.expiryDate)
        tvFoodStatus.text = getExpiryStatus(daysLeft)

        when {
            daysLeft == null -> {
                setPillColor(tvFoodStatus, "#EEF2EF")
                tvFoodStatus.setTextColor(Color.parseColor("#78847B"))
                expiryAlertCard.visibility = View.GONE
            }
            daysLeft < 0L -> {
                setPillColor(tvFoodStatus, "#FFF2F1")
                tvFoodStatus.setTextColor(Color.parseColor("#E04F4F"))
                expiryAlertCard.visibility = View.VISIBLE
                expiryAlertCard.setCardBackgroundColor(Color.parseColor("#FFF2F1"))
                expiryAlertCard.strokeColor = Color.parseColor("#F8D5D2")
                tvExpiryAlertTitle.setTextColor(Color.parseColor("#E04F4F"))
                tvExpiryAlertTitle.text = getString(R.string.detail_alert_expired_title)
                tvExpiryAlertDesc.text = getString(R.string.detail_alert_expired_desc)
            }
            daysLeft == 0L -> {
                setPillColor(tvFoodStatus, "#FFF2F1")
                tvFoodStatus.setTextColor(Color.parseColor("#E04F4F"))
                expiryAlertCard.visibility = View.VISIBLE
                expiryAlertCard.setCardBackgroundColor(Color.parseColor("#FFF2F1"))
                expiryAlertCard.strokeColor = Color.parseColor("#F8D5D2")
                tvExpiryAlertTitle.setTextColor(Color.parseColor("#E04F4F"))
                tvExpiryAlertTitle.text = getString(R.string.detail_alert_today_title)
                tvExpiryAlertDesc.text = getString(R.string.detail_alert_today_desc)
            }
            daysLeft <= 3L -> {
                setPillColor(tvFoodStatus, "#FFF8EE")
                tvFoodStatus.setTextColor(Color.parseColor("#D98218"))
                expiryAlertCard.visibility = View.VISIBLE
                expiryAlertCard.setCardBackgroundColor(Color.parseColor("#FFF8EE"))
                expiryAlertCard.strokeColor = Color.parseColor("#F8E2C2")
                tvExpiryAlertTitle.setTextColor(Color.parseColor("#D98218"))
                tvExpiryAlertTitle.text = getString(R.string.detail_alert_soon_title)
                tvExpiryAlertDesc.text = getString(R.string.detail_alert_soon_desc, daysLeft)
            }
            else -> {
                setPillColor(tvFoodStatus, "#EAF6EC")
                tvFoodStatus.setTextColor(Color.parseColor("#3F7D44"))
                expiryAlertCard.visibility = View.GONE
            }
        }
    }

    private fun setPillColor(view: TextView, hexColor: String) {
        val radiusPx = 14 * resources.displayMetrics.density
        view.background = GradientDrawable().apply {
            cornerRadius = radiusPx
            setColor(Color.parseColor(hexColor))
        }
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

    private fun getDaysLeft(expiryDate: String): Long? {
        return try {
            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
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

            (expiryCalendar.timeInMillis - today.timeInMillis) / (24 * 60 * 60 * 1000L)
        } catch (e: Exception) {
            null
        }
    }

    private fun getExpiryStatus(daysLeft: Long?): String {
        if (daysLeft == null) return "Tanggal tidak valid"
        return when {
            daysLeft < 0L -> "Sudah kedaluwarsa"
            daysLeft == 0L -> "Kedaluwarsa hari ini"
            daysLeft <= 3L -> "Segera kedaluwarsa"
            else -> "Masih aman"
        }
    }
}