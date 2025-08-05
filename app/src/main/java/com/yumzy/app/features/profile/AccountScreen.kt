package com.yumzy.app.features.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("") },
//                actions = {
//                    IconButton(onClick = onNavigateToEditProfile) {
//                        Icon(Icons.Default.Edit, contentDescription = "Edit Profile")
//                    }
//                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    model = userProfile?.photoUrl,
                    contentDescription = "Profile Picture",
                    modifier = Modifier.size(100.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(5.dp))
                Text(text = userProfile?.name ?: "User", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(text = userProfile?.email ?: "No email", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
                Spacer(Modifier.height(32.dp))
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