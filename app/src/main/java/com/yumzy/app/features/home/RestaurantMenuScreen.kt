package com.yumzy.userapp.features.home

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.ArrowCircleRight
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.yumzy.userapp.features.cart.CartViewModel
import com.yumzy.userapp.ui.theme.cardColors

// Data classes defined locally
data class PreOrderCategory(val id: String = "", val name: String = "", val startTime: String = "", val endTime: String = "", val deliveryTime: String = "")
data class MenuItem(val id: String = "", val name: String = "", val price: Double = 0.0, val category: String = "")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantMenuScreen(
    restaurantId: String,
    restaurantName: String,
    cartViewModel: CartViewModel = viewModel(),
    onCategoryClick: (restaurantId: String, restaurantName: String, categoryName: String) -> Unit,
    onBackClicked: () -> Unit,
    onPlaceOrder: (restaurantId: String) -> Unit
) {
    var preOrderCategories by remember { mutableStateOf<List<PreOrderCategory>>(emptyList()) }
    var currentMenuItems by remember { mutableStateOf<List<MenuItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isInstantDeliveryAvailable by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val cartSelection by cartViewModel.currentSelection.collectAsState()
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(key1 = restaurantId) {
        val db = Firebase.firestore
        val restaurantRef = db.collection("restaurants").document(restaurantId)

        restaurantRef.collection("preOrderCategories").get()
            .addOnSuccessListener { snapshot ->
                preOrderCategories = snapshot.documents.mapNotNull { doc -> doc.toObject(PreOrderCategory::class.java)?.copy(id = doc.id) }
            }

        restaurantRef.collection("menuItems").whereEqualTo("category", "Current Menu")
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let {
                    currentMenuItems = it.documents.mapNotNull { doc -> doc.toObject(MenuItem::class.java)?.copy(id = doc.id) }
                }
                isLoading = false
            }

        db.collection("riders")
            .whereEqualTo("isAvailable", true)
            .whereArrayContains("serviceableLocations", "Daffodil Smart City")
            .addSnapshotListener { snapshot, _ ->
                isInstantDeliveryAvailable = snapshot?.isEmpty == false
            }
    }

    Scaffold(
        modifier = Modifier.navigationBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text(restaurantName) },
                navigationIcon = { IconButton(onClick = onBackClicked) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
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
                        onPlaceOrder(restaurantId)
                    }
                )
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Tab selection
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    edgePadding = 16.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = MaterialTheme.colorScheme.primary,
                            height = 3.dp
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("Pre-Order Category", fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Currently Available Food", fontWeight = FontWeight.SemiBold)


                            }
                        }
                    )
                }

                // Tab content
                Box(modifier = Modifier.fillMaxSize()) {
                    when (selectedTabIndex) {
                        0 -> PreOrderContent(
                            preOrderCategories = preOrderCategories,
                            onCategoryClick = { category ->
                                onCategoryClick(restaurantId, restaurantName, "Pre-order ${category.name}")
                            }
                        )
                        1 -> CurrentMenuContent(
                            currentMenuItems = currentMenuItems,
                            isInstantDeliveryAvailable = isInstantDeliveryAvailable,
                            cartViewModel = cartViewModel,
                            restaurantId = restaurantId,
                            restaurantName = restaurantName
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PreOrderContent(
    preOrderCategories: List<PreOrderCategory>,
    onCategoryClick: (PreOrderCategory) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {



        if (preOrderCategories.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No pre-order categories available", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            itemsIndexed(preOrderCategories) { index, category ->
                val color = cardColors[index % cardColors.size]
                PreOrderHeader(
                    category = category,
                    cardColor = color,
                    onClick = { onCategoryClick(category) }
                )
            }
        }
    }
}

@Composable
private fun CurrentMenuContent(
    currentMenuItems: List<MenuItem>,
    isInstantDeliveryAvailable: Boolean,
    cartViewModel: CartViewModel,
    restaurantId: String,
    restaurantName: String
) {
    val cartSelection by cartViewModel.currentSelection.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {  item {
        // Add availability chip at top center
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            AvailabilityChip(isAvailable = isInstantDeliveryAvailable)
        }
    }
        if (currentMenuItems.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No menu items available", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            itemsIndexed(currentMenuItems) { index, menuItem ->
                val color = cardColors[index % cardColors.size]
                MenuItemRow(
                    menuItem = menuItem,
                    cardColor = color,
                    quantity = cartSelection[menuItem.id]?.quantity ?: 0,
                    isEnabled = isInstantDeliveryAvailable,
                    onAddClick = { cartViewModel.addToSelection(menuItem, restaurantId, restaurantName) },
                    onIncrement = { cartViewModel.incrementSelection(menuItem) },
                    onDecrement = { cartViewModel.decrementSelection(menuItem) }
                )
            }
        }
    }
}

@Composable
private fun PreOrderHeader(category: PreOrderCategory, cardColor: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(category.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Order: ${category.startTime} - ${category.endTime}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Delivery: ${category.deliveryTime}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Default.KeyboardDoubleArrowRight, contentDescription = "View ${category.name} menu")
        }
    }
}
@Composable
fun MenuItemRow(
    menuItem: MenuItem,
    cardColor: Color,
    quantity: Int,
    onAddClick: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    isEnabled: Boolean = true
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Name
            Box(modifier = Modifier.weight(1f)) {
                Text(
                    menuItem.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Price - centered in its section
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Tk ${menuItem.price}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isEnabled) MaterialTheme.colorScheme.primary else Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }

            // Quantity Selector - aligned to end of its section
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterEnd
            ) {
                QuantitySelector(
                    quantity = quantity,
                    onAdd = onAddClick,
                    onIncrement = onIncrement,
                    onDecrement = onDecrement,
                    isEnabled = isEnabled
                )
            }
        }
    }
}

@Composable
private fun QuantitySelector(
    quantity: Int,
    onAdd: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    isEnabled: Boolean
) {
    if (quantity == 0) {
        Button(
            onClick = onAdd,
            enabled = isEnabled,
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.size(40.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add to cart")
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDecrement,
                enabled = isEnabled,
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrement quantity")
            }
            Text("$quantity", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Button(
                onClick = onIncrement,
                enabled = isEnabled,
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increment quantity")
            }
        }
    }
}

@Composable
fun BottomBarWithTwoButtons(onAddToCartClick: () -> Unit, onPlaceOrderClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp
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
                modifier = Modifier.height(50.dp)
            ) {
                Icon(Icons.Default.AddShoppingCart, contentDescription = "Add to Cart")
            }
            Button(
                onClick = onPlaceOrderClick,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            ) {
                Text("Place Order Now", fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun AvailabilityChip(isAvailable: Boolean) {
    val backgroundColor = if (isAvailable) Color(0xFFE8F5E9) else Color(0xFFFBE9E7)
    val textColor = if (isAvailable) Color(0xFF2E7D32) else Color(0xFFC62828)
    val text = if (isAvailable) "Instant Delivery Available" else "Instant Delivery Unavailable"
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}