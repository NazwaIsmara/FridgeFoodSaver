package com.example.fridgefoodsaver.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {

    // Mengambil semua data bahan makanan
    @Query("SELECT * FROM food_items ORDER BY id DESC")
    fun getAllFood(): Flow<List<FoodItem>>

    // Mengambil satu bahan berdasarkan ID
    @Query("SELECT * FROM food_items WHERE id = :foodId")
    suspend fun getFoodById(foodId: Int): FoodItem?

    // Menambahkan bahan makanan
    @Insert
    suspend fun insertFood(food: FoodItem)

    // Mengubah data bahan makanan
    @Update
    suspend fun updateFood(food: FoodItem)

    // Menghapus bahan makanan
    @Delete
    suspend fun deleteFood(food: FoodItem)

    // Menghapus berdasarkan ID
    @Query("DELETE FROM food_items WHERE id = :foodId")
    suspend fun deleteFoodById(foodId: Int)
}
