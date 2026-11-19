
package com.example.fridgefoodsaver

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
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

class EditFoodActivity : AppCompatActivity() {

    private var foodId: Int = -1

    private lateinit var etFoodName: EditText
    private lateinit var spCategory: Spinner
    private lateinit var etQuantity: EditText
    private lateinit var spUnit: Spinner
    private lateinit var etExpiryDate: EditText
    private lateinit var etNotes: EditText

    // Daftar kategori makanan
    private val categories = listOf(
        "Sayuran",
        "Buah-buahan",
        "Daging",
        "Seafood",
        "Susu dan Olahan",
        "Telur",
        "Minuman",
        "Lainnya"
    )

    // Daftar satuan makanan
    private val units = listOf(
        "buah",
        "kg",
        "gram",
        "liter",
        "ml",
        "ikat",
        "butir",
        "bungkus",
        "pack"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_food)

        // Menghubungkan komponen XML
        val tvBack = findViewById<TextView>(R.id.tvBack)

        etFoodName = findViewById(R.id.etEditFoodName)
        spCategory = findViewById(R.id.spEditCategory)
        etQuantity = findViewById(R.id.etEditQuantity)
        spUnit = findViewById(R.id.spEditUnit)
        etExpiryDate = findViewById(R.id.etEditExpiryDate)
        etNotes = findViewById(R.id.etEditNotes)

        val btnUpdateFood =
            findViewById<Button>(R.id.btnUpdateFood)

        // Menerima ID makanan
        foodId = intent.getIntExtra("food_id", -1)

        // Menyiapkan Spinner
        setupSpinner()

        // Tombol kembali
        tvBack.setOnClickListener {
            finish()
        }

        // Membuka DatePicker
        etExpiryDate.setOnClickListener {
            showDatePicker()
        }

        etExpiryDate.isFocusable = false

        // Memeriksa ID
        if (foodId == -1) {
            Toast.makeText(
                this,
                "ID makanan tidak ditemukan",
                Toast.LENGTH_SHORT
            ).show()

            finish()
            return
        }

        // Mengambil data lama dari database
        loadFoodData()

        // Tombol simpan perubahan
        btnUpdateFood.setOnClickListener {
            updateFood()
        }
    }

    // Menyiapkan pilihan Spinner
    private fun setupSpinner() {

        val categoryAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            categories
        )

        categoryAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spCategory.adapter = categoryAdapter

        val unitAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            units
        )

        unitAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spUnit.adapter = unitAdapter
    }

    // Mengambil data makanan berdasarkan ID
    private fun loadFoodData() {

        lifecycleScope.launch {

            val food = withContext(Dispatchers.IO) {
                FoodDatabase.getDatabase(applicationContext)
                    .foodDao()
                    .getFoodById(foodId)
            }

            if (food == null) {

                Toast.makeText(
                    this@EditFoodActivity,
                    "Data makanan tidak ditemukan",
                    Toast.LENGTH_SHORT
                ).show()

                finish()
                return@launch
            }

            // Mengisi data ke form
            etFoodName.setText(food.name)
            etExpiryDate.setText(food.expiryDate)
            etNotes.setText(food.notes)

            // Memilih kategori
            val categoryPosition =
                categories.indexOf(food.category)

            if (categoryPosition >= 0) {
                spCategory.setSelection(categoryPosition)
            }

            // Memisahkan jumlah dan satuan
            val quantityParts =
                food.quantity.trim().split(" ", limit = 2)

            etQuantity.setText(quantityParts[0])

            if (quantityParts.size > 1) {

                val unitPosition =
                    units.indexOf(quantityParts[1])

                if (unitPosition >= 0) {
                    spUnit.setSelection(unitPosition)
                }
            }
        }
    }

    // Menampilkan kalender
    private fun showDatePicker() {

        val calendar = Calendar.getInstance()

        val currentDate =
            etExpiryDate.text.toString()

        try {

            val formatter = SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
            )

            formatter.isLenient = false

            val parsedDate = formatter.parse(currentDate)

            if (parsedDate != null) {
                calendar.time = parsedDate
            }

        } catch (_: Exception) {
            // Menggunakan tanggal hari ini.
        }

        val dialog = DatePickerDialog(
            this,
            { _, year, month, day ->

                val selectedDate = String.format(
                    Locale.getDefault(),
                    "%02d/%02d/%04d",
                    day,
                    month + 1,
                    year
                )

                etExpiryDate.setText(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        dialog.show()
    }

    // Memperbarui data makanan
    private fun updateFood() {

        val name = etFoodName.text.toString().trim()

        val category =
            spCategory.selectedItem.toString()

        val quantityValue =
            etQuantity.text.toString().trim()

        val unit =
            spUnit.selectedItem.toString()

        val expiryDate =
            etExpiryDate.text.toString().trim()

        val notes =
            etNotes.text.toString().trim()

        // Validasi nama
        if (name.isEmpty()) {
            etFoodName.error = "Nama makanan harus diisi"
            etFoodName.requestFocus()
            return
        }

        // Validasi jumlah
        if (quantityValue.isEmpty()) {
            etQuantity.error = "Jumlah harus diisi"
            etQuantity.requestFocus()
            return
        }

        val quantityNumber =
            quantityValue.toDoubleOrNull()

        if (quantityNumber == null || quantityNumber <= 0) {
            etQuantity.error = "Masukkan jumlah yang valid"
            etQuantity.requestFocus()
            return
        }

        // Validasi tanggal
        if (expiryDate.isEmpty()) {
            etExpiryDate.error =
                "Tanggal kedaluwarsa harus diisi"
            return
        }

        val formatter = SimpleDateFormat(
            "dd/MM/yyyy",
            Locale.getDefault()
        )

        formatter.isLenient = false

        try {

            formatter.parse(expiryDate)

        } catch (_: Exception) {

            etExpiryDate.error =
                "Format tanggal tidak valid"

            return
        }

        // Membuat data yang sudah diperbarui
        val updatedFood = FoodItem(
            id = foodId,
            name = name,
            category = category,
            quantity = "$quantityValue $unit",
            expiryDate = expiryDate,
            notes = notes
        )

        // Menyimpan perubahan ke Room
        lifecycleScope.launch {

            withContext(Dispatchers.IO) {

                FoodDatabase.getDatabase(applicationContext)
                    .foodDao()
                    .updateFood(updatedFood)
            }

            Toast.makeText(
                this@EditFoodActivity,
                "Data berhasil diperbarui",
                Toast.LENGTH_SHORT
            ).show()

            finish()
        }
    }
}
