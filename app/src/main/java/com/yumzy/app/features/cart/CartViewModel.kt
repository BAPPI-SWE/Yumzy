package com.yumzy.app.features.cart

import androidx.lifecycle.ViewModel
import com.yumzy.app.features.home.MenuItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// This data class will hold an item and its quantity in the cart
data class CartItem(
    val menuItem: MenuItem,
    val quantity: Int
)

class CartViewModel : ViewModel() {
    private val _cartItems = MutableStateFlow<Map<String, CartItem>>(emptyMap())
    val cartItems = _cartItems.asStateFlow()

    fun addToCart(item: MenuItem) {
        _cartItems.update { currentCart ->
            val newCart = currentCart.toMutableMap()
            // If item doesn't exist, add it with quantity 1.
            // If it exists, this does nothing, use incrementQuantity instead.
            newCart.putIfAbsent(item.id, CartItem(menuItem = item, quantity = 1))
            newCart
        }
    }

    fun incrementQuantity(item: MenuItem) {
        _cartItems.update { currentCart ->
            val newCart = currentCart.toMutableMap()
            val existingItem = newCart[item.id]
            if (existingItem != null) {
                newCart[item.id] = existingItem.copy(quantity = existingItem.quantity + 1)
            } else {
                // If for some reason it wasn't there, add it.
                newCart[item.id] = CartItem(menuItem = item, quantity = 1)
            }
            newCart
        }
    }

    fun decrementQuantity(item: MenuItem) {
        _cartItems.update { currentCart ->
            val newCart = currentCart.toMutableMap()
            val existingItem = newCart[item.id]
            if (existingItem != null) {
                // If quantity is 1, remove it. Otherwise, decrement.
                if (existingItem.quantity > 1) {
                    newCart[item.id] = existingItem.copy(quantity = existingItem.quantity - 1)
                } else {
                    newCart.remove(item.id)
                }
            }
            newCart
        }
    }
}