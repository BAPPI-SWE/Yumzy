package com.yumzy.app.features.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

data class Order(
    val id: String = "",
    val restaurantName: String = "",
    val totalPrice: Double = 0.0,
    val orderStatus: String = "",
    val createdAt: Timestamp = Timestamp.now()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen() {
    var orders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(key1 = Unit) {
        val userId = Firebase.auth.currentUser?.uid
        if (userId != null) {
            Firebase.firestore.collection("orders")
                .whereEqualTo("userId", userId)
                .addSnapshotListener { snapshot, _ ->
                    isLoading = false
                    snapshot?.let {
                        orders = it.documents.mapNotNull { doc ->
                            Order(
                                id = doc.id,
                                restaurantName = doc.getString("restaurantName") ?: "Unknown Restaurant",
                                totalPrice = doc.getDouble("totalPrice") ?: 0.0,
                                orderStatus = doc.getString("orderStatus") ?: "Unknown",
                                createdAt = doc.getTimestamp("createdAt") ?: Timestamp.now()
                            )
                        }.sortedByDescending { it.createdAt } // Show newest orders first
                    }
                }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("My Orders") }) }
    ) { paddingValues ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (orders.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("You haven't placed any orders yet.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(orders) { order ->
                    OrderHistoryCard(order = order)
                }
            }
        }
    }
}

@Composable
fun OrderHistoryCard(order: Order) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(order.restaurantName, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Order placed on ${formatDate(order.createdAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Divider(Modifier.padding(vertical = 8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total: ৳${order.totalPrice}", fontWeight = FontWeight.Bold)
                Text(order.orderStatus, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

fun formatDate(timestamp: Timestamp): String {
    val sdf = SimpleDateFormat("dd MMM, yyyy 'at' hh:mm a", Locale.getDefault())
    return sdf.format(timestamp.toDate())
}