package com.yumzy.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.yumzy.app.features.cart.CartScreen
import com.yumzy.app.features.cart.CartViewModel
import com.yumzy.app.features.home.HomeScreen
import com.yumzy.app.features.home.PreOrderCategoryMenuScreen
import com.yumzy.app.features.home.RestaurantMenuScreen
import com.yumzy.app.features.orders.OrdersScreen
import com.yumzy.app.features.profile.AccountScreen
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
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val cartViewModel: CartViewModel = viewModel()

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
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
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
                    }
                )
            }
            composable(Screen.Cart.route) {
                CartScreen(cartViewModel = cartViewModel)
            }
            composable(Screen.Orders.route) { OrdersScreen() }
            composable(Screen.Account.route) { AccountScreen() }

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
                    onCategoryClick = { restId, categoryName ->
                        val encodedRestName = URLEncoder.encode(restaurantName, StandardCharsets.UTF_8.toString())
                        val encodedCatName = URLEncoder.encode(categoryName, StandardCharsets.UTF_8.toString())
                        // Pass the restaurant name along with the category name
                        navController.navigate("${Screen.PreOrderCategoryMenu.route}/$restId/$encodedRestName/$encodedCatName")
                    }
                )
            }

            composable(
                // Update route to accept restaurantName
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
                    cartViewModel = cartViewModel
                )
            }
        }
    }
}