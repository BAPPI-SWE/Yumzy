package com.yumzy.userapp.features.cart

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yumzy.userapp.ui.theme.DeepPink

@OptIn(ExperimentalMaterial3Api::class)
@Composable

fun CartScreen(
    cartViewModel: CartViewModel = viewModel(),
    onPlaceOrder: (restaurantId: String) -> Unit // The signature changes slightly
) {
    val savedCartItems by cartViewModel.savedCart.collectAsState()
    val groupedItems = savedCartItems.values.groupBy { it.restaurantId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Cart") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepPink,
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        if (groupedItems.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("Your cart is empty.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                groupedItems.forEach { (restaurantId, items) ->
                    item {
                        RestaurantCartCard(
                            restaurantName = items.first().restaurantName,
                            items = items,
                            cartViewModel = cartViewModel,
                            onPlaceOrderClicked = {
                                // Just pass the restaurant ID
                                onPlaceOrder(restaurantId)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RestaurantCartCard(
    restaurantName: String,
    items: List<CartItem>,
    cartViewModel: CartViewModel,
    onPlaceOrderClicked: () -> Unit
) {
    val total = items.sumOf { it.menuItem.price * it.quantity }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(restaurantName, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))

            items.forEach { cartItem ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(cartItem.menuItem.name, fontWeight = FontWeight.SemiBold)
                        Text("BDT ${cartItem.menuItem.price}", color = Color.Gray, fontSize = 14.sp)
                    }
                    QuantitySelector(
                        quantity = cartItem.quantity,
                        onAdd = { /* Not used in cart */ },
                        onIncrement = { cartViewModel.incrementSavedItem(cartItem.menuItem) },
                        onDecrement = { cartViewModel.decrementSavedItem(cartItem.menuItem) }
                    )
                }
            }

            Divider(Modifier.padding(vertical = 16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Total:", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.weight(1f))
                Text("BDT $total", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onPlaceOrderClicked,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Place Order", fontSize = 18.sp)
            }
        }
    }
}

// QuantitySelector composable remains the same

@Composable
fun QuantitySelector(
    quantity: Int,
    onAdd: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    if (quantity == 0) {
        Button(onClick = onAdd, shape = CircleShape, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(40.dp)) {
            Icon(Icons.Default.Add, contentDescription = "Add to cart")
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onDecrement, shape = CircleShape, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Remove, contentDescription = "Decrement quantity")
            }
            Text("$quantity", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Button(onClick = onIncrement, shape = CircleShape, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Add, contentDescription = "Increment quantity")
            }
        }
    }
}