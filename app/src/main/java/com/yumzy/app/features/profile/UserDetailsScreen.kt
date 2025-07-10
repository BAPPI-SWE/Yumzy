package com.yumzy.app.features.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yumzy.app.ui.theme.DeepPink
import com.yumzy.app.ui.theme.YumzyTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailsScreen(onSaveClicked: () -> Unit) {
    // State variables to hold the user's input
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var building by remember { mutableStateOf("") }
    var floor by remember { mutableStateOf("") }
    var room by remember { mutableStateOf("") }

    // State for dropdown menus
    val baseLocations = listOf("Daffodil Smart City", "North South University")
    var baseLocationExpanded by remember { mutableStateOf(false) }
    var selectedBaseLocation by remember { mutableStateOf(baseLocations[0]) }

    val subLocations = listOf("Hall 1", "Hall 2", "Faculty Room A", "Faculty Room B")
    var subLocationExpanded by remember { mutableStateOf(false) }
    var selectedSubLocation by remember { mutableStateOf(subLocations[0]) }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Details") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepPink,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            Button(
                onClick = { onSaveClicked() /* TODO: Save data to Firebase */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DeepPink)
            ) {
                Text(text = "Save & Continue", modifier = Modifier.padding(8.dp))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()), // Makes the column scrollable
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Complete Your Profile",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = DeepPink
            )
            Text(
                text = "This information is required to use the app.",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Input Fields
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Location Selection
            // Base Location Dropdown
            ExposedDropdownMenuBox(
                expanded = baseLocationExpanded,
                onExpandedChange = { baseLocationExpanded = !baseLocationExpanded }
            ) {
                OutlinedTextField(
                    value = selectedBaseLocation,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Campus") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = baseLocationExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = baseLocationExpanded,
                    onDismissRequest = { baseLocationExpanded = false }
                ) {
                    baseLocations.forEach { location ->
                        DropdownMenuItem(
                            text = { Text(location) },
                            onClick = {
                                selectedBaseLocation = location
                                baseLocationExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sub Location Dropdown
            ExposedDropdownMenuBox(
                expanded = subLocationExpanded,
                onExpandedChange = { subLocationExpanded = !subLocationExpanded }
            ) {
                OutlinedTextField(
                    value = selectedSubLocation,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Hall / Building") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subLocationExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = subLocationExpanded,
                    onDismissRequest = { subLocationExpanded = false }
                ) {
                    subLocations.forEach { location ->
                        DropdownMenuItem(
                            text = { Text(location) },
                            onClick = {
                                selectedSubLocation = location
                                subLocationExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = building,
                onValueChange = { building = it },
                label = { Text("Building Name (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = floor,
                    onValueChange = { floor = it },
                    label = { Text("Floor No.") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(16.dp))
                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it },
                    label = { Text("Room No.") },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(100.dp)) // Extra space to prevent button overlap
        }
    }
}

@Preview(showBackground = true)
@Composable
fun UserDetailsScreenPreview() {
    YumzyTheme {
        UserDetailsScreen(onSaveClicked = {})
    }
}