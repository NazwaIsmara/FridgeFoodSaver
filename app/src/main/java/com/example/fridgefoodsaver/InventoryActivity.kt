package com.example.fridgefoodsaver

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
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

class InventoryActivity : AppCompatActivity() {

    private lateinit var tvInventoryCount: TextView
    private lateinit var tvEmptyInventory: TextView
    private lateinit var inventoryContainer: LinearLayout
    private lateinit var etSearchFood: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var spinnerExpiry: Spinner

    private var allFoods: List<FoodItem> = emptyList()

    private val categories = listOf(
        "Semua Kategori",
        "Sayuran",
        "Buah-buahan",
        "Daging",
        "Seafood",
        "Susu dan Olahan",
        "Telur",
        "Minuman"
    )

    private val expiryFilters = listOf(
        "Semua Status",
        "Masih Aman",
        "Segera Kedaluwarsa",
        "Kedaluwarsa Hari Ini",
        "Sudah Kedaluwarsa"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inventory)

        tvInventoryCount = findViewById(R.id.tvInventoryCount)
        tvEmptyInventory = findViewById(R.id.tvEmptyInventory)
        inventoryContainer = findViewById(R.id.inventoryContainer)
        etSearchFood = findViewById(R.id.etSearchFood)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        spinnerExpiry = findViewById(R.id.spinnerExpiry)

        findViewById<Button>(R.id.btnInventoryBack)
            .setOnClickListener {
                finish()
            }

