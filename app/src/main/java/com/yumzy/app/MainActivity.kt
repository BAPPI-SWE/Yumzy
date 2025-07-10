package com.yumzy.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.yumzy.app.navigation.MainScreen
import com.yumzy.app.ui.theme.YumzyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            YumzyTheme {
                // The MainScreen now handles all app navigation
                MainScreen()
            }
        }
    }
}