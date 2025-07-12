package com.yumzy.app.features.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Spa
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
import coil.compose.AsyncImage // Import Coil's AsyncImage
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.yumzy.app.R
import com.yumzy.app.ui.theme.DeepPink
import com.yumzy.app.ui.theme.YumzyTheme

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

    LaunchedEffect(key1 = Unit) {
        Firebase.firestore.collection("restaurants")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    isLoading = false
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val fetchedRestaurants = snapshot.documents.mapNotNull { doc ->
                        Restaurant(
                            ownerId = doc.id,
                            name = doc.getString("name") ?: "No Name",
                            cuisine = doc.getString("cuisine") ?: "No Cuisine",
                            deliveryLocations = doc.get("deliveryLocations") as? List<String> ?: emptyList(),
                            imageUrl = doc.getString("imageUrl")
                        )
                    }
                    restaurants = fetchedRestaurants
                    isLoading = false
                }
            }
    }

    Scaffold(
        topBar = { TopBar() }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(16.dp)) }
            item { SearchBar() }
            item { Spacer(modifier = Modifier.height(16.dp)) }
            item { OfferCard() }
            item { Spacer(modifier = Modifier.height(24.dp)) }
            item { CategorySection() }
            item { Spacer(modifier = Modifier.height(24.dp)) }

            item {
                Text(
                    text = "All Restaurants",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
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
                    RestaurantCard(restaurant = restaurant, onClick = {
                        onRestaurantClick(restaurant.ownerId, restaurant.name)
                    })
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun TopBar() {
    Column(modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)) {
        Text(text = "Deliver to ▼", color = DeepPink, fontSize = 14.sp)
        Text(
            text = "Hall 1, Daffodil Smart City",
            color = Color.Black,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SearchBar() {
    OutlinedTextField(
        value = "",
        onValueChange = {},
        placeholder = { Text("Search for dishes & restaurants") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
fun OfferCard() {
    Card(
        modifier = Modifier
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
fun CategorySection() {
    val categories = listOf(
        Category("Fast Food", Icons.Default.Fastfood),
        Category("Pharmacy", Icons.Default.LocalPharmacy),
        Category("Personal Care", Icons.Default.Spa),
        Category("Grocery", Icons.Default.ShoppingCart)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
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
        modifier = Modifier.clickable { /* TODO: Handle category click */ }
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
fun RestaurantCard(restaurant: Restaurant, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // Replace the old Image composable with Coil's AsyncImage
            AsyncImage(
                model = restaurant.imageUrl,
                contentDescription = restaurant.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                contentScale = ContentScale.Crop,
                // Show a placeholder while the image loads
                placeholder = painterResource(id = R.drawable.ic_shopping_bag),
                // Show an error image if the URL is bad or network fails
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