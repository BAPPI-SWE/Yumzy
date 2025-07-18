package com.yumzy.app.features.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.yumzy.app.features.location.LocationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onSaveChanges: (name: String, phone: String, baseLocation: String, subLocation: String, building: String, floor: String, room: String) -> Unit,
    onBackClicked: () -> Unit,
    locationViewModel: LocationViewModel = viewModel()
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var building by remember { mutableStateOf("") }
    var floor by remember { mutableStateOf("") }
    var room by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    val locationState by locationViewModel.uiState.collectAsState()
    var baseLocationExpanded by remember { mutableStateOf(false) }
    var subLocationExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = Unit) {
        val userId = Firebase.auth.currentUser?.uid
        if (userId != null) {
            Firebase.firestore.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        name = document.getString("name") ?: (Firebase.auth.currentUser?.displayName ?: "")
                        phone = document.getString("phone") ?: ""
                        building = document.getString("building") ?: ""
                        floor = document.getString("floor") ?: ""
                        room = document.getString("room") ?: ""

                        val savedBase = document.getString("baseLocation") ?: ""
                        if (savedBase.isNotEmpty()) {
                            locationViewModel.onBaseLocationSelected(savedBase)
                            val savedSub = document.getString("subLocation") ?: ""
                            if (savedSub.isNotEmpty()) {
                                locationViewModel.onSubLocationSelected(savedSub)
                            }
                        }
                    }
                    isLoading = false
                }
        } else { isLoading = false }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth())

                ExposedDropdownMenuBox(expanded = baseLocationExpanded, onExpandedChange = { baseLocationExpanded = !baseLocationExpanded }) {
                    OutlinedTextField(value = locationState.selectedBaseLocation, onValueChange = {}, readOnly = true, label = { Text("Select Campus") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = baseLocationExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                    ExposedDropdownMenu(expanded = baseLocationExpanded, onDismissRequest = { baseLocationExpanded = false }) {
                        locationState.baseLocationOptions.forEach { location ->
                            DropdownMenuItem(text = { Text(location) }, onClick = {
                                locationViewModel.onBaseLocationSelected(location)
                                baseLocationExpanded = false
                            })
                        }
                    }
                }

                ExposedDropdownMenuBox(expanded = subLocationExpanded, onExpandedChange = { subLocationExpanded = !subLocationExpanded }) {
                    OutlinedTextField(value = locationState.selectedSubLocation, onValueChange = {}, readOnly = true, label = { Text("Select Hall / Building") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subLocationExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                    ExposedDropdownMenu(expanded = subLocationExpanded, onDismissRequest = { subLocationExpanded = false }) {
                        if (locationState.subLocationOptions.isEmpty()) {
                            DropdownMenuItem(text = { Text("No locations available") }, onClick = {}, enabled = false)
                        } else {
                            locationState.subLocationOptions.forEach { location ->
                                DropdownMenuItem(text = { Text(location) }, onClick = {
                                    locationViewModel.onSubLocationSelected(location)
                                    subLocationExpanded = false
                                })
                            }
                        }
                    }
                }

                OutlinedTextField(value = building, onValueChange = { building = it }, label = { Text("Building Name (Optional)") }, modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(value = floor, onValueChange = { floor = it }, label = { Text("Floor No.") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = room, onValueChange = { room = it }, label = { Text("Room No.") }, modifier = Modifier.weight(1f))
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        onSaveChanges(
                            name, phone,
                            locationState.selectedBaseLocation,
                            locationState.selectedSubLocation,
                            building, floor, room
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Changes")
                }
            }
        }
    }
}