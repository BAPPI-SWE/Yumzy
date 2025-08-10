package com.yumzy.userapp.features.cart

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cart_items")
data class CartItemEntity(
    @PrimaryKey val menuItemId: String,
    val itemName: String,
    val itemPrice: Double,
    val quantity: Int,
    val restaurantId: String,
    val restaurantName: String,
    val category: String
)