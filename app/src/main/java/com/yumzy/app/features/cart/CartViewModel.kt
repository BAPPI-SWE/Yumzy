package com.yumzy.app.features.cart

import androidx.lifecycle.ViewModel
import com.yumzy.app.features.home.MenuItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CartItem(
    val menuItem: MenuItem,
    var quantity: Int,
    val restaurantId: String,
    val restaurantName: String
)

class CartViewModel : ViewModel() {
    private val _currentSelection = MutableStateFlow<Map<String, CartItem>>(emptyMap())
    val currentSelection = _currentSelection.asStateFlow()

    private val _savedCart = MutableStateFlow<Map<String, CartItem>>(emptyMap())
    val savedCart = _savedCart.asStateFlow()

    fun addToSelection(item: MenuItem, restaurantId: String, restaurantName: String) {
        _currentSelection.update { selection ->
            val newSelection = selection.toMutableMap()
            newSelection.putIfAbsent(item.id, CartItem(
                menuItem = item,
                quantity = 1,
                restaurantId = restaurantId,
                restaurantName = restaurantName
            ))
            newSelection
        }
    }

    fun incrementSelection(item: MenuItem) {
        _currentSelection.update { selection ->
            val newSelection = selection.toMutableMap()
            val existingItem = newSelection[item.id]
            if (existingItem != null) {
                newSelection[item.id] = existingItem.copy(quantity = existingItem.quantity + 1)
            }
            newSelection
        }
    }

    fun decrementSelection(item: MenuItem) {
        _currentSelection.update { selection ->
            val newSelection = selection.toMutableMap()
            val existingItem = newSelection[item.id]
            if (existingItem != null) {
                if (existingItem.quantity > 1) {
                    newSelection[item.id] = existingItem.copy(quantity = existingItem.quantity - 1)
                } else {
                    newSelection.remove(item.id)
                }
            }
            newSelection
        }
    }

    fun saveSelectionToCart() {
        _savedCart.update { currentSavedCart ->
            val newSavedCart = currentSavedCart.toMutableMap()
            _currentSelection.value.forEach { (itemId, cartItem) ->
                newSavedCart[itemId] = cartItem
            }
            newSavedCart
        }
        clearSelection()
    }

    fun clearSelection() {
        _currentSelection.value = emptyMap()
    }

    fun incrementSavedItem(item: MenuItem) {
        _savedCart.update { savedCart ->
            val newCart = savedCart.toMutableMap()
            val existingItem = newCart[item.id]
            if (existingItem != null) {
                newCart[item.id] = existingItem.copy(quantity = existingItem.quantity + 1)
            }
            newCart
        }
    }

    fun decrementSavedItem(item: MenuItem) {
        _savedCart.update { savedCart ->
            val newCart = savedCart.toMutableMap()
            val existingItem = newCart[item.id]
            if (existingItem != null) {
                if (existingItem.quantity > 1) {
                    newCart[item.id] = existingItem.copy(quantity = existingItem.quantity - 1)
                } else {
                    newCart.remove(item.id)
                }
            }
            newCart
        }
    }

    // NEW: Function to clear all items for a specific restaurant from the cart
    fun clearCartForRestaurant(restaurantId: String) {
        _savedCart.update { savedCart ->
            savedCart.filterValues { it.restaurantId != restaurantId }
        }
    }
}