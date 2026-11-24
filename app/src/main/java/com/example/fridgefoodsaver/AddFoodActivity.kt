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

class AddFoodActivity : AppCompatActivity() {

    private lateinit var etFoodName: EditText
    private lateinit var spCategory: Spinner
    private lateinit var etQuantity: EditText
    private lateinit var spUnit: Spinner
    private lateinit var etExpiryDate: EditText
    private lateinit var etNotes: EditText
    private lateinit var btnSaveFood: Button

    private val dateFormat = SimpleDateFormat(
        "dd/MM/yyyy",
        Locale.getDefault()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_food)

        val tvBack = findViewById<TextView>(R.id.tvBack)

        etFoodName = findViewById(R.id.etFoodName)
        spCategory = findViewById(R.id.spCategory)
        etQuantity = findViewById(R.id.etQuantity)
        spUnit = findViewById(R.id.spUnit)
        etExpiryDate = findViewById(R.id.etExpiryDate)
        etNotes = findViewById(R.id.etNotes)
        btnSaveFood = findViewById(R.id.btnSaveFood)

        tvBack.setOnClickListener {
            finish()
        }

        val categories = listOf(
            "Sayuran",
            "Buah-buahan",
            "Daging",
            "Seafood",
            "Susu dan Olahan",
            "Telur",
            "Minuman",
            "Lainnya"
        )

        val categoryAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            categories
        )
        categoryAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )
        spCategory.adapter = categoryAdapter

        val units = listOf(
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

        val unitAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            units
        )
        unitAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )
        spUnit.adapter = unitAdapter

        etExpiryDate.isFocusable = false
        etExpiryDate.isClickable = true

        etExpiryDate.setOnClickListener {
            showDatePicker()
        }

        btnSaveFood.setOnClickListener {
            saveFood()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()

        val datePicker = DatePickerDialog(
            this,
            { _, year, month, day ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(year, month, day)
                etExpiryDate.setText(dateFormat.format(selectedDate.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePicker.show()
    }

    private fun saveFood() {
        val name = etFoodName.text.toString().trim()
        val category = spCategory.selectedItem.toString()
        val quantityValue = etQuantity.text.toString().trim()
        val unit = spUnit.selectedItem.toString()
        val expiryDate = etExpiryDate.text.toString().trim()
        val notes = etNotes.text.toString().trim()

        if (name.isEmpty()) {
            etFoodName.error = "Nama bahan harus diisi"
            etFoodName.requestFocus()
            return
        }

        if (quantityValue.isEmpty()) {
            etQuantity.error = "Jumlah harus diisi"
            etQuantity.requestFocus()
            return
        }

        if (expiryDate.isEmpty()) {
            Toast.makeText(
                this,
                "Pilih tanggal kedaluwarsa",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val food = FoodItem(
            name = name,
            category = category,
            quantity = "$quantityValue $unit",
            expiryDate = expiryDate,
            notes = notes
        )

        btnSaveFood.isEnabled = false
        btnSaveFood.text = "Menyimpan..."

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val database = FoodDatabase.getDatabase(applicationContext)
                    database.foodDao().insertFood(food)
                }

                Toast.makeText(
                    this@AddFoodActivity,
                    "Bahan makanan berhasil disimpan",
                    Toast.LENGTH_SHORT
                ).show()

                finish()
            } catch (e: Exception) {
                btnSaveFood.isEnabled = true
                btnSaveFood.text = "Simpan Bahan Makanan"

                Toast.makeText(
                    this@AddFoodActivity,
                    "Gagal menyimpan: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}