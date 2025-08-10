package com.yumzy.userapp.features.home

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.yumzy.userapp.features.cart.CartViewModel
import com.yumzy.userapp.ui.theme.cardColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreOrderCategoryMenuScreen(
    restaurantId: String,
    restaurantName: String,
    categoryName: String,
    cartViewModel: CartViewModel = viewModel(),
    onBackClicked: () -> Unit,
    // 1. ADD the navigation callback parameter
    onPlaceOrder: (restaurantId: String) -> Unit
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

    Scaffold(modifier = Modifier.navigationBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text(categoryName.removePrefix("Pre-order ").trim()) },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
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
                    // 2. MODIFY the click handler to save and navigate
                    onPlaceOrderClick = {
                        cartViewModel.saveSelectionToCart()
                        onPlaceOrder(restaurantId)
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
                verticalArrangement = Arrangement.spacedBy(12.dp) // Adds space between cards
            ) {
                itemsIndexed(menuItems) { index, menuItem ->
                    val color = cardColors[index % cardColors.size]
                    MenuItemRow(
                        menuItem = menuItem,
                        cardColor = color,
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