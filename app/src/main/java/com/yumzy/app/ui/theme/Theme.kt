package com.yumzy.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Define the color scheme for the light theme using our custom colors.
private val LightColorScheme = lightColorScheme(
    primary = DeepPink,
    onPrimary = Color.White,
    secondary = Pink40,
    background = Color.White,
    surface = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black,
)

@Composable
fun YumzyTheme(
    darkTheme: Boolean = false, // App is always in light mode
    content: @Composable () -> Unit
) {
    // Forcing the light theme as per your project requirements.
    val colorScheme = LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Set the status bar color to our deep pink color
            window.statusBarColor = colorScheme.primary.toArgb()
            // Ensure the status bar icons (time, wifi) are visible (light-colored)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}