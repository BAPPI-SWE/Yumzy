package com.yumzy.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.auth.api.identity.Identity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.yumzy.app.auth.AuthScreen
import com.yumzy.app.auth.AuthViewModel
import com.yumzy.app.auth.GoogleAuthUiClient
import com.yumzy.app.features.profile.UserDetailsScreen
import com.yumzy.app.navigation.MainScreen
import com.yumzy.app.ui.theme.YumzyTheme
import kotlinx.coroutines.launch


import androidx.activity.enableEdgeToEdge


class MainActivity : ComponentActivity() {

    private val googleAuthUiClient by lazy {
        GoogleAuthUiClient(
            context = applicationContext,
            oneTapClient = Identity.getSignInClient(applicationContext)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // ✅ Your solution: Enable drawing behind the system bars
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            YumzyTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "auth") {

                    composable("auth") {
                        val viewModel = viewModel<AuthViewModel>()
                        val state by viewModel.state.collectAsStateWithLifecycle()

                        // Check if user is already signed in on app start
                        LaunchedEffect(key1 = Unit) {
                            val currentUser = googleAuthUiClient.getSignedInUser()
                            if (currentUser != null) {
                                // If signed in, check if their profile exists
                                checkUserProfile(currentUser.userId, navController)
                            }
                        }

                        val launcher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.StartIntentSenderForResult()
                        ) { result ->
                            if (result.resultCode == RESULT_OK) {
                                lifecycleScope.launch {
                                    val signInResult = googleAuthUiClient.signInWithIntent(
                                        intent = result.data ?: return@launch
                                    )
                                    viewModel.onSignInResult(signInResult)
                                }
                            }
                        }

                        // Effect to check profile after a *new* successful sign in
                        LaunchedEffect(key1 = state.isSignInSuccessful) {
                            if (state.isSignInSuccessful) {
                                Toast.makeText(
                                    applicationContext, "Sign in successful", Toast.LENGTH_LONG
                                ).show()
                                val userId = googleAuthUiClient.getSignedInUser()?.userId
                                if (userId != null) {
                                    checkUserProfile(userId, navController)
                                }
                                viewModel.resetState()
                            }
                        }

                        AuthScreen(onSignInSuccess = {
                            lifecycleScope.launch {
                                val signInIntentSender = googleAuthUiClient.signIn()
                                launcher.launch(
                                    IntentSenderRequest.Builder(
                                        signInIntentSender ?: return@launch
                                    ).build()
                                )
                            }
                        })
                    }

                    composable("details") {
                        val userId = Firebase.auth.currentUser?.uid
                        if (userId == null) {
                            // If somehow we get here without a user, go back to auth
                            navController.navigate("auth") { popUpTo("auth") { inclusive = true } }
                            return@composable
                        }
                        UserDetailsScreen(onSaveClicked = { name, phone, baseLocation, subLocation, building, floor, room ->
                            // Create a user object to save to Firestore
                            val userProfile = hashMapOf(
                                "name" to name,
                                "phone" to phone,
                                "baseLocation" to baseLocation,
                                "subLocation" to subLocation,
                                "building" to building,
                                "floor" to floor,
                                "room" to room,
                                "email" to (Firebase.auth.currentUser?.email ?: "")
                            )

                            // Save the data to Firestore
                            Firebase.firestore.collection("users").document(userId)
                                .set(userProfile)
                                .addOnSuccessListener {
                                    // On success, navigate to the main app screen
                                    navController.navigate("main") { popUpTo("auth") { inclusive = true } }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(applicationContext, "Error saving profile: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                        })
                    }

                    composable("main") {
                        MainScreen()
                    }
                }
            }
        }
    }

    private fun checkUserProfile(userId: String, navController: NavController) {
        val db = Firebase.firestore
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // If document exists, user has a profile, go to main screen
                    navController.navigate("main") {
                        popUpTo("auth") { inclusive = true }
                    }
                } else {
                    // If document does not exist, user is new, go to details screen
                    navController.navigate("details") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            }
            .addOnFailureListener {
                // Handle potential errors, for now, just show a message
                Toast.makeText(applicationContext, "Error checking profile. Please try again.", Toast.LENGTH_LONG).show()
            }
    }
}