package com.yumzy.userapp.features.profile

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
            Text(
                text = "About Yumzy",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = DarkPink
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // App Info Section
                Text(
                    text = "App Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                InfoDialogRow(label = "App Name", value = "Yumzy")
                InfoDialogRow(label = "Version", value = "1.0.0")
                InfoDialogRow(label = "Category", value = "Food and Grocery Delivery")


                Divider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )

                // Developer Info Section
                Text(
                    text = "Developer Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                InfoDialogRow(label = "Developer", value = "BAPPI")
                InfoDialogRow(label = "Phone", value = "+880 1625584646")
                InfoDialogRow(label = "Email", value = "bappi616@gmail.com")
                InfoDialogRow(label = "LinkedIn", value = "bappi-swe")

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Thank you for using Yumzy! We're committed to bringing you the best food delivery experience.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = DarkPink
                )
            ) {
                Text("Close")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

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