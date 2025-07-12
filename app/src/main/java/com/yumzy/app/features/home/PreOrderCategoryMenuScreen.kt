package com.yumzy.app.features.home

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.yumzy.app.features.cart.CartViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreOrderCategoryMenuScreen(
    restaurantId: String,
    categoryName: String,
    cartViewModel: CartViewModel = viewModel()
) {
    var menuItems by remember { mutableStateOf<List<MenuItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val cartItems by cartViewModel.cartItems.collectAsState()

    LaunchedEffect(key1 = Unit) {
        Firebase.firestore.collection("restaurants").document(restaurantId)
            .collection("menuItems")
            .whereEqualTo("category", categoryName)
            .get()
            .addOnSuccessListener { snapshot ->
                menuItems = snapshot.documents.mapNotNull { doc ->
                    MenuItem(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        price = doc.getDouble("price") ?: 0.0,
                        category = doc.getString("category") ?: ""
                    )
                }
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(categoryName.removePrefix("Pre-order ").trim()) })
        },
        bottomBar = {
            AnimatedVisibility(
                visible = cartItems.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                BottomBarWithTwoButtons(
                    onAddToCartClick = {
                        // In a real app, this would persist the cart.
                        // For now, it just shows a message.
                        Toast.makeText(context, "Items added to main cart!", Toast.LENGTH_SHORT).show()
                    },
                    onPlaceOrderClick = {
                        // TODO: Navigate to checkout
                        Toast.makeText(context, "Placing Order...", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(menuItems) { menuItem ->
                    MenuItemRow(
                        menuItem = menuItem,
                        quantity = cartItems[menuItem.id]?.quantity ?: 0,
                        onAddClick = { cartViewModel.addToCart(menuItem) },
                        onIncrement = { cartViewModel.incrementQuantity(menuItem) },
                        onDecrement = { cartViewModel.decrementQuantity(menuItem) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}