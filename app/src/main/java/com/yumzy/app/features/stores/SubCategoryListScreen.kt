package com.yumzy.app.features.stores

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.WrongLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.yumzy.app.ui.theme.BrandPink
import kotlinx.coroutines.tasks.await

data class SubCategory(
    val id: String,
    val name: String,
    val imageUrl: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubCategoryListScreen(
    mainCategoryId: String,
    mainCategoryName: String,
    onSubCategoryClick: (subCategoryName: String) -> Unit,
    onBackClicked: () -> Unit
) {
    var subCategories by remember { mutableStateOf<List<SubCategory>>(emptyList()) }
    var itemCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    // Add state to hold the user's location
    var userSubLocation by remember { mutableStateOf<String?>(null) }


    LaunchedEffect(key1 = mainCategoryId) {
        val db = Firebase.firestore
        val currentUser = Firebase.auth.currentUser

        // --- MODIFICATION START ---

        // 1. Get the current user's sub-location first.
        val location = if (currentUser != null) {
            try {
                val userDoc = db.collection("users").document(currentUser.uid).get().await()
                userDoc.getString("subLocation")
            } catch (e: Exception) {
                null // Handle potential exceptions
            }
        } else {
            null
        }
        userSubLocation = location

        // 2. Only query for categories if the user has a location set.
        if (!location.isNullOrBlank()) {
            db.collection("store_sub_categories")
                // Query 1: Match the parent category (e.g., "Fast Food")
                .whereEqualTo("parentCategory", mainCategoryId)
                // Query 2: ALSO match the user's location within the new array field.
                .whereArrayContains("availableLocations", location)
                .get()
                .addOnSuccessListener { subCatSnapshot ->
                    val fetchedSubCats = subCatSnapshot.documents.mapNotNull { doc ->
                        SubCategory(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            imageUrl = doc.getString("imageUrl") ?: ""
                        )
                    }
                    subCategories = fetchedSubCats

                    if (fetchedSubCats.isNotEmpty()) {
                        val subCategoryNames = fetchedSubCats.map { it.name }
                        db.collection("store_items")
                            .whereIn("subCategory", subCategoryNames)
                            .get()
                            .addOnSuccessListener { itemsSnapshot ->
                                itemCounts = itemsSnapshot.documents
                                    .mapNotNull { it.getString("subCategory") }
                                    .groupingBy { it }
                                    .eachCount()
                                isLoading = false
                            }
                    } else {
                        isLoading = false
                    }
                }.addOnFailureListener {
                    // Handle failure
                    isLoading = false
                }
        } else {
            // If user has no location, there's nothing to show.
            isLoading = false
        }
        // --- MODIFICATION END ---
    }

    // UI Structure (mostly unchanged)
    Row(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(60.dp)
                .background(BrandPink)
        ) {
            IconButton(
                onClick = onBackClicked,
                modifier = Modifier
                    .padding(top = 40.dp)
                    .align(Alignment.TopCenter)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { },
                    actions = {
                        IconButton(onClick = { /* TODO: Navigate to cart */ }) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = "Cart", tint = Color.Gray)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { paddingValues ->
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    item {
                        Text(
                            text = mainCategoryName,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(16.dp))
                    }

                    // --- UI MESSAGE MODIFICATION ---
                    if (userSubLocation.isNullOrBlank()) {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.WrongLocation, contentDescription = "No Location", modifier = Modifier.size(48.dp), tint = Color.Gray)
                                Spacer(Modifier.height(16.dp))
                                Text("Please set your location in your profile to see available categories.", textAlign = TextAlign.Center, color = Color.Gray)
                            }
                        }
                    } else if (subCategories.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("No categories found for your location.", textAlign = TextAlign.Center, color = Color.Gray)
                            }
                        }
                    } else {
                        items(subCategories) { subCategory ->
                            SubCategoryCard(
                                subCategory = subCategory,
                                itemCount = itemCounts[subCategory.name] ?: 0,
                                onClick = { onSubCategoryClick(subCategory.name) }
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun SubCategoryCard(subCategory: SubCategory, itemCount: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = subCategory.imageUrl,
                contentDescription = subCategory.name,
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(subCategory.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("$itemCount Items", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = "View items", tint = BrandPink)
        }
    }
}