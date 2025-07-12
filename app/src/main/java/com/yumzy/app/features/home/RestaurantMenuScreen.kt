package com.yumzy.app.features.home

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
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
import com.yumzy.app.features.cart.CartViewModel
import com.yumzy.app.ui.theme.cardColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantMenuScreen(
    restaurantId: String,
    restaurantName: String,
    cartViewModel: CartViewModel = viewModel(),
    onCategoryClick: (restaurantId: String, restaurantName: String, categoryName: String) -> Unit,
    onBackClicked: () -> Unit
) {
    var preOrderCategories by remember { mutableStateOf<List<PreOrderCategory>>(emptyList()) }
    var currentMenuItems by remember { mutableStateOf<List<MenuItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val cartSelection by cartViewModel.currentSelection.collectAsState()

    LaunchedEffect(key1 = restaurantId) {
        val db = Firebase.firestore
        val restaurantRef = db.collection("restaurants").document(restaurantId)

        restaurantRef.collection("preOrderCategories").get()
            .addOnSuccessListener { snapshot ->
                preOrderCategories = snapshot.documents.mapNotNull { doc ->
                    PreOrderCategory(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        startTime = doc.getString("startTime") ?: "",
                        endTime = doc.getString("endTime") ?: "",
                        deliveryTime = doc.getString("deliveryTime") ?: ""
                    )
                }
            }

        restaurantRef.collection("menuItems")
            .whereEqualTo("category", "Current Menu")
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let {
                    currentMenuItems = it.documents.mapNotNull { doc ->
                        MenuItem(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            price = doc.getDouble("price") ?: 0.0,
                            category = doc.getString("category") ?: ""
                        )
                    }
                }
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(restaurantName) },
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
                        Toast.makeText(context, "Items added to cart!", Toast.LENGTH_SHORT).show()
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (preOrderCategories.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Pre-Order")
                    }
                    itemsIndexed(preOrderCategories) { index, category ->
                        val color = cardColors[index % cardColors.size]
                        PreOrderHeader(
                            category = category,
                            cardColor = color,
                            onClick = {
                                onCategoryClick(restaurantId, restaurantName, "Pre-order ${category.name}")
                            }
                        )
                    }
                }

                if (currentMenuItems.isNotEmpty()) {
                    item {
                        if (preOrderCategories.isNotEmpty()) {
                            Spacer(Modifier.height(16.dp))
                        }
                        SectionHeader(title = "Current Menu")
                    }
                    itemsIndexed(currentMenuItems) { index, menuItem ->
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
}