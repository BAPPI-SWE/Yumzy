package com.yumzy.userapp.features.cart

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    cartItems: List<CartItem>,
    onConfirmOrder: () -> Unit,
    onBackClicked: () -> Unit
) {
    val itemsSubtotal = cartItems.sumOf { it.menuItem.price * it.quantity }
    // Hardcoded charges for now. These will come from the admin panel later.
    val deliveryCharge = 20.0
    val serviceCharge = 5.0
    val finalTotal = itemsSubtotal + deliveryCharge + serviceCharge

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Confirm Order") },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Delivery Address section
            Text("Delivery Address", style = MaterialTheme.typography.titleMedium)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Daffodil Smart City", fontWeight = FontWeight.SemiBold)
                    Text("Hall 1, Room 101", color = Color.Gray)
                }
            }

            Spacer(Modifier.height(24.dp))

            // Order Summary
            Text("Order Summary", style = MaterialTheme.typography.titleMedium)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    cartItems.forEach { cartItem ->
                        Row(Modifier.fillMaxWidth()) {
                            Text("${cartItem.quantity} x ${cartItem.menuItem.name}", modifier = Modifier.weight(1f))
                            Text("৳${cartItem.menuItem.price * cartItem.quantity}")
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Price Details
            Text("Price Details", style = MaterialTheme.typography.titleMedium)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PriceRow(label = "Items Subtotal", amount = itemsSubtotal)
                    PriceRow(label = "Delivery Charge", amount = deliveryCharge)
                    PriceRow(label = "Service Charge", amount = serviceCharge)
                    Divider(Modifier.padding(vertical = 8.dp))
                    PriceRow(label = "Total to Pay", amount = finalTotal, isTotal = true)
                }
            }

            Spacer(Modifier.weight(1f)) // Pushes the button to the bottom

            Button(
                onClick = onConfirmOrder,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Confirm & Place Order")
            }
        }
    }
}

@Composable
fun PriceRow(label: String, amount: Double, isTotal: Boolean = false) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            "৳$amount",
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal
        )
    }
}