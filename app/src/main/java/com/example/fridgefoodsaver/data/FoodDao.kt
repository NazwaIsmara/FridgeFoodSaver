package com.example.fridgefoodsaver.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {

    @Query("SELECT * FROM food_items ORDER BY id DESC")
    fun getAllFood(): Flow<List<FoodItem>>

    @Query("SELECT * FROM food_items WHERE id = :foodId")
    suspend fun getFoodById(foodId: Int): FoodItem?

    @Insert
    suspend fun insertFood(food: FoodItem)

    @Update
    suspend fun updateFood(food: FoodItem)

    @Delete
    suspend fun deleteFood(food: FoodItem)

    @Query("DELETE FROM food_items WHERE id = :foodId")
    suspend fun deleteFoodById(foodId: Int)
}
