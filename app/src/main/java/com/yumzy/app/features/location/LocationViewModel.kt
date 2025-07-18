package com.yumzy.app.features.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// This data class will hold all the state for our UI
data class LocationUiState(
    val allLocations: Map<String, List<String>> = emptyMap(),
    val baseLocationOptions: List<String> = emptyList(),
    val subLocationOptions: List<String> = emptyList(),
    val selectedBaseLocation: String = "",
    val selectedSubLocation: String = ""
)

class LocationViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LocationUiState())
    val uiState = _uiState.asStateFlow()

    init {
        fetchLocations()
    }

    private fun fetchLocations() {
        viewModelScope.launch {
            Firebase.firestore.collection("locations").get()
                .addOnSuccessListener { snapshot ->
                    val locationMap = mutableMapOf<String, List<String>>()
                    snapshot.documents.forEach { doc ->
                        val baseName = doc.getString("name") ?: ""
                        val subLocations = doc.get("subLocations") as? List<String> ?: emptyList()
                        if (baseName.isNotBlank()) {
                            locationMap[baseName] = subLocations
                        }
                    }

                    val baseLocations = locationMap.keys.toList()
                    val firstBaseLocation = baseLocations.firstOrNull() ?: ""
                    val firstSubLocations = locationMap[firstBaseLocation] ?: emptyList()

                    _uiState.update {
                        it.copy(
                            allLocations = locationMap,
                            baseLocationOptions = baseLocations,
                            subLocationOptions = firstSubLocations,
                            selectedBaseLocation = firstBaseLocation,
                            selectedSubLocation = firstSubLocations.firstOrNull() ?: ""
                        )
                    }
                }
        }
    }

    fun onBaseLocationSelected(newSelection: String) {
        val newSubLocations = _uiState.value.allLocations[newSelection] ?: emptyList()
        _uiState.update {
            it.copy(
                selectedBaseLocation = newSelection,
                subLocationOptions = newSubLocations,
                selectedSubLocation = newSubLocations.firstOrNull() ?: "" // Reset sub-location
            )
        }
    }

    fun onSubLocationSelected(newSelection: String) {
        _uiState.update { it.copy(selectedSubLocation = newSelection) }
    }
}