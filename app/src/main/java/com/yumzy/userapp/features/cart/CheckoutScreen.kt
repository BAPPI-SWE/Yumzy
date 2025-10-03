// CheckoutScreen.kt
package com.yumzy.userapp.features.cart

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.yumzy.userapp.ads.InterstitialAdManager
import com.yumzy.userapp.ui.theme.DarkPink
import com.yumzy.userapp.ui.theme.LightGray
import kotlinx.coroutines.tasks.await

data class UserProfileDetails(
    val name: String = "...",
    val email: String = "...",
    val fullAddress: String = "...",
    val baseLocation: String = "",
    val subLocation: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    cartItems: List<CartItem>,
    restaurantId: String? = null,
    onConfirmOrder: (deliveryCharge: Double, serviceCharge: Double, finalTotal: Double) -> Unit,
    onBackClicked: () -> Unit
) {
    val itemsSubtotal = cartItems.sumOf { it.menuItem.price * it.quantity }
    var deliveryCharge by remember { mutableStateOf(20.0) }
    var serviceCharge by remember { mutableStateOf(5.0) }
    var isLoadingCharges by remember { mutableStateOf(true) }
    val isPreOrder = cartItems.isNotEmpty() && cartItems.first().menuItem.category.startsWith("Pre-order")
    val finalTotal = itemsSubtotal + deliveryCharge + serviceCharge
    var userProfile by remember { mutableStateOf<UserProfileDetails?>(null) }
    var isLoadingProfile by remember { mutableStateOf(true) }

    // --- AdMob Interstitial Ad Integration ---
    val context = LocalContext.current
    val interstitialAdManager = remember { InterstitialAdManager(context) }

    // Pre-load the ad when the screen is first composed
    LaunchedEffect(Unit) {
        interstitialAdManager.loadAd()
    }
    // --- End AdMob Integration ---

    // Fetch user profile from Firestore
    LaunchedEffect(Unit) {
        val currentUser = Firebase.auth.currentUser
        if (currentUser != null) {
            Firebase.firestore.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val address = "${document.getString("baseLocation") ?: ""} , " +
                                "${document.getString("subLocation") ?: ""}"
                        userProfile = UserProfileDetails(
                            name = document.getString("name") ?: "N/A",
                            email = currentUser.email ?: "N/A",
                            fullAddress = address,
                            baseLocation = document.getString("baseLocation") ?: "",
                            subLocation = document.getString("subLocation") ?: ""
                        )
                    }
                    isLoadingProfile = false
                }
                .addOnFailureListener {
                    isLoadingProfile = false
                }
        } else {
            isLoadingProfile = false
        }
    }

    // Calculate dynamic charges based on user location and store type + additional charges from items
    LaunchedEffect(userProfile) {
        if (userProfile != null && userProfile!!.baseLocation.isNotEmpty() && userProfile!!.subLocation.isNotEmpty()) {
            isLoadingCharges = true
            val db = Firebase.firestore

            // First, get base charges from location
            db.collection("locations")
                .whereEqualTo("name", userProfile!!.baseLocation)
                .get()
                .addOnSuccessListener { documents ->
                    var baseDeliveryCharge = 20.0
                    var baseServiceCharge = 5.0

                    if (!documents.isEmpty) {
                        val locationDoc = documents.documents[0]
                        val subLocations = locationDoc.get("subLocations") as? List<String> ?: emptyList()
                        val subLocationIndex = subLocations.indexOf(userProfile!!.subLocation)
                        if (subLocationIndex != -1) {
                            if (isPreOrder) {
                                val serviceChargeArray = locationDoc.get("serviceCharge") as? List<Number> ?: emptyList()
                                val deliveryChargeArray = locationDoc.get("deliveryCharge") as? List<Number> ?: emptyList()
                                if (subLocationIndex < serviceChargeArray.size) baseServiceCharge = serviceChargeArray[subLocationIndex].toDouble()
                                if (subLocationIndex < deliveryChargeArray.size) baseDeliveryCharge = deliveryChargeArray[subLocationIndex].toDouble()
                            } else {
                                val serviceChargeArray = locationDoc.get("serviceChargeYumzy") as? List<Number> ?: emptyList()
                                val deliveryChargeArray = locationDoc.get("deliveryChargeYumzy") as? List<Number> ?: emptyList()
                                if (subLocationIndex < serviceChargeArray.size) baseServiceCharge = serviceChargeArray[subLocationIndex].toDouble()
                                if (subLocationIndex < deliveryChargeArray.size) baseDeliveryCharge = deliveryChargeArray[subLocationIndex].toDouble()
                            }
                        }
                    }

                    // Now check for additional charges from store items
                    if (restaurantId == "yumzy_store") {
                        val itemIds = cartItems.map { it.menuItem.id }
                        var additionalDelivery = 0.0
                        var additionalService = 0.0
                        var itemsProcessed = 0

                        // Fetch each item's additional charges
                        itemIds.forEach { itemId ->
                            db.collection("store_items").document(itemId).get()
                                .addOnSuccessListener { itemDoc ->
                                    if (itemDoc.exists()) {
                                        additionalDelivery += itemDoc.getDouble("additionalDeliveryCharge") ?: 0.0
                                        additionalService += itemDoc.getDouble("additionalServiceCharge") ?: 0.0
                                    }
                                    itemsProcessed++

                                    // When all items are processed, update the charges
                                    if (itemsProcessed == itemIds.size) {
                                        deliveryCharge = baseDeliveryCharge + additionalDelivery
                                        serviceCharge = baseServiceCharge + additionalService
                                        isLoadingCharges = false
                                    }
                                }
                                .addOnFailureListener {
                                    itemsProcessed++
                                    if (itemsProcessed == itemIds.size) {
                                        deliveryCharge = baseDeliveryCharge + additionalDelivery
                                        serviceCharge = baseServiceCharge + additionalService
                                        isLoadingCharges = false
                                    }
                                }
                        }

                        // If no items, just use base charges
                        if (itemIds.isEmpty()) {
                            deliveryCharge = baseDeliveryCharge
                            serviceCharge = baseServiceCharge
                            isLoadingCharges = false
                        }
                    } else {
                        // For non-store orders, just use base charges
                        deliveryCharge = baseDeliveryCharge
                        serviceCharge = baseServiceCharge
                        isLoadingCharges = false
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("CheckoutScreen", "Error loading charges: ${exception.message}")
                    isLoadingCharges = false
                }
        } else {
            isLoadingCharges = false
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Confirm Order",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClicked,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Gray.copy(alpha = 0.05f))
                            .border(0.5.dp, Color.Black.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.Black,

                            )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Delivery Address Section
            Text(
                "Delivery Address",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    if (isLoadingProfile) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        Text(
                            userProfile?.fullAddress ?: "No Address Found",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Order Summary Section
            Text(
                "Order Summary",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    cartItems.forEach { cartItem ->
                        Row(Modifier.fillMaxWidth()) {
                            Text(
                                "${cartItem.quantity} x ${cartItem.menuItem.name}",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "৳${cartItem.menuItem.price * cartItem.quantity}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Price Details Section
            Text(
                "Price Details",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isLoadingCharges) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Calculating charges...")
                        }
                    } else {
                        PriceRow(label = "Items Subtotal", amount = itemsSubtotal)
                        PriceRow(label = "Delivery Charge", amount = deliveryCharge)
                        PriceRow(label = "Service Charge", amount = serviceCharge)
                        Divider(
                            Modifier.padding(vertical = 12.dp),
                            color = LightGray.copy(alpha = 0.4f)
                        )
                        PriceRow(label = "Total to Pay", amount = finalTotal, isTotal = true)
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Confirm Order Button
            Button(
                onClick = {
                    val activity = context.findActivity()
                    if (activity != null) {
                        // Show the ad. After it's dismissed, confirm the order.
                        interstitialAdManager.showAd(activity) {
                            onConfirmOrder(deliveryCharge, serviceCharge, finalTotal)
                        }
                    } else {
                        // Fallback if activity is not found
                        Toast.makeText(context, "Placing Order...", Toast.LENGTH_SHORT).show()
                        onConfirmOrder(deliveryCharge, serviceCharge, finalTotal)
                    }
                },
                enabled = !isLoadingCharges,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkPink,
                    contentColor = Color.White
                )
            ) {
                if (isLoadingCharges) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        "Confirm & Place Order",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun PriceRow(label: String, amount: Double, isTotal: Boolean = false) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
            color = if (isTotal) DarkPink else MaterialTheme.colorScheme.onSurface,
            fontSize = if (isTotal) 18.sp else 16.sp
        )
        Text(
            "৳$amount",
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
            color = if (isTotal) DarkPink else MaterialTheme.colorScheme.onSurface,
            fontSize = if (isTotal) 18.sp else 16.sp
        )
    }
}