package com.yumzy.app.navigation

import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.yumzy.app.features.cart.CartScreen
import com.yumzy.app.features.cart.CartViewModel
import com.yumzy.app.features.cart.CheckoutScreen
import com.yumzy.app.features.home.HomeScreen
import com.yumzy.app.features.home.PreOrderCategoryMenuScreen
import com.yumzy.app.features.home.RestaurantMenuScreen
import com.yumzy.app.features.orders.OrdersScreen
import com.yumzy.app.features.profile.AccountScreen
import com.yumzy.app.features.profile.EditProfileScreen
import com.yumzy.app.features.stores.StoreItemGridScreen
import com.yumzy.app.features.stores.SubCategoryListScreen
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Home : Screen("home", "Home", Icons.Default.Home)
    data object Cart : Screen("cart", "Cart", Icons.Default.ShoppingCart)
    data object Orders : Screen("orders", "Orders", Icons.Default.ReceiptLong)
    data object Account : Screen("account", "Account", Icons.Default.AccountCircle)
    data object RestaurantMenu : Screen("restaurant_menu", "Menu", Icons.Default.Home)
    data object PreOrderCategoryMenu : Screen("preorder_menu", "Pre-Order Menu", Icons.Default.Home)
    data object SubCategoryList : Screen("sub_category_list", "Sub-Categories", Icons.Default.Home)
    data object StoreItemGrid : Screen("store_item_grid", "Store Items", Icons.Default.Home)
    data object Checkout : Screen("checkout", "Checkout", Icons.Default.Home)
    data object EditUserProfile : Screen("edit_user_profile", "Edit Profile", Icons.Default.Edit)
}

