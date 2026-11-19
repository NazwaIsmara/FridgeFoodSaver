
package com.example.fridgefoodsaver

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.fridgefoodsaver.data.FoodDatabase
import com.example.fridgefoodsaver.data.FoodItem
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_food)

        // Menghubungkan komponen XML
        val tvBack = findViewById<TextView>(R.id.tvBack)

        tvFoodEmoji = findViewById(R.id.tvFoodEmoji)
        tvFoodName = findViewById(R.id.tvFoodName)
        tvFoodStatus = findViewById(R.id.tvFoodStatus)
        tvFoodCategory = findViewById(R.id.tvFoodCategory)
        tvFoodQuantity = findViewById(R.id.tvFoodQuantity)
        tvFoodExpiry = findViewById(R.id.tvFoodExpiry)
        tvFoodNotes = findViewById(R.id.tvFoodNotes)

        val btnEditFood = findViewById<Button>(R.id.btnEditFood)
        val btnDeleteFood = findViewById<Button>(R.id.btnDeleteFood)

        // Menerima ID dari halaman Inventory
        foodId = intent.getIntExtra("food_id", -1)

        // Tombol kembali
        tvBack.setOnClickListener {
            finish()
        }

        // Tombol edit
        btnEditFood.setOnClickListener {
            val food = currentFood

            if (food == null) {
                Toast.makeText(
                    this,
                    "Data makanan belum tersedia",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val editIntent = Intent(
                this,
                EditFoodActivity::class.java
            )

            editIntent.putExtra("food_id", food.id)
            editIntent.putExtra("food_name", food.name)
            editIntent.putExtra("food_category", food.category)
            editIntent.putExtra("food_quantity", food.quantity)
            editIntent.putExtra("food_expiry", food.expiryDate)
            editIntent.putExtra("food_notes", food.notes)

            startActivity(editIntent)
        }

        // Tombol hapus
        btnDeleteFood.setOnClickListener {
            val food = currentFood

            if (food == null) {
                Toast.makeText(
                    this,
                    "Data makanan belum tersedia",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            AlertDialog.Builder(this)
                .setTitle("Hapus Bahan Makanan")
                .setMessage(
                    "Apakah kamu yakin ingin menghapus ${food.name}?"
                )
                .setNegativeButton("Batal", null)
                .setPositiveButton("Hapus") { _, _ ->
                    deleteFood(food.id)
                }
                .show()
        }

        // Periksa ID
        if (foodId == -1) {
            Toast.makeText(
                this,
                "ID bahan makanan tidak ditemukan",
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

    // Mengambil data dari database
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
                    "Data makanan tidak ditemukan",
                    Toast.LENGTH_SHORT
                ).show()

                finish()
                return@launch
            }

            currentFood = food
            displayFood(food)
        }
    }

    // Menampilkan data ke halaman Detail
    private fun displayFood(food: FoodItem) {

        tvFoodEmoji.text = getFoodEmoji(food.category)
        tvFoodName.text = food.name
        tvFoodCategory.text = food.category
        tvFoodQuantity.text = food.quantity
        tvFoodExpiry.text = food.expiryDate

        tvFoodNotes.text = if (food.notes.isBlank()) {
            "Tidak ada catatan."
        } else {
            food.notes
        }

        tvFoodStatus.text = getExpiryStatus(food.expiryDate)
    }

    // Menentukan emoji berdasarkan kategori
    private fun getFoodEmoji(category: String): String {

        return when (category.lowercase(Locale.ROOT)) {
            "sayuran" -> "🥬"
            "buah-buahan" -> "🍓"
            "daging" -> "🥩"
            "seafood" -> "🐟"
            "susu dan olahan" -> "🥛"
            "telur" -> "🥚"
            "minuman" -> "🥤"
            else -> "🍱"
        }
    }

    // Menghitung status kedaluwarsa
    private fun getExpiryStatus(expiryDate: String): String {

        return try {

            val formatter = SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
            )

            formatter.isLenient = false

            val expiry = formatter.parse(expiryDate)
                ?: return "Tanggal tidak valid"

            val today = Calendar.getInstance()
            today.set(Calendar.HOUR_OF_DAY, 0)
            today.set(Calendar.MINUTE, 0)
            today.set(Calendar.SECOND, 0)
            today.set(Calendar.MILLISECOND, 0)

            val expiryCalendar = Calendar.getInstance()
            expiryCalendar.time = expiry
            expiryCalendar.set(Calendar.HOUR_OF_DAY, 0)
            expiryCalendar.set(Calendar.MINUTE, 0)
            expiryCalendar.set(Calendar.SECOND, 0)
            expiryCalendar.set(Calendar.MILLISECOND, 0)

            val difference =
                expiryCalendar.timeInMillis - today.timeInMillis

            val daysLeft =
                difference / (24 * 60 * 60 * 1000)

            when {
                daysLeft < 0 -> "Sudah kedaluwarsa"
                daysLeft == 0L -> "Kedaluwarsa hari ini"
                daysLeft <= 3 -> "Segera kedaluwarsa"
                else -> "Masih aman"
            }

        } catch (e: Exception) {
            "Tanggal tidak valid"
        }
    }

    // Menghapus data dari database
    private fun deleteFood(id: Int) {

        lifecycleScope.launch {

            withContext(Dispatchers.IO) {
                FoodDatabase.getDatabase(applicationContext)
                    .foodDao()
                    .deleteFoodById(id)
            }

            Toast.makeText(
                this@DetailFoodActivity,
                "Bahan makanan berhasil dihapus",
                Toast.LENGTH_SHORT
            ).show()

            finish()
        }
    }
}