        setupSpinners()
        setupSearch()
        observeInventory()
    }

    private fun setupSpinners() {

        val categoryAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            categories
        )

        categoryAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spinnerCategory.adapter = categoryAdapter

        val expiryAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            expiryFilters
        )

        expiryAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spinnerExpiry.adapter = expiryAdapter

        val listener = object : AdapterView.OnItemSelectedListener {

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                applyFilters()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerCategory.onItemSelectedListener = listener
        spinnerExpiry.onItemSelectedListener = listener
    }

    private fun setupSearch() {

        etSearchFood.addTextChangedListener(
            object : android.text.TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {}

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    applyFilters()
                }

                override fun afterTextChanged(
                    s: android.text.Editable?
                ) {}
            }
        )
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

        val selectedCategory =
            spinnerCategory.selectedItem?.toString()
                ?: "Semua Kategori"

        val selectedExpiry =
            spinnerExpiry.selectedItem?.toString()
                ?: "Semua Status"

        val filteredFoods = allFoods.filter { food ->

            val matchesName = food.name
                .lowercase(Locale.ROOT)
                .contains(keyword)

            val matchesCategory =
                selectedCategory == "Semua Kategori" ||
                        food.category.equals(
                            selectedCategory,
                            ignoreCase = true
                        )

            val status = getExpiryStatus(food.expiryDate)

            val matchesExpiry = when (selectedExpiry) {
                "Semua Status" -> true
                "Masih Aman" -> status == "Masih Aman"
                "Segera Kedaluwarsa" ->
                    status == "Segera Kedaluwarsa"
                "Kedaluwarsa Hari Ini" ->
                    status == "Kedaluwarsa Hari Ini"
                "Sudah Kedaluwarsa" ->
                    status == "Sudah Kedaluwarsa"
                else -> true
            }

            matchesName && matchesCategory && matchesExpiry
        }

        displayInventory(filteredFoods)
    }

    private fun displayInventory(foodList: List<FoodItem>) {

        tvInventoryCount.text =
            "Menampilkan ${foodList.size} dari ${allFoods.size} bahan"

        inventoryContainer.removeAllViews()

        if (foodList.isEmpty()) {

            tvEmptyInventory.visibility = View.VISIBLE

            tvEmptyInventory.text =
                if (allFoods.isEmpty()) {
                    "Belum ada bahan makanan."
                } else {
                    "Bahan makanan tidak ditemukan."
                }

            return
        }

        tvEmptyInventory.visibility = View.GONE

        foodList.forEach { food ->
            addFoodCard(food)
        }
    }

    private fun addFoodCard(food: FoodItem) {

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 18, 20, 18)
            setBackgroundColor(getColor(R.color.pure_white))
        }

        val name = TextView(this).apply {
            text = "${getFoodEmoji(food.category)} ${food.name}"
            textSize = 18f
            setTextColor(getColor(R.color.dark_green))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val category = TextView(this).apply {
            text = "Kategori: ${food.category}"
            textSize = 14f
            setTextColor(getColor(R.color.text_gray))
        }

        val quantity = TextView(this).apply {
            text = "Jumlah: ${food.quantity}"
            textSize = 14f
            setTextColor(getColor(R.color.text_dark))
        }

        val expiry = TextView(this).apply {
            text = "Tanggal kedaluwarsa: ${food.expiryDate}"
            textSize = 14f
            setTextColor(getColor(R.color.warm_orange))
        }

        val status = TextView(this).apply {
            text = "Status: ${getExpiryStatus(food.expiryDate)}"
            textSize = 14f
            setTextColor(getColor(R.color.expiry_red))
        }

        card.addView(name)
        card.addView(category)
        card.addView(quantity)
        card.addView(expiry)
        card.addView(status)

        if (food.notes.isNotBlank()) {

            val notes = TextView(this).apply {
                text = "Catatan: ${food.notes}"
                textSize = 13f
                setTextColor(getColor(R.color.text_gray))
            }

            card.addView(notes)
        }


        val btnDetail = Button(this).apply {
            text = "Lihat Detail"
            isAllCaps = false

            setOnClickListener {
                val intent = Intent(
                    this@InventoryActivity,
                    DetailFoodActivity::class.java
                )

                intent.putExtra("food_id", food.id)
                startActivity(intent)
            }
        }


        val btnEdit = Button(this).apply {
            text = "Edit Bahan"
            isAllCaps = false

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


        val btnDelete = Button(this).apply {
            text = "Hapus"
            isAllCaps = false

            setOnClickListener {
                deleteFood(food)
            }
        }

        card.addView(btnDetail)
        card.addView(btnEdit)
        card.addView(btnDelete)

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        params.bottomMargin = 14

        inventoryContainer.addView(card, params)
    }

    private fun deleteFood(food: FoodItem) {

        AlertDialog.Builder(this)
            .setTitle("Hapus Bahan Makanan")
            .setMessage(
                "Apakah kamu yakin ingin menghapus ${food.name}?"
            )
            .setNegativeButton("Batal", null)
            .setPositiveButton("Hapus") { _, _ ->

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
                            "Bahan berhasil dihapus",
                            Toast.LENGTH_SHORT
                        ).show()

                    } catch (e: Exception) {

                        Toast.makeText(
                            this@InventoryActivity,
                            "Gagal menghapus: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .show()
    }

    private fun getFoodEmoji(category: String): String {

        return when (category.lowercase(Locale.ROOT)) {
            "sayuran" -> "🥬"
            "buah-buahan" -> "🍎"
            "daging" -> "🥩"
            "seafood" -> "🐟"
            "susu dan olahan" -> "🥛"
            "telur" -> "🥚"
            "minuman" -> "🥤"
            else -> "🍱"
        }
    }

    private fun getExpiryStatus(expiryDate: String): String {

        return try {

            val formatter = SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
            )

            formatter.isLenient = false

            val expiry = formatter.parse(expiryDate)
                ?: return "Tanggal tidak valid"

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

            val difference =
                expiryCalendar.timeInMillis - today.timeInMillis

            val daysLeft =
                difference / (24 * 60 * 60 * 1000L)

            when {
                daysLeft < 0 -> "Sudah Kedaluwarsa"
                daysLeft == 0L -> "Kedaluwarsa Hari Ini"
                daysLeft <= 3 -> "Segera Kedaluwarsa"
                else -> "Masih Aman"
            }

        } catch (e: Exception) {
            "Tanggal tidak valid"
        }
    }
}
