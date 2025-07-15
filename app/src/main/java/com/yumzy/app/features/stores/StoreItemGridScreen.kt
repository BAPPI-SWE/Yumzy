package com.yumzy.app.features.stores

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.yumzy.app.features.cart.CartViewModel
import com.yumzy.app.features.home.QuantitySelector

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
    cartViewModel: CartViewModel = viewModel()
) {
    var items by remember { mutableStateOf<List<StoreItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val cartSelection by cartViewModel.currentSelection.collectAsState()

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
                        // We pass a generic ID and Name for these store items
                        restaurantId = "yumzy_store",
                        restaurantName = "Yumzy Store",
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
    restaurantId: String,
    restaurantName: String,
    quantity: Int,
    cartViewModel: CartViewModel
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentScale = ContentScale.Crop
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Tk ${item.price}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    // We reuse the same QuantitySelector here!
                    QuantitySelector(
                        quantity = quantity,
                        onAdd = {
                            // We need to convert StoreItem to a MenuItem for the cart
                            val menuItem = com.yumzy.app.features.home.MenuItem(
                                id = item.id,
                                name = item.name,
                                price = item.price,
                                category = "Store Item"
                            )
                            cartViewModel.addToSelection(menuItem, restaurantId, restaurantName)
                        },
                        onIncrement = {
                            val menuItem = com.yumzy.app.features.home.MenuItem(id = item.id, name = item.name, price = item.price, category = "Store Item")
                            cartViewModel.incrementSelection(menuItem)
                        },
                        onDecrement = {
                            val menuItem = com.yumzy.app.features.home.MenuItem(id = item.id, name = item.name, price = item.price, category = "Store Item")
                            cartViewModel.decrementSelection(menuItem)
                        }
                    )
                }
            }
        }
    }
}