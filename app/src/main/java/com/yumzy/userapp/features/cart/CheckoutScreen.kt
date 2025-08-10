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
import androidx.compose.runtime.*
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
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

data class UserProfileDetails(
    val name: String = "...",
    val email: String = "...",
    val fullAddress: String = "..."
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    cartItems: List<CartItem>,
    onConfirmOrder: () -> Unit,
    onBackClicked: () -> Unit
) {
    val itemsSubtotal = cartItems.sumOf { it.menuItem.price * it.quantity }
    val deliveryCharge = 20.0
    val serviceCharge = 5.0
    val finalTotal = itemsSubtotal + deliveryCharge + serviceCharge

    val context = LocalContext.current
    var interstitialAd by remember { mutableStateOf<InterstitialAd?>(null) }

    // State for user profile
    var userProfile by remember { mutableStateOf<UserProfileDetails?>(null) }
    var isLoadingProfile by remember { mutableStateOf(true) }

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
                            fullAddress = address
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

    // Load Interstitial Ad
    LaunchedEffect(key1 = Unit) {
        InterstitialAd.load(
            context,
            "ca-app-pub-1527833190869655/8094999825", // Replace with your Ad Unit ID
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e("AdMob", "Interstitial ad failed to load: ${adError.message}")
                    interstitialAd = null
                }

                override fun onAdLoaded(ad: InterstitialAd) {
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

            Text("Delivery Address", style = MaterialTheme.typography.titleMedium)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (isLoadingProfile) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        Text(
                            userProfile?.fullAddress ?: "No Address Found",
                            fontWeight = FontWeight.Normal
                        )
                    }
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
                            Text(
                                "${cartItem.quantity} x ${cartItem.menuItem.name}",
                                modifier = Modifier.weight(1f)
                            )
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
                    val activity = context.findActivity()
                    if (interstitialAd != null && activity != null) {
                        interstitialAd?.fullScreenContentCallback =
                            object : FullScreenContentCallback() {
                                override fun onAdDismissedFullScreenContent() {
                                    onConfirmOrder()
                                    interstitialAd = null
                                }

                                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                    onConfirmOrder()
                                    interstitialAd = null
                                }

                                override fun onAdShowedFullScreenContent() {
                                    Log.d("AdMob", "Ad showed successfully.")
                                }
                            }
                        interstitialAd?.show(activity)
                    } else {
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

// Helper to get Activity from Context
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
