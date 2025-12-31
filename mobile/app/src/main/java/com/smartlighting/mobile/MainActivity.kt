package com.smartlighting.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.smartlighting.mobile.data.local.TokenManager
import com.smartlighting.mobile.ui.navigation.NavGraph
import com.smartlighting.mobile.ui.navigation.Routes
import com.smartlighting.mobile.ui.theme.SmartLightingTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var tokenManager: TokenManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        
        setContent {
            SmartLightingTheme {
                SmartLightingApp(tokenManager)
            }
        }
    }
}

@Composable
fun SmartLightingApp(tokenManager: TokenManager) {
    val navController = rememberNavController()
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        NavGraph(
            navController = navController,
            tokenManager = tokenManager
        )
    }
}
