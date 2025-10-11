package com.yumzy.userapp.features.stores

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.yumzy.userapp.YLogoLoadingIndicator
import com.yumzy.userapp.features.cart.CartViewModel
import com.yumzy.userapp.ui.theme.BrandPink
import com.yumzy.userapp.ui.theme.DarkPink
import com.yumzy.userapp.ui.theme.DeepPink
import kotlinx.coroutines.tasks.await

// Data class for item variants
data class ItemVariant(
    val name: String = "",
    val price: Double = 0.0
)

// Updated StoreItem data class with multi-variant support
data class StoreItem(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val imageUrl: String = "",
    val itemDescription: String = "",
    val additionalDeliveryCharge: Double = 0.0,
    val additionalServiceCharge: Double = 0.0,
    val isShopOpen: Boolean = true,
    val stock: String = "yes",
    val multiVariant: Int = 0,
    val variants: List<ItemVariant> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreItemGridScreen(
    title: String,
    subCategoryName: String?,
    miniResId: String?,
    onBackClicked: () -> Unit,
    cartViewModel: CartViewModel = viewModel(),
    onPlaceOrder: (restaurantId: String) -> Unit
) {
    var items by remember { mutableStateOf<List<StoreItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedItem by remember { mutableStateOf<StoreItem?>(null) }

    val cartSelection by cartViewModel.currentSelection.collectAsState()
    val context = LocalContext.current

    val totalItems = cartSelection.values.sumOf { it.quantity }

    LaunchedEffect(key1 = subCategoryName, key2 = miniResId) {
        isLoading = true
        val db = Firebase.firestore
        try {
            // PATH 1: Viewing items from a specific Mini Restaurant
            if (!miniResId.isNullOrBlank()) {
                val isRestaurantOpen = db.collection("mini_restaurants").document(miniResId).get().await()
                    .getString("open")?.equals("yes", ignoreCase = true) ?: false

                val snapshot = db.collection("store_items").whereEqualTo("miniRes", miniResId).get().await()
                items = snapshot.documents.mapNotNull { doc ->
                    val multiVariant = doc.getLong("multiVariant")?.toInt() ?: 0
                    val variants = if (multiVariant >= 2) {
                        (1..multiVariant).mapNotNull { index ->
                            val variantName = doc.getString("variant${index}name")
                            val variantPrice = doc.getDouble("variant${index}price")
                            if (variantName != null && variantPrice != null) {
                                ItemVariant(variantName, variantPrice)
                            } else null
                        }
                    } else emptyList()

                    StoreItem(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        price = doc.getDouble("price") ?: 0.0,
                        imageUrl = doc.getString("imageUrl") ?: "",
                        itemDescription = doc.getString("itemDescription") ?: "No description available.",
                        additionalDeliveryCharge = doc.getDouble("additionalDeliveryCharge") ?: 0.0,
                        additionalServiceCharge = doc.getDouble("additionalServiceCharge") ?: 0.0,
                        isShopOpen = isRestaurantOpen,
                        stock = doc.getString("stock") ?: "yes",
                        multiVariant = multiVariant,
                        variants = variants
                    )
                }
            }
            // PATH 2: Viewing items from a Sub Category
            else if (!subCategoryName.isNullOrBlank()) {
                val itemsSnapshot = db.collection("store_items").whereEqualTo("subCategory", subCategoryName).get().await()
                val itemsWithMiniResIds = itemsSnapshot.documents.map { it to it.getString("miniRes") }

                val miniResIds = itemsWithMiniResIds.mapNotNull { it.second }.distinct()
                val statusMap = if (miniResIds.isNotEmpty()) {
                    db.collection("mini_restaurants").whereIn(com.google.firebase.firestore.FieldPath.documentId(), miniResIds).get().await()
                        .documents.associate { it.id to (it.getString("open")?.equals("yes", true) ?: false) }
                } else {
                    emptyMap()
                }

                items = itemsWithMiniResIds.map { (doc, resId) ->
                    val isOpen = resId?.let { statusMap[it] } ?: true
                    val multiVariant = doc.getLong("multiVariant")?.toInt() ?: 0
                    val variants = if (multiVariant >= 2) {
                        (1..multiVariant).mapNotNull { index ->
                            val variantName = doc.getString("variant${index}name")
                            val variantPrice = doc.getDouble("variant${index}price")
                            if (variantName != null && variantPrice != null) {
                                ItemVariant(variantName, variantPrice)
                            } else null
                        }
                    } else emptyList()

                    StoreItem(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        price = doc.getDouble("price") ?: 0.0,
                        imageUrl = doc.getString("imageUrl") ?: "",
                        itemDescription = doc.getString("itemDescription") ?: "No description available.",
                        additionalDeliveryCharge = doc.getDouble("additionalDeliveryCharge") ?: 0.0,
                        additionalServiceCharge = doc.getDouble("additionalServiceCharge") ?: 0.0,
                        isShopOpen = isOpen,
                        stock = doc.getString("stock") ?: "yes",
                        multiVariant = multiVariant,
                        variants = variants
                    )
                }
            } else {
                items = emptyList()
            }
        } catch (e: Exception) {
            items = emptyList()
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = title, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp)) },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Box(
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
                                modifier = Modifier.align(Alignment.Center).size(22.dp)
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
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
                    onPlaceOrderClick = {
                        cartViewModel.saveSelectionToCart()
                        onPlaceOrder("yumzy_store")
                    },
                    totalItems = totalItems
                )
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                YLogoLoadingIndicator(size = 35.dp, color = DeepPink)
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    BannerAd()
                    if (items.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No items found.")
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(items) { item ->
                                StoreItemCard(
                                    item = item,
                                    storeName = "Yumzy Store",
                                    cartViewModel = cartViewModel,
                                    cartSelection = cartSelection,
                                    onClick = { selectedItem = item }
                                )
                            }
                        }
                    }
                }
            }
        }

        selectedItem?.let { item ->
            if (item.multiVariant >= 2) {
                MultiVariantDialog(
                    item = item,
                    cartViewModel = cartViewModel,
                    cartSelection = cartSelection,
                    onDismiss = { selectedItem = null }
                )
            } else {
                val quantity = cartSelection[item.id]?.quantity ?: 0
                StoreItemDetailDialog(
                    item = item,
                    quantity = quantity,
                    cartViewModel = cartViewModel,
                    onDismiss = { selectedItem = null }
                )
            }
        }
    }
}

