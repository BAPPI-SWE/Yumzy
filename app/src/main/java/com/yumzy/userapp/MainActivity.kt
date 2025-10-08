package com.yumzy.userapp

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
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
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.auth.api.identity.Identity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import com.onesignal.user.subscriptions.IPushSubscriptionObserver
import com.onesignal.user.subscriptions.PushSubscriptionChangedState
import com.yumzy.userapp.auth.*
import com.yumzy.userapp.features.profile.UserDetailsScreen
import com.yumzy.userapp.features.splash.SplashScreen
import com.yumzy.userapp.navigation.MainScreen
import com.yumzy.userapp.ui.theme.YumzyTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {

    private val googleAuthUiClient by lazy {
        GoogleAuthUiClient(
            context = applicationContext,
            oneTapClient = Identity.getSignInClient(applicationContext)
        )
    }

    // Ask POST_NOTIFICATIONS permission for Android 13+
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                Log.d("Permission", "Notification permission granted")
            } else {
                Toast.makeText(
                    this,
                    "Please allow notifications to stay updated about your orders.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MobileAds.initialize(this) {}

        // Initialize OneSignal
        OneSignal.Debug.logLevel = LogLevel.DEBUG
        OneSignal.initWithContext(this, "dabb9362-80ed-4e54-be89-32ffc7dbf383")

        // Ask notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Observe OneSignal player ID changes (auto update Firestore)
        OneSignal.User.pushSubscription.addObserver(object : IPushSubscriptionObserver {
            override fun onPushSubscriptionChange(state: PushSubscriptionChangedState) {
                val playerId = state.current.id
                val user = Firebase.auth.currentUser
                if (!playerId.isNullOrEmpty() && user != null) {
                    Firebase.firestore.collection("users").document(user.uid)
                        .update("oneSignalPlayerId", playerId)
                        .addOnSuccessListener {
                            Log.d("Notifications", "Auto-updated OneSignal Player ID: $playerId")
                        }
                        .addOnFailureListener { e ->
                            Log.w("Notifications", "Failed to update Player ID", e)
                        }
                }
            }
        })

        setContent {
            YumzyTheme {
                InAppUpdate()
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
                            onNavigateToEmailSignIn = { navController.navigate("email_sign_in") },
                            onNavigateToEmailSignUp = { navController.navigate("email_sign_up") }
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
        ensureNotificationTokens(userId)
        val db = Firebase.firestore
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val destination = if (document.exists()) "main" else "details"
                navController.navigate(destination) {
                    popUpTo("splash") { inclusive = true }
                }
            }
    }

    // Reliable FCM + OneSignal sync with retry
    private fun ensureNotificationTokens(userId: String) {
        lifecycleScope.launch {
            try {
                val fcmToken = Firebase.messaging.token.await()

                repeat(6) { attempt -> // Retry up to 6 times (≈12 seconds)
                    val playerId = OneSignal.User.pushSubscription.id
                    if (!playerId.isNullOrEmpty()) {
                        Firebase.firestore.collection("users").document(userId)
                            .update(mapOf("fcmToken" to fcmToken, "oneSignalPlayerId" to playerId))
                            .addOnSuccessListener {
                                Log.d("Notifications", "Tokens saved successfully.")
                            }
                        return@launch
                    } else {
                        Log.w("Notifications", "Player ID not ready, retrying... ($attempt)")
                        delay(2000)
                    }
                }

                Log.e("Notifications", "Failed to get OneSignal Player ID after retries")
            } catch (e: Exception) {
                Log.e("Notifications", "Error saving notification tokens", e)
            }
        }
    }
}
