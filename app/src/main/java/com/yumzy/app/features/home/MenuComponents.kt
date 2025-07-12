package com.yumzy.app.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// DATA CLASSES NOW LIVE HERE
data class PreOrderCategory(
    val id: String,
    val name: String,
    val startTime: String,
    val endTime: String,
    val deliveryTime: String
)

data class MenuItem(
    val id: String,
    val name: String,
    val price: Double,
    val category: String
)


@Composable
fun SectionHeader(title: String) {
    Row(modifier = Modifier.padding(vertical = 8.dp)) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun PreOrderHeader(category: PreOrderCategory, cardColor: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(category.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Order: ${category.startTime} - ${category.endTime}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Delivery: ${category.deliveryTime}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = "View ${category.name} menu")
        }
    }
}

// UPDATED: MenuItemRow is now a Card and accepts a color
@Composable
fun MenuItemRow(
    menuItem: MenuItem,
    cardColor: Color,
    quantity: Int,
    onAddClick: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(menuItem.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "BDT ${menuItem.price}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(16.dp))
            QuantitySelector(
                quantity = quantity,
                onAdd = onAddClick,
                onIncrement = onIncrement,
                onDecrement = onDecrement
            )
        }
    }
}


@Composable
fun BottomBarWithTwoButtons(onAddToCartClick: () -> Unit, onPlaceOrderClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shadowElevation = 8.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(onClick = onAddToCartClick, modifier = Modifier.height(50.dp)) {
                Icon(Icons.Default.ShoppingCart, contentDescription = "Add to Cart")
            }
            Button(onClick = onPlaceOrderClick, modifier = Modifier.weight(1f).height(50.dp)) {
                Text("Place Order Now", fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun Chip(label: String) {
    Surface(
        color = Color(0xFFE8F5E9),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            color = Color(0xFF2E7D32),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun QuantitySelector(
    quantity: Int,
    onAdd: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    if (quantity == 0) {
        Button(
            onClick = onAdd,
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.size(40.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add to cart")
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDecrement,
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrement quantity")
            }
            Text("$quantity", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Button(
                onClick = onIncrement,
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increment quantity")
            }
        }
    }
}