@Composable
fun MultiVariantDialog(
    item: StoreItem,
    cartViewModel: CartViewModel,
    cartSelection: Map<String, com.yumzy.userapp.features.cart.CartItem>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    contentScale = ContentScale.Crop
                )
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.itemDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!item.isShopOpen) {
                        Text(
                            text = "This item is currently unavailable as the shop is closed.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (item.stock != "yes") {
                        Text(
                            text = "This item is currently out of stock.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Choose Variant",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    item.variants.forEach { variant ->
                        val variantId = "${item.id}_${variant.name}"
                        val quantity = cartSelection[variantId]?.quantity ?: 0
                        val genericMenuItem = com.yumzy.userapp.features.home.MenuItem(
                            id = variantId,
                            name = "${item.name} (${variant.name})",
                            price = variant.price,
                            category = "Store Item"
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = variant.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "৳${String.format("%.0f", variant.price)}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandPink
                                    )
                                }
                                ModernQuantitySelector(
                                    quantity = quantity,
                                    onAdd = { cartViewModel.addToSelection(genericMenuItem, "yumzy_store", "Yumzy Store") },
                                    onIncrement = { cartViewModel.incrementSelection(genericMenuItem) },
                                    onDecrement = { cartViewModel.decrementSelection(genericMenuItem) },
                                    enabled = item.isShopOpen && item.stock == "yes"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BottomBarWithTwoButtons(
    onAddToCartClick: () -> Unit,
    onPlaceOrderClick: () -> Unit,
    totalItems: Int
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onAddToCartClick,
                modifier = Modifier.height(50.dp),
                shape = RoundedCornerShape(12.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = BrandPink
                )
            ) {
                Icon(
                    Icons.Default.AddShoppingCart,
                    contentDescription = "Add to Cart",
                    modifier = Modifier.size(20.dp)
                )
            }

            Button(
                onClick = onPlaceOrderClick,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkPink,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Place Order",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Place Order Now ($totalItems)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun StoreItemDetailDialog(
    item: StoreItem,
    quantity: Int,
    cartViewModel: CartViewModel,
    onDismiss: () -> Unit
) {
    val genericMenuItem = com.yumzy.userapp.features.home.MenuItem(
        id = item.id,
        name = item.name,
        price = item.price,
        category = "Store Item"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    contentScale = ContentScale.Crop
                )
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.itemDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!item.isShopOpen) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "This item is currently unavailable as the shop is closed.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(4.dp))
                    } else if (item.stock != "yes") {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "This item is currently out of stock.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Price",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "৳${String.format("%.0f", item.price)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = BrandPink
                            )
                        }
                        ModernQuantitySelector(
                            quantity = quantity,
                            onAdd = { cartViewModel.addToSelection(genericMenuItem, "yumzy_store", "Yumzy Store") },
                            onIncrement = { cartViewModel.incrementSelection(genericMenuItem) },
                            onDecrement = { cartViewModel.decrementSelection(genericMenuItem) },
                            enabled = item.isShopOpen && item.stock == "yes"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StoreItemCard(
    item: StoreItem,
    storeName: String,
    cartViewModel: CartViewModel,
    cartSelection: Map<String, com.yumzy.userapp.features.cart.CartItem>,
    onClick: () -> Unit
) {
    val isItemAvailable = item.isShopOpen && item.stock == "yes"
    val hasMultiVariant = item.multiVariant >= 2

    // Calculate total quantity for multi-variant items
    val totalQuantity = if (hasMultiVariant) {
        item.variants.sumOf { variant ->
            val variantId = "${item.id}_${variant.name}"
            cartSelection[variantId]?.quantity ?: 0
        }
    } else {
        cartSelection[item.id]?.quantity ?: 0
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .clickable(enabled = isItemAvailable, onClick = onClick)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
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
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.1f)
                                    ),
                                    startY = 0f,
                                    endY = Float.POSITIVE_INFINITY
                                )
                            )
                    )
                    if (totalQuantity > 0) {
                        Surface(
                            modifier = Modifier
                                .padding(12.dp)
                                .align(Alignment.TopEnd),
                            shape = CircleShape,
                            color = BrandPink.copy(alpha = 0.6f),
                            shadowElevation = 4.dp
                        ) {
                            Text(
                                text = "$totalQuantity",
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 4.dp),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                    if (!isItemAvailable) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                                .background(Color.Black.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    if (!item.isShopOpen) Icons.Default.Lock else Icons.Default.Block,
                                    contentDescription = if (!item.isShopOpen) "Shop Closed" else "Out of Stock",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = if (!item.isShopOpen) "SHOP CLOSED" else "OUT OF STOCK",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Price",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                            if (hasMultiVariant && item.variants.isNotEmpty()) {
                                Text(
                                    text = "৳${String.format("%.0f", item.variants.minOf { it.price })} - ৳${String.format("%.0f", item.variants.maxOf { it.price })}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandPink,
                                    fontSize = 15.sp
                                )
                            } else {
                                Text(
                                    text = "৳${String.format("%.0f", item.price)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandPink,
                                    fontSize = 15.sp
                                )
                            }
                        }

                        if (hasMultiVariant) {
                            // Show selector button styled like add button for multi-variant items
                            if (totalQuantity == 0) {
                                FilledTonalButton(
                                    onClick = onClick,
                                    enabled = isItemAvailable,
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
                                        contentDescription = "Choose variant",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            } else {
                                // Show quantity badge with click to modify
                                Surface(
                                    onClick = onClick,
                                    enabled = isItemAvailable,
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    ) {
                                        Text(
                                            text = "$totalQuantity",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit variants",
                                            modifier = Modifier.size(16.dp),
                                            tint = BrandPink
                                        )
                                    }
                                }
                            }
                        } else {
                            // Show quantity selector for regular items
                            val genericMenuItem = com.yumzy.userapp.features.home.MenuItem(
                                id = item.id,
                                name = item.name,
                                price = item.price,
                                category = "Store Item"
                            )
                            ModernQuantitySelector(
                                quantity = totalQuantity,
                                onAdd = { cartViewModel.addToSelection(genericMenuItem, "yumzy_store", storeName) },
                                onIncrement = { cartViewModel.incrementSelection(genericMenuItem) },
                                onDecrement = { cartViewModel.decrementSelection(genericMenuItem) },
                                enabled = isItemAvailable
                            )
                        }
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
    onDecrement: () -> Unit,
    enabled: Boolean = true
) {
    if (quantity == 0) {
        FilledTonalButton(
            onClick = onAdd,
            enabled = enabled,
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
                FilledIconButton(
                    onClick = onDecrement,
                    enabled = enabled,
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
                Text(
                    text = "$quantity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
                FilledIconButton(
                    onClick = onIncrement,
                    enabled = enabled,
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

@Composable
fun BannerAd() {
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = "ca-app-pub-1527833190869655/8094999825"
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}