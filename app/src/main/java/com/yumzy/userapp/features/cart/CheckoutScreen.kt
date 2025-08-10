package com.yumzy.userapp.features.cart

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

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

    val context = LocalContext.current
    // 1. Create a state to hold the ad. Initialize it as null.
    var interstitialAd by remember { mutableStateOf<InterstitialAd?>(null) }

    // 2. Use LaunchedEffect to load the ad when the screen is first displayed.
    LaunchedEffect(key1 = Unit) {
        InterstitialAd.load(
            context,
            "ca-app-pub-3940256099942544/1033173712", // Test Ad Unit ID
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    // Handle the error, for example, by logging it.
                    Log.e("AdMob", "Interstitial ad failed to load: ${adError.message}")
                    interstitialAd = null
                }

                override fun onAdLoaded(ad: InterstitialAd) {
                    // Ad is loaded and ready to be shown.
                    Log.d("AdMob", "Interstitial ad loaded successfully.")
                    interstitialAd = ad
                }
            }
        )
    }

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
            // ... (Your existing UI for address, summary, etc. remains unchanged)
            Text("Delivery Address", style = MaterialTheme.typography.titleMedium)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Daffodil Smart City", fontWeight = FontWeight.SemiBold)
                    Text("Hall 1, Room 101", color = Color.Gray)
                }
            }

            Spacer(Modifier.height(24.dp))

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

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    // 3. Find the current activity and show the ad if it is not null.
                    val activity = context.findActivity()
                    if (interstitialAd != null && activity != null) {
                        interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() {
                                // Ad was dismissed. Proceed with the order confirmation.
                                onConfirmOrder()
                                interstitialAd = null // Ad is one-time use.
                            }

                            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                // Ad failed to show. Proceed with the order confirmation.
                                onConfirmOrder()
                                interstitialAd = null
                            }

                            override fun onAdShowedFullScreenContent() {
                                // Ad showed successfully.
                                Log.d("AdMob", "Ad showed successfully.")
                            }
                        }
                        interstitialAd?.show(activity)
                    } else {
                        // If the ad is not ready, just proceed with the order confirmation.
                        Toast.makeText(context, "Placing Order...", Toast.LENGTH_SHORT).show()
                        onConfirmOrder()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Confirm & Place Order")
            }
        }
    }
}

// Helper function to find the current activity from the context (remains unchanged)
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
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            "৳$amount",
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal
        )
    }
}