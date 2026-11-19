
package com.example.fridgefoodsaver

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
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

class InventoryActivity : AppCompatActivity() {

    private lateinit var etSearchFood: EditText
    private lateinit var spFilterCategory: Spinner
    private lateinit var tvItemCount: TextView
    private lateinit var foodListContainer: LinearLayout
    private lateinit var tvEmpty: TextView

    private var allFood = listOf<FoodItem>()

    private val categories = listOf(
        "Semua Kategori",
        "Sayuran",
        "Buah-buahan",
        "Daging",
        "Seafood",
        "Susu dan Olahan",
        "Telur",
        "Minuman",
        "Lainnya"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inventory)

        // Menghubungkan komponen XML
        val tvBack = findViewById<TextView>(R.id.tvBack)
        val btnAddFood = findViewById<Button>(R.id.btnAddFood)

        etSearchFood = findViewById(R.id.etSearchFood)
        spFilterCategory = findViewById(R.id.spFilterCategory)
        tvItemCount = findViewById(R.id.tvItemCount)
        foodListContainer = findViewById(R.id.foodListContainer)
        tvEmpty = findViewById(R.id.tvEmpty)

        // Tombol kembali
        tvBack.setOnClickListener {
            finish()
        }

        // Tombol tambah bahan makanan
        btnAddFood.setOnClickListener {
            startActivity(
                Intent(this, AddFoodActivity::class.java)
            )
        }

        // Adapter kategori
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            categories
        )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spFilterCategory.adapter = adapter

        // Pencarian berdasarkan nama
        etSearchFood.addTextChangedListener(object : TextWatcher {

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
                showFilteredFood()
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Filter berdasarkan kategori
        spFilterCategory.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    showFilteredFood()
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {}
            }

        // Mengamati perubahan database
        observeFoodDatabase()
    }

    // Mengambil dan mengamati data Room
    private fun observeFoodDatabase() {

        lifecycleScope.launch {

            repeatOnLifecycle(Lifecycle.State.STARTED) {

                try {

                    val database = FoodDatabase.getDatabase(
                        applicationContext
                    )

                    database.foodDao().getAllFood().collect { foodList ->

                        allFood = foodList
                        showFilteredFood()
                    }

                } catch (e: Exception) {

                    tvEmpty.visibility = View.VISIBLE
                    tvEmpty.text =
                        "Gagal mengambil data: ${e.message}"
                }
            }
        }
    }

    // Menampilkan data berdasarkan pencarian dan kategori
    private fun showFilteredFood() {

        val keyword = etSearchFood.text.toString().trim()

        val selectedCategory =
            spFilterCategory.selectedItem?.toString()
                ?: "Semua Kategori"

        val filteredFood = allFood.filter { food ->

            val matchesName = food.name.contains(
                keyword,
                ignoreCase = true
            )

            val matchesCategory =
                selectedCategory == "Semua Kategori" ||
                        food.category == selectedCategory

            matchesName && matchesCategory
        }

        tvItemCount.text = "Total bahan: ${filteredFood.size}"

        foodListContainer.removeAllViews()

        if (filteredFood.isEmpty()) {

            tvEmpty.visibility = View.VISIBLE

            tvEmpty.text = if (allFood.isEmpty()) {
                "Belum ada bahan makanan.\nSilakan tambahkan bahan makanan."
            } else {
                "Bahan makanan tidak ditemukan."
            }

        } else {

            tvEmpty.visibility = View.GONE

            filteredFood.forEach { food ->
                foodListContainer.addView(
                    createFoodCard(food)
                )
            }
        }
    }

    // Membuat kartu bahan makanan
    private fun createFoodCard(food: FoodItem): View {

        val card = LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL

            setPadding(18, 16, 18, 16)

            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = 18f
                setStroke(
                    1,
                    Color.rgb(225, 232, 222)
                )
            }

            elevation = 3f
        }

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        params.bottomMargin = 16

        card.layoutParams = params

        // Nama makanan
        val nameText = TextView(this).apply {

            text = food.name
            textSize = 18f

            setTextColor(
                Color.rgb(41, 55, 44)
            )

            setTypeface(
                null,
                Typeface.BOLD
            )
        }

        // Kategori
        val categoryText = TextView(this).apply {

            text = "Kategori: ${food.category}"
            textSize = 14f

            setTextColor(
                Color.rgb(123, 133, 124)
            )

            setPadding(0, 8, 0, 0)
        }

        // Jumlah
        val quantityText = TextView(this).apply {

            text = "Jumlah: ${food.quantity}"
            textSize = 14f

            setTextColor(
                Color.rgb(123, 133, 124)
            )

            setPadding(0, 5, 0, 0)
        }

        // Tanggal kedaluwarsa
        val expiryText = TextView(this).apply {

            text = "Kedaluwarsa: ${food.expiryDate}"
            textSize = 14f

            setTextColor(
                Color.rgb(200, 93, 80)
            )

            setPadding(0, 5, 0, 0)
        }

        card.addView(nameText)
        card.addView(categoryText)
        card.addView(quantityText)
        card.addView(expiryText)

        // Membuka halaman Detail
        card.setOnClickListener {

            val intent = Intent(
                this,
                DetailFoodActivity::class.java
            )

            // Mengirim ID dan data makanan
            intent.putExtra("food_id", food.id)
            intent.putExtra("food_name", food.name)
            intent.putExtra("food_category", food.category)
            intent.putExtra("food_quantity", food.quantity)
            intent.putExtra("food_expiry", food.expiryDate)
            intent.putExtra("food_notes", food.notes)

            startActivity(intent)
        }

        return card
    }
}
