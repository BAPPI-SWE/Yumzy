package com.yumzy.app.features.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.yumzy.app.R
import com.yumzy.app.ui.theme.BrandPink
import com.yumzy.app.ui.theme.DeepPink

data class Restaurant(
    val ownerId: String,
    val name: String,
    val cuisine: String,
    val deliveryLocations: List<String>,
    val imageUrl: String?
)

data class Category(val name: String, val icon: ImageVector)

@Composable
fun HomeScreen(
    onRestaurantClick: (restaurantId: String, restaurantName: String) -> Unit
) {
    var restaurants by remember { mutableStateOf<List<Restaurant>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val lazyListState = rememberLazyListState()

    val isScrolled by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 50
        }
    }

    LaunchedEffect(key1 = Unit) {
        Firebase.firestore.collection("restaurants")
            .addSnapshotListener { snapshot, _ ->
                isLoading = false
                snapshot?.let {
                    restaurants = it.documents.mapNotNull { doc ->
                        Restaurant(
                            ownerId = doc.id,
                            name = doc.getString("name") ?: "No Name",
                            cuisine = doc.getString("cuisine") ?: "No Cuisine",
                            deliveryLocations = doc.get("deliveryLocations") as? List<String> ?: emptyList(),
                            imageUrl = doc.getString("imageUrl")
                        )
                    }
                }
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandPink) // Set the base background to pink
    ) {
        // The main content with a white background and rounded corners
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 1.dp) // Prevents a small visual glitch
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(MaterialTheme.colorScheme.surface),
            state = lazyListState,
            contentPadding = PaddingValues(top = 110.dp) // IMPORTANT: Pushes content below the top bar
        ) {
            item { SearchBar(modifier = Modifier.padding(horizontal = 16.dp)) }
            item { Spacer(modifier = Modifier.height(16.dp)) }
            item { OfferCard(modifier = Modifier.padding(horizontal = 16.dp)) }
            item { Spacer(modifier = Modifier.height(24.dp)) }
            item { CategorySection(modifier = Modifier.padding(horizontal = 16.dp)) }
            item { Spacer(modifier = Modifier.height(24.dp)) }
            item {
                Text(
                    text = "All Restaurants",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
            }
            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                items(restaurants) { restaurant ->
                    RestaurantCard(
                        restaurant = restaurant,
                        onClick = { onRestaurantClick(restaurant.ownerId, restaurant.name) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            item {
                // Add padding at the bottom to ensure content doesn't hide behind navigation bar
                Spacer(modifier = Modifier.height(60.dp))
            }
        }

        // The Top Bar now just sits on top of the Box
        CollapsingToolbar(isScrolled = isScrolled)
    }
}

@Composable
fun CollapsingToolbar(isScrolled: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BrandPink)
            .statusBarsPadding() // This adds padding for the status bar area
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        AnimatedVisibility(
            visible = !isScrolled,
            enter = slideInVertically(animationSpec = tween(200), initialOffsetY = { -it }),
            exit = slideOutVertically(animationSpec = tween(200), targetOffsetY = { -it })
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = "Location", tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Daffodil Smart City", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Ashulia, Dhaka", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                        }
                    }
                    Row {
                        IconButton(onClick = { /*TODO*/ }) {
                            Icon(Icons.Default.FavoriteBorder, contentDescription = "Favorites", tint = Color.White)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        SearchBar()
    }
}


// All other helper composables (SearchBar, OfferCard, RestaurantCard, etc.) remain the same.

@Composable
fun SearchBar(modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, contentDescription = "Search Icon", tint = Color.Gray)
            Spacer(Modifier.width(8.dp))
            Text("Search for restaurants and groceries", color = Color.Gray)
        }
    }
}

@Composable
fun OfferCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(DeepPink.copy(alpha = 0.8f))) {
            Text(
                text = "Offer Slider Placeholder",
                modifier = Modifier.align(Alignment.Center),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CategorySection(modifier: Modifier = Modifier) {
    val categories = listOf(
        Category("Fast Food", Icons.Default.Fastfood),
        Category("Pharmacy", Icons.Default.LocalPharmacy),
        Category("Personal Care", Icons.Default.Spa),
        Category("Grocery", Icons.Default.ShoppingCart)
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        categories.forEach { category ->
            CategoryItem(category = category)
        }
    }
}

@Composable
fun CategoryItem(category: Category) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { /* TODO */ }
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(DeepPink.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = category.name,
                tint = DeepPink,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = category.name, fontSize = 12.sp)
    }
}

@Composable
fun RestaurantCard(restaurant: Restaurant, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            AsyncImage(
                model = restaurant.imageUrl,
                contentDescription = restaurant.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.ic_shopping_bag),
                error = painterResource(id = R.drawable.ic_shopping_bag)
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = restaurant.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = restaurant.cuisine, color = Color.Gray)
            }
        }
    }
}