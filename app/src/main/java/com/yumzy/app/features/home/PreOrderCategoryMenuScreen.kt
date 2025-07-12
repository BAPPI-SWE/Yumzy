package com.yumzy.app.features.home

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    restaurantName: String, // We now need this here
    categoryName: String,
    cartViewModel: CartViewModel = viewModel()
) {
    var menuItems by remember { mutableStateOf<List<MenuItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val cartSelection by cartViewModel.currentSelection.collectAsState()

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
                visible = cartSelection.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                BottomBarWithTwoButtons(
                    onAddToCartClick = {
                        cartViewModel.saveSelectionToCart()
                        Toast.makeText(context, "Items added to main cart!", Toast.LENGTH_SHORT).show()
                    },
                    onPlaceOrderClick = {
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
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(menuItems) { menuItem ->
                    MenuItemRow(
                        menuItem = menuItem,
                        quantity = cartSelection[menuItem.id]?.quantity ?: 0,
                        onAddClick = { cartViewModel.addToSelection(menuItem, restaurantId, restaurantName) },
                        onIncrement = { cartViewModel.incrementSelection(menuItem) },
                        onDecrement = { cartViewModel.decrementSelection(menuItem) }
                    )
                }
            }
        }
    }
}