@Composable
fun MainScreen(onSignOut: () -> Unit) {
    val navController = rememberNavController()
    val cartViewModel: CartViewModel = viewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val bottomBarItems = listOf(
        Screen.Home,
        Screen.Cart,
        Screen.Orders,
        Screen.Account,
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                bottomBarItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.secondary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        ),
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController, startDestination = Screen.Home.route, Modifier.padding(innerPadding)) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onRestaurantClick = { restaurantId, restaurantName ->
                        val encodedName = URLEncoder.encode(restaurantName, StandardCharsets.UTF_8.toString())
                        navController.navigate("${Screen.RestaurantMenu.route}/$restaurantId/$encodedName")
                    },
                    onStoreCategoryClick = { categoryId, categoryName ->
                        val encodedCatName = URLEncoder.encode(categoryName, StandardCharsets.UTF_8.toString())
                        navController.navigate("${Screen.SubCategoryList.route}/$categoryId/$encodedCatName")
                    }
                )
            }
            composable(Screen.Cart.route) {
                CartScreen(
                    cartViewModel = cartViewModel,
                    onPlaceOrder = { restaurantId ->
                        navController.navigate("${Screen.Checkout.route}/$restaurantId")
                    }
                )
            }
            composable(Screen.Orders.route) { OrdersScreen() }
            composable(Screen.Account.route) { backStackEntry ->
                val shouldRefresh = backStackEntry.savedStateHandle.get<Boolean>("refresh_profile") ?: true
                AccountScreen(
                    onSignOut = onSignOut,
                    onNavigateToEditProfile = {
                        navController.navigate(Screen.EditUserProfile.route)
                    },
                    refreshTrigger = shouldRefresh
                )
                backStackEntry.savedStateHandle["refresh_profile"] = false
            }

            composable(
                route = "${Screen.RestaurantMenu.route}/{restaurantId}/{restaurantName}",
                arguments = listOf(
                    navArgument("restaurantId") { type = NavType.StringType },
                    navArgument("restaurantName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val restaurantId = backStackEntry.arguments?.getString("restaurantId") ?: ""
                val encodedName = backStackEntry.arguments?.getString("restaurantName") ?: ""
                val restaurantName = URLDecoder.decode(encodedName, StandardCharsets.UTF_8.toString())

                RestaurantMenuScreen(
                    restaurantId = restaurantId,
                    restaurantName = restaurantName,
                    cartViewModel = cartViewModel,
                    onCategoryClick = { restId, restName, categoryName ->
                        val encodedRestName = URLEncoder.encode(restName, StandardCharsets.UTF_8.toString())
                        val encodedCatName = URLEncoder.encode(categoryName, StandardCharsets.UTF_8.toString())
                        navController.navigate("${Screen.PreOrderCategoryMenu.route}/$restId/$encodedRestName/$encodedCatName")
                    },
                    onBackClicked = { navController.popBackStack() }
                )
            }

            composable(
                route = "${Screen.PreOrderCategoryMenu.route}/{restaurantId}/{restaurantName}/{categoryName}",
                arguments = listOf(
                    navArgument("restaurantId") { type = NavType.StringType },
                    navArgument("restaurantName") { type = NavType.StringType },
                    navArgument("categoryName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val restaurantId = backStackEntry.arguments?.getString("restaurantId") ?: ""
                val encodedRestaurantName = backStackEntry.arguments?.getString("restaurantName") ?: ""
                val restaurantName = URLDecoder.decode(encodedRestaurantName, StandardCharsets.UTF_8.toString())
                val encodedCategoryName = backStackEntry.arguments?.getString("categoryName") ?: ""
                val categoryName = URLDecoder.decode(encodedCategoryName, StandardCharsets.UTF_8.toString())

                PreOrderCategoryMenuScreen(
                    restaurantId = restaurantId,
                    restaurantName = restaurantName,
                    categoryName = categoryName,
                    cartViewModel = cartViewModel,
                    onBackClicked = { navController.popBackStack() }
                )
            }

            composable(
                route = "${Screen.SubCategoryList.route}/{mainCategoryId}/{mainCategoryName}",
                arguments = listOf(
                    navArgument("mainCategoryId") { type = NavType.StringType },
                    navArgument("mainCategoryName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val mainCategoryId = backStackEntry.arguments?.getString("mainCategoryId") ?: ""
                val mainCategoryName = URLDecoder.decode(backStackEntry.arguments?.getString("mainCategoryName") ?: "", StandardCharsets.UTF_8.toString())

                SubCategoryListScreen(
                    mainCategoryId = mainCategoryId,
                    mainCategoryName = mainCategoryName,
                    onSubCategoryClick = { subCategoryName ->
                        val encodedSubCatName = URLEncoder.encode(subCategoryName, StandardCharsets.UTF_8.toString())
                        navController.navigate("${Screen.StoreItemGrid.route}/$encodedSubCatName")
                    },
                    onBackClicked = { navController.popBackStack() }
                )
            }

            composable(
                route = "${Screen.StoreItemGrid.route}/{subCategoryName}",
                arguments = listOf(navArgument("subCategoryName") { type = NavType.StringType })
            ) { backStackEntry ->
                val subCategoryName = URLDecoder.decode(backStackEntry.arguments?.getString("subCategoryName") ?: "", StandardCharsets.UTF_8.toString())
                StoreItemGridScreen(
                    subCategoryName = subCategoryName,
                    onBackClicked = { navController.popBackStack() },
                    cartViewModel = cartViewModel
                )
            }

            composable(
                route = "${Screen.Checkout.route}/{restaurantId}",
                arguments = listOf(navArgument("restaurantId") { type = NavType.StringType })
            ) { backStackEntry ->
                val restaurantId = backStackEntry.arguments?.getString("restaurantId") ?: ""
                val savedCartState by cartViewModel.savedCart.collectAsState()
                val itemsForRestaurant = savedCartState.values.filter { it.restaurantId == restaurantId }

                CheckoutScreen(
                    cartItems = itemsForRestaurant,
                    onBackClicked = { navController.popBackStack() },
                    onConfirmOrder = {
                        scope.launch {
                            val user = Firebase.auth.currentUser ?: return@launch

                            Firebase.firestore.collection("users").document(user.uid).get()
                                .addOnSuccessListener { userDoc ->
                                    val totalPrice = itemsForRestaurant.sumOf { it.menuItem.price * it.quantity }
                                    val finalTotal = totalPrice + 20.0 + 5.0
                                    val orderItems = itemsForRestaurant.map { mapOf(
                                        "itemName" to it.menuItem.name,
                                        "quantity" to it.quantity,
                                        "price" to it.menuItem.price
                                    )}
                                    val firstItemCategory = itemsForRestaurant.firstOrNull()?.menuItem?.category ?: ""
                                    val orderType = if (firstItemCategory.startsWith("Pre-order")) "PreOrder" else "Instant"

                                    val newOrder = hashMapOf(
                                        "userId" to user.uid,
                                        "userName" to (userDoc.getString("name") ?: "N/A"),
                                        "userPhone" to (userDoc.getString("phone") ?: "N/A"),
                                        "userBaseLocation" to (userDoc.getString("baseLocation") ?: "N/A"),
                                        "userSubLocation" to (userDoc.getString("subLocation") ?: "N/A"),
                                        "building" to (userDoc.getString("building") ?: ""),
                                        "floor" to (userDoc.getString("floor") ?: ""),
                                        "room" to (userDoc.getString("room") ?: ""),
                                        "restaurantId" to restaurantId,
                                        "restaurantName" to itemsForRestaurant.first().restaurantName,
                                        "totalPrice" to finalTotal,
                                        "items" to orderItems,
                                        "orderStatus" to "Pending",
                                        "createdAt" to Timestamp.now(),
                                        "orderType" to orderType,
                                        "preOrderCategory" to if (orderType == "PreOrder") firstItemCategory else ""
                                    )

                                    Firebase.firestore.collection("orders").add(newOrder)
                                        .addOnSuccessListener {
                                            cartViewModel.clearCartForRestaurant(restaurantId)
                                            Toast.makeText(context, "Order placed successfully!", Toast.LENGTH_SHORT).show()
                                            navController.navigate(Screen.Orders.route) { popUpTo(Screen.Home.route) }
                                        }
                                }
                        }
                    }
                )
            }

            composable(Screen.EditUserProfile.route) {
                EditProfileScreen(
                    onBackClicked = { navController.popBackStack() },
                    onSaveChanges = { name, phone, baseLocation, subLocation, building, floor, room ->
                        val user = Firebase.auth.currentUser
                        if (user != null) {
                            val profileUpdates = userProfileChangeRequest { displayName = name }
                            user.updateProfile(profileUpdates)

                            val firestoreUpdates = mapOf(
                                "name" to name, "phone" to phone,
                                "baseLocation" to baseLocation, "subLocation" to subLocation,
                                "building" to building, "floor" to floor, "room" to room
                            )
                            Firebase.firestore.collection("users").document(user.uid)
                                .update(firestoreUpdates)
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Profile Updated", Toast.LENGTH_SHORT).show()
                                    navController.previousBackStackEntry?.savedStateHandle?.set("refresh_profile", true)
                                    navController.popBackStack()
                                }
                        }
                    }
                )
            }
        }
    }
}