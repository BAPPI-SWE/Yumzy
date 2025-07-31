package com.yumzy.app

import android.os.Bundle
import android.util.Log
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
import com.google.firebase.messaging.ktx.messaging
import com.yumzy.app.auth.*
import com.yumzy.app.features.profile.UserDetailsScreen
import com.yumzy.app.features.splash.SplashScreen
import com.yumzy.app.navigation.MainScreen
import com.yumzy.app.ui.theme.YumzyTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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

                NavHost(navController = navController, startDestination = "splash") {

                    composable("splash") {
                        SplashScreen(
                            onAnimationFinish = {
                                val currentUser = googleAuthUiClient.getSignedInUser()
                                val firebaseUser = Firebase.auth.currentUser
                                if (currentUser != null || firebaseUser != null) {
                                    val userId = currentUser?.userId ?: firebaseUser?.uid
                                    if (userId != null) checkUserProfile(userId, navController)
                                } else {
                                    navController.navigate("auth") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                }
                            }
                        )
                    }

                    composable("auth") {
                        val viewModel = viewModel<AuthViewModel>()
                        val state by viewModel.state.collectAsStateWithLifecycle()

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
                                val userId = googleAuthUiClient.getSignedInUser()?.userId
                                if (userId != null) {
                                    checkUserProfile(userId, navController)
                                }
                                viewModel.resetState()
                            }
                        }

                        AuthScreen(
                            onGoogleSignInClick = {
                                lifecycleScope.launch {
                                    val signInIntentSender = googleAuthUiClient.signIn()
                                    launcher.launch(
                                        IntentSenderRequest.Builder(signInIntentSender ?: return@launch).build()
                                    )
                                }
                            },
                            onNavigateToEmailSignIn = {
                                navController.navigate("email_sign_in")
                            },
                            onNavigateToEmailSignUp = {
                                navController.navigate("email_sign_up")
                            }
                        )
                    }

                    composable("email_sign_up") {
                        EmailSignUpScreen(
                            onBackToAuth = { navController.popBackStack() },
                            onSignUpClicked = { name, email, pass ->
                                if (name.isBlank() || email.isBlank() || pass.isBlank()) {
                                    Toast.makeText(applicationContext, "All fields are required", Toast.LENGTH_SHORT).show()
                                    return@EmailSignUpScreen
                                }
                                Firebase.auth.createUserWithEmailAndPassword(email, pass)
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            val user = task.result?.user
                                            val profileUpdates = userProfileChangeRequest { displayName = name }
                                            user?.updateProfile(profileUpdates)?.addOnCompleteListener {
                                                if (user != null) {
                                                    checkUserProfile(user.uid, navController)
                                                }
                                            }
                                        } else {
                                            Toast.makeText(applicationContext, task.exception?.message ?: "Sign up failed", Toast.LENGTH_LONG).show()
                                        }
                                    }
                            }
                        )
                    }

                    composable("email_sign_in") {
                        EmailSignInScreen(
                            onBackToAuth = { navController.popBackStack() },
                            onSignInClicked = { email, pass ->
                                if (email.isBlank() || pass.isBlank()) {
                                    Toast.makeText(applicationContext, "All fields are required", Toast.LENGTH_SHORT).show()
                                    return@EmailSignInScreen
                                }
                                Firebase.auth.signInWithEmailAndPassword(email, pass)
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            val user = task.result?.user
                                            if (user != null) {
                                                checkUserProfile(user.uid, navController)
                                            }
                                        } else {
                                            Toast.makeText(applicationContext, task.exception?.message ?: "Sign in failed", Toast.LENGTH_LONG).show()
                                        }
                                    }
                            }
                        )
                    }

                    composable("details") {
                        val userId = Firebase.auth.currentUser?.uid ?: return@composable
                        UserDetailsScreen(onSaveClicked = { name, phone, baseLocation, subLocation, building, floor, room ->
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
                            Firebase.firestore.collection("users").document(userId)
                                .set(userProfile)
                                .addOnSuccessListener {
                                    navController.navigate("main") {
                                        popUpTo("auth") { inclusive = true }
                                    }
                                }
                        })
                    }

                    composable("main") {
                        MainScreen(
                            onSignOut = {
                                lifecycleScope.launch {
                                    Firebase.auth.signOut()
                                    googleAuthUiClient.signOut()
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
        getAndSaveFcmToken(userId)
        val db = Firebase.firestore
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val destination = if (document.exists()) "main" else "details"
                navController.navigate(destination) {
                    popUpTo("splash") { inclusive = true }
                }
            }
    }

    private fun getAndSaveFcmToken(userId: String) {
        lifecycleScope.launch {
            try {
                val token = Firebase.messaging.token.await()
                Firebase.firestore.collection("users").document(userId)
                    .update("fcmToken", token)
                Log.d("FCM", "Token saved for user $userId")
            } catch (e: Exception) {
                Log.w("FCM", "Fetching FCM registration token failed", e)
            }
        }
    }
}
