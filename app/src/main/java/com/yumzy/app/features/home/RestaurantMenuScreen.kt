package com.yumzy.app.features.home

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.yumzy.app.features.cart.CartViewModel

// Data classes need an ID now to uniquely identify them in the cart
data class PreOrderCategory(
    val id: String,
    val name: String,
    val startTime: String,
    val endTime: String,
    val deliveryTime: String
)

data class MenuItem(
    val id: String,
    val name: String,
    val price: Double,
    val category: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantMenuScreen(
    restaurantId: String,
    restaurantName: String,
    cartViewModel: CartViewModel = viewModel(),
    onCategoryClick: (restaurantId: String, categoryName: String) -> Unit
) {
    var preOrderCategories by remember { mutableStateOf<List<PreOrderCategory>>(emptyList()) }
    var currentMenuItems by remember { mutableStateOf<List<MenuItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current

    // Collect the temporary selection from the ViewModel
    val cartSelection by cartViewModel.currentSelection.collectAsState()

    LaunchedEffect(key1 = restaurantId) {
        val db = Firebase.firestore
        val restaurantRef = db.collection("restaurants").document(restaurantId)

        // Fetch pre-order categories
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

        // Fetch "Current Menu" items
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
        topBar = { TopAppBar(title = { Text(restaurantName) }) },
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
                // Display clickable Pre-Order category cards
                items(preOrderCategories) { category ->
                    PreOrderHeader(
                        category = category,
                        onClick = {
                            onCategoryClick(restaurantId, "Pre-order ${category.name}")
                        }
                    )
                }

                // Display "Current Menu" items directly
                if (currentMenuItems.isNotEmpty()) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Current Menu", style = MaterialTheme.typography.headlineSmall)
                            Spacer(Modifier.width(8.dp))
                            Chip(label = "Available")
                        }
                    }
                    items(currentMenuItems) { menuItem ->
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
}


@Composable
fun PreOrderHeader(category: PreOrderCategory, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(category.name, style = MaterialTheme.typography.titleLarge)
                Text("Order: ${category.startTime} - ${category.endTime}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                Text("Delivery: ${category.deliveryTime}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = "View ${category.name} menu")
        }
    }
}

@Composable
fun MenuItemRow(
    menuItem: MenuItem,
    quantity: Int,
    onAddClick: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    // This Card gives each item a nice border and shadow, similar to the video
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(menuItem.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text("BDT ${menuItem.price}", color = Color.Gray)
            }
            Spacer(Modifier.width(16.dp))

            // The new, more attractive quantity selector
            QuantitySelector(
                quantity = quantity,
                onAdd = onAddClick,
                onIncrement = onIncrement,
                onDecrement = onDecrement
            )
        }
    }
}

// New Reusable Composable for the quantity buttons
@Composable
fun QuantitySelector(
    quantity: Int,
    onAdd: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    if (quantity == 0) {
        Button(
            onClick = onAdd,
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.size(40.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add to cart")
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Outlined button for the "-" icon
            OutlinedButton(
                onClick = onDecrement,
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrement quantity")
            }
            Text("$quantity", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            // Filled button for the "+" icon
            Button(
                onClick = onIncrement,
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increment quantity")
            }
        }
    }
}
@Composable
fun BottomBarWithTwoButtons(onAddToCartClick: () -> Unit, onPlaceOrderClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onAddToCartClick,
                modifier = Modifier.height(50.dp)
            ) {
                Icon(Icons.Default.ShoppingCart, contentDescription = "Add to Cart")
            }
            Button(
                onClick = onPlaceOrderClick,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            ) {
                Text("Place Order Now", fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun Chip(label: String) {
    Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(8.dp)) {
        Text(
            text = label,
            color = Color(0xFF2E7D32),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}