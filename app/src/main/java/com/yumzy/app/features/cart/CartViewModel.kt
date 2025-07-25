package com.yumzy.app.features.cart

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yumzy.app.features.home.MenuItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CartItem(
    val menuItem: MenuItem,
    var quantity: Int,
    val restaurantId: String,
    val restaurantName: String
)

class CartViewModel(application: Application) : AndroidViewModel(application) {

    // Room DAO
    private val cartDao = AppDatabase.getDatabase(application).cartDao()

    // Saved Cart from Room DB
    val savedCart = cartDao.getCartItems()
        .map { entityList ->
            entityList.associate { entity ->
                entity.menuItemId to CartItem(
                    menuItem = MenuItem(
                        id = entity.menuItemId,
                        name = entity.itemName,
                        price = entity.itemPrice,
                        category = entity.category
                    ),
                    quantity = entity.quantity,
                    restaurantId = entity.restaurantId,
                    restaurantName = entity.restaurantName
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // In-memory cart selection (temporary)
    private val _currentSelection = MutableStateFlow<Map<String, CartItem>>(emptyMap())
    val currentSelection = _currentSelection.asStateFlow()

    // Add item to temporary selection
    fun addToSelection(item: MenuItem, restaurantId: String, restaurantName: String) {
        _currentSelection.update { selection ->
            val newSelection = selection.toMutableMap()
            newSelection.putIfAbsent(
                item.id,
                CartItem(
                    menuItem = item,
                    quantity = 1,
                    restaurantId = restaurantId,
                    restaurantName = restaurantName
                )
            )
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

    fun clearSelection() {
        _currentSelection.value = emptyMap()
    }

    // Save selection to Room DB
    fun saveSelectionToCart() {
        viewModelScope.launch {
            _currentSelection.value.values.forEach { cartItem ->
                val entity = CartItemEntity(
                    menuItemId = cartItem.menuItem.id,
                    itemName = cartItem.menuItem.name,
                    itemPrice = cartItem.menuItem.price,
                    quantity = cartItem.quantity,
                    restaurantId = cartItem.restaurantId,
                    restaurantName = cartItem.restaurantName,
                    category = cartItem.menuItem.category
                )
                cartDao.insertItem(entity)
            }
            clearSelection()
        }
    }

    fun incrementSavedItem(item: MenuItem) {
        viewModelScope.launch {
            val existingItem = cartDao.getItemById(item.id)
            if (existingItem != null) {
                cartDao.updateItem(existingItem.copy(quantity = existingItem.quantity + 1))
            }
        }
    }

    fun decrementSavedItem(item: MenuItem) {
        viewModelScope.launch {
            val existingItem = cartDao.getItemById(item.id)
            if (existingItem != null) {
                if (existingItem.quantity > 1) {
                    cartDao.updateItem(existingItem.copy(quantity = existingItem.quantity - 1))
                } else {
                    cartDao.deleteItemById(item.id)
                }
            }
        }
    }

    fun clearCartForRestaurant(restaurantId: String) {
        viewModelScope.launch {
            cartDao.clearCartForRestaurant(restaurantId)
        }
    }
}
