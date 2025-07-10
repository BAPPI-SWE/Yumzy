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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.auth.api.identity.Identity
import com.yumzy.app.auth.AuthScreen
import com.yumzy.app.auth.AuthViewModel
import com.yumzy.app.auth.GoogleAuthUiClient
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
        super.onCreate(savedInstanceState)
        setContent {
            YumzyTheme {
                val navController = rememberNavController()

                // This NavHost will be our app's main router
                NavHost(navController = navController, startDestination = "auth") {

                    composable("auth") {
                        val viewModel = viewModel<AuthViewModel>()
                        val state by viewModel.state.collectAsStateWithLifecycle()

                        // If user is signed in, navigate away from auth screen
                        LaunchedEffect(key1 = Unit) {
                            if(googleAuthUiClient.getSignedInUser() != null) {
                                navController.navigate("main") {
                                    popUpTo("auth") { inclusive = true }
                                }
                            }
                        }


                        val launcher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.StartIntentSenderForResult(),
                            onResult = { result ->
                                if (result.resultCode == RESULT_OK) {
                                    lifecycleScope.launch {
                                        val signInResult = googleAuthUiClient.signInWithIntent(
                                            intent = result.data ?: return@launch
                                        )
                                        viewModel.onSignInResult(signInResult)
                                    }
                                }
                            }
                        )

                        // Effect to navigate on successful sign in
                        LaunchedEffect(key1 = state.isSignInSuccessful) {
                            if (state.isSignInSuccessful) {
                                Toast.makeText(
                                    applicationContext,
                                    "Sign in successful",
                                    Toast.LENGTH_LONG
                                ).show()

                                // Navigate to main screen, clearing the auth screen from backstack
                                navController.navigate("main") {
                                    popUpTo("auth") { inclusive = true }
                                }

                                viewModel.resetState()
                            }
                        }

                        AuthScreen(
                            onSignInSuccess = {
                                lifecycleScope.launch {
                                    val signInIntentSender = googleAuthUiClient.signIn()
                                    launcher.launch(
                                        IntentSenderRequest.Builder(
                                            signInIntentSender ?: return@launch
                                        ).build()
                                    )
                                }
                            }
                        )
                    }

                    composable("main") {
                        // This destination is your main app with the bottom navigation
                        MainScreen()
                    }

                    // We can add other destinations like UserDetailsScreen here later
                    composable("details") {
                        UserDetailsScreen(onSaveClicked = {
                            // TODO: Logic to save details and navigate to main
                        })
                    }
                }
            }
        }
    }
}