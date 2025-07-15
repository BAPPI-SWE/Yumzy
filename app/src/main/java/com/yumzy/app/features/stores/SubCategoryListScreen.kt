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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.yumzy.app.ui.theme.BrandPink

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
    var itemCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) } // State to hold counts
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(key1 = mainCategoryId) {
        val db = Firebase.firestore

        // Step 1: Fetch the list of sub-categories
        db.collection("store_sub_categories")
            .whereEqualTo("parentCategory", mainCategoryId)
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

                // Step 2: If sub-categories exist, fetch all their items in a single query
                if (fetchedSubCats.isNotEmpty()) {
                    val subCategoryNames = fetchedSubCats.map { it.name }
                    db.collection("store_items")
                        .whereIn("subCategory", subCategoryNames)
                        .get()
                        .addOnSuccessListener { itemsSnapshot ->
                            // Step 3: Group the items by sub-category name and count them
                            itemCounts = itemsSnapshot.documents
                                .mapNotNull { it.getString("subCategory") }
                                .groupingBy { it }
                                .eachCount()
                            isLoading = false
                        }
                } else {
                    isLoading = false
                }
            }
    }

    // The rest of the UI is the same, but it will now use the dynamic itemCounts map
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
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
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
                    items(subCategories) { subCategory ->
                        SubCategoryCard(
                            subCategory = subCategory,
                            // Pass the correct count from our map, defaulting to 0
                            itemCount = itemCounts[subCategory.name] ?: 0,
                            onClick = { onSubCategoryClick(subCategory.name) }
                        )
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
                // Display the dynamic item count
                Text("$itemCount Items", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = "View items", tint = BrandPink)
        }
    }
}