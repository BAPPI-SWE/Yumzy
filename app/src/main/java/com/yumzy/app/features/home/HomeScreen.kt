package com.yumzy.app.features.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.yumzy.app.R
import com.yumzy.app.ui.theme.BrandPink
import com.yumzy.app.ui.theme.DeepPink
import kotlinx.coroutines.delay

// Your data classes from the stable version
data class Offer(val imageUrl: String = "")
data class Restaurant(val ownerId: String, val name: String, val cuisine: String, val deliveryLocations: List<String>, val imageUrl: String?)
data class Category(val name: String, val icon: ImageVector, val id: String)
data class UserProfile(val baseLocation: String = "", val subLocation: String = "")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onRestaurantClick: (restaurantId: String, restaurantName: String) -> Unit,
    onStoreCategoryClick: (categoryId: String, categoryName: String) -> Unit
) {
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var allRestaurants by remember { mutableStateOf<List<Restaurant>>(emptyList()) } // Renamed to clarify it's the full list
    var offers by remember { mutableStateOf<List<Offer>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val lazyListState = rememberLazyListState()

    // NEW: State to hold the user's search text
    var searchQuery by remember { mutableStateOf("") }

    // NEW: A derived state that filters the list based on the search query
    val filteredRestaurants by remember(searchQuery, allRestaurants) {
        derivedStateOf {
            if (searchQuery.isBlank()) {
                allRestaurants
            } else {
                allRestaurants.filter { restaurant ->
                    restaurant.name.contains(searchQuery, ignoreCase = true) ||
                            restaurant.cuisine.contains(searchQuery, ignoreCase = true)
                }
            }
        }
    }

    val isScrolled by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0
        }
    }

    LaunchedEffect(key1 = Unit) {
        val db = Firebase.firestore
        val currentUser = Firebase.auth.currentUser

        if (currentUser != null) {
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        userProfile = UserProfile(
                            baseLocation = document.getString("baseLocation") ?: "Campus",
                            subLocation = document.getString("subLocation") ?: "Building"
                        )
                    }
                }
        }

        db.collection("restaurants")
            .addSnapshotListener { snapshot, _ ->
                isLoading = false
                snapshot?.let {
                    // Store the full list here
                    allRestaurants = it.documents.mapNotNull { doc ->
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
        db.collection("offers").get()
            .addOnSuccessListener { snapshot ->
                offers = snapshot.documents.mapNotNull { doc ->
                    Offer(imageUrl = doc.getString("imageUrl") ?: "")
                }
            }
    }

    Scaffold(
        topBar = {
            // UPDATE: Pass the search query and the function to update it
            HomeTopBar(
                isScrolled = isScrolled,
                userProfile = userProfile,
                searchQuery = searchQuery,
                onSearchQueryChange = { newQuery -> searchQuery = newQuery }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            state = lazyListState
        ) {
            item {
                if (offers.isNotEmpty()) {
                    OfferSlider(offers = offers)
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
            item {
                CategorySection(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    onCategoryClick = onStoreCategoryClick
                )
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
            item {
                Text(
                    text = "All Restaurants",
                    style = MaterialTheme.typography.titleLarge,
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
                // UPDATE: Use the new filtered list here
                items(filteredRestaurants) { restaurant ->
                    RestaurantCard(
                        restaurant = restaurant,
                        onClick = { onRestaurantClick(restaurant.ownerId, restaurant.name) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

// UPDATE: HomeTopBar now accepts the search query and a function to change it
@Composable
fun HomeTopBar(
    isScrolled: Boolean,
    userProfile: UserProfile?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 4.dp,
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(BrandPink)
                .statusBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            AnimatedVisibility(
                visible = !isScrolled,
                enter = expandVertically(animationSpec = tween(300)),
                exit = shrinkVertically(animationSpec = tween(300))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = "Location", tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(userProfile?.baseLocation ?: "...", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(userProfile?.subLocation ?: "...", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                    }
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(Icons.Default.FavoriteBorder, contentDescription = "Favorites", tint = Color.White)
                    }
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = "Cart", tint = Color.White)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            // UPDATE: Pass the query and change handler to the SearchBar
            SearchBar(
                modifier = Modifier.padding(horizontal = 16.dp),
                query = searchQuery,
                onQueryChange = onSearchQueryChange
            )
        }
    }
}

// UPDATE: SearchBar is now a real, functional TextField
@Composable
fun SearchBar(
    modifier: Modifier = Modifier,
    query: String,
    onQueryChange: (String) -> Unit
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("Search for restaurants and groceries", color = Color.Gray) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon", tint = Color.Gray) },
        shape = RoundedCornerShape(50),
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            cursorColor = BrandPink
        )
    )
}

// All other helper composables (OfferSlider, CategorySection, etc.) remain unchanged from your stable version.
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OfferSlider(offers: List<Offer>) {
    val pagerState = rememberPagerState(pageCount = { offers.size })
    LaunchedEffect(Unit) {
        while (true) {
            delay(4000)
            if (pagerState.pageCount > 0) {
                val nextPage = (pagerState.currentPage + 1) % pagerState.pageCount
                pagerState.animateScrollToPage(page = nextPage, animationSpec = tween(600))
            }
        }
    }
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.height(150.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        pageSpacing = 12.dp
    ) { page ->
        Card(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(16.dp)) {
            AsyncImage(model = offers[page].imageUrl, contentDescription = "Offer", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
fun CategorySection(modifier: Modifier = Modifier, onCategoryClick: (categoryId: String, categoryName: String) -> Unit) {
    val categories = listOf(
        Category("Fast Food", Icons.Default.Fastfood, "fast_food"),
        Category("Pharmacy", Icons.Default.LocalPharmacy, "pharmacy"),
        Category("Personal Care", Icons.Default.Spa, "personal_care"),
        Category("Grocery", Icons.Default.ShoppingCart, "grocery")
    )
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
        categories.forEach { category ->
            CategoryItem(category = category, onClick = { onCategoryClick(category.id, category.name) })
        }
    }
}

@Composable
fun CategoryItem(category: Category, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(DeepPink.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
            Icon(imageVector = category.icon, contentDescription = category.name, tint = DeepPink, modifier = Modifier.size(32.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = category.name, fontSize = 12.sp)
    }
}

@Composable
fun RestaurantCard(restaurant: Restaurant, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column {
            AsyncImage(model = restaurant.imageUrl, contentDescription = restaurant.name, modifier = Modifier.fillMaxWidth().height(140.dp), contentScale = ContentScale.Crop, placeholder = painterResource(id = R.drawable.ic_shopping_bag), error = painterResource(id = R.drawable.ic_shopping_bag))
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = restaurant.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = restaurant.cuisine, color = Color.Gray)
            }
        }
    }
}