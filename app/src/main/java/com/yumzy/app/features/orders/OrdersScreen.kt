package com.yumzy.userapp.features.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.yumzy.userapp.ui.theme.DarkPink
import java.text.SimpleDateFormat
import java.util.Locale

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
                        }.sortedByDescending { it.createdAt }
                    }
                }
        }
    }

    Scaffold(
        topBar = {
            Surface(    modifier = Modifier.height(80.dp),
                shape = RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 20.dp),
                shadowElevation = 4.dp
            ) {
                CenterAlignedTopAppBar(
                    title = { Text("My Orders") },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = DarkPink,
                        titleContentColor = Color.White
                    )
                )
            }
        }
    )
    { paddingValues ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (orders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("You haven't placed any orders yet.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(bottom = 75.dp),
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
                Text(order.orderStatus, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

fun formatDate(timestamp: Timestamp): String {
    val sdf = SimpleDateFormat("dd MMM, yyyy 'at' hh:mm a", Locale.getDefault())
    return sdf.format(timestamp.toDate())
}