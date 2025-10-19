package com.yumzy.userapp.features.profile

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.yumzy.userapp.YLogoLoadingIndicator
import com.yumzy.userapp.ui.theme.DarkPink
import com.yumzy.userapp.ui.theme.DeepPink

data class UserProfileDetails(
    val name: String = "...",
    val email: String = "...",
    val photoUrl: String? = null,
    val phone: String = "...",
    val fullAddress: String = "..."
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    onSignOut: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    refreshTrigger: Boolean // New parameter to trigger data reload
) {
    var userProfile by remember { mutableStateOf<UserProfileDetails?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showAppInfoDialog by remember { mutableStateOf(false) }

    // Animation for blinking border
    val infiniteTransition = rememberInfiniteTransition(label = "blinking")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    // This LaunchedEffect will now re-run whenever the 'refreshTrigger' changes
    LaunchedEffect(key1 = refreshTrigger) {
        val currentUser = Firebase.auth.currentUser
        if (currentUser != null) {
            isLoading = true
            Firebase.firestore.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val address = "${document.getString("subLocation")}, ${document.getString("baseLocation")}"
                        userProfile = UserProfileDetails(
                            name = document.getString("name") ?: (currentUser.displayName ?: "N/A"),
                            email = currentUser.email ?: "N/A",
                            photoUrl = currentUser.photoUrl?.toString(),
                            phone = document.getString("phone") ?: "N/A",
                            fullAddress = address
                        )
                    }
                    isLoading = false
                }.addOnFailureListener {
                    isLoading = false
                }
        } else {
            isLoading = false
        }
    }

    // App Info Dialog
    if (showAppInfoDialog) {
        AppInfoDialog(
            onDismiss = { showAppInfoDialog = false }
        )
    }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.height(80.dp),
                shape = RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 20.dp),
                shadowElevation = 4.dp
            ) {
                CenterAlignedTopAppBar(
                    title = { Text("My Account") },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = DarkPink,
                        titleContentColor = Color.White
                    ),
                    actions = {
                        IconButton(
                            onClick = { showAppInfoDialog = true },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "App Info",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )
            }
        }
    )
    { paddingValues ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                YLogoLoadingIndicator(
                    size = 40.dp,
                    color = DeepPink // or Color.Red based on your preference
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(15.dp))

                Surface(
                    shape = CircleShape,
                    border = BorderStroke(
                        width = 2.5.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = alpha)
                    ),
                    modifier = Modifier.size(120.dp),
                    shadowElevation = 10.dp
                ) {
                    AsyncImage(
                        model = userProfile?.photoUrl,
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(Modifier.height(5.dp))
                Text(text = userProfile?.name ?: "User", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                //  Text(text = userProfile?.email ?: "No email", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
                Spacer(Modifier.height(26.dp))
                ProfileInfoCard(userProfile = userProfile)

                Spacer(Modifier.height(35.dp))

                Button(
                    onClick = onNavigateToEditProfile,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Profile")
                    Spacer(Modifier.width(8.dp))
                    Text("Edit Profile")
                }
                Spacer(Modifier.height(5.dp))
                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign Out")
                    Spacer(Modifier.width(8.dp))
                    Text("Sign Out")
                }
            }
        }
    }
}

@Composable
fun AppInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "🍔 Yumzy",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = DarkPink
                )
                Text(
                    text = "Food & Grocery Delivery",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // App Info Section with Cards
                ModernInfoCard(
                    title = "App Information",
                    items = listOf(
                        InfoItem("Version", "1.0.0", "🔢"),
                        InfoItem("Release", "2024", "📅"),
                        InfoItem("Platform", "Android", "📱")
                    ),
                    backgroundColor = Color(0xFFF3E5F5)
                )

                // Developer Info Section - Modern Design
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFE8F5E9),
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF4CAF50),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = "👨‍💻",
                                        fontSize = 16.sp
                                    )
                                }
                            }
                            Text(
                                text = "Developer",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }

                        Divider(
                            color = Color(0xFF4CAF50).copy(alpha = 0.3f),
                            thickness = 1.dp
                        )

                        // Developer Details
                        DeveloperInfoRow(
                            icon = "👤",
                            label = "Name",
                            value = "BAPPI",
                            accentColor = Color(0xFF4CAF50)
                        )

                        DeveloperInfoRow(
                            icon = "📞",
                            label = "Phone",
                            value = "+880 1590093644",
                            accentColor = Color(0xFF4CAF50)
                        )

                        DeveloperInfoRow(
                            icon = "📧",
                            label = "Email",
                            value = "bappi616@gmail.com",
                            accentColor = Color(0xFF4CAF50)
                        )

                        DeveloperInfoRow(
                            icon = "💼",
                            label = "LinkedIn",
                            value = "bappi-swe",
                            accentColor = Color(0xFF4CAF50)
                        )
                    }
                }

                // Footer Message
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = DarkPink.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "Thank you for using Yumzy! 🎉\nWe're committed to bringing you the best food delivery experience.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF424242),
                        modifier = Modifier.padding(16.dp),
                        lineHeight = 20.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkPink
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text("Close", fontWeight = FontWeight.SemiBold)
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun ModernInfoCard(
    title: String,
    items: List<InfoItem>,
    backgroundColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6A1B9A)
            )

            items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.emoji,
                            fontSize = 16.sp
                        )
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF666666)
                        )
                    }
                    Text(
                        text = item.value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF333333)
                    )
                }
            }
        }
    }
}

@Composable
fun DeveloperInfoRow(
    icon: String,
    label: String,
    value: String,
    accentColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = accentColor.copy(alpha = 0.15f),
                modifier = Modifier.size(28.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = icon,
                        fontSize = 14.sp
                    )
                }
            }
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF666666),
                    fontSize = 11.sp
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1B5E20),
                    fontSize = 13.sp
                )
            }
        }
    }
}

data class InfoItem(
    val label: String,
    val value: String,
    val emoji: String
)

@Composable
fun InfoDialogRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun ProfileInfoCard(userProfile: UserProfileDetails?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            InfoRow(icon = Icons.Default.Person, label = "Name", value = userProfile?.name)
            Divider(Modifier.padding(vertical = 8.dp))
            InfoRow(icon = Icons.Default.Email, label = "Email", value = userProfile?.email)
            Divider(Modifier.padding(vertical = 8.dp))
            InfoRow(icon = Icons.Default.Phone, label = "Phone", value = userProfile?.phone)
            Divider(Modifier.padding(vertical = 8.dp))
            InfoRow(icon = Icons.Default.LocationOn, label = "Address", value = userProfile?.fullAddress)
        }
    }
}

@Composable
fun InfoRow(icon: ImageVector, label: String, value: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Text(text = value ?: "...", style = MaterialTheme.typography.bodyLarge)
        }
    }
}