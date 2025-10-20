// CartScreen.kt
package com.yumzy.userapp.features.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yumzy.userapp.ui.theme.DarkPink
import com.yumzy.userapp.ui.theme.LightGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    cartViewModel: CartViewModel = viewModel(),
    onPlaceOrder: (restaurantId: String) -> Unit
) {
    val savedCartItems by cartViewModel.savedCart.collectAsState()
    val groupedItems = savedCartItems.values.groupBy { it.restaurantId }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.height(80.dp),
                shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
                shadowElevation = 8.dp,
                color = DarkPink
            ) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "My Cart",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        }
    ) { paddingValues ->
        if (groupedItems.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.ShoppingCart,
                        contentDescription = "Empty cart",
                        modifier = Modifier.size(64.dp),
                        tint = LightGray
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Your cart is empty.",
                        color = LightGray,
                        fontSize = 16.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color(0xFFF8F9FA)),
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with restaurant icon and name
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Restaurant,
                    contentDescription = null,
                    tint = DarkPink,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    restaurantName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A)
                )
            }

            // Items list
            items.forEachIndexed { index, cartItem ->
                CartItemRow(
                    cartItem = cartItem,
                    onIncrement = { cartViewModel.incrementSavedItem(cartItem.menuItem) },
                    onDecrement = { cartViewModel.decrementSavedItem(cartItem.menuItem) }
                )

                if (index < items.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = Color(0xFFE0E0E0)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 1.5.dp)
            Spacer(Modifier.height(16.dp))

            // Total row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Total:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = Color(0xFF666666)
                )
                Text(
                    "৳$total",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = DarkPink
                )
            }

            Spacer(Modifier.height(16.dp))

            // Place Order Button
            Button(
                onClick = onPlaceOrderClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkPink,
                    contentColor = Color.White
                )
            ) {
                Text(
                    "Place Order",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun CartItemRow(
    cartItem: CartItem,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Item details
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                cartItem.menuItem.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A1A1A)
            )
            Text(
                "৳${cartItem.menuItem.price}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF666666)
            )
        }

        // Quantity selector
        QuantitySelector(
            quantity = cartItem.quantity,
            onAdd = { },
            onIncrement = onIncrement,
            onDecrement = onDecrement
        )
    }
}

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
            modifier = Modifier.size(40.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = DarkPink,
                contentColor = Color.White
            )
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Add to cart",
                modifier = Modifier.size(20.dp)
            )
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(
                onClick = onDecrement,
                modifier = Modifier.size(32.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color(0xFFFFEBEE),
                    contentColor = DarkPink
                )
            ) {
                Icon(
                    Icons.Default.Remove,
                    contentDescription = "Decrement quantity",
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                "$quantity",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A)
            )
            IconButton(
                onClick = onIncrement,
                modifier = Modifier.size(32.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = DarkPink,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Increment quantity",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}