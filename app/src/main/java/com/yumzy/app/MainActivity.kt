package com.yumzy.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.yumzy.app.auth.AuthScreen
import com.yumzy.app.auth.AuthViewModel
import com.yumzy.app.auth.GoogleAuthUiClient
import com.yumzy.app.features.profile.EditProfileScreen
import com.yumzy.app.features.profile.UserDetailsScreen
import com.yumzy.app.navigation.MainScreen
import com.yumzy.app.ui.theme.YumzyTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val googleAuthUiClient by lazy {
        GoogleAuthUiClient(
            context = applicationContext,
            oneTapClient = Identity.getSignInClient(applicationContext)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            YumzyTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "auth") {

                    composable("auth") {
                        val viewModel = viewModel<AuthViewModel>()
                        val state by viewModel.state.collectAsStateWithLifecycle()

                        LaunchedEffect(key1 = Unit) {
                            val currentUser = googleAuthUiClient.getSignedInUser()
                            if (currentUser != null) {
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

                        LaunchedEffect(key1 = state.isSignInSuccessful) {
                            if (state.isSignInSuccessful) {
                                Toast.makeText(applicationContext, "Sign in successful", Toast.LENGTH_LONG).show()
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
                        val userId = Firebase.auth.currentUser?.uid ?: return@composable
                        UserDetailsScreen(onSaveClicked = { name, phone, baseLocation, subLocation, building, floor, room ->
                            val userProfile = hashMapOf(
                                "name" to name, "phone" to phone,
                                "baseLocation" to baseLocation, "subLocation" to subLocation,
                                "building" to building, "floor" to floor, "room" to room,
                                "email" to (Firebase.auth.currentUser?.email ?: "")
                            )
                            Firebase.firestore.collection("users").document(userId)
                                .set(userProfile)
                                .addOnSuccessListener {
                                    navController.navigate("main") { popUpTo("auth") { inclusive = true } }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(applicationContext, "Error saving profile: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                        })
                    }

                    composable("main") {
                        MainScreen(
                            onSignOut = {
                                lifecycleScope.launch {
                                    googleAuthUiClient.signOut()
                                    Toast.makeText(applicationContext, "Signed out", Toast.LENGTH_SHORT).show()
                                    navController.navigate("auth") {
                                        popUpTo(navController.graph.id) { inclusive = true }
                                    }
                                }
                            }
                        )
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
                    navController.navigate("main") { popUpTo("auth") { inclusive = true } }
                } else {
                    navController.navigate("details") { popUpTo("auth") { inclusive = true } }
                }
            }
            .addOnFailureListener {
                Toast.makeText(applicationContext, "Error checking profile. Please try again.", Toast.LENGTH_LONG).show()
            }
    }
}