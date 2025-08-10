package com.yumzy.userapp

// InAppUpdate.kt


import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.launch

/**
 * Simple In-App Update handler for Jetpack Compose.
 *
 * Place `InAppUpdate()` near the root of your Compose UI (inside setContent).
 *
 * Parameters:
 *  - daysForFlexible: minimum staleness (days) to allow flexible update (default 0)
 *  - daysForImmediate: minimum staleness (days) or priority to force immediate (default 7)
 *  - priorityForImmediate: play update priority threshold to force immediate (default 4)
 */
@Composable
fun InAppUpdate(
    daysForFlexible: Int = 0,
    daysForImmediate: Int = 7,
    priorityForImmediate: Int = 4
) {
    val context = LocalContext.current
    val activity = context as Activity
    val appUpdateManager = remember { AppUpdateManagerFactory.create(context) }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Activity result launcher for the update flow (StartIntentSenderForResult)
    val updateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            Toast.makeText(context, "Update canceled or failed", Toast.LENGTH_SHORT).show()
        }
    }

    // Listener to track flexible update progress & completion
    val installListener = remember {
        InstallStateUpdatedListener { state ->
            when (state.installStatus()) {
                InstallStatus.DOWNLOADING -> {
                    // Optional: show progress using state.bytesDownloaded() / state.totalBytesToDownload()
                }
                InstallStatus.DOWNLOADED -> {
                    // For flexible updates: prompt user to restart/install
                    scope.launch {
                        Toast.makeText(context, "Update downloaded — installing now", Toast.LENGTH_LONG).show()
                        appUpdateManager.completeUpdate()
                    }
                }
                else -> {
                    // handle other statuses if you want (FAILED, INSTALLED, etc.)
                }
            }
        }
    }

    // Unregister listener when not needed
    DisposableEffect(Unit) {
        onDispose {
            try { appUpdateManager.unregisterListener(installListener) } catch (_: Exception) {}
        }
    }

    // On resume: if a flexible update was downloaded earlier but not installed, finish it.
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
                    if (info.installStatus() == InstallStatus.DOWNLOADED) {
                        // Option: ask user or auto-complete
                        Toast.makeText(context, "Completing previously downloaded update...", Toast.LENGTH_LONG).show()
                        appUpdateManager.completeUpdate()
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    // Initial check (runs once)
    LaunchedEffect(Unit) {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            when (appUpdateInfo.updateAvailability()) {
                UpdateAvailability.UPDATE_AVAILABLE -> {
                    if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            updateLauncher,
                            AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                        )
                    }
                }

                UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                    // Resume an update already in progress
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        updateLauncher,
                        AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                    )
                }

                else -> {
                    // No update available
                }
            }
        }
    }

}
