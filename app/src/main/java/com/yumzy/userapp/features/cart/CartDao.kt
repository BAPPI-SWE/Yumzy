package com.yumzy.userapp.features.cart

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CartDao {
    @Query("SELECT * FROM cart_items")
    fun getCartItems(): Flow<List<CartItemEntity>>

    @Query("SELECT * FROM cart_items WHERE menuItemId = :itemId")
    suspend fun getItemById(itemId: String): CartItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: CartItemEntity)

    @Update
    suspend fun updateItem(item: CartItemEntity)

    @Query("DELETE FROM cart_items WHERE menuItemId = :itemId")
    suspend fun deleteItemById(itemId: String)

    @Query("DELETE FROM cart_items WHERE restaurantId = :restaurantId")
    suspend fun clearCartForRestaurant(restaurantId: String)
}