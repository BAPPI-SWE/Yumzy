package com.yumzy.userapp.features.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.yumzy.userapp.R
import com.yumzy.userapp.YLogoLoadingIndicator
import com.yumzy.userapp.ui.theme.BrandPink
import com.yumzy.userapp.ui.theme.DeepPink
import com.yumzy.userapp.ui.theme.DarkPink
import kotlinx.coroutines.delay

// --- MODIFICATION 1: Update Offer data class ---
data class Offer(
    val imageUrl: String = "",
    val availableLocations: List<String> = emptyList()
)

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
    var restaurants by remember { mutableStateOf<List<Restaurant>>(emptyList()) }
    var offers by remember { mutableStateOf<List<Offer>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    val lazyListState = rememberLazyListState()

    val searchedRestaurants by remember(searchQuery, restaurants) {
        derivedStateOf {
            if (searchQuery.isBlank()) {
                restaurants
            } else {
                restaurants.filter { restaurant ->
                    restaurant.name.contains(searchQuery, ignoreCase = true) ||
                            restaurant.cuisine.contains(searchQuery, ignoreCase = true)
                }
            }
        }
    }

    val isScrolled by remember {
        derivedStateOf { lazyListState.firstVisibleItemIndex > 0 }
    }

    LaunchedEffect(key1 = Unit) {
        val db = Firebase.firestore
        val currentUser = Firebase.auth.currentUser

        if (currentUser != null) {
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    userProfile = if (document != null && document.exists()) {
                        UserProfile(
                            baseLocation = document.getString("baseLocation") ?: "Campus",
                            subLocation = document.getString("subLocation") ?: ""
                        )
                    } else {
                        UserProfile(subLocation = "")
                    }
                }
        } else {
            isLoading = false
        }
    }

    LaunchedEffect(key1 = userProfile) {
        if (userProfile != null && userProfile!!.subLocation.isNotBlank()) {
            isLoading = true
            val db = Firebase.firestore
            val userLocation = userProfile!!.subLocation

            db.collection("restaurants")
                .whereArrayContains("deliveryLocations", userLocation)
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

            db.collection("offers")
                .whereArrayContains("availableLocations", userLocation)
                .get()
                .addOnSuccessListener { snapshot ->
                    offers = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Offer::class.java)
                    }
                }

        } else if (userProfile != null) {
            isLoading = false
            restaurants = emptyList()
            offers = emptyList()
        }
    }

    Scaffold(
        topBar = {
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
                .padding(paddingValues)
                .padding(bottom = 75.dp),
            state = lazyListState
        ) {
            if (offers.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(10.dp))
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
            item { Spacer(modifier = Modifier.height(20.dp)) }
            item {
                Text(
                    text = "Restaurants Near You",
                    fontSize = 20.sp,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 18.dp, bottom = 11.dp)
                )
            }
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        YLogoLoadingIndicator(
                            size = 35.dp,
                            color = BrandPink
                        )
                    }
                }
            }
            else if (userProfile?.subLocation.isNullOrBlank()) {
                item {
                    EmptyStateMessage(
                        icon = Icons.Default.WrongLocation,
                        message = "Please set your delivery hall/building in your profile to find nearby restaurants."
                    )
                }
            } else if (searchedRestaurants.isEmpty()) {
                item {
                    EmptyStateMessage(
                        icon = Icons.Default.SearchOff,
                        message = if (searchQuery.isNotBlank()) "No restaurants match your search." else "No restaurants currently deliver to your location."
                    )
                }
            } else {
                items(searchedRestaurants) { restaurant ->
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

@Composable
fun EmptyStateMessage(icon: ImageVector, message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp, start = 32.dp, end = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Empty State",
            modifier = Modifier.size(64.dp),
            tint = Color.Gray.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun HomeTopBar(
    isScrolled: Boolean,
    userProfile: UserProfile?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {
    val gradientColors = listOf(
        BrandPink,
        DarkPink
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = if (isScrolled) 4.dp else 0.dp,
        shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = gradientColors,
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    ),
                    shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 4.dp, bottom = 12.dp)
                    .statusBarsPadding()
            ) {
                AnimatedVisibility(
                    visible = !isScrolled,
                    enter = expandVertically(animationSpec = tween(300)),
                    exit = shrinkVertically(animationSpec = tween(300))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = "Location",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = userProfile?.baseLocation ?: "...",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )

                            if (!userProfile?.subLocation.isNullOrBlank()) {
                                Text(
                                    text = userProfile?.subLocation ?: "",
                                    color = Color.White.copy(alpha = 0.90f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                        IconButton(
                            onClick = { /*TODO*/ },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.FavoriteBorder,
                                contentDescription = "Favorites",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = { /*TODO*/ },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.NotificationsNone,
                                contentDescription = "Cart",
                                tint = Color.White,
                                modifier = Modifier.size(23.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(if (isScrolled) 8.dp else 12.dp))

                ModernSearchBar(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange
                )
            }
        }
    }
}

@Composable
fun ModernSearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                clip = true
            )
            .background(
                color = Color.White.copy(alpha = 0.95f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { focusRequester.requestFocus() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = "Search Icon",
                tint = if (isFocused) BrandPink else Color.Gray,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .onFocusChanged { isFocused = it.isFocused },
                textStyle = TextStyle(
                    color = Color.Black,
                    fontSize = 14.sp
                ),
                singleLine = true,
                cursorBrush = SolidColor(BrandPink),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { focusManager.clearFocus() }
                ),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            "Search Your Favourite Restaurants",
                            color = Color.Gray.copy(alpha = 1f),
                            fontSize = 14.sp
                        )
                    }
                    innerTextField()
                }
            )

            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = Color.Gray.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

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
        modifier = Modifier.height(140.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        pageSpacing = 12.dp
    ) { page ->
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            AsyncImage(
                model = offers[page].imageUrl,
                contentDescription = "Offer",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun CategorySection(modifier: Modifier = Modifier, onCategoryClick: (categoryId: String, categoryName: String) -> Unit) {
    val categories = listOf(
        Category("Fast Food", Icons.Default.Fastfood, "fast_food"),
        Category("Pharmacy", Icons.Default.LocalPharmacy, "pharmacy"),
        Category("Friday Deal", Icons.Default.Celebration, "personal_care"),
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
    // Add animation only for "Friday Deal"
    val scale by if (category.id == "personal_care") {
        rememberInfiniteTransition().animateFloat(
            initialValue = 1f,
            targetValue = 1.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = EaseInOutCubic),
                repeatMode = RepeatMode.Reverse
            )
        )
    } else {
        remember { mutableStateOf(1f) }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(DeepPink.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = category.name,
                tint = DeepPink,
                modifier = Modifier
                    .size(28.dp)
                    .scale(scale) // Apply animation here
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = category.name,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun RestaurantCard(restaurant: Restaurant, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var isFavorite by rememberSaveable { mutableStateOf(false) }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                clip = false
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    AsyncImage(
                        model = restaurant.imageUrl,
                        contentDescription = restaurant.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = R.drawable.ic_shopping_bag),
                        error = painterResource(id = R.drawable.ic_shopping_bag)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.1f)
                                    ),
                                    startY = 0f,
                                    endY = Float.POSITIVE_INFINITY
                                ),
                                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                            )
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(32.dp)
                                .clickable { isFavorite = !isFavorite },
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.9f),
                            shadowElevation = 4.dp
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Add to favorites",
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(20.dp),
                                tint = if (isFavorite) Color.Red else Color.Gray.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text(
                        text = restaurant.name,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C2C2C),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(1.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = "Cuisine",
                            modifier = Modifier.size(14.dp),
                            tint = BrandPink.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = restaurant.cuisine,
                            fontSize = 14.sp,
                            color = Color.Gray.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified Shop",
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFFFFA726)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Verified",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2C2C2C)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = "Delivery time",
                                modifier = Modifier.size(14.dp),
                                tint = Color.Gray.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Deliver in Time",
                                fontSize = 12.sp,
                                color = Color.Gray.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp)),
                color = Color.Transparent
            ) {}
        }
    }
}
