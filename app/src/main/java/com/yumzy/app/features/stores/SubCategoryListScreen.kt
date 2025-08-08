package com.yumzy.userapp.features.stores

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.yumzy.userapp.ui.theme.Black
import com.yumzy.userapp.ui.theme.BrandPink
import com.yumzy.userapp.ui.theme.DeepPink
import com.yumzy.userapp.ui.theme.SoftPink
import com.yumzy.userapp.ui.theme.White
import com.yumzy.userapp.ui.theme.softC
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

    // UI Structure
    Row(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(40.dp)
                .background(White)
        ) {
            IconButton(
                onClick = onBackClicked,
                modifier = Modifier
                    .padding(top = 37.dp)
                    .align(Alignment.TopEnd)
                    .size(60.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BrandPink)
        ) {

            Scaffold(
                topBar = {


                    TopAppBar(
                        title = { Text(
                            text = mainCategoryName,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        ) },
                        actions = {
                            IconButton(onClick = { /* TODO: Navigate to cart */ }) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = "Cart", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                    )
                }
            ) { paddingValues ->
                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.Red)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 16.dp, top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        item {
                           // Announcement box
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
                                    Icon(Icons.Default.WrongLocation, contentDescription = "No Location", modifier = Modifier.size(48.dp), tint = Color.White)
                                    Spacer(Modifier.height(16.dp))
                                    Text("Please set your location in your profile to see available categories.", textAlign = TextAlign.Center, color = Color.White)
                                }
                            }
                        } else if (subCategories.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text("No categories found for your location.", textAlign = TextAlign.Center, color = Color.White)
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
}

@Composable
fun SubCategoryCard(subCategory: SubCategory, itemCount: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .padding(start = 20.dp, end = 20.dp)
    ) {
        // Main card
        Card(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(15.dp),
            elevation = CardDefaults.cardElevation(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 50.dp, end = 40.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = subCategory.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "$itemCount items",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
        }

        // Floating image on the left
        Box(
            modifier = Modifier
                .size(60.dp)
                .align(Alignment.CenterStart)
                .offset(x = (-25).dp)
                .zIndex(2f)
        ) {
            Card(
                modifier = Modifier.fillMaxSize()  .clickable(onClick = onClick),
                shape = CircleShape,
                elevation = CardDefaults.cardElevation(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, DeepPink)
            ) {
                AsyncImage(
                    model = subCategory.imageUrl,
                    contentDescription = subCategory.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .padding(0.dp)
                        ,

                    contentScale = ContentScale.Crop
                )
            }
        }

        // Floating arrow on the right
        Box(
            modifier = Modifier
                .size(40.dp)
                .align(Alignment.CenterEnd)
                .offset(x = 20.dp)
                .zIndex(2f)
        ) {
            Card(
                modifier = Modifier.fillMaxSize().clickable(onClick = onClick),
                shape = CircleShape,
                elevation = CardDefaults.cardElevation(8.dp),
                colors = CardDefaults.cardColors(containerColor = BrandPink)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "View items",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}