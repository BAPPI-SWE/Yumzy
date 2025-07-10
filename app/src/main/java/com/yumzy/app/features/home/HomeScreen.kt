package com.yumzy.app.features.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yumzy.app.R
import com.yumzy.app.ui.theme.DeepPink
import com.yumzy.app.ui.theme.YumzyTheme

// --- Data Models (for placeholder data) ---
data class Category(val name: String, val icon: ImageVector)
data class Restaurant(val id: String, val name: String, val cuisine: String, val distance: String, val imageUrl: Int)

// --- Main Composable ---
@Composable
fun HomeScreen() {
    // This is placeholder data. We will fetch this from Firebase later.
    val restaurants = listOf(
        Restaurant("1", "Kacchi Bhai", "Biryani, Kebab", "1.2 km", R.drawable.ic_shopping_bag),
        Restaurant("2", "CP Five Star", "Fried Chicken, Burger", "0.8 km", R.drawable.ic_shopping_bag),
        Restaurant("3", "Pizza Hut", "Pizza, Pasta", "2.1 km", R.drawable.ic_shopping_bag)
    )

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

            // Restaurant List
            items(restaurants) { restaurant ->
                RestaurantCard(restaurant = restaurant)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// --- UI Components ---
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
        // We'll use a simple background color for now.
        // Later, this can be an image from the admin panel.
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
fun RestaurantCard(restaurant: Restaurant) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            Image(
                // Using a placeholder drawable for now.
                // Later we'll load this from a URL.
                painter = painterResource(id = restaurant.imageUrl),
                contentDescription = restaurant.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = restaurant.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "${restaurant.cuisine} • ${restaurant.distance}", color = Color.Gray)
            }
        }
    }
}

// --- Preview ---
@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    YumzyTheme {
        HomeScreen()
    }
}