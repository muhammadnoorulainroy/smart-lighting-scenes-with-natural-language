package com.smartlighting.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.smartlighting.mobile.ui.theme.SmartLightingTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display
        enableEdgeToEdge()
        
        setContent {
            SmartLightingTheme {
                SmartLightingApp()
            }
        }
    }
}

@Composable
fun SmartLightingApp() {
    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        // Main app content will go here
        MainScreen()
    }
}

@Composable
fun MainScreen() {
    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        // Navigation and main content
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // App navigation will be implemented here
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SmartLightingAppPreview() {
    SmartLightingTheme {
        SmartLightingApp()
    }
}
