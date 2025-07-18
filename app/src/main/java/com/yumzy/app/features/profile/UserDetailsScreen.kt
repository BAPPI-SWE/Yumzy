package com.yumzy.app.features.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yumzy.app.features.location.LocationViewModel
import com.yumzy.app.ui.theme.DeepPink

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailsScreen(
    onSaveClicked: (name: String, phone: String, baseLocation: String, subLocation: String, building: String, floor: String, room: String) -> Unit,
    locationViewModel: LocationViewModel = viewModel()
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var building by remember { mutableStateOf("") }
    var floor by remember { mutableStateOf("") }
    var room by remember { mutableStateOf("") }

    val locationState by locationViewModel.uiState.collectAsState()
    var baseLocationExpanded by remember { mutableStateOf(false) }
    var subLocationExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Your Details") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(0.dp))
            Text("Complete Your Profile", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = DeepPink)
            Text("This information is required to use the app.", fontSize = 14.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth())

            ExposedDropdownMenuBox(expanded = baseLocationExpanded, onExpandedChange = { baseLocationExpanded = !baseLocationExpanded }) {
                OutlinedTextField(
                    value = locationState.selectedBaseLocation,
                    onValueChange = {}, readOnly = true,
                    label = { Text("Select Campus") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = baseLocationExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
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
                OutlinedTextField(
                    value = locationState.selectedSubLocation,
                    onValueChange = {}, readOnly = true,
                    label = { Text("Select Hall / Building") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subLocationExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
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

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    onSaveClicked(
                        name, phone,
                        locationState.selectedBaseLocation,
                        locationState.selectedSubLocation,
                        building, floor, room
                    )
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save & Continue")
            }
        }
    }
}