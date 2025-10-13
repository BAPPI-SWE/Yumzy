// OrdersScreen.kt
package com.yumzy.userapp.features.orders

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.yumzy.userapp.ads.SharedInterstitialAdManager
import com.yumzy.userapp.ui.theme.DarkPink
import com.yumzy.userapp.ui.theme.LightGray
import java.text.SimpleDateFormat
import java.util.Locale

data class OrderItem(
    val name: String = "",
    val price: Double = 0.0,
    val quantity: Int = 0
)

data class Order(
    val id: String = "",
    val restaurantName: String = "",
    val totalPrice: Double = 0.0,
    val orderStatus: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val items: List<OrderItem> = emptyList(),
    val deliveryCharge: Double = 0.0,
    val serviceCharge: Double = 0.0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    showAdOnLoad: Boolean = false
) {
    var orders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedOrder by remember { mutableStateOf<Order?>(null) }
    var hasShownAd by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Show the pre-loaded ad when coming from checkout
    LaunchedEffect(showAdOnLoad) {
        if (showAdOnLoad && !hasShownAd) {
            // Small delay to let the screen render smoothly first
            kotlinx.coroutines.delay(300)

            val activity = context.findActivity()
            if (activity != null) {
                // Show the ad that was pre-loaded on CheckoutScreen
                SharedInterstitialAdManager.showAd(activity) {
                    // Ad dismissed or wasn't available - that's fine!
                    android.util.Log.d("OrdersScreen", "Ad flow completed")
                }
                hasShownAd = true
            }
        }
    }

    LaunchedEffect(key1 = Unit) {
        val userId = Firebase.auth.currentUser?.uid
        if (userId != null) {
            Firebase.firestore.collection("orders")
                .whereEqualTo("userId", userId)
                .addSnapshotListener { snapshot, _ ->
                    isLoading = false
                    snapshot?.let {
                        orders = it.documents.mapNotNull { doc ->
                            val itemsData = doc.get("items") as? List<Map<String, Any>> ?: emptyList()
                            val orderItems = itemsData.map { itemMap ->
                                OrderItem(
                                    name = itemMap["itemName"] as? String ?: itemMap["name"] as? String ?: "Unknown Item",
                                    price = (itemMap["itemPrice"] as? Number)?.toDouble() ?: (itemMap["price"] as? Number)?.toDouble() ?: 0.0,
                                    quantity = (itemMap["quantity"] as? Number)?.toInt() ?: 0
                                )
                            }

                            Order(
                                id = doc.id,
                                restaurantName = doc.getString("restaurantName") ?: "Unknown Restaurant",
                                totalPrice = doc.getDouble("totalPrice") ?: 0.0,
                                orderStatus = doc.getString("orderStatus") ?: "Unknown",
                                createdAt = doc.getTimestamp("createdAt") ?: Timestamp.now(),
                                items = orderItems,
                                deliveryCharge = doc.getDouble("deliveryCharge") ?: 0.0,
                                serviceCharge = doc.getDouble("serviceCharge") ?: 0.0
                            )
                        }.sortedByDescending { it.createdAt }
                    }
                }
        }
    }

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
                            "My Orders",
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
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DarkPink)
            }
        } else if (orders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = "No orders",
                        modifier = Modifier.size(64.dp),
                        tint = LightGray
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "You haven't placed any orders yet.",
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
                    .background(Color(0xFFF8F9FA))
                    .padding(bottom = 75.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(orders) { order ->
                    OrderHistoryCard(
                        order = order,
                        onClick = { selectedOrder = order }
                    )
                }
            }
        }
    }

    // Order Details Dialog
    selectedOrder?.let { order ->
        OrderDetailsDialog(
            order = order,
            onDismiss = { selectedOrder = null }
        )
    }
}

// Helper function to find activity from context
private fun android.content.Context.findActivity(): Activity? {
    var context = this
    while (context is android.content.ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

@Composable
fun OrderHistoryCard(
    order: Order,
    onClick: () -> Unit
) {
    ModernCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                order.restaurantName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF333333)
            )
            Text(
                text = "Order placed on ${formatDate(order.createdAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF666666)
            )
            Divider(
                Modifier.padding(vertical = 12.dp),
                color = Color(0xFFE0E0E0)
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "Total: ৳${order.totalPrice}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF333333)
                )
                Text(
                    order.orderStatus,
                    color = DarkPink,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Tap to view details hint
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Tap to view order details",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF666666),
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
fun OrderDetailsDialog(
    order: Order,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.White
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Order Summary",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 16.dp),
                    color = Color(0xFF333333)
                )

                // Restaurant and Order Info
                Text(
                    order.restaurantName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )
                Text(
                    "Ordered on ${formatDate(order.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF666666),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    "Status: ${order.orderStatus}",
                    style = MaterialTheme.typography.bodySmall,
                    color = DarkPink,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Order Items
                Text(
                    "Items:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp),
                    color = Color(0xFF333333)
                )

                order.items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${item.quantity} x ${item.name}",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF333333)
                        )
                        Text(
                            "৳${item.price * item.quantity}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF333333)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color(0xFFE0E0E0))
                Spacer(modifier = Modifier.height(16.dp))

                // Price Breakdown
                val itemsSubtotal = order.items.sumOf { it.price * it.quantity }

                PriceDetailRow("Items Subtotal", itemsSubtotal)
                if (order.deliveryCharge > 0) {
                    PriceDetailRow("Delivery Charge", order.deliveryCharge)
                }
                if (order.serviceCharge > 0) {
                    PriceDetailRow("Service Charge", order.serviceCharge)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = Color(0xFFE0E0E0))
                Spacer(modifier = Modifier.height(8.dp))

                PriceDetailRow(
                    "Total Payable",
                    order.totalPrice,
                    isTotal = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkPink,
                        contentColor = Color.White
                    )
                ) {
                    Text("Close", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun PriceDetailRow(
    label: String,
    amount: Double,
    isTotal: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = if (isTotal) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
            color = if (isTotal) DarkPink else Color(0xFF666666)
        )
        Text(
            "৳$amount",
            style = if (isTotal) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
            color = if (isTotal) DarkPink else Color(0xFF333333)
        )
    }
}

@Composable
fun ModernCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                clip = true
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(content = content)
    }
}

fun formatDate(timestamp: Timestamp): String {
    val sdf = SimpleDateFormat("dd MMM, yyyy 'at' hh:mm a", Locale.getDefault())
    return sdf.format(timestamp.toDate())
}