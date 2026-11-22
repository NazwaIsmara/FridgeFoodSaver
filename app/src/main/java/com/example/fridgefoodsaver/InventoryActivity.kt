package com.example.fridgefoodsaver

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.content.Intent
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

class InventoryActivity : AppCompatActivity() {

    private lateinit var tvInventoryCount: TextView
    private lateinit var tvEmptyInventory: TextView
    private lateinit var inventoryContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_inventory)

        tvInventoryCount = findViewById(R.id.tvInventoryCount)
        tvEmptyInventory = findViewById(R.id.tvEmptyInventory)
        inventoryContainer = findViewById(R.id.inventoryContainer)

        findViewById<Button>(R.id.btnInventoryBack)
            .setOnClickListener {
                finish()
            }

        observeInventory()
    }

    private fun observeInventory() {

        lifecycleScope.launch {

            repeatOnLifecycle(Lifecycle.State.STARTED) {

                val database = withContext(Dispatchers.IO) {
                    FoodDatabase.getDatabase(applicationContext)
                }

                database.foodDao().getAllFood().collect { foodList ->
                    displayInventory(foodList)
                }
            }
        }
    }

    private fun displayInventory(foodList: List<FoodItem>) {

        tvInventoryCount.text = "Total bahan: ${foodList.size}"

        inventoryContainer.removeAllViews()

        if (foodList.isEmpty()) {
            tvEmptyInventory.visibility = View.VISIBLE
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

        card.addView(name)
        card.addView(category)
        card.addView(quantity)
        card.addView(expiry)


        if (food.notes.isNotBlank()) {

            val notes = TextView(this).apply {
                text = "Catatan: ${food.notes}"
                textSize = 13f
                setTextColor(getColor(R.color.text_gray))
            }

            card.addView(notes)
        }


        val buttonContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }


        val btnDetail = Button(this).apply {

            text = "Detail"
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

            text = "Edit"
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


        buttonContainer.addView(btnDetail)
        buttonContainer.addView(btnEdit)
        buttonContainer.addView(btnDelete)

        card.addView(buttonContainer)


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
            .setMessage("Apakah kamu yakin ingin menghapus ${food.name}?")
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
                            "Gagal menghapus bahan: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .show()
    }


    private fun getFoodEmoji(category: String): String {

        return when (category) {
            "Sayuran" -> "🥬"
            "Buah-buahan" -> "🍎"
            "Daging" -> "🥩"
            "Seafood" -> "🐟"
            "Susu dan Olahan" -> "🥛"
            "Telur" -> "🥚"
            "Minuman" -> "🥤"
            else -> "🍱"
        }
    }
}
