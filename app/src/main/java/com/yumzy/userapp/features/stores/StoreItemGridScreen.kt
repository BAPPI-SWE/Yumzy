package com.yumzy.userapp.features.stores

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.yumzy.userapp.features.cart.CartViewModel
import com.yumzy.userapp.ui.theme.BrandPink

// Data class for items from the store
data class StoreItem(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val imageUrl: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreItemGridScreen(
    subCategoryName: String,
    onBackClicked: () -> Unit,
    cartViewModel: CartViewModel = viewModel(),
    // 1. ADD a navigation callback parameter
    onPlaceOrder: (restaurantId: String) -> Unit
) {
    var items by remember { mutableStateOf<List<StoreItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val cartSelection by cartViewModel.currentSelection.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(key1 = subCategoryName) {
        Firebase.firestore.collection("store_items")
            .whereEqualTo("subCategory", subCategoryName)
            .get()
            .addOnSuccessListener { snapshot ->
                items = snapshot.documents.mapNotNull { doc ->
                    StoreItem(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        price = doc.getDouble("price") ?: 0.0,
                        imageUrl = doc.getString("imageUrl") ?: ""
                    )
                }
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(subCategoryName) },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            // This is the new bottom bar that appears when items are selected
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
                    // 2. MODIFY the click handler
                    onPlaceOrderClick = {
                        // First, save the selection to the cart
                        cartViewModel.saveSelectionToCart()
                        // Then, navigate using the callback
                        onPlaceOrder("yumzy_store")
                    }
                )
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(items) { item ->
                    StoreItemCard(
                        item = item,
                        storeName = "Yumzy Store",
                        quantity = cartSelection[item.id]?.quantity ?: 0,
                        cartViewModel = cartViewModel
                    )
                }
            }
        }
    }
}

@Composable
fun StoreItemCard(
    item: StoreItem,
    storeName: String,
    quantity: Int,
    cartViewModel: CartViewModel
) {
    val genericMenuItem = com.yumzy.userapp.features.home.MenuItem(
        id = item.id,
        name = item.name,
        price = item.price,
        category = "Store Item"
    )

    Card(
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Image container with gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                        contentScale = ContentScale.Crop
                    )

                    // Gradient overlay for better text readability
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.3f)
                                    ),
                                    startY = 0f,
                                    endY = Float.POSITIVE_INFINITY
                                )
                            )
                    )

                    // Quantity indicator badge (when item is selected)
                    if (quantity > 0) {
                        Surface(
                            modifier = Modifier
                                .padding(12.dp)
                                .align(Alignment.TopEnd),
                            shape = CircleShape,
                            color = BrandPink,
                            shadowElevation = 4.dp
                        ) {
                            Text(
                                text = "$quantity",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Content section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Product name
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Price and quantity selector row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Price with modern styling
                        Column {
                            Text(
                                text = "Price",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "৳${String.format("%.0f", item.price)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BrandPink,
                                fontSize = 16.sp
                            )
                        }

                        // Quantity selector with modern design
                        ModernQuantitySelector(
                            quantity = quantity,
                            onAdd = { cartViewModel.addToSelection(genericMenuItem, "yumzy_store", storeName) },
                            onIncrement = { cartViewModel.incrementSelection(genericMenuItem) },
                            onDecrement = { cartViewModel.decrementSelection(genericMenuItem) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModernQuantitySelector(
    quantity: Int,
    onAdd: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    if (quantity == 0) {
        // Modern add button with subtle animation
        FilledTonalButton(
            onClick = onAdd,
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.size(36.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = BrandPink.copy(alpha = 0.1f),
                contentColor = BrandPink
            )
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Add to cart",
                modifier = Modifier.size(20.dp)
            )
        }
    } else {
        // Modern quantity selector with elevated design
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.height(36.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                // Decrement button
                FilledIconButton(
                    onClick = onDecrement,
                    modifier = Modifier.size(28.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.White,
                        contentColor = BrandPink
                    )
                ) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = "Decrease quantity",
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Quantity display
                Text(
                    text = "$quantity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                // Increment button
                FilledIconButton(
                    onClick = onIncrement,
                    modifier = Modifier.size(28.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = BrandPink,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Increase quantity",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
// These helper functions are added here to make the file self-contained.

@Composable
fun BottomBarWithTwoButtons(onAddToCartClick: () -> Unit, onPlaceOrderClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().navigationBarsPadding(), shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(onClick = onAddToCartClick, modifier = Modifier.height(50.dp)) {
                Icon(Icons.Default.AddShoppingCart, contentDescription = "Add to Cart")
            }
            Button(onClick = onPlaceOrderClick, modifier = Modifier.weight(1f).height(50.dp)) {
                Text("Place Order Now", fontSize = 16.sp)
            }
        }
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
            modifier = Modifier.size(30.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add to cart")
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedButton(
                onClick = onDecrement,
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrement quantity")
            }
            Text("$quantity", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
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