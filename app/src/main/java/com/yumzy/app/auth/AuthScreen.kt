package com.yumzy.app.auth

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yumzy.app.R
import com.yumzy.app.ui.theme.DeepPink
import com.yumzy.app.ui.theme.YumzyTheme

@Composable
fun AuthScreen(
    onSignInSuccess: () -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = isLoading) {
        if (isLoading) {
            // You can show a loading indicator here if you want
        }
    }

    // The main UI of your AuthScreen
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepPink),
        contentAlignment = Alignment.BottomCenter
    ) {
        // ... (The rest of your UI: Column with Title, Image, etc. remains the same)
        // This code is omitted for brevity but should be the same as your working version.

        // The important part is the buttons inside the bottom white card:
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.55f) // Takes 55% of the screen height
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .background(Color.White)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            //... Your "Sign up or Log in" text widgets
            Text(
                text = "Sign up or Log in",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Select your preferred method to continue",
                fontSize = 14.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(32.dp))


            // Continue with Google Button
            Button(
                onClick = {
                    // This will be handled by the logic in MainActivity for now
                    onSignInSuccess()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_google_logo),
                        contentDescription = "Google Logo",
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Continue with Google",
                        color = Color.Black,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Continue with Email Button
            Button(
                onClick = {
                    Toast.makeText(context, "Email sign-in coming soon!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DeepPink)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Email Icon",
                        tint = Color.White
                    )
                    Text(
                        text = "Continue with email",
                        color = Color.White,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    YumzyTheme {
        AuthScreen(onSignInSuccess = {})
    }
}